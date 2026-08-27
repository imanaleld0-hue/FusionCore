package dev.allofus.fusioncore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import dev.allofus.fusioncore.ui.BaseFullscreenActivity;
import dev.allofus.fusioncore.ui.LogsViewerActivity;
import dev.allofus.fusioncore.ui.ModProjectsActivity;
import dev.allofus.fusioncore.ui.SettingsActivity;

public class SelectorActivity extends BaseFullscreenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);

        Button btnLaunch = findViewById(R.id.btn_launch);
        Button btnMods = findViewById(R.id.btn_mods);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnLogs = findViewById(R.id.btn_logs);

        btnLaunch.setOnClickListener(v -> {
            Intent intent = new Intent(this, BootstrapActivity.class);
            startActivity(intent);
        });

        btnMods.setOnClickListener(v -> {
            startActivity(new Intent(this, ModProjectsActivity.class));
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnLogs.setOnClickListener(v -> {
            startActivity(new Intent(this, LogsViewerActivity.class));
        });
    }
}
