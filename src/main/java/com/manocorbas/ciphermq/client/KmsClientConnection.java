package com.manocorbas.ciphermq.client;

import com.manocorbas.ciphermq.common.Message;
import com.manocorbas.ciphermq.kms.KmsAction; // Certifique-se de importar o Enum correto do KMS
import com.manocorbas.ciphermq.util.FrameUtil;
import com.manocorbas.ciphermq.util.JsonUtil;
import com.manocorbas.ciphermq.util.log.Log;

import java.net.Socket;

public class KmsClientConnection {

    private static final String COMPONENT = "KMS_CLIENT_CONN";
    
    private final String kmsHost;
    private final int kmsPort;
    private final ClientCredentials creds;

    public KmsClientConnection(String kmsHost, int kmsPort, ClientCredentials creds) {
        this.kmsHost = kmsHost;
        this.kmsPort = kmsPort;
        this.creds = creds;
    }

    /**
     * Executa a operação solicitada (INIT_TOPIC ou GET_KEY) e retorna o envelope digital da chave AES.
     */
    public String requestKeyFromKms(KmsAction action, String topic) throws Exception {
        Log.info(COMPONENT, "Connecting to KMS to execute: " + action + " on topic: " + topic);
        
        try (Socket socket = new Socket(kmsHost, kmsPort)) {
            socket.setSoTimeout(5000);

            // --- FLUXO DO KMS HANDSHAKE (Lado Cliente) ---
            
            // Passo 1: Recebe a chave pública do KMS (KMS_CERTIFICATE)
            String kmsPubKeyString = FrameUtil.receive(socket.getInputStream());
            Message kmsPubKeyMessage = JsonUtil.fromJson(kmsPubKeyString, Message.class);
            
            if (kmsPubKeyMessage.action() != KmsAction.KMS_CERTIFICATE) {
                throw new Exception("Expected KMS_CERTIFICATE, got: " + kmsPubKeyMessage.action());
            }
            
            // Passo 2: Envia o certificado do cliente (KMS_CONNECT ou KMS_REGISTER)
            // Se for o primeiro acesso do cliente na vida, usaria REGISTER, mas se ele já tem cert, usa CONNECT
            KmsAction connectAction = (creds.certificate() == null) ? KmsAction.KMS_REGISTER : KmsAction.KMS_CONNECT;
            String certSerialized = (creds.certificate() == null) ? creds.username() : creds.certificate().serialize(); 
            // Nota: Adapte o conteúdo se o seu REGISTER exigir a chave pública em vez do username puro

            Message step2Msg = new Message(connectAction, null, certSerialized);
            FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(step2Msg));

            // Passo 3: Aguarda o OK do KMS (KMS_HELLO)
            String step3Json = FrameUtil.receive(socket.getInputStream());
            Message step3Msg = JsonUtil.fromJson(step3Json, Message.class);
            if (step3Msg.action() != KmsAction.KMS_HELLO) {
                throw new Exception("KMS Handshake rejected: " + step3Msg.content());
            }

            // Passo 4: Envia KMS_READY
            Message step4Msg = new Message(KmsAction.KMS_READY, null, null);
            FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(step4Msg));
            Log.info(COMPONENT, "Handshake with KMS established successfully!");

            // --- FIM DO HANDSHAKE / INÍCIO DA REQUISIÇÃO DA CHAVE ---

            // Envia a requisição real (INIT_TOPIC ou GET_KEY)
            Message req = new Message(action, topic, null);
            FrameUtil.send(socket.getOutputStream(), JsonUtil.toJson(req));

            // Recebe a resposta com o envelope criptografado
            String resJson = FrameUtil.receive(socket.getInputStream());
            Message res = JsonUtil.fromJson(resJson, Message.class);

            if (res.action() == KmsAction.ERROR) {
                throw new Exception("KMS returned an error: " + res.content());
            }

            if (res.action() != KmsAction.KEY_RESPONSE) {
                throw new Exception("Expected KEY_RESPONSE, got: " + res.action());
            }

            // Retorna a String contendo o envelope digital selado
            return res.content(); 
        }
    }
}