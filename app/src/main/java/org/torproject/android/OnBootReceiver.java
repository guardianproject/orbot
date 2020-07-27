package org.torproject.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;

public class OnBootReceiver extends BroadcastReceiver {

	private static boolean sReceivedBoot = false;

	@Override
	public void onReceive(Context context, Intent intent) {

		if (Prefs.startOnBoot() && (!sReceivedBoot))
		{
			startService(TorServiceConstants.ACTION_START_ON_BOOT, context);
			sReceivedBoot = true;
		}
	}


	private void startService (String action, Context context)
	{
		Intent intent = new Intent(context, OrbotService.class);
		intent.setAction(action);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(intent);
		}
		else
		{
			context.startService(intent);
		}

	}


}

