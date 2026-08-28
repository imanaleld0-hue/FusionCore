package dev.allofus.fusioncore;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.allofus.fusioncore.hooks.ClassLoaderHooks;
import dev.allofus.fusioncore.hooks.InstrumentationHooks;
import dev.allofus.fusioncore.hooks.PackageManagerHooks;
import dev.allofus.fusioncore.tools.FusionConfig;
import dev.allofus.fusioncore.tools.FusionConfigStore;
import dev.allofus.fusioncore.tools.NativeLibraryManager;
import dev.allofus.fusioncore.tools.Utilities;

public class BootstrapActivity extends AppCompatActivity {
    private static final String TAG = "FusionCore";
    public static final String EXTRA_TARGET_PACKAGE = "target_package";
    public static final String EXTRA_USE_ORIGINAL_LIBUNITY = "og_libunity";
    private static final String GLOBAL_METADATA_FILE = "global-metadata.dat";

    private final AtomicBoolean fusionInitialized = new AtomicBoolean(false);
    private TextView statusView;
    private ProgressBar spinnerProgress;
    private volatile PreparedFusionState preparedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);
        statusView = findViewById(R.id.bootstrap_status);
        spinnerProgress = findViewById(R.id.bootstrap_progress);
        setPhaseStatus("Preparing...");

        String targetPackage = getIntent().getStringExtra(EXTRA_TARGET_PACKAGE);
        if (targetPackage == null || targetPackage.isEmpty()) {
            failAndFinish("No target package specified!", null);
            return;
        }
        statusView.post(() -> new Thread(() -> runBootstrapFlow(targetPackage), "bootstrap-flow").start());
    }

    private void runBootstrapFlow(String targetPackage) {
        Log.i(TAG, "[Bootstrap] Starting for package: " + targetPackage);
        Context gameContext;
        try {
            gameContext = createPackageContext(targetPackage, CONTEXT_IGNORE_SECURITY | CONTEXT_INCLUDE_CODE);
        } catch (Exception e) {
            failAndFinish("Failed to create package context: " + targetPackage, e);
            return;
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(targetPackage);
        if (launchIntent == null) {
            failAndFinish("No launch intent for: " + targetPackage, null);
            return;
        }

        ComponentName launcher = launchIntent.getComponent();
        if (launcher == null) launcher = launchIntent.resolveActivity(getPackageManager());

        String overrideActivity = FusionSettings.getActivityOverrideForGame(this, targetPackage);
        try {
            if (!"Automatic".equals(overrideActivity)) {
                var overrideClass = gameContext.getClassLoader().loadClass(overrideActivity);
                if (overrideClass != null) {
                    launcher = new ComponentName(targetPackage, overrideActivity);
                    Log.i(TAG, "[Bootstrap] Using override activity: " + overrideActivity);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "[Bootstrap] Override activity failed", e);
        }

        if (launcher == null) {
            failAndFinish("Failed to resolve launcher activity!", null);
            return;
        }

        final int targetOrientation = resolveTargetOrientation(launcher);

        try {
            preparedState = prepareFusionState(this, gameContext, targetPackage);
        } catch (Throwable t) {
            failAndFinish("Failed while preparing Fusion runtime.", t);
            return;
        }

        final String launcherClassName = launcher.getClassName();
        Class<?> launcherClass;
        try {
            launcherClass = gameContext.getClassLoader().loadClass(launcherClassName);
        } catch (ClassNotFoundException e) {
            failAndFinish("Failed to load launcher class!", e);
            return;
        }

        setPhaseStatus("Installing hooks...");
        try {
            ClassLoaderHooks.installHooks(gameContext.getClassLoader());
            PackageManagerHooks.installHooks(getPackageManager());
            InstrumentationHooks.install(getApplicationContext());
            Log.i(TAG, "[Bootstrap] Core hooks installed successfully");
        } catch (Exception e) {
            Log.e(TAG, "[Bootstrap] Hook installation error", e);
        }

        try {
            setPhaseStatus("Launching game...");
            initializeFusion(launcherClassName, targetPackage);
            runOnMainThread(() -> {
                try {
                    var intent = new Intent(this, launcherClass);
                    intent.putExtra(InstrumentationHooks.EXTRA_TARGET_ORIENTATION, targetOrientation);
                    var wrapped = new Intent(this, StubActivity.class);
                    wrapped.putExtra(InstrumentationHooks.EXTRA_IS_DYNAMIC_ACTIVITY, true);
                    wrapped.putExtra(InstrumentationHooks.EXTRA_ORIGINAL_INTENT, intent);
                    wrapped.putExtra(InstrumentationHooks.EXTRA_TARGET_ORIENTATION, targetOrientation);
                    startActivity(wrapped);
                    finish();
                } catch (Throwable t) {
                    failAndFinish("Launch failed: " + launcherClassName, t);
                }
            });
        } catch (Exception e) {
            failAndFinish("Launch exception: " + launcherClassName, e);
        }
    }

    private void setPhaseStatus(String status) {
        runOnMainThread(() -> {
            if (statusView != null) statusView.setText(status);
            if (spinnerProgress != null) spinnerProgress.setVisibility(View.VISIBLE);
            Log.i(TAG, "[Bootstrap] Phase: " + status);
        });
    }

    private void failAndFinish(String message, Throwable error) {
        runOnMainThread(() -> {
            if (error != null) Log.e(TAG, "[Bootstrap] ERROR: " + message, error);
            else Log.e(TAG, "[Bootstrap] ERROR: " + message);
            if (statusView != null) statusView.setText("Error");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void runOnMainThread(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else runOnUiThread(r);
    }

    private void initializeFusion(String launcherName, String targetPackage) {
        if (!fusionInitialized.compareAndSet(false, true)) return;
        PreparedFusionState prepared = preparedState;
        if (prepared == null || !targetPackage.equals(prepared.targetPackage)) {
            Log.e(TAG, "[Bootstrap] Config mismatch for: " + targetPackage);
            return;
        }
        Log.i(TAG, "[Bootstrap] Initializing Fusion for " + targetPackage);
        try {
            FusionConfig config = prepared.config;
            NativeLibraryManager.addFusionLibrary("main");
            NativeLibraryManager.addFusionLibrary("fusion");
            NativeLibraryManager.addDataLibrary("il2cpp");
            NativeLibraryManager.addDataLibrary("unity");
            NativeLibraryManager.setupLibraryHooks(config);

            File stagedConfig = FusionConfigStore.write(this, config);
            Log.i(TAG, "[Bootstrap] Config staged: " + stagedConfig.getAbsolutePath());
        } catch (Throwable t) {
            Log.e(TAG, "[Bootstrap] Init error", t);
        }
    }

    private PreparedFusionState prepareFusionState(Context appContext, Context gameContext, String targetPackage) {
        String gameLibDir = gameContext.getApplicationInfo().nativeLibraryDir;
        String appLibDir = appContext.getApplicationInfo().nativeLibraryDir;
        File appDataDir = new File(appContext.getFilesDir(), targetPackage);
        File dataOnSdCard = new File(new File(Environment.getExternalStorageDirectory(), "FusionCore"), targetPackage);

        setPhaseStatus("Copying assets...");
        File copiedData = new File(appDataDir, "Data_copy");
        boolean copied = Utilities.copyAssets(gameContext.getAssets(), "bin/Data", copiedData);
        if (!copied) Log.e(TAG, "[Bootstrap] Asset copy failed!");
        else applyGlobalMetadataOverride(dataOnSdCard, copiedData);

        setPhaseStatus("Extracting runtime...");
        File dotnetDir = new File(appDataDir, "dotnet");
        File bepInExDir = new File(dataOnSdCard, "BepInEx");
        Utilities.extractZipFromAssets(appContext, "BepInEx-arm64.zip", bepInExDir);
        Utilities.extractZipFromAssets(appContext, "dotnet-arm64.zip", dotnetDir);

        setPhaseStatus("Registering libraries...");
        File[] nativeLibs = new File(gameLibDir).listFiles();
        if (nativeLibs != null) {
            for (File f : nativeLibs) {
                String name = f.getName();
                if (name.startsWith("lib") && name.endsWith(".so")) {
                    NativeLibraryManager.addGameLibrary(name.substring(3, name.length() - 3));
                }
            }
        } else {
            Log.e(TAG, "[Bootstrap] No native libs found!");
        }

        FusionConfig config = new FusionConfig(
                gameLibDir, appLibDir, appDataDir.getAbsolutePath(),
                bepInExDir.getAbsolutePath(), dotnetDir.getAbsolutePath(),
                copiedData.getAbsolutePath(), "2017.0.0", true
        );
        return new PreparedFusionState(targetPackage, config);
    }

    private void applyGlobalMetadataOverride(File dataOnSdCard, File copiedData) {
        File override = new File(dataOnSdCard, GLOBAL_METADATA_FILE);
        if (!override.isFile()) return;
        File target = new File(new File(copiedData, "Managed/Metadata"), GLOBAL_METADATA_FILE);
        try {
            copyFile(override, target);
            Log.i(TAG, "[Bootstrap] global-metadata override applied");
        } catch (IOException e) {
            throw new IllegalStateException("Metadata override failed", e);
        }
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

    private int resolveTargetOrientation(ComponentName launcher) {
        try {
            ActivityInfo info = getPackageManager().getActivityInfo(launcher, 0);
            if (info.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            return info.screenOrientation;
        } catch (PackageManager.NameNotFoundException e) { return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; }
    }
            }
