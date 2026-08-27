package dev.allofus.fusioncore.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ConfigEditorActivity extends BaseFullscreenActivity {

    private EditText editor;
    private File configFile;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        Button saveBtn = new Button(this);
        saveBtn.setText("Save Configuration");

        editor = new EditText(this);

        editor.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );

        editor.setGravity(Gravity.TOP | Gravity.START);

        layout.addView(saveBtn);
        layout.addView(editor);

        setContentView(layout);

        configFile = new File(getFilesDir(), "config.cfg");

        loadConfig();

        saveBtn.setOnClickListener(v -> saveConfig());
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            return;
        }

        if (configFile.length() > MAX_FILE_SIZE) {
            Toast.makeText(
                    this,
                    "Configuration file is too large.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {

            byte[] bytes = new byte[(int) configFile.length()];

            int read = fis.read(bytes);

            if (read > 0) {
                editor.setText(
                        new String(
                                bytes,
                                0,
                                read,
                                StandardCharsets.UTF_8
                        )
                );
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Failed to load configuration.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void saveConfig() {
        byte[] data = editor
                .getText()
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        if (data.length > MAX_FILE_SIZE) {
            Toast.makeText(
                    this,
                    "Configuration file is too large.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try (FileOutputStream fos = new FileOutputStream(configFile)) {

            fos.write(data);
            fos.flush();

            Toast.makeText(
                    this,
                    "Configuration saved successfully.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Failed to save configuration.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
