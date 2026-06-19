package com.manocorbas.ciphermq.util;

import java.nio.file.Path;

public class PathUtil {
    private static final Path BASE = Path.of(System.getProperty("user.home"), ".ciphermq");

    public static final Path AC_DIR = BASE.resolve("ac");
    public static final Path AC_PRIV = AC_DIR.resolve("ac_priv.key");
    public static final Path AC_PUB = AC_DIR.resolve("ac_pub.key");
    public static final Path CA_CERT = AC_DIR.resolve("ca.crt"); // ca's (teacher) certificate
    public static final Path BROKER_CERT = AC_DIR.resolve("broker.crt"); // broker's certificate
    public static final Path CLIENTS_DIR = BASE.resolve("clients");

    public static Path clientsDir(String username) {
        return CLIENTS_DIR.resolve(username);
    }
}
