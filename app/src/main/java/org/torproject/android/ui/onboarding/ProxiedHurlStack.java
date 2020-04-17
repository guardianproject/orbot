/* Copyright (c) 2020, Benjamin Erhart, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */
package org.torproject.android.ui.onboarding;

import com.android.volley.toolbox.HurlStack;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

@SuppressWarnings("WeakerAccess")
public class ProxiedHurlStack extends HurlStack {

    private final String mHost;
    private final int mPort;

    public ProxiedHurlStack(String host, int port) {
        super();

        mHost = host;
        mPort = port;
    }

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                InetSocketAddress.createUnresolved(mHost, mPort));

        return (HttpURLConnection) url.openConnection(proxy);
    }
}
