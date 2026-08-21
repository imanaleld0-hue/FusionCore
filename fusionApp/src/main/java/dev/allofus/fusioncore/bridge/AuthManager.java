package dev.allofus.fusioncore.bridge;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.json.JSONObject;

public class AuthManager {

    private static final String TAG = "AuthManager";
    private static final long SESSION_TIMEOUT_SECONDS = 3600;
    private static final String EXPECTED_ISSUER_GOOGLE = "https://accounts.google.com";
    private static final String EXPECTED_ISSUER_GOOGLE_ALT = "accounts.google.com";

    private static AuthManager instance;

    private String currentToken = "";
    private String userId = "";
    private String displayName = "";
    private String email = "";
    private String photoUrl = "";
    private String idToken = "";
    private long expiresAtTimestamp = 0;
    private boolean isGoogleSignedIn = false;

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public synchronized void handleGoogleSignInAccount(GoogleSignInAccount account) {
        if (account == null) {
            Log.w(TAG, "handleGoogleSignInAccount: account is null");
            return;
        }
        this.idToken = safe(account.getIdToken());
        this.userId = safe(account.getId());
        this.displayName = safe(account.getDisplayName());
        this.email = safe(account.getEmail());
        if (account.getPhotoUrl() != null) {
            this.photoUrl = account.getPhotoUrl().toString();
        }
        this.isGoogleSignedIn = true;
        this.expiresAtTimestamp = (System.currentTimeMillis() / 1000) + SESSION_TIMEOUT_SECONDS;
        this.currentToken = this.idToken;

        Log.i(TAG, "Google Sign-In сессия. Пользователь: " + displayName + ", ID: " + maskId(userId));


        NativeAuthBridge.notifyAuthSuccess(userId, displayName, email, idToken);
    }

    
    public synchronized void handleReceivedToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) return;

        JSONObject payload = AuthIntentParser.parseJwtPayload(rawToken);
        if (payload != null) {
            
            String iss = payload.optString("iss", "");
            if (!iss.equals(EXPECTED_ISSUER_GOOGLE) && !iss.equals(EXPECTED_ISSUER_GOOGLE_ALT)) {
                Log.e(TAG, "Невалидный issuer JWT: " + iss);
                return;
            }

            
            String aud = payload.optString("aud", "");
            if (aud.isEmpty()) {
                Log.e(TAG, "JWT не содержит audience (aud)");
                return;
            }

            this.userId = payload.optString("sub", "user_" + System.currentTimeMillis());
            this.displayName = payload.optString("name", "FusionUser");
            this.email = payload.optString("email", "");
            long exp = payload.optLong("exp", 0L);

          
            this.expiresAtTimestamp = exp > 0 ? exp : (System.currentTimeMillis() / 1000) + SESSION_TIMEOUT_SECONDS;
        } else {
            this.userId = "user_" + System.currentTimeMillis();
            this.displayName = "FusionUser";
            this.email = "";
            this.expiresAtTimestamp = (System.currentTimeMillis() / 1000) + SESSION_TIMEOUT_SECONDS;
        }

        this.currentToken = rawToken;
        this.idToken = rawToken;
        Log.i(TAG, "Сессия авторизована. Пользователь: " + displayName + ", Токен: " + AuthIntentParser.maskToken(currentToken));

        NativeAuthBridge.notifyAuthSuccess(userId, displayName, email, idToken);
    }

    public boolean isSessionActive() {
        return !currentToken.isEmpty() && (System.currentTimeMillis() / 1000) < expiresAtTimestamp;
    }

    public boolean isGoogleSignedIn() {
        return isGoogleSignedIn && isSessionActive();
    }

    public String getCurrentToken() { return currentToken; }
    public String getIdToken() { return idToken; }
    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhotoUrl() { return photoUrl; }
    public long getExpiresAtTimestamp() { return expiresAtTimestamp; }

    public synchronized void logout() {
        this.currentToken = "";
        this.idToken = "";
        this.userId = "";
        this.displayName = "";
        this.email = "";
        this.photoUrl = "";
        this.expiresAtTimestamp = 0;
        this.isGoogleSignedIn = false;
        Log.i(TAG, "Сессия завершена.");
        NativeAuthBridge.notifyAuthLogout();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String maskId(String id) {
        if (id == null || id.length() < 8) return "***";
        return id.substring(0, 4) + "..." + id.substring(id.length() - 4);
    }
            }
