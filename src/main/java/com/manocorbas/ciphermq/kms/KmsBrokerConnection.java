package com.manocorbas.ciphermq.kms;

import com.manocorbas.ciphermq.client.ClientCredentials;
import com.manocorbas.ciphermq.common.ActionType;
import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.protocols.handshake.ClientHandShake;
import com.manocorbas.ciphermq.protocols.handshake.HandshakeResult;
import com.manocorbas.ciphermq.util.CipherUtil;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

import java.net.Socket;
import java.security.PublicKey;
import javax.crypto.SecretKey;

/**
 * KMS -> Broker connection
 *
 * O KMS age como um cliente normal do broker para fazer o handshake e
 * depois usa a mesma conexão TCP para consultas CHECK_SUBSCRIPTION.
 * Sincronizado porque múltiplas threads do KmsClientHandler podem consultar.
 */
public class KmsBrokerConnection {

    private static final String COMPONENT = "KMSBROKERCONN";

    private Socket socket;
    private String sessionId;
    private PublicKey brokerAcPublicKey;
    private SecretKey sessionKey;

    public KmsBrokerConnection(String brokerHost, int brokerPort, ClientCredentials kmsCreds) throws Exception {
        socket = new Socket(brokerHost, brokerPort);
        Log.info(COMPONENT, "Connected to broker at " + brokerHost + ":" + brokerPort);

        ClientHandShake handshake = new ClientHandShake(socket);
        HandshakeResult result = handshake.doHandshake(kmsCreds);

        if (!result.success()) {
            throw new Exception("KMS failed to handshake with broker");
        }

        this.sessionId = result.sessionId();
        this.sessionKey = CipherUtil.deriveSessionKey(sessionId);
        this.brokerAcPublicKey = result.extractedPublicKey();

        Log.info(COMPONENT, "KMS authenticated with broker as: " + result.clientName());
    }

    /**
     * Pergunta ao broker se {@code clientId} está subscrito em {@code topic}.
     * Bloqueante e sincronizado — uma consulta por vez.
     */
    public synchronized boolean checkSubscription(String clientId, String topic) throws Exception {
        // CHECK_SUBSCRIPTION: topic = itself , content = clientId
        Message req = new Message(ActionType.CHECK_SUBSCRIPTION, topic, clientId);
        String jsonReq = JsonUtil.toJson(req);

        // symetric cripto
        String encryptedPayload = CipherUtil.encryptWithSessionKey(jsonReq, this.sessionKey);
        FrameUtil.send(socket.getOutputStream(), encryptedPayload);

        // waits for SUBSCRIPTION_RESULT
        String data = FrameUtil.receive(socket.getInputStream());
        
        String jsonRes = CipherUtil.decryptWithSessionKey(data, this.sessionKey);
        Message res = JsonUtil.fromJson(jsonRes, Message.class);

        if (res.action() != ActionType.SUBSCRIPTION_RESULT) {
            throw new Exception("Expected SUBSCRIPTION_RESULT, got: " + res.action());
        }

        boolean subscribed = Boolean.parseBoolean(res.content());
        Log.info(COMPONENT, "Subscription check [" + clientId + " @ " + topic + "]: " + subscribed);
        return subscribed;
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }

    public PublicKey getBrokerAcPublicKey() {
        return brokerAcPublicKey;
    }
}