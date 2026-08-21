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

import dev.allofus.fusioncore.auth.AuthBottomSheet;
import dev.allofus.fusioncore.auth.AuthManager;
import dev.allofus.fusioncore.auth.InnerslothAuthData;
import dev.allofus.fusioncore.bridge.ActivityBridge;

public class SelectorActivity extends Activity {

    private static final String TAG = "FusionCore";
    private static final int REQ_STORAGE = 1001;

    private String pendingPkg;
    private String selectedPkg;

    private List<AppEntry> targets = new ArrayList<>();

    private CardView cardAuth;
    private CardView cardGame;
    private CardView cardMods;
    private CardView cardDiag;
    private CardView cardLogs;

    private View dot;
    private TextView authStatus;
    private TextView authName;

    private ImageView gameIcon;
    private TextView gameName;
    private TextView gamePkg;

    private View btnLaunch;
    private View btnSelect;

    private TextView tvEmpty;
    private View btnOpenAuth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LogcatCollector.start(this);

        ActivityBridge.registerActivity(this);

        setContentView(R.layout.activity_selector);

        ActivityBridge.initialize(this);
        AuthManager.getInstance().init(this);

        findViews();
        setup();

        refreshAuth();
        refreshGame();

        tvEmpty = findViewById(R.id.tvSelectorEmpty);
        if (tvEmpty != null) {
            tvEmpty.setText(R.string.selector_empty_text);
        }

        btnOpenAuth = findViewById(R.id.btnOpenAuth);
        if (btnOpenAuth != null) {
            btnOpenAuth.setOnClickListener(v -> {
                AuthBottomSheet bottomSheet = AuthBottomSheet.newInstance();

                /*
                 * Если AuthBottomSheet является androidx.fragment.app.DialogFragment,
                 * этот Activity должен быть AppCompatActivity.
                 *
                 * Если он обычный android.app.DialogFragment,
                 * используется getFragmentManager().
                 */
                bottomSheet.show(getFragmentManager(), "AuthBottomSheet");
            });
        }

        if (ActivityBridge.handleAuthIntent(getIntent())) {
            refreshAuth();
        }

        targets = resolveTargets();

        if (targets.size() == 1) {
            selectedPkg = targets.get(0).packageName;
            refreshGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshAuth();

        if (pendingPkg != null && hasStorage()) {
            String p = pendingPkg;
            pendingPkg = null;
            launch(p);
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);

        setIntent(i);

        if (ActivityBridge.handleAuthIntent(i)) {
            refreshAuth();
        }
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);

        if (rc != REQ_STORAGE || pendingPkg == null) {
            return;
        }

        if (hasStorage()) {
            String p = pendingPkg;
            pendingPkg = null;
            launch(p);
        } else {
            Toast.makeText(
                this,
                R.string.selector_storage_permission_required,
                Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityBridge.clearActivity(this);
    }

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
        View authActionButton = findViewById(R.id.auth_action_button);

        if (authActionButton != null) {
            authActionButton.setOnClickListener(v -> {
                if (AuthManager.getInstance().isAuthenticated()) {
                    AuthManager.getInstance().clearAuth();
                    refreshAuth();

                    Toast.makeText(
                        this,
                        "Вы вышли",
                        Toast.LENGTH_SHORT
                    ).show();

                } else {
                    AuthBottomSheet s = AuthBottomSheet.newInstance();

                    s.setOnAuthResult(
                        () -> runOnUiThread(this::refreshAuth)
                    );

                    s.show(getFragmentManager(), "auth");
                }
            });
        }

        if (cardAuth != null) {
            cardAuth.setOnClickListener(v -> {
                if (authActionButton != null) {
                    authActionButton.performClick();
                }
            });
        }

        if (btnLaunch != null) {
            btnLaunch.setOnClickListener(v -> {
                if (selectedPkg == null) {
                    showSelector();
                } else {
                    maybeLaunch(selectedPkg);
                }
            });
        }

        if (btnSelect != null) {
            btnSelect.setOnClickListener(v -> showSelector());
        }

        if (cardMods != null) {
            cardMods.setOnClickListener(v ->
                Toast.makeText(
                    this,
                    "Mods — soon",
                    Toast.LENGTH_SHORT
                ).show()
            );
        }

        if (cardDiag != null) {
            cardDiag.setOnClickListener(v ->
                Toast.makeText(
                    this,
                    "Diagnostics — soon",
                    Toast.LENGTH_SHORT
                ).show()
            );
        }

        if (cardLogs != null) {
            cardLogs.setOnClickListener(v ->
                startActivity(
                    new Intent(this, SettingsActivity.class)
                )
            );
        }
    }

