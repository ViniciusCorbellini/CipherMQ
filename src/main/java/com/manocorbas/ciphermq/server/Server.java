package com.manocorbas.ciphermq.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException{

        // =========== Dep Inj ==============
        TopicManager tm = new TopicManager();
        BrokerService bs = new BrokerService(tm);
        // ==================================

        ServerSocket ss = new ServerSocket(port);

        boolean running = true;

        while (running) {
            Socket client = ss.accept();

            new Thread(
                new ClientHandler(
                    client,
                    bs
                )
            ).start();
            
        }

        ss.close();
    }

}
