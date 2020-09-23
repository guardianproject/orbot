package org.torproject.android.ui.hiddenservices.dialogs;


import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

public class AddCookieDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_add_client_cookie_dialog, null);
        return new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.client_cookies)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.save, (dialog, which) -> doSave(dialog_view, getContext()))
                .create();
    }

    private void doSave(View dialogView, Context context) {
        String onion = ((EditText) dialogView.findViewById(R.id.cookie_onion)).getText().toString();
        String cookie = ((EditText) dialogView.findViewById(R.id.cookie_value)).getText().toString();

        if (checkInput(onion, cookie)) {
            saveData(onion, cookie);
            Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG).show();
        }
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
