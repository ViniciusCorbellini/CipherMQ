package com.manocorbas.ciphermq.client;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;

public class Client {

    private final ClientConnection connection = new ClientConnection();

    public void connect(String host, int port) {
        connection.connect(host, port);
    }

    public void subscribe(String topic) {
        Message msg = new Message(
                ActionType.SUBSCRIBE,
                topic,
                null);

        connection.send(msg);
    }

    public void unsubscribe(String topic) {
        Message msg = new Message(
                ActionType.UNSUBSCRIBE,
                topic,
                null);

        connection.send(msg);
    }

    public void publish(String topic, String content) {
        Message msg = new Message(
                ActionType.PUBLISH,
                topic,
                content);

        connection.send(msg);
    }

    public void createTopic(String topic) {
        Message msg = new Message(
                ActionType.CREATE_TOPIC,
                topic,
                null);

        connection.send(msg);
    }
}
