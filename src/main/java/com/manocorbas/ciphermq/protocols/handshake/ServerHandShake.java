package com.manocorbas.ciphermq.protocols.handshake;

import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.protocols.certificate.CertificateAuthority;
import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.server.registry.SessionIdGenerator;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;

public class ServerHandShake {

    // TODO: ERROR RESPONSE (HandShakeException)

    private final Socket socket;

    public ServerHandShake(Socket socket) {
        this.socket = socket;
    }

    /**
     * Executes the whole server-side MQ client registry
     */
    public HandshakeResult doHandshake(PublicKey acPublicKey) throws IOException {

        // FIST STEP: Client sends his <name>
        Message m = waitForClientHello();

        // SECOND STEP: Client's certificate validation; Session ID generation
        ClientCertificate cert;
        try {
            cert = ClientCertificate.deserialize(m.content());
            boolean valid = CertificateAuthority.verifyCertificate(cert, acPublicKey);
            if (!valid) {
                // sendError("INVALID_CERTIFICATE"); TODO
                return new HandshakeResult(null, null, false);
            }
        } catch (Exception e) {
            //  sendError("MALFORMED_CERTIFICATE"); TODO
            return new HandshakeResult(null, null, false);
        }

        String clientName = cert.clientId();
        String sessionId = SessionIdGenerator.generate();

        // THIRD STEP: BROKER sends his hello (acks the user hello and sends the id)
        sendHello(sessionId);

        // FOURTH STEP:
        // Client stores session id

        // FIFTH STEP:
        // Client sends READY (third step of the three way handshake)
        // so the server can finally REGISTER | CONNECT
        boolean clientReady = waitForReady();

        return new HandshakeResult(clientName, sessionId, clientReady);
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
