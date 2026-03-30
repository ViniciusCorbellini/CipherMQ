package com.manocorbas.ciphermq.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.util.JsonFrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;

public class ClientHandler implements Runnable, ClientConnection {

    private Socket client;
    private BrokerService brokerService;

    private InputStream in;
    private OutputStream out;

    public ClientHandler(Socket client, BrokerService brokerService) {
        this.client = client;
        this.brokerService = brokerService;
    }

    @Override
    public void run() {
        try {
            in = client.getInputStream();
            out = client.getOutputStream();

            while (true) {
                String json = JsonFrameUtil.receive(in);

                Message msg = JsonUtil.fromJson(json, Message.class);

                brokerService.handle(msg, this);
            }

        } catch (Exception e) {
            System.out.println("Cliente desconectado");
        } finally {
            cleanup();
        }
    }

    @Override
    public void send(Message message) {
        try {
            out = client.getOutputStream();

            String json = JsonUtil.toJson(message);
            JsonFrameUtil.send(out, json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void cleanup() {
        brokerService.disconnect(this);

        try {
            client.close();
        } catch (Exception ignored) {
        }
    }
}
