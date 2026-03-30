package com.manocorbas.ciphermq.server;

import java.net.Socket;

import com.manocorbas.ciphermq.common.Message;

public class ClientHandler implements Runnable, ClientConnection{

    private Socket client;
    private TopicManager tm;

    public ClientHandler(Socket client, TopicManager tm) {
        this.client = client;
        this.tm = tm;
    }

    @Override
    public void run() {
        //TODO
    }

    @Override
    public void send(Message message){
        //TODO
    }
    
}
