/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.torproject.android.service.vpn;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;

import org.apache.commons.io.IOUtils;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.R;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.CustomNativeLoader;
import org.torproject.android.service.util.Prefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.torproject.android.service.TorServiceConstants.ACTION_START;
import static org.torproject.android.service.TorServiceConstants.ACTION_START_VPN;
import static org.torproject.android.service.TorServiceConstants.ACTION_STOP_VPN;

public class OrbotVpnManager implements Handler.Callback {
    private static final String TAG = "OrbotVpnService";
    private final static int VPN_MTU = 1500;
    private final static boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    private final static String PDNSD_BIN = "pdnsd";
    public static int sSocksProxyServerPort = -1;
    public static String sSocksProxyLocalhost = null;
    boolean isStarted = false;
    File filePdnsPid;
    private Thread mThreadVPN;
    private final static String mSessionName = "OrbotVPN";
    private ParcelFileDescriptor mInterface;
    private int mTorSocks = -1;
    private int mTorDns = -1;
    private int pdnsdPort = 8091;
    private ProxyServer mSocksProxyServer;
    private final File filePdnsd;
    private boolean isRestart = false;
    private final VpnService mService;

    public OrbotVpnManager(VpnService service) throws IOException, TimeoutException {
        mService = service;
        filePdnsd = CustomNativeLoader.loadNativeBinary(service.getApplicationContext(), PDNSD_BIN, new File(service.getFilesDir(), PDNSD_BIN));
        Tun2Socks.init();
    }

    public static File makePdnsdConf(Context context, File fileDir, String torDnsHost, int torDnsPort, String pdnsdHost, int pdnsdPort) throws IOException {
        String conf = String.format(context.getString(R.string.pdnsd_conf), torDnsHost, torDnsPort, fileDir.getCanonicalPath(), pdnsdHost, pdnsdPort);

        Log.d(TAG, "pdsnd conf:" + conf);

        File fPid = new File(fileDir, pdnsdPort + "pdnsd.conf");

        if (fPid.exists()) {
            fPid.delete();
        }

        FileOutputStream fos = new FileOutputStream(fPid, false);
        PrintStream ps = new PrintStream(fos);
        ps.print(conf);
        ps.close();

        File cache = new File(fileDir, "pdnsd.cache");

        if (!cache.exists()) {
            try {
                cache.createNewFile();
            } catch (Exception e) {
            }
        }
        return fPid;
    }

