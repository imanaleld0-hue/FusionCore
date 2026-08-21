package dev.allofus.fusioncore.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import dev.allofus.fusioncore.R;

public class AuthBottomSheet extends BottomSheetDialogFragment {

    private EditText etAuthInput;
    private Button btnAuthSubmit;

    public static AuthBottomSheet newInstance() {
        return new AuthBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_auth_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etAuthInput = view.findViewById(R.id.etAuthInput);
        btnAuthSubmit = view.findViewById(R.id.btnAuthSubmit);

        btnAuthSubmit.setOnClickListener(v -> {
            String input = etAuthInput.getText() != null ? etAuthInput.getText().toString() : "";
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(requireContext(), R.string.auth_error, Toast.LENGTH_SHORT).show();
                return;
            }

            AuthIntentParser.AuthResult result = AuthIntentParser.parseRawText(input);
            if (result != null && result.isValid()) {
                AuthManager.getInstance(requireContext()).setAuth(result.token, result.mergeId, result.store, 0);
                Toast.makeText(requireContext(), R.string.auth_success, Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                Toast.makeText(requireContext(), R.string.auth_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
