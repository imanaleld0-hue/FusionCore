package dev.allofus.fusioncore.bridge;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

import dev.allofus.fusioncore.R;


public class GoogleSignInHelper {
    private static final String TAG = "GoogleSignInHelper";
    public static final int RC_SIGN_IN = 9001;

    private final GoogleSignInClient client;

    public GoogleSignInHelper(Activity activity) {
        String webClientId = activity.getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build();
        this.client = GoogleSignIn.getClient(activity, gso);
    }

   
    public GoogleSignInAccount getLastSignedInAccount(Activity activity) {
        return GoogleSignIn.getLastSignedInAccount(activity);
    }

    public void signIn(Activity activity) {
        Intent signInIntent = client.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
        Log.i(TAG, "Запущен Google Sign-In Intent");
    }

    public void signOut(Activity activity, Runnable onComplete) {
        client.signOut().addOnCompleteListener(activity, task -> {
            Log.i(TAG, "Google Sign-Out выполнен");
            AuthManager.getInstance().logout();
            if (onComplete != null) onComplete.run();
        });
    }


    public static boolean handleActivityResult(int requestCode, Intent data, AuthCallback callback) {
        if (requestCode != RC_SIGN_IN) return false;
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException.class);
            if (account != null) {
                Log.i(TAG, "Google Sign-In успех: " + account.getEmail());
                AuthManager.getInstance().handleGoogleSignInAccount(account);
                if (callback != null) callback.onSuccess(account);
                return true;
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In ошибка: code=" + e.getStatusCode(), e);
            if (callback != null) callback.onError(e);
        }
        return true; 
    }

    public interface AuthCallback {
        void onSuccess(GoogleSignInAccount account);
        void onError(Exception e);
    }
}
