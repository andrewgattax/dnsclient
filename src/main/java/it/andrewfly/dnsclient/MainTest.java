package it.andrewfly.dnsclient;

import it.andrewfly.dnsclient.model.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainTest {
    private static String server = "1.1.1.1";

    static void main(String[] args) throws Exception {
        // 1. Costruisci la query
        DNSMessage query = DNSMessage.buildQuery("google.com", Type.NS);
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

        DNSMessage response = DNSMessage.fromByteArray(recvPacket.getData());

        System.out.println(response);

    }

}
