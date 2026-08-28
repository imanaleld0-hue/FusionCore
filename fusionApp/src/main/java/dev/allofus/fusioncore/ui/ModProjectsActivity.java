package dev.allofus.fusioncore.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import dev.allofus.fusioncore.R;
import dev.allofus.fusioncore.mod.ModProjectManager;
import dev.allofus.fusioncore.mod.ModValidator;
import java.io.File;
import java.io.InputStream;
import java.util.List;

public class ModProjectsActivity extends BaseFullscreenActivity {

    private RecyclerView recyclerView;
    private ModAdapter adapter;
    private ModProjectManager projectManager;
    private ModValidator validator;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openFilePicker();
                else Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String[]> pickFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
                if (result != null) handleSelectedFile(result);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_projects);
        projectManager = new ModProjectManager(this);
        validator = new ModValidator();

        recyclerView = findViewById(R.id.recycler_mods);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ModAdapter();
        recyclerView.setAdapter(adapter);

        MaterialButton btnUpload = findViewById(R.id.btn_upload);
        MaterialButton btnCreate = findViewById(R.id.btn_create);

        btnUpload.setOnClickListener(v -> checkPermissionAndPick());
        btnCreate.setOnClickListener(v -> showCreateModDialog());

        loadProjects();
    }

    private void checkPermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openFilePicker();
            return;
        }
        String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openFilePicker();
        } else {
            requestPermissionLauncher.launch(perm);
        }
    }

    private void openFilePicker() {
        pickFileLauncher.launch(new String[]{"*/*"});
    }

    private void handleSelectedFile(Uri uri) {
        try {
            String name = getFileName(uri);
            if (!validator.isValidFileName(name)) {
                Toast.makeText(this, "Invalid file type. Allowed: .dll, .cs, .zip, .tar", Toast.LENGTH_SHORT).show();
                return;
            }
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) throw new IllegalStateException("Cannot open stream");
            File modsDir = new File(getFilesDir(), "mods");
            modsDir.mkdirs();
            File target = new File(modsDir, name);
            copyStream(is, target);

            if (name.endsWith(".zip") || name.endsWith(".tar")) {
                if (!validator.validateArchive(target)) {
                    Toast.makeText(this, "Archive validation failed (check arm64-v8a compatibility)", Toast.LENGTH_SHORT).show();
                    target.delete();
                    return;
                }
            }

            projectManager.addProject(name.replaceAll("\\.[^.]+$", ""), target.getAbsolutePath());
            loadProjects();
            Toast.makeText(this, "Mod uploaded successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyStream(InputStream is, File target) throws Exception {
        java.io.FileOutputStream fos = new java.io.FileOutputStream(target);
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        fos.close();
        is.close();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) result = uri.getLastPathSegment();
        return result;
    }

    private void showCreateModDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_mod, null);
        EditText etName = view.findViewById(R.id.et_mod_name);
        EditText etVersion = view.findViewById(R.id.et_mod_version);
        EditText etPlugin = view.findViewById(R.id.et_plugin_name);

        new AlertDialog.Builder(this)
                .setTitle("Create New Mod")
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String version = etVersion.getText().toString().trim();
                    String plugin = etPlugin.getText().toString().trim();
                    if (name.isEmpty() || plugin.isEmpty()) {
                        Toast.makeText(this, "Name and Plugin are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    projectManager.createProject(name, version.isEmpty() ? "1.0.0" : version, plugin);
                    loadProjects();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProjects() {
        adapter.setProjects(projectManager.getProjects());
    }

    private class ModAdapter extends RecyclerView.Adapter<ModAdapter.VH> {
        List<ModProjectManager.ModProject> list;

        void setProjects(List<ModProjectManager.ModProject> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mod_project, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            ModProjectManager.ModProject mod = list.get(p);
            h.name.setText(mod.name);
            h.version.setText(mod.version);
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ModProjectsActivity.this, ModDetailActivity.class);
                intent.putExtra("mod_id", mod.id);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView name, version;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tv_name);
                version = v.findViewById(R.id.tv_version);
            }
        }
    }
}
