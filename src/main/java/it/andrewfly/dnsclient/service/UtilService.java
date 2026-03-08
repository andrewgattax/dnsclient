package it.andrewfly.dnsclient.service;

import it.andrewfly.dnsclient.model.DNSClass;
import it.andrewfly.dnsclient.model.Type;

public class UtilService {

    // DNS name encoding with label format
    public static byte[] encodeName(String domain) {
        if (domain == null || domain.isEmpty()) {
            return new byte[]{0};
        }
        
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

    // DNS name parsing with compression pointer support
    public static String parseName(byte[] bytes, int offset) {
        StringBuilder nameBuilder = new StringBuilder();
        boolean jumped = false;
        int currentOffset = offset;

        while (true) {
            int length = bytes[currentOffset] & 0xFF;

            // Check for compression pointer
            if ((length & 0xC0) == 0xC0) {
                // It's a pointer! Read the offset to jump to
                int pointerOffset = ((length & 0x3F) << 8) | (bytes[currentOffset + 1] & 0xFF);

                if (!jumped) {
                    jumped = true;
                }

                // Jump to the pointed offset
                currentOffset = pointerOffset;
                continue;
            }

            currentOffset++;

            if (length == 0) {
                break; // End of name
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

    // Skip over a DNS name (used to calculate lengths)
    public static int skipName(byte[] bytes, int offset) {
        int currentOffset = offset;

        while (true) {
            int length = bytes[currentOffset] & 0xFF;

            // Check for compression pointer
            if ((length & 0xC0) == 0xC0) {
                // It's a pointer, consume 2 bytes and terminate
                return currentOffset + 2;
            }

            currentOffset++;

            if (length == 0) {
                break; // End of name
            }

            currentOffset += length;
        }

        return currentOffset;
    }

    // Read unsigned 16-bit integer in big-endian format
    public static int readUint16(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    // Write unsigned 16-bit integer in big-endian format
    public static void writeUint16(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) ((value >> 8) & 0xFF);
        bytes[offset + 1] = (byte) (value & 0xFF);
    }

    // Read unsigned 32-bit integer in big-endian format
    public static int readUint32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
               ((bytes[offset + 1] & 0xFF) << 16) |
               ((bytes[offset + 2] & 0xFF) << 8) |
               (bytes[offset + 3] & 0xFF);
    }

    // Write unsigned 32-bit integer in big-endian format
    public static void writeUint32(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) ((value >> 24) & 0xFF);
        bytes[offset + 1] = (byte) ((value >> 16) & 0xFF);
        bytes[offset + 2] = (byte) ((value >> 8) & 0xFF);
        bytes[offset + 3] = (byte) (value & 0xFF);
    }

    // Format IPv4 address from 4 bytes starting at offset
    public static String formatIPv4(byte[] bytes, int offset) {
        return String.format("%d.%d.%d.%d",
                bytes[offset] & 0xFF,
                bytes[offset + 1] & 0xFF,
                bytes[offset + 2] & 0xFF,
                bytes[offset + 3] & 0xFF);
    }

    // Format IPv6 address from 16 bytes starting at offset
    public static String formatIPv6(byte[] bytes, int offset) {
        StringBuilder ipv6Builder = new StringBuilder();
        for (int i = 0; i < 16; i += 2) {
            if (i > 0) ipv6Builder.append(":");
            int value = ((bytes[offset + i] & 0xFF) << 8) | (bytes[offset + i + 1] & 0xFF);
            ipv6Builder.append(String.format("%04x", value));
        }
        return ipv6Builder.toString();
    }

    // Format bytes as hex string
    public static String formatHex(byte[] bytes, int offset, int length) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) hex.append(" ");
            hex.append(String.format("%02X", bytes[offset + i] & 0xFF));
        }
        return hex.toString();
    }

    // Encode RDATA based on record type
    public static byte[] encodeRData(String rData, Type rType) {
        if (rData == null || rData.isEmpty()) {
            return new byte[0];
        }

        switch (rType) {
            case A:
                // IPv4 address: "192.168.1.1" → 4 bytes
                String[] parts = rData.split("\\.");
                if (parts.length == 4) {
                    byte[] ip = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        ip[i] = (byte) Integer.parseInt(parts[i]);
                    }
                    return ip;
                }
                return new byte[0];

            case AAAA:
                // IPv6 address: "2001:db8::1" → 16 bytes
                String[] ipv6Parts = rData.split(":");
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                for (String part : ipv6Parts) {
                    if (part.isEmpty()) {
                        // Handle :: compression - pad with zeros
                        int zeros = 8 - ipv6Parts.length + 1;
                        for (int i = 0; i < zeros; i++) {
                            writeUint16ToStream(baos, 0);
                        }
                    } else {
                        int value = Integer.parseInt(part, 16);
                        writeUint16ToStream(baos, value);
                    }
                }
                // If we still don't have 16 bytes, pad at the end
                while (baos.size() < 16) {
                    writeUint16ToStream(baos, 0);
                }
                return baos.toByteArray();

            case MX:
                // MX: "10 smtp.google.com" → 2 bytes preference + encoded domain name
                String[] mxParts = rData.split(" ", 2);
                if (mxParts.length == 2) {
                    int preference = Integer.parseInt(mxParts[0]);
                    byte[] exchangeName = encodeName(mxParts[1]);
                    byte[] mxData = new byte[2 + exchangeName.length];
                    writeUint16(mxData, 0, preference);
                    System.arraycopy(exchangeName, 0, mxData, 2, exchangeName.length);
                    return mxData;
                }
                return new byte[0];

            case NS:
            case CNAME:
                // NS and CNAME: domain name → encoded domain name
                return encodeName(rData);

            case TXT:
                // TXT: text string → length-prefixed string(s)
                java.io.ByteArrayOutputStream txtBaos = new java.io.ByteArrayOutputStream();
                // Split by spaces and encode each part as a character-string
                String[] txtParts = rData.split(" ");
                for (String part : txtParts) {
                    byte[] partBytes = part.getBytes();
                    txtBaos.write((byte) partBytes.length);
                    txtBaos.write(partBytes, 0, partBytes.length);
                }
                return txtBaos.toByteArray();

            default:
                // For unknown types, return raw bytes
                return rData.getBytes();
        }
    }

    // Helper method to write uint16 to ByteArrayOutputStream
    private static void writeUint16ToStream(java.io.ByteArrayOutputStream baos, int value) {
        baos.write((value >> 8) & 0xFF);
        baos.write(value & 0xFF);
    }
}
