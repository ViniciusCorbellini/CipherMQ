package com.manocorbas.ciphermq.client;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.SecretKey;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.kms.KmsAction;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.util.CipherUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

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
        
        HandshakeResult result = connection.connect(c);
        
        this.username = result.clientName();
        this.sessionId = result.sessionId();
        
        // Gambiarra da porra kkkkkkk
        this.creds = ClientSetup.load(c.credentials().username());
        
        SecretKey secretKey = CipherUtil.deriveSessionKey(sessionId);
        this.connection.setSessionKey(secretKey);
        
        this.kmsConnection = new KmsClientConnection(c.kmsHost(), c.kmsPort(), creds);
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

        boolean received = false;

        while (System.currentTimeMillis() < deadline) {
            Message msg = messageQueue.poll();

            if (msg == null) {
                Thread.sleep(50);
                continue;
            }

            // If we get a GET_TOPICS message, we update the subscribedIn set
            if (msg.action() == ActionType.GET_TOPICS) {
                this.subscribedIn = JsonUtil.fromJson(msg.content(), HashSet.class);
                received = true;
                break;
            }

            // else, we enqueue it back so the dispatcher can proccess it
            messageQueue.add(msg);
        }

        if (!received) {
            throw new Exception("Timeout waiting for GET_TOPICS response");
        }

        // fetching topics keys
        Log.info(COMPONENT,
                "Recovering missing E2E topic keys from KMS for " + this.subscribedIn.size() + " topics...");

        for (String topic : this.subscribedIn) {

            if (topicKeys.containsKey(topic)) {
                continue;
            }

            try {
                notifyKms(KmsAction.GET_KEY, topic);
            }

            catch (Exception e) {
                Log.error(COMPONENT,
                        "Failed to recover key from KMS for topic [" + topic + "]: " + e.getMessage(), e);
            }
        }
    }

    public Message decryptE2E(Message msg) {

        if (msg.action() != ActionType.PUBLISH)
            return msg;

        String topic = msg.topic();
        SecretKey topicKey = topicKeys.get(topic);

        if (topicKey != null) {
            try {

                String decryptedContent = CipherUtil.decryptWithSessionKey(msg.content(), topicKey);

                return new Message(msg.action(), topic, decryptedContent, msg.sender(), msg.time());

            } catch (Exception e) {
                Log.error(COMPONENT, "Failed to decrypt E2E payload for topic: " + topic, e);
                return new Message(msg.action(), topic, "[ERRO DE CRIPTOGRAFIA: Não foi possível decifrar]",
                        msg.sender(), msg.time());
            }
        }

        Log.warn(COMPONENT, "Received PUBLISH for topic [" + topic + "] but local key is missing.");
        
        return new Message(msg.action(), topic, "[ERRO: Chave E2E ausente para este tópico]", msg.sender(),
                msg.time());

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
