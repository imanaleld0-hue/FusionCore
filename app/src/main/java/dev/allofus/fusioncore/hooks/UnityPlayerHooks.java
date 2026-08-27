package dev.allofus.fusioncore.hooks;

import android.app.Activity;
import android.util.Log;
import java.lang.reflect.Field;

public class UnityPlayerHooks {
    // Отменяем подмену ContextWrapper. UnityPlayer должен получать только оригинальный Activity.
    public static void fixUnityPlayerFields(Object unityPlayerInstance) {
        if (unityPlayerInstance == null) return;
        Class<?> clazz = unityPlayerInstance.getClass();
        // Используем getDeclaredFields() вместо getFields(), чтобы извлекать private поля
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(unityPlayerInstance);
                Log.d("FusionCore", "UnityPlayer Field: " + field.getName() + " = " + value);
            } catch (Exception e) {
                Log.e("FusionCore", "Error inspecting field: " + field.getName(), e);
            }
        }
    }
}
