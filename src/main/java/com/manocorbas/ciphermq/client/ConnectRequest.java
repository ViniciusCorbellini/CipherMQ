package com.manocorbas.ciphermq.client;

public record ConnectRequest(
        String host,
        int port,
        ClientCredentials credentials
) {}
