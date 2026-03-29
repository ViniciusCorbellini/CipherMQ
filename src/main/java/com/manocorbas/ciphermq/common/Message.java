package com.manocorbas.ciphermq.common;

public record Message(
    ActionType action, 
    String topic, 
    String content
) {}
