package com.manocorbas.ciphermq.util;

import java.nio.file.Path;

public class PathUtil {
    private static final Path BASE = Path.of(System.getProperty("user.home"), ".ciphermq");

    // Broker
    public static final Path BROKER_DIR = BASE.resolve("broker");
    public static final Path BROKER_PRIV = BROKER_DIR.resolve("broker_priv.key");
    public static final Path BROKER_PUB = BROKER_DIR.resolve("broker_pub.key");
    public static final Path BROKER_CERT = BROKER_DIR.resolve("broker.crt"); // assinado pelo professor

    // CA (teatcher)
    public static final Path CA_CERT = BASE.resolve("ca.crt"); // cert do professor, colocado manualmente

    // Client
    public static final Path CLIENTS_DIR = BASE.resolve("clients");

    // KMS
    public static final Path KMS_DIR = CLIENTS_DIR;
    public static final Path KMS_PRIV = KMS_DIR.resolve("kms_priv.key");
    public static final Path KMS_PUB = KMS_DIR.resolve("kms_pub.key");
    public static final Path KMS_KEYSTORE = KMS_DIR.resolve("keystore.json");

    public static Path clientsDir(String username) {
        return CLIENTS_DIR.resolve(username);
    }
}