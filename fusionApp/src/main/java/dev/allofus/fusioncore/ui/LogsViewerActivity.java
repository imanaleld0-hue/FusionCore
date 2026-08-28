package dev.allofus.fusioncore.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import dev.allofus.fusioncore.log.FusionLog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LogsViewerActivity extends BaseFullscreenActivity {

    private static final int MAX_VISIBLE_LINES = 1000;
    private static final String LOGCAT_TAG = "FusionCore";

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final List<String> lines =
            new ArrayList<>();

    private volatile boolean running = false;

    private java.lang.Process process;
    private BufferedReader reader;
    private FileOutputStream logOutput;
    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logView = new TextView(this);
        logView.setTextIsSelectable(true);
        logView.setTextSize(12);
        logView.setPadding(16, 16, 16, 16);

        setContentView(logView);

        startLogcat();
    }

    private void startLogcat() {
        if (running) {
            return;
        }

        running = true;

        new Thread(
                () -> {
                    try {
                        openLogFile();
                        readExistingLogs();
                        startLogcatProcess();
                    } catch (Throwable e) {
                        if (running) {
                            showToast(
                                    "FusionCore log error: "
                                            + getErrorMessage(e)
                            );
                        }
                    } finally {
                        closeResources();
                    }
                },
                "fusion-logcat-reader"
        ).start();
    }

    private void openLogFile() throws IOException {
        File file = FusionLog.getLogFile(this);

        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException(
                        "Cannot create log directory: "
                                + parent.getAbsolutePath()
                );
            }
        }

        logOutput = new FileOutputStream(file, true);
    }

    private void readExistingLogs() {
        File file = FusionLog.getLogFile(this);

        if (!file.isFile()) {
            return;
        }

        try (
                BufferedReader existing =
                        new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(file),
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {
            String line;

            while ((line = existing.readLine()) != null) {
                synchronized (lines) {
                    lines.add(line);

                    if (lines.size() > MAX_VISIBLE_LINES) {
                        lines.remove(0);
                    }
                }
            }

            updateLogView();

        } catch (IOException ignored) {
            
        }
    }

    private void startLogcatProcess() throws IOException {

        String[] command = new String[] {
                "logcat",
                "-v",
                "threadtime",
                "-T",
                "500",
                LOGCAT_TAG + ":D",
                "*:S"
        };

        process = Runtime.getRuntime().exec(command);

        reader =
                new BufferedReader(
                        new InputStreamReader(
                                process.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

        String line;

        while (
                running
                        && reader != null
                        && (line = reader.readLine()) != null
        ) {
            appendLog(line);
        }
    }

    private void appendLog(String line) {

        if (line == null || line.isEmpty()) {
            return;
        }

        synchronized (lines) {
            lines.add(line);

            if (lines.size() > MAX_VISIBLE_LINES) {
                lines.remove(0);
            }
        }

        
        try {
            if (logOutput != null) {
                logOutput.write(
                        (line + "\n")
                                .getBytes(StandardCharsets.UTF_8)
                );

                logOutput.flush();
            }
        } catch (IOException e) {
            if (running) {
                showToast(
                        "Cannot write log: "
                                + getErrorMessage(e)
                );
            }
        }

        updateLogView();
    }

    private void updateLogView() {

        mainHandler.post(
                () -> {

                    if (logView == null) {
                        return;
                    }

                    StringBuilder builder =
                            new StringBuilder();

                    synchronized (lines) {
                        for (String line : lines) {
                            builder
                                    .append(line)
                                    .append('\n');
                        }
                    }

                    logView.setText(builder.toString());


                    logView.post(
                            () -> {
                                if (logView != null) {
                                    logView.scrollTo(
                                            0,
                                            logView.getBottom()
                                    );
                                }
                            }
                    );
                }
        );
    }

    private void closeResources() {

        BufferedReader currentReader = reader;
        reader = null;

        if (currentReader != null) {
            try {
                currentReader.close();
            } catch (IOException ignored) {
            }
        }

        FileOutputStream currentOutput = logOutput;
        logOutput = null;

        if (currentOutput != null) {
            try {
                currentOutput.flush();
            } catch (IOException ignored) {
            }

            try {
                currentOutput.close();
            } catch (IOException ignored) {
            }
        }

        java.lang.Process currentProcess = process;
        process = null;

        if (currentProcess != null) {
            try {
                currentProcess.destroy();
            } catch (Throwable ignored) {
            }

            try {
                if (currentProcess.isAlive()) {
                    currentProcess.destroyForcibly();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private String getErrorMessage(Throwable throwable) {

        if (throwable == null) {
            return "Unknown error";
        }

        String message = throwable.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return throwable.getClass().getSimpleName();
        }

        return message;
    }

    private void showToast(String message) {

        mainHandler.post(
                () ->
                        Toast.makeText(
                                LogsViewerActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show()
        );
    }


    public static void exportLogs(
            Context context,
            Uri destination
    ) throws Exception {

        if (context == null) {
            throw new IllegalArgumentException(
                    "Context is null"
            );
        }

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Destination Uri is null"
            );
        }

        File source =
                FusionLog.getLogFile(context);

        try (
                OutputStream output =
                        context
                                .getContentResolver()
                                .openOutputStream(destination)
        ) {

            if (output == null) {
                throw new IOException(
                        "Cannot open destination"
                );
            }

            if (!source.isFile()) {
                output.write(
                        "FusionCore log is empty\n"
                                .getBytes(StandardCharsets.UTF_8)
                );

                return;
            }

            try (
                    FileInputStream input =
                            new FileInputStream(source)
            ) {

                byte[] buffer = new byte[8192];

                int count;

                while (
                        (count = input.read(buffer))
                                != -1
                ) {
                    output.write(
                            buffer,
                            0,
                            count
                    );
                }

                output.flush();
            }
        }
    }
 
    public static boolean clearLogs(Context context) {

        try {
            File file =
                    FusionLog.getLogFile(context);

            if (!file.exists()) {
                return true;
            }

            return file.delete();

        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {

        running = false;

        closeResources();

        mainHandler.removeCallbacksAndMessages(null);

        logView = null;

        super.onDestroy();
    }
}
