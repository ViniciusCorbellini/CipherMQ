package com.manocorbas.ciphermq.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Broker {

    private int port;

    public Broker(int port) {
        this.port = port;
    }

    public void start() throws IOException{

        // =========== Dep Inj ==============
        TopicManager tm = new TopicManager();
        // ==================================

        ServerSocket ss = new ServerSocket(port);

        boolean running = true;

        while (running) {
            Socket client = ss.accept();

            new Thread(
                new ClientHandler(
                    client,
                    tm
                )
            ).start();
            
        }

        ss.close();
    }

}
