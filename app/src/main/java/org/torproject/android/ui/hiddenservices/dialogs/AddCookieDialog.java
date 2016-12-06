package org.torproject.android.ui.hiddenservices.dialogs;


import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

public class AddCookieDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_add_client_cookie_dialog, null);

        final AlertDialog addCookieDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.client_cookies)
                .create();

        Button save = (Button) dialog_view.findViewById(R.id.cookie_dialog_save);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String onion = ((EditText) dialog_view.findViewById(R.id.cookie_onion)).getText().toString();
                String cookie = ((EditText) dialog_view.findViewById(R.id.cookie_value)).getText().toString();

                if (checkInput(onion, cookie)) {
                    saveData(onion, cookie);
                    Toast.makeText(
                            v.getContext(), R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG
                    ).show();
                    addCookieDialog.dismiss();
                }
            }
        });

        Button cancel = (Button) dialog_view.findViewById(R.id.cookie_dialog_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addCookieDialog.cancel();
            }
        });

        return addCookieDialog;
    }

    private boolean checkInput(String onion, String cookie) {

        boolean is_set = ((onion != null && onion.length() > 0) && (cookie != null && cookie.length() > 0));
        if (!is_set) {
            Toast.makeText(getContext(), R.string.fields_can_t_be_empty, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!onion.matches("([a-z0-9]{16})\\.onion")) {
            Toast.makeText(getContext(), R.string.invalid_onion_address, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveData(String domain, String cookie) {

        ContentValues fields = new ContentValues();
        fields.put(CookieContentProvider.ClientCookie.DOMAIN, domain);
        fields.put(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE, cookie);

        ContentResolver cr = getContext().getContentResolver();

        cr.insert(CookieContentProvider.CONTENT_URI, fields);
    }
}
