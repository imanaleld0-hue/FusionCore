package dev.allofus.fusioncore;

import android.content.Context;
import android.util.Log;

public class UnityPlayerHooks {

    public static String TAG = "UnityPlayerHooks";

    public static void installHooks(Context gameContext) {
        Log.i(TAG, "UnityPlayerHooks: SKIPPING constructor hooks (Pine stability)");
        Log.i(TAG, "Library redirection and config injection handled by other hooks");
    }
}