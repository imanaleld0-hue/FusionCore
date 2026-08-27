package dev.allofus.fusioncore.hooks;

import android.util.Log;

public class ChatUnlocker {

    private static final String TAG =
            "FusionCore_ChatMod";

    public static void initHooks() {

        Log.i(
                TAG,
                "Initializing ChatUnlocker..."
        );

        try {

            long libBase =
                    0L;

            long saveManagerGetChatOffset =
                    0xABC123L;

            Log.w(
                    TAG,
                    "Chat hook is not installed because the target offset is a placeholder: 0xABC123"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to initialize chat hooks.",
                    e
            );
        }
    }
}
