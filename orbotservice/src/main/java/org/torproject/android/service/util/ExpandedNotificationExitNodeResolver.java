package org.torproject.android.service.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.torproject.android.service.OrbotRawEventListener;
import org.torproject.android.service.OrbotService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class ExpandedNotificationExitNodeResolver implements Runnable {

    private static final String ONIONOO_BASE_URL = "https://onionoo.torproject.org/details?fields=country_name,or_addresses&lookup=";
    private static final int CONNECTION_TIMEOUT_MS = 60000;
    private final OrbotService mService;
    private final int mLocalHttpProxyPort;
    private final OrbotRawEventListener.ExitNode exitNode;

    public ExpandedNotificationExitNodeResolver(OrbotService service, int localProxyPort, OrbotRawEventListener.ExitNode exitNode) {
        mService = service;
        mLocalHttpProxyPort = localProxyPort;
        this.exitNode = exitNode;
    }

    @Override
    public void run() {
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", mLocalHttpProxyPort));
            URLConnection conn = new URL(ONIONOO_BASE_URL + exitNode.fingerPrint).openConnection(proxy);

            conn.setRequestProperty("Connection", "Close");
            conn.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            conn.setReadTimeout(CONNECTION_TIMEOUT_MS);

            InputStream is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
                json.append(line);

            JSONObject jsonNodeInfo = new org.json.JSONObject(json.toString());
            JSONArray jsonRelays = jsonNodeInfo.getJSONArray("relays");

            if (jsonRelays.length() > 0) {
                exitNode.country = jsonRelays.getJSONObject(0).getString("country_name");
                exitNode.ipAddress = jsonRelays.getJSONObject(0).getJSONArray("or_addresses").getString(0).split(":")[0];
                mService.setNotificationSubtext(exitNode.ipAddress + " | " + exitNode.country);
            }

            reader.close();
            is.close();
        } catch (Exception e) {
            Log.d(ExpandedNotificationExitNodeResolver.class.getSimpleName(), "error fingerprint=" + exitNode.fingerPrint);
            Log.d(ExpandedNotificationExitNodeResolver.class.getSimpleName(), e.getMessage());
        }
    }
}
