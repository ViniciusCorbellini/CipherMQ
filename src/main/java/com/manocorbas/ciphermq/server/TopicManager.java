package com.manocorbas.ciphermq.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.util.log.Log;

public class TopicManager {

    // TODO: resposta ao cliente em casos de erro

    private Map<String, List<ClientConnection>> topics = new ConcurrentHashMap<>();

    private String COMPONENT = "TOPICMANAGER";

    public void subscribe(String topic, ClientConnection client) {
        Log.info(COMPONENT, "Subscribing client in topic: " + topic);

        topics.putIfAbsent(topic, new CopyOnWriteArrayList<>());

        List<ClientConnection> list = topics.get(topic);

        if (!list.contains(client)) {
            list.add(client);
        }

        Log.debug(COMPONENT, "Topic list: " + list.toString());
        printTopics();
    }

    public void unsubscribe(String topic, ClientConnection client) {
        Log.info(COMPONENT, "Unubscribing client in topic: " + topic);

        List<ClientConnection> list = topics.get(topic);

        if (list == null)
            return;

        list.remove(client);

        Log.debug(COMPONENT, "Topic list: " + list.toString());
        printTopics();

        if (list.isEmpty()) {
            Log.debug(COMPONENT, "Topic is now empty... removing it ");
            topics.remove(topic);
        }
    }

    public void publish(Message message, ClientConnection pub) {

        List<ClientConnection> clients = topics.get(message.topic());

        if (clients == null){
            Log.info(COMPONENT, "Invalid Topic: " + message.topic());
            return;
        }

        if(!topicContainsClient(message.topic(), pub)){
            Log.info(COMPONENT, "Client is not subscribed to topic: " + message.topic());
            return;
        }

        Log.info(COMPONENT, "Publishing message for ( " + clients.size() + " ) clients : " + message.content() + " | Topic: " + message.topic());

        for (ClientConnection client : clients) {
            client.send(message);
        }
    }

    public void createTopic(String topic, ClientConnection clientConnection) {
        Log.info(COMPONENT, "Creating topic: " + topic);
        topics.putIfAbsent(topic, new CopyOnWriteArrayList<>());

        subscribe(topic, clientConnection);
    }

    public boolean topicContainsClient(String topic, ClientConnection client){
        return this.topics.get(topic).contains(client);
    }

    public void removeClient(ClientConnection client) {
        Log.info(COMPONENT, "Removing client: " + client);
        for (Map.Entry<String, List<ClientConnection>> entry : topics.entrySet()) {
            List<ClientConnection> list = entry.getValue();

            list.removeAll(List.of(client));
        }
    }

    private void printTopics() {
        for (Map.Entry<String, List<ClientConnection>> entry : topics.entrySet()) {
            List<ClientConnection> list = entry.getValue();
            
            System.out.println("Topic: " + entry.getKey());
            list.forEach(client -> System.out.println("Client: " + ((ClientHandler) client).getClient().getInetAddress().getHostAddress()));
            System.out.println("==============");
        }
    }
}
