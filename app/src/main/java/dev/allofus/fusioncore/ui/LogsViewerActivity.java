package dev.allofus.fusioncore.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class LogsViewerActivity extends AppCompatActivity {
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textView = new TextView(this);
        setContentView(textView);
        loadLastLines(new File(getFilesDir(), "logs/Logs.txt"), 1000);
    }

    private void loadLastLines(File file, int maxLines) {
        if (!file.exists()) {
            textView.setText("Лог-файл пуст.");
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            List<String> lines = new ArrayList<>();
            long pos = length - 1;
            StringBuilder sb = new StringBuilder();
            while (pos >= 0 && lines.size() < maxLines) {
                raf.seek(pos);
                char c = (char) raf.readByte();
                if (c == '\n') {
                    if (sb.length() > 0) {
                        lines.add(0, sb.reverse().toString());
                        sb.setLength(0);
                    }
                } else {
                    sb.append(c);
                }
                pos--;
            }
            if (sb.length() > 0) lines.add(0, sb.reverse().toString());
            StringBuilder result = new StringBuilder();
            for (String l : lines) result.append(l).append("\n");
            textView.setText(result.toString());
        } catch (Exception e) {
            textView.setText("Ошибка чтения логов: " + e.getMessage());
        }
    }
}
