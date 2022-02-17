package com.example.pilot.security.certificate;


import com.example.pilot.security.exceptions.KeyException;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;

public class CertificateVerifier {
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SUPPORTED_KEY_ENCODING = "pem";

    private final File CAPublicKeyFile;

    @Inject
    public CertificateVerifier(@Named("CA public key file") File CAPublicKeyFile) {
        this.CAPublicKeyFile = CAPublicKeyFile;

        if (!CAPublicKeyFile.getName().endsWith(SUPPORTED_KEY_ENCODING))
            throw new IllegalArgumentException("Public key format is not supported, expected " +
                    SUPPORTED_KEY_ENCODING + ",got " + CAPublicKeyFile.getName());
    }

    public boolean verify(Certificate certificate) throws KeyException {
        try {
            PublicKey CA_publicKey = loadCAPublicKey();

            return certificate.verify(CA_publicKey);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException exception) {
            exception.printStackTrace();
            throw new KeyException(exception.getMessage());
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return false;
    }

    private PublicKey loadCAPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] pubKeyFileData = new byte[(int) CAPublicKeyFile.length()];

        try (FileInputStream fileInputStream = new FileInputStream(CAPublicKeyFile);
             DataInputStream dataStream = new DataInputStream(fileInputStream)) {
            dataStream.readFully(pubKeyFileData);
        }

        byte[] decodedKey = decodePublicKey(pubKeyFileData);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);

        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

        return keyFactory.generatePublic(keySpec);
    }

    private byte[] decodePublicKey(byte[] encodedKey) {
        String utf8Key = new String(encodedKey, StandardCharsets.US_ASCII);
        utf8Key = utf8Key.replace("-----BEGIN PUBLIC KEY-----", "");
        utf8Key = utf8Key.replace("-----END PUBLIC KEY-----", "");
        utf8Key = utf8Key.replace("\n", "");

        return Base64.getDecoder().decode(utf8Key);
    }


}
