package com.manocorbas.ciphermq.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ActionDeserializer.class)
public interface Action {
    String getVal();
}