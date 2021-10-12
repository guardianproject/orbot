package org.torproject.android.ui.v3onionservice;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.service.TorServiceConstants;

import java.io.File;

public class OnionServiceDeleteDialogFragment extends DialogFragment {
    OnionServiceDeleteDialogFragment(Bundle arguments) {
        super();
        setArguments(arguments);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_service_deletion)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> doDelete(getArguments(), getContext()))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .create();
    }

    private void doDelete(Bundle arguments, Context context) {
        context.getContentResolver().delete(OnionServiceContentProvider.CONTENT_URI, OnionServiceContentProvider.OnionService._ID + '=' + arguments.getInt(OnionServiceActivity.BUNDLE_KEY_ID), null);
        String base = context.getFilesDir().getAbsolutePath() + "/" + TorServiceConstants.ONION_SERVICES_DIR;
        DiskUtils.recursivelyDeleteDirectory(new File(base, arguments.getString(OnionServiceActivity.BUNDLE_KEY_PATH)));
    }

}
