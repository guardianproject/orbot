package org.torproject.android;

import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(context.getApplicationContext());
		
		boolean startOnBoot = prefs.getBoolean("pref_start_boot",true);
		
		if (startOnBoot)
		{
			startService("init",context);
			startService("start",context);			
		}

	}
	

	private void startService (String action, Context context)
	{
		
		Intent torService = new Intent(context, TorService.class);    
		torService.setAction(action);
		context.startService(torService);
		
        
	}
	
	
}

