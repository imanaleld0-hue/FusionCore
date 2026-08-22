package dev.allofus.fusioncore;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class SettingsActivity extends Activity {
    private EditText editName;
    private EditText editLevel;
    private Switch switchAutoClear;
    private TextView cacheSizeView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        editName = findViewById(R.id.settings_name);
        editLevel = findViewById(R.id.settings_level);
        switchAutoClear = findViewById(R.id.settings_auto_clear);
        cacheSizeView = findViewById(R.id.settings_cache_size);
        Button btnClearCache = findViewById(R.id.settings_clear_cache);
        Button btnRefreshCache = findViewById(R.id.settings_refresh_cache);
        View back = findViewById(R.id.settings_action_back);

        editName.setText(FusionSettings.getPlayerName(this));
        editLevel.setText(String.valueOf(FusionSettings.getPlayerLevel(this)));
        switchAutoClear.setChecked(FusionSettings.isAutoClearLogs(this));
        updateCacheSize();

        back.setOnClickListener(v -> finish());
        btnClearCache.setOnClickListener(v -> {
            clearGameCache();
            updateCacheSize();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
        });
        btnRefreshCache.setOnClickListener(v -> {
            clearGameCache();
            Toast.makeText(this, "Cache will refresh on next launch", Toast.LENGTH_SHORT).show();
        });
    }

    @Override protected void onPause() {
        super.onPause();
        FusionSettings.setPlayerName(this, editName.getText().toString());
        try {
            FusionSettings.setPlayerLevel(this, Integer.parseInt(editLevel.getText().toString()));
        } catch (NumberFormatException ignored) {}
        FusionSettings.setAutoClearLogs(this, switchAutoClear.isChecked());
    }

    private void updateCacheSize() {
        File dataDir = new File(getFilesDir(), "com.innersloth.spacemafia");
        long size = folderSize(dataDir);
        cacheSizeView.setText("Cache: " + formatSize(size));
    }

    private void clearGameCache() {
        File dataDir = new File(getFilesDir(), "com.innersloth.spacemafia");
        deleteRecursive(dataDir);
    }

    private long folderSize(File dir) {
        long size = 0;
        if (dir == null || !dir.exists()) return 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) size += folderSize(f);
                else size += f.length();
            }
        }
        return size;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"KB", "MB", "GB"};
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024, exp), units[exp - 1]);
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        file.delete();
    }
}
