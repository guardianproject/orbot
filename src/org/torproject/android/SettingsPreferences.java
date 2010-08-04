/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import org.torproject.android.service.TorServiceUtils;
import org.torproject.android.service.TorTransProxy;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;


public class SettingsPreferences 
		extends PreferenceActivity implements OnPreferenceClickListener {

	private CheckBoxPreference prefCBTransProxy = null;
	private CheckBoxPreference prefcBTransProxyAll = null;
	private Preference prefTransProxyApps = null;
	private Preference prefWebProxy = null;
	
	
	
	private boolean hasRoot = false;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		hasRoot = TorTransProxy.hasRootAccess();
		
	}
	
	
	
	@Override
	protected void onResume() {
	
		super.onResume();
	
		if (!hasRoot)
		{
			getPreferenceScreen().getPreference(0).setEnabled(false);
		}
		else
		{
			prefCBTransProxy = ((CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(0)).getPreference(0));
			prefcBTransProxyAll = (CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(0)).getPreference(1);
			prefTransProxyApps = ((PreferenceCategory)this.getPreferenceScreen().getPreference(0)).getPreference(2);

			prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
			
			prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked() && (!prefcBTransProxyAll.isChecked()));
			
			prefCBTransProxy.setOnPreferenceClickListener(this);
			prefcBTransProxyAll.setOnPreferenceClickListener(this);
			prefTransProxyApps.setOnPreferenceClickListener(this);
			
		}
		
		//disabled for now 28/07 nf
		//prefWebProxy = ((PreferenceCategory)this.getPreferenceScreen().getPreference(1)).getPreference(0);
		//prefWebProxy.setOnPreferenceClickListener(this);
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		//Log.i(getClass().getName(),"Exiting Preferences");
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		
		
		if (preference == prefTransProxyApps)
		{
			startActivity(new Intent(this, AppManager.class));
		}
		/*
		else if (preference == prefWebProxy)
		{
			 Intent intent = new Intent();
			 intent.setClassName(this,"com.android.settings.ProxySelector");
			 intent.putExtra("title", "Set host=127.0.0.1 and port=8118");
			 intent.putExtra("button-label", "Save");
			 
			 startActivity(intent);
			 
			 
		}*/
		else
		{
			prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
			prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked() && (!prefcBTransProxyAll.isChecked()));
			
		}
		
		return true;
	}

	

}
