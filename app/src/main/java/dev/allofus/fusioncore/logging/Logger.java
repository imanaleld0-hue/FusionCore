package dev.allofus.fusioncore.logging;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    private static File logFile;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static void init(Context context) {
        File dir = new File(context.getFilesDir(), "logs");
        if (!dir.exists()) dir.mkdirs();
        logFile = new File(dir, "Logs.txt");
        
        writeHeader(context);
    }

    private static void writeHeader(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== FUSIONCORE SESSION ==========\n");
        sb.append("DATE: ").append(new Date().toString()).append("\n");
        sb.append("ANDROID: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("ABI: ").append(Build.SUPPORTED_ABIS[0]).append("\n");
        sb.append("PID: ").append(android.os.Process.myPid()).append("\n");
        sb.append("=========================================\n");
        appendToFile(sb.toString());
    }

    public static synchronized void log(String level, String component, String thread, String message) {
        String line = String.format("%s\n%s\n%s\n%s\n%s\n\n", sdf.format(new Date()), level, component, thread, message);
        Log.d("FusionCore_" + component, message);
        appendToFile(line);
    }

    private static void appendToFile(String content) {
        if (logFile == null) return;
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(content);
        } catch (Exception ignored) {}
    }
}
