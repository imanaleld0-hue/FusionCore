package dev.allofus.fusioncore.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import dev.allofus.fusioncore.R;

public class ConfigEditorActivity extends BaseFullscreenActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_editor);
        String modId = getIntent().getStringExtra("mod_id");
        Toast.makeText(this, "Editing config for: " + modId, Toast.LENGTH_SHORT).show();
        // Load BepInEx config from mod path and present editor
    }
}
