package it.andrewfly.dnsclient;

import it.andrewfly.dnsclient.model.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Main {
    private static String server = "127.0.0.53";
    private static int retries = 3;
    private static int timeout = 1;
    private static boolean showHelp = false;

    public static void main(String[] args) {
        try {
            // Parse options and collect positional arguments
            java.util.List<String> positionalArgs = new java.util.ArrayList<>();
            parseArguments(args, positionalArgs);

            if (showHelp) {
                printHelp();
                System.exit(0);
            }

            // Get positional arguments
            if (positionalArgs.isEmpty()) {
                System.err.println("Error: Missing query argument");
                System.err.println("Usage: ./dnslookup [options] <query> [TYPE]");
                System.exit(1);
            }

            String query = positionalArgs.get(0);
            String typeStr = positionalArgs.size() > 1 ? positionalArgs.get(1) : "A";

            // Parse and normalize type
            Type type;
            try {
                type = Type.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Error: Unknown record type '" + typeStr + "'");
                System.err.println("Supported types: A, AAAA, MX, CNAME, NS, TXT");
                System.exit(1);
                return;
            }

            // Build and send query with retries
            DNSMessage response = sendQueryWithRetry(query, type);

            if (response == null) {
                System.err.println("Error: No response received after " + retries + " attempts");
                System.exit(2);
                return;
            }

            // Print response
            printResponse(response);
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void parseArguments(String[] args, java.util.List<String> positionalArgs) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-s":
                case "--server":
                    if (i + 1 < args.length) {
                        server = args[++i];
                    } else {
                        System.err.println("Error: Missing argument for " + args[i]);
                        System.exit(1);
                    }
                    break;
                case "-r":
                case "--retries":
                    if (i + 1 < args.length) {
                        try {
                            retries = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Error: Invalid retries value");
                            System.exit(1);
                        }
                    } else {
                        System.err.println("Error: Missing argument for " + args[i]);
                        System.exit(1);
                    }
                    break;
                case "-t":
                case "--timeout":
                    if (i + 1 < args.length) {
                        try {
                            timeout = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Error: Invalid timeout value");
                            System.exit(1);
                        }
                    } else {
                        System.err.println("Error: Missing argument for " + args[i]);
                        System.exit(1);
                    }
                    break;
                case "-h":
                case "--help":
                    showHelp = true;
                    break;
                default:
                    // If it doesn't start with "-", it's a positional argument
                    if (!args[i].startsWith("-")) {
                        positionalArgs.add(args[i]);
                    }
                    break;
            }
        }
    }

    private static void printHelp() {
        System.out.println("Usage: ./dnslookup [options] <query> [TYPE]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  query             The DNS query to solve");
        System.out.println("  TYPE              The DNS record type for the query. Supported values are");
        System.out.println("                    A, AAAA, MX, CNAME, NS, TXT (default: A)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -s <server>, --server <server>");
        System.out.println("                    The IPv4 address of the DNS resolver (default: 127.0.0.53)");
        System.out.println("  -r <retries>, --retries <retries>");
        System.out.println("                    The maximum number of retries before declaring failure (default: 3)");
        System.out.println("  -t <timeout>, --timeout <timeout>");
        System.out.println("                    The timeout for receiving the DNS reply in seconds (default: 1)");
        System.out.println("  -h, --help");
        System.out.println("                    Display this help and exit");
    }

    private static DNSMessage sendQueryWithRetry(String query, Type type) throws Exception {
        for (int attempt = 0; attempt < retries; attempt++) {
            try {
                DNSMessage dnsQuery = DNSMessage.buildQuery(query, type);
                byte[] queryBytes = dnsQuery.toByteArray();

                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(timeout * 1000);

                InetAddress resolver = InetAddress.getByName(server);
                DatagramPacket sendPacket = new DatagramPacket(queryBytes, queryBytes.length, resolver, 53);
                socket.send(sendPacket);

                byte[] buffer = new byte[512];
                DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(recvPacket);

                socket.close();

                return DNSMessage.fromByteArray(recvPacket.getData());

            } catch (java.net.SocketTimeoutException e) {
                // Retry
            }
        }
        return null;
    }

    private static void printResponse(DNSMessage response) {
        Header header = response.getHeader();

        // Print header line
        System.out.printf("QUERY: %d, ANSWER: %d, AUTHORITY: %d, ADDITIONAL: %d%n",
                header.getQdCount(),
                header.getAnCount(),
                header.getNsCount(),
                header.getArCount());
        System.out.println();

        // Print QUESTION SECTION
        if (!response.getQuestions().isEmpty()) {
            System.out.println("QUESTION SECTION:");
            for (Question q : response.getQuestions()) {
                System.out.printf("%s. IN %s%n", q.getQName(), q.getQType().name());
            }
            System.out.println();
        }

        // Print ANSWER SECTION
        if (!response.getAnswers().isEmpty()) {
            System.out.println("ANSWER SECTION:");
            for (ResourceRecord rr : response.getAnswers()) {
                printResourceRecord(rr);
            }
            System.out.println();
        }

        // Print AUTHORITY SECTION
        if (!response.getAuthorities().isEmpty()) {
            System.out.println("AUTHORITY SECTION:");
            for (ResourceRecord rr : response.getAuthorities()) {
                printResourceRecord(rr);
            }
            System.out.println();
        }

        // Print ADDITIONAL SECTION
        if (!response.getAdditionals().isEmpty()) {
            System.out.println("ADDITIONAL SECTION:");
            for (ResourceRecord rr : response.getAdditionals()) {
                printResourceRecord(rr);
            }
            System.out.println();
        }
    }

    private static void printResourceRecord(ResourceRecord rr) {
        System.out.printf("%s. %d IN %s %s%n",
                rr.getRName(),
                rr.getTtl(),
                rr.getRType().name(),
                rr.getRData());
    }
}
