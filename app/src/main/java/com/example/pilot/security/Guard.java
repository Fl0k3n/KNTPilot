package com.example.pilot.security;

import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;

import javax.crypto.SecretKey;

public interface Guard {
    SecretKey getSessionKey();

    // if guard support MAC then tag will be appended to the end of returned data
    byte[] encrypt(byte[] message, byte[] aad, byte[] nonce) throws SecurityException;

    // if guard support MAC then decryption expects tag appended to the end of message
    byte[] decrypt(byte[] message, byte[] aad, byte[] nonce) throws SecurityException, AuthenticationException;

    int getNonceLength();

    int getTagLength();
}
