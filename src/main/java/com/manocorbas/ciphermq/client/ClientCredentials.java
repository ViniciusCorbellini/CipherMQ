package com.manocorbas.ciphermq.client;

import java.security.PrivateKey;
import java.security.PublicKey;

import com.manocorbas.ciphermq.protocols.certificate.ClientCertificate;

public record ClientCredentials(
    String username,
    PublicKey publicKey,
    PrivateKey privateKey,
    ClientCertificate certificate
) {}
