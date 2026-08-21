package dev.allofus.fusioncore.bridge;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Парсер токенов из Intent, Deep Link, Sharesheet и клипборда.
 * Очищен от прямой сетевой логики и секретов.
 */
public class AuthIntentParser {

    private static final String TAG = "AuthIntentParser";

    public static String parseIntent(Intent intent, Context context) {
        if (intent == null) return "";

        // 1. Проверка URI (Deep Link / Action View)
        if (intent.getData() != null) {
            String tokenFromUri = extractTokenFromUri(intent.getData());
            if (!tokenFromUri.isEmpty()) {
                return tokenFromUri;
            }
        }

        // 2. Проверка EXTRA_TEXT (Android Sharesheet)
        CharSequence extraText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (extraText != null) {
            String tokenFromText = extractTokenFromText(extraText.toString());
            if (!tokenFromText.isEmpty()) {
                return tokenFromText;
            }
        }

        // 3. Проверка ClipData (Буфер обмена)
        if (context != null) {
            ClipData clipData = intent.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    CharSequence itemText = clipData.getItemAt(i).coerceToText(context);
                    if (itemText != null) {
                        String tokenFromClip = extractTokenFromText(itemText.toString());
                        if (!tokenFromClip.isEmpty()) {
                            return tokenFromClip;
                        }
                    }
                }
            }
        }

        return "";
    }

    public static String extractTokenFromUri(Uri uri) {
        if (uri == null) return "";

        String token = safe(uri.getQueryParameter("token"));
        if (looksLikeJwt(token)) return token;

        String fragment = uri.getFragment();
        if (fragment != null && !fragment.isEmpty()) {
            try {
                Uri fakeUri = Uri.parse("[https://accounts.example.com/](https://accounts.example.com/)?" + fragment);
                String fragmentToken = safe(fakeUri.getQueryParameter("token"));
                if (looksLikeJwt(fragmentToken)) return fragmentToken;
            } catch (Exception ignored) {}
        }
        return "";
    }

    public static String extractTokenFromText(String text) {
        String trimmed = safe(text).trim();
        if (trimmed.isEmpty()) return "";

        if (looksLikeJwt(trimmed)) return trimmed;

        String[] parts = trimmed.split("\\s+");
        for (String part : parts) {
            String candidate = trimCandidate(part);
            if (looksLikeJwt(candidate)) return candidate;
        }
        return "";
    }

    public static boolean looksLikeJwt(String str) {
        if (str == null || str.isEmpty() || str.indexOf('=') >= 0) {
            return false;
        }
        return str.matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*$");
    }

    public static JSONObject parseJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.size() < 2) return null;
            String base64Payload = padBase64(parts[1]);
            byte[] decodedBytes = Base64.decode(base64Payload, Base64.URL_SAFE | Base64.NO_WRAP);
            return new JSONObject(new String(decodedBytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "Ошибка декодирования JWT", e);
            return null;
        }
    }

    /**
     * Маскирование токена в логах для обеспечения безопасности.
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) return "null";
        if (token.length() <= 10) return "***";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    private static String trimCandidate(String str) {
        return safe(str).trim().replaceAll("^[\"'<>]+", "").replaceAll("[\"'<>.,;]+$", "");
    }

    private static String padBase64(String str) {
        int length = str.length() % 4;
        if (length == 0) return str;
        StringBuilder sb = new StringBuilder(str);
        while (length < 4) {
            sb.append('=');
            length++;
        }
        return sb.toString();
    }

    private static String safe(String str) {
        return str == null ? "" : str;
    }
}
