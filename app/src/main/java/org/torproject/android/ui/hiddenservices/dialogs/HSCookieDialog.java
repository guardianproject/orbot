package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

public class HSCookieDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_cookie, null);

        final Bundle arguments = getArguments();
        final String auth_cookie_value = arguments.getString("auth_cookie_value");

        final AlertDialog cookieDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .create();

        TextView cookie = dialog_view.findViewById(R.id.hs_cookie);
        cookie.setText(auth_cookie_value);

        Button clipboard = dialog_view.findViewById(R.id.hs_cookie_to_clipboard);
        clipboard.setOnClickListener(v -> {
            Context mContext = v.getContext();
            ClipboardManager clipboard1 = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("cookie", auth_cookie_value);
            clipboard1.setPrimaryClip(clip);
            Toast.makeText(mContext, R.string.done, Toast.LENGTH_LONG).show();
            cookieDialog.dismiss();
        });

        Button shareQR = dialog_view.findViewById(R.id.hs_cookie_to_qr);
        shareQR.setOnClickListener(v -> {
            try {
                JSONObject backup = new JSONObject();
                backup.put(CookieContentProvider.ClientCookie.DOMAIN, arguments.getString("onion"));
                backup.put(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE, arguments.getString("auth_cookie_value"));

                IntentIntegrator integrator = new IntentIntegrator(getActivity());
                integrator.shareText(backup.toString());

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            cookieDialog.dismiss();
        });

        Button cancel = dialog_view.findViewById(R.id.hs_cookie_cancel);
        cancel.setOnClickListener(v -> cookieDialog.dismiss());

        return cookieDialog;
    }
}
