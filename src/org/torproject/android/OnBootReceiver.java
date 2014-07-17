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

		if (intent.getAction() != null 
				&& Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
		{
			
			SharedPreferences prefs = TorServiceUtils.getSharedPrefs(context.getApplicationContext());
			
			boolean startOnBoot = prefs.getBoolean("pref_start_boot",false);
			
			if (startOnBoot)
			{
				Intent torService = new Intent(context.getApplicationContext(), TorService.class);
				torService.setAction(Intent.ACTION_BOOT_COMPLETED);
				context.getApplicationContext().startService(torService);
				
			}
		}
	
		
	}

	
	
}

