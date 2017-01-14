package org.torproject.android.ui.hiddenservices.dialogs;


import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;

public class HSActionsDialog extends DialogFragment {
    public static final int WRITE_EXTERNAL_STORAGE_FROM_ACTION_DIALOG = 2;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_actions, null);
        final AlertDialog actionDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.hidden_services)
                .create();

        Button backup = (Button) dialog_view.findViewById(R.id.btn_hs_backup);
        backup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Context mContext = v.getContext();

                if (PermissionManager.isLollipopOrHigher()
                        && !PermissionManager.hasExternalWritePermission(mContext)) {

                    PermissionManager.requestExternalWritePermissions(
                            getActivity(), WRITE_EXTERNAL_STORAGE_FROM_ACTION_DIALOG);

                    return;
                }

                BackupUtils hsutils = new BackupUtils(mContext);
                String backupPath = hsutils.createZipBackup(Integer.parseInt(arguments.getString("port")));

                if (backupPath == null || backupPath.length() < 1) {
                    Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
                    actionDialog.dismiss();
                    return;
                }

                Toast.makeText(mContext, R.string.backup_saved_at_external_storage, Toast.LENGTH_LONG).show();

                Uri selectedUri = Uri.parse(backupPath.substring(0, backupPath.lastIndexOf("/")));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(selectedUri, "resource/folder");

                if (intent.resolveActivityInfo(mContext.getPackageManager(), 0) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(mContext, R.string.filemanager_not_available, Toast.LENGTH_LONG).show();
                }
                actionDialog.dismiss();
            }
        });

        Button copy = (Button) dialog_view.findViewById(R.id.btn_hs_clipboard);
        copy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Context mContext = v.getContext();
                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("onion", arguments.getString("onion"));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(mContext, R.string.done, Toast.LENGTH_LONG).show();
                actionDialog.dismiss();
            }
        });

        Button showAuth = (Button) dialog_view.findViewById(R.id.bt_hs_show_auth);
        showAuth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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
            }
        });

        Button delete = (Button) dialog_view.findViewById(R.id.btn_hs_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                HSDeleteDialog dialog = new HSDeleteDialog();
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager(), "HSDeleteDialog");
                actionDialog.dismiss();
            }
        });

        Button cancel = (Button) dialog_view.findViewById(R.id.btn_hs_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionDialog.dismiss();
            }
        });

        return actionDialog;
    }
}
