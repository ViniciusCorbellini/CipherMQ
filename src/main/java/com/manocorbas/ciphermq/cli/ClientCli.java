package com.manocorbas.ciphermq.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.manocorbas.ciphermq.gui.Dashboard;
import com.manocorbas.ciphermq.util.PathUtil;
import com.manocorbas.ciphermq.util.log.Log;

public class ClientCli {

    public static void start(String host, int port, String kmsHost, int kmsPort, String caCertPath) throws IOException {

        if (caCertPath != null) {
            Path source = Path.of(caCertPath);

            if (!Files.exists(source)) {
                throw new IllegalArgumentException("CA certificate not found at: " + caCertPath);
            }

            Files.createDirectories(PathUtil.CA_CERT.getParent());
            Files.copy(source, PathUtil.CA_CERT, StandardCopyOption.REPLACE_EXISTING);

            Log.info("CLIENTCLI", "CA certificate saved to " + PathUtil.CA_CERT);
        }

        if (!Files.exists(PathUtil.CA_CERT)) {
            throw new IllegalStateException(
                    "No CA certificate found. Run with --ca-cert <path/to/ca.crt> on first use.");
        }

        Log.debug("CLIENTCLI", "Client CLI started");

        Dashboard dashboard = new Dashboard(host, port, kmsHost, kmsPort);
        dashboard.pack();
        dashboard.setVisible(true);
    }

}
