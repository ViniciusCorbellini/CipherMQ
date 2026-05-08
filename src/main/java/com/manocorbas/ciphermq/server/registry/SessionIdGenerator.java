package com.manocorbas.ciphermq.server.registry;

import java.security.SecureRandom;
import java.util.Base64;

public class SessionIdGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generate() {
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
}