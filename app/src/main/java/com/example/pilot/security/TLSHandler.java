package com.example.pilot.security;

import com.example.pilot.security.certificate.Certificate;
import com.example.pilot.security.certificate.CertificateVerifier;
import com.example.pilot.security.exceptions.KeyException;
import com.example.pilot.security.exceptions.SecurityException;
import com.example.pilot.security.utils.TLSCode;
import com.example.pilot.security.utils.TLSPacket;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;

public class TLSHandler {
    // RSA PKCS#1 OAEP, this should be negotiated, TODO
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
        int headerSize = TLSPacket.HEADER_SIZE;
        byte[] headerBuff = new byte[headerSize]; // TODO
        int totalRead = 0;

        while (totalRead < headerSize) {
            totalRead += serverSocket.getInputStream().read(headerBuff, totalRead, headerSize - totalRead);
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

        return certBuff;
    }

    private void sendHello() throws IOException {
        sendTLSInitData(TLSCode.HELLO, new byte[0]);
    }

    private void sendTLSInitData(TLSCode code, byte[] data) throws IOException {
        TLSPacket packet = new TLSPacket(code, (short) data.length, 0, new byte[0], data);

        OutputStream outputStream = serverSocket.getOutputStream();
        outputStream.write(packet.full);
        outputStream.flush();
    }




}
