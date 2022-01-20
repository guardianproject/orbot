package org.torproject.android.service.util;

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

public class ExternalIPFetcher implements Runnable {

    private static final String ONIONOO_BASE_URL = "https://onionoo.torproject.org/details?fields=country_name,as_name,or_addresses&lookup=";
    private final OrbotService mService;
    private final OrbotRawEventListener.Node mNode;
    private final int mLocalHttpProxyPort;

    public ExternalIPFetcher(OrbotService service, OrbotRawEventListener.Node node, int localProxyPort) {
        mService = service;
        mNode = node;
        mLocalHttpProxyPort = localProxyPort;
    }

    @Override
    public void run() {
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", mLocalHttpProxyPort));
            URLConnection conn = new URL(ONIONOO_BASE_URL + mNode.id).openConnection(proxy);

            conn.setRequestProperty("Connection", "Close");
            conn.setConnectTimeout(60000);
            conn.setReadTimeout(60000);

            InputStream is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // getting JSON string from URL

            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
                json.append(line);

            JSONObject jsonNodeInfo = new org.json.JSONObject(json.toString());

            JSONArray jsonRelays = jsonNodeInfo.getJSONArray("relays");

            if (jsonRelays.length() > 0) {
                mNode.ipAddress = jsonRelays.getJSONObject(0).getJSONArray("or_addresses").getString(0).split(":")[0];
                mNode.country = jsonRelays.getJSONObject(0).getString("country_name");
                mNode.organization = jsonRelays.getJSONObject(0).getString("as_name");

                StringBuilder sbInfo = new StringBuilder();
                sbInfo.append(mNode.name).append("(");
                sbInfo.append(mNode.ipAddress).append(")");

                if (mNode.country != null)
                    sbInfo.append(' ').append(mNode.country);

                if (mNode.organization != null)
                    sbInfo.append(" (").append(mNode.organization).append(')');

                mService.debug(sbInfo.toString());
            }

            reader.close();
            is.close();
        } catch (Exception e) {
            //    mService.debug ("Error getting node details from onionoo: " + e.getMessage());
        }
    }
}