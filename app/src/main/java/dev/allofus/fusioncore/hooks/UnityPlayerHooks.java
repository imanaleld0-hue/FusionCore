package dev.allofus.fusioncore.hooks;

import android.app.Activity;
import java.lang.reflect.Field;

public class UnityPlayerHooks {
    public static void hookUnityPlayerInit(Activity originalActivity) {
        try {
            Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            Field currentActivityField = unityPlayerClass.getDeclaredField("currentActivity");
            currentActivityField.setAccessible(true);
            currentActivityField.set(null, originalActivity);
            System.out.println("[FusionCore] UnityPlayer initialized securely.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
