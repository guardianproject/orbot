package org.torproject.android.ui.hiddenservices.dialogs;

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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

public class HSDataDialog extends DialogFragment {

    private EditText etServer, etLocalPort, etOnionPort;
    private TextWatcher inputValidator;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_data_dialog, null);
        etServer = dialogView.findViewById(R.id.hsName);
        etLocalPort = dialogView.findViewById(R.id.hsLocalPort);
        etOnionPort = dialogView.findViewById(R.id.hsOnionPort);
        AlertDialog ad = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.hidden_services)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.save, (dialog, which) -> doSave(dialogView, getContext()))
                .setView(dialogView)
                .create();

        inputValidator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Button btn = ad.getButton(AlertDialog.BUTTON_POSITIVE);
                try {
                    int localPort = Integer.parseInt(etLocalPort.getText().toString());
                    int onionPort = Integer.parseInt(etOnionPort.getText().toString());
                    btn.setEnabled(checkInput(localPort, onionPort));
                } catch (NumberFormatException nfe) {
                    btn.setEnabled(false);
                }
            }
        };

        etOnionPort.addTextChangedListener(inputValidator);
        etLocalPort.addTextChangedListener(inputValidator);
        etServer.addTextChangedListener(inputValidator);

        return ad;
    }

    @Override
    public void onStart() {
        super.onStart();
        inputValidator.afterTextChanged(null); // initially disable positive button
    }

    private void doSave(View dialogView, Context context) {
        String serverName = etServer.getText().toString().trim();
        int localPort = Integer.parseInt(etLocalPort.getText().toString());
        int onionPort = Integer.parseInt(etOnionPort.getText().toString());
        boolean authCookie = ((CheckBox) dialogView.findViewById(R.id.hsAuth)).isChecked();
        saveData(serverName, localPort, onionPort, authCookie);
        Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
    }

    private boolean checkInput(int local, int remote) {
        if ((local < 1 || local > 65535) || (remote < 1 || remote > 65535)) return false;
        return !TextUtils.isEmpty(etServer.getText().toString().trim());
    }

    private void saveData(String name, int local, int remote, boolean authCookie) {
        ContentValues fields = new ContentValues();
        fields.put(HSContentProvider.HiddenService.NAME, name);
        fields.put(HSContentProvider.HiddenService.PORT, local);
        fields.put(HSContentProvider.HiddenService.ONION_PORT, remote);
        fields.put(HSContentProvider.HiddenService.AUTH_COOKIE, authCookie);
        fields.put(HSContentProvider.HiddenService.CREATED_BY_USER, 1);

        ContentResolver cr = getContext().getContentResolver();

        cr.insert(HSContentProvider.CONTENT_URI, fields);
    }
}
