package com.example.pilot.security;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class TLSHandler {
    private static final int HEADER_SIZE = 4;
    // RSA PKCS#1 OAEP, this should be negotiated, TODO
    private static final String SUBJECT_ASYMMETRIC_ENCRYPTION_ALGORITHM = "RSA/NONE/OAEPWithSHA1AndMGF1Padding";

    private Socket serverSocket;
    private final TCPGuard tcpGuard;
    private final CertificateVerifier certificateVerifier;
    private final SecureRandom nonceGenerator;

    public TLSHandler(CertificateVerifier certificateVerifier, TCPGuard tcpGuard) {
        this.certificateVerifier = certificateVerifier;
        this.tcpGuard = tcpGuard;
        this.nonceGenerator = new SecureRandom();
    }

    public void establishSecureChannel(Socket serverSocket) throws IOException, SecurityException {
        this.serverSocket = serverSocket;
        System.out.println("sending hello");
        sendHello();
        System.out.println("sent hello");
        byte[] certBytes = awaitCertificate();
        System.out.println("got certificiate");
        Certificate certificate = assertValidCertificate(certBytes);
        System.out.println("sending secret key");
        sendEncryptedSessionKey(certificate);
    }


    private void sendEncryptedSessionKey(Certificate certificate) throws SecurityException {
        try {
            byte[] key = certificate.getEncryptedSessionKey(tcpGuard.getSessionKey(), SUBJECT_ASYMMETRIC_ENCRYPTION_ALGORITHM);
            sendTLSInitData(TLSCode.SECRET, key);
        } catch (KeyException | IOException e) {
            e.printStackTrace();
            throw new SecurityException(e.getMessage());
        }
    }


    private Certificate assertValidCertificate(byte[] certBytes) throws SecurityException {
        try {
            Certificate certificate = new Certificate(certBytes);
            if (certificateVerifier.verify(certificate)) {
                System.out.println("OK certificate is valid");
                return certificate;
            }
            else {
                System.out.println("Certificate invalid");
                throw new SecurityException("Received Invalid certificate");
            }
        } catch (JSONException | KeyException jsonException) {
            jsonException.printStackTrace();
            throw new SecurityException("Failed to process certificate");
        }
    }

    private byte[] awaitCertificate() throws IOException {
        byte[] headerBuff = new byte[HEADER_SIZE]; // TODO
        int totalRead = 0;

        while (totalRead < HEADER_SIZE) {
            totalRead += serverSocket.getInputStream().read(headerBuff, totalRead, HEADER_SIZE - totalRead);
        }

        ByteBuffer headerByteBuffer = ByteBuffer.wrap(headerBuff);
        int code = headerByteBuffer.get();
        short certSize = headerByteBuffer.getShort();

        TLSCode tlsCode = TLSCode.fromInteger(code);
        if (!tlsCode.equals(TLSCode.CERTIFICATE)) {
            System.out.println("Received wrong code " + code); // TODO
            return awaitCertificate();
        }

        byte[] certBuff = new byte[certSize];

        totalRead = 0;
        while (totalRead < certSize) {
            totalRead += serverSocket.getInputStream().read(certBuff, totalRead, certSize - totalRead);
        }

        System.out.println("OK got certificate");
        return certBuff;
    }

    private void sendHello() throws IOException {
        sendTLSInitData(TLSCode.HELLO, new byte[0]);
    }

    private void sendTLSInitData(TLSCode code, byte[] data) throws IOException {
        System.out.println(data.length);
        ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_SIZE + data.length);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put((byte)code.ordinal());
        byteBuffer.putShort((short) data.length);
        byteBuffer.put((byte) 0); // no nonce

        byteBuffer.put(data);
        System.out.println("SENDING data with length: " + byteBuffer.array().length + " and data length: " + data.length);
        OutputStream outputStream = serverSocket.getOutputStream();
        outputStream.write(byteBuffer.array());
        outputStream.flush();
    }

    public byte[] buildHeader(TLSCode code, int size, byte[] nonce) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_SIZE + nonce.length);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put((byte) code.ordinal());
        byteBuffer.putShort((short) size);
        byteBuffer.put((byte) nonce.length);

        byteBuffer.put(nonce);

        return byteBuffer.array();
    }

    public byte[] getNonce(int len) {
        byte[] nonce = new byte[len];
        nonceGenerator.nextBytes(nonce);
        return nonce;
    }


    public int getBasicHeaderSize() {
        return HEADER_SIZE;
    }


    public int getMessageSize(byte[] basicHeader) {
        // first HEADER_SIZE bytes
        ByteBuffer byteBuffer = ByteBuffer.wrap(basicHeader);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        int messageSize = (int) byteBuffer.getShort(1) + extractNonceLength(basicHeader);
        if (TLSCode.fromInteger(byteBuffer.get(0)) == TLSCode.SECURE)
            messageSize += tcpGuard.getTagLength();

        return messageSize;
    }

    private int extractNonceLength(byte[] packet) {
        return (int) packet[3];
    }

    public int getTotalHeaderLength(byte[] packet) {
        return HEADER_SIZE + extractNonceLength(packet);
    }

    public byte[] extractNonceFromHeader(byte[] header) {
        byte[] nonce = new byte[header.length - HEADER_SIZE];
        System.arraycopy(header, HEADER_SIZE, nonce, 0, header.length - HEADER_SIZE);
        return nonce;
    }

}
