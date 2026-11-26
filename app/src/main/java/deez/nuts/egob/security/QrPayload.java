package deez.nuts.egob.security;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Signed payload embedded in the citizen's dynamic QR credential. */
public final class QrPayload {

    public static final String PREFIX = "EGOB1";
    private static final String HMAC = "HmacSHA256";

    private QrPayload() {
    }

    public static String create(String nin, String name, long timestamp, String secret) {
        String body = safe(nin) + "|" + safe(name) + "|" + timestamp;
        return PREFIX + "|" + body + "|" + sign(body, secret);
    }

    public static boolean verify(String payload, String secret) {
        if (payload == null) {
            return false;
        }
        String[] parts = payload.split("\\|");
        if (parts.length != 5 || !PREFIX.equals(parts[0])) {
            return false;
        }
        String body = parts[1] + "|" + parts[2] + "|" + parts[3];
        return constantTimeEquals(sign(body, secret), parts[4]);
    }

    public static String extractNin(String payload) {
        String[] parts = payload == null ? new String[0] : payload.split("\\|");
        return parts.length >= 5 ? parts[1] : "";
    }

    public static long extractTimestamp(String payload) {
        String[] parts = payload == null ? new String[0] : payload.split("\\|");
        if (parts.length < 5) {
            return 0L;
        }
        try {
            return Long.parseLong(parts[3]);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String sign(String body, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC));
            return Base64.encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)),
                    Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el QR", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("|", "/");
    }
}
