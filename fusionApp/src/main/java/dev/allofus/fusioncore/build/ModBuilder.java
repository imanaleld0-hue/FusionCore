package dev.allofus.fusioncore.build;

import android.content.Context;
import dev.allofus.fusioncore.mod.ModProjectManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ModBuilder {
    private final Context context;

    public ModBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    public File build(ModProjectManager.ModProject project) throws Exception {
        File tempDir = new File(context.getCacheDir(), "build_" + project.id);
        if (tempDir.exists()) deleteDir(tempDir);
        tempDir.mkdirs();

        File libsDir = new File(tempDir, "libs");
        libsDir.mkdirs();
        copyAssetsLibs(libsDir);

        File srcDir = new File(project.path);
        copyDir(srcDir, new File(tempDir, "src"));

        // Compilation stub: in a real environment you would invoke an embedded compiler
        // (e.g., via Termux or bundled dotnet runtime) here.
        File output = new File(tempDir, project.plugin + ".dll");
        output.createNewFile(); // placeholder artifact

        return tempDir;
    }

    public File exportToZip(ModProjectManager.ModProject project) throws Exception {
        File buildDir = build(project);
        File zipFile = new File(context.getFilesDir(), "exports/" + project.name + ".zip");
        zipFile.getParentFile().mkdirs();
        zipDirectory(buildDir, zipFile);
        return zipFile;
    }

    private void copyAssetsLibs(File dest) throws Exception {
        File bepinex = new File(context.getFilesDir(), "libs/BepInEx.dll");
        if (bepinex.exists()) {
            copyFile(bepinex, new File(dest, "BepInEx.dll"));
        }
        // Copy additional references as needed
    }

    private void copyFile(File src, File dst) throws Exception {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.close();
    }

    private void copyDir(File src, File dst) throws Exception {
        if (!dst.exists()) dst.mkdirs();
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            File dest = new File(dst, f.getName());
            if (f.isDirectory()) {
                copyDir(f, dest);
            } else {
                copyFile(f, dest);
            }
        }
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private void zipDirectory(File srcDir, File zipFile) throws Exception {
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        zipRecursive(srcDir, "", zos);
        zos.close();
        fos.close();
    }

    private void zipRecursive(File dir, String prefix, ZipOutputStream zos) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                zipRecursive(f, prefix + f.getName() + "/", zos);
            } else {
                ZipEntry entry = new ZipEntry(prefix + f.getName());
                zos.putNextEntry(entry);
                FileInputStream fis = new FileInputStream(f);
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) > 0) zos.write(buf, 0, n);
                fis.close();
                zos.closeEntry();
            }
        }
    }
}
