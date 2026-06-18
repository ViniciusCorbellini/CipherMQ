package com.manocorbas.ciphermq.protocols.handshake;

public record ServerHelloPayload(
    String sessionId,
    String certSerialized  // null in CONNECT, signed cert in REGISTER
) {}