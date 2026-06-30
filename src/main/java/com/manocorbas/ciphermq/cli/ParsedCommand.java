package com.manocorbas.ciphermq.cli;

// TODO: make this a record
public class ParsedCommand {

    public enum Mode {
        SERVER,
        CLIENT,
        SIGN,
        KMS
    }

    private Mode mode;
    private int port;
    private int brokerPort;
    private String host;
    private String kmsHost;
    private int kmsPort;
    private String username;
    private String path;

    public static ParsedCommand server(int port, String brokerCertPath) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.SERVER;
        cmd.port = port;
        cmd.path = brokerCertPath;
        return cmd;
    }

    public static ParsedCommand client(String host, int port, String kmsHost, int kmsPort, String caCertPath) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.CLIENT;
        cmd.host = host;
        cmd.port = port;
        cmd.kmsHost = kmsHost;
        cmd.kmsPort = kmsPort;
        cmd.path = caCertPath;
        return cmd;
    }

    public static ParsedCommand sign(String username) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.SIGN;
        cmd.username = username;
        return cmd;
    }

    public static ParsedCommand kms(int kmsPort, String brokerHost, int brokerPort, String caCertPath) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.KMS;
        cmd.port = kmsPort;
        cmd.host = brokerHost;
        cmd.brokerPort = brokerPort;
        cmd.path = caCertPath;
        return cmd;
    }

    public Mode getMode() {
        return mode;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPath() {
        return path;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public String getKmsHost() {
        return kmsHost;
    }

    public int getKmsPort() {
        return kmsPort;
    }

}
