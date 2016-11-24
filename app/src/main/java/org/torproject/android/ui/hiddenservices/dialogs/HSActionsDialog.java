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
import org.torproject.android.ui.hiddenservices.storage.PermissionManager;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

public class HSActionsDialog extends DialogFragment {
    public final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

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

                if (PermissionManager.usesRuntimePermissions()
                        && !PermissionManager.hasExternalWritePermission(mContext)) {
                    PermissionManager.requestPermissions(getActivity());
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

        Button delete = (Button) dialog_view.findViewById(R.id.btn_hs_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.getContext().getContentResolver().delete(
                        HSContentProvider.CONTENT_URI, "port=" + arguments.getString("port"), null
                );
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
