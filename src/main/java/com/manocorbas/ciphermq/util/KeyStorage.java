package com.manocorbas.ciphermq.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;

public class KeyStorage {

    public static void savePrivateKey(PrivateKey key, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, Base64.getEncoder().encode(key.getEncoded()));
    }

    public static void savePublicKey(PublicKey key, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, Base64.getEncoder().encode(key.getEncoded()));
    }

    public static PrivateKey loadPrivateKey(Path path) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(Files.readAllBytes(path));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static PublicKey loadPublicKey(Path path) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(Files.readAllBytes(path));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static void saveCertificate(ClientCertificate cert, Path path) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, cert.serialize());
    }

    public static ClientCertificate loadCertificate(Path path) throws Exception {
        return ClientCertificate.deserialize(Files.readString(path));
    }
}
