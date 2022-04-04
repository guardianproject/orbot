package org.torproject.android.ui.v3onionservice;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;

public class OnionServiceCreateDialogFragment extends DialogFragment {

    private EditText etServer, etLocalPort, etOnionPort;
    private TextWatcher inputValidator;

    @Override
    public void onStart() {
        super.onStart();
        inputValidator.afterTextChanged(null); // initially disable positive button
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_data_dialog, null);
        etServer = dialogView.findViewById(R.id.hsName);
        etLocalPort = dialogView.findViewById(R.id.hsLocalPort);
        etOnionPort = dialogView.findViewById(R.id.hsOnionPort);

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.hidden_services)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.save, (dialog, which) -> doSave(getContext()))
                .setView(dialogView)
                .create();


        inputValidator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { //no-op
            }

            @Override
            public void afterTextChanged(Editable s) {
                Button btn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                try {
                    int localPort = Integer.parseInt(etLocalPort.getText().toString());
                    int onionPort = Integer.parseInt(etOnionPort.getText().toString());
                    btn.setEnabled(checkInput(localPort, onionPort));
                } catch (NumberFormatException nfe) {
                    btn.setEnabled(false);
                }
            }
        };

        etServer.addTextChangedListener(inputValidator);
        etLocalPort.addTextChangedListener(inputValidator);
        etOnionPort.addTextChangedListener(inputValidator);
        return alertDialog;
    }

    private boolean checkInput(int local, int remote) {
        if ((local < 1 || local > 65535) || (remote < 1 || remote > 65535)) return false;
        return !TextUtils.isEmpty(etServer.getText().toString().trim());
    }

    private void doSave(Context context) {
        String serverName = etServer.getText().toString().trim();
        int localPort = Integer.parseInt(etLocalPort.getText().toString());
        int onionPort = Integer.parseInt(etOnionPort.getText().toString());
        ContentValues fields = new ContentValues();
        fields.put(OnionServiceContentProvider.OnionService.NAME, serverName);
        fields.put(OnionServiceContentProvider.OnionService.PORT, localPort);
        fields.put(OnionServiceContentProvider.OnionService.ONION_PORT, onionPort);
        fields.put(OnionServiceContentProvider.OnionService.CREATED_BY_USER, 1);
        ContentResolver cr = context.getContentResolver();
        cr.insert(OnionServiceContentProvider.CONTENT_URI, fields);
        Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_SHORT).show();
        ((OnionServiceActivity) getActivity()).showBatteryOptimizationsMessageIfAppropriate();
    }

}