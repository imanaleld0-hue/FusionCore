package dev.allofus.fusioncore;

import androidx.fragment.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import dev.allofus.fusioncore.auth.AuthIntentParser;
import dev.allofus.fusioncore.auth.AuthManager;

public class AuthBottomSheet extends DialogFragment {
    public static AuthBottomSheet newInstance() {
        return new AuthBottomSheet();
    }


    private Runnable onAuthResult;
    public void setOnAuthResult(Runnable r) { this.onAuthResult = r; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_auth, container, false);
        EditText input = v.findViewById(R.id.auth_input);
        v.findViewById(R.id.auth_paste).setOnClickListener(btn -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                requireActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
                if (t != null) input.setText(t.toString());
            }
        });
        v.findViewById(R.id.auth_submit).setOnClickListener(btn -> {
            String text = input.getText().toString().trim();
            AuthIntentParser.AuthResult d = AuthIntentParser.parseRawText(text);
            if (d != null && d.isValid()) {
            AuthManager.getInstance(requireContext()).setAuth(
            d.token,
            d.mergeId,
            d.store,
            86400000L
            );

            Toast.makeText(getActivity(), "Authorization saved", Toast.LENGTH_SHORT).show();
            dismiss();
   
            if (onAuthResult != null) {
            onAuthResult.run();
}
}
            } else Toast.makeText(getActivity(), "Invalid link", Toast.LENGTH_LONG).show();
        });
        v.findViewById(R.id.auth_cancel).setOnClickListener(btn -> dismiss());
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null)
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
