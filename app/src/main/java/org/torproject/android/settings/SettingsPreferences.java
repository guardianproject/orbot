/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import org.torproject.android.R;
import org.torproject.android.service.util.Prefs;

public class SettingsPreferences
        extends PreferenceActivity {
    private static final String TAG = "SettingsPreferences";

    private ListPreference prefLocale = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);

        prefLocale = (ListPreference) findPreference("pref_default_locale");
        Languages languages = Languages.get(this);
        prefLocale.setEntries(languages.getAllNames());
        prefLocale.setEntryValues(languages.getSupportedLocales());
        prefLocale.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String language = (String) newValue;
                Prefs.setDefaultLocale(language);
                LocaleHelper.setLocale(getApplicationContext(), language);
                Intent intentResult = new Intent();
                intentResult.putExtra("locale", language);
                setResult(RESULT_OK, intentResult);
                finish();
                return false;
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

}
