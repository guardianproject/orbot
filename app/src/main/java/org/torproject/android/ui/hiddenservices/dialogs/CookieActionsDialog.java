package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.ui.hiddenservices.ClientCookiesActivity;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class CookieActionsDialog extends DialogFragment {
    private static final int REQUEST_CODE_WRITE_FILE = 123;
    private String domain;
    private String cookie;
    private int enabled;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        domain = arguments.getString(ClientCookiesActivity.BUNDLE_KEY_DOMAIN);
        cookie = arguments.getString(ClientCookiesActivity.BUNDLE_KEY_COOKIE);
        enabled = arguments.getInt(ClientCookiesActivity.BUNDLE_KEY_ENABLED);

        AlertDialog ad = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.client_cookies)
                .setItems(new CharSequence[]{
                        getString(R.string.backup_cookie),
                        getString(R.string.delete_cookie)
                }, null)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();

        ad.getListView().setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) doBackup();
            else {
                doDelete(arguments);
                ad.dismiss();
            }
        });

        return ad;
    }

    private void doDelete(Bundle arguments) {
        CookieDeleteDialog dialog = new CookieDeleteDialog();
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager(), "CookieDeleteDialog");
        dismiss();
    }

    private void doBackup() {
        String filename = domain.replace(".onion", ".json");
        if (DiskUtils.supportsStorageAccessFramework()) {
            Intent createFile = DiskUtils.createWriteFileIntent(filename, "application/json");
            startActivityForResult(createFile, REQUEST_CODE_WRITE_FILE);
        } else { // API 16, 17, and 18
            int msg = R.string.backup_saved_at_external_storage;
            try {
                File externalStorage = DiskUtils.getOrCreateLegacyBackupDir(getString(R.string.app_name));
                String backupFile = externalStorage.getAbsolutePath() + "/" + filename;
                String data = createBackupData();
                FileWriter writer = new FileWriter(backupFile);
                writer.write(data);
                writer.close();
            } catch (JSONException | IOException e) {
                msg = R.string.error;
            }
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            dismiss();
        }
    }

    private String createBackupData() throws JSONException {
        JSONObject backup = new JSONObject();
        backup.put(CookieContentProvider.ClientCookie.DOMAIN, domain);
        backup.put(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE, cookie);
        backup.put(CookieContentProvider.ClientCookie.ENABLED, enabled);
        return backup.toString();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_WRITE_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                int msg = R.string.backup_saved_at_external_storage;
                Uri file = data.getData();
                try {
                    ParcelFileDescriptor pfd = getActivity().getContentResolver().openFileDescriptor(file, "w");
                    FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                    fileOutputStream.write(createBackupData().getBytes());
                    fileOutputStream.close(); // Let the document provider know you're done by closing the stream
                    pfd.close();
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    msg = R.string.error;
                }
                dismiss();
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        }
    }
}
