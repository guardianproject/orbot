package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.ListView;
import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.adapters.BackupAdapter;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.core.ExternalStorage;

import java.io.File;
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
            files = backupDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        } catch (NullPointerException e) {
            // Silent block
        }

        if (files == null || files.length < 1) {
            backupsDialog.setMessage(R.string.create_a_backup_first);
            backupsDialog.setNegativeButton(R.string.btn_cancel, (dialog, id) -> dialog.dismiss());

            return backupsDialog.create();
        }

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_backups_list, null);

        backupsDialog.setView(dialog_view);
        backupsDialog.setPositiveButton(R.string.btn_okay, (dialog, id) -> dialog.dismiss());

        ListView backups = dialog_view.findViewById(R.id.listview_hs_backups);

        List<File> zips = new ArrayList<>();
        Collections.addAll(zips, files);

        backups.setAdapter(new BackupAdapter(getContext(), R.layout.layout_hs_backups_list_item, zips));
        backups.setOnItemClickListener((parent, view, position, id) -> {
            BackupUtils backupUtils = new BackupUtils(view.getContext().getApplicationContext());
            File p = (File) parent.getItemAtPosition(position);
            backupUtils.restoreZipBackup(p);
        });

        return backupsDialog.create();
    }
}
