package org.torproject.android;

import org.torproject.android.service.TorService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction() != null 
				&& intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
		{
			SharedPreferences prefs = TorService.getSharedPrefs(context.getApplicationContext());
			
			boolean startOnBoot = prefs.getBoolean("pref_start_boot",false);
			
			if (startOnBoot)
			{
				//Phase 1: Launch a service
				Intent service = new Intent();
				service.setAction("onboot");
				service.setClass(context, TorService.class);
				context.startService(service);
			}
		}
	
		
	}

	
}

