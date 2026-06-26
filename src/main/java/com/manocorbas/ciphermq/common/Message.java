package com.manocorbas.ciphermq.common;

import java.time.LocalDateTime;

public record Message(
    Action action,
    String topic,
    String content,
    String sender,
    LocalDateTime time
) {

    public Message(Action action, String topic, String content) {
        this(action, topic, content, null, null);
    }
    
}
