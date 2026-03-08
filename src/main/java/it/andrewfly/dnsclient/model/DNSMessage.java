package it.andrewfly.dnsclient.model;

import it.andrewfly.dnsclient.DNSSerializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class DNSMessage implements DNSSerializable {
    private Header header;
    private List<Question> questions;
    private List<ResourceRecord> answers;
    private List<ResourceRecord> authorities;
    private List<ResourceRecord> additionals;

    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // Header
            baos.write(header.toByteArray());

            // Questions
            for (Question question : questions) {
                baos.write(question.toByteArray());
            }

            // Answers
            for (ResourceRecord answer : answers) {
                baos.write(answer.toByteArray());
            }

            // Authorities
            for (ResourceRecord authority : authorities) {
                baos.write(authority.toByteArray());
            }

            // Additionals
            for (ResourceRecord additional : additionals) {
                baos.write(additional.toByteArray());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error serializing DNS message", e);
        }

        return baos.toByteArray();
    }

    public static DNSMessage fromByteArray(byte[] bytes) {
        int offset = 0;

        // Parse Header (12 bytes)
        Header header = Header.fromByteArray(bytes);
        offset += 12;

        // Parse Questions
        List<Question> questions = new java.util.ArrayList<>();
        for (int i = 0; i < header.getQdCount(); i++) {
            Question question = Question.fromByteArray(bytes, offset);
            questions.add(question);
            offset += calculateQuestionLength(bytes, offset);
        }

        // Parse Answers
        List<ResourceRecord> answers = new java.util.ArrayList<>();
        for (int i = 0; i < header.getAnCount(); i++) {
            ResourceRecord answer = ResourceRecord.fromByteArray(bytes, offset);
            answers.add(answer);
            offset += calculateResourceRecordLength(bytes, offset);
        }

        // Parse Authorities
        List<ResourceRecord> authorities = new java.util.ArrayList<>();
        for (int i = 0; i < header.getNsCount(); i++) {
            ResourceRecord authority = ResourceRecord.fromByteArray(bytes, offset);
            authorities.add(authority);
            offset += calculateResourceRecordLength(bytes, offset);
        }

        // Parse Additionals
        List<ResourceRecord> additionals = new java.util.ArrayList<>();
        for (int i = 0; i < header.getArCount(); i++) {
            ResourceRecord additional = ResourceRecord.fromByteArray(bytes, offset);
            additionals.add(additional);
            offset += calculateResourceRecordLength(bytes, offset);
        }

        return DNSMessage.builder()
                .header(header)
                .questions(questions)
                .answers(answers)
                .authorities(authorities)
                .additionals(additionals)
                .build();
    }

    private static int calculateQuestionLength(byte[] bytes, int offset) {
        int originalOffset = offset;

        // Skip QNAME
        offset = skipName(bytes, offset);

        // QTYPE (2 bytes) + QCLASS (2 bytes)
        offset += 4;

        return offset - originalOffset;
    }

    private static int calculateResourceRecordLength(byte[] bytes, int offset) {
        int originalOffset = offset;

        // Skip RNAME
        offset = skipName(bytes, offset);

        // RTYPE (2) + RCLASS (2) + TTL (4) + RDLENGTH (2)
        offset += 10;

        // Leggi RDLENGTH
        int rdLength = ((bytes[offset - 2] & 0xFF) << 8) | (bytes[offset - 1] & 0xFF);

        // Aggiungi RDLENGTH
        offset += rdLength;

        return offset - originalOffset;
    }

    private static int skipName(byte[] bytes, int offset) {
        int currentOffset = offset;

        while (true) {
            int length = bytes[currentOffset] & 0xFF;

            // Check for compression pointer
            if ((length & 0xC0) == 0xC0) {
                // È un puntatore, consuma 2 byte e termina
                return currentOffset + 2;
            }

            currentOffset++;

            if (length == 0) {
                break; // Fine del nome
            }

            currentOffset += length;
        }

        return currentOffset;
    }

    public static DNSMessage buildQuery(String query, Type queryType) {
        // Genera ID random
        java.util.Random random = new java.util.Random();
        int id = random.nextInt(0x10000);

        // Crea Flags per query standard
        Flags flags = Flags.builder()
                .qr(false)           // Query
                .opCode(0)           // Standard query
                .aa(false)           // Non authoritative
                .tc(false)           // Not truncated
                .rd(true)            // Recursion desired
                .ra(false)           // N/A per query
                .z(0)             // Reserved
                .rCode(0)            // N/A per query
                .build();

        // Crea Header
        Header header = Header.builder()
                .id(id)
                .flags(flags)
                .qdCount(1)          // 1 domanda
                .anCount(0)          // 0 risposte nella query
                .nsCount(0)          // 0 nameserver
                .arCount(0)          // 0 additional
                .build();

        // Crea Question
        Question question = Question.builder()
                .qName(query)
                .qType(queryType)
                .qClass(DNSClass.IN) // Internet class
                .build();

        return DNSMessage.builder()
                .header(header)
                .questions(java.util.List.of(question))
                .answers(java.util.List.of())
                .authorities(java.util.List.of())
                .additionals(java.util.List.of())
                .build();
    }
}
