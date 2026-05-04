package com.manocorbas.ciphermq.server;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.util.log.Log;

public class BrokerService {

    private final TopicManager topicManager;

    private String COMPONENT = "BROKERSERVICE";

    public BrokerService(TopicManager topicManager) {
        this.topicManager = topicManager;
    }

    public void handle(Message msg, ClientConnection client) {

        Log.debug(COMPONENT, "Handling message | action: " + msg.action());

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

            default -> {
                Log.debug(COMPONENT, "Unexpected value read");
                throw new IllegalArgumentException("Unexpected value: " + msg.action());
            }
        }
    }

    public void disconnect(ClientConnection client) {
        Log.debug(COMPONENT, "Removing client from topics");
        topicManager.removeClient(client);
    }
}
