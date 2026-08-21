package dev.allofus.fusioncore.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

public class EncryptedAuthStore {

    private static final String TAG = "EncryptedAuthStore";
    private static final String PREFS_FILE = "fusion_auth_secure";
    private static final String KEY_AUTH_DATA = "innersloth_auth_data";

    private final SharedPreferences prefs;

    public EncryptedAuthStore(Context context) {
        this.prefs = createSecurePrefs(context);
    }

    private SharedPreferences createSecurePrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.w(TAG, "EncryptedSharedPreferences недоступен, используем fallback", e);
            return context.getSharedPreferences(PREFS_FILE + "_fallback", Context.MODE_PRIVATE);
        }
    }

    public void save(InnerslothAuthData data) {
        if (data == null) {
            prefs.edit().remove(KEY_AUTH_DATA).apply();
            return;
        }
        String json = data.toJson().toString();
        prefs.edit().putString(KEY_AUTH_DATA, json).apply();
        Log.i(TAG, "Auth data saved securely. User: " + data.name);
    }

    public InnerslothAuthData load() {
        String json = prefs.getString(KEY_AUTH_DATA, null);
        if (json == null || json.isEmpty()) return null;
        try {
            return InnerslothAuthData.fromJson(new JSONObject(json));
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse auth data", e);
            return null;
        }
    }

    public void clear() {
        prefs.edit().remove(KEY_AUTH_DATA).apply();
        Log.i(TAG, "Auth data cleared");
    }

    public boolean hasAuth() {
        return load() != null;
    }
}
