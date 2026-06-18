package com.manocorbas.ciphermq.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.util.KeyStorage;
import com.manocorbas.ciphermq.util.PathUtil;
import com.manocorbas.ciphermq.util.log.Log;

public class ClientSetup {

    private static final Path CLIENTS_DIR = PathUtil.CLIENTS_DIR;

    /**
     * Loads the clients's certificate and key pair.0
     * Creates a keypair if the certificate doesn't exist
     */
    public static ClientCredentials load(String username) throws Exception {

        Path certPath = PathUtil.clientsDir(username).resolve("client.cert");
        Path privPath = PathUtil.clientsDir(username).resolve("client_priv.key");
        Path pubPath = PathUtil.clientsDir(username).resolve("client_pub.key");

        if (Files.exists(certPath) && Files.exists(privPath)) {
            // client is already registered
            PrivateKey priv = KeyStorage.loadPrivateKey(privPath);
            PublicKey pub = KeyStorage.loadPublicKey(pubPath);
            ClientCertificate cert = KeyStorage.loadCertificate(certPath);
            return new ClientCredentials(username, pub, priv, cert);
        }

        // first access: generates key pair, cert will be received from broker
        Log.info("CLIENTSETUP",
                "No credentials found for '" + username + "' - generating key pair for registration...");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        // Saves the keys, returns a null certificate (broker will sign it)
        KeyStorage.savePrivateKey(pair.getPrivate(), privPath);
        KeyStorage.savePublicKey(pair.getPublic(), pubPath);

        return new ClientCredentials(username, pair.getPublic(), pair.getPrivate(), null);
    }

    /**
     * will be called after the broker signs the cert
     */
    public static void saveCertificate(String username, ClientCertificate cert) throws Exception {
        Path certPath = PathUtil.clientsDir(username).resolve("client.cert");
        KeyStorage.saveCertificate(cert, certPath);
    }

}