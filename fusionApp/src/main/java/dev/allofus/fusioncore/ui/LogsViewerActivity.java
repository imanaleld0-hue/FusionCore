package dev.allofus.fusioncore.ui;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class LogsViewerActivity extends BaseFullscreenActivity {

    private TextView logTextView;

    private static final int CHUNK_LINES = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);

        logTextView = new TextView(this);
        logTextView.setTextIsSelectable(true);
        logTextView.setFocusable(false);
        logTextView.setFocusableInTouchMode(false);
        logTextView.setPadding(16, 16, 16, 16);
        logTextView.setTextSize(12f);

        scrollView.addView(logTextView);

        setContentView(scrollView);

        loadLogTail();
    }

    private void loadLogTail() {
        File logFile = new File(getFilesDir(), "logs/Logs.txt");

        if (!logFile.exists()) {
            logTextView.setText("Logs are empty.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {

            long length = raf.length();

            if (length == 0) {
                logTextView.setText("");
                return;
            }

            long pos = length - 1;
            int lines = 0;

            while (pos >= 0 && lines < CHUNK_LINES) {
                raf.seek(pos);

                if (raf.readByte() == '\n') {
                    lines++;
                }

                pos--;
            }

            long start = pos + 1;
            long size = length - start;

            if (size > Integer.MAX_VALUE) {
                logTextView.setText("Log section is too large.");
                return;
            }

            raf.seek(start);

            byte[] bytes = new byte[(int) size];

            raf.readFully(bytes);

            logTextView.setText(
                    new String(bytes, StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            logTextView.setText(
                    "Failed to read logs: " + e.getMessage()
            );
        }
    }
}
