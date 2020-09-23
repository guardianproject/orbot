package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

public class HSDataDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_data_dialog, null);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.hidden_services)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.save, (dialog, which) -> doSave(dialog_view, getContext()))
                .setView(dialog_view)
                .create();
    }

    private void doSave(View dialogView, Context context) {
        String serverName = ((EditText) dialogView.findViewById(R.id.hsName)).getText().toString();
        int localPort, onionPort;
        try {
            localPort = Integer.parseInt(((EditText) dialogView.findViewById(R.id.hsLocalPort)).getText().toString());
            onionPort = Integer.parseInt(((EditText) dialogView.findViewById(R.id.hsOnionPort)).getText().toString());
        } catch (NumberFormatException nfe) {
            Toast.makeText(context, R.string.fields_can_t_be_empty, Toast.LENGTH_LONG).show();
            return;
        }
        boolean authCookie = ((CheckBox) dialogView.findViewById(R.id.hsAuth)).isChecked();

        if (checkInput(serverName, localPort, onionPort)) {
            saveData(serverName, localPort, onionPort, authCookie);
            Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkInput(String serverName, int local, int remote) {
        boolean is_ok = true;
        int error_msg = 0;

        if ((local < 1 || local > 65535) || (remote < 1 || remote > 65535)) {
            error_msg = R.string.invalid_port;
            is_ok = false;
        }

        if (serverName == null || serverName.length() < 1) {
            error_msg = R.string.name_can_t_be_empty;
            is_ok = false;
        }

        if (!is_ok) {
            Toast.makeText(getContext(), error_msg, Toast.LENGTH_SHORT).show();
        }

        return is_ok;
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
