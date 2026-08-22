package dev.allofus.fusioncore;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogcatCollector {
    private static final String TAG = "LogcatCollector";
    private static Process process;
    private static Thread collectorThread;
    private static final AtomicBoolean running = new AtomicBoolean(false);

    public static void start(Context ctx) {
        if (running.compareAndSet(false, true)) {
            File logDir = new File(ctx.getExternalFilesDir(null), "logs");
            if (!logDir.exists()) logDir.mkdirs();
            collectorThread = new Thread(() -> collect(logDir), "logcat-collector");
            collectorThread.start();
        }
    }

    private static void collect(File logDir) {
        File mainLog = new File(logDir, "Java.log");
        File nativeLog = new File(logDir, "Native.log");
        File bepLog = new File(logDir, "BepInEx.log");
        File hookLog = new File(logDir, "Hooking.log");
        try {
            process = Runtime.getRuntime().exec("logcat -v threadtime");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            FileWriter mainWriter = new FileWriter(mainLog, true);
            FileWriter nativeWriter = new FileWriter(nativeLog, true);
            FileWriter bepWriter = new FileWriter(bepLog, true);
            FileWriter hookWriter = new FileWriter(hookLog, true);
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                if (isNoise(line)) continue;
                if (line.contains("BepInEx") || line.contains("Reactor")) {
                    bepWriter.write(line + "\n"); bepWriter.flush();
                } else if (line.contains("Pine") || line.contains("ClassLoaderHooks") || line.contains("UnityPlayerHooks")) {
                    hookWriter.write(line + "\n"); hookWriter.flush();
                } else if (line.contains("libc") || line.contains("DEBUG") || line.contains("libmain") || line.contains("libunity")) {
                    nativeWriter.write(line + "\n"); nativeWriter.flush();
                } else {
                    mainWriter.write(line + "\n"); mainWriter.flush();
                }
            }
            mainWriter.close(); nativeWriter.close(); bepWriter.close(); hookWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "Logcat collect failed", e);
        }
    }

    public static void stop() {
        running.set(false);
        if (process != null) process.destroy();
    }

    public static void clearLogs(Context ctx) {
        File logDir = new File(ctx.getExternalFilesDir(null), "logs");
        if (logDir.exists() && logDir.isDirectory()) {
            File[] files = logDir.listFiles();
            if (files != null) for (File f : files) f.delete();
        }
    }

    private static boolean isNoise(String line) {
        if (line == null) return true;
        String l = line.toLowerCase();
        return l.contains("miui") || l.contains("wmdebug") || l.contains("forcedark") || l.contains("handwriting")
                || l.contains("blastbuffer") || l.contains("userscenedetector") || l.contains("vri[")
                || l.contains("inputeventreceiver") || l.contains("inputtransport") || l.contains("perfmonitor")
                || l.contains("decorviewimmersive") || l.contains("bufferqueue") || l.contains("surfaceview");
    }
}
