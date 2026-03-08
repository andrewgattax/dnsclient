package it.andrewfly.dnsclient.model;

import it.andrewfly.dnsclient.DNSSerializable;
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
        byte[] nameBytes = encodeName(rName);
        int totalLength = nameBytes.length + 10 + rdLength; // name + 2+2+4+2 + data

        byte[] bytes = new byte[totalLength];
        int offset = 0;

        // RNAME: label encoding
        System.arraycopy(nameBytes, 0, bytes, offset, nameBytes.length);
        offset += nameBytes.length;

        // RTYPE: 2 bytes
        bytes[offset++] = (byte) ((rType.getValue() >> 8) & 0xFF);
        bytes[offset++] = (byte) (rType.getValue() & 0xFF);

        // RCLASS: 2 bytes
        bytes[offset++] = (byte) ((rClass.getValue() >> 8) & 0xFF);
        bytes[offset++] = (byte) (rClass.getValue() & 0xFF);

        // TTL: 4 bytes
        bytes[offset++] = (byte) ((ttl >> 24) & 0xFF);
        bytes[offset++] = (byte) ((ttl >> 16) & 0xFF);
        bytes[offset++] = (byte) ((ttl >> 8) & 0xFF);
        bytes[offset++] = (byte) (ttl & 0xFF);

        // RDLENGTH: 2 bytes
        bytes[offset++] = (byte) ((rdLength >> 8) & 0xFF);
        bytes[offset++] = (byte) (rdLength & 0xFF);

        // RDATA
        byte[] dataBytes = encodeRData();
        System.arraycopy(dataBytes, 0, bytes, offset, dataBytes.length);

        return bytes;
    }

    private byte[] encodeName(String domain) {
        String[] labels = domain.split("\\.");
        byte[] encoded = new byte[domain.length() + 2];
        int offset = 0;

        for (String label : labels) {
            byte[] labelBytes = label.getBytes();
            encoded[offset++] = (byte) labelBytes.length;
            System.arraycopy(labelBytes, 0, encoded, offset, labelBytes.length);
            offset += labelBytes.length;
        }

        encoded[offset] = 0;
        return java.util.Arrays.copyOf(encoded, offset + 1);
    }

    private byte[] encodeRData() {
        // Per ora gestiamo solo IP address per record A
        // Se è un record A, rData è un IP in formato "xxx.xxx.xxx.xxx"
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
        String rName = parseName(bytes, offset);

        // Calcola quanti byte abbiamo consumato per il nome
        offset = skipName(bytes, offset);

        // RTYPE: 2 bytes
        int rTypeValue = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        Type rType = Type.fromValue(rTypeValue);
        offset += 2;

        // RCLASS: 2 bytes
        int rClassValue = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        DNSClass rClass = DNSClass.fromValue(rClassValue);
        offset += 2;

        // TTL: 4 bytes
        int ttl = ((bytes[offset] & 0xFF) << 24) |
                  ((bytes[offset + 1] & 0xFF) << 16) |
                  ((bytes[offset + 2] & 0xFF) << 8) |
                  (bytes[offset + 3] & 0xFF);
        offset += 4;

        // RDLENGTH: 2 bytes
        int rdLength = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
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

    private static String parseName(byte[] bytes, int offset) {
        StringBuilder nameBuilder = new StringBuilder();
        boolean jumped = false;
        int currentOffset = offset;

        while (true) {
            int length = bytes[currentOffset] & 0xFF;

            // Check for compression pointer
            if ((length & 0xC0) == 0xC0) {
                // È un puntatore! Leggi l'offset a cui puntare
                int pointerOffset = ((length & 0x3F) << 8) | (bytes[currentOffset + 1] & 0xFF);

                if (!jumped) {
                    jumped = true;
                }

                // Salta all'offset puntato
                currentOffset = pointerOffset;
                continue;
            }

            currentOffset++;

            if (length == 0) {
                break; // Fine del nome
            }

            if (!nameBuilder.isEmpty()) {
                nameBuilder.append('.');
            }

            String label = new String(bytes, currentOffset, length);
            nameBuilder.append(label);
            currentOffset += length;
        }

        return nameBuilder.toString();
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

    private static String parseRData(byte[] bytes, int offset, int length, Type type) {
        switch (type) {
            case A:
                // Record A: IPv4 address (4 bytes)
                if (length == 4) {
                    return String.format("%d.%d.%d.%d",
                            bytes[offset] & 0xFF,
                            bytes[offset + 1] & 0xFF,
                            bytes[offset + 2] & 0xFF,
                            bytes[offset + 3] & 0xFF);
                }
                break;
            case NS:
                // Record NS: domain name
                return parseName(bytes, offset);
            case CNAME:
                // Record CNAME: domain name
                return parseName(bytes, offset);
            case MX:
                // Record MX: 2 bytes preference + domain name
                if (length >= 2) {
                    int preference = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
                    String exchange = parseName(bytes, offset + 2);
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
                    StringBuilder ipv6Builder = new StringBuilder();
                    for (int i = 0; i < 16; i += 2) {
                        if (i > 0) ipv6Builder.append(":");
                        int value = ((bytes[offset + i] & 0xFF) << 8) | (bytes[offset + i + 1] & 0xFF);
                        ipv6Builder.append(String.format("%04x", value));
                    }
                    return ipv6Builder.toString();
                }
                break;
        }
        // Per tipi non gestiti o dati malformati, ritorna la rappresentazione esadecimale
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) hex.append(" ");
            hex.append(String.format("%02X", bytes[offset + i] & 0xFF));
        }
        return hex.toString();
    }

    public static ResourceRecord fromByteArray(byte[] bytes) {
        return fromByteArray(bytes, 0);
    }
}
