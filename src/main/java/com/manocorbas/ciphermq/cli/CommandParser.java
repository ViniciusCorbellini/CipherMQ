package com.manocorbas.ciphermq.cli;

public class CommandParser {

    public static ParsedCommand parse(String[] args) {

        if (args.length == 0) {
            throw new IllegalArgumentException("No command found");
        }

        String mode = args[0];

        if (mode.equalsIgnoreCase("srv")) {
            return parseServer(args);
        }

        if (mode.equalsIgnoreCase("cli")) {
            return parseClient(args);
        }

        if (mode.equalsIgnoreCase("sign")) {
            return parseSign(args);
        }

        if (mode.equalsIgnoreCase("kms")) {
            return parseKms(args);
        }

        throw new IllegalArgumentException("Invalid Mode: " + mode);
    }

    private static ParsedCommand parseServer(String[] args) {
        int port = 8080; // Default server port
        String brokerCertPath = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
            } else if (args[i].equals("--broker-cert") && i + 1 < args.length) {
                brokerCertPath = args[i + 1];
                i++;
            }
        }

        return ParsedCommand.server(port, brokerCertPath);
    }

    private static ParsedCommand parseClient(String[] args) {
        String host = "localhost";
        int port = 8080; // defaul broker port
        String kmsHost = "localhost";
        int kmsPort = 9090; // defaul kms port
        String caCertPath = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--connect") && i + 1 < args.length) {
                String[] parts = args[i + 1].split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
                i++;
            } else if (args[i].equals("--kms") && i + 1 < args.length) {
                String[] parts = args[i + 1].split(":");
                kmsHost = parts[0];
                kmsPort = Integer.parseInt(parts[1]);
                i++;
            } else if (args[i].equals("--ca-cert") && i + 1 < args.length) {
                caCertPath = args[i + 1];
                i++;
            }
        }

        // Ajuste a assinatura do método factory do ParsedCommand de acordo com o seu
        // projeto
        return ParsedCommand.client(host, port, kmsHost, kmsPort, caCertPath);
    }

    private static ParsedCommand parseSign(String[] args) {
        String username = "default"; // default username

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--username") && i + 1 < args.length) {
                username = args[i + 1];
            }
        }

        return ParsedCommand.sign(username);
    }

    /**
     * kms --port <kmsPort> --broker <host>:<port> [--ca-cert <path>]
     *
     * Exemplo:
     * kms --port 9090 --broker localhost:8080 --ca-cert /tmp/ca.crt
     */
    private static ParsedCommand parseKms(String[] args) {
        int kmsPort = 9090;
        String brokerHost = "localhost";
        int brokerPort = 8080;
        String caCertPath = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                kmsPort = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--broker") && i + 1 < args.length) {
                String[] parts = args[++i].split(":");
                brokerHost = parts[0];
                brokerPort = Integer.parseInt(parts[1]);
            } else if (args[i].equals("--ca-cert") && i + 1 < args.length) {
                caCertPath = args[++i];
            }
        }

        return ParsedCommand.kms(kmsPort, brokerHost, brokerPort, caCertPath);
    }
}
