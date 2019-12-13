package org.torproject.android.service.vpn;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by n8fr8 on 9/26/16.
 */
public class TorVpnService extends VpnService {

    public static final String TAG = "TorVpnService";

    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    public static void start(Context context) {
        Intent intent = new Intent(context, TorVpnService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, TorVpnService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    OrbotVpnManager mVpnManager;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mVpnManager = new OrbotVpnManager(this);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }


    }

    /* (non-Javadoc)
     * @see android.app.Service#onStart(android.content.Intent, int)
     */
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (ACTION_START.equals(intent.getAction()))
        {
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
            lbm.registerReceiver(mLocalBroadcastReceiver,
                    new IntentFilter(TorServiceConstants.LOCAL_ACTION_PORTS));
        }
        else if (ACTION_STOP.equals(intent.getAction()))
        {
            Log.d(TAG, "clearing VPN Proxy");
            Prefs.putUseVpn(false);

            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
            lbm.unregisterReceiver(mLocalBroadcastReceiver);
        }

        mVpnManager.handleIntent(new Builder(), intent);

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    /**
     * The state and log info from {@link OrbotService} are sent to the UI here in
     * the form of a local broadcast. Regular broadcasts can be sent by any app,
     * so local ones are used here so other apps cannot interfere with Orbot's
     * operation.
     */
    private BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;

            if (action.equals(TorServiceConstants.LOCAL_ACTION_PORTS)) {

                mVpnManager.handleIntent(new Builder(),intent);
            }
        }
    };


}
