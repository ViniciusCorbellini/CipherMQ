package com.manocorbas.ciphermq.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.exceptions.HandShakeException;
import com.manocorbas.ciphermq.exceptions.UnauthorizedAccessException;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.protocols.handshake.ServerHandShake;
import com.manocorbas.ciphermq.server.registry.ClientSession;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

public class ClientHandler implements Runnable, ClientConnection {

    // TODO: error response

    // Handler
    private ClientSession session;
    private BrokerService brokerService;
    private ServerHandShake serverHandShake;

    // Net
    private Socket clientSocket;
    private InputStream in;
    private OutputStream out;

    // thread
    private volatile boolean running = true;

    // Log
    private String COMPONENT = "CLIENTHANDLER";

    public ClientHandler(Socket clientSocket, BrokerService brokerService) {
        this.clientSocket = clientSocket;
        this.brokerService = brokerService;
        this.serverHandShake = new ServerHandShake(clientSocket);
    }

    @Override
    public void run() {
        Log.debug(COMPONENT, "ClientHandler Running");

        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            session = doHandShakeAndRegistry();
            Log.debug(COMPONENT, "Session online: " + session.isOnline());

        } catch (IOException | HandShakeException e) {
            Log.error(COMPONENT, e.getMessage(), e);
            running = false;
        }

        while (running) {
            try {
                Log.info(COMPONENT, "Waiting for message");

                String json = FrameUtil.receive(in);

                Message msg = JsonUtil.fromJson(json, Message.class);

                brokerService.handle(msg, session);
            } catch (SocketException e) {
                Log.info(COMPONENT, "Connection closed: " + e.getMessage());
                running = false;

            } catch (UnauthorizedAccessException e) {
                Log.error(COMPONENT, "Client is not subscribed to this topic", e);

            } catch (Exception e) {
                Log.error(COMPONENT, "Unexpected error while handling client", e);
                running = false; // geralmente você quer derrubar conexão aqui
            }
        }

        cleanup();
    }

    private ClientSession doHandShakeAndRegistry() throws IOException, HandShakeException {
        Log.info(COMPONENT, "Handshaking");
        HandshakeResult result = serverHandShake.doHandshake();

        if (!result.success())
            throw new HandShakeException("Error while trying to do handshake");

        return brokerService.register(result.clientName(), this, result.sessionId());
    }

    @Override
    public void send(Message message) {
        try {
            String json = JsonUtil.toJson(message);

            FrameUtil.send(out, json);

        } catch (IOException e) {
            Log.error(COMPONENT, "Error while sending message", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getClientId() {
        return session.getClientId();
    }

    private void cleanup() {
        Log.debug(COMPONENT, "Cleanup");
        brokerService.disconnect(session);

        try {
            clientSocket.close();
        } catch (Exception ignored) {
        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }
}
