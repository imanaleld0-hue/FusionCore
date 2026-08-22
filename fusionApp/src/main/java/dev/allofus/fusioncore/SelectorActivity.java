package dev.allofus.fusioncore;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import dev.allofus.fusioncore.bridge.ActivityBridge;

public class SelectorActivity extends AppCompatActivity {
    private static final String TAG = "FusionCore";
    private static final int REQ_STORAGE = 1001;
    private static final String TARGET_PKG = "com.innersloth.spacemafia";
    private String pendingPkg;
    private View cardGame, cardMods, cardDiag, cardLogs, cardGitHub, btnSettings, btnLaunch;
    private TextView gameName, gamePkg;
    private ImageView gameIcon;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);
        LogcatCollector.start(this);
        ActivityBridge.registerActivity(this);
        ActivityBridge.initialize(this);
        findViews();
        setup();
        loadGameInfo();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ActivityBridge.handleGooglePlayMergeIntent(intent)) {
            Log.i(TAG, "Auth intent processed");
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (pendingPkg != null && hasStorage()) {
            String pkg = pendingPkg; pendingPkg = null; launch(pkg);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_STORAGE) return;
        if (pendingPkg == null) return;
        if (hasStorage()) { String pkg = pendingPkg; pendingPkg = null; launch(pkg); }
        else Toast.makeText(this, R.string.selector_storage_permission_required, Toast.LENGTH_LONG).show();
    }

    @Override protected void onDestroy() {
        ActivityBridge.clearActivity(this);
        super.onDestroy();
    }

    private void findViews() {
        cardGame = findViewById(R.id.card_game);
        cardMods = findViewById(R.id.card_mods);
        cardDiag = findViewById(R.id.card_diagnostics);
        cardLogs = findViewById(R.id.card_logs);
        cardGitHub = findViewById(R.id.card_github);
        gameIcon = findViewById(R.id.game_icon);
        gameName = findViewById(R.id.game_name);
        gamePkg = findViewById(R.id.game_package);
        btnSettings = findViewById(R.id.btn_settings);
        btnLaunch = findViewById(R.id.launch_button);
    }

    private void setup() {
        if (btnLaunch != null) btnLaunch.setOnClickListener(v -> maybeLaunch(TARGET_PKG));
        if (btnSettings != null) btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        if (cardMods != null) cardMods.setOnClickListener(v -> startActivity(new Intent(this, ModManagerActivity.class)));
        if (cardDiag != null) cardDiag.setOnClickListener(v -> startActivity(new Intent(this, ConfigEditorActivity.class)));
        if (cardLogs != null) cardLogs.setOnClickListener(v -> startActivity(new Intent(this, LogsActivity.class)));
        if (cardGitHub != null) cardGitHub.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/imanaleld0-hue/FusionCore"));
            startActivity(i);
        });
    }

    private void loadGameInfo() {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(TARGET_PKG, 0);
            String label = pm.getApplicationLabel(info).toString();
            Drawable icon = pm.getApplicationIcon(info);
            if (gameIcon != null) gameIcon.setImageDrawable(icon);
            if (gameName != null) gameName.setText(label);
            if (gamePkg != null) gamePkg.setText(TARGET_PKG);
            if (btnLaunch != null) btnLaunch.setEnabled(true);
        } catch (Exception e) {
            if (gameName != null) gameName.setText("Among Us not installed");
            if (btnLaunch != null) btnLaunch.setEnabled(false);
        }
    }

    private void maybeLaunch(String pkg) {
        if (!hasStorage()) { pendingPkg = pkg; requestStorage(); return; }
        launch(pkg);
    }

    private void launch(String pkg) {
        Intent intent = new Intent(this, BootstrapActivity.class);
        intent.putExtra(BootstrapActivity.EXTRA_TARGET_PACKAGE, pkg);
        intent.putExtra(BootstrapActivity.EXTRA_USE_ORIGINAL_LIBUNITY, !FusionSettings.isDownloadUnstrippedLibUnity(this));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private boolean hasStorage() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    private void requestStorage() {
        Toast.makeText(this, R.string.selector_storage_permission_prompt, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        try { startActivityForResult(intent, REQ_STORAGE); }
        catch (Exception e) {
            Log.w(TAG, "storage settings failed", e);
            try { startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), REQ_STORAGE); }
            catch (Exception e2) { Toast.makeText(this, R.string.selector_storage_permission_open_failed, Toast.LENGTH_LONG).show(); }
        }
    }
}
