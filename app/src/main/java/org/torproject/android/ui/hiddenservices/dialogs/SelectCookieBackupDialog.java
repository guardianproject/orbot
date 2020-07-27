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
import org.torproject.android.ui.hiddenservices.storage.ExternalStorage;

import java.io.File;
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
            files = backupDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        } catch (NullPointerException e) {
            // Silent block
        }

        if (files == null || files.length < 1) {
            cookieBackupDialog.setMessage(R.string.create_a_backup_first);
            cookieBackupDialog.setNegativeButton(R.string.btn_cancel, (dialog, id) -> dialog.dismiss());

            return cookieBackupDialog.create();
        }

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_backups_list, null);

        cookieBackupDialog.setView(dialog_view);
        cookieBackupDialog.setPositiveButton(R.string.btn_okay, (dialog, id) -> dialog.dismiss());

        ListView backups = dialog_view.findViewById(R.id.listview_hs_backups);

        List<File> json_backups = new ArrayList<>();
        Collections.addAll(json_backups, files);

        backups.setAdapter(new BackupAdapter(getContext(), R.layout.layout_hs_backups_list_item, json_backups));
        backups.setOnItemClickListener((parent, view, position, id) -> {
            BackupUtils backupUtils = new BackupUtils(view.getContext().getApplicationContext());
            File p = (File) parent.getItemAtPosition(position);
            backupUtils.restoreCookieBackup(p);
        });

        return cookieBackupDialog.create();
    }
}
