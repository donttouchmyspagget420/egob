package deez.nuts.egob.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Keeps the current citizen id in memory, or on disk when "trust device" is enabled. */
public final class SessionManager {

    private static final String PREFS = "egob_session";
    private static final String KEY_USER = "user_id";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_BULLETIN = "bulletin_dismissed_";

    private static int currentUserId;

    private SessionManager() {
    }

    public static void start(Context context, int userId, boolean rememberDevice) {
        currentUserId = userId;
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit().putBoolean(KEY_REMEMBER, rememberDevice);
        if (rememberDevice) {
            editor.putInt(KEY_USER, userId);
        } else {
            editor.remove(KEY_USER);
        }
        editor.apply();
    }

    public static int currentUserId(Context context) {
        if (currentUserId > 0) {
            return currentUserId;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!preferences.getBoolean(KEY_REMEMBER, false)) {
            return -1;
        }
        int userId = preferences.getInt(KEY_USER, -1);
        currentUserId = userId;
        return userId;
    }

    public static boolean isRemembered(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_REMEMBER, false);
    }

    public static void clear(Context context) {
        currentUserId = 0;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    /** Removes the persisted auto-login key without ending the current in-memory session. */
    public static void revokeAccessKey(Context context, int userId) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (preferences.getInt(KEY_USER, -1) == userId) {
            preferences.edit()
                    .remove(KEY_USER)
                    .putBoolean(KEY_REMEMBER, false)
                    .apply();
        }
    }

    public static boolean isBulletinDismissed(Context context, int userId) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_BULLETIN + userId, false);
    }

    public static void dismissBulletin(Context context, int userId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_BULLETIN + userId, true).apply();
    }
}
