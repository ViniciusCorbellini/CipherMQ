package com.manocorbas.ciphermq.protocols.handshake;

import java.io.IOException;
import java.net.Socket;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;

public class ClientHandShake {

    private final Socket socket;

    public ClientHandShake(Socket socket) {
        this.socket = socket;
    }

    public HandshakeResult register(String username) throws Exception {
        // Client can now store his sess id
        return doHandshake(ActionType.REGISTER, username);
    }

    public HandshakeResult connect(String username) throws Exception {
        // Client can now store his sess id
        return doHandshake(ActionType.CONNECT, username);
    }

    /**
     * Executes the whole client-side MQ client registry
     */
    private HandshakeResult doHandshake(ActionType action, String username) throws Exception {

        // avoids blocking threads and zombie clients
        socket.setSoTimeout(5000);

        // FIST STEP: Client sends his <name> and <action> (REGISTER || CONNECT)
        sendHello(action, username);

        // SECOND STEP (Server Side):
        // BROKER generates a new session ID, which leads us to
        // ...
        // THIRD STEP (Server Side):
        // the broker sends an ACK for the user and his new session ID
        // so the client waits for it:
        String sessionId = waitForServerHello();

        // FOURTH STEP:
        // Client stores session id
        // ...
        // obs: actually this will be done after we return the result

        // FIFTH STEP:
        // Client sends his ACK (ready)
        // so the server can finally REGISTER | CONNECT
        sendReady();

        return new HandshakeResult(sessionId, true, action);

    }

    // Step 1: CLIENT_HELLO
    private void sendHello(ActionType action, String username) throws Exception {

        // For now i'll use the message obj,
        // should use an HelloMessage instead (todo in the future)
        Message hello = new Message(action, null, username);

        String json = JsonUtil.toJson(hello);

        FrameUtil.send(socket.getOutputStream(), json);
    }

    // Step 3: client side SERVER HELLO
    private String waitForServerHello() throws Exception {

        // data will always be sent and received as Json
        String messageJson = FrameUtil.receive(socket.getInputStream());

        Message m = JsonUtil.fromJson(messageJson, Message.class);

        if (m.action() != ActionType.SERVER_HELLO) {
            throw new Exception("Error while waiting for SERVER_HELLO");
        }

        String sessionId = m.content();

        return sessionId;
    }

    // Step 5: client sends ack
    private void sendReady() throws IOException {
        Message m = new Message(ActionType.CLIENT_READY, null, null);

        String ack = JsonUtil.toJson(m);

        FrameUtil.send(socket.getOutputStream(), ack);
    }

}
