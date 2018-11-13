package org.torproject.android;

import android.app.Dialog;
import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PrivateNetworkDialog extends DialogFragment {
    public interface PrivateNetworkDialogListener {
        public void onPrivateNetworkConnect(RequestTorConfigTask.TorRESTRequestParams params);
    }

    PrivateNetworkDialogListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (PrivateNetworkDialogListener)context;
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_private_network_dialog, null);

        final AlertDialog connectPrivateNetworkDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.pnd_title)
                .create();

        final EditText privateNetworkURLET = dialog_view.findViewById(R.id.private_network_url);
        final EditText privateNetworkUsername = dialog_view.findViewById(R.id.private_network_username);
        final EditText privateNetworkPassword = dialog_view.findViewById(R.id.private_network_password);

        Button save = (Button) dialog_view.findViewById(R.id.private_network_dialog_connect);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RequestTorConfigTask.TorRESTRequestParams params = new RequestTorConfigTask.TorRESTRequestParams();
                params.url = privateNetworkURLET.getText().toString();
                params.username = privateNetworkUsername.getText().toString();
                params.password = privateNetworkPassword.getText().toString();

                // The URL won't work without the protocol
                if (!params.url.contains("://")) {
                    params.url = "http://" + params.url;
                }

                mListener.onPrivateNetworkConnect(params);

                connectPrivateNetworkDialog.dismiss();
            }
        });

        Button cancel = (Button) dialog_view.findViewById(R.id.private_network_dialog_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectPrivateNetworkDialog.cancel();
            }
        });

        return connectPrivateNetworkDialog;
    }
}
