package com.manocorbas.ciphermq.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitário de envelopamento digital (sem TLS).
 *
 * Envelope digital:
 * - Gera chave AES-256 aleatória (DEK)
 * - Cifra o payload com AES-GCM
 * - Cifra a DEK com RSA-OAEP (chave pública do destinatário)
 * - Resultado: Base64(IV) + ":" + Base64(ciphertext+tag) + ":" +
 * Base64(encryptedDEK)
 */
public class CipherUtil {

    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final String RSA_ALGO = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int GCM_IV_LEN = 12; // 96 bits
    private static final int GCM_TAG_LEN = 128; // bits

    // ------------------------------------------------------------------ //
    // Envelope digital completo (RSA + AES-GCM) //
    // ------------------------------------------------------------------ //

    /**
     * Cifra {@code plaintext} para o destinatário com chave pública
     * {@code recipientPub}.
     * 
     * @return token "IV:ciphertext:encryptedDEK" (todos em Base64)
     */
    public static String sealEnvelope(String plaintext, PublicKey recipientPub) throws Exception {
        // 1. Gera DEK aleatória
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey dek = kg.generateKey();

        // 2. Cifra payload com AES-GCM
        byte[] iv = new byte[GCM_IV_LEN];
        new SecureRandom().nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance(AES_ALGO);
        aesCipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG_LEN, iv));
        byte[] ciphertext = aesCipher.doFinal(plaintext.getBytes("UTF-8"));

        // 3. Cifra DEK com RSA-OAEP
        Cipher rsaCipher = Cipher.getInstance(RSA_ALGO);
        rsaCipher.init(Cipher.ENCRYPT_MODE, recipientPub);
        byte[] encryptedDek = rsaCipher.doFinal(dek.getEncoded());

        // 4. Monta token
        String b64Iv = Base64.getEncoder().encodeToString(iv);
        String b64Ciphertext = Base64.getEncoder().encodeToString(ciphertext);
        String b64Dek = Base64.getEncoder().encodeToString(encryptedDek);

        return b64Iv + ":" + b64Ciphertext + ":" + b64Dek;
    }

    /**
     * Decifra um token produzido por {@link #sealEnvelope} usando a chave privada
     * {@code recipientPriv}.
     */
    public static String openEnvelope(String token, PrivateKey recipientPriv) throws Exception {
        String[] parts = token.split(":", 3);
        if (parts.length != 3)
            throw new IllegalArgumentException("Token de envelope inválido");

        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[1]);
        byte[] encryptedDek = Base64.getDecoder().decode(parts[2]);

        // 1. Decifra DEK com RSA
        Cipher rsaCipher = Cipher.getInstance(RSA_ALGO);
        rsaCipher.init(Cipher.DECRYPT_MODE, recipientPriv);
        byte[] dekBytes = rsaCipher.doFinal(encryptedDek);
        SecretKey dek = new SecretKeySpec(dekBytes, "AES");

        // 2. Decifra payload com AES-GCM
        Cipher aesCipher = Cipher.getInstance(AES_ALGO);
        aesCipher.init(Cipher.DECRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG_LEN, iv));
        byte[] plaintext = aesCipher.doFinal(ciphertext);

        return new String(plaintext, "UTF-8");
    }

    // ------------------------------------------------------------------ //
    // AES-GCM com chave de sessão já estabelecida (cliente <-> broker) //
    // ------------------------------------------------------------------ //

    /**
     * Cifra {@code plaintext} com a chave de sessão AES já negociada.
     * 
     * @return "IV:ciphertext" em Base64
     */
    public static String encryptWithSessionKey(String plaintext, SecretKey sessionKey) throws Exception {
        byte[] iv = new byte[GCM_IV_LEN];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, new GCMParameterSpec(GCM_TAG_LEN, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(iv) + ":"
                + Base64.getEncoder().encodeToString(ciphertext);
    }

    /**
     * Decifra um token produzido por {@link #encryptWithSessionKey}.
     */
    public static String decryptWithSessionKey(String token, SecretKey sessionKey) throws Exception {
        String[] parts = token.split(":", 2);
        if (parts.length != 2)
            throw new IllegalArgumentException("Token de sessão inválido");

        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, sessionKey, new GCMParameterSpec(GCM_TAG_LEN, iv));
        return new String(cipher.doFinal(ciphertext), "UTF-8");
    }

    // ------------------------------------------------------------------ //
    // Serialização de SecretKey //
    // ------------------------------------------------------------------ //

    /** Serializa SecretKey AES para Base64 */
    public static String serializeSecretKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** Desserializa SecretKey AES a partir de Base64 */
    public static SecretKey deserializeSecretKey(String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64);
        return new SecretKeySpec(bytes, "AES");
    }

    /**
     * Deriva AES-256 a partir do sessionId via SHA-256.
     * ClientConnection. e ClientHandler devem consumir esse mesmo método
     * 
     * @throws NoSuchAlgorithmException 
     * @throws UnsupportedEncodingException 
     */
    public static SecretKey deriveSessionKey(String sessionId) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(sessionId.getBytes("UTF-8"));
        return new SecretKeySpec(keyBytes, "AES");
    }
}