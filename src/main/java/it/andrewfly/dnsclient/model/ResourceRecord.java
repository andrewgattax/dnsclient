package it.andrewfly.dnsclient.model;

import it.andrewfly.dnsclient.DNSSerializable;
import it.andrewfly.dnsclient.service.UtilService;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ResourceRecord implements DNSSerializable {
    private String rName; // lunghezza variabile
    private Type rType; // 2B
    private DNSClass rClass; // 2B
    private int ttl; // 4B
    private int rdLength; // 2B
    private String rData; // rdLenght * 1B

    @Override
    public byte[] toByteArray() {
        // Per ora non implementiamo la compressione
        byte[] nameBytes = UtilService.encodeName(rName);
        int totalLength = nameBytes.length + 10 + rdLength; // name + 2+2+4+2 + data

        byte[] bytes = new byte[totalLength];
        int offset = 0;

        // RNAME: label encoding
        System.arraycopy(nameBytes, 0, bytes, offset, nameBytes.length);
        offset += nameBytes.length;

        // RTYPE: 2 bytes
        UtilService.writeUint16(bytes, offset, rType.getValue());
        offset += 2;

        // RCLASS: 2 bytes
        UtilService.writeUint16(bytes, offset, rClass.getValue());
        offset += 2;

        // TTL: 4 bytes
        UtilService.writeUint32(bytes, offset, ttl);
        offset += 4;

        // RDLENGTH: 2 bytes
        UtilService.writeUint16(bytes, offset, rdLength);
        offset += 2;

        // RDATA
        byte[] dataBytes = encodeRData();
        System.arraycopy(dataBytes, 0, bytes, offset, dataBytes.length);

        return bytes;
    }

    private byte[] encodeRData() {
        if (rType == Type.A && rData != null && !rData.isEmpty()) {
            String[] parts = rData.split("\\.");
            byte[] ip = new byte[4];
            for (int i = 0; i < 4; i++) {
                ip[i] = (byte) Integer.parseInt(parts[i]);
            }
            return ip;
        }
        // Per altri tipi, ritorna i byte raw della stringa
        return rData != null ? rData.getBytes() : new byte[0];
    }

    public static ResourceRecord fromByteArray(byte[] bytes, int offset) {
        int originalOffset = offset;

        // Parse RNAME (con gestione compressione)
        String rName = UtilService.parseName(bytes, offset);

        // Calcola quanti byte abbiamo consumato per il nome
        offset = UtilService.skipName(bytes, offset);

        // RTYPE: 2 bytes
        int rTypeValue = UtilService.readUint16(bytes, offset);
        Type rType = Type.fromValue(rTypeValue);
        offset += 2;

        // RCLASS: 2 bytes
        int rClassValue = UtilService.readUint16(bytes, offset);
        DNSClass rClass = DNSClass.fromValue(rClassValue);
        offset += 2;

        // TTL: 4 bytes
        int ttl = UtilService.readUint32(bytes, offset);
        offset += 4;

        // RDLENGTH: 2 bytes
        int rdLength = UtilService.readUint16(bytes, offset);
        offset += 2;

        // RDATA
        String rData = parseRData(bytes, offset, rdLength, rType);

        return ResourceRecord.builder()
                .rName(rName)
                .rType(rType)
                .rClass(rClass)
                .ttl(ttl)
                .rdLength(rdLength)
                .rData(rData)
                .build();
    }

    private static String parseRData(byte[] bytes, int offset, int length, Type type) {
        switch (type) {
            case A:
                // Record A: IPv4 address (4 bytes)
                if (length == 4) {
                    return UtilService.formatIPv4(bytes, offset);
                }
                break;
            case NS:
                // Record NS: domain name
                return UtilService.parseName(bytes, offset);
            case CNAME:
                // Record CNAME: domain name
                return UtilService.parseName(bytes, offset);
            case MX:
                // Record MX: 2 bytes preference + domain name
                if (length >= 2) {
                    int preference = UtilService.readUint16(bytes, offset);
                    String exchange = UtilService.parseName(bytes, offset + 2);
                    return preference + " " + exchange;
                }
                break;
            case TXT:
                // Record TXT: one or more character-strings
                StringBuilder txtBuilder = new StringBuilder();
                int currentOffset = offset;
                while (currentOffset < offset + length) {
                    int strLen = bytes[currentOffset] & 0xFF;
                    if (currentOffset + 1 + strLen > offset + length) break;
                    
                    if (txtBuilder.length() > 0) txtBuilder.append(" ");
                    txtBuilder.append(new String(bytes, currentOffset + 1, strLen));
                    currentOffset += 1 + strLen;
                }
                return txtBuilder.toString();
            case AAAA:
                // Record AAAA: IPv6 address (16 bytes)
                if (length == 16) {
                    return UtilService.formatIPv6(bytes, offset);
                }
                break;
        }
        // Per tipi non gestiti o dati malformati, ritorna la rappresentazione esadecimale
        return UtilService.formatHex(bytes, offset, length);
    }

    public static ResourceRecord fromByteArray(byte[] bytes) {
        return fromByteArray(bytes, 0);
    }
}
