/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.settings;

import java.util.Locale;

import org.sufficientlysecure.rootcommands.RootCommands;
import org.sufficientlysecure.rootcommands.Shell;
import org.torproject.android.R;
import org.torproject.android.service.TorServiceUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;


public class SettingsPreferences 
		extends PreferenceActivity implements OnPreferenceClickListener {

	private CheckBoxPreference prefCBTransProxy = null;
	private CheckBoxPreference prefcBTransProxyAll = null;
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

        prefCBTransProxy = (CheckBoxPreference) findPreference("pref_transparent");
        prefcBTransProxyAll = (CheckBoxPreference) findPreference("pref_transparent_all");

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
        prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked()
                && (!prefcBTransProxyAll.isChecked()));

        prefCBTransProxy.setOnPreferenceClickListener(this);
        prefcBTransProxyAll.setOnPreferenceClickListener(this);
        prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());

        prefHiddenServices = (CheckBoxPreference) findPreference("pref_hs_enable");
        prefHiddenServices.setOnPreferenceClickListener(this);
        prefHiddenServicesPorts = (EditTextPreference) findPreference("pref_hs_ports");
        prefHiddenServicesPorts.setEnabled(prefHiddenServices.isChecked());
        prefHiddenServicesHostname = (EditTextPreference) findPreference("pref_hs_hostname");
        prefHiddenServicesHostname.setEnabled(prefHiddenServices.isChecked());
    }

	public boolean onPreferenceClick(Preference preference) {
		
		setResult(RESULT_OK);
		
		if (preference == prefRequestRoot)
		{
			if (prefRequestRoot.isChecked())
			{
				
				boolean canRoot = RootCommands.rootAccessGiven();
				prefRequestRoot.setChecked(canRoot);

				if (!canRoot)
				{
					try
					{
						Shell shell = Shell.startRootShell();
						shell.close();
						
						prefRequestRoot.setChecked(true);

					}
					catch (Exception e)
					{
						Toast.makeText(this, R.string.wizard_permissions_no_root_msg, Toast.LENGTH_LONG).show();
					}
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
		else if (preference == prefLocale)
		{
			 SharedPreferences settings = TorServiceUtils.getSharedPrefs(getApplicationContext());

		        Configuration config = getResources().getConfiguration();

		        String lang = settings.getString("pref_default_locale", "");
		        
		        Locale locale;
		        
		        if (lang.equals("xx"))
		        {
		        	locale = Locale.getDefault();
		        
		        }
		        else
		        	locale = new Locale(lang);
		        
	            Locale.setDefault(locale);
	            config.locale = locale;
	            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
	            
		}
		else
		{
			prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
			prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked() && (!prefcBTransProxyAll.isChecked()));
			
			
		}
		
		return true;
	}


}
