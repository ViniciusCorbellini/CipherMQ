package com.manocorbas.ciphermq.server;

import java.util.Set;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.exceptions.UnauthorizedAccessException;
import com.manocorbas.ciphermq.server.registry.ClientRegistry;
import com.manocorbas.ciphermq.server.registry.ClientSession;
import com.manocorbas.ciphermq.util.log.Log;

public class BrokerService {

    // TODO: LOG

    private final TopicManager topicManager;
    private final ClientRegistry clientRegistry;

    private String COMPONENT = "BROKERSERVICE";

    public BrokerService(TopicManager topicManager, ClientRegistry clientRegistry) {
        this.topicManager = topicManager;
        this.clientRegistry = clientRegistry;
    }

    public void handle(Message msg, ClientSession client) throws UnauthorizedAccessException  {

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

    private void publish(Message message, ClientSession con) throws UnauthorizedAccessException {

        String topic = message.topic();

        if (!topicManager.topicContainsClient(topic, con.getClientId())) {
            throw new UnauthorizedAccessException(con.getClientId() + "does not belong in topic: " + topic);
        }

        Set<String> subscribers = topicManager.getSubscribers(message.topic());

        for (String clientId : subscribers) {
            ClientSession session = clientRegistry.getOrCreate(clientId);

            session.enqueue(message);

            Log.debug(COMPONENT, "Session online: " + session.isOnline());
            if (session.isOnline()) {
                session.flushQueue();
            }
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
