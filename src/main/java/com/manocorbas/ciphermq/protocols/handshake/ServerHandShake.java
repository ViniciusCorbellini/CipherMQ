package com.manocorbas.ciphermq.protocols.handshake;

import java.io.IOException;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.protocols.certificate.CertificateAuthority;
import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.server.registry.SessionIdGenerator;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.KeyStorage;
import com.manocorbas.ciphermq.util.log.Log;

public class ServerHandShake {

    // TODO: ERROR RESPONSE (HandShakeException)

    private final Socket socket;

    private final String COMPONENT = "SERVERHANDSHAKE";

    public ServerHandShake(Socket socket) {
        this.socket = socket;
    }

    /**
     * Executes the whole server-side MQ client registry
     */
    public HandshakeResult doHandshake(PublicKey acPublicKey, PrivateKey acPrivKey) throws IOException {
        Message m = waitForClientHello();
        String sessionId = SessionIdGenerator.generate();
        String clientName;

        if (m.action() == ActionType.REGISTER) {
            try {
                // topic carrega o username, content carrega a pubKey serializada
                String clientId = m.topic();
                PublicKey clientPubKey = KeyStorage.deserializePublicKey(m.content());

                ClientCertificate cert = CertificateAuthority.signClient(clientId, clientPubKey, acPrivKey);

                sendHelloWithCert(sessionId, cert.serialize());
                clientName = clientId;
                Log.info(COMPONENT, "Registered new client: " + clientId);

            } catch (Exception e) {
                System.out.println("Erro no registr ============ \n " + e.getMessage());
                sendError("REGISTRATION_FAILED");
                return new HandshakeResult(null, null, false);
            }

        } else if (m.action() == ActionType.CONNECT) {
            try {
                ClientCertificate cert = ClientCertificate.deserialize(m.content());
                if (!CertificateAuthority.verifyCertificate(cert, acPublicKey)) {
                    sendError("INVALID_CERTIFICATE");
                    return new HandshakeResult(null, null, false);
                }
                sendHelloWithCert(sessionId, null); // cert null = acesso normal
                clientName = cert.clientId();

            } catch (Exception e) {
                sendError("MALFORMED_CERTIFICATE");
                return new HandshakeResult(null, null, false);
            }

        } else {
            sendError("UNEXPECTED_ACTION");
            return new HandshakeResult(null, null, false);
        }

        boolean clientReady = waitForReady();
        return new HandshakeResult(clientName, sessionId, clientReady);
    }

    // SERVER_HELLO sempre carrega um ServerHelloPayload — cert é null em CONNECT
    private void sendHelloWithCert(String sessionId, String certSerialized) throws IOException {
        ServerHelloPayload payload = new ServerHelloPayload(sessionId, certSerialized);
        Message m = new Message(ActionType.SERVER_HELLO, null, JsonUtil.toJson(payload));
        FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(m));
    }

    private void sendError(String errorMessage) throws IOException {
        Message m = new Message(ActionType.ERROR, null, errorMessage);

        String json = JsonUtil.toJson(m);

        FrameUtil.send(socket.getOutputStream(), json);
    }

    private Message waitForClientHello() throws IOException {
        String json = FrameUtil.receive(socket.getInputStream());

        Message m = JsonUtil.fromJson(json, Message.class);

        return m;
    }

    private void sendHello(String sessionId) throws IOException {
        Message m = new Message(ActionType.SERVER_HELLO, null, sessionId);

        String json = JsonUtil.toJson(m);

        FrameUtil.send(socket.getOutputStream(), json);
    }

    private boolean waitForReady() throws IOException {
        String json = FrameUtil.receive(socket.getInputStream());

        Message m = JsonUtil.fromJson(json, Message.class);

        return m.action() == ActionType.CLIENT_READY;
    }
}
