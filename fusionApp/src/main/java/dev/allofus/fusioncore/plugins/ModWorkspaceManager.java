package dev.allofus.fusioncore.plugins;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ModWorkspaceManager {

    private final File workspaceDir;

    public ModWorkspaceManager(Context ctx) {
        workspaceDir = new File(
                ctx.getFilesDir(),
                "ModWorkspace"
        );

        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs();
        }
    }

    public File getWorkspace() {
        return workspaceDir;
    }

    public void importModZip(InputStream zipStream) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                File target = new File(
                        workspaceDir,
                        entry.getName()
                );

                String workspacePath =
                        workspaceDir.getCanonicalPath();

                String targetPath =
                        target.getCanonicalPath();

                if (!targetPath.equals(workspacePath) &&
                        !targetPath.startsWith(
                                workspacePath + File.separator
                        )) {

                    throw new IOException(
                            "Invalid ZIP entry path: "
                                    + entry.getName()
                    );
                }

                if (entry.isDirectory()) {

                    if (!target.exists() && !target.mkdirs()) {
                        throw new IOException(
                                "Failed to create directory: "
                                        + target
                        );
                    }

                } else {

                    File parent = target.getParentFile();

                    if (parent != null &&
                            !parent.exists() &&
                            !parent.mkdirs()) {

                        throw new IOException(
                                "Failed to create directory: "
                                        + parent
                        );
                    }

                    try (FileOutputStream fos =
                                 new FileOutputStream(target)) {

                        byte[] buffer = new byte[8192];
                        int len;

                        while ((len = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }

    public void exportModZip(File outZip) throws IOException {
        try (ZipOutputStream zos =
                     new ZipOutputStream(
                             new FileOutputStream(outZip)
                     )) {

            compressDir(
                    workspaceDir,
                    workspaceDir,
                    zos
            );
        }
    }

    private void compressDir(
            File root,
            File current,
            ZipOutputStream zos
    ) throws IOException {

        File[] files = current.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file.isDirectory()) {

                compressDir(
                        root,
                        file,
                        zos
                );

            } else {

                String name =
                        root.toURI()
                                .relativize(file.toURI())
                                .getPath();

                zos.putNextEntry(
                        new ZipEntry(name)
                );

                try (FileInputStream fis =
                             new FileInputStream(file)) {

                    byte[] buffer = new byte[8192];
                    int len;

                    while ((len = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }

                zos.closeEntry();
            }
        }
    }
}
