package com.manocorbas.ciphermq.client;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.exceptions.HandShakeException;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.util.log.Log;

public class Client {
    
    // Client
    private String username;
    private String sessionId;

    // Net
    private final ClientConnection connection = new ClientConnection();

    // log
    private final String COMPONENT = "CLIENT";

    public void connect(ConnectRequest c) throws HandShakeException {

        HandshakeResult result = connection.connect(c);

        this.username = result.clientName();
        this.sessionId = result.sessionId();
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
    
    public void close() {
        connection.close();
    }
}
