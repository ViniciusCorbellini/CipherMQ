package com.manocorbas.ciphermq.server;

import java.time.LocalDateTime;
import java.util.Set;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.exceptions.NonExistentTopicException;
import com.manocorbas.ciphermq.exceptions.UnauthorizedAccessException;
import com.manocorbas.ciphermq.server.registry.ClientRegistry;
import com.manocorbas.ciphermq.server.registry.ClientSession;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

public class BrokerService {

    private final TopicManager topicManager;
    private final ClientRegistry clientRegistry;

    private String COMPONENT = "BROKERSERVICE";

    public BrokerService(TopicManager topicManager, ClientRegistry clientRegistry) {
        this.topicManager = topicManager;
        this.clientRegistry = clientRegistry;
    }

    public void handle(Message msg, ClientSession client)
            throws UnauthorizedAccessException, NonExistentTopicException {

        Log.debug(COMPONENT, "Handling message | action: " + msg.action());

        switch (msg.action()) {

            case SUBSCRIBE -> {
                topicManager.subscribe(msg.topic(), client.getClientId());
            }

            case UNSUBSCRIBE -> {
                topicManager.unsubscribe(msg.topic(), client.getClientId());
            }

            case PUBLISH -> {
                publish(msg, client);
            }

            case CREATE_TOPIC -> {
                topicManager.createTopic(msg.topic(), client.getClientId());
            }

            case GET_TOPICS -> {
                sendTopics(client);
            }

            default -> {
                Log.debug(COMPONENT, "Unexpected value read");
                throw new IllegalArgumentException("Unexpected value: " + msg.action());
            }
        }
    }

    public void disconnect(ClientSession session) {
        Log.debug(COMPONENT, "Detaching Sesssion");
        session.detachConnection();
    }

    private void publish(Message message, ClientSession session)
            throws UnauthorizedAccessException, NonExistentTopicException {

        String topic = message.topic();

        if (!topicManager.topicExists(topic)) {
            throw new NonExistentTopicException(topic + " does not exist");
        }

        if (!topicManager.topicContainsClient(topic, session.getClientId())) {
            throw new UnauthorizedAccessException(session.getClientId() + " does not belong in topic: " + topic);
        }

        Set<String> subscribers = topicManager.getSubscribers(message.topic());

        Log.debug(COMPONENT, "Enqueueing message to " + subscribers.size() + " users (including publisher)");
        message = new Message(message.action(), topic, message.content(), session.getClientId(), LocalDateTime.now());

        for (String clientId : subscribers) {
            ClientSession cs = clientRegistry.getOrCreate(clientId);

            cs.enqueue(message);

            if (cs.isOnline()) {
                cs.flushQueue();
            }
        }
    }

    private void sendTopics(ClientSession client) {
        Set<String> topics = topicManager.getTopicsByClient(client.getClientId());

        String body = JsonUtil.toJson(topics);

        Message m = new Message(ActionType.GET_TOPICS, null, body);

        client.enqueue(m);
        if (client.isOnline()) {
            client.flushQueue();
        }
    }

    public ClientSession register(String clientName, ClientConnection connection, String sessionId) {
        Log.info(COMPONENT, "Registering client: " + clientName);
        ClientSession session = clientRegistry.getOrCreate(clientName);

        Log.debug(COMPONENT, "Attaching client: " + clientName + " | SessionId: " + sessionId);
        session.attachConnection(connection, sessionId);

        return session;
    }
}
