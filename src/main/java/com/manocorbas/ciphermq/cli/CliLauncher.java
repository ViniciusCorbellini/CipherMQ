package com.manocorbas.ciphermq.cli;

import com.manocorbas.ciphermq.util.log.Log;

public class CliLauncher {
    public static void launch(String[] args) {

        try {
            ParsedCommand cmd = CommandParser.parse(args);

            switch (cmd.getMode()) {
                case SERVER -> ServerCli.start(cmd.getPort(), cmd.getPath());
                case CLIENT -> ClientCli.start(cmd.getHost(), cmd.getPort(), cmd.getKmsHost(), cmd.getKmsPort(), cmd.getPath());
                case SIGN   -> SignCli.start(cmd.getUsername());
                case KMS -> KmsCli.start(cmd.getPort(), cmd.getHost(), cmd.getBrokerPort(), cmd.getPath());
            }

        } catch (Exception e) {
            Log.error("CLILAUNCHER", e.getMessage(), e);
            printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("""
            Usage:
                srv --port <port> --broker-cert <path/to/broker.crt>
                cli --connect <host>:<port> --kms <kms-host>:<kms-port> --ca-cert <path/to/ca.crt>
                kms --port <kmsPort> --broker <host>:<port> [--ca-cert <path>]
                sign --username <username>
        """);

    }
}