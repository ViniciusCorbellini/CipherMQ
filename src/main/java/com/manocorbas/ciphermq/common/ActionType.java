package com.manocorbas.ciphermq.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType implements Action {
    // pub-sub
    SUBSCRIBE("sub"),
    UNSUBSCRIBE("unsub"),
    PUBLISH("pub"),
    CREATE_TOPIC("cr_top"),
    GET_TOPICS("get_top"),

    // 3way register handshake 
    BROKER_CERTIFICATE("brk_crt"),
    REGISTER("reg"),
    CONNECT("con"),

    SERVER_HELLO("srv_hll"),
    CLIENT_READY("clt_rdy"),

    // KMS <-> broker
    CHECK_SUBSCRIPTION("chk_sub"),
    SUBSCRIPTION_RESULT("sub_res"),

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
    public static Action ofValue(String val) {
        for (ActionType a : ActionType.values()) {
            if (a.val.equals(val)) return a;
        }
        throw new IllegalArgumentException("Unknown action: " + val);
    }
}