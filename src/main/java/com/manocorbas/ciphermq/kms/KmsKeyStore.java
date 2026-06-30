package com.manocorbas.ciphermq.kms;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manocorbas.ciphermq.util.CipherUtil;
import com.manocorbas.ciphermq.util.PathUtil;
import com.manocorbas.ciphermq.util.log.Log;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armazena e persiste as chaves AES por tópico.
 *
 * Formato em disco (keystore.json):
 * { "topicName": "base64AesKey", ... }
 */
public class KmsKeyStore {

    private static final String COMPONENT = "KMSKEYSTORE";
    private static final Path   STORE_PATH = PathUtil.KMS_KEYSTORE;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // topic -> SecretKey AES-256
    private final Map<String, SecretKey> keys = new ConcurrentHashMap<>();

    public KmsKeyStore() throws IOException {
        load();
    }

    /**
     * Cria uma nova chave AES-256 para o tópico (se ainda não existir)
     * e persiste em disco.
     */
    public SecretKey initTopic(String topic) throws Exception {
        if (keys.containsKey(topic)) {
            Log.info(COMPONENT, "Topic already has a key: " + topic);
            return keys.get(topic);
        }

        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey key = kg.generateKey();

        keys.put(topic, key);
        persist();

        Log.info(COMPONENT, "Generated key for topic: " + topic);
        return key;
    }

    /** Retorna a chave do tópico, ou null se não existir. */
    public SecretKey getKey(String topic) {
        return keys.get(topic);
    }

    public boolean hasTopic(String topic) {
        return keys.containsKey(topic);
    }

    private void load() throws IOException {
        if (!Files.exists(STORE_PATH)) {
            Log.info(COMPONENT, "No keystore found, starting empty.");
            return;
        }

        String json = Files.readString(STORE_PATH);
        Map<String, String> raw = MAPPER.readValue(json, new TypeReference<>() {});

        for (Map.Entry<String, String> entry : raw.entrySet()) {
            keys.put(entry.getKey(), CipherUtil.deserializeSecretKey(entry.getValue()));
        }

        Log.info(COMPONENT, "Loaded " + keys.size() + " topic keys from disk.");
    }

    private synchronized void persist() throws IOException {
        Files.createDirectories(STORE_PATH.getParent());

        Map<String, String> raw = new HashMap<>();
        for (Map.Entry<String, SecretKey> entry : keys.entrySet()) {
            raw.put(entry.getKey(), CipherUtil.serializeSecretKey(entry.getValue()));
        }

        Files.writeString(STORE_PATH, MAPPER.writeValueAsString(raw));
    }
}