
package org.torproject.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import org.torproject.android.service.util.Prefs;


public class StartTorReceiver extends BroadcastReceiver implements TorServiceConstants {

    @Override
    public void onReceive(Context context, Intent intent) {
        /* sanitize the Intent before forwarding it to TorService */
        Prefs.setContext(context);
        String action = intent.getAction();
        if (TextUtils.equals(action, ACTION_START)) {
            String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            if (Prefs.allowBackgroundStarts()) {
                Intent startTorIntent = new Intent(context, TorService.class);
                startTorIntent.setAction(action);
                if (packageName != null)
                    startTorIntent.putExtra(EXTRA_PACKAGE_NAME, packageName);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(startTorIntent);
                }
                else
                {
                    context.startService(startTorIntent);
                }
            } else if (!TextUtils.isEmpty(packageName)) {
                // let the requesting app know that the user has disabled
                // starting via Intent
                Intent startsDisabledIntent = new Intent(ACTION_STATUS);
                startsDisabledIntent.putExtra(EXTRA_STATUS, STATUS_STARTS_DISABLED);
                startsDisabledIntent.setPackage(packageName);
                context.sendBroadcast(startsDisabledIntent);
            }
        }
    }
}
