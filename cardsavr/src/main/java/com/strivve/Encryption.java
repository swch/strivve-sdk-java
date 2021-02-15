package com.strivve;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class Encryption {

    
    private Encryption() {}

    public static String encryptAES256(String strToEncrypt, Key secretKey) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8))) + '$' + Base64.getEncoder().encodeToString(iv);
    }

    public static String decryptAES256(String strToDecrypt, Key secretKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String[] items = strToDecrypt.split("\\$");
        IvParameterSpec ivspec = new IvParameterSpec(Base64.getDecoder().decode(items[1]));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
        return new String(cipher.doFinal(Base64.getDecoder().decode(items[0])));
    }

    public static SecretKey convertRawToAESKey(byte[] raw) {
        return new SecretKeySpec(raw, "AES");
    }

    public static String hmacSign(byte[] input, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretkey = new SecretKeySpec(key, "HmacSHA256");
        sha256HMAC.init(secretkey);
        return Base64.getEncoder().encodeToString(sha256HMAC.doFinal(input));
    }

    public static KeyPair generateECKeys() throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException {
       
        String ecdhCurvenameString = "secp256r1";
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "SunEC");
        ECGenParameterSpec ecParameterSpec = new ECGenParameterSpec(ecdhCurvenameString);
        keyPairGenerator.initialize(ecParameterSpec);
        return keyPairGenerator.genKeyPair();
    }

    public static SecretKey generateECSecretKey(Key serverPublicKey, Key clientPrivateKey)
            throws NoSuchAlgorithmException, InvalidKeyException {

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(clientPrivateKey);
        ka.doPhase(serverPublicKey, true);
        return convertRawToAESKey(ka.generateSecret());
    }

    public static String generateSignedPasswordKey(String password, String username, byte[] salt)
        throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        byte[] passwordKey = sha256pbkdf2(password, sha256Hash(username.getBytes()), 5000);
        return hmacSign(salt, passwordKey);
    }

    public static byte[] sha256pbkdf2(String password, byte[] salt, int iterations)
        throws NoSuchAlgorithmException, InvalidKeySpecException {

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 32 * 8);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return f.generateSecret(spec).getEncoded();
    }

    public static byte[] sha256Hash(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input);
    }

}
