package dev.allofus.fusioncore.modding;

import java.io.File;
import java.io.RandomAccessFile;

public class ModValidator {
    public static boolean isValidArm64Dll(File file) {
        if (!file.getName().toLowerCase().endsWith(".dll")) return false;
        
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Check MZ header
            if (raf.readByte() != 0x4D || raf.readByte() != 0x5A) {
                return false; 
            }
            // Move to PE header offset
            raf.seek(0x3C);
            int peOffset = Integer.reverseBytes(raf.readInt());
            
            // Move to Machine Type
            raf.seek(peOffset + 4);
            short machineType = Short.reverseBytes(raf.readShort());
            
            // 0xAA64 is ARM64
            return machineType == (short)0xAA64 || machineType == (short)0x014C; // allow i386 for pure IL code
        } catch (Exception e) {
            return false;
        }
    }
}
