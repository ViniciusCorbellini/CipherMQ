package com.manocorbas.ciphermq.protocols.handshake;

import java.io.IOException;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.protocols.certificate.CertificateAuthority;
import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.server.registry.SessionIdGenerator;
import com.manocorbas.ciphermq.util.CipherUtil;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.KeyStorage;
import com.manocorbas.ciphermq.util.X509Util;
import com.manocorbas.ciphermq.util.log.Log;

public class ServerHandShake {

    private final Socket socket;

    private final String COMPONENT = "SERVERHANDSHAKE";

    public ServerHandShake(Socket socket) {
        this.socket = socket;
    }

    /**
     * Executes the whole server-side MQ client registry
     */
    public HandshakeResult doHandshake(PublicKey acPublicKey, PrivateKey acPrivKey, X509Certificate brokerCertificate) throws IOException {

        // STEP ZERO: Broker sends his certificate
        sendBrokerCert(brokerCertificate);

        // FIST STEP: Client sends hello (REGISTER or CONNECT)
        Message m = waitForClientHello();

        // SECOND STEP: Client's certificate validation; Session ID generation
        String sessionId = SessionIdGenerator.generate();
        String clientName;
        PublicKey clientPubKey = null;

        if (m.action() == ActionType.REGISTER) {
            try {
                // topic carrega o username, content carrega a pubKey serializada
                String clientId = m.topic();
                clientPubKey = KeyStorage.deserializePublicKey(m.content());
                ClientCertificate cert = CertificateAuthority.signClient(clientId, clientPubKey, acPrivKey);

                // THIRD STEP: BROKER sends his hello (acks the user hello and sends the id)
                sendHelloWithCert(sessionId, cert.serialize(), clientPubKey);
                clientName = clientId;
                Log.info(COMPONENT, "Registered new client: " + clientId);

            } catch (Exception e) {
                System.out.println("Erro no registro ============ \n " + e.getMessage());
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

                clientPubKey = cert.clientPublicKey();

                // THIRD STEP: BROKER sends his hello (acks the user hello and sends the id)
                sendHelloWithCert(sessionId, null, clientPubKey); // cert null = acesso normal
                clientName = cert.clientId();

            } catch (Exception e) {
                sendError("MALFORMED_CERTIFICATE");
                Log.error(COMPONENT, e.getLocalizedMessage(), e);
                return new HandshakeResult(null, null, false);
            }

        } else {
            sendError("UNEXPECTED_ACTION");
            return new HandshakeResult(null, null, false);
        }

        // FIFTH STEP:
        // Client sends READY (third step of the three way handshake)
        // so the server can finally REGISTER | CONNECT
        boolean clientReady = waitForReady();
        return new HandshakeResult(clientName, sessionId, clientReady);
    }

    /**
     * Serializes ServerHelloPayload, ciphers it using client's pub key (digital envelope)
     */
    private void sendHelloWithCert(String sessionId, String certSerialized, PublicKey clientPublicKey) throws Exception {
        ServerHelloPayload payload = new ServerHelloPayload(sessionId, certSerialized);
        String jsonPayload = JsonUtil.toJson(payload);
        
        // Protege o conteúdo do ServerHello (incluindo o sessionId que agirá como chave)
        String sealedEnvelope = CipherUtil.sealEnvelope(jsonPayload, clientPublicKey);
        
        Message m = new Message(ActionType.SERVER_HELLO, null, sealedEnvelope);
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

    private void sendBrokerCert(X509Certificate cert) throws IOException {
        try {
            Message m = new Message(ActionType.BROKER_CERTIFICATE, null, X509Util.serialize(cert));
            FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(m));
        } catch (Exception e) {
            throw new IOException("Failed to send broker certificate", e);
        }
    }
}
