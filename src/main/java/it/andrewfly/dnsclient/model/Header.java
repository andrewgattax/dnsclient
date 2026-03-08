package it.andrewfly.dnsclient.model;

import it.andrewfly.dnsclient.DNSSerializable;
import it.andrewfly.dnsclient.service.UtilService;
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
        UtilService.writeUint16(bytes, offset, id);
        offset += 2;

        // Flags: 2 bytes
        byte[] flagsBytes = flags.toByteArray();
        bytes[offset++] = flagsBytes[0];
        bytes[offset++] = flagsBytes[1];

        // QD Count: 2 bytes
        UtilService.writeUint16(bytes, offset, qdCount);
        offset += 2;

        // AN Count: 2 bytes
        UtilService.writeUint16(bytes, offset, anCount);
        offset += 2;

        // NS Count: 2 bytes
        UtilService.writeUint16(bytes, offset, nsCount);
        offset += 2;

        // AR Count: 2 bytes
        UtilService.writeUint16(bytes, offset, arCount);
        offset += 2;

        return bytes;
    }

    public static Header fromByteArray(byte[] bytes) {
        if (bytes.length < 12) {
            throw new IllegalArgumentException("Header array must be at least 12 bytes");
        }

        int offset = 0;

        // ID: 2 bytes (big-endian)
        int id = UtilService.readUint16(bytes, offset);
        offset += 2;

        // Flags: 2 bytes
        Flags flags = Flags.fromByteArray(new byte[]{bytes[offset], bytes[offset + 1]});
        offset += 2;

        // QD Count: 2 bytes (big-endian)
        int qdCount = UtilService.readUint16(bytes, offset);
        offset += 2;

        // AN Count: 2 bytes (big-endian)
        int anCount = UtilService.readUint16(bytes, offset);
        offset += 2;

        // NS Count: 2 bytes (big-endian)
        int nsCount = UtilService.readUint16(bytes, offset);
        offset += 2;

        // AR Count: 2 bytes (big-endian)
        int arCount = UtilService.readUint16(bytes, offset);

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
