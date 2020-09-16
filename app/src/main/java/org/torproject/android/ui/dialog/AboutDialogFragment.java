package org.torproject.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import org.torproject.android.R;
import org.torproject.android.service.OrbotService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutDialogFragment extends DialogFragment {

    public static final String TAG = AboutDialogFragment.class.getSimpleName();
    private static final String BUNDLE_KEY_TV_ABOUT_TEXT = "about_tv_txt";
    private TextView tvAbout;

    @SuppressWarnings("SameParameterValue")
    private static String readFromAssets(Context context, String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

        // do reading, usually loop until end of file reading
        StringBuilder sb = new StringBuilder();
        String mLine = reader.readLine();
        while (mLine != null) {
            sb.append(mLine).append('\n'); // process line
            mLine = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }

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
                String aboutText = readFromAssets(getContext(), "LICENSE");
                aboutText = aboutText.replace("\n", "<br/>");
                tvAbout.setText(Html.fromHtml(aboutText));
            } catch (IOException e) {
            }
        }
        return new AlertDialog.Builder(getContext())
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