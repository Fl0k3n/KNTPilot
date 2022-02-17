package com.example.pilot.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public abstract class AbstractGuard implements Guard {
    protected SecretKey sessionKey;
    protected final SecureRandom nonceGenerator;
    private final int nonceLength;
    private final int macLength;

    public AbstractGuard(int nonceLength, int keyLengthBytes, int macLength, String keyAlgorithm) throws NoSuchAlgorithmException {
        this(nonceLength, macLength);
        sessionKey = generateKey(keyAlgorithm, keyLengthBytes);
    }

    public AbstractGuard(int nonceLength, int macLength) {
        this.nonceLength = nonceLength;
        this.macLength = macLength;
        nonceGenerator = new SecureRandom();
    }

    @Override
    public SecretKey getSessionKey() {
        return sessionKey;
    }

    @Override
    public int getNonceLength() {
        return nonceLength;
    }


    @Override
    public int getTagLength() {
        return macLength;
    }

    protected SecretKey generateKey(String keyAlgorithm, int keyLengthBytes) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(keyAlgorithm);
        keyGenerator.init(keyLengthBytes * 8);
        return keyGenerator.generateKey();
    }

    @Override
    public byte[] getNonce() {
        byte[] nonce = new byte[getNonceLength()];
        nonceGenerator.nextBytes(nonce);
        return nonce;
    }
}
