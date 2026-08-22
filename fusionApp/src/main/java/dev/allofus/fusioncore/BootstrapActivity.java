package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.pm.ActivityInfo;
import android.widget.Toast;
import dev.allofus.fusioncore.bridge.ActivityBridge;
import dev.allofus.fusioncore.bridge.GoogleSignInHelper;
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
    private TextView stageView, operationView, progressDetailsView, percentView, logs;
    private View errorPanel;
    private TextView errorView;
    private View retryButton;
    private ProgressBar downloadProgress;
    private volatile PreparedFusionState preparedState;

    @Override protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);
        enableImmersiveMode();
        ActivityBridge.initialize(this);
        logs = findViewById(R.id.bootstrap_logs);
        errorPanel = findViewById(R.id.bootstrap_error_panel);
        errorView = findViewById(R.id.bootstrap_error);
        retryButton = findViewById(R.id.bootstrap_retry);
        if (errorPanel != null) errorPanel.setVisibility(View.GONE);
        stageView = findViewById(R.id.bootstrap_stage);
        operationView = findViewById(R.id.bootstrap_operation);
        progressDetailsView = findViewById(R.id.bootstrap_details);
        percentView = findViewById(R.id.bootstrap_percent);
        downloadProgress = findViewById(R.id.bootstrap_download_progress);
        setPhaseStatus("Initializing");
        if (FusionSettings.isAutoClearLogs(this)) autoClearLogs();
        String targetPackage = getIntent().getStringExtra(EXTRA_TARGET_PACKAGE);
        if (targetPackage == null || targetPackage.isEmpty()) {
            failAndFinish("No target package specified!", null); return;
        }
        if (!bootstrapStarted.compareAndSet(false, true)) {
            Log.w(TAG, "Bootstrap already started"); return;
        }
        new Thread(() -> runBootstrapFlow(targetPackage), "bootstrap-flow").start();
    }

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void autoClearLogs() {
        File logDir = new File(getExternalFilesDir(null), "logs");
        if (logDir.exists() && logDir.isDirectory()) {
            File[] files = logDir.listFiles();
            if (files != null) for (File f : files) f.delete();
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ActivityBridge.handleGooglePlayMergeIntent(intent)) Log.i(TAG, "Auth intent processed");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        boolean handled = GoogleSignInHelper.handleActivityResult(requestCode, data,
            new GoogleSignInHelper.AuthCallback() {
                @Override public void onSuccess(com.google.android.gms.auth.api.signin.GoogleSignInAccount a) {
                    Toast.makeText(BootstrapActivity.this, "Signed in: " + a.getDisplayName(), Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(Exception e) {
                    Toast.makeText(BootstrapActivity.this, "Sign-in failed", Toast.LENGTH_SHORT).show();
                }
            });
        if (!handled) Log.d(TAG, "onActivityResult: " + requestCode);
    }

    @Override protected void onDestroy() { super.onDestroy(); ActivityBridge.cleanup(); }

    private void runBootstrapFlow(String targetPackage) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(targetPackage);
        if (launchIntent == null) { failAndFinish("No launch intent for " + targetPackage, null); return; }
        ComponentName launcher = launchIntent.getComponent();
        if (launcher == null) launcher = launchIntent.resolveActivity(getPackageManager());
        if (launcher == null) { failAndFinish("No launcher for " + targetPackage, null); return; }
        Context gameContext;
        try { gameContext = createPackageContext(targetPackage, CONTEXT_IGNORE_SECURITY | CONTEXT_INCLUDE_CODE); }
        catch (Exception e) { failAndFinish("Failed context for " + targetPackage, e); return; }
        boolean useOriginalLibUnity = getIntent().getBooleanExtra(EXTRA_USE_ORIGINAL_LIBUNITY, false);
        try { preparedState = prepareFusionState(this, gameContext, targetPackage, useOriginalLibUnity); }
        catch (Throwable t) { failAndFinish("Prepare failed", t); return; }
        final String launcherClassName = launcher.getClassName();
        Class<?> launcherClass;
        try { launcherClass = gameContext.getClassLoader().loadClass(launcherClassName); }
        catch (ClassNotFoundException e) { Log.e(TAG, "Class not found"); return; }
        setPhaseStatus(getString(R.string.bootstrap_status_installing_hooks));
        try {
            ClassLoaderHooks.installHooks(gameContext.getClassLoader());
            PackageManagerHooks.installHooks(getPackageManager());
            InstrumentationHooks.install();
            UnityPlayerHooks.installHooks(gameContext);
        } catch (Exception e) { Log.e(TAG, "Hooks failed", e); }
        try {
            setPhaseStatus(getString(R.string.bootstrap_status_launching));
            initializeFusion(launcherClassName, targetPackage);
            runOnMainThread(() -> {
                try {
                    var intent = new Intent(this, launcherClass);
                    var wrapped = new Intent(this, StubActivity.class);
                    wrapped.putExtra(InstrumentationHooks.EXTRA_IS_DYNAMIC_ACTIVITY, true);
                    wrapped.putExtra(InstrumentationHooks.EXTRA_ORIGINAL_INTENT, intent);
                    startActivity(wrapped);
                } catch (Throwable t) { failAndFinish("Launch failed: " + launcherClassName, t); }
            });
        } catch (Exception e) { failAndFinish("Launch failed: " + launcherClassName, e); }
    }

    private void setPhaseStatus(String status) {
        runOnMainThread(() -> {
            if (stageView != null) stageView.setText(status);
            if (operationView != null) operationView.setText("Preparing...");
            if (downloadProgress != null) { downloadProgress.setVisibility(View.GONE); downloadProgress.setProgress(0); }
            if (progressDetailsView != null) { progressDetailsView.setVisibility(View.GONE); progressDetailsView.setText(""); }
        });
    }

    private void addLog(String line) { runOnMainThread(() -> { if (logs != null) logs.append(line + "\n"); }); }

    private void setDownloadStatus(long down, long total) {
        runOnMainThread(() -> {
            boolean hasTotal = total > 0;
            if (downloadProgress != null) {
                downloadProgress.setVisibility(View.VISIBLE);
                downloadProgress.setIndeterminate(!hasTotal);
                if (hasTotal) {
                    int p = (int) Math.max(0, Math.min(100, (down * 100) / total));
                    downloadProgress.setProgress(p);
                    if (percentView != null) percentView.setText(p + "%");
                } else if (percentView != null) percentView.setText("…");
            }
            if (stageView != null) stageView.setText("Downloading");
            if (operationView != null) operationView.setText("Downloading libunity.so");
            if (progressDetailsView != null) {
                progressDetailsView.setVisibility(View.VISIBLE);
                progressDetailsView.setText(formatBytes(down) + " / " + (hasTotal ? formatBytes(total) : "?"));
            }
        });
    }

    private String formatBytes(long b) {
        if (b < 1024) return b + " B";
        double v = b; int i = 0;
        String[] u = {"B","KB","MB","GB"};
        while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
        return String.format(Locale.US, "%.1f %s", v, u[i]);
    }

    private void failAndFinish(String msg, Throwable err) {
        if (err != null) Log.e(TAG, msg, err); else Log.e(TAG, msg);
        FusionLogger.write(msg + (err != null ? "\n" + Log.getStackTraceString(err) : ""));
        runOnMainThread(() -> {
            if (stageView != null) stageView.setText("Bootstrap failed");
            if (operationView != null) operationView.setText(msg);
            if (percentView != null) percentView.setText("!");
            if (downloadProgress != null) downloadProgress.setVisibility(View.GONE);
            if (progressDetailsView != null) { progressDetailsView.setVisibility(View.VISIBLE); progressDetailsView.setText(err != null ? Log.getStackTraceString(err) : msg); }
            if (errorPanel != null) errorPanel.setVisibility(View.VISIBLE);
            if (errorView != null) errorView.setText(msg);
            if (retryButton != null) retryButton.setOnClickListener(v -> { if (errorPanel != null) errorPanel.setVisibility(View.GONE); if (percentView != null) percentView.setText("0%"); setPhaseStatus("Retrying"); });
        });
    }

    private void runOnMainThread(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run(); else runOnUiThread(r);
    }

    private void initializeFusion(String launcherName, String targetPackage) {
        if (!fusionInitialized.compareAndSet(false, true)) return;
        PreparedFusionState p = preparedState;
        if (p == null || !targetPackage.equals(p.targetPackage)) { Log.e(TAG, "Config not prepared"); return; }
        Log.i(TAG, "Init Fusion for " + targetPackage);
        try {
            FusionConfig cfg = p.config;
            NativeLibraryManager.addFusionLibrary("main");
            NativeLibraryManager.addFusionLibrary("fusion");
            NativeLibraryManager.addDataLibrary("il2cpp");
            NativeLibraryManager.addDataLibrary("unity");
            NativeLibraryManager.setupLibraryHooks(cfg);
            File staged = FusionConfigStore.write(this, cfg);
            Log.i(TAG, "Config staged at " + staged.getAbsolutePath());
        } catch (Throwable t) { Log.e(TAG, "Init failed", t); }
    }

    private PreparedFusionState prepareFusionState(Context appCtx, Context gameCtx, String targetPkg, boolean useOgUnity) {
        String gameLibDir = gameCtx.getApplicationInfo().nativeLibraryDir;
        String appLibDir = appCtx.getApplicationInfo().nativeLibraryDir;
        String targetAbi = resolveTargetGameAbi(gameLibDir);
        File appDataDir = new File(appCtx.getFilesDir(), targetPkg);
        File sdData = new File(new File(Environment.getExternalStorageDirectory(), "FusionCore"), targetPkg);
        setPhaseStatus(getString(R.string.bootstrap_status_copy_assets));
        File copiedData = new File(appDataDir, "Data_copy");
        boolean copied = Utilities.copyAssets(gameCtx.getAssets(), "bin/Data", copiedData);
        if (!copied) Log.e(TAG, "Copy assets failed"); else applyGlobalMetadataOverride(sdData, copiedData);
        setPhaseStatus(getString(R.string.bootstrap_status_detecting_version));
        String version = VersionLookup.TryLookup(copiedData);
        if (version == null) { Log.e(TAG, "Version detect failed"); version = BACKUP_UNITY_VERSION; useOgUnity = true; }
        else if (useOgUnity) Log.i(TAG, "Skip libunity download");
        else {
            Log.i(TAG, "Unity version: " + version);
            if (LibUnityDownloader.downloadAndCacheSafely(appDataDir, version, targetAbi, new LibUnityDownloader.DownloadProgressListener() {
                @Override public void onDownloadStarted(String url, long total) { setDownloadStatus(0, total); }
                @Override public void onDownloadProgress(long down, long total) { setDownloadStatus(down, total); }
                @Override public void onDownloadFinished(boolean ok, boolean cache) {}
            })) Log.i(TAG, "libunity downloaded");
            else { Log.e(TAG, "libunity download failed"); useOgUnity = true; }
        }
        setPhaseStatus(getString(R.string.bootstrap_status_extracting_runtime));
        File dotnetDir = new File(appDataDir, "dotnet");
        File bepDir = new File(sdData, "BepInEx");
        Utilities.extractZipFromAssets(appCtx, "BepInEx-arm64.zip", bepDir);
        Utilities.extractZipFromAssets(appCtx, "dotnet-arm64.zip", dotnetDir);
        setPhaseStatus(getString(R.string.bootstrap_status_registering_libraries));
        File[] nativeLibs = new File(gameLibDir).listFiles();
        if (nativeLibs != null) {
            for (File file : nativeLibs) {
                String name = file.getName();
                if (name.startsWith("lib") && name.endsWith(".so") && name.length() > 6) {
                    NativeLibraryManager.addGameLibrary(name.substring(3, name.length() - 3));
                }
            }
        } else Log.e(TAG, "No native libs found");
        FusionConfig cfg = new FusionConfig(gameLibDir, appLibDir, appDataDir.getAbsolutePath(), bepDir.getAbsolutePath(), dotnetDir.getAbsolutePath(), copiedData.getAbsolutePath(), version, useOgUnity);
        return new PreparedFusionState(targetPkg, cfg);
    }

    private void applyGlobalMetadataOverride(File sdData, File copiedData) {
        File override = new File(sdData, GLOBAL_METADATA_FILE);
        if (!override.isFile()) { Log.i(TAG, "No metadata override"); return; }
        File target = new File(new File(copiedData, "Managed/Metadata"), GLOBAL_METADATA_FILE);
        try { copyFile(override, target); Log.i(TAG, "Metadata override applied"); }
        catch (IOException e) { throw new IllegalStateException("Metadata override failed", e); }
    }

    private static void copyFile(File src, File dst) throws IOException {
        File parent = dst.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("mkdirs failed");
        byte[] buf = new byte[8192];
        try (FileInputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst, false)) {
            int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    private record PreparedFusionState(String targetPackage, FusionConfig config) {}
    private String resolveTargetGameAbi(String dir) { if (dir == null || dir.isEmpty()) return null; String a = new File(dir).getName(); return a.isEmpty() ? null : a; }
}
