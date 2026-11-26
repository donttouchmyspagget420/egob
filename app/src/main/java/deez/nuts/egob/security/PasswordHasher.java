package deez.nuts.egob.security;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2 password hashing with per-user salts. */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int ITERATIONS = 15000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password == null");
        }
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] digest = derive(password, salt, ITERATIONS);
        return ITERATIONS + ":" + encode(salt) + ":" + encode(digest);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = decode(parts[1]);
            byte[] expected = decode(parts[2]);
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo derivar la contraseña", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    private static byte[] decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }
}
