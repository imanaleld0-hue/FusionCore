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
import android.widget.ImageButton;
import android.widget.ScrollView;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogsActivity extends Activity {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private TextView emptyView;
    private File logDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        ImageButton backButton = findViewById(R.id.logs_action_back);
        backButton.setOnClickListener(v -> finish());
        TextView clearButton = findViewById(R.id.logs_action_clear);
        clearButton.setOnClickListener(v -> clearAllLogs());
        recyclerView = findViewById(R.id.logs_recycler);
        emptyView = findViewById(R.id.logs_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        logDir = new File(getExternalFilesDir(null), "logs");
        refreshList();
    }

    private void refreshList() {
        List<LogEntry> entries = new ArrayList<>();
        if (logDir.exists() && logDir.isDirectory()) {
            File[] files = logDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) entries.add(new LogEntry(f.getName(), f.length(), f.lastModified(), f));
                }
            }
        }
        Collections.sort(entries, (a, b) -> Long.compare(b.modified, a.modified));
        adapter = new LogAdapter(entries);
        recyclerView.setAdapter(adapter);
        emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearAllLogs() {
        if (logDir.exists() && logDir.isDirectory()) {
            File[] files = logDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
        refreshList();
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
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        } catch (IOException e) {
            return "Failed to read: " + e.getMessage();
        }
        return sb.toString();
    }

    private void showLogContent(File file) {
        String content = readLog(file);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(file.getName());
        TextView textView = new TextView(this);
        textView.setText(content);
        textView.setPadding(24, 24, 24, 24);
        textView.setTextIsSelectable(true);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        builder.setNeutralButton("Copy", (d, w) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("log", content));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"KB", "MB", "GB"};
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024, exp), units[exp - 1]);
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private class LogEntry {
        final String name;
        final long size;
        final long modified;
        final File file;
        LogEntry(String name, long size, long modified, File file) {
            this.name = name;
            this.size = size;
            this.modified = modified;
            this.file = file;
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {
        private final List<LogEntry> entries;
        LogAdapter(List<LogEntry> entries) { this.entries = entries; }
        @NonNull @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            LogEntry entry = entries.get(position);
            holder.name.setText(entry.name);
            holder.size.setText(formatSize(entry.size));
            holder.date.setText(formatDate(entry.modified));
            holder.itemView.setOnClickListener(v -> showLogContent(entry.file));
            holder.share.setOnClickListener(v -> shareLog(entry.file));
        }
        @Override public int getItemCount() { return entries.size(); }
    }

    private static class LogViewHolder extends RecyclerView.ViewHolder {
        final TextView name, size, date;
        final View share;
        LogViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.log_name);
            size = itemView.findViewById(R.id.log_size);
            date = itemView.findViewById(R.id.log_date);
            share = itemView.findViewById(R.id.log_share);
        }
    }
}
