package com.manocorbas.ciphermq.common;

import java.time.LocalDateTime;

public record Message(
    ActionType action,
    String topic,
    String content,
    String sender,
    LocalDateTime time
) {

    public Message(ActionType action, String topic, String content) {
        this(action, topic, content, null, null);
    }
    
}
