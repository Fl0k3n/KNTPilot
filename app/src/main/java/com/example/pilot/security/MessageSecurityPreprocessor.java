package com.example.pilot.security;

import java.nio.charset.StandardCharsets;

public class MessageSecurityPreprocessor {
    private final TLSHandler tlsHandler;
    private final Guard guard;

    public MessageSecurityPreprocessor(TLSHandler tlsHandler, Guard guard) {
        this.tlsHandler = tlsHandler;
        this.guard = guard;
    }

    public byte[] preprocessToSend(byte[] messageToSend) throws SecurityException {
        byte[] nonce = tlsHandler.getNonce(guard.getNonceLength());
        byte[] header = tlsHandler.buildHeader(TLSCode.SECURE, messageToSend.length, nonce);

        System.out.println("sending len: " + messageToSend.length);

        byte[] encryptedData = guard.encrypt(messageToSend, header, nonce);

        return new Message(header, encryptedData).full;
    }

    public byte[] preprocessReceived(byte[] receivedMessage) throws SecurityException, AuthenticationException {
        int headerLength = tlsHandler.getTotalHeaderLength(receivedMessage);
        Message msg = new Message(receivedMessage, headerLength);

        byte[] nonce = tlsHandler.extractNonceFromHeader(msg.header);

        if (nonce.length != guard.getNonceLength())
            throw new SecurityException("Invalid nonce length, expected " + guard.getNonceLength() +
                    " got " + nonce.length + "\nFrom: " + new String(msg.full, StandardCharsets.UTF_8));

        return guard.decrypt(msg.data, msg.header, nonce);
    }

}
