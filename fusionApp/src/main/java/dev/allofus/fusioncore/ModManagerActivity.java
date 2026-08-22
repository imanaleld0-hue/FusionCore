package dev.allofus.fusioncore;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModManagerActivity extends Activity {
    private RecyclerView recycler;
    private File pluginsDir;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_manager);
        recycler = findViewById(R.id.mod_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.mod_back).setOnClickListener(v -> finish());
        File sd = Environment.getExternalStorageDirectory();
        pluginsDir = new File(new File(sd, "FusionCore/com.innersloth.spacemafia/BepInEx/plugins"), "");
        refresh();
    }

    private void refresh() {
        List<ModEntry> list = new ArrayList<>();
        if (pluginsDir.exists() && pluginsDir.isDirectory()) {
            File[] arr = pluginsDir.listFiles();
            if (arr != null) {
                for (File f : arr) {
                    if (f.isFile()) {
                        String name = f.getName();
                        if (name.endsWith(".dll")) list.add(new ModEntry(name, true, f));
                        else if (name.endsWith(".dll.disabled")) list.add(new ModEntry(name.replace(".disabled", ""), false, f));
                    }
                }
            }
        }
        recycler.setAdapter(new ModAdapter(list));
    }

    private void toggleMod(ModEntry entry, boolean enabled) {
        File target = new File(pluginsDir, enabled ? entry.name : entry.name + ".disabled");
        if (!entry.file.renameTo(target)) {
            Toast.makeText(this, "Failed to toggle mod", Toast.LENGTH_SHORT).show();
        } else {
            entry.file = target;
            entry.enabled = enabled;
        }
    }

    private static class ModEntry {
        String name;
        boolean enabled;
        File file;
        ModEntry(String name, boolean enabled, File file) {
            this.name = name; this.enabled = enabled; this.file = file;
        }
    }

    private class ModAdapter extends RecyclerView.Adapter<ModVH> {
        private final List<ModEntry> list;
        ModAdapter(List<ModEntry> list) { this.list = list; }
        @NonNull @Override public ModVH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new ModVH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_mod, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ModVH h, int pos) {
            ModEntry e = list.get(pos);
            h.name.setText(e.name);
            h.sw.setChecked(e.enabled);
            h.sw.setOnCheckedChangeListener((btn, checked) -> toggleMod(e, checked));
        }
        @Override public int getItemCount() { return list.size(); }
    }

    private static class ModVH extends RecyclerView.ViewHolder {
        final TextView name;
        final Switch sw;
        ModVH(View v) { super(v); name = v.findViewById(R.id.mod_name); sw = v.findViewById(R.id.mod_switch); }
    }
}
