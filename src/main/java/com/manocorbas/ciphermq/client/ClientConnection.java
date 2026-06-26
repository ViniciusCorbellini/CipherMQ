package com.manocorbas.ciphermq.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;
import javax.crypto.SecretKey;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.exceptions.HandShakeException;
import com.manocorbas.ciphermq.protocols.handshake.ClientHandShake;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.util.CipherUtil;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;


// Parte de rede do cliente
public class ClientConnection {

    // Net
    private Socket socket;
    private ClientHandShake clientHandShake;

    // threads
    private Thread listenThread;
    private volatile boolean running = true;
    private Queue<Message> messageQueue;

    // cripto
    private final Map<String, SecretKey> topicKeys;
    private SecretKey sessionKey;

    private String COMPONENT = "CLIENTCONNECTION";

    public ClientConnection(Queue<Message> messageQueue, Map<String, SecretKey> topicKeys) {
        this.messageQueue = messageQueue;
        this.topicKeys = topicKeys;
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

                    if (this.sessionKey != null) {
                        // Descriptografa a camada de transporte Broker -> Cliente
                        json = CipherUtil.decryptWithSessionKey(json, this.sessionKey);
                        System.out.println("Json: " + json);
                    }

                    Message msg = JsonUtil.fromJson(json, Message.class);
                    System.out.println("Content: " + msg.content());

                    // E2E: Se for uma mensagem publicada em um tópico, descriptografa
                    if (msg.action() == ActionType.PUBLISH) {
                        String topic = msg.topic();
                        SecretKey topicKey = topicKeys.get(topic);

                        if (topicKey != null) {
                            try {
                                // Decifra usando a chave simétrica do tópico obtida do KMS
                                String decryptedContent = CipherUtil.decryptWithSessionKey(msg.content(), topicKey);

                                // Substitui o conteúdo criptografado pelo texto claro original
                                msg = new Message(msg.action(), topic, decryptedContent);

                            } catch (Exception cryptoEx) {
                                Log.error(COMPONENT, "Failed to decrypt E2E payload for topic: " + topic, cryptoEx);
                                msg = new Message(msg.action(), topic,
                                        "[ERRO DE CRIPTOGRAFIA: Não foi possível decifrar]");
                            }
                        } else {
                            Log.warn(COMPONENT, "Received PUBLISH for topic [" + topic + "] but local key is missing.");
                            msg = new Message(msg.action(), topic, "[ERRO: Chave E2E ausente para este tópico]");
                        }
                    }
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

    public void send(Message msg) {
        try {
            String json = JsonUtil.toJson(msg);

            // after the handshake the sessionKey should not be null
            if (this.sessionKey != null) {
                json = CipherUtil.encryptWithSessionKey(json, this.sessionKey);
            }

            FrameUtil.send(socket.getOutputStream(), json);

        } catch (Exception e) {
            Log.error(COMPONENT, "Error sending message", e);
        }
    }

    public void close() {

        Log.info(COMPONENT, "Closing Connection");

        try {
            running = false;
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public void setSessionKey(SecretKey sessionKey) {
        this.sessionKey = sessionKey;
    }

}
