package com.example.pilot.security;

import org.json.JSONException;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.TreeSet;

public class CertificateVerifier {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SUPPORTED_KEY_ENCODING = "pem";

    private final File CAPublicKeyFile;

    public CertificateVerifier(File CAPublicKeyFile) {
        this.CAPublicKeyFile = CAPublicKeyFile;

        TreeSet<String> algorithms = new TreeSet<>();
        for (Provider provider : Security.getProviders())
            for (Provider.Service service : provider.getServices())
                if (service.getType().equals("Signature"))
                    algorithms.add(service.getAlgorithm());
        for (String algorithm : algorithms)
            System.out.println(algorithm);

        if (!CAPublicKeyFile.getName().endsWith(SUPPORTED_KEY_ENCODING))
            throw new IllegalArgumentException("Public key format is not supported, expected " +
                    SUPPORTED_KEY_ENCODING + ",got " + CAPublicKeyFile.getName());
    }

    public boolean verify(Certificate certificate) throws KeyException {
        try {
            PublicKey CA_publicKey = loadCAPublicKey();

            Signature verifier = getSignatureVerifier();

            certificate.setupVerifierParams(verifier);

            return certificate.verify(verifier, CA_publicKey);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException exception) {
            exception.printStackTrace();
            throw new KeyException(exception.getMessage());
        } catch (SignatureException | JSONException e) {
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

        String utf8Key = new String(pubKeyFileData, StandardCharsets.US_ASCII);
        utf8Key = utf8Key.replace("-----BEGIN PUBLIC KEY-----", "");
        utf8Key = utf8Key.replace("-----END PUBLIC KEY-----", "");
        utf8Key = utf8Key.replace("\n", "");


        byte[] decodedKey = Base64.getDecoder().decode(utf8Key);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);

        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

        return keyFactory.generatePublic(keySpec);
    }

    private Signature getSignatureVerifier() throws NoSuchAlgorithmException {
        return Signature.getInstance(SIGNATURE_ALGORITHM);
    }
}
