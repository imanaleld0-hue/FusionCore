package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

public class StubActivity extends Activity {
    private static final String TAG = "StubActivity";

    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        enableImmersiveMode();
        Intent current = getIntent();
        if (current != null && current.getBooleanExtra(InstrumentationHooks.EXTRA_IS_DYNAMIC_ACTIVITY, false)) {
            Intent original = current.getParcelableExtra(InstrumentationHooks.EXTRA_ORIGINAL_INTENT);
            if (original != null && original.getComponent() != null) {
                Log.d(TAG, "Placeholder: " + original.getComponent().getClassName());
            } else {
                Log.e(TAG, "No valid original intent");
            }
        }
    }

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableImmersiveMode();
    }
}
