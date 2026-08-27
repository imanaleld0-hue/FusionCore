
package dev.allofus.fusioncore.util;

import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FusionCoreLogger {
    private static final String TAG = "FusionCoreLogger";
    private static File logFile;
    private static final Object lock = new Object();
    private static boolean initialized = false;

    public static void init(File baseDir) {
        if (initialized) return;
        File logDir = new File(baseDir, "logs");
        if (!logDir.exists()) logDir.mkdirs();
        logFile = new File(logDir, "Logs.txt");
        initialized = true;
        logSessionHeader(baseDir);
    }

    private static void logSessionHeader(File baseDir) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String header = "\n========== FUSIONCORE SESSION ==========\n" +
                "DATE: " + sdf.format(new Date()) + "\n" +
                "ANDROID: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n" +
                "ABI: " + Build.SUPPORTED_ABIS[0] + "\n" +
                "DEVICE: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                "========================================\n";
        write("SYSTEM", "INFO", "Session", header);
    }

    public static void d(String component, String message) {
        Log.d(component, message);
        write(component, "DEBUG", Thread.currentThread().getName(), message);
    }

    public static void i(String component, String message) {
        Log.i(component, message);
        write(component, "INFO", Thread.currentThread().getName(), message);
    }

    public static void w(String component, String message) {
        Log.w(component, message);
        write(component, "WARN", Thread.currentThread().getName(), message);
    }

    public static void e(String component, String message, Throwable t) {
        Log.e(component, message, t);
        write(component, "ERROR", Thread.currentThread().getName(), message + "\n" + Log.getStackTraceString(t));
    }

    private static void write(String component, String level, String thread, String message) {
        if (!initialized || logFile == null) return;
        synchronized (lock) {
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
                String time = sdf.format(new Date());
                pw.println(time + " [" + level + "] [" + component + "] [" + thread + "] " + message);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write log", e);
            }
        }
    }

    public static File getLogFile() {
        return logFile;
    }
}
