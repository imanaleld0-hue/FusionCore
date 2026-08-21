package dev.allofus.fusioncore.auth;

import android.content.Context;
import android.util.Log;

public class AuthManager {

    private static final String TAG = "AuthManager";
    private static AuthManager instance;

    private EncryptedAuthStore store;
    private InnerslothAuthData currentAuth;

    private AuthManager() {}

    public static synchronized AuthManager getInstance() {
        if (instance == null) instance = new AuthManager();
        return instance;
    }

    public void init(Context context) {
        if (store == null) {
            store = new EncryptedAuthStore(context.getApplicationContext());
            currentAuth = store.load();
            if (currentAuth != null) {
                Log.i(TAG, "Loaded saved auth: " + currentAuth.name);
                if (currentAuth.isExpired()) {
                    Log.w(TAG, "Saved auth expired, clearing");
                    clearAuth();
                }
            }
        }
    }

    public void setAuth(InnerslothAuthData data) {
        if (data == null || !data.isValid()) {
            Log.w(TAG, "Attempted to set invalid auth data");
            return;
        }
        this.currentAuth = data;
        if (store != null) store.save(data);

        
        NativeAuthBridge.notifyAuthSuccess(
            data.sub,
            data.name,
            data.email,
            data.token,
            data.mergeId,
            data.store
        );
        Log.i(TAG, "Auth set for user: " + data.name);
    }

    public void clearAuth() {
        this.currentAuth = null;
        if (store != null) store.clear();
        NativeAuthBridge.notifyAuthLogout();
        Log.i(TAG, "Auth cleared");
    }

    public InnerslothAuthData getCurrentAuth() {
        return currentAuth;
    }

    public boolean isAuthenticated() {
        return currentAuth != null && currentAuth.isValid();
    }

    public String getToken() {
        return currentAuth != null ? currentAuth.token : "";
    }

    public String getUserId() {
        return currentAuth != null ? currentAuth.sub : "";
    }

    public String getDisplayName() {
        return currentAuth != null ? currentAuth.name : "";
    }

    public long getSessionExpiry() {
        return currentAuth != null ? currentAuth.exp : 0;
    }

    public boolean isSessionExpired() {
        return currentAuth == null || currentAuth.isExpired();
    }
}
