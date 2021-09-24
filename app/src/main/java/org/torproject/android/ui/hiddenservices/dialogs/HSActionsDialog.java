package org.torproject.android.ui.hiddenservices.dialogs;

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
import org.torproject.android.ui.hiddenservices.HiddenServicesActivity;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;

import java.io.File;

public class HSActionsDialog extends DialogFragment {
    private static final int REQUEST_CODE_WRITE_FILE = 123;
    private int port;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        port = Integer.parseInt(arguments.getString(HiddenServicesActivity.BUNDLE_KEY_PORT));
        AlertDialog ad = new AlertDialog.Builder(getActivity())
                .setItems(new CharSequence[]{
                        getString(R.string.copy_address_to_clipboard),
                        getString(R.string.show_auth_cookie),
                        Html.fromHtml(getString(R.string.backup_service)),
                        getString(R.string.delete_service)}, null)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setTitle(R.string.hidden_services)
                .create();

        // done this way so we can startActivityForResult on backup without the dialog vanishing
        ad.getListView().setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) doCopy(arguments, getContext());
            else if (position == 1) doShowAuthCookie(arguments, getContext());
            else if (position == 2) doBackup(arguments, getContext());
            else if (position == 3) doDelete(arguments);
            if (position != 2) dismiss();
        });

        return ad;
    }

    private void doDelete(Bundle arguments) {
        HSDeleteDialog dialog = new HSDeleteDialog();
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager(), "HSDeleteDialog");
    }

    private void doShowAuthCookie(Bundle arguments, Context context) {
        String auth_cookie_value = arguments.getString(HiddenServicesActivity.BUNDLE_KEY_AUTH_COOKIE_VALUE);

        if (arguments.getInt(HiddenServicesActivity.BUNDLE_KEY_AUTH_COOKIE) == 1) {
            if (auth_cookie_value == null || auth_cookie_value.length() < 1) {
                Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
            } else {
                HSCookieDialog dialog = new HSCookieDialog();
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager(), "HSCookieDialog");
            }
        } else {
            Toast.makeText(context, R.string.auth_cookie_was_not_configured, Toast.LENGTH_LONG).show();
        }
    }

    private void doCopy(Bundle arguments, Context context) {
        String onion = arguments.getString(HiddenServicesActivity.BUNDLE_KEY_ONION);
        if (onion == null)
            Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
        else
            ClipboardUtils.copyToClipboard("onion", onion, getString(R.string.done), context);
    }

    private void doBackup(Bundle arguments, Context context) {
        String filename = "hs" + port + ".zip";
        String onion = arguments.getString(HiddenServicesActivity.BUNDLE_KEY_ONION);
        if (onion == null) {
            Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
            return;
        }
        if (DiskUtils.supportsStorageAccessFramework()) {
            Intent createFile = DiskUtils.createWriteFileIntent(filename, "application/zip");
            startActivityForResult(createFile, REQUEST_CODE_WRITE_FILE);
        } else { // API 16, 17, 18
            attemptToWriteBackup(Uri.fromFile(new File(DiskUtils.getOrCreateLegacyBackupDir(getString(R.string.app_name)), filename)));
        }
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
        String path = getArguments().getString(HiddenServicesActivity.BUNDLE_KEY_PATH);
        BackupUtils backupUtils = new BackupUtils(getContext());
        String backup = backupUtils.createV2ZipBackup(port, path, outputFile);
        Toast.makeText(getContext(), backup != null ? R.string.backup_saved_at_external_storage : R.string.error, Toast.LENGTH_LONG).show();
        dismiss();
    }

}
