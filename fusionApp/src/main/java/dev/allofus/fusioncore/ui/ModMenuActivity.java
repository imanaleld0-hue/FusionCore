package dev.allofus.fusioncore.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import dev.allofus.fusioncore.plugins.ModWorkspaceManager;
import dev.allofus.fusioncore.plugins.PluginValidator;

public class ModMenuActivity extends BaseFullscreenActivity {

    private ModWorkspaceManager workspaceManager;
    private TextView consoleOutput;
    private EditText codeEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        workspaceManager =
                new ModWorkspaceManager(this);

        LinearLayout mainLayout =
                new LinearLayout(this);

        mainLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        mainLayout.setPadding(
                16,
                16,
                16,
                16
        );

        LinearLayout topBar =
                new LinearLayout(this);

        Button btnCheckDll =
                new Button(this);

        btnCheckDll.setText("Check DLL");

        Button btnLoadZip =
                new Button(this);

        btnLoadZip.setText("Load ZIP");

        Button btnBuild =
                new Button(this);

        btnBuild.setText("Build Mod");

        topBar.addView(btnCheckDll);
        topBar.addView(btnLoadZip);
        topBar.addView(btnBuild);

        codeEditor =
                new EditText(this);

        codeEditor.setHint(
                "C# script and configuration editor"
        );

        codeEditor.setBackgroundColor(
                Color.parseColor("#2B2B2B")
        );

        codeEditor.setTextColor(Color.WHITE);
        codeEditor.setGravity(
                Gravity.TOP
        );

        codeEditor.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        600
                )
        );

        consoleOutput =
                new TextView(this);

        consoleOutput.setText(
                "Console initialized. Workspace ready."
        );

        consoleOutput.setTextColor(
                Color.GREEN
        );

        consoleOutput.setBackgroundColor(
                Color.BLACK
        );

        consoleOutput.setPadding(
                10,
                10,
                10,
                10
        );

        ScrollView scrollConsole =
                new ScrollView(this);

        scrollConsole.addView(
                consoleOutput
        );

        mainLayout.addView(topBar);
        mainLayout.addView(codeEditor);
        mainLayout.addView(scrollConsole);

        setContentView(mainLayout);

        btnCheckDll.setOnClickListener(
                v -> checkDllsInWorkspace()
        );

        btnBuild.setOnClickListener(
                v -> buildWorkspace()
        );

        btnLoadZip.setOnClickListener(
                v -> log("ZIP loading is not implemented yet.")
        );
    }

    private void log(String msg) {
        consoleOutput.append(
                "\n> " + msg
        );
    }

    private void checkDllsInWorkspace() {

        File[] files =
                workspaceManager
                        .getWorkspace()
                        .listFiles();

        if (files == null || files.length == 0) {
            log("Workspace is empty. Load files first.");
            return;
        }

        for (File file : files) {

            if (file.isFile() &&
                    file.getName()
                            .toLowerCase()
                            .endsWith(".dll")) {

                PluginValidator.Result result =
                        PluginValidator.checkDll(file);

                if (result.valid) {

                    log(
                            file.getName()
                                    + " [VALID] Architecture: "
                                    + result.arch
                    );

                    if (result.error != null) {
                        log(
                                "Warning: "
                                        + result.error
                        );
                    }

                } else {

                    log(
                            file.getName()
                                    + " [ERROR]: "
                                    + result.error
                    );
                }
            }
        }
    }

    private void buildWorkspace() {

        try {

            File out =
                    new File(
                            getFilesDir(),
                            "CompiledMod.zip"
                    );

            workspaceManager.exportModZip(out);

            log(
                    "Workspace exported successfully: "
                            + out.getAbsolutePath()
            );

            Toast.makeText(
                    this,
                    "Mod exported to ZIP.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            log(
                    "Build failed: "
                            + e.getMessage()
            );
        }
    }
}
