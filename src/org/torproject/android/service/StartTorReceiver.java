
package org.torproject.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class StartTorReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        /* sanitize the Intent before forwarding it to TorService */
        if (TextUtils.equals(intent.getAction(), TorServiceConstants.ACTION_START)) {
            Intent startTorService = new Intent(context, TorService.class);
            startTorService.setAction(intent.getAction());
            context.startService(startTorService);
        }
    }
}
