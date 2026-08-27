package dev.allofus.fusioncore.ide;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ModBuilder {
    public static boolean buildModZip(File sourceDir, File outputZip) {
        try (FileOutputStream fos = new FileOutputStream(outputZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            File[] files = sourceDir.listFiles();
            if (files == null) return false;
            
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".dll") || file.getName().endsWith(".cs") || file.getName().endsWith(".json"))) {
                    addToZip(file, file.getName(), zos);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void addToZip(File file, String fileName, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }
}
