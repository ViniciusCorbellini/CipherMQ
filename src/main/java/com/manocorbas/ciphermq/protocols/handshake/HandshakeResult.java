package com.manocorbas.ciphermq.protocols.handshake;

import com.manocorbas.ciphermq.common.ActionType;

public record HandshakeResult(
    String sessionId,
    boolean success,
    ActionType action // REGISTER || CONNECT
) {}
