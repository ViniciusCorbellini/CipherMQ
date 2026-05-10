package com.manocorbas.ciphermq.protocols.handshake;

public record HandshakeResult(
    String clientName,
    String sessionId,
    boolean success
) {}
