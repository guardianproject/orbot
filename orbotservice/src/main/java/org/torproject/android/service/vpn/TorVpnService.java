package org.torproject.android.service.vpn;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;

/**
 * Created by n8fr8 on 9/26/16.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class TorVpnService extends VpnService {



    /* (non-Javadoc)
    * @see android.app.Service#onStart(android.content.Intent, int)
    */
    public int onStartCommand(Intent intent, int flags, int startId) {



        if (!TextUtils.isEmpty(intent.getAction()))
        {
            if (intent.getAction().equals("start"))
                enableVpnProxy();;
        }

        return Service.START_STICKY;
    }


    public void enableVpnProxy () {
       // debug ("enabling VPN Proxy");

        OrbotVpnManager vpnManager = new OrbotVpnManager(this);

        int portSocks = 9050;

        Prefs.putUseVpn(true);
       // processTransparentProxying();

        //updateConfiguration("DNSPort",TOR_VPN_DNS_LISTEN_ADDRESS + ":" + TorServiceConstants.TOR_DNS_PORT_DEFAULT,false);

        //  if (mVpnManager == null)
        //   	mVpnManager = new OrbotVpnManager (this);

        Intent intent = new Intent();
        intent.setAction("start");
        intent.putExtra("torSocks", portSocks);

        vpnManager.handleIntent(new Builder(),intent);

    }
}
