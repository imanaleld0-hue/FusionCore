package dev.allofus.fusioncore.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LogsViewerActivity extends BaseFullscreenActivity {
    private TextView tvLogs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> buffer = new ArrayList<>();
    private Process logcatProcess;
    private boolean running = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tvLogs = new TextView(this);
        tvLogs.setTextIsSelectable(true);
        tvLogs.setPadding(16, 16, 16, 16);
        tvLogs.setTextSize(12);
        setContentView(tvLogs);
        startLogcat();
    }

    private void startLogcat() {
        new Thread(() -> {
            try {
                logcatProcess = Runtime.getRuntime().exec(new String[]{"logcat", "-v", "threadtime", "-T", "500", "FusionCore:D", "*:S"});
                BufferedReader br = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                String line;
                while (running && (line = br.readLine()) != null) {
                    synchronized (buffer) {
                        buffer.add(line);
                        if (buffer.size() > 500) buffer.remove(0);
                    }
                    handler.post(this::updateUi);
                }
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(this, "Logcat error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "logcat-reader").start();
    }

    private void updateUi() {
        synchronized (buffer) {
            StringBuilder sb = new StringBuilder();
            for (String s : buffer) sb.append(s).append('\n');
            tvLogs.setText(sb.toString());
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        running = false;
        if (logcatProcess != null) logcatProcess.destroy();
    }
}
