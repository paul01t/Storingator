package com.neto.Storingator.Utilities;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
    public static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}|;:\'\",<.>/?";
    private static final int taglengh = 16;
    private static final int ivlength = 12;
    private static final int saltlength = 16;
    private static final int pbkdf2Iterations = 600000;
    private static final int keyBits = 256;

    public static byte[] getRandomNonce() {
        byte[] nonce = new byte[ivlength];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    public static byte[] getRandomSalt() {
        byte[] salt = new byte[saltlength];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] deriveKeyFromPassphrase(String passphrase, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, pbkdf2Iterations, keyBits);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
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

    public static String randomPassphrase(int length, String supportedCharset) {
        StringBuilder x = new StringBuilder();
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int int_random = rand.nextInt(supportedCharset.length());
            x.append(supportedCharset.charAt(int_random));
        }
        return x.toString();
    }
}