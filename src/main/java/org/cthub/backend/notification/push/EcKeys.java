package org.cthub.backend.notification.push;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.util.Arrays;
import java.util.Base64;

/**
 * P-256 key handling in the raw formats Web Push uses: public keys as 65-byte uncompressed points
 * ({@code 0x04 || X || Y}), private keys as the 32-byte scalar, both base64url without padding.
 */
public final class EcKeys {

    private static final String CURVE = "secp256r1";
    private static final int COORDINATE_LENGTH = 32;
    public static final int PUBLIC_KEY_LENGTH = 1 + 2 * COORDINATE_LENGTH;

    static final ECParameterSpec P256;

    static {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec(CURVE));
            P256 = parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private EcKeys() {
    }

    public static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec(CURVE));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is not available", e);
        }
    }

    /** Parses and validates an uncompressed point; rejects anything that is not on the curve. */
    public static ECPublicKey publicKey(byte[] uncompressed) {
        if (uncompressed == null || uncompressed.length != PUBLIC_KEY_LENGTH || uncompressed[0] != 0x04) {
            throw new IllegalArgumentException("Not an uncompressed P-256 point");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 1 + COORDINATE_LENGTH));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1 + COORDINATE_LENGTH, PUBLIC_KEY_LENGTH));
        if (!isOnCurve(x, y)) {
            throw new IllegalArgumentException("Point is not on P-256");
        }
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid P-256 public key", e);
        }
    }

    public static ECPrivateKey privateKey(byte[] scalar) {
        if (scalar == null || scalar.length != COORDINATE_LENGTH) {
            throw new IllegalArgumentException("A P-256 private key is 32 bytes");
        }
        BigInteger s = new BigInteger(1, scalar);
        if (s.signum() == 0 || s.compareTo(P256.getOrder()) >= 0) {
            throw new IllegalArgumentException("P-256 private key out of range");
        }
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(s, P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid P-256 private key", e);
        }
    }

    public static byte[] encode(ECPublicKey key) {
        byte[] out = new byte[PUBLIC_KEY_LENGTH];
        out[0] = 0x04;
        writeUnsigned(key.getW().getAffineX(), out, 1);
        writeUnsigned(key.getW().getAffineY(), out, 1 + COORDINATE_LENGTH);
        return out;
    }

    public static byte[] encode(ECPrivateKey key) {
        byte[] out = new byte[COORDINATE_LENGTH];
        writeUnsigned(key.getS(), out, 0);
        return out;
    }

    /** Lenient base64url decoding: padding is optional, as browsers omit it. */
    public static byte[] decode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url.strip());
    }

    public static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean isOnCurve(BigInteger x, BigInteger y) {
        EllipticCurve curve = P256.getCurve();
        BigInteger p = ((ECFieldFp) curve.getField()).getP();
        if (x.compareTo(p) >= 0 || y.compareTo(p) >= 0) {
            return false;
        }
        BigInteger left = y.multiply(y).mod(p);
        BigInteger right = x.pow(3).add(curve.getA().multiply(x)).add(curve.getB()).mod(p);
        return left.equals(right);
    }

    private static void writeUnsigned(BigInteger value, byte[] target, int offset) {
        byte[] bytes = value.toByteArray();
        int length = Math.min(bytes.length, COORDINATE_LENGTH);
        System.arraycopy(bytes, bytes.length - length, target, offset + COORDINATE_LENGTH - length, length);
    }
}
