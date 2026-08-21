package dev.allofus.fusioncore.bridge;

import android.util.Log;
import org.json.JSONObject;

/**
 * Управление авторизационной сессией с тайм-аутом 60 минут.
 */
public class AuthManager {

    private static final String TAG = "AuthManager";
    private static AuthManager instance;

    private String currentToken = "";
    private String userId = "";
    private String displayName = "";
    private long expiresAtTimestamp = 0;

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public synchronized void handleReceivedToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) return;

        JSONObject payload = AuthIntentParser.parseJwtPayload(rawToken);
        if (payload != null) {
            this.userId = payload.optString("sub", "user_" + System.currentTimeMillis());
            this.displayName = payload.optString("name", "FusionUser");
            long exp = payload.optLong("exp", 0L);
            
            // Задаем время жизни сессии на 60 минут (3600 секунд)
            this.expiresAtTimestamp = exp > 0 ? exp : (System.currentTimeMillis() / 1000) + 3600;
        } else {
            this.userId = "user_" + System.currentTimeMillis();
            this.displayName = "FusionUser";
            this.expiresAtTimestamp = (System.currentTimeMillis() / 1000) + 3600;
        }

        this.currentToken = rawToken;
        Log.i(TAG, "Сессия авторизована. Пользователь: " + displayName + ", Токен: " + AuthIntentParser.maskToken(currentToken));
    }

    public boolean isSessionActive() {
        return !currentToken.isEmpty() && (System.currentTimeMillis() / 1000) < expiresAtTimestamp;
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getExpiresAtTimestamp() {
        return expiresAtTimestamp;
    }

    public synchronized void logout() {
        this.currentToken = "";
        this.userId = "";
        this.displayName = "";
        this.expiresAtTimestamp = 0;
        Log.i(TAG, "Сессия завершена.");
    }
}
