package org.torproject.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.service.OrbotService;

import java.io.IOException;

import IPtProxy.IPtProxy;

public class AboutDialogFragment extends DialogFragment {

    public static final String TAG = AboutDialogFragment.class.getSimpleName();
    private static final String BUNDLE_KEY_TV_ABOUT_TEXT = "about_tv_txt";
    private TextView tvAbout;
    private static final String ABOUT_LICENSE_EQUALSIGN = "===============================================================================";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_about, null);
        String version;

        try {
            version = getContext().getPackageManager().getPackageInfo(
                    getContext().getPackageName(), 0).versionName + " (Tor " +
                    OrbotService.BINARY_TOR_VERSION + ")";
        } catch (PackageManager.NameNotFoundException e) {
            version = "Version Not Found";
        }

        TextView versionName = view.findViewById(R.id.versionName);
        versionName.setText(version);

        tvAbout = view.findViewById(R.id.aboutother);

        TextView tvObfs4 = view.findViewById(R.id.tvObfs4);
        tvObfs4.setText(getString(R.string.obfs4_url, IPtProxy.obfs4ProxyVersion()));

        TextView tvSnowflake = view.findViewById(R.id.tvSnowflake);
        tvSnowflake.setText(getString(R.string.snowflake_url, IPtProxy.snowflakeVersion()));

        boolean buildAboutText = true;

        if (savedInstanceState != null) {
            String tvAboutText = savedInstanceState.getString(BUNDLE_KEY_TV_ABOUT_TEXT);
            if (tvAboutText != null) {
                buildAboutText = false;
                tvAbout.setText(tvAboutText);
            }
        }

        if (buildAboutText) {
            try {
                String aboutText = DiskUtils.readFileFromAssets("LICENSE", getContext());
                aboutText = aboutText.replaceAll(ABOUT_LICENSE_EQUALSIGN, "\n").replace("\n\n", "<br/><br/>").replace("\n", "");
                tvAbout.setText(Html.fromHtml(aboutText));
            } catch (IOException e) {
            }
        }
        return new AlertDialog.Builder(getContext(), R.style.OrbotDialogTheme)
                .setTitle(getString(R.string.button_about))
                .setView(view)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_KEY_TV_ABOUT_TEXT, tvAbout.getText().toString());
    }
}