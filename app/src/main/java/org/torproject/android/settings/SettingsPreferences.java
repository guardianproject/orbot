/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import org.torproject.android.OrbotApp;
import org.torproject.android.R;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.service.util.TorServiceUtils;

import java.util.Locale;


public class SettingsPreferences 
		extends PreferenceActivity implements OnPreferenceClickListener {
    private static final String TAG = "SettingsPreferences";

	private ListPreference prefLocale = null;
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        prefLocale = (ListPreference) findPreference("pref_default_locale");
        prefLocale.setOnPreferenceClickListener(this);
        Languages languages = Languages.get(this);
        prefLocale.setEntries(languages.getAllNames());
        prefLocale.setEntryValues(languages.getSupportedLocales());
        prefLocale.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String language = (String) newValue;
                if (preference == prefLocale) {
                    OrbotApp app = (OrbotApp) getApplication();
                    Languages.setLanguage(app, language, true);
                    Prefs.setDefaultLocale(language);
                    OrbotApp.forceChangeLanguage(SettingsPreferences.this);
                }
                return false;
            }
        });


        
    }

	public boolean onPreferenceClick(Preference preference) {
		
		setResult(RESULT_OK);


		return true;
	}


}
