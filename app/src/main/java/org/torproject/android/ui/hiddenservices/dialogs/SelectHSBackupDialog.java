package org.torproject.android.ui.hiddenservices.dialogs;

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
import org.torproject.android.ui.hiddenservices.adapters.BackupAdapter;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.storage.ExternalStorage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectHSBackupDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder backupsDialog = new AlertDialog.Builder(getActivity());

        backupsDialog.setTitle(R.string.restore_backup);

        File backupDir = ExternalStorage.getOrCreateBackupDir();
        File[] files = null;

        try {
            files = backupDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".zip");
                }
            });
        } catch (NullPointerException e) {
            // Silent block
        }

        if (files == null || files.length < 1) {
            backupsDialog.setMessage(R.string.create_a_backup_first);
            backupsDialog.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

            return backupsDialog.create();
        }

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_backups_list, null);

        backupsDialog.setView(dialog_view);
        backupsDialog.setPositiveButton(R.string.btn_okay, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        ListView backups = (ListView) dialog_view.findViewById(R.id.listview_hs_backups);

        List<File> zips = new ArrayList<>();
        Collections.addAll(zips, files);

        backups.setAdapter(new BackupAdapter(getContext(), R.layout.layout_hs_backups_list_item, zips));
        backups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BackupUtils backupUtils = new BackupUtils(view.getContext().getApplicationContext());
                File p = (File) parent.getItemAtPosition(position);
                backupUtils.restoreZipBackup(p);
            }
        });

        return backupsDialog.create();
    }
}
