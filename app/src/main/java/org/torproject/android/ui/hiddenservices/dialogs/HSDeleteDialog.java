package org.torproject.android.ui.hiddenservices.dialogs;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

public class HSDeleteDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        final Context context = getContext();

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        context.getContentResolver().delete(
                                HSContentProvider.CONTENT_URI,
                                HSContentProvider.HiddenService._ID + "=" + arguments.getInt("_id"),
                                null
                        );
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // Do nothing
                        break;
                }
            }
        };

        return new AlertDialog.Builder(context)
                .setMessage(R.string.confirm_service_deletion)
                .setPositiveButton(R.string.btn_okay, dialogClickListener)
                .setNegativeButton(R.string.btn_cancel, dialogClickListener)
                .create();
    }
}
