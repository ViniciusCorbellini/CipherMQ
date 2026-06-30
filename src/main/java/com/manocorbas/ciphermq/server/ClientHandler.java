package com.manocorbas.ciphermq.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import javax.crypto.SecretKey;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.exceptions.HandShakeException;
import com.manocorbas.ciphermq.exceptions.NonExistentTopicException;
import com.manocorbas.ciphermq.exceptions.UnauthorizedAccessException;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.protocols.handshake.ServerHandShake;
import com.manocorbas.ciphermq.server.model.ClientConnection;
import com.manocorbas.ciphermq.server.registry.ClientSession;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;
import com.manocorbas.ciphermq.util.CipherUtil;

public class ClientHandler implements Runnable, ClientConnection {

    // Handler
    private ClientSession session;
    private BrokerService brokerService;

    // Net
    private Socket clientSocket;
    private InputStream in;
    private OutputStream out;

    // protocols
    private ServerHandShake serverHandShake;

    // security
    private PublicKey pubKey;
    private PrivateKey privKey;
    private X509Certificate certificate;

    // Chave de sessão AES derivada do sessionId após o handshake
    private SecretKey sessionKey;

    // thread
    private volatile boolean running = true;

    // Log
    private String COMPONENT = "CLIENTHANDLER";

    public ClientHandler(Socket clientSocket, BrokerService brokerService, PublicKey pubKey, PrivateKey privKey,
            X509Certificate certificate) {
        this.clientSocket = clientSocket;
        this.brokerService = brokerService;
        this.serverHandShake = new ServerHandShake(clientSocket);
        this.pubKey = pubKey;
        this.privKey = privKey;
        this.certificate = certificate;
    }

    @Override
    public void run() {
        Log.debug(COMPONENT, "ClientHandler Running");

        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            session = doHandShakeAndRegistry();

        } catch (IOException | HandShakeException | NoSuchAlgorithmException e) {
            Log.error(COMPONENT, e.getMessage(), e);
            running = false;
        }

        Log.info(COMPONENT, "Listening");
        while (running) {
            try {
                String frame = FrameUtil.receive(in);

                // Decifra o envelope cliente-broker com a chave de sessão
                String json = CipherUtil.decryptWithSessionKey(frame, sessionKey);

                Message msg = JsonUtil.fromJson(json, Message.class);

                brokerService.handle(msg, session);
            } catch (SocketException e) {
                Log.info(COMPONENT, "Connection closed: " + e.getMessage());
                running = false;

            } catch (UnauthorizedAccessException e) {
                Log.warn(COMPONENT, "Client is not subscribed to this topic");

            } catch (NonExistentTopicException e) {
                Log.error(COMPONENT, e.getMessage(), e);

            } catch (Exception e) {
                Log.warn(COMPONENT, "Unexpected error while handling client: " + e.getMessage());
                running = false;
            }
        }

        cleanup();
    }

    private ClientSession doHandShakeAndRegistry() throws IOException, HandShakeException, NoSuchAlgorithmException {
        Log.info(COMPONENT, "Handshaking");
        HandshakeResult result = serverHandShake.doHandshake(this.pubKey, this.privKey, this.certificate);

        if (!result.success())
            throw new HandShakeException("Error while trying to do handshake");

        // Deriva a mesma chave de sessão que o cliente vai derivar
        this.sessionKey = CipherUtil.deriveSessionKey(result.sessionId());
        Log.info(COMPONENT, "Session key derived for: " + result.clientName());

        return brokerService.register(result.clientName(), this, result.sessionId());
    }

    @Override
    public void send(Message message) {
        try {
            String json = JsonUtil.toJson(message);

            // Cifra com a chave de sessão antes de enviar
            String encrypted = CipherUtil.encryptWithSessionKey(json, sessionKey);

            FrameUtil.send(out, encrypted);

        } catch (Exception e) {
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
