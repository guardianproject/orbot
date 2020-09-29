package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

import java.io.File;

public class HSDeleteDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        final Context context = getContext();
        return new AlertDialog.Builder(context)
                .setTitle(R.string.confirm_service_deletion)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> doDelete(arguments, context))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .create();
    }

    private void doDelete(Bundle arguments, Context context) {
        context.getContentResolver().delete( // Delete from db
                HSContentProvider.CONTENT_URI,
                HSContentProvider.HiddenService._ID + "=" + arguments.getInt("_id"),
                null);

        // Delete from internal storage
        String base = context.getFilesDir().getAbsolutePath() + "/" + TorServiceConstants.HIDDEN_SERVICES_DIR;
        File dir = new File(base, "hs" + arguments.getString("port"));

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                new File(dir, aChildren).delete();
            }
            dir.delete();
        }
    }
}
