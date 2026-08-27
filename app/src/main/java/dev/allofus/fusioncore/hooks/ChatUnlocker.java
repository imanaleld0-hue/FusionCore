package dev.allofus.fusioncore.hooks;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;
import java.lang.reflect.Method;

public class ChatUnlocker {
    public static void enableFreeChat() {
        try {
            Class<?> playerControl = Class.forName("PlayerControl");
            Method checkChatMethod = playerControl.getDeclaredMethod("GetIsFreeChatAllowed");
            
            Pine.hook(checkChatMethod, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                    callFrame.setResult(true);
                }
            });
            System.out.println("[FusionCore] ChatUnlocker applied locally.");
        } catch (Exception e) {
            System.out.println("[FusionCore] ChatUnlocker failed to apply.");
        }
    }
}
