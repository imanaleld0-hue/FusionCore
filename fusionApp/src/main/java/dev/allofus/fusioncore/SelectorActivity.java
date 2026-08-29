package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import dev.allofus.fusioncore.ui.LogsViewerActivity;
import dev.allofus.fusioncore.ui.ModProjectsActivity;
import dev.allofus.fusioncore.ui.SettingsActivity;

public class SelectorActivity extends Activity {

    private static final String TAG = "FusionCore";
    private static final String TARGET_PACKAGE = "com.innersloth.spacemafia";
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogcatCollector.start(this);
        setContentView(R.layout.activity_selector);

        View root = findViewById(R.id.selector_root);
        int basePadding = Math.round(getResources().getDisplayMetrics().density * 16f);
        Utilities.applyWindowInsets(root, basePadding);

        Button launchButton = findViewById(R.id.selector_launch);
        launchButton.setOnClickListener(v -> maybeLaunchBootstrap());

        ImageButton logsButton = findViewById(R.id.selector_action_logs);
        if (logsButton != null) {
            logsButton.setOnClickListener(v -> startActivity(new Intent(this, LogsViewerActivity.class)));
        }

        ImageButton modsButton = findViewById(R.id.selector_action_mods);
        if (modsButton != null) {
            modsButton.setOnClickListener(v -> startActivity(new Intent(this, ModProjectsActivity.class)));
        }

        ImageButton settingsButton = findViewById(R.id.selector_action_settings);
        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasExternalStorageManagerAccess()) {
            launchBootstrap();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_MANAGE_EXTERNAL_STORAGE) return;
        if (hasExternalStorageManagerAccess()) {
            launchBootstrap();
            return;
        }
        Toast.makeText(this, getString(R.string.selector_storage_permission_required), Toast.LENGTH_LONG).show();
    }

    private void launchBootstrap() {
        Intent intent = new Intent(this, BootstrapActivity.class);
        intent.putExtra(BootstrapActivity.EXTRA_TARGET_PACKAGE, TARGET_PACKAGE);
        intent.putExtra(BootstrapActivity.EXTRA_USE_ORIGINAL_LIBUNITY,
                !FusionSettings.isDownloadUnstrippedLibUnity(this));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private void maybeLaunchBootstrap() {
        if (!hasExternalStorageManagerAccess()) {
            requestExternalStorageManagerAccess();
            return;
        }
        launchBootstrap();
    }

    private boolean hasExternalStorageManagerAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true;
        return Environment.isExternalStorageManager();
    }

    private void requestExternalStorageManagerAccess() {
        Toast.makeText(this, getString(R.string.selector_storage_permission_prompt), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        try {
            startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
        } catch (Exception e) {
            Log.w(TAG, "Failed to open app-specific all-files access screen, opening generic page", e);
            Intent fallbackIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            try {
                startActivityForResult(fallbackIntent, REQUEST_MANAGE_EXTERNAL_STORAGE);
            } catch (Exception inner) {
                Log.e(TAG, "Failed to open all-files access settings", inner);
                Toast.makeText(this, getString(R.string.selector_storage_permission_open_failed), Toast.LENGTH_LONG).show();
            }
        }
    }
}