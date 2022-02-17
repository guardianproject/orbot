/* Copyright (c) 2020, Benjamin Erhart, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */
package org.torproject.android.ui.onboarding;

import android.util.Log;

import com.android.volley.toolbox.HurlStack;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

@SuppressWarnings("WeakerAccess")
public class ProxiedHurlStack extends HurlStack {

    private final String mHost;
    private final int mPort;
    private final String mUsername;
    private final String mPassword;

    public ProxiedHurlStack(String host, int port, String username, String password) {
        super();

        mHost = host;
        mPort = port;
        mUsername = username;
        mPassword = password;
    }

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                InetSocketAddress.createUnresolved(mHost, mPort));

        if (mUsername != null && mPassword != null) {
            Log.d(getClass().getSimpleName(), String.format("mHost=%s, mPort=%d, mUsername=%s, mPassword=%s", mHost, mPort, mUsername, mPassword));

            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    Log.d(getClass().getSimpleName(), "getPasswordAuthentication!");

                    return new PasswordAuthentication(mUsername, mPassword.toCharArray());
                }
            };

            Authenticator.setDefault(authenticator);
        }

        return (HttpURLConnection) url.openConnection(proxy);
    }
}
