package dev.allofus.fusioncore.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import dev.allofus.fusioncore.R;
import dev.allofus.fusioncore.build.ModBuilder;
import dev.allofus.fusioncore.mod.ModProjectManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import android.app.AlertDialog;
import android.app.ProgressDialog;

public class ModDetailActivity extends BaseFullscreenActivity {

    private String modId;
    private ModProjectManager projectManager;
    private ModProjectManager.ModProject project;
    private RecyclerView recyclerFiles;
    private FileAdapter fileAdapter;

    private final ActivityResultLauncher<String> exportPicker =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
                if (uri != null && project != null) performExport(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_detail);
        modId = getIntent().getStringExtra("mod_id");
        projectManager = new ModProjectManager(this);
        project = projectManager.getProject(modId);
        if (project == null) { finish(); return; }

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(project.name);

        MaterialButton btnConfig = findViewById(R.id.btn_config);
        MaterialButton btnIde = findViewById(R.id.btn_ide);
        MaterialButton btnBuild = findViewById(R.id.btn_build);
        MaterialButton btnExport = findViewById(R.id.btn_export);
        MaterialButton btnAdd = findViewById(R.id.btn_add_to_mods_list); 

        recyclerFiles = findViewById(R.id.recycler_files);
        recyclerFiles.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new FileAdapter();
        recyclerFiles.setAdapter(fileAdapter);
        loadFiles();

        btnConfig.setOnClickListener(v -> openConfig());
        btnIde.setOnClickListener(v -> {
            Intent intent = new Intent(this, MiniIDEActivity.class);
            intent.putExtra("mod_id", modId);
            startActivity(intent);
        });
        btnBuild.setOnClickListener(v -> buildMod());
        btnExport.setOnClickListener(v -> {
            if (project == null) return;
            exportPicker.launch(project.name + "_export.zip");
        });
        if (btnAdd != null) btnAdd.setOnClickListener(v -> addToModsList());
    }

    private void loadFiles() {
        File dir = new File(project.path);
        List<FileItem> items = new ArrayList<>();
        scanDir(dir, items, "");
        fileAdapter.setItems(items);
    }

    private void scanDir(File dir, List<FileItem> items, String prefix) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanDir(f, items, prefix + f.getName() + "/");
            else items.add(new FileItem(prefix + f.getName(), f.getAbsolutePath()));
        }
    }

    private void openConfig() {
        Intent intent = new Intent(this, ConfigEditorActivity.class);
        intent.putExtra("mod_id", modId);
        startActivity(intent);
    }

    private void buildMod() {
    ModBuilder builder = new ModBuilder(this);

    if (!builder.isDotnetInstalled()) {
        showDotnetDownloadDialog(builder);
        return;
    }
   
    runBuild(builder);
    }

    private void showDotnetDownloadDialog(ModBuilder builder) {
 
    new AlertDialog.Builder(this)
            .setTitle(".NET SDK Required")
            .setMessage(
                    "To build C# mods, FusionCore needs to download " +
                    ".NET SDK 10.0.400 for ARM64.\n\n" +
                    "The SDK will be downloaded once and stored " +
                    "in FusionCore's internal storage."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Download", (dialog, which) -> {

                ProgressDialog progress =
                        new ProgressDialog(this);

                progress.setTitle("Downloading .NET SDK");
                progress.setMessage("Preparing...");
                progress.setProgressStyle(
                        ProgressDialog.STYLE_HORIZONTAL
                );
                progress.setMax(100);
                progress.setProgress(0);
                progress.setCancelable(false);
                progress.show();

                builder.installDotnet(
                        new ModBuilder.ProgressListener() {

                            @Override
                            public void onProgress(
                                    int percent,
                                    String message
                            ) {
                                progress.setProgress(percent);
                                progress.setMessage(
                                        message + "\n" +
                                        percent + "%"
                                );
                            }

                            @Override
                            public void onComplete(
                                    File dotnet
                            ) {
                                progress.dismiss();

                                Toast.makeText(
                                        ModDetailActivity.this,
                                        ".NET SDK installed successfully",
                                        Toast.LENGTH_SHORT
                                ).show();

                                runBuild(builder);
                            }

                            @Override
                            public void onError(
                                    Exception error
                            ) {
                                progress.dismiss();

                                new AlertDialog.Builder(
                                        ModDetailActivity.this
                                )
                                        .setTitle(
                                                ".NET SDK Installation Failed"
                                        )
                                        .setMessage(
                                                error.getMessage()
                                        )
                                        .setPositiveButton(
                                                "OK",
                                                null
                                        )
                                        .show();
                            }
                        }
                );
            })
            .show();
}
    
    private void addToModsList() {
        ModBuilder builder = new ModBuilder(this);
        try {
            File output = builder.build(project);
            File pluginsDir = new File(Environment.getExternalStorageDirectory(),
                    "FusionCore/com.innersloth.spacemafia/BepInEx/plugins");
            pluginsDir.mkdirs();
            File dest = new File(pluginsDir, output.getName());
            copyFile(output, dest);
            Toast.makeText(this, "Added to mods list: " + dest.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Add failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void performExport(Uri uri) {
        ModBuilder builder = new ModBuilder(this);
        try {
            File zip = builder.exportToZip(project);
            try (InputStream in = new FileInputStream(zip); OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("Cannot open output stream");
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            Toast.makeText(this, "Exported successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {
        List<FileItem> list = new ArrayList<>();
        void setItems(List<FileItem> list) { this.list = list; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            FileItem item = list.get(p);
            h.name.setText(item.name);
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ModDetailActivity.this, MiniIDEActivity.class);
                intent.putExtra("file_path", item.path);
                intent.putExtra("read_only", item.name.endsWith(".log"));
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView name;
            VH(View v) { super(v); name = v.findViewById(R.id.tv_name); }
        }
    }

    private static class FileItem {
        String name, path;
        FileItem(String n, String p) { name = n; path = p; }
    }
                                }
