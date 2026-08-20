package dev.allofus.fusioncore;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class LibUnityDownloader {
    private static final String TAG = "FusionCore";
    private static final String LIBUNITY_DOWNLOAD_URL = "https://unity.bepinex.dev/android/";
    private static final String LIBUNITY_CACHE_META_FILE = "libunity.cache.properties";
    private static final Pattern UNITY_BASE_VERSION_PATTERN = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern UNITY_FULL_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+(?:[abcfp]\\d+|rc\\d+)?");

    public interface DownloadProgressListener {
        void onDownloadStarted(String url, long totalBytes);
        void onDownloadProgress(long downloadedBytes, long totalBytes);
        void onDownloadFinished(boolean success, boolean usedCache);
    }

    public static boolean downloadAndCacheSafely(
            File outputDir,
            String version,
            String targetGameAbi,
            DownloadProgressListener progressListener) {

        synchronized (LibUnityDownloader.class) {
            FutureTask<Boolean> task = new FutureTask<>(
                    () -> downloadAndCache(
                            outputDir,
                            version,
                            targetGameAbi,
                            progressListener
                    )
            );

            Thread worker = new Thread(
                    task,
                    "FusionCore-LibUnityDownload"
            );

            worker.start();

            try {
                return task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Libunity download thread was interrupted", e);
                return false;
            } catch (ExecutionException e) {
                Log.e(
                        TAG,
                        "Libunity download failed",
                        e.getCause() != null ? e.getCause() : e
                );
                return false;
            }
        }
    }

    private static boolean downloadAndCache(
            File outputDir,
            String version,
            String targetGameAbi,
            DownloadProgressListener progressListener) {

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "Failed to create output directory");
            return false;
        }

        File outputLibUnity = new File(outputDir, "libunity.so");
        File tempOutputLibUnity = new File(outputDir, "libunity.so.download");
        File tempZipFile = new File(outputDir, "libunity.so.zip.download");
        File cacheMetaFile = new File(outputDir, LIBUNITY_CACHE_META_FILE);
        String trimmedVersion = version.trim();
        String downloadVersion = normalizeVersionForDownload(trimmedVersion);
        String currentAbi = normalizeAbiForDownload(targetGameAbi);

        if (currentAbi == null) {
            Log.e(TAG, "Unsupported target game ABI: " + targetGameAbi);
            notifyDownloadFinished(progressListener, false, false);
            return false;
        }

        if (!trimmedVersion.equals(downloadVersion)) {
            Log.i(TAG, "Normalized Unity version for download URL: " + trimmedVersion + " -> " + downloadVersion);
        }

        if (isCachedLibUnityValid(outputLibUnity, cacheMetaFile, trimmedVersion, currentAbi)) {
            Properties meta = readCacheMeta(cacheMetaFile);
            String actualKey = meta != null ? meta.getProperty("cacheKey", "") : "";
            Log.i(TAG, "Using cached libunity for " + actualKey + " at " + outputLibUnity.getAbsolutePath());
            notifyDownloadFinished(progressListener, true, true);
            return true;
        }

        String url = LIBUNITY_DOWNLOAD_URL + downloadVersion + "/" + currentAbi + ".zip";
        Log.i(TAG, "Downloading libunity from " + url);

        HttpURLConnection connection = null;
        boolean extracted = false;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);

            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                Log.e(TAG, "Failed to download libunity zip, HTTP " + statusCode);
                notifyDownloadFinished(progressListener, false, false);
                return false;
            }

            long totalBytes = connection.getContentLengthLong();
            notifyDownloadStarted(progressListener, url, totalBytes);

            byte[] buffer = new byte[8192];
            long downloadedBytes = 0L;
            long lastProgressDispatchMs = 0L;

            try (InputStream is = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream zipOut = new FileOutputStream(tempZipFile, false)) {
                int count;
                while ((count = is.read(buffer)) != -1) {
                    zipOut.write(buffer, 0, count);
                    downloadedBytes += count;

                    long now = System.currentTimeMillis();
                    if (now - lastProgressDispatchMs >= 120L) {
                        notifyDownloadProgress(progressListener, downloadedBytes, totalBytes);
                        lastProgressDispatchMs = now;
                    }
                }
            }

            notifyDownloadProgress(progressListener, downloadedBytes, totalBytes);

            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(tempZipFile)))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        zis.closeEntry();
                        continue;
                    }

                    String entryName = entry.getName();
                    String fileName = entryName == null ? "" : new File(entryName).getName();
                    if (!"libunity.so".equals(fileName)) {
                        zis.closeEntry();
                        continue;
                    }

                    try (FileOutputStream fos = new FileOutputStream(tempOutputLibUnity, false)) {
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                        }
                    }

                    extracted = true;
                    zis.closeEntry();
                    break;
                }
            }

            if (!extracted) {
                Log.e(TAG, "Downloaded zip did not contain libunity.so");
                notifyDownloadFinished(progressListener, false, false);
                return false;
            }

            // Inspect downloaded binary for embedded Unity version
            String binaryVersion = detectUnityVersion(tempOutputLibUnity);
            Log.i(TAG, "Target Unity version: " + trimmedVersion + ", downloaded libunity version: " + binaryVersion);

            if (binaryVersion == null) {
                Log.e(TAG, "Could not determine libunity.so Unity version");
                if (tempOutputLibUnity.exists() && !tempOutputLibUnity.delete()) {
                    Log.w(TAG, "Failed to delete temporary libunity after undetectable version: " + tempOutputLibUnity.getAbsolutePath());
                }
                notifyDownloadFinished(progressListener, false, false);
                return false;
            }

            if (!trimmedVersion.equals(binaryVersion)) {
                Log.e(
                        TAG,
                        "Unity version mismatch! Expected " +
                        trimmedVersion +
                        ", libunity is " +
                        binaryVersion + ". Keeping original libunity."
                );

                if (tempOutputLibUnity.exists() && !tempOutputLibUnity.delete()) {
                    Log.w(TAG, "Failed to delete mismatched temporary libunity: " + tempOutputLibUnity.getAbsolutePath());
                }

                // Invalidate any old cache metadata (remove file)
                if (cacheMetaFile.exists() && !cacheMetaFile.delete()) {
                    Log.w(TAG, "Failed to delete incompatible cache metadata: " + cacheMetaFile.getAbsolutePath());
                }

                notifyDownloadFinished(progressListener, false, false);
                return false;
            }

            // Replace existing libunity
            if (outputLibUnity.exists() && !outputLibUnity.delete()) {
                Log.e(TAG, "Failed to replace existing libunity: " + outputLibUnity.getAbsolutePath());
                notifyDownloadFinished(progressListener, false, false);
                return false;
            }

            if (!tempOutputLibUnity.renameTo(outputLibUnity)) {
                Log.e(TAG, "Failed to move downloaded libunity into place");
                notifyDownloadFinished(progressListener, false, false);
                return false;
            }

            // Write cache metadata including the binary version
            String finalCacheKey = trimmedVersion + "|" + currentAbi + "|" + binaryVersion;
            if (!writeLibUnityCacheMeta(cacheMetaFile, finalCacheKey, outputLibUnity.length(), binaryVersion)) {
                Log.w(TAG, "Downloaded libunity but failed to update cache metadata");
            }

            Log.i(TAG, "Successfully downloaded libunity to " + outputLibUnity.getAbsolutePath());
            notifyDownloadFinished(progressListener, true, false);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to download libunity", e);
            notifyDownloadFinished(progressListener, false, false);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (tempZipFile.exists() && !tempZipFile.delete()) {
                Log.w(TAG, "Failed to clean temporary zip file: " + tempZipFile.getAbsolutePath());
            }
            if (tempOutputLibUnity.exists() && !outputLibUnity.exists() && !tempOutputLibUnity.delete()) {
                Log.w(TAG, "Failed to clean temporary libunity file: " + tempOutputLibUnity.getAbsolutePath());
            }
        }
    }

    private static void notifyDownloadStarted(DownloadProgressListener listener, String url, long totalBytes) {
        if (listener != null) {
            listener.onDownloadStarted(url, totalBytes);
        }
    }

    private static void notifyDownloadProgress(DownloadProgressListener listener, long downloadedBytes, long totalBytes) {
        if (listener != null) {
            listener.onDownloadProgress(downloadedBytes, totalBytes);
        }
    }

    private static void notifyDownloadFinished(DownloadProgressListener listener, boolean success, boolean usedCache) {
        if (listener != null) {
            listener.onDownloadFinished(success, usedCache);
        }
    }

    private static boolean isCachedLibUnityValid(File outputLibUnity, File cacheMetaFile, String trimmedVersion, String currentAbi) {
        if (!outputLibUnity.exists() || !outputLibUnity.isFile() || outputLibUnity.length() <= 0) {
            return false;
        }
        if (!cacheMetaFile.exists() || !cacheMetaFile.isFile()) {
            return false;
        }

        Properties meta = readCacheMeta(cacheMetaFile);
        if (meta == null) {
            return false;
        }

        // Require binary version to be present in metadata; old cache entries are invalid
        String binaryVersion = meta.getProperty("libunityBinaryVersion", null);
        if (binaryVersion == null || binaryVersion.isEmpty()) {
            Log.i(TAG, "Libunity cache metadata missing binary version, treating as invalid");
            return false;
        }

        String actualKey = meta.getProperty("cacheKey", "");
        String expectedKey = trimmedVersion + "|" + currentAbi + "|" + binaryVersion;
        if (!expectedKey.equals(actualKey)) {
            Log.i(TAG, "Libunity cache key mismatch. expected=" + expectedKey + ", actual=" + actualKey);
            return false;
        }

        String sizeString = meta.getProperty("libunitySize", "0");
        try {
            long expectedSize = Long.parseLong(sizeString);
            return expectedSize > 0 && expectedSize == outputLibUnity.length();
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid libunity cache metadata size", e);
            return false;
        }
    }

    private static Properties readCacheMeta(File cacheMetaFile) {
        Properties meta = new Properties();
        try (FileInputStream fis = new FileInputStream(cacheMetaFile)) {
            meta.load(fis);
            return meta;
        } catch (IOException e) {
            Log.w(TAG, "Failed reading libunity cache metadata", e);
            return null;
        }
    }

    private static boolean writeLibUnityCacheMeta(File cacheMetaFile, String cacheKey, long libunitySize, String binaryVersion) {
        Properties meta = new Properties();
        meta.setProperty("cacheKey", cacheKey);
        meta.setProperty("libunitySize", Long.toString(libunitySize));
        meta.setProperty("libunityBinaryVersion", binaryVersion);

        try (FileOutputStream fos = new FileOutputStream(cacheMetaFile, false)) {
            meta.store(fos, "libunity cache metadata");
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Failed writing libunity cache metadata", e);
            return false;
        }
    }

    private static String normalizeVersionForDownload(String version) {
        Matcher matcher = UNITY_BASE_VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return version;
    }

    private static String normalizeAbiForDownload(String abiValue) {
        if (abiValue == null) {
            return null;
        }

        String normalized = abiValue.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }

        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            normalized = normalized.substring(slash + 1);
        }

        int backslash = normalized.lastIndexOf('\\');
        if (backslash >= 0 && backslash < normalized.length() - 1) {
            normalized = normalized.substring(backslash + 1);
        }

        switch (normalized) {
            case "arm64":
            case "aarch64":
            case "arm64-v8a":
                return "arm64-v8a";
            case "armeabi-v7a":
            case "armeabi":
            case "armv7":
                return "armeabi-v7a";
        }

        return null;
    }

    private static String detectUnityVersion(File libUnity) {
        byte[] buffer = new byte[1024 * 1024];
        try (FileInputStream in = new FileInputStream(libUnity)) {
            StringBuilder text = new StringBuilder();

            int read;

            while ((read = in.read(buffer)) != -1) {
                text.append(
                        new String(
                                buffer,
                                0,
                                read,
                                StandardCharsets.ISO_8859_1
                        )
                );

                Matcher matcher = UNITY_FULL_VERSION_PATTERN.matcher(text);

                if (matcher.find()) {
                    String version = matcher.group();

                    Log.i(TAG, "Detected Unity version from libunity.so: " + version);

                    return version;
                }

                if (text.length() > 4 * 1024 * 1024) {
                    text.delete(
                            0,
                            text.length() - 1024 * 1024
                    );
                }
            }

        } catch (IOException e) {
            Log.e(
                    TAG,
                    "Failed to detect Unity version from libunity.so",
                    e
            );
        }

        Log.e(TAG, "Unity version was not found in libunity.so");

        return null;
    }
}
