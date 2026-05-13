package com.manocorbas.ciphermq.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.util.KeyStorage;

public class ClientSetup {

    private static final Path CLIENTS_DIR = Path.of("/data/clients");

    /**
     * Loads the clients's certificate and key pair
     * 
     * @throws IllegalStateException if they dont exist (client must be signed first)
     */
    public static ClientCredentials load(String username) throws Exception {
        Path dir = CLIENTS_DIR.resolve(username);

        Path privPath = dir.resolve("client_priv.key");
        Path pubPath = dir.resolve("client_pub.key");
        Path certPath = dir.resolve("client.cert");

        if (!Files.exists(certPath) || !Files.exists(privPath)) {
            throw new IllegalStateException(
                    "No credentials found for client '" + username + "'. " +
                            "Run set USERNAME=" + username + " && docker compose run sign on the broker first.");
        }

        PrivateKey priv = KeyStorage.loadPrivateKey(privPath);
        PublicKey pub = KeyStorage.loadPublicKey(pubPath);
        ClientCertificate cert = KeyStorage.loadCertificate(certPath);

        return new ClientCredentials(username, pub, priv, cert);
    }
}