package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

public class AddCookieDialog extends DialogFragment {

    private EditText etOnion, etCookie;
    private TextWatcher inputValidator;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.layout_add_client_cookie_dialog, null);
        final AlertDialog ad = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setTitle(R.string.client_cookies)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.save, (dialog, which) -> doSave(getContext()))
                .create();

        etOnion = dialogView.findViewById(R.id.cookie_onion);
        etCookie = dialogView.findViewById(R.id.cookie_value);

        inputValidator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(checkInput());
            }
        };

        etOnion.addTextChangedListener(inputValidator);
        etCookie.addTextChangedListener(inputValidator);

        return ad;
    }

    @Override
    public void onStart() {
        super.onStart();
        inputValidator.afterTextChanged(null); // set positive button to be initially disabled
    }
    private void doSave(Context context) {
        saveData(etOnion.getText().toString(), etCookie.getText().toString());
        Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
    }

    private boolean checkInput() {
        String onion = etOnion.getText().toString();
        String cookie = etCookie.getText().toString();
        if (TextUtils.isEmpty(onion.trim()) || TextUtils.isEmpty(cookie.trim())) return false;
        return isV2OnionAddressValid(onion);
    }

    private static boolean isV2OnionAddressValid(String onionToTest) {
        return onionToTest.matches("([a-z0-9]{16}).onion");
    }

    private void saveData(String domain, String cookie) {
        ContentValues fields = new ContentValues();
        fields.put(CookieContentProvider.ClientCookie.DOMAIN, domain);
        fields.put(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE, cookie);

        ContentResolver cr = getContext().getContentResolver();

        cr.insert(CookieContentProvider.CONTENT_URI, fields);
    }
}
