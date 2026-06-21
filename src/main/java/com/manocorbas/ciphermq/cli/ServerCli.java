package com.manocorbas.ciphermq.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.manocorbas.ciphermq.server.Server;
import com.manocorbas.ciphermq.util.PathUtil;
import com.manocorbas.ciphermq.util.log.Log;

public class ServerCli {
    public static void start(int port, String brokerCertPath) throws Exception {

        if (brokerCertPath != null) {
            Path source = Path.of(brokerCertPath);
            if (!Files.exists(source)) {
                throw new IllegalArgumentException("Broker certificate not found at: " + brokerCertPath);
            }
            Files.createDirectories(PathUtil.BROKER_CERT.getParent());
            Files.copy(source, PathUtil.BROKER_CERT, StandardCopyOption.REPLACE_EXISTING);
            Log.info("SERVERCLI", "Broker certificate saved to " + PathUtil.BROKER_CERT);
        }

        Server s = new Server(port);
        s.start();
    }
}
