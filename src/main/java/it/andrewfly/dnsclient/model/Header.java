package it.andrewfly.dnsclient.model;

import it.andrewfly.dnsclient.DNSSerializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Header implements DNSSerializable {
    private int id; // 2B id per tracking della response
    private Flags flags; // 2B fra sono flags literally
    private int qdCount; // 2B numero di entries nella question section
    private int anCount; // 2B numero di entries nella response
    private int nsCount; // 2B numero di nameserver autoritativi nella response
    private int arCount; // 2B numero di informazioni extra utili

    @Override
    public byte[] toByteArray() {
        byte[] bytes = new byte[12];
        int offset = 0;

        // ID: 2 bytes
        bytes[offset++] = (byte) ((id >> 8) & 0xFF);
        bytes[offset++] = (byte) (id & 0xFF);

        // Flags: 2 bytes
        byte[] flagsBytes = flags.toByteArray();
        bytes[offset++] = flagsBytes[0];
        bytes[offset++] = flagsBytes[1];

        // QD Count: 2 bytes
        bytes[offset++] = (byte) ((qdCount >> 8) & 0xFF);
        bytes[offset++] = (byte) (qdCount & 0xFF);

        // AN Count: 2 bytes
        bytes[offset++] = (byte) ((anCount >> 8) & 0xFF);
        bytes[offset++] = (byte) (anCount & 0xFF);

        // NS Count: 2 bytes
        bytes[offset++] = (byte) ((nsCount >> 8) & 0xFF);
        bytes[offset++] = (byte) (nsCount & 0xFF);

        // AR Count: 2 bytes
        bytes[offset++] = (byte) ((arCount >> 8) & 0xFF);
        bytes[offset++] = (byte) (arCount & 0xFF);

        return bytes;
    }

    public static Header fromByteArray(byte[] bytes) {
        if (bytes.length < 12) {
            throw new IllegalArgumentException("Header array must be at least 12 bytes");
        }

        int offset = 0;

        // ID: 2 bytes (big-endian)
        int id = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        offset += 2;

        // Flags: 2 bytes
        Flags flags = Flags.fromByteArray(new byte[]{bytes[offset], bytes[offset + 1]});
        offset += 2;

        // QD Count: 2 bytes (big-endian)
        int qdCount = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        offset += 2;

        // AN Count: 2 bytes (big-endian)
        int anCount = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        offset += 2;

        // NS Count: 2 bytes (big-endian)
        int nsCount = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        offset += 2;

        // AR Count: 2 bytes (big-endian)
        int arCount = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);

        return Header.builder()
                .id(id)
                .flags(flags)
                .qdCount(qdCount)
                .anCount(anCount)
                .nsCount(nsCount)
                .arCount(arCount)
                .build();
    }
}
