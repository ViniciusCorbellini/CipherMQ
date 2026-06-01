package com.manocorbas.ciphermq.common;

public record Message(
    ActionType action,
    String topic,
    String content,
    String sender
) {

    public Message(ActionType action, String topic, String content) {
        this(action, topic, content, null);
    }
    
}
