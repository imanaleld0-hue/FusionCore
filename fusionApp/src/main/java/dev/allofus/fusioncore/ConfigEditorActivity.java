package dev.allofus.fusioncore;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigEditorActivity extends Activity {
    private RecyclerView recycler;
    private File configDir;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_editor);
        recycler = findViewById(R.id.config_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.config_back).setOnClickListener(v -> finish());
        File sd = Environment.getExternalStorageDirectory();
        configDir = new File(new File(sd, "FusionCore/com.innersloth.spacemafia/BepInEx/config"), "");
        refresh();
    }

    private void refresh() {
        List<File> files = new ArrayList<>();
        if (configDir.exists() && configDir.isDirectory()) {
            File[] arr = configDir.listFiles();
            if (arr != null) for (File f : arr) if (f.isFile() && f.getName().endsWith(".cfg")) files.add(f);
        }
        recycler.setAdapter(new ConfigAdapter(files));
    }

    private String readFile(File f) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
        } catch (IOException e) { return "Error: " + e.getMessage(); }
        return sb.toString();
    }

    private void saveFile(File f, String text) {
        try (FileWriter w = new FileWriter(f, false)) {
            w.write(text);
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void editFile(File f) {
        String content = readFile(f);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(f.getName());
        EditText edit = new EditText(this);
        edit.setText(content);
        ScrollView sv = new ScrollView(this);
        sv.addView(edit);
        b.setView(sv);
        b.setPositiveButton("Save", (d, w) -> saveFile(f, edit.getText().toString()));
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void deleteConfig(File f) {
        new AlertDialog.Builder(this)
            .setTitle("Delete " + f.getName() + "?")
            .setPositiveButton("Delete", (d, w) -> {
                if (f.delete()) { Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show(); refresh(); }
                else Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private class ConfigAdapter extends RecyclerView.Adapter<ConfigVH> {
        private final List<File> files;
        ConfigAdapter(List<File> files) { this.files = files; }
        @NonNull @Override public ConfigVH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new ConfigVH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_config, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ConfigVH h, int pos) {
            File f = files.get(pos);
            h.name.setText(f.getName());
            h.itemView.setOnClickListener(v -> editFile(f));
            h.itemView.setOnLongClickListener(v -> { deleteConfig(f); return true; });
        }
        @Override public int getItemCount() { return files.size(); }
    }

    private static class ConfigVH extends RecyclerView.ViewHolder {
        final TextView name;
        ConfigVH(View v) { super(v); name = v.findViewById(R.id.config_name); }
    }
}
