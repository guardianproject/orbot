package org.torproject.android;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.torproject.android.service.Prefs;
import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.vpn.VPNEnableActivity;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    Prefs.setContext(context);
		if (Prefs.startOnBoot())
		{			

			if (Prefs.useVpn())
				startVpnService(context); //VPN will start Tor once it is done
			else
				startService(TorServiceConstants.ACTION_START, context);
				
		}
	}
	
	public void startVpnService (final Context context)
    	{
		   Intent intent = new Intent(context,VPNEnableActivity.class);
           intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           context.startActivity(intent);
    	}

	private void startService (String action, Context context)
	{
		
		Intent torService = new Intent(context, TorService.class);
		torService.setAction(action);
		context.startService(torService);
		
        
	}
	
	
}