    public int handleIntent(VpnService.Builder builder, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action)) {
                if (action.equals(ACTION_START_VPN) || action.equals(ACTION_START)) {
                    Log.d(TAG, "starting VPN");

                    isStarted = true;

                    // Stop the previous session by interrupting the thread.
                    if (mThreadVPN != null && mThreadVPN.isAlive())
                        stopVPN();

                    if (mTorSocks != -1) {
                        if (!mIsLollipop) {
                            startSocksBypass();
                        }

                        setupTun2Socks(builder);
                    }

                } else if (action.equals(ACTION_STOP_VPN)) {
                    isStarted = false;

                    Log.d(TAG, "stopping VPN");

                    stopVPN();
                } else if (action.equals(TorServiceConstants.LOCAL_ACTION_PORTS)) {
                    Log.d(TAG, "setting VPN ports");

                    int torSocks = intent.getIntExtra(OrbotService.EXTRA_SOCKS_PROXY_PORT, -1);
                    int torDns = intent.getIntExtra(OrbotService.EXTRA_DNS_PORT, -1);

                    //if running, we need to restart
                    if ((torSocks != mTorSocks || torDns != mTorDns)) {

                        mTorSocks = torSocks;
                        mTorDns = torDns;

                        if (!mIsLollipop) {
                            stopSocksBypass();
                            startSocksBypass();
                        }

                        setupTun2Socks(builder);
                    }
                }
            }

        }


        return Service.START_STICKY;
    }

    private void startSocksBypass() {
        new Thread() {
            public void run() {

                //generate the proxy port that the
                if (sSocksProxyServerPort == -1) {
                    try {

                        sSocksProxyLocalhost = "127.0.0.1";// InetAddress.getLocalHost().getHostAddress();
                        sSocksProxyServerPort = (int) ((Math.random() * 1000) + 10000);

                    } catch (Exception e) {
                        Log.e(TAG, "Unable to access localhost", e);
                        throw new RuntimeException("Unable to access localhost: " + e);

                    }

                }


                if (mSocksProxyServer != null) {
                    stopSocksBypass();
                }

                try {
                    mSocksProxyServer = new ProxyServer(new ServerAuthenticatorNone(null, null));
                    ProxyServer.setVpnService(mService);
                    mSocksProxyServer.start(sSocksProxyServerPort, 5, InetAddress.getLocalHost());

                } catch (Exception e) {
                    Log.e(TAG, "error getting host", e);
                }
            }
        }.start();
    }

    private synchronized void stopSocksBypass() {
        if (mSocksProxyServer != null) {
            mSocksProxyServer.stop();
            mSocksProxyServer = null;
        }
    }

    private void stopVPN() {
        if (mIsLollipop)
            stopSocksBypass();

        Tun2Socks.Stop();

        if (mInterface != null) {
            try {
                Log.d(TAG, "closing interface, destroying VPN interface");

                mInterface.close();
                mInterface = null;

            } catch (Exception e) {
                Log.d(TAG, "error stopping tun2socks", e);
            } catch (Error e) {
                Log.d(TAG, "error stopping tun2socks", e);
            }
        }
        stopDns();
        mThreadVPN = null;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(mService, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private synchronized void setupTun2Socks(final VpnService.Builder builder) {
        if (mInterface != null) //stop tun2socks now to give it time to clean up
        {
            isRestart = true;
            Tun2Socks.Stop();

            stopDns();

        }

        mThreadVPN = new Thread() {

            public void run() {
                try {

                    if (isRestart) {
                        Log.d(TAG, "is a restart... let's wait for a few seconds");
                        Thread.sleep(3000);
                    }

                    final String vpnName = "OrbotVPN";
                    final String localhost = "127.0.0.1";

                    final String virtualGateway = "192.168.200.1";
                    final String virtualIP = "192.168.200.2";
                    final String virtualNetMask = "255.255.255.0";
                    final String dummyDNS = "1.1.1.1"; //this is intercepted by the tun2socks library, but we must put in a valid DNS to start
                    final String defaultRoute = "0.0.0.0";

                    final String localSocks = localhost + ':' + mTorSocks;

                    builder.setMtu(VPN_MTU);
                    builder.addAddress(virtualGateway, 32);

                    builder.setSession(vpnName);

                    //route all traffic through VPN (we might offer country specific exclude lists in the future)
                    builder.addRoute(defaultRoute, 0);

                    builder.addDnsServer(dummyDNS);
                    builder.addRoute(dummyDNS, 32);

                    //handle ipv6
                    //builder.addAddress("fdfe:dcba:9876::1", 126);
                    //builder.addRoute("::", 0);

                    if (mIsLollipop)
                        doLollipopAppRouting(builder);

                    // https://developer.android.com/reference/android/net/VpnService.Builder#setMetered(boolean)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        builder.setMetered(false);
                    }

                    // Create a new interface using the builder and save the parameters.
                    ParcelFileDescriptor newInterface = builder.setSession(mSessionName)
                            .setConfigureIntent(null) // previously this was set to a null member variable
                            .establish();

                    if (mInterface != null) {
                        Log.d(TAG, "Stopping existing VPN interface");
                        mInterface.close();
                        mInterface = null;
                    }

                    mInterface = newInterface;

                    isRestart = false;

                    //start PDNSD daemon pointing to actual DNS
                    if (filePdnsd != null) {

                        pdnsdPort++;
                        startDNS(filePdnsd.getCanonicalPath(), localhost, mTorDns, virtualGateway, pdnsdPort);
                        final boolean localDnsTransparentProxy = true;

                        Tun2Socks.Start(mService, mInterface, VPN_MTU, virtualIP, virtualNetMask, localSocks, virtualGateway + ":" + pdnsdPort, localDnsTransparentProxy);
                    }

                } catch (Exception e) {
                    Log.d(TAG, "tun2Socks has stopped", e);
                }
            }

        };

        mThreadVPN.start();

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void doLollipopAppRouting(VpnService.Builder builder) throws NameNotFoundException {
        SharedPreferences prefs = Prefs.getSharedPrefs(mService.getApplicationContext());
        ArrayList<TorifiedApp> apps = TorifiedApp.getApps(mService, prefs);


        boolean perAppEnabled = false;

        for (TorifiedApp app : apps) {
            if (app.isTorified() && (!app.getPackageName().equals(mService.getPackageName()))) {
                if (prefs.getBoolean(app.getPackageName() + OrbotConstants.APP_TOR_KEY, true)) {

                    builder.addAllowedApplication(app.getPackageName());

                }

                perAppEnabled = true;

            }
        }

        if (!perAppEnabled)
            builder.addDisallowedApplication(mService.getPackageName());

    }

    private void startDNS(String pdnsPath, String torDnsHost, int torDnsPort, String pdnsdHost, int pdnsdPort) throws IOException, TimeoutException {

        File fileConf = makePdnsdConf(mService, mService.getFilesDir(), torDnsHost, torDnsPort, pdnsdHost, pdnsdPort);

        String[] cmdString = {pdnsPath, "-c", fileConf.toString(), "-g", "-v2"};
        ProcessBuilder pb = new ProcessBuilder(cmdString);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        try {
            proc.waitFor();
        } catch (Exception e) {
        }

        Log.i(TAG, "PDNSD: " + proc.exitValue());

        if (proc.exitValue() != 0) {
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                Log.d(TAG, "pdnsd: " + line);
            }
        }


    }

    private void stopDns() {
        if (filePdnsPid != null && filePdnsPid.exists()) {
            List<String> lines;
            try {
                lines = IOUtils.readLines(new FileReader(filePdnsPid));
                String dnsPid = lines.get(0);
                VpnUtils.killProcess(dnsPid, "");
                filePdnsPid.delete();
                filePdnsPid = null;
            } catch (Exception e) {
                Log.e("OrbotVPN", "error killing dns process", e);
            }
        }
    }

    public boolean isStarted() {
        return isStarted;
    }
}
