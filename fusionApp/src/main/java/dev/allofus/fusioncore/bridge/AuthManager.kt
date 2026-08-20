package dev.allofus.fusioncore.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthSession(
    val token: String,
    val userId: String,
    val displayName: String,
    val expiresAtTimestamp: Long
) {
    val isExpired: Boolean get() = System.currentTimeMillis() / 1000 >= expiresAtTimestamp
}

interface AuthValidator {
    suspend fun validate(token: String): Result<AuthSession>
}

/**
 * Главный менеджер аутентификации FusionCore.
 */
object AuthManager {

    private const val PREFS_FILENAME = "fusioncore_secure_prefs"
    private const val KEY_TOKEN = "session_token"
    private const val KEY_EXPIRES_AT = "expires_at"

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    private var authValidator: AuthValidator? = null

    fun configureValidator(validator: AuthValidator) {
        this.authValidator = validator
    }

    fun initialize(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val savedToken = prefs.getString(KEY_TOKEN, null)
            val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

            if (!savedToken.isNullOrEmpty() && expiresAt > System.currentTimeMillis() / 1000) {
                val payload = AuthIntentParser.parseJwtPayload(savedToken)
                val userId = payload?.optString("sub", "user_id") ?: "user_id"
                val name = payload?.optString("name", "FusionUser") ?: "FusionUser"

                _currentSession.value = AuthSession(savedToken, userId, name, expiresAt)
            }
        } catch (_: Exception) {}
    }

    fun handleReceivedToken(rawToken: String) {
        val payload = AuthIntentParser.parseJwtPayload(rawToken)
        val exp = payload?.optLong("exp", 0L) ?: 0L
        val userId = payload?.optString("sub", "user_${System.currentTimeMillis()}") ?: "user"
        val name = payload?.optString("name", "FusionUser") ?: "FusionUser"

        // Сессия действует 60 минут (3600 сек), если нет поля exp
        val sessionDuration = if (exp > 0) exp else (System.currentTimeMillis() / 1000) + 3600

        val session = AuthSession(rawToken, userId, name, sessionDuration)
        _currentSession.value = session
    }

    fun logout(context: Context) {
        _currentSession.value = null
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }
}
