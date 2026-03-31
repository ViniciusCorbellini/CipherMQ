package com.manocorbas.ciphermq.cli;

public class ParsedCommand {

    public enum Mode {
        SERVER,
        CLIENT
    }

    private Mode mode;
    private int port;
    private String host;

    public static ParsedCommand server(int port) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.SERVER;
        cmd.port = port;
        return cmd;
    }

    public static ParsedCommand client(String host, int port) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.mode = Mode.CLIENT;
        cmd.host = host;
        cmd.port = port;
        return cmd;
    }

    public Mode getMode() { return mode; }
    public int getPort() { return port; }
    public String getHost() { return host; }
    
}
