package com.manocorbas.ciphermq.kms;

import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.util.CipherUtil;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;
import com.manocorbas.ciphermq.common.Message;

import javax.crypto.SecretKey;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Atende um cliente MQ conectado ao KMS.
 *
 * Após o handshake, o cliente pode enviar:
 * INIT_TOPIC — publisher notifica que criou tópico; KMS gera/retorna chave
 * GET_KEY — qualquer cliente pede a chave de um tópico
 */
public class KmsClientHandler implements Runnable {

    private static final String COMPONENT = "KMSCLIENTHANDLER";

    private final Socket socket;
    private final KmsKeyStore keyStore;
    private final KmsBrokerConnection brokerConn;
    private final PublicKey kmsPub;
    private final PrivateKey kmsPriv;
    private final PublicKey brokerAcPub;

    private ClientCertificate clientCert;

    public KmsClientHandler(Socket socket,
            KmsKeyStore keyStore,
            KmsBrokerConnection brokerConn,
            PublicKey kmsPub,
            PrivateKey kmsPriv,
            PublicKey brokerAcPub) {
        this.socket = socket;
        this.keyStore = keyStore;
        this.brokerConn = brokerConn;
        this.kmsPub = kmsPub;
        this.kmsPriv = kmsPriv;
        this.brokerAcPub = brokerAcPub;
    }

    @Override
    public void run() {
        try {
            // Handshake: valida certificado do cliente
            KmsHandshake handshake = new KmsHandshake(socket, kmsPub, kmsPriv, brokerAcPub);
            clientCert = handshake.doHandshake();

            Log.info(COMPONENT, "Client connected: " + clientCert.clientId());

            // Loop de mensagens
            while (true) {
                String json = FrameUtil.receive(socket.getInputStream());
                Message msg = JsonUtil.fromJson(json, Message.class);
                handle(msg);
            }

        } catch (Exception e) {
            Log.error(COMPONENT, "Client disconnected: " +
                    (clientCert != null ? clientCert.clientId() : "unknown") +
                    " — " + e.getMessage(), e);
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void handle(Message msg) throws Exception {

        KmsAction msgAction = (KmsAction) msg.action();

        switch (msgAction) {

            case INIT_TOPIC -> {
                // Publisher acabou de criar o tópico; KMS gera a chave
                String topic = msg.topic();
                Log.info(COMPONENT, "INIT_TOPIC: " + topic + " by " + clientCert.clientId());

                // Verifica se o publisher realmente está no tópico antes de criar a chave
                boolean subscribed = brokerConn.checkSubscription(clientCert.clientId(), topic);
                if (!subscribed) {
                    send(new Message(KmsAction.ERROR, topic, "NOT_SUBSCRIBED"));
                    return;
                }

                SecretKey key = keyStore.initTopic(topic);

                // Entrega a chave já envelopada para o publisher (ele vai querer publicar logo)
                String envelope = CipherUtil.sealEnvelope(
                        CipherUtil.serializeSecretKey(key),
                        clientCert.clientPublicKey());

                send(new Message(KmsAction.KEY_RESPONSE, topic, envelope));
            }

            case GET_KEY -> {
                String topic = msg.topic();
                Log.info(COMPONENT, "GET_KEY: " + topic + " by " + clientCert.clientId());

                if (!keyStore.hasTopic(topic)) {
                    send(new Message(KmsAction.ERROR, topic, "TOPIC_NOT_FOUND"));
                    return;
                }

                // Verifica subscrição no broker
                boolean subscribed = brokerConn.checkSubscription(clientCert.clientId(), topic);
                if (!subscribed) {
                    send(new Message(KmsAction.ERROR, topic, "NOT_SUBSCRIBED"));
                    return;
                }

                SecretKey key = keyStore.getKey(topic);

                // Entrega envelopada com a pubkey do cliente solicitante
                String envelope = CipherUtil.sealEnvelope(
                        CipherUtil.serializeSecretKey(key),
                        clientCert.clientPublicKey());

                send(new Message(KmsAction.KEY_RESPONSE, topic, envelope));
            }

            default -> {
                Log.warn(COMPONENT, "Unexpected action: " + msg.action());
                send(new Message(KmsAction.ERROR, null, "UNEXPECTED_ACTION"));
            }
        }
    }

    private void send(Message msg) throws Exception {
        FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(msg));
    }
}