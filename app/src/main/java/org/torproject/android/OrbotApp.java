package org.torproject.android;

import android.app.Application;
import android.content.res.Configuration;

import org.torproject.android.core.Languages;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.tensor.CleanInsightsManager;

import java.util.Locale;

public class OrbotApp extends Application implements OrbotConstants {

    private CleanInsightsManager ciManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.setContext(this);
        LocaleHelper.onAttach(this);

        Languages.setup(OrbotMainActivity.class, R.string.menu_settings);

        if (!Prefs.getDefaultLocale().equals(Locale.getDefault().getLanguage())) {
            Languages.setLanguage(this, Prefs.getDefaultLocale(), true);
        }

        deleteDatabase("hidden_services"); // if it exists, remove v2 onion service data

        ciManager = new CleanInsightsManager();
        ciManager.initMeasurement(this);
    }

    public CleanInsightsManager getCleanInsightsManager () {
        return ciManager;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!Prefs.getDefaultLocale().equals(Locale.getDefault().getLanguage()))
            Languages.setLanguage(this, Prefs.getDefaultLocale(), true);
    }

}
