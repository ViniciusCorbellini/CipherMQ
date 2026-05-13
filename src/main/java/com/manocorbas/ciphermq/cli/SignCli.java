package com.manocorbas.ciphermq.cli;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import com.manocorbas.ciphermq.protocols.certificate.CertificateAuthority;
import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.server.CaSetup;
import com.manocorbas.ciphermq.util.KeyStorage;
import com.manocorbas.ciphermq.util.log.Log;

public class SignCli {

    private static final Path CLIENTS_DIR = Path.of("/data/clients");

    public static void start() throws Exception {
        String COMPONENT = "SIGNCLI";

        Log.debug(COMPONENT, "SignCli Started");

        String username = System.getenv("SIGN_USERNAME").strip();
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("SIGN_USERNAME env var not set");
        }

        // Loads or generates the CA's key pair
        KeyPair acPair = CaSetup.initAC();

        Path clientDir = CLIENTS_DIR.resolve(username);

        // Loads or generates the client's key pair
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair clientPair = gen.generateKeyPair();

        // signs the certificate with the CA private key
        ClientCertificate cert = CertificateAuthority.signClient(
                username,
                clientPair.getPublic(),
                acPair.getPrivate());

        // saves those in the client dir
        KeyStorage.savePrivateKey(clientPair.getPrivate(), clientDir.resolve("client_priv.key"));
        KeyStorage.savePublicKey(clientPair.getPublic(), clientDir.resolve("client_pub.key"));
        KeyStorage.saveCertificate(cert, clientDir.resolve("client.cert"));

        Log.info(COMPONENT, "Credentials generated for '" + username + "' at " + clientDir);
    }
}
