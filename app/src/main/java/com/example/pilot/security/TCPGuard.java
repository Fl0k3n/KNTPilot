package com.example.pilot.security;

import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

public class TCPGuard extends AbstractGuard{
    private static final String KEY_ALGORITHM = "AES";
    private static final String SYMMETRIC_ALGORITHM = "AES_256/GCM/NoPadding";
    private static final int MAC_LENGTH = 16; // bytes
    private static final int NONCE_LENGTH = 12;
    private static final int KEY_LENGTH = 32;

    public TCPGuard() throws NoSuchAlgorithmException {
        super(NONCE_LENGTH, KEY_LENGTH, MAC_LENGTH, KEY_ALGORITHM);
    }


    @Override
    public void setSessionKey(byte[] keyBytes) {
        sessionKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    @Override
    public byte[] encrypt(byte[] message, byte[] aad, byte[] nonce) throws SecurityException {
        Cipher cipher = initCipher(nonce, Cipher.ENCRYPT_MODE, aad);

        try {
            return cipher.doFinal(message);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            throw new SecurityException("Failed to encrypt message " + e.getMessage());
        }
    }

    @Override
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
}
