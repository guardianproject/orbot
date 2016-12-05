package org.torproject.android.ui.hiddenservices.dialogs;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.torproject.android.R;

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

        TextView cookie = (TextView) dialog_view.findViewById(R.id.hs_cookie);
        cookie.setText(auth_cookie_value);

        Button clipboard = (Button) dialog_view.findViewById(R.id.hs_cookie_to_clipboard);
        clipboard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Context mContext = v.getContext();
                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("cookie", auth_cookie_value);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(mContext, R.string.done, Toast.LENGTH_LONG).show();
                cookieDialog.dismiss();
            }
        });

        Button cancel = (Button) dialog_view.findViewById(R.id.hs_cookie_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cookieDialog.dismiss();
            }
        });

        return cookieDialog;
    }
}
