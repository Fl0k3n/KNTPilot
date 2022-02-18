package com.example.pilot.security;

import android.util.Log;

import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;
import com.example.pilot.security.utils.TLSCode;
import com.example.pilot.security.utils.TLSPacket;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class MessageSecurityPreprocessor {
    private final static String TAG = "Message Security Preprocessor";
    private final Guard guard;

    public MessageSecurityPreprocessor(Guard guard) {
        this.guard = guard;
    }

    public byte[] preprocessToSend(byte[] messageToSend) throws SecurityException {
        byte[] nonce = guard.getNonce();
        TLSPacket tlsPacket = new TLSPacket(TLSCode.SECURE, (short) messageToSend.length, nonce.length, nonce, messageToSend);

        Log.d(TAG, "sending len: " + messageToSend.length);

        byte[] encryptedData = guard.encrypt(tlsPacket.data, tlsPacket.header, nonce);

        return new TLSPacket(tlsPacket.header, encryptedData).full;
    }

    // returns decrypted data from underlying protocol, with all security-layer data stripped
    public byte[] preprocessReceived(byte[] receivedMessage) throws SecurityException, AuthenticationException {
        TLSPacket tlsPacket = new TLSPacket(receivedMessage);

        if (tlsPacket.code != TLSCode.SECURE)
            throw new SecurityException("Expected encrypted packet but got code " + tlsPacket.code);

        byte[] nonce = tlsPacket.nonce;

        if (nonce.length != guard.getNonceLength())
            throw new SecurityException("Invalid nonce length, expected " + guard.getNonceLength() +
                    " got " + nonce.length + "\nFrom: " + new String(tlsPacket.full, StandardCharsets.UTF_8));

        return guard.decrypt(tlsPacket.data, tlsPacket.header, nonce);
    }


    public int getBasicHeaderSize() {
        return TLSPacket.HEADER_SIZE;
    }

    public int getMessageSize(byte[] basicHeader) {
        // first HEADER_SIZE bytes
        return TLSPacket.getMessageSize(basicHeader, guard.getTagLength());
    }

    public void setKey(byte[] decoded) {
        guard.setSessionKey(decoded);
    }
}
