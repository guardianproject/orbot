package org.torproject.android.boot;

import org.torproject.android.service.ITorService;
import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class OnbootBroadcastReceiver extends BroadcastReceiver implements TorServiceConstants {
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.d(TAG, "received on boot notification");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean startOnBoot = prefs.getBoolean("pref_start_boot",true);
		
		Log.d(TAG, "startOnBoot:" + startOnBoot);
		
		if (startOnBoot)
		{
			Intent serviceIntent = new Intent(context,TorService.class);
			serviceIntent.setAction("onboot");
			context.startService(serviceIntent);
		}
		
		//bindService(new Intent(ITorService.class.getName()),
          //      mConnection, Context.BIND_AUTO_CREATE);
	}

}