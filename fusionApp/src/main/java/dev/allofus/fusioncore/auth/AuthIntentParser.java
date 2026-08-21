package dev.allofus.fusioncore.auth;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public class AuthIntentParser {

    public static class AuthResult {
        public final String token;
        public final String mergeId;
        public final String store;

        public AuthResult(String token, String mergeId, String store) {
            this.token = token;
            this.mergeId = mergeId;
            this.store = store;
        }

        public boolean isValid() {
            return !TextUtils.isEmpty(token) || !TextUtils.isEmpty(mergeId);
        }
    }

    public static AuthResult parseIntent(Intent intent) {
        if (intent == null) {
            return null;
        }

        Uri dataUri = intent.getData();
        if (dataUri != null) {
            AuthResult result = parseUri(dataUri);
            if (result != null && result.isValid()) {
                return result;
            }
        }

        
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(extraText)) {
                AuthResult result = parseRawText(extraText);
                if (result != null && result.isValid()) {
                    return result;
                }
            }
        }

        
        ClipData clipData = intent.getClipData();
        if (clipData != null && clipData.getItemCount() > 0) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                if (item != null) {
                    CharSequence text = item.getText();
                    if (!TextUtils.isEmpty(text)) {
                        AuthResult result = parseRawText(text.toString());
                        if (result != null && result.isValid()) {
                            return result;
                        }
                    }
                    Uri itemUri = item.getUri();
                    if (itemUri != null) {
                        AuthResult result = parseUri(itemUri);
                        if (result != null && result.isValid()) {
                            return result;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static AuthResult parseRawText(String rawText) {
        if (TextUtils.isEmpty(rawText)) {
            return null;
        }
        rawText = rawText.trim();
        if (rawText.startsWith("http://") || rawText.startsWith("https://")) {
            try {
                Uri uri = Uri.parse(rawText);
                return parseUri(uri);
            } catch (Exception ignored) {
            }
        }
        
        return new AuthResult(rawText, null, "google");
    }

    public static AuthResult parseUri(Uri uri) {
        if (uri == null) {
            return null;
        }

        String host = uri.getHost();
        String path = uri.getPath();

    
        boolean isAccountMgmt = (host != null && host.contains("accounts.innersloth.com"))
                || (path != null && path.contains("account-management"));

        String token = uri.getQueryParameter("token");
        String mergeId = uri.getQueryParameter("mergeId");
        String store = uri.getQueryParameter("store");

        if (TextUtils.isEmpty(store)) {
            store = "google";
        }

        if (isAccountMgmt || !TextUtils.isEmpty(token) || !TextUtils.isEmpty(mergeId)) {
            return new AuthResult(token, mergeId, store);
        }

        return null;
    }
}
