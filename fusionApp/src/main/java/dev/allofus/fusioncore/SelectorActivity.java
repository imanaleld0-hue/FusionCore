package dev.allofus.fusioncore;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import dev.allofus.fusioncore.ui.BaseFullscreenActivity;
import dev.allofus.fusioncore.ui.LogsViewerActivity;
import dev.allofus.fusioncore.ui.ModProjectsActivity;
import dev.allofus.fusioncore.ui.SettingsActivity;

public class SelectorActivity extends BaseFullscreenActivity {
    private static final String TAG = "FusionCore";
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 1001;
    private static final String[] UNITY_ABIS = {"arm64-v8a", "armeabi-v7a", "x86_64", "x86"};

    private String pendingLaunchPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);

        Button btnLaunch = findViewById(R.id.btn_launch);
        Button btnMods = findViewById(R.id.btn_mods);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnLogs = findViewById(R.id.btn_logs);

        btnLaunch.setOnClickListener(v -> showGameSelector());
        btnMods.setOnClickListener(v -> startActivity(new Intent(this, ModProjectsActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnLogs.setOnClickListener(v -> startActivity(new Intent(this, LogsViewerActivity.class)));

        new Handler(getMainLooper()).postDelayed(() -> {
            if (!hasExternalStorageManagerAccess()) requestExternalStorageManagerAccess();
        }, 100);
    }

    private void showGameSelector() {
        List<AppEntry> targets = resolveInstalledTargets();
        if (targets.isEmpty()) {
            Toast.makeText(this, "No IL2CPP games found", Toast.LENGTH_LONG).show();
            return;
        }

        ListView listView = new ListView(this);
        Drawable defaultIcon = getPackageManager().getDefaultActivityIcon();
        ArrayAdapter<AppEntry> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1, targets) {
            @NonNull @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                AppEntry e = getItem(position);
                ((TextView)v.findViewById(android.R.id.text1)).setText(e.label);
                ((TextView)v.findViewById(android.R.id.text2)).setText(e.packageName);
                return v;
            }
        };
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Game")
                .setView(listView)
                .setNegativeButton("Cancel", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            maybeLaunchBootstrap(targets.get(position).packageName);
        });

        dialog.show();
    }

    private List<AppEntry> resolveInstalledTargets() {
        PackageManager pm = getPackageManager();
        List<AppEntry> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        Drawable icon = getPackageManager().getDefaultActivityIcon();
        
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL);

        for (ResolveInfo ri : activities) {
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || !seen.add(pkg) || pkg.equals(getPackageName())) continue;

            ApplicationInfo info;
            try { info = pm.getApplicationInfo(pkg, 0); }
            catch (PackageManager.NameNotFoundException e) { continue; }

            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (!hasIl2Cpp(info)) continue;

            String label = pkg;
            Drawable icon = defaultIcon;
            String versionName = "Unknown";
            long versionCode = 0L;
            try {
                label = pm.getApplicationLabel(info).toString();
                icon = pm.getApplicationIcon(info);
                PackageInfo pi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                        : pm.getPackageInfo(pkg, 0);
                versionName = pi.versionName != null ? pi.versionName : "Unknown";
                versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? pi.getLongVersionCode() : pi.versionCode;
            } catch (Exception e) { Log.w(TAG, "Metadata fail: " + pkg, e); }

            result.add(new AppEntry(pkg, label, icon, versionName, versionCode));
        }
        return result;
    }

    private static boolean hasIl2Cpp(ApplicationInfo info) {
        List<String> apkPaths = new ArrayList<>();
        if (info.sourceDir != null) apkPaths.add(info.sourceDir);
        if (info.splitSourceDirs != null) Collections.addAll(apkPaths, info.splitSourceDirs);
        for (String apk : apkPaths) if (apkContainsIl2Cpp(apk)) return true;

        String nativeDir = info.nativeLibraryDir;
        if (nativeDir != null) {
            File dir = new File(nativeDir);
            if (new File(dir, "libil2cpp.so").exists()) return true;
            File[] abiDirs = dir.listFiles();
            if (abiDirs != null) {
                for (File abiDir : abiDirs) {
                    if (abiDir.isDirectory() && new File(abiDir, "libil2cpp.so").exists()) return true;
                }
            }
        }
        return false;
    }

    private static boolean apkContainsIl2Cpp(String apkPath) {
        try (ZipFile zip = new ZipFile(apkPath)) {
            for (String abi : UNITY_ABIS) {
                if (zip.getEntry("lib/" + abi + "/libil2cpp.so") != null) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void launchBootstrap(String packageName) {
        Intent intent = new Intent(this, BootstrapActivity.class);
        intent.putExtra(BootstrapActivity.EXTRA_TARGET_PACKAGE, packageName);
        intent.putExtra(BootstrapActivity.EXTRA_USE_ORIGINAL_LIBUNITY, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), map -> {
                String pkg = pendingLaunchPackage;
                pendingLaunchPackage = null;
                if (pkg != null) launchBootstrap(pkg);
            });

    private void maybeLaunchBootstrap(String packageName) {
        if (!hasExternalStorageManagerAccess()) {
            pendingLaunchPackage = packageName;
            requestExternalStorageManagerAccess();
            return;
        }
        try {
            var pi = getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (pi.requestedPermissions != null) {
                ArrayList<String> missing = new ArrayList<>();
                for (String p : pi.requestedPermissions) {
                    if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) missing.add(p);
                }
                if (!missing.isEmpty()) {
                    pendingLaunchPackage = packageName;
                    requestPermissionsLauncher.launch(missing.toArray(new String[0]));
                    return;
                }
            }
        } catch (Exception e) { Log.e(TAG, "Perm check failed", e); }
        launchBootstrap(packageName);
    }

    private boolean hasExternalStorageManagerAccess() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    private void requestExternalStorageManagerAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
        Toast.makeText(this, "Storage permission required", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        try {
            startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
        } catch (Exception e) {
            try {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), REQUEST_MANAGE_EXTERNAL_STORAGE);
            } catch (Exception inner) {
                Toast.makeText(this, "Cannot open permission settings", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE && pendingLaunchPackage != null && hasExternalStorageManagerAccess()) {
            String pkg = pendingLaunchPackage; pendingLaunchPackage = null;
            launchBootstrap(pkg);
        }
    }

    private record AppEntry(String packageName, String label, Drawable icon, String versionName, long versionCode) {
        @NonNull @Override public String toString() { return label + " (" + packageName + ")"; }
    }
                }
                            
