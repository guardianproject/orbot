package org.torproject.android;

import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {

		//Log.i(TorService.TAG,"got boot receiver event");
		
		if (intent.getAction() != null 
				&& Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
		{
		
			//Log.i(TorService.TAG,"boot is completed");
			
			SharedPreferences prefs = TorServiceUtils.getSharedPrefs(context.getApplicationContext());
			
			boolean startOnBoot = prefs.getBoolean("pref_start_boot",true);
			
		//	Log.i(TorService.TAG,"start on boot: " + startOnBoot);
			
			
			if (startOnBoot)
			{
				Intent torService = new Intent(context.getApplicationContext(), TorService.class);
				torService.setAction(Intent.ACTION_BOOT_COMPLETED);
				context.getApplicationContext().startService(torService);
				
			}
		}
	
		
	}

	
	
}

