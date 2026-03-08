package it.andrewfly.dnsclient.model;

import it.andrewfly.dnsclient.DNSSerializable;
import it.andrewfly.dnsclient.service.UtilService;
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
        byte[] nameBytes = UtilService.encodeName(qName);
        int nameLength = nameBytes.length;
        int totalLength = nameLength + 4; // name + 2B type + 2B class

        byte[] bytes = new byte[totalLength];
        int offset = 0;

        // QNAME: label encoding
        System.arraycopy(nameBytes, 0, bytes, offset, nameBytes.length);
        offset += nameBytes.length;

        // QTYPE: 2 bytes
        UtilService.writeUint16(bytes, offset, qType.getValue());
        offset += 2;

        // QCLASS: 2 bytes
        UtilService.writeUint16(bytes, offset, qClass.getValue());
        offset += 2;

        return bytes;
    }

    public static Question fromByteArray(byte[] bytes) {
        return fromByteArray(bytes, 0);
    }

    public static Question fromByteArray(byte[] bytes, int offset) {
        // Parse QNAME with compression support
        String qName = UtilService.parseName(bytes, offset);

        // Calculate bytes consumed for name
        offset = UtilService.skipName(bytes, offset);

        // QTYPE: 2 bytes
        int qTypeValue = UtilService.readUint16(bytes, offset);
        Type qType = Type.fromValue(qTypeValue);
        offset += 2;

        // QCLASS: 2 bytes
        int qClassValue = UtilService.readUint16(bytes, offset);
        DNSClass qClass = DNSClass.fromValue(qClassValue);
        offset += 2;

        return Question.builder()
                .qName(qName)
                .qType(qType)
                .qClass(qClass)
                .build();
    }
}
