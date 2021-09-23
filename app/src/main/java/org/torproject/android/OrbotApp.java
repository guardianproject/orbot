package org.torproject.android;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import org.torproject.android.core.Languages;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.util.Prefs;

import java.util.Locale;

public class OrbotApp extends Application implements OrbotConstants {

    @Override
    public void onCreate() {
        super.onCreate();
        Languages.setup(OrbotMainActivity.class, R.string.menu_settings);

        if (!Prefs.getDefaultLocale().equals(Locale.getDefault().getLanguage())) {
            Languages.setLanguage(this, Prefs.getDefaultLocale(), true);
        }

        deleteDatabase("hidden_services"); // if exists remove v2 onion service data

    }

    @Override
    protected void attachBaseContext(Context base) {
        Prefs.setContext(base);
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!Prefs.getDefaultLocale().equals(Locale.getDefault().getLanguage()))
            Languages.setLanguage(this, Prefs.getDefaultLocale(), true);
    }

}
