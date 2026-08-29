package dev.allofus.fusioncore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/** Manifest-declared placeholder for dynamically loaded Unity activities. */
public class StubActivity extends Activity {
    private static final String TAG = "StubActivity";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent current = getIntent();

        if (current != null
                && current.getBooleanExtra(
                    InstrumentationHooks.EXTRA_IS_DYNAMIC_ACTIVITY, false)) {

            Intent original = current.getParcelableExtra(
                    InstrumentationHooks.EXTRA_ORIGINAL_INTENT);

            if (original != null && original.getComponent() != null) {
                Log.d(TAG, "Dynamic activity placeholder: "
                        + original.getComponent().getClassName());
            } else {
                Log.e(TAG, "Dynamic activity has no valid original intent");
            }
        }
    }
}