package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogsActivity extends Activity {
    private RecyclerView recycler;
    private LogAdapter adapter;
    private TextView emptyView;
    private File logDir;
    private String currentFilter = "All";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        ImageButton back = findViewById(R.id.logs_action_back);
        TextView clear = findViewById(R.id.logs_action_clear);
        Spinner filter = findViewById(R.id.logs_filter);
        recycler = findViewById(R.id.logs_recycler);
        emptyView = findViewById(R.id.logs_empty);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        back.setOnClickListener(v -> finish());
        clear.setOnClickListener(v -> { clearAllLogs(); refreshList(); });
        logDir = new File(getExternalFilesDir(null), "logs");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"All", "Java", "Native", "BepInEx", "Hooking"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filter.setAdapter(spinnerAdapter);
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentFilter = (String) p.getItemAtPosition(pos);
                refreshList();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        refreshList();
    }

    private void refreshList() {
        List<LogEntry> entries = new ArrayList<>();
        if (logDir.exists() && logDir.isDirectory()) {
            File[] files = logDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.isFile()) continue;
                    String name = f.getName();
                    if (!name.endsWith(".log")) continue;
                    if (!currentFilter.equals("All") && !name.contains(currentFilter)) continue;
                    entries.add(new LogEntry(name, f.length(), f.lastModified(), f));
                }
            }
        }
        Collections.sort(entries, (a, b) -> Long.compare(b.modified, a.modified));
        adapter = new LogAdapter(entries);
        recycler.setAdapter(adapter);
        emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearAllLogs() {
        if (logDir.exists() && logDir.isDirectory()) {
            File[] files = logDir.listFiles();
            if (files != null) for (File f : files) f.delete();
        }
        Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
    }

    private void shareLog(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share log"));
    }

    private String readLog(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (isMiuiNoise(line)) continue;
                sb.append(line).append("\n");
            }
        } catch (IOException e) { return "Error: " + e.getMessage(); }
        return sb.toString();
    }

    private boolean isMiuiNoise(String line) {
        if (line == null) return true;
        String l = line.toLowerCase();
        return l.contains("miui") || l.contains("wmdebug") || l.contains("forcedark") || l.contains("handwriting")
                || l.contains("blastbuffer") || l.contains("userscenedetector") || l.contains("vri[")
                || l.contains("inputeventreceiver") || l.contains("inputtransport");
    }

    private void showLogContent(File file) {
        String content = readLog(file);
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle(file.getName());
        TextView tv = new TextView(this);
        tv.setText(content);
        tv.setPadding(24, 24, 24, 24);
        tv.setTextIsSelectable(true);
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(tv);
        b.setView(sv);
        b.setPositiveButton("Close", null);
        b.setNeutralButton("Copy", (d, w) -> {
            ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("log", content));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
        b.show();
    }

    private String fmtSize(long b) {
        if (b < 1024) return b + " B";
        int e = (int) (Math.log(b) / Math.log(1024));
        return String.format(Locale.US, "%.1f %s", b / Math.pow(1024, e), new String[]{"KB","MB","GB"}[e-1]);
    }

    private String fmtDate(long ts) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(ts));
    }

    private static class LogEntry {
        final String name; final long size, modified; final File file;
        LogEntry(String n, long s, long m, File f) { name=n; size=s; modified=m; file=f; }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogVH> {
        private final List<LogEntry> list;
        LogAdapter(List<LogEntry> list) { this.list = list; }
        @NonNull @Override public LogVH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new LogVH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
        }
        @Override public void onBindViewHolder(@NonNull LogVH h, int pos) {
            LogEntry e = list.get(pos);
            h.name.setText(e.name);
            h.size.setText(fmtSize(e.size));
            h.date.setText(fmtDate(e.modified));
            h.itemView.setOnClickListener(v -> showLogContent(e.file));
            h.share.setOnClickListener(v -> shareLog(e.file));
        }
        @Override public int getItemCount() { return list.size(); }
    }

    private static class LogVH extends RecyclerView.ViewHolder {
        final TextView name, size, date;
        final View share;
        LogVH(View v) { super(v); name=v.findViewById(R.id.log_name); size=v.findViewById(R.id.log_size); date=v.findViewById(R.id.log_date); share=v.findViewById(R.id.log_share); }
    }
}
