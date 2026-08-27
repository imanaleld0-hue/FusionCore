package dev.allofus.fusioncore.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import dev.allofus.fusioncore.R;
import dev.allofus.fusioncore.build.ModBuilder;
import dev.allofus.fusioncore.mod.ModProjectManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModDetailActivity extends BaseFullscreenActivity {

    private String modId;
    private ModProjectManager projectManager;
    private ModProjectManager.ModProject project;
    private RecyclerView recyclerFiles;
    private FileAdapter fileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_detail);
        modId = getIntent().getStringExtra("mod_id");
        projectManager = new ModProjectManager(this);
        project = projectManager.getProject(modId);
        if (project == null) {
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(project.name);

        MaterialButton btnConfig = findViewById(R.id.btn_config);
        MaterialButton btnIde = findViewById(R.id.btn_ide);
        MaterialButton btnBuild = findViewById(R.id.btn_build);
        MaterialButton btnExport = findViewById(R.id.btn_export);
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
        btnExport.setOnClickListener(v -> exportMod());
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
            if (f.isDirectory()) {
                scanDir(f, items, prefix + f.getName() + "/");
            } else {
                FileItem item = new FileItem();
                item.name = prefix + f.getName();
                item.path = f.getAbsolutePath();
                items.add(item);
            }
        }
    }

    private void openConfig() {
        Intent intent = new Intent(this, ConfigEditorActivity.class);
        intent.putExtra("mod_id", modId);
        startActivity(intent);
    }

    private void buildMod() {
        ModBuilder builder = new ModBuilder(this);
        try {
            File output = builder.build(project);
            Toast.makeText(this, "Build ready: " + output.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Build failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportMod() {
        ModBuilder builder = new ModBuilder(this);
        try {
            File zip = builder.exportToZip(project);
            Toast.makeText(this, "Exported: " + zip.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {
        List<FileItem> list = new ArrayList<>();

        void setItems(List<FileItem> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
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

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView name;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tv_name);
            }
        }
    }

    private static class FileItem {
        String name;
        String path;
    }
}
