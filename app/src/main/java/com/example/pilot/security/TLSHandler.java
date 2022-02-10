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

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class TLSHandler {
    private static final int HEADER_SIZE = 4;
    // RSA PKCS#1 OAEP
    private static final String SUBJECT_ASYMMETRIC_ENCRYPTION_ALGORITHM = "RSA/NONE/OAEPWithSHA1AndMGF1Padding";

    private Socket serverSocket;
    private final TCPGuard tcpGuard;
    private final CertificateVerifier certificateVerifier;

    public TLSHandler(CertificateVerifier certificateVerifier, TCPGuard tcpGuard) {
        this.certificateVerifier = certificateVerifier;
        this.tcpGuard = tcpGuard;
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
        byteBuffer.put((byte) 0); // reserved

        byteBuffer.put(data);
        OutputStream outputStream = serverSocket.getOutputStream();
        outputStream.write(byteBuffer.array());
        outputStream.flush();
    }

}
