package com.manocorbas.ciphermq.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType {
    // pub-sub
    SUBSCRIBE("sub"),
    UNSUBSCRIBE("unsub"),
    PUBLISH("pub"),
    CREATE_TOPIC("cr_top"),

    // 3way register handshake 
    REGISTER("reg"),
    CONNECT("con"),

    SERVER_HELLO("srv_hll"),
    CLIENT_READY("clt_rdy"),

    // general purpose
    ERROR("err");


    private final String val;

    ActionType(String val) {
        this.val = val;
    }

    @JsonValue    
    public String getVal() {
        return val;
    }

    @JsonCreator
    public static ActionType ofValue(String val) {
        for (ActionType a : ActionType.values()) {
            if (a.val.equals(val)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknow action: " + val);
    }
}