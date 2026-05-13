package com.manocorbas.ciphermq.cli;

public class CliLauncher {
    public static void launch(String[] args) {

        try {
            ParsedCommand cmd = CommandParser.parse(args);

            switch (cmd.getMode()) {
                case SERVER -> ServerCli.start(cmd.getPort());
                case CLIENT -> ClientCli.start(cmd.getHost(), cmd.getPort());
                case SIGN   -> SignCli.start();
            }

        } catch (Exception e) {
            System.out.println("Error " + e.getMessage());
            printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("""
            Usage:
                srv --port <port>
                cli --connect <host>:<port>
        """);

    }
}