package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.zxing.integration.android.IntentIntegrator;

import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.core.ClipboardUtils;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

public class HSCookieDialog extends DialogFragment {
    private String authCookieValue, onion;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        authCookieValue = arguments.getString("auth_cookie_value");
        onion = arguments.getString("onion");
        return new AlertDialog.Builder(getActivity())
                .setTitle(authCookieValue)
                .setItems(new CharSequence[]{
                        getString(R.string.copy_cookie_to_clipboard),
                        getString(R.string.share_as_qr)
                }, (dialog, which) -> {
                    if (which == 0) doCopy();
                    else if (which == 1) doShareQr();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
    }

    private void doShareQr() {
        try {
            JSONObject backup = new JSONObject();
            backup.put(CookieContentProvider.ClientCookie.DOMAIN, onion);
            backup.put(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE, authCookieValue);

            new IntentIntegrator(getActivity()).shareText(backup.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doCopy() {
        if (authCookieValue != null)
            ClipboardUtils.copyToClipboard("cookie", authCookieValue, getString(R.string.done), getContext());
    }

}
