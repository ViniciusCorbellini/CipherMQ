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

        throw new IllegalArgumentException("Invalid Mode: " + mode);
    }

    private static ParsedCommand parseServer(String[] args) {
        int port = 8080; // Default server port

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
            }
        }

        return ParsedCommand.server(port);
    }

    private static ParsedCommand parseClient(String[] args) {
        String host = "localhost"; // Default Host
        int port = 8080;           // Default Port

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--connect") && i + 1 < args.length) {
                String[] parts = args[i + 1].split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            }
        }

        return ParsedCommand.client(host, port);
    }

    private static ParsedCommand parseSign(String[] args) {
        return ParsedCommand.sign();
    }
}
