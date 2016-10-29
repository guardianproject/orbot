/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import org.torproject.android.OrbotApp;
import org.torproject.android.R;
import org.torproject.android.service.transproxy.TorTransProxy;
import org.torproject.android.ui.AppManager;
import org.torproject.android.service.util.TorServiceUtils;

import java.util.Locale;


public class SettingsPreferences 
		extends PreferenceActivity implements OnPreferenceClickListener {
    private static final String TAG = "SettingsPreferences";

	private CheckBoxPreference prefCBTransProxy = null;
	private CheckBoxPreference prefcBTransProxyAll = null;
    private CheckBoxPreference prefcbTransTethering = null;

	private Preference prefTransProxyFlush = null;
	
	private Preference prefTransProxyApps = null;
    private CheckBoxPreference prefHiddenServices = null;
    private EditTextPreference prefHiddenServicesPorts;
    private EditTextPreference prefHiddenServicesHostname;
	private CheckBoxPreference prefRequestRoot = null;
	private ListPreference prefLocale = null;
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        prefRequestRoot = (CheckBoxPreference) findPreference("has_root");
        prefRequestRoot.setOnPreferenceClickListener(this);

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

        prefCBTransProxy = (CheckBoxPreference) findPreference("pref_transparent");
        prefcBTransProxyAll = (CheckBoxPreference) findPreference("pref_transparent_all");
        prefcbTransTethering = (CheckBoxPreference) findPreference("pref_transparent_tethering");

        prefTransProxyFlush = (Preference) findPreference("pref_transproxy_flush");
        prefTransProxyFlush.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {

                Intent data = new Intent();
                data.putExtra("transproxywipe", true);
                setResult(RESULT_OK, data);

                finish();
                return false;
            }

        });

        
        prefTransProxyApps = findPreference("pref_transparent_app_list");
        prefTransProxyApps.setOnPreferenceClickListener(this);
        prefCBTransProxy.setOnPreferenceClickListener(this);
        prefcBTransProxyAll.setOnPreferenceClickListener(this);
        prefHiddenServices = (CheckBoxPreference) findPreference("pref_hs_enable");
        prefHiddenServices.setOnPreferenceClickListener(this);
        prefHiddenServicesHostname = (EditTextPreference) findPreference("pref_hs_hostname");
        
        
        prefCBTransProxy.setEnabled(prefRequestRoot.isChecked());
        prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
        prefcbTransTethering.setEnabled(prefCBTransProxy.isChecked());

        if (prefCBTransProxy.isChecked())
        	prefTransProxyApps.setEnabled((!prefcBTransProxyAll.isChecked()));
        
        prefHiddenServicesPorts = (EditTextPreference) findPreference("pref_hs_ports");
        prefHiddenServicesHostname.setEnabled(prefHiddenServices.isChecked());
        prefHiddenServicesPorts.setEnabled(prefHiddenServices.isChecked());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
       	 	prefTransProxyApps.setEnabled(true);
        }
        
        
    }

	public boolean onPreferenceClick(Preference preference) {
		
		setResult(RESULT_OK);
		
		if (preference == prefRequestRoot)
		{
			if (prefRequestRoot.isChecked())
			{

                try {
                    TorTransProxy.testRoot();
                    prefCBTransProxy.setEnabled(true);

                }
                catch (Exception e)
                {
                    Log.d(OrbotApp.TAG,"root not yet enabled");
                }

			}
		}
		else if (preference == prefTransProxyApps)
		{
			startActivity(new Intent(this, AppManager.class));
			
		}
		else if (preference == prefHiddenServices)
		{
	        prefHiddenServicesPorts.setEnabled(prefHiddenServices.isChecked());
	        prefHiddenServicesHostname.setEnabled(prefHiddenServices.isChecked());
		}
		else
		{
			prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
			prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked() && (!prefcBTransProxyAll.isChecked()));
			
			
		}
		
		return true;
	}


}
