package com.manocorbas.ciphermq.cli;

import com.manocorbas.ciphermq.client.ClientCredentials;
import com.manocorbas.ciphermq.client.ClientSetup;
import com.manocorbas.ciphermq.kms.KeyManagementServer;
import com.manocorbas.ciphermq.util.PathUtil;
import com.manocorbas.ciphermq.util.log.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class KmsCli {

    private static final String COMPONENT = "KMSCLI";
    private static final String KMS_USERNAME = "kms";

    /**
     * Inicia o KMS.
     *
     * @param kmsPort   porta que o KMS vai escutar
     * @param brokerHost host do broker
     * @param brokerPort porta do broker
     * @param caCertPath caminho para o ca.crt (opcional se já salvo)
     */
    public static void start(int kmsPort, String brokerHost, int brokerPort, String caCertPath) throws Exception {

        // Copia o ca.crt se fornecido
        if (caCertPath != null) {
            Path source = Path.of(caCertPath);
            if (!Files.exists(source)) {
                throw new IllegalArgumentException("CA certificate not found at: " + caCertPath);
            }
            Files.createDirectories(PathUtil.CA_CERT.getParent());
            Files.copy(source, PathUtil.CA_CERT, StandardCopyOption.REPLACE_EXISTING);
            Log.info(COMPONENT, "CA certificate saved to " + PathUtil.CA_CERT);
        }

        if (!Files.exists(PathUtil.CA_CERT)) {
            throw new IllegalStateException(
                    "No CA certificate found. Run with --ca-cert <path/to/ca.crt> on first use.");
        }

        // O KMS se conecta ao broker como um cliente registrado
        // Usa ClientSetup para carregar/criar o certificado do KMS
        ClientCredentials kmsCreds = ClientSetup.load(KMS_USERNAME);

        // Sobe o servidor
        KeyManagementServer kms = new KeyManagementServer(kmsPort, brokerHost, brokerPort, kmsCreds);
        Log.info(COMPONENT, "Starting KMS on port " + kmsPort
                + " | broker=" + brokerHost + ":" + brokerPort);
        kms.start(); // bloqueante
    }
}