    private void refreshAuth() {
        InnerslothAuthData d =
            AuthManager.getInstance().getCurrentAuth();

        if (d != null && d.isValid()) {
            if (dot != null) {
                dot.setBackgroundResource(R.drawable.dot_green);
            }

            if (authStatus != null) {
                authStatus.setText(R.string.auth_status_connected);
            }

            if (authName != null) {
                authName.setText(d.name);
                authName.setVisibility(View.VISIBLE);
            }

        } else {
            if (dot != null) {
                dot.setBackgroundResource(R.drawable.dot_red);
            }

            if (authStatus != null) {
                authStatus.setText(R.string.auth_status_disconnected);
            }

            if (authName != null) {
                authName.setVisibility(View.GONE);
            }
        }
    }

    private void refreshGame() {
        if (selectedPkg != null) {
            AppEntry e = findEntry(selectedPkg);

            if (e != null) {
                gameIcon.setImageDrawable(e.icon);
                gameName.setText(e.label);
                gamePkg.setText(e.packageName);

                if (btnLaunch != null) {
                    btnLaunch.setEnabled(true);
                }

                return;
            }
        }

        gameIcon.setImageResource(R.mipmap.app_icon);
        gameName.setText(R.string.game_select_prompt);
        gamePkg.setText(R.string.game_select_subtitle);

        if (btnLaunch != null) {
            btnLaunch.setEnabled(false);
        }
    }

    private AppEntry findEntry(String pkg) {
        for (AppEntry e : targets) {
            if (e.packageName.equals(pkg)) {
                return e;
            }
        }

        return null;
    }

    private void showSelector() {
        if (targets.isEmpty()) {
            Toast.makeText(
                this,
                R.string.selector_empty_text,
                Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (targets.size() == 1) {
            selectedPkg = targets.get(0).packageName;
            refreshGame();
            return;
        }

        String[] items = new String[targets.size()];

        for (int i = 0; i < targets.size(); i++) {
            items[i] = targets.get(i).label;
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.game_select_title)
            .setItems(
                items,
                (d, w) -> {
                    selectedPkg = targets.get(w).packageName;
                    refreshGame();
                }
            )
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void maybeLaunch(String pkg) {
        if (!hasStorage()) {
            pendingPkg = pkg;
            requestStorage();
            return;
        }

        launch(pkg);
    }

    private void launch(String pkg) {
        Intent i = new Intent(
            this,
            BootstrapActivity.class
        );

        i.putExtra(
            BootstrapActivity.EXTRA_TARGET_PACKAGE,
            pkg
        );

        i.putExtra(
            BootstrapActivity.EXTRA_USE_ORIGINAL_LIBUNITY,
            !FusionSettings.isDownloadUnstrippedLibUnity(this)
        );

        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        startActivity(i);

        overridePendingTransition(0, 0);

        finish();

        overridePendingTransition(0, 0);
    }

    private boolean hasStorage() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R
            || Environment.isExternalStorageManager();
    }

    private void requestStorage() {
        Toast.makeText(
            this,
            R.string.selector_storage_permission_prompt,
            Toast.LENGTH_LONG
        ).show();

        Intent i = new Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        );

        i.setData(
            Uri.parse("package:" + getPackageName())
        );

        try {
            startActivityForResult(i, REQ_STORAGE);

        } catch (Exception e) {
            Log.w(TAG, "storage settings failed", e);

            try {
                startActivityForResult(
                    new Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    ),
                    REQ_STORAGE
                );

            } catch (Exception e2) {
                Toast.makeText(
                    this,
                    R.string.selector_storage_permission_open_failed,
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private List<AppEntry> resolveTargets() {
        PackageManager pm = getPackageManager();

        List<AppEntry> res = new ArrayList<>();

        for (
            ApplicationInfo app :
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        ) {
            File libs = new File(app.nativeLibraryDir);

            if (
                !new File(libs, "libunity.so").exists()
                    || !new File(libs, "libil2cpp.so").exists()
            ) {
                continue;
            }

            if (
                pm.getLaunchIntentForPackage(app.packageName) == null
            ) {
                continue;
            }

            String label = app.packageName;
            Drawable icon = pm.getDefaultActivityIcon();

            String ver = "Unknown";
            long code = 0;

            try {
                ApplicationInfo info =
                    pm.getApplicationInfo(
                        app.packageName,
                        0
                    );

                label =
                    pm.getApplicationLabel(info).toString();

                icon =
                    pm.getApplicationIcon(info);

                PackageInfo pi =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? pm.getPackageInfo(
                            app.packageName,
                            PackageManager.PackageInfoFlags.of(0)
                        )
                        : pm.getPackageInfo(
                            app.packageName,
                            0
                        );

                if (pi.versionName != null) {
                    ver = pi.versionName;
                }

                code =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        ? pi.getLongVersionCode()
                        : pi.versionCode;

            } catch (Exception e) {
                Log.w(
                    TAG,
                    "meta fail " + app.packageName,
                    e
                );
            }

            res.add(
                new AppEntry(
                    app.packageName,
                    label,
                    icon,
                    ver,
                    code
                )
            );
        }

        return res;
    }

    private record AppEntry(
        String packageName,
        String label,
        Drawable icon,
        String versionName,
        long versionCode
    ) {}
                    }
