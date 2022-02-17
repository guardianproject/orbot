package org.torproject.android.ui.v3onionservice.clientauth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;

public class ClientAuthCreateDialogFragment extends DialogFragment {

    private EditText etOnionUrl, etKeyHash;
    private TextWatcher inputValidator;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_v3_client_auth, null);
        final AlertDialog ad = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setTitle(R.string.v3_client_auth_activity_title)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.save, (dialog, which) -> doSave(getContext()))
                .create();


        etOnionUrl = dialogView.findViewById(R.id.cookie_onion);
        etKeyHash = dialogView.findViewById(R.id.cookie_value);

        inputValidator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(checkInput());
            }
        };

        etOnionUrl.addTextChangedListener(inputValidator);
        etKeyHash.addTextChangedListener(inputValidator);

        return ad;
    }

    private void doSave(Context context) {
        String onionName = sanitizeOnionDomainTextField();
        String hash = etKeyHash.getText().toString();
        ContentValues fields = new ContentValues();
        fields.put(ClientAuthContentProvider.V3ClientAuth.DOMAIN, onionName);
        fields.put(ClientAuthContentProvider.V3ClientAuth.HASH, hash);
        ContentResolver cr = context.getContentResolver();
        cr.insert(ClientAuthContentProvider.CONTENT_URI, fields);
        Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
    }

    private String sanitizeOnionDomainTextField() {
        String domain = ".onion";
        String onion = etOnionUrl.getText().toString();
        if (onion.endsWith(domain))
            return onion.substring(0, onion.indexOf(domain));
        return onion;
    }

    @Override
    public void onStart() {
        super.onStart();
        inputValidator.afterTextChanged(null);
    }

    private boolean checkInput() {
        String onion = sanitizeOnionDomainTextField();
        if (!onion.matches("([a-z0-9]{56})")) return false;
        String hash = etKeyHash.getText().toString();
        return hash.matches("([A-Z2-7]{52})");
    }

}
