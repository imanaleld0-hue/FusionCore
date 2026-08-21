package dev.allofus.fusioncore.bridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;
import androidx.browser.customtabs.CustomTabsIntent;
import java.lang.ref.WeakReference;
import dev.allofus.fusioncore.auth.AuthIntentParser;
import dev.allofus.fusioncore.auth.AuthManager;
import dev.allofus.fusioncore.auth.InnerslothAuthData;

public class ActivityBridge {
    private static final String TAG = "ActivityBridge";
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);

    public static void initialize(Activity a) {
        activityRef = new WeakReference<>(a);
        Log.i(TAG, "init " + a.getClass().getSimpleName());
    }

    public static void cleanup() { activityRef.clear(); }
    public static Activity getActivity() { return activityRef.get(); }

    public static boolean hasActiveActivity() {
        Activity a = activityRef.get();
        return a != null && !a.isFinishing() && !a.isDestroyed();
    }

    public static boolean handleAuthIntent(Intent intent) {
        Activity a = getActivity();
        if (a == null) return false;
        InnerslothAuthData d = AuthIntentParser.parseAuthIntent(intent, a);
        if (d != null) {
            AuthManager.getInstance().init(a);
            AuthManager.getInstance().setAuth(d);
            showToast("Авторизован: " + d.name);
            return true;
        }
        return false;
    }

    public static void openAuthUrl(String url) {
        Activity a = getActivity();
        if (a == null) return;
        a.runOnUiThread(() -> {
            try {
                new CustomTabsIntent.Builder().build().launchUrl(a, Uri.parse(url));
            } catch (Exception e) { openUrl(url); }
        });
    }

    public static void openUrl(String url) {
        Activity a = getActivity();
        if (a == null || url == null) return;
        a.runOnUiThread(() -> {
            try {
                Uri u = Uri.parse(url);
                if (u.getScheme() == null) u = Uri.parse("https://" + url);
                Intent i = new Intent(Intent.ACTION_VIEW, u);
                i.addCategory(Intent.CATEGORY_BROWSABLE);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                a.startActivity(i);
            } catch (Exception e) {
                Toast.makeText(a, "Ошибка URL", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void createAlertWindow(String t, String m) {
        Activity a = getActivity();
        if (a == null) return;
        a.runOnUiThread(() -> new AlertDialog.Builder(a)
            .setTitle(t).setMessage(m)
            .setPositiveButton("OK", (d, w) -> d.dismiss()).show());
    }

    public static int getScreenWidth() {
        Activity a = getActivity();
        if (a == null) return 0;
        if (Build.VERSION.SDK_INT >= 30)
            return a.getWindowManager().getCurrentWindowMetrics().getBounds().width();
        DisplayMetrics dm = new DisplayMetrics();
        a.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public static int getScreenHeight() {
        Activity a = getActivity();
        if (a == null) return 0;
        if (Build.VERSION.SDK_INT >= 30)
            return a.getWindowManager().getCurrentWindowMetrics().getBounds().height();
        DisplayMetrics dm = new DisplayMetrics();
        a.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    public static void showToast(String t) {
        Activity a = getActivity();
        if (a != null) a.runOnUiThread(() -> Toast.makeText(a, t, Toast.LENGTH_SHORT).show());
    }

    public static void returnToLauncher() {
        Activity a = getActivity();
        if (a == null) return;
        a.runOnUiThread(() -> {
            try {
                Intent i = a.getPackageManager().getLaunchIntentForPackage(a.getPackageName());
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addCategory(Intent.CATEGORY_LAUNCHER);
                    a.startActivity(i);
                }
            } catch (Exception ignored) {}
            a.overridePendingTransition(0, 0);
            a.finishAffinity();
            Process.killProcess(Process.myPid());
            System.exit(0);
        });
    }
}
