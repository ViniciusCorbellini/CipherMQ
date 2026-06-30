package com.manocorbas.ciphermq.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.manocorbas.ciphermq.kms.KmsAction;

public class ActionDeserializer extends JsonDeserializer<Action> {

    @Override
    public Action deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        String value = p.getValueAsString();

        for (ActionType a : ActionType.values()) {
            if (a.getVal().equals(value)) return a;
        }

        for (KmsAction a : KmsAction.values()) {
            if (a.getVal().equals(value)) return a;
        }

        throw new IllegalArgumentException("Unknown action: " + value);
    }
}