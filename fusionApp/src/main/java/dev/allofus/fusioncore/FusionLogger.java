package dev.allofus.fusioncore;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FusionLogger {

    private static File logFile;

    private FusionLogger() {
    }

    public static synchronized void init() {
        if (logFile != null) {
            return;
        }

        File baseDir = new File(
                android.os.Environment.getExternalStorageDirectory(),
                "FusionCore/logs"
        );

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Log.e("FusionCore", "Failed to create log directory");
            return;
        }

        logFile = new File(baseDir, "fusioncore.log");
    }

    public static synchronized void write(String message) {
        if (logFile == null) {
            init();
        }

        if (logFile == null) {
            return;
        }

        String time = new SimpleDateFormat(
                "HH:mm:ss.SSS",
                Locale.getDefault()
        ).format(new Date());

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write("[" + time + "] " + message + "\n");
        } catch (IOException e) {
            Log.e("FusionCore", "Failed to write log", e);
        }
    }
}