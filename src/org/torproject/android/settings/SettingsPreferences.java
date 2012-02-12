/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.settings;

import org.torproject.android.R;
import org.torproject.android.R.xml;
import org.torproject.android.TorConstants;
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
import android.util.Log;
import android.widget.Toast;


public class SettingsPreferences 
		extends PreferenceActivity implements OnPreferenceClickListener {

	private CheckBoxPreference prefCBTransProxy = null;
	private CheckBoxPreference prefcBTransProxyAll = null;
	private Preference prefTransProxyApps = null;
	private CheckBoxPreference prefHiddenServices = null;
	private CheckBoxPreference prefRequestRoot = null;
	
	private boolean hasRoot = false;
	

	private final static int HIDDEN_SERVICE_PREF_IDX = 6;
	private final static int TRANSPROXY_GROUP_IDX = 1;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		hasRoot = prefs.getBoolean("has_root",false);
		
		if (!hasRoot)
		{
			hasRoot = prefs.getBoolean("use_whispercore", false);
			
		}
	}
	
	
	@Override
	protected void onResume() {
	
		super.onResume();
	
		int REQUEST_ROOT_IDX = 1;
		int GENERAL_GROUP_IDX = 0;
		
		prefRequestRoot = ((CheckBoxPreference)((PreferenceCategory)getPreferenceScreen().getPreference(GENERAL_GROUP_IDX)).getPreference(REQUEST_ROOT_IDX));
		prefRequestRoot.setOnPreferenceClickListener(this);

		prefCBTransProxy = ((CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(TRANSPROXY_GROUP_IDX)).getPreference(0));
		prefcBTransProxyAll = (CheckBoxPreference)((PreferenceCategory)this.getPreferenceScreen().getPreference(TRANSPROXY_GROUP_IDX)).getPreference(1);
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
	
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		//Log.d(getClass().getName(),"Exiting Preferences");
	}

	public boolean onPreferenceClick(Preference preference) {
		
		setResult(1010);
		
		if (preference == prefRequestRoot)
		{

			if (prefRequestRoot.isChecked())
			{
				//boolean canRoot = TorServiceUtils.isRootPossible();
				boolean canRoot;
				
				try
				{
					StringBuilder res = new StringBuilder();
					String[] cmd = {"ls /data/data"}; //only root can do this!
					int code = TorServiceUtils.doShellCommand(cmd, res, true, true);		
					canRoot = code > -1;
				}
				catch (Exception e)
				{
					//probably not root
					canRoot = false;
				}
				
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
		else
		{
			prefcBTransProxyAll.setEnabled(prefCBTransProxy.isChecked());
			prefTransProxyApps.setEnabled(prefCBTransProxy.isChecked() && (!prefcBTransProxyAll.isChecked()));
			
			if (!prefCBTransProxy.isChecked())
				clearTransProxyState ();
			
		}
		
		return true;
	}

	private void clearTransProxyState ()
	{
		try {
			new TorTransProxy().flushIptables(this);
		} catch (Exception e) {
			Log.e(TorConstants.TAG,"error flushing iptables",e);
		}
	}

}
