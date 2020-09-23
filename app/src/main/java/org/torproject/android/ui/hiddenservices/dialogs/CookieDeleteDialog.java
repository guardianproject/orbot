package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

public class CookieDeleteDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        final Context context = getContext();

        return new AlertDialog.Builder(context)
                .setTitle(R.string.confirm_cookie_deletion)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> doDelete(arguments, context))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .create();
    }

    private void doDelete(Bundle arguments, Context context) {
        context.getContentResolver().delete( // delete from db
                CookieContentProvider.CONTENT_URI,
                CookieContentProvider.ClientCookie._ID + "=" + arguments.getInt("_id"),
                null
        );
    }
}
