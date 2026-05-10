package com.manocorbas.ciphermq.server.registry;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.server.ClientConnection;

public class ClientSession {

    // Client
    private final String clientId;
    private final Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    
    // Session
    private volatile ClientConnection connection;
    private Status status;
    private String sessionId;
    // private long expiresAt; TODO

    public ClientSession(String clientId) {
        this.clientId = clientId;
    }

    public void enqueue(Message msg) {
        pendingMessages.add(msg);
    }

    public void attachConnection(ClientConnection connection, String sessionId) {
        this.status = Status.ONLINE;
        this.sessionId = sessionId;
        this.connection = connection;
        flushQueue();
    }

    public void detachConnection() {
        this.status = Status.OFFLINE;
        this.sessionId = null;
        this.connection = null;
    }

    public void flushQueue() {
        Message msg;
        while ((msg = pendingMessages.poll()) != null) {
            connection.send(msg);
        }
    }

    public boolean isOnline() {
        return status == Status.ONLINE;
    }

    public String getClientId() {
        return clientId;
    }
}