package dev.allofus.fusioncore.bridge;

import android.util.Log;


public class NativeAuthBridge {
    private static final String TAG = "NativeAuthBridge";

    static {
        try {
            System.loadLibrary("fusion");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "libfusion.so не загружена (возможно, ещё не инициализирована)");
        }
    }

    
    public static native void notifyAuthSuccess(String userId, String displayName, String email, String idToken);

    
    public static native void notifyAuthLogout();

    
    public static native boolean isNativeAuthActive();
}
