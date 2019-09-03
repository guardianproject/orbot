package org.torproject.android.mini.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.torproject.android.R;
import org.torproject.android.mini.ui.hiddenservices.adapters.BackupAdapter;
import org.torproject.android.mini.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.mini.ui.hiddenservices.storage.ExternalStorage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectCookieBackupDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder cookieBackupDialog = new AlertDialog.Builder(getActivity());

        cookieBackupDialog.setTitle(R.string.restore_backup);

        File backupDir = ExternalStorage.getOrCreateBackupDir();
        File[] files = null;

        try {
            files = backupDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".json");
                }
            });
        } catch (NullPointerException e) {
            // Silent block
        }

        if (files == null || files.length < 1) {
            cookieBackupDialog.setMessage(R.string.create_a_backup_first);
            cookieBackupDialog.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

            return cookieBackupDialog.create();
        }

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_backups_list, null);

        cookieBackupDialog.setView(dialog_view);
        cookieBackupDialog.setPositiveButton(R.string.btn_okay, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        ListView backups = (ListView) dialog_view.findViewById(R.id.listview_hs_backups);

        List<File> json_backups = new ArrayList<>();
        Collections.addAll(json_backups, files);

        backups.setAdapter(new BackupAdapter(getContext(), R.layout.layout_hs_backups_list_item, json_backups));
        backups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BackupUtils backupUtils = new BackupUtils(view.getContext().getApplicationContext());
                File p = (File) parent.getItemAtPosition(position);
                backupUtils.restoreCookieBackup(p);
            }
        });

        return cookieBackupDialog.create();
    }
}
