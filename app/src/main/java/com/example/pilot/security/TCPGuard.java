package com.example.pilot.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TCPGuard implements Guard{
    private static final String KEY_ALGORITHM = "AES";
    private static final String SYMMETRIC_ALGORITHM = "AES_256/GCM/NoPadding";
    private static final int MAC_LENGTH = 16; // bytes
    private static final int NONCE_LENGTH = 12;
    private static final int KEY_LENGTH = 32;
    private final SecretKey sessionKey;


    public TCPGuard() throws NoSuchAlgorithmException {
        this.sessionKey = generateKey();
    }


    private SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(KEY_LENGTH * 8);
        return keyGenerator.generateKey();
    }

    public SecretKey getSessionKey() {
        return sessionKey;
    }

    public byte[] encrypt(byte[] message, byte[] aad, byte[] nonce) throws SecurityException {
        Cipher cipher = initCipher(nonce, Cipher.ENCRYPT_MODE, aad);

        try {
            return cipher.doFinal(message);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            throw new SecurityException("Failed to encrypt message " + e.getMessage());
        }
    }

    public byte[] decrypt(byte[] message, byte[] aad, byte[] nonce) throws SecurityException, AuthenticationException {
        Cipher cipher = initCipher(nonce, Cipher.DECRYPT_MODE, aad);

        try {
            return cipher.doFinal(message);
        } catch (AEADBadTagException tagException) {
            throw new AuthenticationException("MAC authentication failed " + tagException.getMessage());
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            throw new SecurityException("Failed to decrypt message " + e.getMessage());
        }
    }

    @Override
    public int getNonceLength() {
        return NONCE_LENGTH;
    }

    @Override
    public int getTagLength() {
        return MAC_LENGTH;
    }

    private Cipher initCipher(byte[] nonce, int mode, byte[] aad) throws SecurityException {
        try {
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(sessionKey.getEncoded(), KEY_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(MAC_LENGTH * 8, nonce);
            cipher.init(mode, keySpec, gcmParameterSpec);
            cipher.updateAAD(aad);

            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
            throw new SecurityException("Failed to initialise cipher " + e.getMessage());
        }
    }

    private void testIt() {

        //decryption
        try {
            byte[] nonce = "123456789012".getBytes();
            byte[] header = "len=12".getBytes();
            byte[] key = "12345678901234561234567890123456".getBytes();
            byte[] encrypted = Base64.getDecoder().decode("BlTN8A==");
            byte[] tag = Base64.getDecoder().decode("W+Pol0NjuhbDoQKvMzDiFQ==");

            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(MAC_LENGTH * 8, nonce);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            cipher.updateAAD(header);
            byte[] msg = new byte[encrypted.length + tag.length];
            System.arraycopy(encrypted, 0, msg, 0, encrypted.length);
            System.arraycopy(tag, 0, msg, encrypted.length, tag.length);

            byte[] data = cipher.doFinal(msg);
            System.out.println("decrypted: "  + new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private void testIt2() {

        //encryption
        try {
            byte[] nonce = "123456789012".getBytes();
            byte[] header = "len=12".getBytes();
            byte[] key = "12345678901234561234567890123456".getBytes();
            byte[] data = "kntp2".getBytes();

            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(MAC_LENGTH * 8, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            cipher.updateAAD(header);

            byte[] ciphertext_tag = cipher.doFinal(data);
            System.out.println("ENRYPTED:");
            System.out.println(new String(Base64.getEncoder().encode(ciphertext_tag), StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed: " + e.getMessage());
        }
    }
}
