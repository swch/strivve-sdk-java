package com.strivve;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.crypto.SecretKey;

public class Signing {

    private Signing() {}

    public static String signRequest(String path, String authorization, String nonce, SecretKey encryptionKey, String body) 
        throws InvalidKeyException, NoSuchAlgorithmException {

        String requestSigning = path + authorization + nonce;
        if (body != null) {
            requestSigning += body;
        }
        return Encryption.hmacSign(requestSigning.getBytes(), encryptionKey.getEncoded());                
    }

    /* Why an encryptionKey array?  If for some reason keys get rotated, it will come in handy to have both the current and old key */
    public static boolean verifySignature(String signature, String path, String authorization, String nonce, SecretKey[] encyrptionKeys, String body) 
        throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        
        for (SecretKey encryptionKey : encyrptionKeys) {
            String sig = signRequest(path, authorization, nonce, encryptionKey, body);
            if (sig.equals(signature)) {
                return true;
            }
        }
        throw new SignatureException();
    }

}
