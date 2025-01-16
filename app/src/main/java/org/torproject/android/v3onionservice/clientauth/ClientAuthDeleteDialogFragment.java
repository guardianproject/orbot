package org.torproject.android.v3onionservice.clientauth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;

public class ClientAuthDeleteDialogFragment extends DialogFragment {

    public ClientAuthDeleteDialogFragment() {}
    public ClientAuthDeleteDialogFragment(Bundle args) {
        super();
        setArguments(args);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.v3_delete_client_authorization)
                .setPositiveButton(R.string.v3_delete_client_authorization_confirm, (dialog, which) -> doDelete())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
    }

    private void doDelete() {
        assert getArguments() != null;
        int id = getArguments().getInt(ClientAuthActivity.BUNDLE_KEY_ID);
        requireContext().getContentResolver().delete(ClientAuthContentProvider.CONTENT_URI, ClientAuthContentProvider.V3ClientAuth._ID + "=" + id, null);
    }

}
