package com.manocorbas.ciphermq.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.manocorbas.ciphermq.protocols.certificate.CertificateAuthority;
import com.manocorbas.ciphermq.util.KeyStorage;
import com.manocorbas.ciphermq.util.log.Log;

public class CaSetup {

    private static final Path AC_DIR    = Path.of("/data/ac");
    private static final Path PRIV_PATH = AC_DIR.resolve("ac_priv.key");
    private static final Path PUB_PATH  = AC_DIR.resolve("ac_pub.key");

    /**
     * If the CA's keys dont exist, generates ands saves them.
     * Else, just loads them. 
     * 
     * @return the CA's key pair (private and public key)
     */
    public static KeyPair initAC() throws Exception {
        if (Files.exists(PRIV_PATH) && Files.exists(PUB_PATH)) {
            Log.debug("CA", "Loading existing AC keys...");
            PrivateKey priv = KeyStorage.loadPrivateKey(PRIV_PATH);
            PublicKey  pub  = KeyStorage.loadPublicKey(PUB_PATH);
            return new KeyPair(pub, priv);
        }

        Log.debug("CA", "No AC keys found — generating new pair...");
        KeyPair pair = CertificateAuthority.generateACKeyPair();
        KeyStorage.savePrivateKey(pair.getPrivate(), PRIV_PATH);
        KeyStorage.savePublicKey(pair.getPublic(),  PUB_PATH);
        Log.debug("CA", "AC keys saved to " + AC_DIR);
        return pair;
    }
}
