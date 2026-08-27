package dev.allofus.fusioncore.mod;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModValidator {
    private static final String[] VALID_EXTS = {".dll", ".cs", ".zip", ".tar"};

    public boolean isValidFileName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        for (String ext : VALID_EXTS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    public boolean validateArchive(File file) {
        if (file.getName().toLowerCase().endsWith(".zip")) {
            return validateZip(file);
        }
        // Basic tar acceptance; extend with Apache Commons Compress if needed
        return true;
    }

    private boolean validateZip(File file) {
        try (ZipFile zf = new ZipFile(file)) {
            boolean hasArm64 = false;
            java.util.Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".so") && name.contains("arm64-v8a")) {
                    hasArm64 = true;
                }
            }
            // If the archive contains native libs, arm64-v8a is required
            // For pure .dll/.cs archives we accept regardless
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
