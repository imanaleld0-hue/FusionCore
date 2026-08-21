package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import dev.allofus.fusioncore.auth.AuthManager;
import dev.allofus.fusioncore.auth.InnerslothAuthData;
import dev.allofus.fusioncore.bridge.ActivityBridge;

public class SelectorActivity extends Activity {
    private static final String TAG = "FusionCore";
    private static final int REQ_STORAGE = 1001;
    private String pendingPkg, selectedPkg;
    private List<AppEntry> targets = new ArrayList<>();

    private CardView cardAuth, cardGame, cardMods, cardDiag, cardLogs;
    private View dot;
    private TextView authStatus, authName;
    private ImageView gameIcon;
    private TextView gameName, gamePkg;
    private View btnLaunch, btnSelect;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LogcatCollector.start(this);
        setContentView(R.layout.activity_selector);
        ActivityBridge.initialize(this);
        AuthManager.getInstance().init(this);
        findViews();
        setup();
        refreshAuth();
        refreshGame();
        if (ActivityBridge.handleAuthIntent(getIntent())) refreshAuth();
        targets = resolveTargets();
        if (targets.size() == 1) { selectedPkg = targets.get(0).packageName; refreshGame(); }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshAuth();
        if (pendingPkg != null && hasStorage()) { String p = pendingPkg; pendingPkg = null; launch(p); }
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); if (ActivityBridge.handleAuthIntent(i)) refreshAuth(); }

    @Override protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc != REQ_STORAGE || pendingPkg == null) return;
        if (hasStorage()) { String p = pendingPkg; pendingPkg = null; launch(p); }
        else Toast.makeText(this, R.string.selector_storage_permission_required, Toast.LENGTH_LONG).show();
    }

    @Override protected void onDestroy() { super.onDestroy(); ActivityBridge.cleanup(); }

    private void findViews() {
        cardAuth = findViewById(R.id.card_auth);
        cardGame = findViewById(R.id.card_game);
        cardMods = findViewById(R.id.card_mods);
        cardDiag = findViewById(R.id.card_diagnostics);
        cardLogs = findViewById(R.id.card_logs);
        dot = findViewById(R.id.auth_status_dot);
        authStatus = findViewById(R.id.auth_status_text);
        authName = findViewById(R.id.auth_user_name);
        gameIcon = findViewById(R.id.game_icon);
        gameName = findViewById(R.id.game_name);
        gamePkg = findViewById(R.id.game_package);
        btnLaunch = findViewById(R.id.launch_button);
        btnSelect = findViewById(R.id.select_game_button);
    }

    private void setup() {
        findViewById(R.id.auth_action_button).setOnClickListener(v -> {
            if (AuthManager.getInstance().isAuthenticated()) {
                AuthManager.getInstance().clearAuth(); refreshAuth();
                Toast.makeText(this, "Вы вышли", Toast.LENGTH_SHORT).show();
            } else {
                AuthBottomSheet s = new AuthBottomSheet();
                s.setOnAuthResult(() -> runOnUiThread(this::refreshAuth));
                s.show(getFragmentManager(), "auth");
            }
        });
        cardAuth.setOnClickListener(v -> findViewById(R.id.auth_action_button).performClick());
        btnLaunch.setOnClickListener(v -> { if (selectedPkg == null) showSelector(); else maybeLaunch(selectedPkg); });
        btnSelect.setOnClickListener(v -> showSelector());
        cardMods.setOnClickListener(v -> Toast.makeText(this, "Mods — soon", Toast.LENGTH_SHORT).show());
        cardDiag.setOnClickListener(v -> Toast.makeText(this, "Diagnostics — soon", Toast.LENGTH_SHORT).show());
        cardLogs.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void refreshAuth() {
        InnerslothAuthData d = AuthManager.getInstance().getCurrentAuth();
        if (d != null && d.isValid()) {
            dot.setBackgroundResource(R.drawable.dot_green);
            authStatus.setText(R.string.auth_status_connected);
            authName.setText(d.name); authName.setVisibility(View.VISIBLE);
        } else {
            dot.setBackgroundResource(R.drawable.dot_red);
            authStatus.setText(R.string.auth_status_disconnected);
            authName.setVisibility(View.GONE);
        }
    }

    private void refreshGame() {
        if (selectedPkg != null) {
            AppEntry e = findEntry(selectedPkg);
            if (e != null) {
                gameIcon.setImageDrawable(e.icon);
                gameName.setText(e.label);
                gamePkg.setText(e.packageName);
                btnLaunch.setEnabled(true); return;
            }
        }
        gameIcon.setImageResource(R.mipmap.app_icon);
        gameName.setText(R.string.game_select_prompt);
        gamePkg.setText(R.string.game_select_subtitle);
        btnLaunch.setEnabled(false);
    }

    private AppEntry findEntry(String pkg) { for (AppEntry e : targets) if (e.packageName.equals(pkg)) return e; return null; }

    private void showSelector() {
        if (targets.isEmpty()) { Toast.makeText(this, R.string.selector_empty_text, Toast.LENGTH_LONG).show(); return; }
        if (targets.size() == 1) { selectedPkg = targets.get(0).packageName; refreshGame(); return; }
        String[] items = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) items[i] = targets.get(i).label;
        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.game_select_title).setItems(items, (d, w) -> { selectedPkg = targets.get(w).packageName; refreshGame(); })
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void maybeLaunch(String pkg) {
        if (!hasStorage()) { pendingPkg = pkg; requestStorage(); return; }
        launch(pkg);
    }

    private void launch(String pkg) {
        Intent i = new Intent(this, BootstrapActivity.class);
        i.putExtra(BootstrapActivity.EXTRA_TARGET_PACKAGE, pkg);
        i.putExtra(BootstrapActivity.EXTRA_USE_ORIGINAL_LIBUNITY, !FusionSettings.isDownloadUnstrippedLibUnity(this));
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i); overridePendingTransition(0, 0); finish(); overridePendingTransition(0, 0);
    }

    private boolean hasStorage() { return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager(); }

    private void requestStorage() {
        Toast.makeText(this, R.string.selector_storage_permission_prompt, Toast.LENGTH_LONG).show();
        Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        i.setData(Uri.parse("package:" + getPackageName()));
        try { startActivityForResult(i, REQ_STORAGE); }
        catch (Exception e) {
            Log.w(TAG, "storage settings failed", e);
            try { startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), REQ_STORAGE); }
            catch (Exception e2) { Toast.makeText(this, R.string.selector_storage_permission_open_failed, Toast.LENGTH_LONG).show(); }
        }
    }

    private List<AppEntry> resolveTargets() {
        PackageManager pm = getPackageManager();
        List<AppEntry> res = new ArrayList<>();
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            File libs = new File(app.nativeLibraryDir);
            if (!new File(libs, "libunity.so").exists() || !new File(libs, "libil2cpp.so").exists()) continue;
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            String label = app.packageName;
            Drawable icon = pm.getDefaultActivityIcon();
            String ver = "Unknown";
            long code = 0;
            try {
                ApplicationInfo info = pm.getApplicationInfo(app.packageName, 0);
                label = pm.getApplicationLabel(info).toString();
                icon = pm.getApplicationIcon(info);
                PackageInfo pi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
                    : pm.getPackageInfo(app.packageName, 0);
                if (pi.versionName != null) ver = pi.versionName;
                code = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? pi.getLongVersionCode() : pi.versionCode;
            } catch (Exception e) { Log.w(TAG, "meta fail " + app.packageName, e); }
            res.add(new AppEntry(app.packageName, label, icon, ver, code));
        }
        return res;
    }

    private record AppEntry(String packageName, String label, Drawable icon, String versionName, long versionCode) {}
}
