package com.manocorbas.ciphermq.server;

import com.manocorbas.ciphermq.common.Message;

public class BrokerService {

    private final TopicManager topicManager;

    public BrokerService(TopicManager topicManager) {
        this.topicManager = topicManager;
    }

    public void handle(Message msg, ClientConnection client) {

        switch (msg.action()) {

            case SUBSCRIBE -> {
                topicManager.subscribe(msg.topic(), client);
            }

            case UNSUBSCRIBE -> {
                topicManager.unsubscribe(msg.topic(), client);
            }

            case PUBLISH -> {
                topicManager.publish(msg);
            }

            case CREATE_TOPIC -> {
                topicManager.createTopic(msg.topic());
            }

            default -> throw new IllegalArgumentException("Unexpected value: " + msg.action());
        }
    }

    public void disconnect(ClientConnection client) {
        topicManager.removeClient(client);
    }
}
