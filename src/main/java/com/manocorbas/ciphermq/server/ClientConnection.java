package com.manocorbas.ciphermq.server;

import com.manocorbas.ciphermq.common.Message;

public interface ClientConnection {
    void send(Message message);
    String getClientId();
}
