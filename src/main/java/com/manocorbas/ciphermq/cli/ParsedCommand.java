package com.manocorbas.ciphermq.cli;

public class ParsedCommand {

    public enum Mode {
        SERVER,
        CLIENT,
        SIGN
    }

    private Mode mode;
    private int port;
    private String host;
    private String username;
    private String path;

    public static ParsedCommand server(int port, String brokerCertPath) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.SERVER;
        cmd.port = port;
        cmd.path = brokerCertPath;
        return cmd;
    }

    public static ParsedCommand client(String host, int port, String caCertPath) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.CLIENT;
        cmd.host = host;
        cmd.port = port;
        cmd.path = caCertPath;
        return cmd;
    }

    public static ParsedCommand sign(String username) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.SIGN;
        cmd.username = username;
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

}
