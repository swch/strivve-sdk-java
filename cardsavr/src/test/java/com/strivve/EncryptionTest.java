package com.strivve;

import static org.junit.Assert.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.util.Arrays;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class EncryptionTest {
    public final static String stringToEncrypt = "This is a string to test";
    public final static String username = "username";
    public final static String password = "password";
    public final static byte[] salt = "Ouz/hj6TER8Sf+nI2t8h3PICweZ0Jb2o8c4OySIjIIU=".getBytes();
    public final static SecretKey integratorKey = Encryption
        .convertRawToAESKey(Base64.getDecoder().decode("OkzxB4HQit43k3OEbSYTAPG5hf96erAAlhb4prAjH/I="));

    /**
     * Rigorous Test :-)
     */
    @Test
    public void sha256Hash() throws NoSuchAlgorithmException {
        byte[] enc = Encryption.sha256Hash(stringToEncrypt.getBytes());
        assertEquals("phNlTjUfBv63D27bmLEIeIYHqe9zbSVaF0K3EFvbHUA=", Base64.getEncoder().encodeToString(enc));
    }

    @Test
    public void sha256pbkdf2() throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] enc = Encryption.sha256pbkdf2(password, salt, 5000);
        assertEquals("ykWREt8EZn335UvJXPcSmRkU13ca4VKuvnRRj07SVyo=", Base64.getEncoder().encodeToString(enc));
    }

    @Test
    public void generateSignedPasswordKey()
            throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
        String key = Encryption.generateSignedPasswordKey(password, username, salt);
        assertEquals("BCWnfGG2Hb70aFXne6A2pFCZgn8NCSjENwOZVf8L9ko=", key);
    }

    @Test
    public void testECKeys() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchProviderException, InvalidAlgorithmParameterException {

        KeyPair kp1 = Encryption.generateECKeys(); // server
        KeyPair kp2 = Encryption.generateECKeys(); // client

        // Unnecessary to test, but this is what we have to do in reality since the
        // public keys are Bas64 encoded raw keys
        String kp1PublicKeyString = Base64.getEncoder()
                .encodeToString(UncompressedPublicKeys.encodeUncompressedECPublicKey((ECPublicKey) kp1.getPublic()));
        ECPublicKey kp1PublicKey = UncompressedPublicKeys.decodeUncompressedECPublicKey(
                ((ECPublicKey) kp2.getPublic()).getParams(), Base64.getDecoder().decode(kp1PublicKeyString));

        SecretKey encryptionKey2 = Encryption.generateECSecretKey(kp1PublicKey, kp2.getPrivate());

        String kp2PublicKeyString = Base64.getEncoder()
                .encodeToString(UncompressedPublicKeys.encodeUncompressedECPublicKey((ECPublicKey) kp2.getPublic()));
        ECPublicKey kp2PublicKey = UncompressedPublicKeys.decodeUncompressedECPublicKey(kp1PublicKey.getParams(),
                Base64.getDecoder().decode(kp2PublicKeyString));

        SecretKey encryptionKey1 = Encryption.generateECSecretKey(kp2PublicKey, kp1.getPrivate());

        assertTrue(Arrays.equals(encryptionKey1.getEncoded(), encryptionKey2.getEncoded()));

    }

    @Test
    public void hmacSign() throws InvalidKeyException, NoSuchAlgorithmException {
        String signature = Encryption.hmacSign(stringToEncrypt.getBytes(), integratorKey.getEncoded());
        assertEquals("/G/EmFauLHppkvXkX7wN2amzGCMGEV4CS0I37L1jG5Q=", signature);
    }

    @Test
    public void convertRawToAESKey() {
        SecretKey sk = Encryption.convertRawToAESKey(integratorKey.getEncoded());
        assertTrue(Arrays.equals(sk.getEncoded(), integratorKey.getEncoded()));
    }

    @Test
    public void AESEncryption() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        SecretKey sk = Encryption.convertRawToAESKey(integratorKey.getEncoded());
        String encrypted = Encryption.encryptAES256(stringToEncrypt, sk);
        String decrypted = Encryption.decryptAES256(encrypted, sk);
        assertEquals(stringToEncrypt, decrypted);
    }


}
