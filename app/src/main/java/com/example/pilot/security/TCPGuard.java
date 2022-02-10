package com.example.pilot.security;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class TCPGuard {
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256;
    private final SecretKey sessionKey;


    public TCPGuard() throws NoSuchAlgorithmException {
        this.sessionKey = generateKey();
    }

    private SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
        keyGenerator.init(KEY_LENGTH);
        return keyGenerator.generateKey();
    }

    public SecretKey getSessionKey() {
        return sessionKey;
    }
}
