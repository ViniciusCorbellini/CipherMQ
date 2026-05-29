package com.manocorbas.ciphermq.client;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client {

    // Client
    private String username;
    private String sessionId;
    private Queue<Message> messageQueue;
    private Set<String> subscribedIn;

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

        this.subscribedIn = fetchTopics();
        this.connection.startListening();
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

    public Set<String> fetchTopics() throws Exception {
        // envia GET_TOPICS ao broker
        connection.send(new Message(ActionType.GET_TOPICS, null, null));

        // aguarda a resposta diretamente (bloqueante, antes de iniciar o dispatcher)
        Message response = connection.waitForMessage(ActionType.GET_TOPICS);

        Set<String> topics = JsonUtil.fromJson(response.content(), Set.class);

        return topics;
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

}
