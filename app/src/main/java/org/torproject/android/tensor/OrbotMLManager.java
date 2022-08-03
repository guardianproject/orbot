package org.torproject.android.tensor;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.os.ConfigurationCompat;

import org.tensorflow.lite.support.label.Category;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.ui.onboarding.BridgeWizardActivity;

import java.util.List;
import java.util.Locale;

public class OrbotMLManager {

    private TextClassificationHelper mConnClassifier;

    private Context mContext;

    public OrbotMLManager (Context context)
    {
        mContext = context;
    }

    public static String generateConnectionConfigurationToken (Context context) {
        //generate connection config
        StringBuilder sb = new StringBuilder();

        //type of bridge or bridge info
        sb.append(Prefs.getBridgesList()).append(' ');

        //version of tor
        sb.append(OrbotService.BINARY_TOR_VERSION).append(' ');

        //type of network
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            sb.append(netInfo.getTypeName()).append(' ');
            sb.append(netInfo.getSubtypeName()).append(' ');
        }

        //type of device
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;
        sb.append(manufacturer).append(' ');
        sb.append(model).append(' ');
        sb.append(version).append(' ');
        sb.append(versionRelease).append(' ');

        //locale of device
        Locale locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
        sb.append(locale.getLanguage()).append(' ');
        sb.append(locale.getCountry()).append(' ');

        return sb.toString();
    }

    public void useMLConnectionClassifier (String connConfigCode) {

        if (mConnClassifier == null) {
            TextClassificationHelper.TextResultsListener listener = new TextClassificationHelper.TextResultsListener() {
                @Override
                public void onResult(@NonNull List<Category> results, long inferenceTime) {

                    for (Category cat : results) {
                        String status = cat.getDisplayName() + " label=" + cat.getLabel() + " " + cat.getScore();
                        Log.d("OrbotML",status);
                    }


                }

                @Override
                public void onError(@NonNull String error) {
                    Log.d("OrbotML","Error:" + error);

                }
            };

            mConnClassifier = new TextClassificationHelper(0, TextClassificationHelper.WORD_VEC, mContext, listener);
        }

        mConnClassifier.classify(connConfigCode);

    }
}
