package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

public class UnityPlayerHooks {

    public static final String TAG = "UnityPlayerHooks";

    public static final String[] UnityPlayerClassNames = new String[] {
            "com.unity3d.player.UnityPlayer",
            "com.unity3d.player.UnityPlayerForGameActivity",
            "com.unity3d.player.UnityPlayerForActivityOrService"
    };

    // Inject CustomContextWrapper into the UnityPlayer constructor.
    public static void installHooks(Context gameContext) {
        ClassLoader classLoader = gameContext.getClassLoader();

        if (classLoader == null) {
            throw new IllegalStateException("ClassLoader is null");
        }

        ArrayList<Constructor<?>> constructors = new ArrayList<>();
        Class<?> unityPlayerClass = null;

        // Find the UnityPlayer implementation used by this Unity version.
        for (String className : UnityPlayerClassNames) {
            try {
                Class<?> c = classLoader.loadClass(className);

                Log.i(TAG, "========== UNITY CLASS FOUND ==========");
                Log.i(TAG, "Class: " + c.getName());
                Log.i(TAG, "Loader: " + c.getClassLoader());

                unityPlayerClass = c;

                for (Constructor<?> cons : c.getDeclaredConstructors()) {
                    Class<?>[] params = cons.getParameterTypes();

                    if (params.length > 0 &&
                            (Context.class.isAssignableFrom(params[0])
                                    || Activity.class.isAssignableFrom(params[0]))) {

                        cons.setAccessible(true);
                        constructors.add(cons);

                        Log.d(TAG, "Found candidate constructor: " + cons);
                    }
                }

                if (!constructors.isEmpty()) {
                    break;
                }

            } catch (Throwable e) {
                Log.e(TAG,
                        "========== UNITY CLASS NOT FOUND: "
                                + className + " ==========",
                        e);
            }
        }

        if (unityPlayerClass == null || constructors.isEmpty()) {
            throw new IllegalStateException(
                    "Failed to find UnityPlayer class or constructor"
            );
        }

        Log.i(TAG, "Found UnityPlayer class: " + unityPlayerClass.getName());

        /*
         * Find a static Activity field inside UnityPlayer.
         *
         * getFields() only returns public fields. Keep the existing behavior
         * for now because this is enough for the current Unity build.
         */
        ArrayList<Field> activityFields = new ArrayList<>();

        for (Field field : unityPlayerClass.getFields()) {
            if (Activity.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                } catch (Throwable ignored) {
                }

                activityFields.add(field);
                break;
            }
        }

        for (Constructor<?> constructor : constructors) {

            Log.i(TAG, "Hooking constructor: " + constructor);

            Pine.hook(constructor, new MethodHook() {

                private Activity activity = null;

                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    try {
                        if (callFrame.args.length == 0
                                || callFrame.args[0] == null
                                || !(callFrame.args[0] instanceof Activity)) {

                            Log.w(
                                    TAG,
                                    "First argument is not an Activity, skipping hook"
                            );
                            return;
                        }

                        Activity originalActivity =
                                (Activity) callFrame.args[0];

                        Log.i(
                                TAG,
                                "Constructor firing, context class: "
                                        + originalActivity.getClass().getName()
                        );

                        activity = originalActivity;

                        callFrame.args[0] =
                                new CustomContextWrapper(
                                        gameContext,
                                        activity,
                                        activity
                                );

                    } catch (Throwable e) {
                        Log.e(TAG, "Failed to wrap context!", e);
                    }
                }

                @Override
                public void afterCall(Pine.CallFrame callFrame) {

                    if (activity == null) {
                        return;
                    }

                    try {
                        /*
                         * Hide Android system bars.
                         *
                         * Unity itself may later modify these flags, so this
                         * is intentionally done after the constructor returns.
                         */
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                            activity.getWindow()
                                    .setDecorFitsSystemWindows(false);

                            WindowInsetsController controller =
                                    activity.getWindow()
                                            .getInsetsController();

                            if (controller != null) {

                                controller.hide(
                                        WindowInsets.Type.statusBars()
                                                | WindowInsets.Type.navigationBars()
                                );

                                controller.setSystemBarsBehavior(
                                        WindowInsetsController
                                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                );
                            }
                        }

                        /*
                         * Restore the Activity reference expected by Unity.
                         */
                        for (Field field : activityFields) {
                            try {
                                Log.i(
                                        TAG,
                                        "Setting activity field: "
                                                + field.getName()
                                );

                                field.set(callFrame.thisObject, activity);

                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(
                                        "Failed to set activity field: "
                                                + field.getName(),
                                        e
                                );
                            }
                        }

                    } catch (Throwable e) {
                        Log.e(
                                TAG,
                                "Failed during UnityPlayer constructor hook",
                                e
                        );
                    }
                }
            });
        }
    }
                            }
