package dev.allofus.fusioncore.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Модуль парсинга токенов из ActivityBridge, перенесенный в dev.allofus.fusioncore.auth.
 */
object AuthIntentParser {

    fun parseIntent(intent: Intent?, context: Context?): String? {
        if (intent == null) return null

        // 1. Поиск в Uri (Deep Link / Action View)
        intent.data?.let { uri ->
            extractTokenFromUri(uri)?.let { return it }
        }

        // 2. Поиск в EXTRA_TEXT (Sharesheet / Intent Extra)
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
            extractTokenFromText(text)?.let { return it }
        }

        // 3. Поиск в HTML тексте
        intent.getStringExtra(Intent.EXTRA_HTML_TEXT)?.let { htmlText ->
            extractTokenFromText(htmlText)?.let { return it }
        }

        // 4. Поиск в Буфере Обмена (ClipData)
        if (context != null) {
            intent.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val itemText = clipData.getItemAt(i).coerceToText(context)?.toString() ?: ""
                    extractTokenFromText(itemText)?.let { return it }
                }
            }
        }

        return null
    }

    fun extractTokenFromUri(uri: Uri): String? {
        val token = uri.getQueryParameter("token")
        if (looksLikeJwt(token)) return token

        val fragment = uri.fragment
        if (!fragment.isNullOrEmpty()) {
            try {
                val fakeUri = Uri.parse("https://starlight.local/?$fragment")
                val fragmentToken = fakeUri.getQueryParameter("token")
                if (looksLikeJwt(fragmentToken)) return fragmentToken
            } catch (_: Exception) {}
        }
        return null
    }

    fun extractTokenFromText(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        if (looksLikeJwt(trimmed)) return trimmed

        val parts = trimmed.split("\\s+".toRegex())
        for (part in parts) {
            val candidate = part.replace("^[\"'<>]+".toRegex(), "").replace("[\"'<>.,;]+$".toRegex(), "")
            if (looksLikeJwt(candidate)) return candidate
        }
        return null
    }

    fun looksLikeJwt(str: String?): Boolean {
        if (str.isNullOrEmpty()) return false
        if (str.contains("=")) return false
        return str.matches(Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*$"))
    }

    fun parseJwtPayload(jwt: String): JSONObject? {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val base64Payload = padBase64(parts[1])
            val decodedBytes = Base64.decode(base64Payload, Base64.URL_SAFE or Base64.NO_WRAP)
            JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    fun maskToken(token: String?): String {
        if (token.isNullOrEmpty()) return "null"
        if (token.length <= 10) return "***"
        return "${token.take(6)}...${token.takeLast(4)}"
    }

    private fun padBase64(str: String): String {
        val remainder = str.length % 4
        if (remainder == 0) return str
        return str + "=".repeat(4 - remainder)
    }
        }
