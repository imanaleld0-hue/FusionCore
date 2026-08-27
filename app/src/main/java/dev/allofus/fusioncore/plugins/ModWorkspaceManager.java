package dev.allofus.fusioncore.plugins;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import android.content.Context;
import android.util.Log;

// Сборщик модов: мини IDE & Zip Workspace exporter
public class ModWorkspaceManager {
    private static final String TAG = "ModWorkspace";
    private File workspaceDir;

    public ModWorkspaceManager(Context ctx) {
        workspaceDir = new File(ctx.getFilesDir(), "ModWorkspace");
        if (!workspaceDir.exists()) workspaceDir.mkdirs();
    }

    public void importModZip(InputStream zipStream) {
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File target = new File(workspaceDir, entry.getName());
                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    target.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(target)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Import error", e);
        }
    }

    public void exportModZip(File outZip) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZip))) {
            compressDir(workspaceDir, workspaceDir, zos);
        } catch (IOException e) {
            Log.e(TAG, "Export error", e);
        }
    }

    private void compressDir(File root, File current, ZipOutputStream zos) throws IOException {
        File[] files = current.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                compressDir(root, f, zos);
            } else {
                String name = root.toURI().relativize(f.toURI()).getPath();
                zos.putNextEntry(new ZipEntry(name));
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
                }
                zos.closeEntry();
            }
        }
    }
}
