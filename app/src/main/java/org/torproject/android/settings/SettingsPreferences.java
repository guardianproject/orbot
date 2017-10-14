/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import org.torproject.android.OrbotApp;
import org.torproject.android.R;
import org.torproject.android.ui.AppManager;
import org.torproject.android.service.util.TorServiceUtils;

import java.util.Locale;


public class SettingsPreferences 
		extends PreferenceActivity implements OnPreferenceClickListener {
    private static final String TAG = "SettingsPreferences";

	private Preference prefTransProxyApps = null;
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
                    SharedPreferences settings = TorServiceUtils
                            .getSharedPrefs(getApplicationContext());

                    String lang = settings.getString("pref_default_locale",
                            Locale.getDefault().getLanguage());
                    OrbotApp app = (OrbotApp) getApplication();
                    Languages.setLanguage(app, language, true);
                    lang = settings.getString("pref_default_locale",
                            Locale.getDefault().getLanguage());
                    OrbotApp.forceChangeLanguage(SettingsPreferences.this);
                }
                return false;
            }
        });


        prefTransProxyApps = findPreference("pref_transparent_app_list");
        prefTransProxyApps.setOnPreferenceClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
       	 	prefTransProxyApps.setEnabled(true);
        }
        
        
    }

	public boolean onPreferenceClick(Preference preference) {
		
		setResult(RESULT_OK);


		return true;
	}


}
