package org.torproject.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;

public class MoatErrorDialogFragment extends DialogFragment {

    public static final String TAG = MoatErrorDialogFragment.class.getSimpleName();
    private static final String BUNDLE_KEY_MSG = "msg";

    public static MoatErrorDialogFragment newInstance(String message) {
        MoatErrorDialogFragment fragment = new MoatErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_MSG, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.error)
                .setMessage(getArguments().getString(BUNDLE_KEY_MSG))
                .setNegativeButton(android.R.string.ok, null)
                .create();
    }
}
