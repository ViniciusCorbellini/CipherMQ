package com.manocorbas.ciphermq.client;

import java.io.IOException;
import java.net.Socket;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.exceptions.HandShakeException;
import com.manocorbas.ciphermq.protocols.handshake.ClientHandShake;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;
import java.util.Queue;

// Parte de rede do cliente
public class ClientConnection {

    // TODO: ping pong to see if server is healthy (heartbeat)

    // Net
    private Socket socket;
    private ClientHandShake clientHandShake;

    // threads
    private Thread listenThread;
    private volatile boolean running = true;
    private Queue<Message> messageQueue;

    private String COMPONENT = "CLIENTCONNECTION";

    public ClientConnection(Queue<Message> messageQueue) {
        this.messageQueue = messageQueue;
    }

    public HandshakeResult connect(ConnectRequest c) {
        try {
            socket = new Socket(c.host(), c.port());
            Log.info(COMPONENT, "Client connected to broker: " + socket.getRemoteSocketAddress());

            Log.info(COMPONENT, "Atempting to handshake");
            clientHandShake = new ClientHandShake(socket);

            HandshakeResult result = clientHandShake.doHandshake(c.credentials());

            if (!result.success()) {
                Log.warn(COMPONENT, "Unsuccessful Handshake");
                throw new HandShakeException("Unsuccessful Handshake");
            }

            return result;

        } catch (Exception e) {
            Log.error(COMPONENT, "Error atempting to connect", e);
            throw new RuntimeException("Error atempting to connect", e);
        }
    }

    public void startListening() {

        Log.info(COMPONENT, "Started to listen");

        this.listenThread = new Thread(() -> {
            try {

                while (running) {
                    String json = FrameUtil.receive(socket.getInputStream());

                    Message msg = JsonUtil.fromJson(json, Message.class);

                    messageQueue.add(msg);
                }

            } catch (Exception e) {
                if (running) {
                    Log.error(COMPONENT, "Error while listening", e);
                } else {
                    Log.info(COMPONENT, "ListenThread interrupted");
                }
            }
        });

        listenThread.start();
    }

    public void send(Message message) {

        // Log.debug(COMPONENT, "Sending message: " + message.content());

        try {
            String json = JsonUtil.toJson(message);
            FrameUtil.send(socket.getOutputStream(), json);
        } catch (IOException e) {
            Log.warn(COMPONENT, "Error while sending message");
            e.printStackTrace();
        }
    }

    public void close() {

        Log.info(COMPONENT, "Closing Connection");

        try {
            running = false;
            socket.close();
        } catch (IOException ignored) {}
    }
}
