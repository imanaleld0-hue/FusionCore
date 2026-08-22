package dev.allofus.fusioncore;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LogsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        ImageButton backButton = findViewById(R.id.logs_action_back);
        backButton.setOnClickListener(v -> finish());

        TextView logsText = findViewById(R.id.logs_text);
        logsText.setText(readLogs());
    }

    private String readLogs() {
        File logDir = new File(getExternalFilesDir(null), "logs");
        File logFile = new File(logDir, "fusioncore-logcat.log");

        if (!logFile.exists()) {
            return "No logs found yet.\nPath: " + logFile.getAbsolutePath();
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            return "Failed to read logs: " + e.getMessage();
        }

        return sb.toString();
    }
}

