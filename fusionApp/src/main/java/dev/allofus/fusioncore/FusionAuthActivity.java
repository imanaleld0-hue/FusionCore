package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import dev.allofus.fusioncore.bridge.ActivityBridge;
import dev.allofus.fusioncore.bridge.AuthManager;
import dev.allofus.fusioncore.bridge.GoogleSignInHelper;

public class FusionAuthActivity extends Activity {

    private static final String TAG = "FusionAuthActivity";
    private static final String EXTRA_TARGET_PACKAGE = "target_package";

    private View signInButton;
    private View signOutButton;
    private View continueButton;
    private View profileCard;
    private ImageView avatarView;
    private TextView nameView;
    private TextView emailView;
    private TextView statusView;

    private String targetPackage;

    public static Intent createIntent(Activity from, String targetPackage) {
        Intent intent = new Intent(from, FusionAuthActivity.class);
        intent.putExtra(EXTRA_TARGET_PACKAGE, targetPackage);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fusion_auth);

        targetPackage = getIntent().getStringExtra(EXTRA_TARGET_PACKAGE);
        ActivityBridge.initialize(this);

        signInButton = findViewById(R.id.auth_sign_in_button);
        signOutButton = findViewById(R.id.auth_sign_out_button);
        continueButton = findViewById(R.id.auth_continue_button);
        profileCard = findViewById(R.id.auth_profile_card);
        avatarView = findViewById(R.id.auth_avatar);
        nameView = findViewById(R.id.auth_name);
        emailView = findViewById(R.id.auth_email);
        statusView = findViewById(R.id.auth_status);

        signInButton.setOnClickListener(v -> startGoogleSignIn());
        signOutButton.setOnClickListener(v -> signOut());
        continueButton.setOnClickListener(v -> proceedToBootstrap());

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ActivityBridge.handleGooglePlayMergeIntent(intent)) {
            updateUI();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        boolean handled = GoogleSignInHelper.handleActivityResult(requestCode, data,
            new GoogleSignInHelper.AuthCallback() {
                @Override
                public void onSuccess(GoogleSignInAccount account) {
                    Toast.makeText(FusionAuthActivity.this,
                        "Добро пожаловать, " + account.getDisplayName(), Toast.LENGTH_SHORT).show();
                    updateUI();
                }
                @Override
                public void onError(Exception e) {
                    Toast.makeText(FusionAuthActivity.this,
                        "Ошибка входа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateUI();
                }
            });
        if (!handled) {
            Log.d(TAG, "onActivityResult: не наш запрос (requestCode=" + requestCode + ")");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityBridge.cleanup();
    }

    private void startGoogleSignIn() {
        ActivityBridge.startGoogleSignIn(this);
    }

    private void signOut() {
        GoogleSignInHelper helper = ActivityBridge.getGoogleSignInHelper();
        if (helper != null) {
            helper.signOut(this, this::updateUI);
        } else {
            AuthManager.getInstance().logout();
            updateUI();
        }
    }

    private void updateUI() {
        AuthManager auth = AuthManager.getInstance();
        if (auth.isSessionActive()) {
            profileCard.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.VISIBLE);

            nameView.setText(auth.getDisplayName());
            emailView.setText(auth.getEmail());
            statusView.setText("Авторизован через Google");

            // Аватар через Glide можно добавить позже
            avatarView.setImageResource(R.mipmap.app_icon);
        } else {
            profileCard.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
            continueButton.setVisibility(View.GONE);
            statusView.setText("Войдите через Google для продолжения");
        }
    }

    private void proceedToBootstrap() {
        if (targetPackage == null || targetPackage.isEmpty()) {
            Toast.makeText(this, "Не выбран пакет игры", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, BootstrapActivity.class);
        intent.putExtra(BootstrapActivity.EXTRA_TARGET_PACKAGE, targetPackage);
        intent.putExtra(BootstrapActivity.EXTRA_USE_ORIGINAL_LIBUNITY,
            !FusionSettings.isDownloadUnstrippedLibUnity(this));
        startActivity(intent);
        finish();
    }
              }

