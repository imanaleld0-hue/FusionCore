package dev.allofus.fusioncore.auth;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;


public class AuthIntentParser {

    private static final String TAG = "AuthIntentParser";
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    
    private static final String[] AUTH_HOSTS = {
        "accounts.innersloth.com",
        "accounts.example.com"
    };

    public static InnerslothAuthData parseAuthIntent(Intent intent, Context context) {
        if (intent == null) return null;


        if (intent.getData() != null) {
            InnerslothAuthData data = extractFromUri(intent.getData());
            if (data != null) return data;
        }


        if (extraText != null) {
            InnerslothAuthData data = extractFromText(extraText.toString());
            if (data != null) return data;
        }

        // 3. Clipboard / ClipData
        if (context != null) {
            ClipData clipData = intent.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    CharSequence text = clipData.getItemAt(i).coerceToText(context);
                    if (text != null) {
                        InnerslothAuthData data = extractFromText(text.toString());
                        if (data != null) return data;
                    }
                }
            }
        }

        return null;
    }

    public static InnerslothAuthData extractFromText(String text) {
        String trimmed = safe(text).trim();
        if (trimmed.isEmpty()) return null;

   
        try {
            if (trimmed.startsWith("http")) {
                Uri uri = Uri.parse(trimmed);
                InnerslothAuthData data = extractFromUri(uri);
                if (data != null) return data;
            }
        } catch (Exception ignored) {}

        
        String[] parts = trimmed.split("\\s+");
        for (String part : parts) {
            String candidate = trimCandidate(part);
            if (looksLikeJwt(candidate)) {
                return buildFromJwt(candidate, null, null);
            }
        }
        return null;
    }

    private static InnerslothAuthData extractFromUri(Uri uri) {
        if (uri == null) return null;
        String host = safe(uri.getHost());
        boolean validHost = false;
        for (String h : AUTH_HOSTS) {
            if (h.equalsIgnoreCase(host)) { validHost = true; break; }
        }
        if (!validHost) return null;

        String store = safe(uri.getQueryParameter("store"));
        String token = safe(uri.getQueryParameter("token"));
        String mergeId = safe(uri.getQueryParameter("mergeId"));

        if (token.isEmpty()) return null;

        return buildFromJwt(token, store, mergeId);
    }

    private static InnerslothAuthData buildFromJwt(String jwt, String store, String mergeId) {
        JSONObject payload = parseJwtPayload(jwt);
        if (payload == null) return null;

        
        if (!payload.has("sub") || !payload.has("exp")) {
            Log.w(TAG, "JWT does not contain required fields (sub, exp)");
            return null;
        }

        return new InnerslothAuthData(
            safe(store).isEmpty() ? "google" : store,
            jwt,
            safe(mergeId),
            payload.optString("sub", ""),
            payload.optString("name", ""),
            payload.optString("given_name", ""),
            payload.optString("family_name", ""),
            payload.optString("picture", ""),
            payload.optString("email", ""),
            payload.optLong("iat", 0),
            payload.optLong("exp", 0)
        );
    }

    public static JSONObject parseJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String base64Payload = padBase64(parts[1]);
            byte[] decoded = Base64.decode(base64Payload, Base64.URL_SAFE | Base64.NO_WRAP);
            return new JSONObject(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "JWT decode error", e);
            return null;
        }
    }

    public static boolean looksLikeJwt(String str) {
        if (str == null || str.isEmpty() || str.indexOf('=') >= 0) return false;
        return JWT_PATTERN.matcher(str).matches();
    }

    public static String maskToken(String token) {
        if (token == null || token.length() <= 12) return "***";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    private static String trimCandidate(String str) {
        return safe(str).trim()
            .replaceAll("^[\\\"'<>]+", "")
            .replaceAll("[\\\"'<>.,;]+$", "");
    }

    private static String padBase64(String str) {
        int rem = str.length() % 4;
        if (rem == 0) return str;
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() % 4 != 0) sb.append('=');
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
        }
