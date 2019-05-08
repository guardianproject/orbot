package org.torproject.android.service.vpn;

import android.app.Service;
import android.content.Intent;
import android.net.VpnService;

/**
 * Created by n8fr8 on 9/26/16.
 */
public class TorVpnService extends VpnService {
    OrbotVpnManager mVpnManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mVpnManager = new OrbotVpnManager(this);
    }

    /* (non-Javadoc)
     * @see android.app.Service#onStart(android.content.Intent, int)
     */
    public int onStartCommand(Intent intent, int flags, int startId) {

        mVpnManager.handleIntent(new Builder(), intent);

        return Service.START_STICKY;
    }
}
