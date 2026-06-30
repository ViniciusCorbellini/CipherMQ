package com.manocorbas.ciphermq.kms;

import com.manocorbas.ciphermq.client.ClientCredentials;
import com.manocorbas.ciphermq.util.log.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Servidor Central de Gerenciamento de Chaves (KMS)
 * * Abre uma porta TCP para receber clientes MQ que necessitam de chaves de
 * criptografia E2E.
 * Cria uma única conexão persistente com o Broker para realizar checagens de
 * subscrição.
 */
public class KeyManagementServer {

    private static final String COMPONENT = "KMS_SERVER";

    private final int kmsPort;
    private final String brokerHost;
    private final int brokerPort;
    private final ClientCredentials kmsCreds;

    private ServerSocket serverSocket;
    private KmsKeyStore keyStore;
    private KmsBrokerConnection brokerConn;

    // Pares de chaves criptográficas do próprio KMS e a Pública da AC do Professor
    private PublicKey kmsPublicKey;
    private PrivateKey kmsPrivateKey;
    private PublicKey brokerAcPublicKey;

    private boolean running = false;

    public KeyManagementServer(int kmsPort, String brokerHost, int brokerPort, ClientCredentials kmsCreds) {
        this.kmsPort = kmsPort;
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.kmsCreds = kmsCreds;

        this.kmsPublicKey = kmsCreds.publicKey();
        this.kmsPrivateKey = kmsCreds.privateKey();
    }

    /**
     * Inicializa os componentes internos e inicia o loop do servidor TCP.
     */
    public void start() {
        try {
            Log.info(COMPONENT, "Starting Key Management Server...");

            // 1. Carrega o KeyStore local onde as chaves simétricas dos tópicos são
            // persistidas
            this.keyStore = new KmsKeyStore();

            // 3. Estabelece a conexão de controle KMS <-> Broker usando o protocolo de
            // handshake padrão
            Log.info(COMPONENT, "Establishing control connection with Broker...");
            this.brokerConn = new KmsBrokerConnection(brokerHost, brokerPort, kmsCreds);
            this.brokerAcPublicKey = brokerConn.getBrokerAcPublicKey();

            // 4. Inicia o ServerSocket TCP do KMS
            this.serverSocket = new ServerSocket(kmsPort);
            this.running = true;
            Log.info(COMPONENT, "KMS listening for clients on port: " + kmsPort);

            // 5. Loop de aceitação de clientes (Multithreading)
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Log.debug(COMPONENT,
                            "New raw TCP connection received from " + clientSocket.getRemoteSocketAddress());

                    // Instancia o handler passando todos os motores compartilhados do KMS
                    KmsClientHandler handler = new KmsClientHandler(
                            clientSocket,
                            keyStore,
                            brokerConn,
                            kmsPublicKey,
                            kmsPrivateKey,
                            brokerAcPublicKey);

                    // Dispara a Thread concorrente para o cliente
                    Thread clientThread = new Thread(handler);
                    clientThread.setName("KmsClient-" + clientSocket.getRemoteSocketAddress());
                    clientThread.start();

                } catch (IOException e) {
                    if (!running)
                        break;
                    Log.error(COMPONENT, "Error accepting client connection: " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            Log.error(COMPONENT, "Critical error during KMS startup: " + e.getMessage(), e);
            e.printStackTrace();
            stop();
        }
    }

    /*
     * Desliga o servidor graciosamente liberando as portas e conexões TCP.
     */
    public synchronized void stop() {
        Log.info(COMPONENT, "Stopping KMS...");
        this.running = false;

        if (brokerConn != null) {
            brokerConn.close();
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        Log.info(COMPONENT, "KMS stopped.");
    }
}