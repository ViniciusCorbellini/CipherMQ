package com.manocorbas.ciphermq.server.model;

import com.manocorbas.ciphermq.common.Message;

public interface ClientConnection {
    void send(Message message);
    String getClientId();
}
