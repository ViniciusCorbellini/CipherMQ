package com.manocorbas.ciphermq.server.registry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {
    
    private final Map<String, ClientSession> clients = new ConcurrentHashMap<>();

    public ClientSession getOrCreate(String clientName) {
        return clients.computeIfAbsent(clientName, id -> new ClientSession(clientName));
    }

    public Optional<ClientSession> get(String clientName) {
        return Optional.ofNullable(clients.get(clientName));
    }

}
