/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.settings;

import java.util.Locale;

import org.sufficientlysecure.rootcommands.RootCommands;
import org.torproject.android.R;
import org.torproject.android.service.TorServiceUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.widget.Toast;


public class SettingsPreferences 
		extends PreferenceActivity implements OnPreferenceClickListener {

	private CheckBoxPreference prefCBTransProxy = null;
	private CheckBoxPreference prefcBTransProxyAll = null;
	private Preference prefTransProxyFlush = null;
	
	private Preference prefTransProxyApps = null;
	private CheckBoxPreference prefHiddenServices = null;
	private CheckBoxPreference prefRequestRoot = null;
	private Preference prefLocale = null;
	
	private boolean hasRoot = false;

	private final static int HIDDEN_SERVICE_PREF_IDX = 6;
	private final static int TRANSPROXY_GROUP_IDX = 1;
	private final static int DEBUG_GROUP_IDX = 8;
	
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
		
		hasRoot = prefs.getBoolean("has_root",false);
		
		init();
	}
	
	
	@Override
	protected void onResume() {
	
		super.onResume();
	
	}
	
	private void init ()
	{
		int REQUEST_ROOT_IDX = 1;
		int SET_LOCALE_IDX = 3;

		int GENERAL_GROUP_IDX = 0;
		
		prefRequestRoot = ((CheckBoxPreference)((PreferenceCategory)getPreferenceScreen().getPreference(GENERAL_GROUP_IDX)).getPreference(REQUEST_ROOT_IDX));
		prefRequestRoot.setOnPreferenceClickListener(this);

		prefLocale = (((PreferenceCategory)getPreferenceScreen().getPreference(GENERAL_GROUP_IDX)).getPreference(SET_LOCALE_IDX));
		prefLocale.setOnPreferenceClickListener(this);
				
		prefCBTransProxy = ((CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(TRANSPROXY_GROUP_IDX)).getPreference(0));
		prefcBTransProxyAll = (CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(TRANSPROXY_GROUP_IDX)).getPreference(1);
		
		prefTransProxyFlush = (Preference)((PreferenceCategory)this.getPreferenceScreen().getPreference(DEBUG_GROUP_IDX)).getPreference(8);
		prefTransProxyFlush.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference arg0) {
				
				Intent data = new Intent();
				data.putExtra("transproxywipe", true);
				setResult(RESULT_OK,data);
				
				finish();
				return false;
			}
			
		});
		
		prefTransProxyApps = ((PreferenceCategory)this.getPreferenceScreen().getPreference(TRANSPROXY_GROUP_IDX)).getPreference(2);


		prefCBTransProxy.setOnPreferenceClickListener(this);
		prefcBTransProxyAll.setOnPreferenceClickListener(this);
		prefTransProxyApps.setOnPreferenceClickListener(this);
		
		if (!hasRoot)
		{
			getPreferenceScreen().getPreference(TRANSPROXY_GROUP_IDX).setEnabled(false);
		}
		else
		{

			prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
			prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked() && (!prefcBTransProxyAll.isChecked()));
			
		}
		
		
		prefHiddenServices = ((CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(0));
		prefHiddenServices.setOnPreferenceClickListener(this);
		((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(1).setEnabled(prefHiddenServices.isChecked());
		((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(2).setEnabled(prefHiddenServices.isChecked());
				
		
	};
	
	
	
	
	@Override
	protected void onPause() {

		super.onPause();
		


		
		
		
		
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
	}

	public boolean onPreferenceClick(Preference preference) {
		
		setResult(RESULT_OK);
		
		if (preference == prefRequestRoot)
		{

			if (prefRequestRoot.isChecked())
			{
				boolean canRoot = RootCommands.rootAccessGiven();
			
				getPreferenceScreen().getPreference(TRANSPROXY_GROUP_IDX).setEnabled(canRoot);
				prefRequestRoot.setChecked(canRoot);

				if (!canRoot)
				{
					Toast.makeText(this, R.string.wizard_permissions_no_root_msg, Toast.LENGTH_LONG).show();
				}
			}
		}
		else if (preference == prefTransProxyApps)
		{
			startActivity(new Intent(this, AppManager.class));
			
		}
		else if (preference == prefHiddenServices)
		{
			
			((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(1).setEnabled(prefHiddenServices.isChecked());
			((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(2).setEnabled(prefHiddenServices.isChecked());
			
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
