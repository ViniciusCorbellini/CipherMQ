package com.manocorbas.ciphermq.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.util.JsonFrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

public class ClientHandler implements Runnable, ClientConnection {

    private Socket client;
    private BrokerService brokerService;

    private InputStream in;
    private OutputStream out;

    private String COMPONENT = "CLIENTHANDLER";

    public ClientHandler(Socket client, BrokerService brokerService) {
        this.client = client;
        this.brokerService = brokerService;
    }

    @Override
    public void run() {
        Log.debug(COMPONENT, "ClientHandler Running");

        try {
            in = client.getInputStream();
            out = client.getOutputStream();

            while (true) {
                Log.info(COMPONENT, "Waiting for message");

                String json = JsonFrameUtil.receive(in);

                Message msg = JsonUtil.fromJson(json, Message.class);

                brokerService.handle(msg, this);
            }

        } catch (Exception e) {
            Log.error(COMPONENT, "Client disconnected", e);
        } finally {
            Log.debug(COMPONENT, "Cleanup");
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
            Log.error(COMPONENT, "Error while sending message", e);
            throw new RuntimeException(e);
        }
    }

    private void cleanup() {
        brokerService.disconnect(this);

        try {
            client.close();
        } catch (Exception ignored) {}
    }

    public Socket getClient() {
        return client;
    }
}
