package dev.allofus.fusioncore;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LogcatCollector {

    private static final String TAG = "FusionCore";
    private static Process process;
    private static Thread collectorThread;
    private static volatile boolean running = false;

    private LogcatCollector() {
    }

    public static synchronized void start(Context context) {
        if (running) {
            return;
        }

        running = true;

        File logDir = new File(
                context.getExternalFilesDir(null),
                "logs"
        );

        if (!logDir.exists() && !logDir.mkdirs()) {
            Log.e(TAG, "Failed to create log directory: " + logDir);
            running = false;
            return;
        }

        File logFile = new File(logDir, "fusioncore-logcat.log");

        collectorThread = new Thread(() -> collect(logFile), "FusionCore-Logcat");
        collectorThread.start();

        Log.i(TAG, "Logcat collector started: " + logFile.getAbsolutePath());
    }

    private static void collect(File logFile) {
        try {
            int pid = android.os.Process.myPid();

            ProcessBuilder builder = new ProcessBuilder(
                    "logcat",
                    "--pid=" + pid,
                    "-v",
                    "threadtime"
            );

            builder.redirectErrorStream(true);

            process = builder.start();

            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream())
                    );
                    FileWriter writer = new FileWriter(logFile, true)
            ) {
                writer.write("\n========== FusionCore logcat started "
                        + timestamp()
                        + " ==========\n");
                writer.flush();

                String line;

                while (running && (line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.write('\n');
                    writer.flush();
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Logcat collector failed", e);
        } finally {
            process = null;
        }
    }

    public static synchronized void stop() {
        running = false;

        if (process != null) {
            process.destroy();
            process = null;
        }

        if (collectorThread != null) {
            collectorThread.interrupt();
            collectorThread = null;
        }

        Log.i(TAG, "Logcat collector stopped");
    }

    private static String timestamp() {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS",
                Locale.getDefault()
        ).format(new Date());
    }
}