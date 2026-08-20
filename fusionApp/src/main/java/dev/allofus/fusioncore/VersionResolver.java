package dev.allofus.fusioncore;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.ByteArrayOutputStream;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized Unity version detection. Single source of truth for resolved versions.
 */
public final class VersionResolver {
    private static final String TAG = "FusionCore.VersionResolver";
    
    // Matches full versions like 2022.3.62f3, 2022.3.62f1, 2022.3.62rc1, 2022.3.62a1, 2022.3.62b1, 2022.3.62p1
    public static final Pattern UNITY_FULL_VERSION_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)\\.(\\d+)([abcfp]\\d+|rc\\d+)?$"
    );
    
    // Matches base version for download URLs: 2022.3.62 from 2022.3.62f3
    public static final Pattern UNITY_BASE_VERSION_PATTERN = Pattern.compile(
            "^(\\d+\\.\\d+\\.\\d+)"
    );

    private VersionResolver() {}

    @Nullable
    public static String determineUnityVersion(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            String apkPath = appInfo.sourceDir;
            if (apkPath == null) {
                Log.w(TAG, "APK path is null for " + packageName);
                return null;
            }
            File apkFile = new File(apkPath);
            if (!apkFile.exists()) {
                Log.w(TAG, "APK does not exist: " + apkPath);
                return null;
            }
            // Delegate to existing VersionLookup if available, otherwise use our own
            String version = VersionLookup.fromApk(apkFile);
            if (version != null) {
                version = version.trim();
                if (isValidFullVersion(version)) {
                    Log.i(TAG, "Resolved Unity version for " + packageName + ": " + version);
                    return version;
                } else {
                    Log.w(TAG, "VersionLookup returned invalid version: " + version);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package not found: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to determine Unity version for " + packageName, e);
        }
        return null;
    }

    public static boolean isValidFullVersion(@Nullable String version) {
        if (version == null || version.isEmpty()) return false;
        return UNITY_FULL_VERSION_PATTERN.matcher(version).matches();
    }

    /**
     * Returns the base version suitable for download URLs.
     * e.g. "2022.3.62f3" -> "2022.3.62"
     */
    @Nullable
    public static String normalizeForDownload(@Nullable String fullVersion) {
        if (fullVersion == null || fullVersion.isEmpty()) return null;
        Matcher m = UNITY_BASE_VERSION_PATTERN.matcher(fullVersion.trim());
        return m.find() ? m.group(1) : null;
    }

    /**
     * Preserves the full version. Returns null if invalid.
     */
    @Nullable
    public static String validateAndNormalize(@Nullable String version) {
        if (version == null) return null;
        String trimmed = version.trim();
        return isValidFullVersion(trimmed) ? trimmed : null;
    }
}
