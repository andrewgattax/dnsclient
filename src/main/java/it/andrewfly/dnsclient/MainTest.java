package it.andrewfly.dnsclient;

import it.andrewfly.dnsclient.model.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainTest {
    private static String server = "1.1.1.1";

    static void main(String[] args) throws Exception {
        // 1. Costruisci la query
        DNSMessage query = DNSMessage.buildQuery("andrewfly.it", Type.TXT);
        byte[] queryBytes = query.toByteArray();

        // 2. Apri socket UDP
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(1000);

        // 3. Manda la query
        InetAddress resolver = InetAddress.getByName(server);
        DatagramPacket sendPacket = new DatagramPacket(queryBytes, queryBytes.length, resolver, 53);
        socket.send(sendPacket);

        // 4. Ricevi la response
        byte[] buffer = new byte[512];
        DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(recvPacket);

        // Stampa il buffer in hex
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < recvPacket.getLength(); i++) {
            hexString.append(String.format("%02X ", buffer[i]));
        }
        System.out.println(hexString.toString().trim());

        DNSMessage response = DNSMessage.fromByteArray(recvPacket.getData());

        System.out.println(response);

    }

}
