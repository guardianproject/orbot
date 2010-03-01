/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import org.torproject.android.service.TorServiceUtils;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsPreferences 
		extends PreferenceActivity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		if (!TorServiceUtils.hasRoot())
			getPreferenceScreen().getPreference(3).setEnabled(false);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		Log.i(getClass().getName(),"Exiting Preferences");
	}

}
