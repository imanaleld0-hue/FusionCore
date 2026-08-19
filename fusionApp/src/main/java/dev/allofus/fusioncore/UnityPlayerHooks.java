package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

public class UnityPlayerHooks {

    public static String TAG = "UnityPlayerHooks";

    public static final String[] UnityPlayerClassNames = new String[] {
            "com.unity3d.player.UnityPlayer",
            "com.unity3d.player.UnityPlayerForGameActivity",
            "com.unity3d.player.UnityPlayerForActivityOrService"
    };

    // this is used to inject CustomContextWrapper into the game activity
    public static void installHooks(Context gameContext) {
        var classLoader = gameContext.getClassLoader();
        if (classLoader == null) {
            throw new IllegalStateException("ClassLoader is null");
        }

        // get constructor
        ArrayList<Constructor<?>> constructors = new ArrayList<>();
        Class<?> unityPlayerClass = null;
        for (String className : UnityPlayerClassNames) {
            try {
                // Try to load the candidate UnityPlayer class name from the game's classloader.
                Class<?> c = classLoader.loadClass(className);
                Log.i(TAG, "========== UNITY CLASS FOUND ==========");
                Log.i(TAG, "Class: " + c.getName());
                Log.i(TAG, "Loader: " + c.getClassLoader());

                // remember the class and collect constructors that take a Context/Activity as the first parameter
                unityPlayerClass = c;
                for (Constructor<?> cons : c.getDeclaredConstructors()) {
                    Class<?>[] params = cons.getParameterTypes();
                    if (params.length > 0 && (Context.class.isAssignableFrom(params[0]) || Activity.class.isAssignableFrom(params[0]))) {
                        cons.setAccessible(true);
                        constructors.add(cons);
                        Log.d(TAG, "Found candidate constructor: " + cons);
                    }
                }

                // if we found any usable constructors for this class, stop searching further names
                if (!constructors.isEmpty()) {
                    break;
                }
            } catch (Throwable e) {
                Log.e(TAG, "========== UNITY CLASS NOT FOUND ==========", e);
            }
        }

        if (unityPlayerClass == null || constructors.isEmpty()) {
            throw new IllegalStateException("Failed to find UnityPlayer class or constructor");
        }

        Log.i(TAG, "Found UnityPlayer class: " + unityPlayerClass.getName());

        ArrayList<Field> activityFields = new ArrayList<>();
        for (Field field : unityPlayerClass.getFields()) {
            if (Activity.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                activityFields.add(field);
                break;
            }
        }

        for (Constructor<?> constructor : constructors) {
            Log.i(TAG, "Hooking constructor: " + constructor);
            Pine.hook(constructor, new MethodHook() {
                Activity activity = null;

                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    try {
                        if (callFrame.args[0] == null || !(callFrame.args[0] instanceof Activity)) {
                            Log.w(TAG, "First argument is not a Activity, skipping hook");
                            return;
                        }
                        // In UnityPlayerHooks beforeCall:
                        Log.i("UnityPlayerHooks", "Constructor firing, context class: "
                                + callFrame.args[0].getClass().getName());
                        activity = (Activity) callFrame.args[0];
                        callFrame.args[0] = new CustomContextWrapper(gameContext, activity, activity);
                    } catch (Exception e) {
                        Log.i(TAG, "Failed to wrap context!", e);
                    }
                }

                @Override
                public void afterCall(Pine.CallFrame callFrame) {
                    if (activity == null) {
                        return;
                    }

                    for (Field field : activityFields) {
                        try {
                            Log.i(TAG, "Setting activity field: " + field.getName());
                            field.set(callFrame.thisObject, activity);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Failed to set activity field: " + field.getName(), e);
                        }
                    }
                }
            });
        }
    }
}