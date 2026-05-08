package com.manocorbas.ciphermq.protocols.handshake;

import java.io.IOException;
import java.net.Socket;

import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.server.registry.SessionIdGenerator;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;

public class ServerHandShake { 
    
    //TODO: ERROR RESPONSE

    private final Socket socket;

    public ServerHandShake(Socket socket) {
        this.socket = socket;
    }

    /**
     * Executes the whole server-side MQ client registry
     */
    private HandshakeResult doHandshake() throws Exception {

        // FIST STEP: Client sends his <name> and <action> (REGISTER || CONNECT)
        Message m = waitForClientHello();

        // SECOND STEP: BROKER generates a random session id
        String sessionId = SessionIdGenerator.generate();

        // THIRD STEP: BROKER sends his hello (acks the user hello and sends the id)
        sendHello(sessionId);        

        // FOURTH STEP:
        // Client stores session id

        // FIFTH STEP:
        // Client sends READY (third step of the three way handshake)
        // so the server can finally REGISTER | CONNECT
        boolean clientReady = waitForReady();

        return new HandshakeResult(sessionId, clientReady, m.action());

    }

    private boolean waitForReady() throws IOException {
        String json = FrameUtil.receive(socket.getInputStream());

        Message m = JsonUtil.fromJson(json, Message.class);

        return m.action() == ActionType.CLIENT_READY;
    }

    private Message waitForClientHello() throws Exception {
        String json = FrameUtil.receive(socket.getInputStream());

        Message m = JsonUtil.fromJson(json, Message.class);

        if(m.action() != ActionType.CONNECT && m.action() != ActionType.REGISTER){
            throw new Exception("Invalid action");
        }

        return m;
    }

    private void sendHello(String sessionId) throws IOException{
        Message m = new Message(ActionType.SERVER_HELLO, null, sessionId);

        String json = JsonUtil.toJson(m);

        FrameUtil.send(socket.getOutputStream(), json);
    }
}
