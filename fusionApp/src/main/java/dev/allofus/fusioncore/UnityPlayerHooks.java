package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

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

    public static void installHooks(Context gameContext) {
        ClassLoader classLoader = gameContext.getClassLoader();

        if (classLoader == null) {
            throw new IllegalStateException("ClassLoader is null");
        }

        ArrayList<Constructor<?>> constructors = new ArrayList<>();
        Class<?> unityPlayerClass = null;

        // Find UnityPlayer class.
        for (String className : UnityPlayerClassNames) {
            try {
                Class<?> c = classLoader.loadClass(className);

                Log.i(TAG, "========== UNITY CLASS FOUND ==========");
                Log.i(TAG, "Class: " + c.getName());
                Log.i(TAG, "Loader: " + c.getClassLoader());

                unityPlayerClass = c;

                for (Constructor<?> constructor : c.getDeclaredConstructors()) {
                    Class<?>[] params = constructor.getParameterTypes();

                    if (params.length > 0
                            && Context.class.isAssignableFrom(params[0])) {

                        constructor.setAccessible(true);
                        constructors.add(constructor);

                        Log.d(
                                TAG,
                                "Found candidate constructor: "
                                        + constructor
                        );
                    }
                }

                if (!constructors.isEmpty()) {
                    break;
                }

            } catch (Throwable e) {
                Log.e(
                        TAG,
                        "========== UNITY CLASS NOT FOUND ==========",
                        e
                );
            }
        }

        if (unityPlayerClass == null || constructors.isEmpty()) {
            throw new IllegalStateException(
                    "Failed to find UnityPlayer class or constructor"
            );
        }

        Log.i(
                TAG,
                "Found UnityPlayer class: "
                        + unityPlayerClass.getName()
        );

        // Find Activity fields.
        ArrayList<Field> activityFields = new ArrayList<>();

        for (Field field : unityPlayerClass.getFields()) {
            if (Activity.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    activityFields.add(field);

                    Log.d(
                            TAG,
                            "Found Activity field: "
                                    + field.getName()
                    );

                    break;
                } catch (Throwable e) {
                    Log.e(
                            TAG,
                            "Failed to access Activity field",
                            e
                    );
                }
            }
        }

        // Hook UnityPlayer constructors.
        for (Constructor<?> constructor : constructors) {

            Log.i(
                    TAG,
                    "Hooking constructor: "
                            + constructor
            );

            Pine.hook(
                    constructor,
                    new MethodHook() {

                        private Activity activity;

                        @Override
                        public void beforeCall(Pine.CallFrame callFrame) {
                            try {
                                if (callFrame.args == null
                                        || callFrame.args.length == 0
                                        || !(callFrame.args[0]
                                        instanceof Activity)) {

                                    Log.w(
                                            TAG,
                                            "First argument is not an Activity, skipping hook"
                                    );

                                    return;
                                }

                                activity =
                                        (Activity) callFrame.args[0];

                                Log.i(
                                        TAG,
                                        "Constructor firing, context class: "
                                                + activity
                                                .getClass()
                                                .getName()
                                );

                                /*
                                 * Replace Unity's Context with our wrapper.
                                 */
                                callFrame.args[0] =
                                        new CustomContextWrapper(
                                                gameContext,
                                                activity,
                                                activity
                                        );

                            } catch (Throwable e) {
                                Log.e(
                                        TAG,
                                        "Failed to wrap context!",
                                        e
                                );
                            }
                        }

                        @Override
                        public void afterCall(Pine.CallFrame callFrame) {
                            if (activity == null) {
                                return;
                            }

                            final Activity targetActivity = activity;

                            /*
                             * UnityPlayer is constructed before the
                             * Activity's DecorView is guaranteed to exist.
                             *
                             * Post the fullscreen operation so that the
                             * window/decor view has time to initialize.
                             */
                            targetActivity
                                    .getWindow()
                                    .getDecorView()
                                    .post(new Runnable() {

                                        @Override
                                        public void run() {
                                            try {
                                                applyFullscreen(
                                                        targetActivity
                                                );

                                            } catch (Throwable e) {
                                                Log.e(
                                                        TAG,
                                                        "Failed to apply fullscreen",
                                                        e
                                                );
                                            }
                                        }
                                    });

                            /*
                             * Restore UnityPlayer's Activity field.
                             */
                            for (Field field : activityFields) {
                                try {
                                    Log.i(
                                            TAG,
                                            "Setting activity field: "
                                                    + field.getName()
                                    );

                                    field.set(
                                            callFrame.thisObject,
                                            activity
                                    );

                                } catch (Throwable e) {
                                    Log.e(
                                            TAG,
                                            "Failed to set activity field: "
                                                    + field.getName(),
                                            e
                                    );
                                }
                            }
                        }
                    }
            );
        }
    }

    private static void applyFullscreen(Activity activity) {
        Window window = activity.getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller =
                    window.getInsetsController();

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

        } else {

            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        Log.i(TAG, "Fullscreen applied successfully");
    }
                                    }
