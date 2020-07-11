package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
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
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;

public class CookieActionsDialog extends DialogFragment {
    public static final int WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTION_DIALOG = 4;
    private AlertDialog actionDialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_cookie_actions, null);
        actionDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.client_cookies)
                .create();

        dialog_view.findViewById(R.id.btn_cookie_backup).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doBackup();
            }
        });

        dialog_view.findViewById(R.id.btn_cookie_delete).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CookieDeleteDialog dialog = new CookieDeleteDialog();
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager(), "CookieDeleteDialog");
                actionDialog.dismiss();
            }
        });

        dialog_view.findViewById(R.id.btn_cookie_cancel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                actionDialog.dismiss();
            }
        });

        return actionDialog;
    }

    public void doBackup() {
        Context mContext = getContext();
        Bundle arguments = getArguments();

        if (PermissionManager.isLollipopOrHigher()
                && !PermissionManager.hasExternalWritePermission(mContext)) {

            PermissionManager.requestExternalWritePermissions(
                    getActivity(), WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTION_DIALOG);

            return;
        }

        BackupUtils backup_utils = new BackupUtils(mContext);
        String backupPath;
        try {
            backupPath = backup_utils.createCookieBackup(
                    arguments.getString("domain"),
                    arguments.getString("auth_cookie_value"),
                    arguments.getInt("enabled")
            );
        } catch (NullPointerException npe) {
            backupPath = null;
        }

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

}
