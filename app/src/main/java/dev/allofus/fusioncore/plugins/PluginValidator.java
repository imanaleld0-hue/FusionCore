package dev.allofus.fusioncore.plugins;

import java.io.File;
import java.io.RandomAccessFile;

public class PluginValidator {
    public static class Result {
        public boolean valid;
        public String arch;
        public String error;
    }

    public static Result checkDll(File file) {
        Result res = new Result();
        if (!file.getName().toLowerCase().endsWith(".dll")) {
            res.valid = false; res.error = "Not a .dll file"; return res;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.readShort() != 0x4D5A) { // MZ
                res.valid = false; res.error = "Invalid DOS header"; return res;
            }
            raf.seek(0x3C);
            int peOffset = Integer.reverseBytes(raf.readInt());
            raf.seek(peOffset);
            if (raf.readInt() != 0x00004550) { // PE\0\0
                res.valid = false; res.error = "Invalid PE header"; return res;
            }
            int machineType = Short.reverseBytes(raf.readShort()) & 0xFFFF;
            res.valid = true;
            switch (machineType) {
                case 0x8664: res.arch = "x64"; break;
                case 0x014c: res.arch = "x86"; break;
                case 0xAA64: res.arch = "ARM64"; break;
                case 0x01c0: res.arch = "ARM"; break;
                default: res.arch = "Any CPU"; break;
            }
        } catch (Exception e) {
            res.valid = false; res.error = e.getMessage();
        }
        return res;
    }
}
