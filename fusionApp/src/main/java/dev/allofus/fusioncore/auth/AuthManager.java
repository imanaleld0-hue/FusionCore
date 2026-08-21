package dev.allofus.fusioncore.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String PREF_FILE = "fusion_auth_secure_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_MERGE_ID = "auth_merge_id";
    private static final String KEY_STORE = "auth_store";
    private static final String KEY_EXPIRATION = "auth_expiration";

    private static AuthManager instance;
    private SharedPreferences sharedPreferences;

    private AuthManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    PREF_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SKEY_RAW,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Initialization error EncryptedSharedPreferences, fallback to ordinary ones SharedPreferences", e);
            sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_FILE + "_fallback", Context.MODE_PRIVATE);
        }
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public synchronized void setAuth(String token, String mergeId, String store, long durationMs) {
        long expirationTime = System.currentTimeMillis() + (durationMs > 0 ? durationMs : 86400000L); // Default 24h
        sharedPreferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_MERGE_ID, mergeId)
                .putString(KEY_STORE, store)
                .putLong(KEY_EXPIRATION, expirationTime)
                .apply();
        
        Log.i(TAG, "Authorization data has been updated. Expiration: " + expirationTime);
    }

    public synchronized boolean isAuthenticated() {
        String token = sharedPreferences.getString(KEY_TOKEN, null);
        String mergeId = sharedPreferences.getString(KEY_MERGE_ID, null);
        long exp = sharedPreferences.getLong(KEY_EXPIRATION, 0);

        boolean hasCredentials = !TextUtils.isEmpty(token) || !TextUtils.isEmpty(mergeId);
        boolean isNotExpired = exp == 0 || System.currentTimeMillis() < exp;

        return hasCredentials && isNotExpired;
    }

    public synchronized String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, "");
    }

    public synchronized String getMergeId() {
        return sharedPreferences.getString(KEY_MERGE_ID, "");
    }

    public synchronized String getStore() {
        return sharedPreferences.getString(KEY_STORE, "google");
    }

    public synchronized void clearAuth() {
        sharedPreferences.edit().clear().apply();
        Log.i(TAG, "Authorization data has been cleared");
    }
}
