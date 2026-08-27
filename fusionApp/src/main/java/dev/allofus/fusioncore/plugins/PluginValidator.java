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
            res.valid = false;
            res.error = "File extension must be .dll";
            return res;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

            if (raf.length() < 64) {
                res.valid = false;
                res.error = "File is too small to be a valid PE file.";
                return res;
            }

            int mz = raf.readUnsignedShort();

            if (mz != 0x4D5A) {
                res.valid = false;
                res.error = "Invalid MZ header.";
                return res;
            }

            raf.seek(0x3C);

            long peOffset = Integer.toUnsignedLong(
                    Integer.reverseBytes(raf.readInt())
            );

            if (peOffset < 64 || peOffset + 6 > raf.length()) {
                res.valid = false;
                res.error = "Invalid PE header offset.";
                return res;
            }

            raf.seek(peOffset);

            int peSignature = raf.readInt();

            if (peSignature != 0x00004550) {
                res.valid = false;
                res.error = "Invalid PE signature.";
                return res;
            }

            int machineType =
                    Short.reverseBytes(raf.readShort()) & 0xFFFF;

            res.valid = true;

            switch (machineType) {
                case 0x8664:
                    res.arch = "x64";
                    break;

                case 0x014C:
                    res.arch = "x86";
                    break;

                case 0xAA64:
                    res.arch = "ARM64";
                    break;

                case 0x01C0:
                    res.arch = "ARM";
                    break;

                default:
                    res.arch = "Unknown";
                    break;
            }

            if (!res.arch.equals("ARM64") &&
                    !res.arch.equals("Unknown")) {

                res.error =
                        "Architecture may be incompatible with Android ARM64: "
                                + res.arch;
            }

        } catch (Exception e) {
            res.valid = false;
            res.error = e.getMessage();
        }

        return res;
    }
}
