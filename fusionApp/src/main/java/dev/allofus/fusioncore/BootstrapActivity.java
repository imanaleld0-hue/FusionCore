package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class BootstrapActivity extends Activity {

    private static final String TAG = "FusionCore";

    public static final String EXTRA_TARGET_PACKAGE = "target_package";
    public static final String EXTRA_USE_ORIGINAL_LIBUNITY = "og_libunity";
    public static final String BACKUP_UNITY_VERSION = "2017.0.0";
    private static final String GLOBAL_METADATA_FILE = "global-metadata.dat";

    private final AtomicBoolean fusionInitialized = new AtomicBoolean(false);
    private final AtomicBoolean bootstrapStarted = new AtomicBoolean(false);
    private TextView currentAction;
    private TextView logs;

    private TextView stageView;
    private TextView operationView;
    private TextView progressDetailsView;
    private TextView percentView;

    private View errorPanel;
    private TextView errorView;
    private View retryButton;

    private ProgressBar downloadProgress;
    private volatile PreparedFusionState preparedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);
        currentAction = findViewById(R.id.bootstrap_operation);
        logs = findViewById(R.id.bootstrap_logs);
        errorPanel = findViewById(R.id.bootstrap_error_panel);
        errorView = findViewById(R.id.bootstrap_error);
        retryButton = findViewById(R.id.bootstrap_retry);

        if (errorPanel != null) {
            errorPanel.setVisibility(View.GONE);
        }

        stageView = findViewById(R.id.bootstrap_stage);
        operationView = findViewById(R.id.bootstrap_operation);
        progressDetailsView = findViewById(R.id.bootstrap_details);
        percentView = findViewById(R.id.bootstrap_percent);

        downloadProgress = findViewById(R.id.bootstrap_download_progress);

        setPhaseStatus("Initializing");

        String targetPackage = getIntent().getStringExtra(EXTRA_TARGET_PACKAGE);
        if (targetPackage == null || targetPackage.isEmpty()) {
            failAndFinish("No target package specified in intent extras!", null);
            return;
        }

        if (!bootstrapStarted.compareAndSet(false, true)) {
            Log.w(TAG, "Bootstrap flow already started, ignoring duplicate start");
            return;
        }

        new Thread(
            () -> runBootstrapFlow(targetPackage),
            "bootstrap-flow"
        ).start();
    }

    private void runBootstrapFlow(String targetPackage) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(targetPackage);
        if (launchIntent == null) {
            failAndFinish("No launch intent for target package: " + targetPackage, null);
            return;
        }

        ComponentName launcher = launchIntent.getComponent();
        if (launcher == null) {
            launcher = launchIntent.resolveActivity(getPackageManager());
        }

        if (launcher == null) {
            failAndFinish("Failed to resolve launcher activity for target package: " + targetPackage, null);
            return;
        }

        Context gameContext;
        try {
            gameContext = createPackageContext(targetPackage, CONTEXT_IGNORE_SECURITY | CONTEXT_INCLUDE_CODE);
        } catch (Exception e) {
            failAndFinish("Failed to create package context for target package: " + targetPackage, e);
            return;
        }

        boolean useOriginalLibUnity = getIntent().getBooleanExtra(EXTRA_USE_ORIGINAL_LIBUNITY, false);

        try {
            preparedState = prepareFusionState(this, gameContext, targetPackage, useOriginalLibUnity);
        } catch (Throwable t) {
            failAndFinish("Failed while preparing Fusion runtime.", t);
            return;
        }

        final String launcherClassName = launcher.getClassName();
        Class<?> launcherClass;
        try {
            launcherClass = gameContext.getClassLoader().loadClass(launcherClassName);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Failed to get class for launcher activity!");
            return;
        }

        setPhaseStatus(getString(R.string.bootstrap_status_installing_hooks));
        try {
            ClassLoaderHooks.installHooks(gameContext.getClassLoader());
            PackageManagerHooks.installHooks(getPackageManager());
            InstrumentationHooks.install();
            UnityPlayerHooks.installHooks(gameContext);
        } catch (Exception e) {
            Log.e(TAG, "Failed to install base hooks", e);
        }

        try {
            setPhaseStatus(getString(R.string.bootstrap_status_launching));
            initializeFusion(launcherClassName, targetPackage);
            runOnMainThread(() -> {
                try {
                    var intent = new Intent(this, launcherClass);
                    var intentWrapped = new Intent(this, StubActivity.class);
                    intentWrapped.putExtra(InstrumentationHooks.EXTRA_IS_DYNAMIC_ACTIVITY, true);
                    intentWrapped.putExtra(InstrumentationHooks.EXTRA_ORIGINAL_INTENT, intent);
                    startActivity(intentWrapped);
                } catch (Throwable t) {
                    failAndFinish("Failed to launch target app's launcher activity: " + launcherClassName, t);
                }
            });
        } catch (Exception e) {
            failAndFinish("Failed to launch target app's launcher activity: " + launcherClassName, e);
        }
    }

    private void setCurrentAction(String action) {
        runOnMainThread(() -> {
            if (operationView != null) {
                operationView.setText(action);
            }
            addLog(action);
            FusionLogger.write(action);
        });
    }

    private void setPhaseStatus(String status) {
        runOnMainThread(() -> {
            if (stageView != null) {
                stageView.setText(status);
            }
            if (operationView != null) {
                operationView.setText("Preparing...");
            }
            if (downloadProgress != null) {
                downloadProgress.setVisibility(View.GONE);
                downloadProgress.setIndeterminate(false);
                downloadProgress.setProgress(0);
            }
            if (progressDetailsView != null) {
                progressDetailsView.setVisibility(View.GONE);
                progressDetailsView.setText("");
            }
        });
    }

    private void addLog(String line) {
        runOnMainThread(() -> {
            if (logs != null) {
                logs.append(line + "
");
            }
        });
    }

    private void setDownloadStatus(long downloadedBytes, long totalBytes) {
        runOnMainThread(() -> {
            boolean hasTotal = totalBytes > 0L;

            if (downloadProgress != null) {
                downloadProgress.setVisibility(View.VISIBLE);
                downloadProgress.setIndeterminate(!hasTotal);

                if (hasTotal) {
                    int percent = (int) Math.max(
                            0L,
                            Math.min(
                                    100L,
                                    (downloadedBytes * 100L) / totalBytes
                            )
                    );
                    downloadProgress.setProgress(percent);
                    if (percentView != null) {
                        percentView.setText(percent + "%");
                    }
                } else {
                    if (percentView != null) {
                        percentView.setText("…");
                    }
                }
            }

            if (stageView != null) {
                stageView.setText("Downloading");
            }
            if (operationView != null) {
                operationView.setText("Downloading libunity.so");
            }
            if (progressDetailsView != null) {
                progressDetailsView.setVisibility(View.VISIBLE);
                String totalText = hasTotal ? formatBytes(totalBytes) : "?";
                progressDetailsView.setText(
                        formatBytes(downloadedBytes) + " / " + totalText
                );
            }
        });
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = new String[]{"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex]);
    }

    private void failAndFinish(String message, Throwable error) {
        if (error != null) {
            Log.e(TAG, message, error);
        } else {
            Log.e(TAG, message);
        }

        FusionLogger.write(
                message + (error != null
                        ? "
" + Log.getStackTraceString(error)
                        : "")
        );

        runOnMainThread(() -> {
            if (stageView != null) {
                stageView.setText("Bootstrap failed");
            }
            if (operationView != null) {
                operationView.setText(message);
            }
            if (percentView != null) {
                percentView.setText("!");
            }
            if (downloadProgress != null) {
                downloadProgress.setVisibility(View.GONE);
            }
            if (progressDetailsView != null) {
                progressDetailsView.setVisibility(View.VISIBLE);
                progressDetailsView.setText(
                        error != null
                                ? Log.getStackTraceString(error)
                                : message
                );
            }
            if (errorPanel != null) {
                errorPanel.setVisibility(View.VISIBLE);
            }
            if (errorView != null) {
                errorView.setText(message);
            }
            if (retryButton != null) {
                retryButton.setOnClickListener(v -> {
                    if (errorPanel != null) {
                        errorPanel.setVisibility(View.GONE);
                    }
                    if (percentView != null) {
                        percentView.setText("0%");
                    }
                    setPhaseStatus("Retrying");
                });
            }
        });
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            runOnUiThread(runnable);
        }
    }

    private void updateBootstrapPercent(int percent) {
        runOnMainThread(() -> {
            int safePercent = Math.max(0, Math.min(100, percent));
            if (percentView != null) {
                percentView.setText(safePercent + "%");
            }
            if (downloadProgress != null && !downloadProgress.isIndeterminate()) {
                downloadProgress.setProgress(safePercent);
            }
        });
    }

    private void initializeFusion(String launcherName, String targetPackage) {
        if (!fusionInitialized.compareAndSet(false, true)) {
            return;
        }

        PreparedFusionState prepared = preparedState;
        if (prepared == null || !targetPackage.equals(prepared.targetPackage)) {
            Log.e(TAG, "Fusion config was not prepared for target package: " + targetPackage);
            return;
        }

        Log.i(TAG, "Initializing Fusion for " + targetPackage + " via " + launcherName);

        try {
            FusionConfig config = prepared.config;

            NativeLibraryManager.addFusionLibrary("main");
            NativeLibraryManager.addFusionLibrary("fusion");
            NativeLibraryManager.addDataLibrary("il2cpp");
            NativeLibraryManager.addDataLibrary("unity");
            NativeLibraryManager.setupLibraryHooks(config);

            File stagedConfig = FusionConfigStore.write(this, config);
            Log.i(TAG, "Fusion config staged at " + stagedConfig.getAbsolutePath());
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize Fusion in launcher beforeCall", t);
        }
    }

    private PreparedFusionState prepareFusionState(Context appContext,
            Context gameContext,
            String targetPackage,
            boolean useOriginalLibUnity) {
        String gameLibDir = gameContext.getApplicationInfo().nativeLibraryDir;
        String appLibDir = appContext.getApplicationInfo().nativeLibraryDir;
        String targetGameAbi = resolveTargetGameAbi(gameLibDir);
        File appDataDir = new File(appContext.getFilesDir(), targetPackage);
        File dataOnSdCard = new File(new File(Environment.getExternalStorageDirectory(), "FusionCore"), targetPackage);

        setPhaseStatus(getString(R.string.bootstrap_status_copy_assets));
        File copiedData = new File(appDataDir, "Data_copy");
        boolean copied = Utilities.copyAssets(gameContext.getAssets(), "bin/Data", copiedData);
        if (!copied) {
            Log.e(TAG, "Failed to copy Unity Data assets! BepInEx may not work correctly.");
        } else {
            applyGlobalMetadataOverride(dataOnSdCard, copiedData);
        }

        setPhaseStatus(getString(R.string.bootstrap_status_detecting_version));
        String version = VersionLookup.TryLookup(copiedData);
        if (version == null) {
            Log.e(TAG, "Failed to determine Unity version! BepInEx may not work correctly.");
            version = BACKUP_UNITY_VERSION;
            useOriginalLibUnity = true;
        } else if (useOriginalLibUnity) {
            Log.i(TAG, "Skipping libunity download");
        } else {
            Log.i(TAG, "Determined Unity version: " + version);
            if (LibUnityDownloader.downloadAndCacheSafely(appDataDir, version, targetGameAbi, new LibUnityDownloader.DownloadProgressListener() {
                @Override
                public void onDownloadStarted(String url, long totalBytes) {
                    setDownloadStatus(0L, totalBytes);
                }

                @Override
                public void onDownloadProgress(long downloadedBytes, long totalBytes) {
                    setDownloadStatus(downloadedBytes, totalBytes);
                }

                @Override
                public void onDownloadFinished(boolean success, boolean usedCache) {
                }
            })) {
                Log.i(TAG, "Successfully downloaded libunity for version " + version + " and ABI " + targetGameAbi);
            } else {
                Log.e(TAG, "Failed to download libunity for version " + version + " and ABI " + targetGameAbi + ", falling back to original.");
                useOriginalLibUnity = true;
            }
        }

        setPhaseStatus(getString(R.string.bootstrap_status_extracting_runtime));
        File dotnetDir = new File(appDataDir, "dotnet");
        File bepInExDir = new File(dataOnSdCard, "BepInEx");

        Utilities.extractZipFromAssets(appContext, "BepInEx-arm64.zip", bepInExDir);
        Utilities.extractZipFromAssets(appContext, "dotnet-arm64.zip", dotnetDir);

        setPhaseStatus(getString(R.string.bootstrap_status_registering_libraries));
        File[] nativeLibs = new File(gameLibDir).listFiles();
        if (nativeLibs != null) {
            for (File file : nativeLibs) {
                String name = file.getName();
                if (name.startsWith("lib") && name.endsWith(".so") && name.length() > 6) {
                    String extractedName = name.substring(3, name.length() - 3);
                    NativeLibraryManager.addGameLibrary(extractedName);
                }
            }
        } else {
            Log.e(TAG, "Failed to list game native libraries! BepInEx may not work correctly.");
        }

        FusionConfig config = new FusionConfig(
                gameLibDir,
                appLibDir,
                appDataDir.getAbsolutePath(),
                bepInExDir.getAbsolutePath(),
                dotnetDir.getAbsolutePath(),
                copiedData.getAbsolutePath(),
                version,
                useOriginalLibUnity
        );

        return new PreparedFusionState(targetPackage, config);
    }

    private void applyGlobalMetadataOverride(File dataOnSdCard, File copiedData) {
        File overrideMetadata = new File(dataOnSdCard, GLOBAL_METADATA_FILE);
        if (!overrideMetadata.isFile()) {
            Log.i(TAG, "No global-metadata override found at " + overrideMetadata.getAbsolutePath());
            return;
        }

        File targetMetadata = new File(new File(copiedData, "Managed/Metadata"), GLOBAL_METADATA_FILE);
        try {
            copyFile(overrideMetadata, targetMetadata);
            Log.i(TAG, "Applied global-metadata override from " + overrideMetadata.getAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to apply global-metadata override from "
                    + overrideMetadata.getAbsolutePath(), e);
        }
    }

    private static void copyFile(File source, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
        }

        byte[] buffer = new byte[8192];
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target, false)) {
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }
    }

    private record PreparedFusionState(String targetPackage, FusionConfig config) { }

    private String resolveTargetGameAbi(String gameLibDir) {
        if (gameLibDir == null || gameLibDir.isEmpty()) {
            return null;
        }
        String abi = new File(gameLibDir).getName();
        if (abi.isEmpty()) {
            return null;
        }
        return abi;
    }
}