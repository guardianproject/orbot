package org.torproject.android.ui.hs.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import org.torproject.android.R;

public class HSActionsDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_actions, null);
        final AlertDialog actionDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.hidden_services)
                .create();

        Button save = (Button) dialog_view.findViewById(R.id.btn_hs_backup);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionDialog.dismiss();
            }
        });

        Button cancel = (Button) dialog_view.findViewById(R.id.btn_hs_clipboard);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionDialog.dismiss();
            }
        });

        Button delete = (Button) dialog_view.findViewById(R.id.btn_hs_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionDialog.dismiss();
            }
        });

        Button btn_cancel = (Button) dialog_view.findViewById(R.id.btn_hs_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionDialog.dismiss();
            }
        });

        return actionDialog;
    }
}
