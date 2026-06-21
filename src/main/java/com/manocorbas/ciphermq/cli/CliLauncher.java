package com.manocorbas.ciphermq.cli;

import com.manocorbas.ciphermq.util.log.Log;

public class CliLauncher {
    public static void launch(String[] args) {

        try {
            ParsedCommand cmd = CommandParser.parse(args);

            switch (cmd.getMode()) {
                case SERVER -> ServerCli.start(cmd.getPort(), cmd.getPath());
                case CLIENT -> ClientCli.start(cmd.getHost(), cmd.getPort(), cmd.getPath());
                case SIGN   -> SignCli.start(cmd.getUsername());
            }

        } catch (Exception e) {
            Log.error("CLILAUNCHER", e.getMessage(), e);
            printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("""
            Usage:
                srv --port <port>
                cli --connect <host>:<port>
                sign --username <username>
        """);

    }
}