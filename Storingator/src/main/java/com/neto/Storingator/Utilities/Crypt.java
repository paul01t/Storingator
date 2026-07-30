package com.neto.Storingator.Utilities;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Random;

import com.neto.Storingator.Entities.Appuser;

public class Crypt {
    public static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?";
    private static final int taglengh = 16;
    private static final int ivlength = 12;

    public static byte[] getRandomNonce() {
        byte[] nonce = new byte[ivlength];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    public static byte[] keyGenerateEncoded() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey key = keyGenerator.generateKey();
        return key.getEncoded();
    }

    public static byte[] getCorrectSizeKey(String temp) {
        byte[] bytetemp = temp.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = bytetemp[i];
        }
        return key;
    }

    public static byte[] keyGenerateEncodedFromUser(Appuser s, TempUser j) throws NoSuchAlgorithmException {
        String temp = s.getTimestamp() + j.getClearPassword() + s.getUsername();
        if (temp.length() < 32) {
            temp = temp + j.getClearPassword();
        }
        return getCorrectSizeKey(temp);
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(taglengh * 8, IV);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        return cipher.doFinal(plaintext);
    }

    public static String decrypt(byte[] cipherText, byte[] key, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, IV);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
        byte[] decryptedText = cipher.doFinal(cipherText);
        return new String(decryptedText);
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static String randomPassphrase(int length, String supportedCharset) {
        StringBuilder x = new StringBuilder();
        Random rand = new Random();
        int int_random = rand.nextInt(supportedCharset.length());
        for (int i = 0; i < length; i++) {
            x.append(supportedCharset.charAt(int_random));
            int_random = rand.nextInt(supportedCharset.length());
        }
        return x.toString();
    }
}