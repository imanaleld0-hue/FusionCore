package dev.allofus.fusioncore.hooks;

import android.app.Activity;
import android.content.Context;
import java.lang.reflect.Field;

public class UnityPlayerHooks {
    public static void hookUnityPlayerInit(Activity originalActivity) {
        try {
            // FIX: Using getDeclaredFields instead of getFields to avoid crashes
            // FIX: Passing the original Activity, NEVER a ContextWrapper
            Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            Field currentActivityField = unityPlayerClass.getDeclaredField("currentActivity");
            currentActivityField.setAccessible(true);
            currentActivityField.set(null, originalActivity);
            
            System.out.println("[FusionCore] UnityPlayer initialized securely with original context.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
