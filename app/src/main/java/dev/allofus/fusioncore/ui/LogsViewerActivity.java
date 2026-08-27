package dev.allofus.fusioncore.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.RandomAccessFile;

// Lazy Loading для предотвращения OOM при огромных логах.
public class LogsViewerActivity extends AppCompatActivity {
    private TextView logTextView;
    private static final int CHUNK_LINES = 1500;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Предполагается, что logTextView будет инициализирован из layout
        loadLogTail();
    }

    private void loadLogTail() {
        File logFile = new File(getFilesDir(), "logs/Logs.txt");
        if (!logFile.exists()) return;
        
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long length = raf.length();
            if (length == 0) return;
            
            long pos = length - 1;
            int lines = 0;
            
            while (pos >= 0 && lines < CHUNK_LINES) {
                raf.seek(pos);
                if ((char) raf.readByte() == '\n') lines++;
                pos--;
            }
            raf.seek(pos + 1);
            byte[] bytes = new byte[(int)(length - (pos + 1))];
            raf.read(bytes);
            
            // logTextView.setText(new String(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
