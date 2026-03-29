package com.manocorbas.ciphermq.server;

import java.util.List;
import java.util.Map;

public class TopicManager {
    Map<String,  List<ClientHandler>> subscribers;
    

    void subscribe(String topic, ClientHandler client){}
    void unsubscribe(String topic, ClientHandler client){}
    void publish(String topic, String message){}
}
