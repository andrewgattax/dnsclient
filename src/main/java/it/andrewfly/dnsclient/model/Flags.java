package it.andrewfly.dnsclient.model;

import it.andrewfly.dnsclient.DNSSerializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class Flags implements DNSSerializable {
    private boolean qr; // query 0 / response 1
    private int opCode; // 4 bits, tipo di query, 0 per query standard
    private boolean aa; // authoritative answer, se il resolver è anche il proprietario autoritativo del dominio
    private boolean tc; // se la response è troncata (oltre 512B)
    private boolean rd; // da settare nella query se vogliamo ricorsione automatica
    private boolean ra; // ci arriva nella response se abbiamo ricorsione disponibile
    private int z; // 3 bit completamente inutili
    private int rCode; // 4 bits, codici errore response: 0 OK, 1 FORMAT ERROR, 2 SERVER FAILURE, 3 NAME ERROR, 5 REFUSED

    @Override
    public byte[] toByteArray() {
        int firstByte = 0;
        int secondByte = 0;

        // Primo byte: QR (bit 7), Opcode (bits 6-3), AA (bit 2), TC (bit 1), RD (bit 0)
        firstByte |= (qr ? 1 : 0) << 7;
        firstByte |= (opCode & 0xF) << 3;
        firstByte |= (aa ? 1 : 0) << 2;
        firstByte |= (tc ? 1 : 0) << 1;
        firstByte |= (rd ? 1 : 0);

        // Secondo byte: RA (bit 7), Z (bits 6-4), RCode (bits 3-0)
        secondByte |= (ra ? 1 : 0) << 7;
        secondByte |= (z & 0x7) << 4;
        secondByte |= rCode & 0xF;

        return new byte[] { (byte) firstByte, (byte) secondByte };
    }

    public static Flags fromByteArray(byte[] bytes) {
        if (bytes.length != 2) {
            throw new IllegalArgumentException("Flags array must be 2 bytes");
        }

        int firstByte = bytes[0] & 0xFF;  // Convert to unsigned
        int secondByte = bytes[1] & 0xFF;

        return Flags.builder()
                .qr(((firstByte >> 7) & 1) == 1)
                .opCode((firstByte >> 3) & 0xF)
                .aa(((firstByte >> 2) & 1) == 1)
                .tc(((firstByte >> 1) & 1) == 1)
                .rd((firstByte & 1) == 1)
                .ra(((secondByte >> 7) & 1) == 1)
                .z((secondByte >> 4) & 0x7)
                .rCode(secondByte & 0xF)
                .build();
    }

}
