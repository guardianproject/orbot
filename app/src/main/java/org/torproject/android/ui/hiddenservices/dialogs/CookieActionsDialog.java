package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

import java.io.FileOutputStream;
import java.io.IOException;

public class CookieActionsDialog extends DialogFragment {
    public static final int WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTION_DIALOG = 4;
    private static final int REQUEST_CODE_WRITE_FILE = 123;
    private AlertDialog actionDialog;
    private String domain;
    private String cookie;
    private int enabled;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        domain = arguments.getString("domain");
        cookie = arguments.getString("auth_cookie_value");
        enabled = arguments.getInt("enabled");

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_cookie_actions, null);
        actionDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.client_cookies)
                .create();

        dialog_view.findViewById(R.id.btn_cookie_cancel).setOnClickListener(v -> actionDialog.dismiss());
        dialog_view.findViewById(R.id.btn_cookie_backup).setOnClickListener(v -> doBackup());
        dialog_view.findViewById(R.id.btn_cookie_delete).setOnClickListener(v -> {
            CookieDeleteDialog dialog = new CookieDeleteDialog();
            dialog.setArguments(arguments);
            dialog.show(getFragmentManager(), "CookieDeleteDialog");
            actionDialog.dismiss();
        });

        return actionDialog;
    }

    public void doBackup() {
        Intent createFile = DiskUtils.createWriteFileIntent(domain.replace(".onion", ".json"), "application/json");
        startActivityForResult(createFile, REQUEST_CODE_WRITE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_WRITE_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri file = data.getData();
                try {
                    ParcelFileDescriptor pfd = getActivity().getContentResolver().openFileDescriptor(file, "w");
                    FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                    JSONObject backup = new JSONObject();
                    backup.put(CookieContentProvider.ClientCookie.DOMAIN, domain);
                    backup.put(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE, cookie);
                    backup.put(CookieContentProvider.ClientCookie.ENABLED, enabled);
                    fileOutputStream.write(backup.toString().getBytes());
                    // Let the document provider know you're done by closing the stream.
                    fileOutputStream.close();
                    pfd.close();
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), R.string.error, Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(getContext(), R.string.backup_saved_at_external_storage, Toast.LENGTH_LONG).show();
                actionDialog.dismiss();
            }
        }
    }
}
