package dev.allofus.fusioncore.auth;

import android.util.Log;


public class NativeAuthBridge {
    private static final String TAG = "NativeAuthBridge";

    static {
        try {
            System.loadLibrary("fusion");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "libfusion.so not yet loaded (expected before game launch)");
        }
    }

    public static native void notifyAuthSuccess(String userId, String displayName,
                                                 String email, String idToken,
                                                 String mergeId, String store);
    public static native void notifyAuthLogout();

    public static native String getNativeToken();

    public static native String getNativeUserId();
       
    public static native boolean isNativeAuthActive();
}
