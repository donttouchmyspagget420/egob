package deez.nuts.egob.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.SecureRandom;

/** Per-install secret used to sign dynamic QR credentials. */
public final class SecurityKeys {

    private static final String PREFS = "egob_security";
    private static final String KEY_QR_SECRET = "qr_secret";

    private SecurityKeys() {
    }

    public static synchronized String qrSecret(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String existing = preferences.getString(KEY_QR_SECRET, null);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        byte[] value = new byte[32];
        new SecureRandom().nextBytes(value);
        String secret = Base64.encodeToString(value, Base64.NO_WRAP | Base64.URL_SAFE);
        preferences.edit().putString(KEY_QR_SECRET, secret).apply();
        return secret;
    }
}
