package org.torproject.android.ui.hiddenservices.dialogs;


import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

public class HSDataDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout
        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_data_dialog, null);

        // Use the Builder class for convenient dialog construction
        final AlertDialog serviceDataDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.hidden_services)
                .create();

        // Buttons action
        Button save = (Button) dialog_view.findViewById(R.id.HSDialogSave);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String serverName = ((EditText) dialog_view.findViewById(R.id.hsName)).getText().toString();
                Integer localPort = Integer.parseInt(
                        ((EditText) dialog_view.findViewById(R.id.hsLocalPort)).getText().toString()
                );
                Integer onionPort = Integer.parseInt(
                        ((EditText) dialog_view.findViewById(R.id.hsOnionPort)).getText().toString()
                );

                if (checkInput(serverName, localPort, onionPort)) {
                    saveData(serverName, localPort, onionPort);
                    serviceDataDialog.dismiss();
                }
            }
        });

        Button cancel = (Button) dialog_view.findViewById(R.id.HSDialogCancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                serviceDataDialog.cancel();
            }
        });

        return serviceDataDialog;
    }

    private boolean checkInput(String serverName, Integer local, Integer remote) {
        boolean is_ok = true;
        Integer error_msg = 0;

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

    private void saveData(String name, Integer local, Integer remote) {
        ContentValues fields = new ContentValues();
        fields.put(HSContentProvider.HiddenService.NAME, name);
        fields.put(HSContentProvider.HiddenService.PORT, local);
        fields.put(HSContentProvider.HiddenService.ONION_PORT, remote);
        fields.put(HSContentProvider.HiddenService.CREATED_BY_USER, 1);

        ContentResolver cr = getContext().getContentResolver();

        cr.insert(HSContentProvider.CONTENT_URI, fields);
    }
}
