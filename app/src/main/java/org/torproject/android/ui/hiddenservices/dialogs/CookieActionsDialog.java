package org.torproject.android.ui.hiddenservices.dialogs;


import android.app.Dialog;
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

public class CookieActionsDialog extends DialogFragment {
    public static final int WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTION_DIALOG = 4;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_cookie_actions, null);
        final AlertDialog actionDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.client_cookies)
                .create();

        Button backup = (Button) dialog_view.findViewById(R.id.btn_cookie_backup);
        backup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Context mContext = v.getContext();

                if (PermissionManager.isLollipopOrHigher()
                        && !PermissionManager.hasExternalWritePermission(mContext)) {

                    PermissionManager.requestExternalWritePermissions(
                            getActivity(), WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTION_DIALOG);

                    return;
                }

                BackupUtils backup_utils = new BackupUtils(mContext);
                String backupPath = backup_utils.createCookieBackup(
                        arguments.getString("domain"),
                        arguments.getString("auth_cookie_value"),
                        arguments.getInt("enabled")
                );

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

        Button delete = (Button) dialog_view.findViewById(R.id.btn_cookie_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CookieDeleteDialog dialog = new CookieDeleteDialog();
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager(), "CookieDeleteDialog");
                actionDialog.dismiss();
            }
        });

        Button cancel = (Button) dialog_view.findViewById(R.id.btn_cookie_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionDialog.dismiss();
            }
        });

        return actionDialog;
    }
}
