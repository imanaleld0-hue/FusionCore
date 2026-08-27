package dev.allofus.fusioncore.ide;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MiniIDEActivity extends Activity {
    private EditText codeEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        codeEditor = new EditText(this);
        codeEditor.setBackgroundColor(0xFF1E1E1E);
        codeEditor.setTextColor(0xFFD4D4D4);
        codeEditor.setTextSize(14f);
        setContentView(codeEditor);

        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                SyntaxHighlighter.highlight(s);
            }
        });
    }
}
