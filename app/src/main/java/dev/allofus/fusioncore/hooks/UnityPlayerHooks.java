package dev.allofus.fusioncore.hooks;

import android.app.Activity;
import android.util.Log;
import java.lang.reflect.Field;

public class UnityPlayerHooks {
    private static final String TAG = "FusionCore_UnityHooks";

    // Анализ: Идеальная архитектура подразумевает, что UnityPlayer должен получать ОРИГИНАЛЬНЫЙ Activity.
    // Причина SIGSEGV (0x1) была в том, что BepInEx / Unity API обращались к невалидному ContextWrapper, 
    // и из-за getFields() native слой получал null для приватных полей.
    public static void processUnityPlayer(Object unityPlayerInstance, Activity activity) {
        if (unityPlayerInstance == null || activity == null) return;
        Log.i(TAG, "Hooking UnityPlayer with original Activity: " + activity.getClass().getName());
        try {
            Class<?> clazz = unityPlayerInstance.getClass();
            // Возвращаем getDeclaredFields вместо проблемного getFields(), чтобы libil2cpp нашел нужные поля
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                // Если поле ожидает Context/Activity, инжектим правильный инстанс
                if (field.getType().isAssignableFrom(Activity.class)) {
                    Object val = field.get(unityPlayerInstance);
                    if (val == null || !val.equals(activity)) {
                        field.set(unityPlayerInstance, activity);
                        Log.d(TAG, "Injected Activity into UnityPlayer field: " + field.getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in UnityPlayerHooks", e);
        }
    }
}
