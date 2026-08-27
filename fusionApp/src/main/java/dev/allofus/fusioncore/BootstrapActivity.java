package dev.allofus.fusioncore;

import android.os.Bundle;
import dev.allofus.fusioncore.ui.BaseFullscreenActivity;

public class BootstrapActivity extends BaseFullscreenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);
        // Bootstrap and loading logic only. No IDE entry point here.
    }
}
