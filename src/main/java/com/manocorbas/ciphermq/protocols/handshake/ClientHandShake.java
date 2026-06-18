package com.manocorbas.ciphermq.protocols.handshake;

import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Base64;

import com.manocorbas.ciphermq.client.ClientCredentials;
import com.manocorbas.ciphermq.client.ClientSetup;
import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;

public class ClientHandShake {

    private final Socket socket;

    public ClientHandShake(Socket socket) {
        this.socket = socket;
    }

    /**
     * Executes the whole client-side MQ client registry
     */
    public HandshakeResult doHandshake(ClientCredentials cred) throws Exception {

        // avoids blocking threads and zombie clients
        socket.setSoTimeout(5000);

        ClientCertificate certificate = cred.certificate();

        // TODO: Broker validation FIRST

        // FIST STEP: Client sends his certificate and CONNECT | REGISTER
        boolean firstAccess = certificate == null;

        if (firstAccess) {
            sendHello(ActionType.REGISTER, cred.username(), cred.publicKey());
        } else {
            sendHello(ActionType.CONNECT, certificate.serialize());
        }

        /**
         * SECOND STEP (Server Side):
         * BROKER registers/connects the user and generates a new session ID,
         * which leads us to
         * ...
         */

        /**
         * THIRD STEP (Server Side):
         * the broker sends an ACK for the user and the ServerHelloPayload
         * so the client waits for it:
         */
        ServerHelloPayload hello = waitForServerHello();

        /**
         * FOURTH STEP:
         * Client stores his certificate (and session id after we return this result)
         */
        if (firstAccess) {
            // saves the received cert in disk
            ClientCertificate signedCert = ClientCertificate.deserialize(hello.certSerialized());
            ClientSetup.saveCertificate(cred.username(), signedCert);
        }

        // FIFTH STEP:
        // Client sends his ACK (ready)
        // so the server can finally REGISTER | CONNECT
        sendReady();

        // resets to an infinite timeout
        socket.setSoTimeout(0);

        String clientId = firstAccess
                ? ClientCertificate.deserialize(hello.certSerialized()).clientId()
                : certificate.clientId();

        return new HandshakeResult(clientId, hello.sessionId(), true);
    }

    // Step 1: CLIENT_HELLO (CONNECT)
    private void sendHello(ActionType action, String certificateContent) throws Exception {

        // For now i'll use the message obj,
        // should use an HelloMessage instead (todo in the future)
        Message hello = new Message(action, null, certificateContent);

        String json = JsonUtil.toJson(hello);

        FrameUtil.send(socket.getOutputStream(), json);
    }

    // Step 1: CLIENT_HELLO (REGISTER):
    // envia username no topic, pubKey serializada no content
    private void sendHello(ActionType action, String username, PublicKey publicKey) throws Exception {
        String pubKeyEncoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        Message hello = new Message(action, username, pubKeyEncoded);
        FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(hello));
    }

    // Step 3: client side SERVER HELLO
    private ServerHelloPayload waitForServerHello() throws Exception {
        String messageJson = FrameUtil.receive(socket.getInputStream());
        Message m = JsonUtil.fromJson(messageJson, Message.class);

        if (m.action() == ActionType.ERROR) {
            throw new Exception("Broker error: " + m.content());
        }
        if (m.action() != ActionType.SERVER_HELLO) {
            throw new Exception("Expected SERVER_HELLO, got: " + m.action());
        }

        return JsonUtil.fromJson(m.content(), ServerHelloPayload.class);
    }

    // Step 5: client sends ack
    private void sendReady() throws IOException {
        Message m = new Message(ActionType.CLIENT_READY, null, null);

        String ack = JsonUtil.toJson(m);

        FrameUtil.send(socket.getOutputStream(), ack);
    }

}
