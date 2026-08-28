package dev.allofus.fusioncore.ui;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;
import dev.allofus.fusioncore.R;
import dev.allofus.fusioncore.ide.SyntaxHighlighter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MiniIDEActivity extends BaseFullscreenActivity {

    private EditText editor;
    private TextView tvReadOnly;
    private MaterialButton btnPrev;
    private MaterialButton btnNext;
    private String filePath;
    private boolean readOnly;
    private static final long MAX_SIZE = 1024 * 1024; // 1 MB threshold for chunking
    private StringBuilder fullContent;
    private int currentChunk = 0;
    private static final int CHUNK_SIZE = 50000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mini_ide);

        editor = findViewById(R.id.editor);
        tvReadOnly = findViewById(R.id.tv_read_only);
        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        filePath = getIntent().getStringExtra("file_path");
        readOnly = getIntent().getBooleanExtra("read_only", false);

        if (readOnly) {
            editor.setEnabled(false);
            tvReadOnly.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
        } else {
            tvReadOnly.setVisibility(View.GONE);
            btnSave.setOnClickListener(v -> saveFile());
        }

        if (filePath != null) {
            loadFileLazy(new File(filePath));
        }

        btnPrev.setOnClickListener(v -> showChunk(currentChunk - 1));
        btnNext.setOnClickListener(v -> showChunk(currentChunk + 1));
    }

    private void loadFileLazy(File file) {
        try {
            long size = file.length();
            BufferedReader br = new BufferedReader(new FileReader(file));
            fullContent = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = br.read(buf)) > 0) {
                fullContent.append(buf, 0, n);
            }
            br.close();

            if (size > MAX_SIZE) {
                showChunk(0);
            } else {
                editor.setText(highlight(fullContent.toString()));
                btnPrev.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showChunk(int chunk) {
        if (fullContent == null) return;
        int maxChunk = Math.max(0, (fullContent.length() - 1) / CHUNK_SIZE);
        if (chunk < 0) chunk = 0;
        if (chunk > maxChunk) chunk = maxChunk;
        currentChunk = chunk;
        int start = chunk * CHUNK_SIZE;
        int end = Math.min(start + CHUNK_SIZE, fullContent.length());
        String text = fullContent.substring(start, end);
        editor.setText(highlight(text));
    }

    private Spannable highlight(String text) {
        if (filePath != null && filePath.endsWith(".cs")) {
            return SyntaxHighlighter.highlightCs(text);
        }
        return new SpannableStringBuilder(text);
    }

    private void saveFile() {
        if (filePath == null || readOnly) return;
        try {
            FileWriter fw = new FileWriter(filePath);
            if (fullContent != null) {
                fw.write(fullContent.toString());
            } else {
                fw.write(editor.getText().toString());
            }
            fw.close();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
