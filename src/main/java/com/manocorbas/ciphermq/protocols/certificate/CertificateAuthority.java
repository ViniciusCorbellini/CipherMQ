package com.manocorbas.ciphermq.protocols.certificate;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class CertificateAuthority {

    /**
     * Gera o par de chaves da AC (salva em ac_pub.key e ac_priv.key)
     */
    public static KeyPair generateACKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /**
     * Signs a client's public key and id with the CA's private key
     * and returns the certificate
     * 
     * "certificate" = { clientId, clientPublicKey, signature(clientId +
     * clientPublicKey) }
     */
    public static ClientCertificate signClient(
            String clientId,
            PublicKey clientPubKey,
            PrivateKey acPrivKey) throws Exception {

        byte[] payload = buildPayload(clientId, clientPubKey);

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(acPrivKey);
        signer.update(payload);
        byte[] signature = signer.sign();

        return new ClientCertificate(clientId, clientPubKey, signature);
    }

    /**
     * Verifies if a cert was signed by the CA
     */
    public static boolean verifyCertificate(
            ClientCertificate cert,
            PublicKey acPubKey) throws Exception {

        byte[] payload = buildPayload(cert.clientId(), cert.clientPublicKey());

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(acPubKey);
        verifier.update(payload);
        return verifier.verify(cert.signature());
    }

    // payload = clientId bytes + encoded public key bytes
    private static byte[] buildPayload(String clientId, PublicKey pubKey) {
        byte[] idBytes = clientId.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = pubKey.getEncoded();

        byte[] payload = new byte[idBytes.length + keyBytes.length];

        System.arraycopy(idBytes, 0, payload, 0, idBytes.length);
        System.arraycopy(keyBytes, 0, payload, idBytes.length, keyBytes.length);
        
        return payload;
    }
}
