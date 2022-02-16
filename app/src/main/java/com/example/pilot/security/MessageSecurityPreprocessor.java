package com.example.pilot.security;

import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;
import com.example.pilot.security.utils.TLSCode;
import com.example.pilot.security.utils.TLSPacket;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class MessageSecurityPreprocessor {
    private final Guard guard;

    public MessageSecurityPreprocessor(Guard guard) {
        this.guard = guard;
    }

    public byte[] preprocessToSend(byte[] messageToSend) throws SecurityException {
        byte[] nonce = guard.getNonce();
        TLSPacket tlsPacket = new TLSPacket(TLSCode.SECURE, (short) messageToSend.length, nonce.length, nonce, messageToSend);

        System.out.println("sending len: " + messageToSend.length);

        byte[] encryptedData = guard.encrypt(tlsPacket.data, tlsPacket.header, nonce);

        return new TLSPacket(tlsPacket.header, encryptedData).full;
    }

    public byte[] preprocessReceived(byte[] receivedMessage) throws SecurityException, AuthenticationException {
        TLSPacket tlsPacket = new TLSPacket(receivedMessage);

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
}
