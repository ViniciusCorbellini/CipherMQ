package com.manocorbas.ciphermq.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.util.JsonFrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

// Parte de rede do cliente
// TODO: GUI
public class ClientConnection {

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    private String COMPONENT = "CLIENTCONNECTION";

    public void connect(String host, int port) {
        try {            
            
            socket = new Socket(host, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            
            Log.info(COMPONENT, "Client " + socket.getRemoteSocketAddress());
            startListening();

        } catch (Exception e) {
            throw new RuntimeException("Error atempting to connect", e);
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {

                while (true) {
                    String json = JsonFrameUtil.receive(in);

                    Message msg = JsonUtil.fromJson(json, Message.class);

                    printMessage(msg);
                }

            } catch (Exception e) {
                System.out.println("Connection Closed");
            }
        }).start();
    }

    public void send(Message message) {
        try {
            String json = JsonUtil.toJson(message);
            JsonFrameUtil.send(out, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printMessage(Message m){
        System.out.println("======= Message =======");
        System.out.println("Topic: " + m.topic() + " | Action: " + m.action());
        System.out.println("Content: ");
        System.out.println("-----------------------");
        System.out.println(m.content());
        System.out.println("-----------------------");
        System.out.println("========= End =========");
    }

}
