/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import org.torproject.android.service.TorServiceUtils;
import org.torproject.android.service.TorTransProxy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;


public class SettingsPreferences 
		extends PreferenceActivity implements OnPreferenceClickListener {

	private CheckBoxPreference prefCBTransProxy = null;
	private CheckBoxPreference prefcBTransProxyAll = null;
	private Preference prefTransProxyApps = null;
	private CheckBoxPreference prefHiddenServices = null;
	
	private boolean hasRoot = false;
	

	private final static int HIDDEN_SERVICE_PREF_IDX = 6;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (prefs.contains("has_root"))
		{
			hasRoot = prefs.getBoolean("has_root",false);
		}
		else
		{
			hasRoot = TorServiceUtils.checkRootAccess();
			Editor pEdit = prefs.edit();
			pEdit.putBoolean("has_root",hasRoot);
			pEdit.commit();
		}
	}
	
	
	@Override
	protected void onResume() {
	
		super.onResume();
	

		int transProxyGroupIdx = 1;
		
		if (!hasRoot)
		{
			getPreferenceScreen().getPreference(transProxyGroupIdx).setEnabled(false);
		}
		else
		{
			prefCBTransProxy = ((CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(transProxyGroupIdx)).getPreference(0));
			prefcBTransProxyAll = (CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(transProxyGroupIdx)).getPreference(1);
			prefTransProxyApps = ((PreferenceCategory)this.getPreferenceScreen().getPreference(transProxyGroupIdx)).getPreference(2);

			prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
			
			prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked() && (!prefcBTransProxyAll.isChecked()));
			
			prefCBTransProxy.setOnPreferenceClickListener(this);
			prefcBTransProxyAll.setOnPreferenceClickListener(this);
			prefTransProxyApps.setOnPreferenceClickListener(this);
			
		}
		
		
		prefHiddenServices = ((CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(0));
		prefHiddenServices.setOnPreferenceClickListener(this);
		((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(1).setEnabled(prefHiddenServices.isChecked());
		((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(2).setEnabled(prefHiddenServices.isChecked());
				
		
	};
	
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		//Log.d(getClass().getName(),"Exiting Preferences");
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		
		setResult(1010);
		
		if (preference == prefTransProxyApps)
		{
			startActivity(new Intent(this, AppManager.class));
			
		}
		else if (preference == prefHiddenServices)
		{
			
			((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(1).setEnabled(prefHiddenServices.isChecked());
			((PreferenceCategory)this.getPreferenceScreen().getPreference(HIDDEN_SERVICE_PREF_IDX)).getPreference(2).setEnabled(prefHiddenServices.isChecked());
			
		}
		else
		{
			prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
			prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked() && (!prefcBTransProxyAll.isChecked()));
			
		}
		
		return true;
	}

	

}
