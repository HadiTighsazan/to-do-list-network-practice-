package org.example.todo.server.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Step 1: PBKDF2WithHmacSHA256 password hasher.
 */
public class PasswordHasher {
    private static final String ALGO = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITER = 100_000;
    private static final int SALT_LEN = 16;
    private static final int KEY_LEN = 256; // bits

    private final SecureRandom rng = new SecureRandom();

    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LEN];
        rng.nextBytes(salt);
        return salt;
    }

    public byte[] hash(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LEN);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }

    public boolean verify(char[] password, byte[] salt, byte[] expected, int iterations) {
        byte[] actual = hash(password, salt, iterations);
        return constantTimeEquals(actual, expected);
    }

    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= (a[i] ^ b[i]);
        return r == 0;
    }

    public static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] fromBase64(String s) {
        return Base64.getDecoder().decode(s);
    }

    public int defaultIterations() { return DEFAULT_ITER; }
}
