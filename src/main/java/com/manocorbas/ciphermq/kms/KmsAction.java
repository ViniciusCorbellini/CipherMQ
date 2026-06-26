package com.manocorbas.ciphermq.kms;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.manocorbas.ciphermq.common.Action;

public enum KmsAction implements Action{
    // client -> KMS
    INIT_TOPIC("init_top"),    // publisher notifica que criou tópico; KMS gera a chave
    GET_KEY("get_key"),        // cliente pede a chave de um tópico

    // KMS -> client
    KEY_RESPONSE("key_res"),   // KMS entrega chave envelopada
    ERROR("err"),

    // handshake KMS (reutiliza o mesmo padrão do broker)
    KMS_CERTIFICATE("kms_crt"),
    KMS_HELLO("kms_hll"),
    KMS_READY("kms_rdy"),
    KMS_REGISTER("kms_reg"),
    KMS_CONNECT("kms_con");

    private final String val;

    KmsAction(String val) { this.val = val; }

    @JsonValue
    public String getVal() { return val; }

    @JsonCreator
    public static KmsAction ofValue(String val) {
        for (KmsAction a : KmsAction.values()) {
            if (a.val.equals(val)) return a;
        }
        throw new IllegalArgumentException("Unknown KMS action: " + val);
    }
}