package com.example.pilot.security.certificate;

import com.example.pilot.security.exceptions.KeyException;
import com.example.pilot.security.exceptions.SecurityException;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class Certificate {
    private final String signature_algorithm;
    private final JSONObject jsonCertificate;
    private boolean isVerified;

    public Certificate(byte[] data) throws JSONException {
        jsonCertificate = new JSONObject(new String(data, StandardCharsets.UTF_8));
        // TODO assert valid dates
        isVerified = false;

        // TODO get it from certificate data
        signature_algorithm = "SHA256withRSA";
    }

    private byte[] getVerifiable() throws JSONException {
        JSONObject copy = new JSONObject(jsonCertificate.toString());
        copy.remove("signature");
        return Base64.getEncoder().encode(copy.toString().getBytes(StandardCharsets.UTF_8));
    }

    private byte[] getSignature() throws JSONException {
        String signature = jsonCertificate.getString("signature");
        return Base64.getDecoder().decode(signature);
    }


    public boolean verify(PublicKey verifierPublicKey) throws SignatureException, KeyException {
        try {
            Signature verifier = getSignatureVerifier();
            verifier.initVerify(verifierPublicKey);
            verifier.update(getVerifiable());
            isVerified = verifier.verify(getSignature());
            return isVerified;
        } catch (InvalidKeyException | JSONException e) {
            e.printStackTrace();
            throw new KeyException("Failed to use key " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new SignatureException("Failed to verify. " + e.getMessage());
        }

    }


    private PublicKey getSubjectPublicKey() throws SecurityException, KeyException {
        if (!isVerified)
            throw new SecurityException("Can't use public key from unverified source");

        try {
            JSONObject subjectParams = jsonCertificate.getJSONObject("subject_params");
            JSONObject subjectPublicKey = jsonCertificate.getJSONObject("subject_public_key");

            // TODO assert supported subject params

            // for now its assumed to be RSA
            return buildRSAPublicKey(subjectParams, subjectPublicKey);

        } catch (InvalidKeySpecException | JSONException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new KeyException("Failed to extract key\n" + e.getMessage());
        }
    }


    public byte[] getEncryptedSessionKey(SecretKey secretKey, String expectedAlgorithm) throws KeyException, SecurityException {
        try {
            PublicKey subjectPublicKey = getSubjectPublicKey();

            Cipher cipher = Cipher.getInstance(expectedAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, subjectPublicKey);

            return cipher.doFinal(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new KeyException("Failed to encrypt secret key " + e.getMessage());
        }
    }

    private PublicKey buildRSAPublicKey(JSONObject subjectParams, JSONObject subjectPublicKey) throws JSONException, NoSuchAlgorithmException, InvalidKeySpecException {
        String n = subjectPublicKey.getString("n");
        String e = subjectPublicKey.getString("e");

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(n), new BigInteger(e));
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(keySpec);
    }

    private Signature getSignatureVerifier() throws NoSuchAlgorithmException {
        return Signature.getInstance(signature_algorithm);
    }

}
