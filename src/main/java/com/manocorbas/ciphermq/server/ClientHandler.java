package com.manocorbas.ciphermq.server;

import java.net.Socket;

public class ClientHandler implements Runnable{

    private Socket client;
    private TopicManager tm;

    public ClientHandler(Socket client, TopicManager tm) {
        this.client = client;
        this.tm = tm;
    }

    @Override
    public void run() {
        
    }
    
}
