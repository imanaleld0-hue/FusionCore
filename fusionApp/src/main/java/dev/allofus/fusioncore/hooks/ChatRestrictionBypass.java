
package dev.allofus.fusioncore.hooks;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;
import java.lang.reflect.Method;
import dev.allofus.fusioncore.util.FusionCoreLogger;

public class ChatRestrictionBypass {
    private static final String TAG = "ChatBypass";

    public static void applyBypass(boolean enabled) {
        if (!enabled) return;
        try {
            Class<?> accountManager = Class.forName("AccountManager");
            Method method = accountManager.getDeclaredMethod("IsUnderage");
            Pine.hook(method, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                    callFrame.setResult(false);
                }
            });
            FusionCoreLogger.i(TAG, "Chat restriction local bypass successfully applied.");
        } catch (Exception e) {
            FusionCoreLogger.e(TAG, "Failed to apply chat restriction bypass", e);
        }
    }
}
