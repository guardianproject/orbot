package org.torproject.android.service;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import org.torproject.android.Prefs;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    Prefs.setContext(context);
		if (Prefs.startOnBoot())
		{
			startService(TorServiceConstants.ACTION_START, context);

			if (Prefs.useVpn())
				startVpnService(context);
		}
	}
	
	 @SuppressLint("NewApi")
	public void startVpnService (Context context)
    	{
       		Intent intent = VpnService.prepare(context);

		if (intent != null) {
        		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

