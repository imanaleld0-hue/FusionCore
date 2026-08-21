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


public class ActivityBridge {
    private static final String TAG = "ActivityBridge";
    private static final String ACCOUNT_MERGE_URL = "https://accounts.google.com/signin/oauth/identifier";

    private static WeakReference<Activity> activityRef = new WeakReference<>(null);
    private static GoogleSignInHelper googleSignInHelper;

    public static void initialize(Activity currentActivity) {
        activityRef = new WeakReference<>(currentActivity);
        Log.i(TAG, "ActivityBridge initialized: " + currentActivity.getClass().getName());
    }

    public static void cleanup() {
        activityRef.clear();
        googleSignInHelper = null;
    }

    public static Activity getActivity() {
        return activityRef.get();
    }

    public static boolean hasActiveActivity() {
        Activity a = activityRef.get();
        return a != null && !a.isFinishing() && !a.isDestroyed();
    }

    
    public static boolean handleGooglePlayMergeIntent(Intent intent) {
        if (intent == null) return false;

        String extractedToken = AuthIntentParser.parseIntent(intent, getActivity());
        if (!extractedToken.isEmpty()) {
            Log.i(TAG, "Authorization token intercepted: " + AuthIntentParser.maskToken(extractedToken));

            if (!AuthIntentParser.looksLikeJwt(extractedToken)) {
                Log.w(TAG, "The token has an invalid JWT structure and is rejected.");
                showToast("Error: Invalid authorization token");
                return false;
            }

            AuthManager.getInstance().handleReceivedToken(extractedToken);
            showToast("Authorization token successfully received!");
            return true;
        }
        return false;
    }

    public static void startGoogleSignIn(Activity activity) {
        if (activity == null) {
            Log.e(TAG, "startGoogleSignIn: activity is null");
            return;
        }
        if (googleSignInHelper == null) {
            googleSignInHelper = new GoogleSignInHelper(activity);
        }
        googleSignInHelper.signIn(activity);
    }

    public static GoogleSignInHelper getGoogleSignInHelper() {
        return googleSignInHelper;
    }

    public static void setGoogleSignInHelper(GoogleSignInHelper helper) {
        googleSignInHelper = helper;
    }

    public static void openAccountMergeWindow() {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            try {
                new CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(ACCOUNT_MERGE_URL));
            } catch (Exception e) {
                openUrl(ACCOUNT_MERGE_URL);
            }
        });
    }

    public static void openUrl(String url) {
        Activity activity = getActivity();
        if (activity == null || url == null || url.trim().isEmpty()) return;
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            try {
                Uri uri = Uri.parse(url);
                if (uri.getScheme() == null) {
                    uri = Uri.parse("https://" + url);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } catch (ActivityNotFoundException unused) {
                Toast.makeText(activity, "Browser not found", Toast.LENGTH_SHORT).show();
            } catch (Exception unused2) {
                Toast.makeText(activity, "Failed to open URL", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void createAlertWindow(String title, String message) {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Copy", (dialog, which) -> {
                    ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                    if (clipboardManager != null) {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("FusionCore Alert", message));
                        Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setCancelable(false)
                .create()
                .show();
        });
    }

    public static int getScreenWidth() {
        Activity activity = getActivity();
        if (activity == null) return 0;
        if (Build.VERSION.SDK_INT >= 30) {
            return activity.getWindowManager().getCurrentWindowMetrics().getBounds().width();
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public static int getScreenHeight() {
        Activity activity = getActivity();
        if (activity == null) return 0;
        if (Build.VERSION.SDK_INT >= 30) {
            return activity.getWindowManager().getCurrentWindowMetrics().getBounds().height();
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    public static void showToast(String text) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
        }
    }

    public static void returnToLauncher() {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            try {
                Intent launchIntent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    activity.startActivity(launchIntent);
                }
            } catch (Exception ignored) {}
            activity.overridePendingTransition(0, 0);
            activity.finishAffinity();
            Process.killProcess(Process.myPid());
            System.exit(0);
        });
    }
}
