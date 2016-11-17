package org.torproject.android.ui.hs.dialogs;


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
import org.torproject.android.ui.hs.providers.HSContentProvider;

public class HSDataDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout
        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.dialog_hs_data, null);

        // Use the Builder class for convenient dialog construction
        final AlertDialog serverDataDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.hs_dialog_title)
                .create();

        // Buttons action
        Button save = (Button) dialog_view.findViewById(R.id.HSDialogSave);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String serverName = ((EditText) dialog_view.findViewById(R.id.hsName)).getText().toString();
                Integer serverPort = Integer.parseInt(
                        ((EditText) dialog_view.findViewById(R.id.serverPort)).getText().toString()
                );

                if (checkInput(serverPort)) {
                    saveData(serverName, serverPort);
                    serverDataDialog.dismiss();
                }
            }
        });

        Button cancel = (Button) dialog_view.findViewById(R.id.HSDialogCancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                serverDataDialog.cancel();
            }
        });

        return serverDataDialog;
    }

    private boolean checkInput(Integer port) {
        boolean is_ok = true;
        Integer error_msg = 0;

        if (port <= 1 || port > 65535) {
            error_msg = R.string.invalid_port;
            is_ok = false;
        }

        if (!is_ok) {
            Toast.makeText(getContext(), error_msg, Toast.LENGTH_SHORT).show();
        }

        return is_ok;
    }

    private void saveData(String name, Integer port) {
        ContentValues fields = new ContentValues();
        fields.put("name", name);
        fields.put("port", port);

        ContentResolver cr = getContext().getContentResolver();

        cr.insert(HSContentProvider.CONTENT_URI, fields);
    }
}
