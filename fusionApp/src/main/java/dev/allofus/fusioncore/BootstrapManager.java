package dev.allofus.fusioncore.bootstrap;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class BootstrapManager {

    private static final String TAG = "FusionCore";

    private static BootstrapManager instance;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final List<String> logs =
            new ArrayList<>();

    private final Object lock =
            new Object();

    private Future<?> runningTask;

    private BootstrapStatus currentStatus;

    private BootstrapManager() {
    }

    public static synchronized BootstrapManager getInstance() {

        if (instance == null) {
            instance = new BootstrapManager();
        }

        return instance;
    }

    public boolean isRunning() {

        synchronized (lock) {

            return runningTask != null
                    && !runningTask.isDone();
        }
    }

    public BootstrapStatus getCurrentStatus() {

        synchronized (lock) {
            return currentStatus;
        }
    }

    public void start(Runnable bootstrapWork) {

        synchronized (lock) {

            if (runningTask != null
                    && !runningTask.isDone()) {

                log("Bootstrap already running");

                return;
            }

            runningTask = executor.submit(() -> {

                try {

                    publish(
                            BootstrapStage.INITIALIZING,
                            0,
                            -1,
                            "Initializing",
                            true,
                            null
                    );

                    bootstrapWork.run();

                } catch (Throwable e) {

                    Log.e(
                            TAG,
                            "Bootstrap failed",
                            e
                    );

                    publish(
                            BootstrapStage.ERROR,
                            0,
                            -1,
                            "Bootstrap failed",
                            true,
                            e
                    );
                }
            });
        }
    }

    public void publish(
            BootstrapStage stage,
            long completed,
            long total,
            String message,
            boolean indeterminate,
            Throwable error
    ) {

        synchronized (lock) {

            currentStatus =
                    new BootstrapStatus(
                            stage,
                            completed,
                            total,
                            message,
                            indeterminate,
                            error,
                            logs
                    );
        }
    }

    public void log(String message) {

        synchronized (lock) {

            Log.i(TAG, message);

            logs.add(message);

            if (logs.size() > 200) {
                logs.remove(0);
            }
        }
    }

    public void shutdown() {

        executor.shutdownNow();
    }
}
