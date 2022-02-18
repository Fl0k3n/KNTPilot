package com.example.pilot.security;

import android.util.Log;

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

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TLSHandler {
    private static final String TAG = "TLS Handler";
    // RSA PKCS#1 OAEP, this should be negotiated, TODO
    private static final String SUBJECT_ASYMMETRIC_ENCRYPTION_ALGORITHM = "RSA/NONE/OAEPWithSHA1AndMGF1Padding";

    private Socket serverSocket;

    private final TCPGuard tcpGuard;
    private final CertificateVerifier certificateVerifier;

    @Inject
    public TLSHandler(CertificateVerifier certificateVerifier, TCPGuard tcpGuard) {
        this.certificateVerifier = certificateVerifier;
        this.tcpGuard = tcpGuard;
    }

    public void establishSecureChannel(Socket serverSocket) throws IOException, SecurityException {
        this.serverSocket = serverSocket;

        Log.i(TAG, "sending hello");

        sendHello();

        Log.i(TAG, "sent hello");

        byte[] certBytes = awaitCertificate();

        Log.i(TAG, "got certificiate");

        Certificate certificate = assertValidCertificate(certBytes);

        Log.i(TAG, "certificate valid, sending secret key");

        sendEncryptedSessionKey(certificate);

        Log.i(TAG, "secret key sent, secure channel established successfully");
    }


    private void sendEncryptedSessionKey(Certificate certificate) throws SecurityException {
        try {
            byte[] key = certificate.getEncryptedSessionKey(tcpGuard.getSessionKey(), SUBJECT_ASYMMETRIC_ENCRYPTION_ALGORITHM);
            sendTLSInitData(TLSCode.SECRET, key);
        } catch (KeyException | IOException e) {
            Log.w(TAG, e);
            throw new SecurityException(e.getMessage());
        }
    }


    private Certificate assertValidCertificate(byte[] certBytes) throws SecurityException {
        try {
            Certificate certificate = new Certificate(certBytes);
            if (certificateVerifier.verify(certificate)) {
                return certificate;
            }

            throw new SecurityException("Received Invalid certificate");
        } catch (JSONException | KeyException e) {
            Log.e(TAG, "Failed to verify certificate" , e);
            throw new SecurityException("Failed to process certificate");
        }
    }

    private byte[] awaitCertificate() throws IOException {
        int headerSize = TLSPacket.HEADER_SIZE;
        byte[] headerBuff = new byte[headerSize];
        int totalRead = 0;

        while (totalRead < headerSize) {
            totalRead += serverSocket.getInputStream().read(headerBuff, totalRead, headerSize - totalRead);
        }

        ByteBuffer headerByteBuffer = ByteBuffer.wrap(headerBuff);
        int code = headerByteBuffer.get();
        short certSize = headerByteBuffer.getShort();

        TLSCode tlsCode = TLSCode.fromInteger(code);
        if (!tlsCode.equals(TLSCode.CERTIFICATE)) {
            Log.w(TAG, "TLS got unexpected code while waiting for certificate" + tlsCode);
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
