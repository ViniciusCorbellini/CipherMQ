package com.manocorbas.ciphermq.server;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import com.manocorbas.ciphermq.server.model.BrokerCredentials;
import com.manocorbas.ciphermq.util.KeyStorage;
import com.manocorbas.ciphermq.util.PathUtil;
import com.manocorbas.ciphermq.util.X509Util;

/**
 * Obs: O broker serve como uma CA para assinar os clientes.
 * Ainda assim, os clientes devem validar o broker pela CA (professor)
 */
public class CaSetup {

    private static final Path AC_DIR = PathUtil.AC_DIR;
    private static final Path PRIV_PATH = AC_DIR.resolve("ac_priv.key");
    private static final Path PUB_PATH = AC_DIR.resolve("ac_pub.key");

    /**
     * If the CA's keys dont exist, generates ands saves them.
     * Else, just loads them.
     * 
     * @return the CA's key pair (private and public key)
     * 
     *         obs: changed from initAC() to initBroker() to avoid confusion between
     *         the teatcher's CA from the Broker's CA
     */
    public static BrokerCredentials initBroker() throws Exception {
        PrivateKey priv = KeyStorage.loadPrivateKey(PathUtil.AC_PRIV);
        PublicKey pub = KeyStorage.loadPublicKey(PathUtil.AC_PUB);

        X509Certificate brokerCert = X509Util.loadCertificate(PathUtil.BROKER_CERT);

        return new BrokerCredentials(pub, priv, brokerCert);
    }
}
