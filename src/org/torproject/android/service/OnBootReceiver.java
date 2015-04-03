package org.torproject.android.service;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(context.getApplicationContext());
		
		boolean startOnBoot = prefs.getBoolean("pref_start_boot",true);
		boolean useVPN = prefs.getBoolean("pref_vpn",true);
		
		if (startOnBoot)
		{
			
			startService(TorServiceConstants.CMD_INIT,context);
			startService(TorServiceConstants.CMD_START,context);		
			
			if (useVPN)
				startVpnService(context);
			
		}

	}
	
	 @SuppressLint("NewApi")
	public void startVpnService (Context context)
    {
        Intent intent = VpnService.prepare(context);
       // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent != null) {
            context.startActivity(intent);
        } 

    }

	private void startService (String action, Context context)
	{
		
		Intent torService = new Intent(context, TorService.class);    
		torService.setAction(action);
		context.startService(torService);
		
        
	}
	
	
}

