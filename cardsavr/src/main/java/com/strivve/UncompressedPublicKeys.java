package com.strivve;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class UncompressedPublicKeys {

    private UncompressedPublicKeys() {}

    public static ECPublicKey decodeUncompressedECPublicKey(ECParameterSpec params, final byte[] pubkey) 
        throws NoSuchAlgorithmException, InvalidKeySpecException {

        int keySizeBytes = params.getOrder().bitLength() / Byte.SIZE;

        int offset = 1;
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(pubkey, offset, offset + keySizeBytes));
        offset += keySizeBytes;
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(pubkey, offset, offset + keySizeBytes));
        ECPoint w = new ECPoint(x, y);

        ECPublicKeySpec otherKeySpec = new ECPublicKeySpec(w, params);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return (ECPublicKey) keyFactory.generatePublic(otherKeySpec);
    }

    public static byte[] encodeUncompressedECPublicKey(ECPublicKey ecPublicKey) {
        int keyLengthBytes = ecPublicKey.getParams().getOrder().bitLength() / Byte.SIZE;
        byte[] publicKeyEncoded = new byte[2 * keyLengthBytes + 1];
        publicKeyEncoded[0] = 0x04; 

        int offset = 1;

        BigInteger x = ecPublicKey.getW().getAffineX();
        byte[] xba = x.toByteArray();
        if (xba.length > keyLengthBytes + 1 || xba.length == keyLengthBytes + 1 && xba[0] != 0) {
            throw new IllegalStateException("X coordinate of EC public key has wrong size");
        }

        if (xba.length == keyLengthBytes + 1) {
            System.arraycopy(xba, 1, publicKeyEncoded, offset, keyLengthBytes);
        } else {
            System.arraycopy(xba, 0, publicKeyEncoded, offset + keyLengthBytes - xba.length, xba.length);
        }
        offset += keyLengthBytes;

        BigInteger y = ecPublicKey.getW().getAffineY();
        byte[] yba = y.toByteArray();
        if (yba.length > keyLengthBytes + 1 || yba.length == keyLengthBytes + 1 && yba[0] != 0) {
            throw new IllegalStateException("Y coordinate of EC public key has wrong size");
        }

        if (yba.length == keyLengthBytes + 1) {
            System.arraycopy(yba, 1, publicKeyEncoded, offset, keyLengthBytes);
        } else {
            System.arraycopy(yba, 0, publicKeyEncoded, offset + keyLengthBytes - yba.length, yba.length);
        }

        return publicKeyEncoded;
    }
}
