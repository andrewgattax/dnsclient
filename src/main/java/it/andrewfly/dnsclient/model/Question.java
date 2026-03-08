package it.andrewfly.dnsclient.model;

import it.andrewfly.dnsclient.DNSSerializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Question implements DNSSerializable {
    private String qName; // lunghezza variabile, iL DOMINIOO
    private Type qType; // 2B, il tipo della richiesta
    private DNSClass qClass; // 2B, la classe della query

    @Override
    public byte[] toByteArray() {
        // Calcolo la lunghezza totale prima di allocare
        byte[] nameBytes = encodeName(qName);
        int nameLength = nameBytes.length;
        int totalLength = nameLength + 4; // name + 2B type + 2B class

        byte[] bytes = new byte[totalLength];
        int offset = 0;

        // QNAME: label encoding
        System.arraycopy(nameBytes, 0, bytes, offset, nameBytes.length);
        offset += nameBytes.length;

        // QTYPE: 2 bytes
        bytes[offset++] = (byte) ((qType.getValue() >> 8) & 0xFF);
        bytes[offset++] = (byte) (qType.getValue() & 0xFF);

        // QCLASS: 2 bytes
        bytes[offset++] = (byte) ((qClass.getValue() >> 8) & 0xFF);
        bytes[offset++] = (byte) (qClass.getValue() & 0xFF);

        return bytes;
    }

    private byte[] encodeName(String domain) {
        String[] labels = domain.split("\\.");
        byte[] encoded = new byte[domain.length() + 2]; // +1 per ogni length byte, +1 per il byte 0 finale
        int offset = 0;

        for (String label : labels) {
            byte[] labelBytes = label.getBytes();
            encoded[offset++] = (byte) labelBytes.length;
            System.arraycopy(labelBytes, 0, encoded, offset, labelBytes.length);
            offset += labelBytes.length;
        }

        encoded[offset] = 0; // Terminatore
        return java.util.Arrays.copyOf(encoded, offset + 1);
    }

    public static Question fromByteArray(byte[] bytes) {
        // Parse QNAME
        StringBuilder nameBuilder = new StringBuilder();
        int offset = 0;

        while (true) {
            int length = bytes[offset] & 0xFF;

            // Check for compression (pointer)
            if ((length & 0xC0) == 0xC0) {
                // Compression pointer - salta questo per ora
                offset += 2;
                break;
            }

            offset++;

            if (length == 0) {
                break; // Fine del nome
            }

            if (!nameBuilder.isEmpty()) {
                nameBuilder.append('.');
            }

            String label = new String(bytes, offset, length);
            nameBuilder.append(label);
            offset += length;
        }

        String qName = nameBuilder.toString();

        // QTYPE: 2 bytes
        int qTypeValue = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        Type qType = Type.fromValue(qTypeValue);
        offset += 2;

        // QCLASS: 2 bytes
        int qClassValue = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        DNSClass qClass = DNSClass.fromValue(qClassValue);
        offset += 2;

        return Question.builder()
                .qName(qName)
                .qType(qType)
                .qClass(qClass)
                .build();
    }

    public static Question fromByteArray(byte[] bytes, int offset) {
        // Parse QNAME
        StringBuilder nameBuilder = new StringBuilder();
        int originalOffset = offset;

        while (true) {
            int length = bytes[offset] & 0xFF;

            // Check for compression (pointer)
            if ((length & 0xC0) == 0xC0) {
                // Compression pointer - salta questo per ora
                offset += 2;
                break;
            }

            offset++;

            if (length == 0) {
                break; // Fine del nome
            }

            if (!nameBuilder.isEmpty()) {
                nameBuilder.append('.');
            }

            String label = new String(bytes, offset, length);
            nameBuilder.append(label);
            offset += length;
        }

        String qName = nameBuilder.toString();

        // QTYPE: 2 bytes
        int qTypeValue = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        Type qType = Type.fromValue(qTypeValue);
        offset += 2;

        // QCLASS: 2 bytes
        int qClassValue = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        DNSClass qClass = DNSClass.fromValue(qClassValue);
        offset += 2;

        return Question.builder()
                .qName(qName)
                .qType(qType)
                .qClass(qClass)
                .build();
    }
}
