package com.manocorbas.ciphermq.client;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.kms.KmsAction;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.util.CipherUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

import java.security.KeyStore.Entry;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

public class Client {

    // Client
    private String username;
    private String sessionId;
    private Queue<Message> messageQueue;
    private Set<String> subscribedIn = new HashSet<>();
    private ClientCredentials creds;

    // net
    private ClientConnection connection;
    private KmsClientConnection kmsConnection;

    // cripto
    private final Map<String, SecretKey> topicKeys = new ConcurrentHashMap<>();

    // log
    private final String COMPONENT = "CLIENT";

    public void connect(ConnectRequest c) throws Exception {
        this.creds = c.credentials();
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.connection = new ClientConnection(messageQueue, topicKeys);
        this.kmsConnection = new KmsClientConnection(c.kmsHost(), c.kmsPort(), creds);

        HandshakeResult result = connection.connect(c);

        this.username = result.clientName();
        this.sessionId = result.sessionId();

        SecretKey secretKey = CipherUtil.deriveSessionKey(sessionId);
        this.connection.setSessionKey(secretKey);

        this.connection.startListening();
        fetchTopics();
    }

    public void subscribe(String topic) {
        Log.info(COMPONENT, "Subscribing to topic: " + topic);

        Message msg = new Message(
                ActionType.SUBSCRIBE,
                topic,
                null);

        connection.send(msg);
        notifyKms(KmsAction.GET_KEY, topic);
    }

    public void unsubscribe(String topic) {
        Log.info(COMPONENT, "Unsubscribing from topic: " + topic);

        Message msg = new Message(
                ActionType.UNSUBSCRIBE,
                topic,
                null);

        connection.send(msg);
    }

    public void publish(String topic, String content) throws Exception {
        Log.info(COMPONENT, "Publishing message: <" + content + "> to topic: " + topic);

        String encrypted = CipherUtil.encryptWithSessionKey(content, topicKeys.get(topic));

        Message msg = new Message(
                ActionType.PUBLISH,
                topic,
                encrypted);

        connection.send(msg);
    }

    public void createTopic(String topic) {
        Log.info(COMPONENT, "Creating topic: " + topic);

        Message msg = new Message(
                ActionType.CREATE_TOPIC,
                topic,
                null);

        connection.send(msg);
        notifyKms(KmsAction.INIT_TOPIC, topic);
    }

    public void notifyKms(KmsAction action, String topic) {
        try {
            // Pequena pausa ou aguardar confirmação do broker pode ser necessária
            // dependendo do timing
            Thread.sleep(100);

            String keyEnvelope = kmsConnection.requestKeyFromKms(action, topic);

            String secretKeyB64 = CipherUtil.openEnvelope(keyEnvelope, creds.privateKey());
            SecretKey secretKey = CipherUtil.deserializeSecretKey(secretKeyB64);

            topicKeys.put(topic, secretKey);
            Log.info(COMPONENT, "Key for topic [" + topic + "] stored locally and ready for E2E!");

        } catch (Exception e) {
            Log.error(COMPONENT, "Failed to initialize/fetch topic key in KMS: " + e.getMessage(), e);
        }
    }

    public void fetchTopics() throws Exception {
        Log.info(COMPONENT, "Fetching topics");

        connection.send(new Message(ActionType.GET_TOPICS, null, null));

        // polling in the messageQueue instead of trying to read directly from the
        // socket
        long deadline = System.currentTimeMillis() + 5000; // timeout 5s

        while (System.currentTimeMillis() < deadline) {
            Message msg = messageQueue.poll();

            if (msg == null) {
                Thread.sleep(50);
                continue;
            }

            // If we get a GET_TOPICS message, we update the subscribedIn set
            if (msg.action() == ActionType.GET_TOPICS) {
                this.subscribedIn = JsonUtil.fromJson(msg.content(), HashSet.class);
                return;
            }

            // else, we enqueue it back so the dispatcher can proccess it
            messageQueue.add(msg);
        }

        throw new Exception("Timeout waiting for GET_TOPICS response");
    }

    public void close() {
        connection.close();
    }

    public Queue<Message> getMessageQueue() {
        return messageQueue;
    }

    public Set<String> getSubscribedIn() {
        return subscribedIn;
    }

    public String getUsername() {
        return username;
    }
}
