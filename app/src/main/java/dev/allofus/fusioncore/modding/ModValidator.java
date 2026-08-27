package dev.allofus.fusioncore.modding;

import java.io.File;
import java.io.RandomAccessFile;

public class ModValidator {
    public static boolean isValidDll(File file) {
        if (!file.getName().toLowerCase().endsWith(".dll")) return false;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.readByte() != 0x4D || raf.readByte() != 0x5A) return false;
            raf.seek(0x3C);
            int peOffset = Integer.reverseBytes(raf.readInt());
            raf.seek(peOffset + 4);
            short machineType = Short.reverseBytes(raf.readShort());
            return machineType == (short)0xAA64 || machineType == (short)0x014C; 
        } catch (Exception e) {
            return false;
        }
    }
}
