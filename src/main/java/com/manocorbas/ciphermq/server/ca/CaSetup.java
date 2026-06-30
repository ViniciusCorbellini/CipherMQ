package com.manocorbas.ciphermq.server.ca;

import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

import com.manocorbas.ciphermq.server.model.BrokerCredentials;
import com.manocorbas.ciphermq.util.KeyStorage;
import com.manocorbas.ciphermq.util.PathUtil;
import com.manocorbas.ciphermq.util.X509Util;
import com.manocorbas.ciphermq.util.log.Log;

/**
 * Obs: O broker serve como uma CA para assinar os clientes.
 * Ainda assim, os clientes devem validar o broker pela CA (professor)
 */
public class CaSetup {

    /**
     * If the CA's keys and certificate dont exist, generates ands saves them.
     * Else, just loads them.
     * 
     * @return the CA's key pair (private and public key)
     * 
     *         obs: changed from initAC() to initBroker() to avoid confusion between
     *         the teatcher's CA from the Broker's CA
     */
    public static BrokerCredentials initBroker() throws Exception {
        KeyPair brokerPair = initBrokerKeyPair();
        X509Certificate brokerCert = initBrokerCert(brokerPair.getPublic(), brokerPair.getPrivate());
        return new BrokerCredentials(brokerPair.getPublic(), brokerPair.getPrivate(), brokerCert);
    }

    private static KeyPair initBrokerKeyPair() throws Exception {
        if (Files.exists(PathUtil.BROKER_PRIV) && Files.exists(PathUtil.BROKER_PUB)) {
            Log.debug("CASETUP", "Loading existing broker key pair...");
            PrivateKey priv = KeyStorage.loadPrivateKey(PathUtil.BROKER_PRIV);
            PublicKey pub = KeyStorage.loadPublicKey(PathUtil.BROKER_PUB);
            return new KeyPair(pub, priv);
        }

        Log.info("CASETUP", "No broker keys found — generating new pair...");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        Files.createDirectories(PathUtil.BROKER_DIR);
        KeyStorage.savePrivateKey(pair.getPrivate(), PathUtil.BROKER_PRIV);
        KeyStorage.savePublicKey(pair.getPublic(), PathUtil.BROKER_PUB);
        return pair;
    }

    public static X509Certificate initBrokerCert(PublicKey brokerPub, PrivateKey brokerPriv) throws Exception {
        if (Files.exists(PathUtil.BROKER_CERT)) {
            Log.debug("CASETUP", "Loading existing broker certificate...");
            return X509Util.loadCertificate(PathUtil.BROKER_CERT);
        }

        Log.warn("CASETUP", "No broker.crt found — generating self-signed for development");
        X509Certificate cert = BrokerCertificateSetup.selfSign(brokerPub, brokerPriv);

        Files.createDirectories(PathUtil.BROKER_DIR);
        try (OutputStream os = Files.newOutputStream(PathUtil.BROKER_CERT)) {
            os.write("-----BEGIN CERTIFICATE-----\n".getBytes());
            os.write(Base64.getMimeEncoder(64, new byte[] { '\n' }).encode(cert.getEncoded()));
            os.write("\n-----END CERTIFICATE-----\n".getBytes());
        }

        Log.info("CASETUP", "Self-signed broker cert saved to " + PathUtil.BROKER_CERT);
        return cert;
    }
}
