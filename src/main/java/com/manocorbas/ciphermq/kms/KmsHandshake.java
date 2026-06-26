package com.manocorbas.ciphermq.kms;

import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.protocols.certificate.CertificateAuthority;
import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

/**
 * Handshake do lado servidor do KMS.
 *
 * Fluxo (espelhado no handshake broker↔cliente):
 * S→C KMS_CERTIFICATE (pubkey do KMS em Base64)
 * C→S KMS_CONNECT (certificado do cliente serializado)
 * S→C KMS_HELLO (ok ou erro)
 * C→S KMS_READY
 *
 * Retorna o ClientCertificate validado do cliente conectado.
 */
public class KmsHandshake {

    private static final String COMPONENT = "KMSHANDSHAKE";

    private final Socket socket;
    private final PublicKey kmsPub;
    private final PrivateKey kmsPriv;
    private final PublicKey brokerPub;

    public KmsHandshake(Socket socket, PublicKey kmsPub, PrivateKey kmsPriv, PublicKey brokerPub) {
        this.socket = socket;
        this.kmsPub = kmsPub;
        this.kmsPriv = kmsPriv;
        this.brokerPub = brokerPub;
    }

    /**
     * Executa o handshake e retorna o certificado validado do cliente,
     * ou lança exceção em caso de falha.
     */
    public ClientCertificate doHandshake() throws Exception {
        socket.setSoTimeout(5000);

        // Passo 1: envia a pubkey do KMS para o cliente poder selar envelopes
        sendKmsCertificate();

        // Passo 2: recebe o certificado do cliente
        ClientCertificate clientCert = receiveClientCert();

        // Passo 3: valida com a AC do broker
        try {
            boolean isValid = CertificateAuthority.verifyCertificate(clientCert, this.brokerPub);
            if (!isValid) {
                sendError("INVALID_CERTIFICATE");
                throw new Exception("Client certificate verification failed against Broker CA");
            }
        } catch (Exception e) {
            Log.error(COMPONENT, "Crypto error during certificate verification: " + e.getMessage(), e);
            sendError("BAD_SIGNATURE");
            throw e;
        }

        if (!CertificateAuthority.verifyCertificate(clientCert, this.brokerPub)) {
            sendError("INVALID_CERTIFICATE");
            throw new Exception("Client certificate invalid for: " + clientCert.clientId());
        }

        // Passo 4: envia OK
        sendHello();

        // Passo 5: aguarda KMS_READY do cliente
        waitForReady();

        socket.setSoTimeout(0);
        Log.info(COMPONENT, "Handshake OK for client: " + clientCert.clientId());
        return clientCert;
    }

    private void sendKmsCertificate() throws Exception {
        String pubKeyB64 = Base64.getEncoder().encodeToString(kmsPub.getEncoded());
        Message msg = new Message(KmsAction.KMS_CERTIFICATE, null, pubKeyB64);
        FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(msg));
    }

    private ClientCertificate receiveClientCert() throws Exception {
        String json = FrameUtil.receive(socket.getInputStream());
        Message msg = JsonUtil.fromJson(json, Message.class);

        if (msg.action() != KmsAction.KMS_CONNECT && msg.action() != KmsAction.KMS_REGISTER) {
            throw new Exception("Expected KMS_CONNECT, got: " + msg.action());
        }

        return ClientCertificate.deserialize(msg.content());
    }

    private void sendHello() throws Exception {
        Message msg = new Message(KmsAction.KMS_HELLO, null, "OK");
        FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(msg));
    }

    private void sendError(String reason) throws Exception {
        Message msg = new Message(KmsAction.ERROR, null, reason);
        FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(msg));
    }

    private void waitForReady() throws Exception {
        String json = FrameUtil.receive(socket.getInputStream());
        Message msg = JsonUtil.fromJson(json, Message.class);
        if (msg.action() != KmsAction.KMS_READY) {
            throw new Exception("Expected KMS_READY, got: " + msg.action());
        }
    }
}