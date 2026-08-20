package dev.allofus.fusioncore;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.regex.Pattern;

// https://github.com/BepInEx/BepInEx/blob/3fab71a1914132a1ce3a545caf3192da603f2258/Runtimes/Unity/BepInEx.Unity.Common/UnityInfo.cs#L61
public class VersionLookup {

    private static final int MAX_VERSION_LENGTH = 32;

    private static final Pattern UNITY_VERSION_PATTERN =
            Pattern.compile("^\\d+\\.\\d+\\.\\d+(?:[abcfp]\\d+|rc\\d+)?$");

    private static final HashMap<String, int[]> lookupMap = new HashMap<>() {{
        put("globalgamemanagers", new int[]{0x14, 0x30});
        put("data.unity3d", new int[]{0x12});
        put("mainData", new int[]{0x14});
    }};

    /**
     * Finds the Unity version inside a Unity data directory.
     *
     * @param dataFolder Unity data directory
     * @return full Unity version, for example 2022.3.62f3, or null if not found
     */
    public static String TryLookup(File dataFolder) {
        if (dataFolder == null || !dataFolder.isDirectory()) {
            return null;
        }

        for (String fileName : lookupMap.keySet()) {
            File file = new File(dataFolder, fileName);

            if (!file.exists() || !file.isFile()) {
                continue;
            }

            int[] offsets = lookupMap.get(fileName);

            if (offsets == null) {
                continue;
            }

            try (RandomAccessFile reader = new RandomAccessFile(file, "r")) {
                long length = reader.length();

                for (int offset : offsets) {
                    if (offset < 0 || offset >= length) {
                        continue;
                    }

                    reader.seek(offset);

                    String candidate =
                            readAsciiString(reader, MAX_VERSION_LENGTH);

                    if (isValidUnityVersion(candidate)) {
                        return candidate;
                    }
                }
            } catch (IOException e) {
                // Try the next Unity data file instead of aborting detection.
            }
        }

        return null;
    }

    /**
     * Compatibility method used by VersionResolver.
     *
     * IMPORTANT:
     * This method expects a Unity data directory, not an APK file.
     *
     * If VersionResolver passes an APK itself, the method attempts to
     * locate the corresponding Unity data directory beside it.
     */
    public static String fromApk(File apkFile) {
        if (apkFile == null) {
            return null;
        }

        File parent = apkFile.isDirectory()
                ? apkFile
                : apkFile.getParentFile();

        if (parent == null) {
            return null;
        }

        // Common layout:
        //
        // /game/
        //     game.apk
        //     game/
        //         globalgamemanagers
        //
        // Try the APK's parent first.
        String version = TryLookup(parent);

        if (version != null) {
            return version;
        }

        // Then try directories next to the APK.
        File[] children = parent.listFiles();

        if (children == null) {
            return null;
        }

        for (File child : children) {
            if (!child.isDirectory()) {
                continue;
            }

            version = TryLookup(child);

            if (version != null) {
                return version;
            }
        }

        return null;
    }

    private static String readAsciiString(
            RandomAccessFile reader,
            int maxLength
    ) throws IOException {

        StringBuilder builder = new StringBuilder(maxLength);

        for (int i = 0; i < maxLength; i++) {
            int b = reader.read();

            if (b == -1 || b == 0) {
                break;
            }

            // Unity version tokens are plain ASCII.
            if (b < 0x20 || b > 0x7E) {
                break;
            }

            builder.append((char) b);
        }

        if (builder.length() == 0) {
            return null;
        }

        return builder.toString().trim();
    }

    private static boolean isValidUnityVersion(String value) {
        return value != null
                && UNITY_VERSION_PATTERN.matcher(value).matches();
    }
}
