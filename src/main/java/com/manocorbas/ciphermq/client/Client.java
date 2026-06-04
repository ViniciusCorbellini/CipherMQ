package com.manocorbas.ciphermq.client;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client {

    // Client
    private String username;
    private String sessionId;
    private Queue<Message> messageQueue;
    private Set<String> subscribedIn = new HashSet<>();

    // net
    private ClientConnection connection;

    // log
    private final String COMPONENT = "CLIENT";

    public void connect(ConnectRequest c) throws Exception {
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.connection = new ClientConnection(messageQueue);

        HandshakeResult result = connection.connect(c);

        this.username = result.clientName();
        this.sessionId = result.sessionId();

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
    }

    public void unsubscribe(String topic) {
        Log.info(COMPONENT, "Unsubscribing from topic: " + topic);

        Message msg = new Message(
                ActionType.UNSUBSCRIBE,
                topic,
                null);

        connection.send(msg);
    }

    public void publish(String topic, String content) {
        Log.info(COMPONENT, "Publishing message: <" + content + "> to topic: " + topic);

        Message msg = new Message(
                ActionType.PUBLISH,
                topic,
                content);

        connection.send(msg);
    }

    public void createTopic(String topic) {
        Log.info(COMPONENT, "Creating topic: " + topic);

        Message msg = new Message(
                ActionType.CREATE_TOPIC,
                topic,
                null);

        connection.send(msg);
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
