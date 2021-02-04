package org.torproject.android.ui.v3onionservice.clientauth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.core.ui.NoPersonalizedLearningEditText;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;

import java.io.File;

public class ClientAuthBackupDialogFragment extends DialogFragment {

    private NoPersonalizedLearningEditText etFilename;

    public ClientAuthBackupDialogFragment() {
    }

    public ClientAuthBackupDialogFragment(Bundle args) {
        super();
        setArguments(args);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog ad = new AlertDialog.Builder(getContext())
                .setTitle(R.string.v3_backup_key)
                .setMessage(R.string.v3_backup_key_warning)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        ad.setOnShowListener(dialog -> ad.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> doBackup()));
        FrameLayout container = new FrameLayout(ad.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelOffset(R.dimen.alert_dialog_margin);
        params.leftMargin = margin;
        params.rightMargin = margin;
        etFilename = new NoPersonalizedLearningEditText(ad.getContext(), null);
        etFilename.setSingleLine(true);
        etFilename.setHint(R.string.v3_backup_name_hint);
        etFilename.setLayoutParams(params);
        container.addView(etFilename);
        ad.setView(container);
        return ad;
    }

    private void doBackup() {
        String filename = etFilename.getText().toString().trim();
        if (filename.equals("")) filename = "filename";
        filename += ".auth_private";
        if (DiskUtils.supportsStorageAccessFramework()) {
            Intent createFileIntent = DiskUtils.createWriteFileIntent(filename, "text/*");
            getActivity().startActivityForResult(createFileIntent, REQUEST_CODE_WRITE_FILE);
        } else { // APIs 16, 17, 18
            attemptToWriteBackup(Uri.fromFile(new File(DiskUtils.getOrCreateLegacyBackupDir("Orbot"), filename)));
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
        BackupUtils backupUtils = new BackupUtils(getContext());
        String domain = getArguments().getString(ClientAuthActivity.BUNDLE_KEY_DOMAIN);
        String hash = getArguments().getString(ClientAuthActivity.BUNDLE_KEY_HASH);
        String backup = backupUtils.createV3AuthBackup(domain, hash, outputFile);
        Toast.makeText(getContext(), backup != null ? R.string.backup_saved_at_external_storage : R.string.error, Toast.LENGTH_LONG).show();
        dismiss();
    }

    private static final int REQUEST_CODE_WRITE_FILE = 432;
}
