package com.manocorbas.ciphermq.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.manocorbas.ciphermq.common.Message;

public class TopicManager {
    // TODO: logs

    Map<String,  List<ClientConnection>> topics = new ConcurrentHashMap<>();

    public void subscribe(String topic, ClientConnection client){
        topics.putIfAbsent(topic, new CopyOnWriteArrayList<>());

        List<ClientConnection> list = topics.get(topic);

        if (!list.contains(client)){
            list.add(client);
        }
    }

    public void unsubscribe(String topic, ClientConnection client){
        List<ClientConnection> list = topics.get(topic);

        if(list == null) return;

        list.remove(client);

        if(list.isEmpty()){
            topics.remove(topic);
        }
    }


    public void publish(Message message) {
        List<ClientConnection> clients = topics.get(message.topic());

        if (clients == null) return;

        for (ClientConnection client : clients) {
            client.send(message);
        }
    }

    public void createTopic(String topic) {
        topics.putIfAbsent(topic, new CopyOnWriteArrayList<>());
    }

    public void removeClient(ClientConnection client) {
        for (Map.Entry<String, List<ClientConnection>> entry : topics.entrySet()) {
            List<ClientConnection> list = entry.getValue();

            list.removeAll(List.of(client));
        }
    }
}
