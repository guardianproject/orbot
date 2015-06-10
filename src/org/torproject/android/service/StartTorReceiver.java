
package org.torproject.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import org.torproject.android.Prefs;

public class StartTorReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        /* sanitize the Intent before forwarding it to TorService */
        Prefs.setContext(context);
        String action = intent.getAction();
        if (Prefs.allowBackgroundStarts()
                && TextUtils.equals(action, TorServiceConstants.ACTION_START)) {
            Intent startTorIntent = new Intent(context, TorService.class);
            startTorIntent.setAction(action);
            String packageName = intent.getStringExtra(TorServiceConstants.EXTRA_PACKAGE_NAME);
            if (packageName != null)
                startTorIntent.putExtra(TorServiceConstants.EXTRA_PACKAGE_NAME, packageName);
            context.startService(startTorIntent);
        }
    }
}
