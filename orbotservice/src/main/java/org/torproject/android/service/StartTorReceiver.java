package org.torproject.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import org.torproject.android.service.util.Prefs;


public class StartTorReceiver extends BroadcastReceiver implements OrbotConstants {

    @Override
    public void onReceive(Context context, Intent intent) {
        /* sanitize the Intent before forwarding it to OrbotService */
        Prefs.setContext(context);
        String action = intent.getAction();
        if (TextUtils.equals(action, ACTION_START)) {
            String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            if (Prefs.allowBackgroundStarts()) {
                Intent startTorIntent = new Intent(context, OrbotService.class)
                        .setAction(action);
                if (packageName != null) {
                    startTorIntent.putExtra(OrbotService.EXTRA_PACKAGE_NAME, packageName);
                }
                ContextCompat.startForegroundService(context, startTorIntent);
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
