package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.widget.Toast;

import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;

public class HSActionsDialog extends DialogFragment {
    public static final int WRITE_EXTERNAL_STORAGE_FROM_ACTION_DIALOG = 2;
    private AlertDialog actionDialog;
    private int port;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        port = Integer.parseInt(arguments.getString("port"));
        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_actions, null);
        actionDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.hidden_services)
                .create();

        dialog_view.findViewById(R.id.btn_hs_backup).setOnClickListener(v -> doBackup());

        dialog_view.findViewById(R.id.btn_hs_clipboard).setOnClickListener(v -> {
            Context mContext = v.getContext();
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("onion", arguments.getString("onion"));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(mContext, R.string.done, Toast.LENGTH_LONG).show();
            actionDialog.dismiss();
        });

        dialog_view.findViewById(R.id.bt_hs_show_auth).setOnClickListener(v -> {
            String auth_cookie_value = arguments.getString("auth_cookie_value");

            if (arguments.getInt("auth_cookie") == 1) {
                if (auth_cookie_value == null || auth_cookie_value.length() < 1) {
                    Toast.makeText(
                            v.getContext(), R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG
                    ).show();
                } else {
                    HSCookieDialog dialog = new HSCookieDialog();
                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager(), "HSCookieDialog");
                }
            } else {
                Toast.makeText(
                        v.getContext(), R.string.auth_cookie_was_not_configured, Toast.LENGTH_LONG
                ).show();
            }

            actionDialog.dismiss();
        });

        dialog_view.findViewById(R.id.btn_hs_delete).setOnClickListener(v -> {
            HSDeleteDialog dialog = new HSDeleteDialog();
            dialog.setArguments(arguments);
            dialog.show(getFragmentManager(), "HSDeleteDialog");
            actionDialog.dismiss();
        });

        dialog_view.findViewById(R.id.btn_hs_cancel).setOnClickListener(v -> actionDialog.dismiss());

        return actionDialog;
    }

    public void doBackup() {
        Intent createFile = DiskUtils.createWriteFileIntent("hs" + port + ".zip", "application/zip");
        startActivityForResult(createFile, REQUEST_CODE_WRITE_FILE);
    }

    public void doBackup1() {
        if (PermissionManager.isLollipopOrHigher()
                && !PermissionManager.hasExternalWritePermission(getActivity())) {

            PermissionManager.requestExternalWritePermissions(
                    getActivity(), WRITE_EXTERNAL_STORAGE_FROM_ACTION_DIALOG);
        }
    }

    private static final int REQUEST_CODE_WRITE_FILE = 123;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_WRITE_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri file = data.getData();
                BackupUtils backupUtils = new BackupUtils(getContext());
                String backup = backupUtils.createZipBackup(port, file);
                if (backup != null) {
                    Toast.makeText(getContext(), R.string.backup_saved_at_external_storage, Toast.LENGTH_LONG).show();
                } else{
                    Toast.makeText(getContext(), R.string.error, Toast.LENGTH_LONG).show();
                }
                actionDialog.dismiss();
            }
        }
    }
}
