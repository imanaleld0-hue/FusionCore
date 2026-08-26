package dev.allofus.fusioncore.hooks;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

public class UnityPlayerHooks {
    public static final String TAG = "UnityPlayerHooks";

    public static final String[] UnityPlayerClassNames = {
        "com.unity3d.player.UnityPlayer",
        "com.unity3d.player.UnityPlayerForGameActivity",
        "com.unity3d.player.UnityPlayerForActivityOrService"
    };

    public static void installHooks(Context gameContext) {
        ClassLoader classLoader = gameContext.getClassLoader();

        if (classLoader == null) {
            throw new IllegalStateException("ClassLoader is null");
        }

        ArrayList<Constructor<?>> constructors = new ArrayList<>();

        for (String className : UnityPlayerClassNames) {
            try {
                Class<?> clazz = classLoader.loadClass(className);

                Log.i(TAG, "Found Unity class: " + clazz.getName());

                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    Class<?>[] params = constructor.getParameterTypes();

                    if (params.length > 0 &&
                        Context.class.isAssignableFrom(params[0])) {

                        constructor.setAccessible(true);
                        constructors.add(constructor);

                        Log.i(TAG, "Found constructor: " + constructor);
                    }
                }

            } catch (ClassNotFoundException e) {
                Log.d(TAG, "Unity class not found: " + className);
            } catch (Throwable e) {
                Log.e(TAG, "Failed inspecting " + className, e);
            }
        }

        if (constructors.isEmpty()) {
            throw new IllegalStateException(
                "No compatible UnityPlayer constructors found"
            );
        }

        for (Constructor<?> constructor : constructors) {
            Log.i(TAG, "Hooking Unity constructor: " + constructor);

            Pine.hook(constructor, new MethodHook() {

                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    try {
                        if (callFrame.args == null ||
                            callFrame.args.length == 0) {
                            return;
                        }

                        Object context = callFrame.args[0];

                        Log.i(
                            TAG,
                            "UnityPlayer constructor: context=" +
                            (context == null
                                ? "null"
                                : context.getClass().getName())
                        );

                        /*
                         * IMPORTANT:
                         *
                         * Не заменяем Context.
                         * Не изменяем Activity-поля UnityPlayer.
                         * Не трогаем constructor arguments.
                         *
                         * Unity получает свои оригинальные аргументы.
                         */
                    } catch (Throwable e) {
                        Log.e(TAG, "Unity constructor beforeCall failed", e);
                    }
                }

                @Override
                public void afterCall(Pine.CallFrame callFrame) {
                    try {
                        Activity activity = findActivity(callFrame);

                        if (activity == null) {
                            Log.w(
                                TAG,
                                "UnityPlayer created, but Activity was not found"
                            );
                            return;
                        }

                        Log.i(
                            TAG,
                            "UnityPlayer created. Applying fullscreen to " +
                            activity.getClass().getName()
                        );

                        final Activity targetActivity = activity;

                        targetActivity.runOnUiThread(() -> {
                            try {
                                applyFullscreen(targetActivity);
                            } catch (Throwable e) {
                                Log.e(
                                    TAG,
                                    "Failed to apply fullscreen",
                                    e
                                );
                            }
                        });

                    } catch (Throwable e) {
                        Log.e(
                            TAG,
                            "Unity constructor afterCall failed",
                            e
                        );
                    }
                }
            });
        }

        Log.i(
            TAG,
            "UnityPlayer hooks installed: " + constructors.size()
        );
    }

    private static Activity findActivity(Pine.CallFrame callFrame) {
        try {
            if (callFrame.args == null) {
                return null;
            }

            for (Object arg : callFrame.args) {
                if (arg instanceof Activity) {
                    return (Activity) arg;
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "Failed to find Activity", e);
        }

        return null;
    }

    private static void applyFullscreen(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        View decorView = activity
            .getWindow()
            .getDecorView();

        int flags =
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

        decorView.setSystemUiVisibility(flags);

        Log.i(TAG, "Fullscreen mode applied");
    }
}
