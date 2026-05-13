package com.manocorbas.ciphermq.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;

import com.manocorbas.ciphermq.server.registry.ClientRegistry;
import com.manocorbas.ciphermq.util.log.Log;

public class Server {

    private int port;
    private String COMPONENT = "SERVER";

    public Server(int port) {
        this.port = port;
    }

    public void start() {

        boolean running = true;

        // =========== Dep Inj ==============
        KeyPair acPair = null;
        try {
            acPair = CaSetup.initAC();
        } catch (Exception e) {
            Log.error(COMPONENT, "Instantiating CA", e);
        } 
        PublicKey acPublicKey = acPair.getPublic();

        TopicManager tm = new TopicManager();
        ClientRegistry cr = new ClientRegistry();

        BrokerService bs = new BrokerService(tm, cr);
        // ==================================

        ServerSocket ss = null;

        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            Log.error(COMPONENT, "Error starting ServerSocket", e);
        }

        Log.info(COMPONENT, "Server started on -> " + ss.getInetAddress().getHostAddress() + ":" + ss.getLocalPort());

        while (running) {
            try {
                Socket client = ss.accept();

                Log.info(COMPONENT,
                        "Client accepted -> " + client.getInetAddress().getHostAddress() + ":" + client.getLocalPort());

                new Thread(
                        new ClientHandler(
                                client,
                                bs,
                                acPublicKey
                        )
                ).start();
            } catch (Exception e) {
                Log.error(COMPONENT, "Error while accepting client", e);
            }
        }

        try {
            ss.close();
        } catch (IOException e) {
            Log.error(COMPONENT, "Error while closing server", e);
        }
    }
}
