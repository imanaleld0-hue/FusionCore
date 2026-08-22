package dev.allofus.fusioncore;

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
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dev.allofus.fusioncore.auth.AuthManager;
import dev.allofus.fusioncore.auth.InnerslothAuthData;
import dev.allofus.fusioncore.auth.AuthBottomSheet;
import dev.allofus.fusioncore.bridge.ActivityBridge;


public class SelectorActivity extends AppCompatActivity {

    private static final String TAG = "FusionCore";

    private static final int REQ_STORAGE = 1001;

    private String pendingPkg;
    private String selectedPkg;

    private List<AppEntry> targets = new ArrayList<>();

    private View cardAuth;
    private View cardGame;
    private View cardMods;
    private View cardDiag;
    private View cardLogs;

    private View dot;

    private TextView authStatus;
    private TextView authName;

    private ImageView gameIcon;

    private TextView gameName;
    private TextView gamePkg;

    private View btnLaunch;
    private View btnSelect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_selector);

        LogcatCollector.start(this);

        ActivityBridge.registerActivity(this);
        ActivityBridge.initialize(this);

        findViews();
        setup();

        targets = resolveTargets();

        if (targets.size() == 1) {
            selectedPkg = targets.get(0).packageName;
        }

        refreshAuth();
        refreshGame();

        if (ActivityBridge.handleAuthIntent(getIntent())) {
            refreshAuth();
        }
    }

   private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (ActivityBridge.handleGooglePlayMergeIntent(intent)) {
            Log.i(TAG, "Auth intent обработан в onNewIntent");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        refreshAuth();

        if (pendingPkg != null && hasStorage()) {
            String pkg = pendingPkg;
            pendingPkg = null;
            launch(pkg);
        }
    }



    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != REQ_STORAGE) {
            return;
        }

        if (pendingPkg == null) {
            return;
        }

        if (hasStorage()) {
            String pkg = pendingPkg;
            pendingPkg = null;
            launch(pkg);
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
        ActivityBridge.clearActivity(this);
        super.onDestroy();
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
        btnSettings = findViewById(R.id.btn_settings);
        
        gameIcon = findViewById(R.id.game_icon);

        gameName = findViewById(R.id.game_name);
        gamePkg = findViewById(R.id.game_package);

        btnLaunch = findViewById(R.id.launch_button);
        btnSelect = findViewById(R.id.select_game_button);
    }


    private void setup() {
        if (cardAuth != null) {
    cardAuth.setOnClickListener(v -> {

        AuthManager manager =
                AuthManager.getInstance(this);

        if (manager.isAuthenticated()) {

            manager.clearAuth();

            refreshAuth();

            Toast.makeText(
                    this,
                    "You're out",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            AuthBottomSheet sheet =
                    AuthBottomSheet.newInstance();

            sheet.show(
                    getSupportFragmentManager(),
                    "AuthBottomSheet"
            );
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
            btnSelect.setOnClickListener(
                    v -> showSelector()
            );
        }

        if (btnSettings != null) {
    btnSettings.setOnClickListener(v ->
        startActivity(new Intent(this, SettingsActivity.class))
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
        startActivity(new Intent(this, LogsActivity.class))
    );
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
    }


    private void refreshAuth() {
        AuthManager manager = AuthManager.getInstance(this);

        if (authStatus != null) {
            authStatus.setText(
                    manager.isAuthenticated()
                            ? R.string.auth_status_connected
                            : R.string.auth_status_disconnected
            );
        }

        if (authName != null) {
            authName.setVisibility(View.GONE);
        }
    }


    private void refreshGame() {

        if (selectedPkg != null) {

            AppEntry entry =
                    findEntry(selectedPkg);

            if (entry != null) {

                if (gameIcon != null) {
                    gameIcon.setImageDrawable(
                            entry.icon
                    );
                }

                if (gameName != null) {
                    gameName.setText(entry.label);
                }

                if (gamePkg != null) {
                    gamePkg.setText(entry.packageName);
                }

                if (btnLaunch != null) {
                    btnLaunch.setEnabled(true);
                }

                return;
            }
        }


        if (gameIcon != null) {
            gameIcon.setImageResource(
                    R.mipmap.app_icon
            );
        }

        if (gameName != null) {
            gameName.setText(
                    R.string.game_select_prompt
            );
        }

        if (gamePkg != null) {
            gamePkg.setText(
                    R.string.game_select_subtitle
            );
        }

        if (btnLaunch != null) {
            btnLaunch.setEnabled(false);
        }
    }


    private AppEntry findEntry(String pkg) {

        for (AppEntry entry : targets) {

            if (entry.packageName.equals(pkg)) {
                return entry;
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

            selectedPkg =
                    targets.get(0).packageName;

            refreshGame();

            return;
        }


        String[] items =
                new String[targets.size()];

        for (int i = 0; i < targets.size(); i++) {
            items[i] = targets.get(i).label;
        }


        new android.app.AlertDialog.Builder(this)
                .setTitle(
                        R.string.game_select_title
                )
                .setItems(
                        items,
                        (dialog, which) -> {

                            selectedPkg =
                                    targets.get(which)
                                            .packageName;

                            refreshGame();
                        }
                )
                .setNegativeButton(
                        R.string.cancel,
                        null
                )
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

        Intent intent =
                new Intent(
                        this,
                        BootstrapActivity.class
                );

        intent.putExtra(
                BootstrapActivity.EXTRA_TARGET_PACKAGE,
                pkg
        );

        intent.putExtra(
                BootstrapActivity.EXTRA_USE_ORIGINAL_LIBUNITY,
                !FusionSettings
                        .isDownloadUnstrippedLibUnity(this)
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        );

        startActivity(intent);

        overridePendingTransition(0, 0);

        finish();

        overridePendingTransition(0, 0);
    }


    private boolean hasStorage() {

        return Build.VERSION.SDK_INT
                < Build.VERSION_CODES.R
                || Environment.isExternalStorageManager();
    }


    private void requestStorage() {

        Toast.makeText(
                this,
                R.string.selector_storage_permission_prompt,
                Toast.LENGTH_LONG
        ).show();


        Intent intent =
                new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                );

        intent.setData(
                Uri.parse(
                        "package:" + getPackageName()
                )
        );


        try {

            startActivityForResult(
                    intent,
                    REQ_STORAGE
            );

        } catch (Exception e) {

            Log.w(
                    TAG,
                    "storage settings failed",
                    e
            );


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

        PackageManager pm =
                getPackageManager();

        List<AppEntry> result =
                new ArrayList<>();


        for (
                ApplicationInfo app :
                pm.getInstalledApplications(
                        PackageManager.GET_META_DATA
                )
        ) {

            File libs =
                    new File(
                            app.nativeLibraryDir
                    );


            if (
                    !new File(
                            libs,
                            "libunity.so"
                    ).exists()
            ) {
                continue;
            }


            if (
                    !new File(
                            libs,
                            "libil2cpp.so"
                    ).exists()
            ) {
                continue;
            }


            if (
                    pm.getLaunchIntentForPackage(
                            app.packageName
                    ) == null
            ) {
                continue;
            }


            String label =
                    app.packageName;

            Drawable icon =
                    pm.getDefaultActivityIcon();

            String versionName =
                    "Unknown";

            long versionCode = 0;


            try {

                ApplicationInfo info =
                        pm.getApplicationInfo(
                                app.packageName,
                                0
                        );

                label =
                        pm.getApplicationLabel(
                                info
                        ).toString();

                icon =
                        pm.getApplicationIcon(
                                info
                        );


                PackageInfo packageInfo;


                if (
                        Build.VERSION.SDK_INT
                                >= Build.VERSION_CODES.TIRAMISU
                ) {

                    packageInfo =
                            pm.getPackageInfo(
                                    app.packageName,
                                    PackageManager.PackageInfoFlags
                                            .of(0)
                            );

                } else {

                    packageInfo =
                            pm.getPackageInfo(
                                    app.packageName,
                                    0
                            );
                }


                if (packageInfo.versionName != null) {
                    versionName =
                            packageInfo.versionName;
                }


                if (
                        Build.VERSION.SDK_INT
                                >= Build.VERSION_CODES.P
                ) {

                    versionCode =
                            packageInfo.getLongVersionCode();

                } else {

                    versionCode =
                            packageInfo.versionCode;
                }


            } catch (Exception e) {

                Log.w(
                        TAG,
                        "meta fail "
                                + app.packageName,
                        e
                );
            }


            result.add(
                    new AppEntry(
                            app.packageName,
                            label,
                            icon,
                            versionName,
                            versionCode
                    )
            );
        }


        return result;
    }


    private static final class AppEntry {

        final String packageName;
        final String label;
        final Drawable icon;
        final String versionName;
        final long versionCode;


        AppEntry(
                String packageName,
                String label,
                Drawable icon,
                String versionName,
                long versionCode
        ) {

            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.versionName = versionName;
            this.versionCode = versionCode;
        }
    }
}
