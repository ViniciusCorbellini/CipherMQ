package com.manocorbas.ciphermq.protocols.handshake;

import java.security.PublicKey;

public record HandshakeResult(
    String clientName,
    String sessionId,
    boolean success,
    PublicKey extractedPublicKey
) {

    public HandshakeResult(String clientName, String sessionId, boolean success) {
        this(clientName, sessionId, success, null);
    }

}
