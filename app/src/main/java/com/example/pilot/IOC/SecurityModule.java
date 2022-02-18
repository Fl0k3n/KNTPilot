package com.example.pilot.IOC;

import com.example.pilot.security.MessageSecurityPreprocessor;
import com.example.pilot.security.TCPGuard;
import com.example.pilot.security.UDPGuard;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SecurityModule {
    private final File securityFilesDirectory;
    private final String CAPublicKeyFileName;
    private TCPGuard tcpGuard;
    private UDPGuard udpGuard;

    public SecurityModule(File securityFilesDirectory, String CAPublicKeyFileName) {
        this.securityFilesDirectory = securityFilesDirectory;
        this.CAPublicKeyFileName = CAPublicKeyFileName;
        try {
            this.tcpGuard = new TCPGuard();
            this.udpGuard = new UDPGuard();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create guards");
            // TODO
        }
    }

    @Provides
    @Singleton
    @Named("UDP preprocessor")
    MessageSecurityPreprocessor provideUdpPreprocessor() {
        return new MessageSecurityPreprocessor(udpGuard);
    }

    @Provides
    @Singleton
    @Named("TCP preprocessor")
    MessageSecurityPreprocessor provideTcpPreprocessor() {
        return new MessageSecurityPreprocessor(tcpGuard);
    }

    @Provides
    @Named("CA public key file")
    File provideCAPublicKeyFile() {
        String key = "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx1317n8rtVa0lcj5GERl\n" +
                "FZ4CtW+oWLP/Frqt8lh9E+yQSR+jV5Wj/1yfrV8ybfzU4d+KdR1pejQD8r1mGp6/\n" +
                "0cxrpoRszE6x5H1ZYUBQFvG6Pqdvi/WEp2t9lLrgTCoUs+7KXRb1WUhnB4afGpoW\n" +
                "a9FcqQOluQTi9YPTAEsrzp0u6HqtZUXx52784fvCc78smcTERaRnd5nt01Z6Sz0a\n" +
                "lsjYtnkH2ZycIYHTsql8uHvHmrogIO4IJvstv7YO4k8/AeVvWENS07BnTS2tnNhu\n" +
                "5URWIS+ybs4L9owFxweKUw5MUnBIKRwehQEa47L1naBIXSwkWdNja+VXvy6sXtRl\n" +
                "UwIDAQAB\n" +
                "-----END PUBLIC KEY-----";

        File keyFile = new File(securityFilesDirectory, CAPublicKeyFileName);

        try (FileOutputStream fileOutputStream = new FileOutputStream(keyFile);
             DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream)) {
            dataOutputStream.writeBytes(key);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("failed to save key");
        }

        return keyFile;
    }

    @Provides
    TCPGuard provideTCPGuard() {
        return tcpGuard;
    }

}
