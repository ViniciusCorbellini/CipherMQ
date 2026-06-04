package com.manocorbas.ciphermq.server;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.manocorbas.ciphermq.util.log.Log;

/**
 * Manages topics and its users
 */
public class TopicManager {

    // TODO: resposta ao cliente em casos de erro

    private Map<String, Set<String>> topics = new ConcurrentHashMap<>();

    private String COMPONENT = "TOPICMANAGER";

    public void createTopic(String topic, String clientId) {
        Log.info(COMPONENT, "Creating topic: " + topic);
        topics.computeIfAbsent(topic, t -> ConcurrentHashMap.newKeySet()).add(clientId);

        subscribe(topic, clientId);
    }

    public void subscribe(String topic, String clientId) {
        Log.info(COMPONENT, "Subscribing client in topic: " + topic);

        topics.computeIfAbsent(topic, t -> ConcurrentHashMap.newKeySet()).add(clientId);

        Log.debug(COMPONENT, "Topic list: " + topics.get(topic).toString());
        printTopics();
    }

    public void unsubscribe(String topic, String clientId) {
        Log.info(COMPONENT, "Unubscribing client in topic: " + topic);

        Set<String> list = topics.get(topic);

        if (list == null)
            return;

        list.remove(clientId);

        Log.debug(COMPONENT, "Topic list: " + topics.get(topic).toString());
        printTopics();

        if (list.isEmpty()) {
            Log.debug(COMPONENT, "Topic is now empty... removing it ");
            topics.remove(topic);
        }
    }

    public Set<String> getTopicsByClient(String clientId) {

        Set<String> subscribedTopics = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : topics.entrySet()) {

            String topic = entry.getKey();
            Set<String> subscribers = entry.getValue();

            if (subscribers.contains(clientId)) {
                subscribedTopics.add(topic);
            }
        }

        return subscribedTopics;
    }

    public Set<String> getSubscribers(String topic) {
        return topics.getOrDefault(topic, Set.of());
    }

    public boolean topicExists(String topic) {
        return topics.get(topic) != null;
    }

    public boolean topicContainsClient(String topic, String clientId) {
        return topics.get(topic).contains(clientId);
    }

    private void printTopics() {
        for (Map.Entry<String, Set<String>> entry : topics.entrySet()) {
            Set<String> list = entry.getValue();

            System.out.println("Topic: " + entry.getKey());
            list.forEach(client -> System.out.println("Client: " + client));
            System.out.println("==============");
        }
    }

}
