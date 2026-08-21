package dev.allofus.fusioncore.bridge;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

public class GoogleSignInHelper {
    private static final String TAG = "GoogleSignInHelper";
    private static final int RC_SIGN_IN = 9001;
    
    private final GoogleSignInClient client;
    
    public GoogleSignInHelper(Activity activity) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        this.client = GoogleSignIn.getClient(activity, gso);
    }
    
    public void signIn(Activity activity) {
        Intent signInIntent = client.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    public static boolean handleActivityResult(int requestCode, Intent data, AuthCallback callback) {
        if (requestCode != RC_SIGN_IN) return false;
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                Log.i(TAG, "Google Sign-In success: " + account.getEmail());
                AuthManager.getInstance().handleReceivedToken(account.getIdToken());
                if (callback != null) callback.onSuccess(account);
                return true;
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In failed: " + e.getStatusCode(), e);
            if (callback != null) callback.onError(e);
        }
        return false;
    }
    
    public interface AuthCallback {
        void onSuccess(GoogleSignInAccount account);
        void onError(Exception e);
    }
}

