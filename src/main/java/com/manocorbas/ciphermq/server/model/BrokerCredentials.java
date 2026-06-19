package com.manocorbas.ciphermq.server.model;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public record BrokerCredentials (
        PublicKey publicKey,
        PrivateKey privateKey,
        X509Certificate certificate
) {}
