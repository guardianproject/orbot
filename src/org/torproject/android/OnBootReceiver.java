package org.torproject.android;

import org.torproject.android.service.TorService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction() != null 
				&& intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
		{
			//Phase 1: Launch a service
			Intent service = new Intent();
			service.setAction("onboot");
			service.setClass(context, TorService.class);
			context.startService(service);
		}
	
		
	}

	
}

