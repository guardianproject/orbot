package org.torproject.android.v3onionservice;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.core.ClipboardUtils;
import org.torproject.android.core.DiskUtils;

public class OnionServiceActionsDialogFragment extends DialogFragment {

    private static final int REQUEST_CODE_WRITE_FILE = 343;

    OnionServiceActionsDialogFragment(Bundle arguments) {
        super();
        setArguments(arguments);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        AlertDialog ad = new AlertDialog.Builder(requireActivity())
                .setItems(new CharSequence[]{
                        getString(R.string.copy_address_to_clipboard),
                        Html.fromHtml(getString(R.string.backup_service), Html.FROM_HTML_MODE_LEGACY),
                        getString(R.string.delete_service)}, null)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setTitle(R.string.hidden_services)
                .create();

        // done this way so we can startActivityForResult on backup without the dialog vanishing
        ad.getListView().setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                assert arguments != null;
                doCopy(arguments, getContext());
            }
            else if (position == 1) {
                assert arguments != null;
                doBackup(arguments, getContext());
            }
            else if (position == 2) {
                assert getFragmentManager() != null;
                new OnionServiceDeleteDialogFragment(arguments).show(getFragmentManager(), OnionServiceDeleteDialogFragment.class.getSimpleName());
            }
            if (position != 1) dismiss();
        });
        return ad;
    }

    private void doCopy(Bundle arguments, Context context) {
        String onion = arguments.getString(OnionServiceActivity.BUNDLE_KEY_DOMAIN);
        if (onion == null)
            Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
        else
            ClipboardUtils.copyToClipboard("onion", onion, getString(R.string.done), context);
    }

    private void doBackup(Bundle arguments, Context context) {
        String filename = "onion_service" + arguments.getString(OnionServiceActivity.BUNDLE_KEY_PORT) + ".zip";
        String relativePath = arguments.getString(OnionServiceActivity.BUNDLE_KEY_PATH);
        if (arguments.getString(OnionServiceActivity.BUNDLE_KEY_DOMAIN) == null) {
            Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
            return;
        }
        Intent createFileIntent = DiskUtils.createWriteFileIntent(filename, "application/zip");
        startActivityForResult(createFileIntent, REQUEST_CODE_WRITE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_WRITE_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                attemptToWriteBackup(data.getData());
            }
        }
    }

    private void attemptToWriteBackup(Uri outputFile) {
        assert getArguments() != null;
        String relativePath = getArguments().getString(OnionServiceActivity.BUNDLE_KEY_PATH);
        V3BackupUtils v3BackupUtils = new V3BackupUtils(getContext());
        String backup = v3BackupUtils.createV3ZipBackup(relativePath, outputFile);
        Toast.makeText(getContext(), backup != null ? R.string.backup_saved_at_external_storage : R.string.error, Toast.LENGTH_LONG).show();
        dismiss();
    }

}