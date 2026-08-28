package dev.allofus.fusioncore;

import android.content.Context;
import android.content.SharedPreferences;

public class FusionSettings {
    private static final String PREFS = "fusion_settings";

    public static String getPlayerName(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("player_name", "Player");
    }
    
    public static String getActivityOverrideForGame(Context ctx, String packageName) {
    return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("activity_override_" + packageName, "Automatic");
    }
    
    public static void setPlayerName(Context ctx, String name) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("player_name", name).apply();
    }
    public static int getPlayerLevel(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("player_level", 1);
    }
    public static void setPlayerLevel(Context ctx, int level) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("player_level", level).apply();
    }
    public static boolean isAutoClearLogs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("auto_clear_logs", false);
    }
    public static void setAutoClearLogs(Context ctx, boolean v) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("auto_clear_logs", v).apply();
    }
    public static boolean isDownloadUnstrippedLibUnity(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("download_unstripped", true);
    }
}
