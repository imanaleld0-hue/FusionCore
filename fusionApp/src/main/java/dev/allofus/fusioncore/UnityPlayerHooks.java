package dev.allofus.fusioncore;

import android.content.Context;
import android.util.Log;

public class UnityPlayerHooks {

    public static String TAG = "UnityPlayerHooks";

    /**
     * SAFETY FIX: Skip UnityPlayer constructor hooks to avoid Pine trampoline
     * crashes on native constructor backup. The essential functionality is provided by:
     * - ClassLoaderHooks (redirects class loading)
     * - NativeLibraryManager (redirects native library resolution via findLibrary)
     * - InstrumentationHooks (handles activity lifecycle)
     * - FusionConfigStore (stages config for native code)
     */
    public static void installHooks(Context gameContext) {
        Log.i(TAG, "UnityPlayerHooks: SKIPPING constructor hooks (Pine stability)");
        Log.i(TAG, "Library redirection and config injection handled by other hooks");
    }
}