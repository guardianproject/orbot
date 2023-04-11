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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ProxyInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.UdpPort;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.R;
import org.torproject.android.service.util.Prefs;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import androidx.annotation.ChecksSdkIntAtLeast;

import IPtProxy.IPtProxy;
import IPtProxy.PacketFlow;

public class OrbotVpnManager implements Handler.Callback, OrbotConstants {
    private static final String TAG = "OrbotVpnService";
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
    private final static boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    public static int sSocksProxyServerPort = -1;
    public static String sSocksProxyLocalhost = null;
    boolean isStarted = false;
    private final static String mSessionName = "OrbotVPN";
    private ParcelFileDescriptor mInterface;
    private int mTorSocks = -1;
    private int mTorDns = -1;
    private int mTorHttp = -1;
    private ProxyServer mSocksProxyServer;
    private final VpnService mService;
    private final SharedPreferences prefs;
    private DNSResolver mDnsResolver;

    private final ExecutorService mExec = Executors.newFixedThreadPool(10);
    private Thread mThreadPacket;
    private boolean keepRunningPacket = false;

    private FileInputStream fis;
    private DataOutputStream fos;

    private static final int DELAY_FD_LISTEN_MS = 5000;

    public OrbotVpnManager(OrbotService service) {
        mService = service;
        prefs = Prefs.getSharedPrefs(mService.getApplicationContext());
    }

    public int handleIntent(VpnService.Builder builder, Intent intent) {
        if (intent != null) {
            var action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_START_VPN) || action.equals(ACTION_START) ) {
                    Log.d(TAG, "starting VPN");
                    isStarted = true;
                } else if (action.equals(ACTION_STOP_VPN) || action.equals(ACTION_STOP)) {
                    isStarted = false;
                    Log.d(TAG, "stopping VPN");
                    stopVPN();

                    //reset ports
                    mTorSocks = -1;
                    mTorDns = -1;

                } else if (action.equals(OrbotConstants.LOCAL_ACTION_PORTS)) {
                    Log.d(TAG, "setting VPN ports");

                    int torSocks = intent.getIntExtra(OrbotService.EXTRA_SOCKS_PROXY_PORT, -1);
                    int torHttp = intent.getIntExtra(OrbotService.EXTRA_HTTP_PROXY_PORT,-1);
                    int torDns = intent.getIntExtra(OrbotService.EXTRA_DNS_PORT, -1);

                    //if running, we need to restart
                    if ((torSocks != -1 && torSocks != mTorSocks
                            && torDns != -1 && torDns != mTorDns)) {

                        mTorHttp = torHttp;
                        mTorSocks = torSocks;
                        mTorDns = torDns;

                        if (!mIsLollipop) {
                            startSocksBypass();
                        }

                        setupTun2Socks(builder);
                    }
                }
            }
        }
        return Service.START_STICKY;
    }

    public void restartVPN (VpnService.Builder builder) {
        stopVPN();
        if (!mIsLollipop) {
            startSocksBypass();
        }
        setupTun2Socks(builder);
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
        if (!mIsLollipop)
            stopSocksBypass();

        keepRunningPacket = false;

        if (mInterface != null) {
            try {
                Log.d(TAG, "closing interface, destroying VPN interface");
                IPtProxy.stopSocks();
                if (fis != null) {
                    fis.close();
                    fis = null;
                }

                if (fos != null) {
                    fos.close();
                    fos = null;
                }

                mInterface.close();
                mInterface = null;
            } catch (Exception e) {
                Log.d(TAG, "error stopping tun2socks", e);
            } catch (Error e) {
                Log.d(TAG, "error stopping tun2socks", e);
            }
        }

        if (mThreadPacket != null && mThreadPacket.isAlive()) {
            mThreadPacket.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(mService, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public final static String FAKE_DNS = "10.10.10.10";

    private synchronized void setupTun2Socks(final VpnService.Builder builder) {
        try {
            final String localhost = "127.0.0.1";
            final String defaultRoute = "0.0.0.0";
            final String virtualGateway = "192.168.50.1";

            //    builder.setMtu(VPN_MTU);
            //   builder.addAddress(virtualGateway, 32);
            builder.addAddress(virtualGateway, 24)
                .addRoute(defaultRoute, 0)
                .setSession(mService.getString(R.string.orbot_vpn))
                .addDnsServer(FAKE_DNS) //just setting a value here so DNS is captured by TUN interface
                .addRoute(FAKE_DNS, 32);

            //route all traffic through VPN (we might offer country specific exclude lists in the future)
            //    builder.addRoute(defaultRoute, 0);


            //handle ipv6
            //builder.addAddress("fdfe:dcba:9876::1", 126);
            //builder.addRoute("::", 0);

            /**
             * Can't use this since our HTTP proxy is only CONNECT and not a full proxy
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setHttpProxy(ProxyInfo.buildDirectProxy("localhost",mTorHttp));
            }**/

            if (mIsLollipop)
                doLollipopAppRouting(builder);

            // https://developer.android.com/reference/android/net/VpnService.Builder#setMetered(boolean)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false);


                /**
                 // Allow applications to bypass the VPN
                 builder.allowBypass();

                 // Explicitly allow both families, so we do not block
                 // traffic for ones without DNS servers (issue 129).
                 builder.allowFamily(OsConstants.AF_INET);
                 builder.allowFamily(OsConstants.AF_INET6);
                 **/
            }

             builder.setSession(mSessionName)
                    .setConfigureIntent(null); // previously this was set to a null member variable

            if (mIsLollipop)
                builder.setBlocking(true);

            mInterface = builder.establish();

            mDnsResolver = new DNSResolver(mTorDns);

            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                try {
                    startListeningToFD();
                } catch (IOException e) {
                    Log.d(TAG, "VPN tun listening has stopped", e);
                }
            }, DELAY_FD_LISTEN_MS);

        } catch (Exception e) {
            Log.d(TAG, "VPN tun setup has stopped", e);
        }
    }

    private void startListeningToFD() throws IOException {
        if (mInterface == null) return; // Prepare hasn't been called yet

        fis = new FileInputStream(mInterface.getFileDescriptor());
        fos = new DataOutputStream(new FileOutputStream(mInterface.getFileDescriptor()));

        //write packets back out to TUN
        PacketFlow pFlow = packet -> {
            try {
                fos.write(packet);
            } catch (IOException e) {
                Log.e(TAG, "error writing to VPN fd", e);
            }
        };

        IPtProxy.startSocks(pFlow, "127.0.0.1",mTorSocks);

        //read packets from TUN and send to go-tun2socks
        mThreadPacket = new Thread() {
            public void run () {

                var buffer = new byte[32767*2]; //64k
                keepRunningPacket = true;
                while (keepRunningPacket) {
                    try {
                        int pLen = fis.read(buffer); // will block on API 21+

                        if (pLen > 0) {
                            var pdata = Arrays.copyOf(buffer, pLen);
                            try {
                                var packet = IpSelector.newPacket(pdata,0,pdata.length);

                                if (packet instanceof IpPacket) {
                                    IpPacket ipPacket = (IpPacket) packet;
                                    if (isPacketDNS(ipPacket))
                                        mExec.execute(new RequestPacketHandler(ipPacket, pFlow, mDnsResolver));
                                    else
                                        IPtProxy.inputPacket(pdata);
                                }
                            } catch (IllegalRawDataException e) {
                                Log.e(TAG, e.getLocalizedMessage());
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "error reading from VPN fd: " +  e.getLocalizedMessage());
                    }
                }
            }
        };
        mThreadPacket.start();
    }

    private static boolean isPacketDNS(IpPacket p) {
        if (p.getHeader().getProtocol() == IpNumber.UDP) {
            var up = (UdpPacket) p.getPayload();
            return up.getHeader().getDstPort() == UdpPort.DOMAIN;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void doLollipopAppRouting(VpnService.Builder builder) throws NameNotFoundException {
        var apps = TorifiedApp.getApps(mService, prefs);
        var perAppEnabled = false;
        var canBypass = !isVpnLockdown(mService);

        for (TorifiedApp app : apps) {
            if (app.isTorified() && (!app.getPackageName().equals(mService.getPackageName()))) {
                if (prefs.getBoolean(app.getPackageName() + OrbotConstants.APP_TOR_KEY, true)) {
                    builder.addAllowedApplication(app.getPackageName());
                }

                perAppEnabled = true;
            }
        }

        if (!perAppEnabled) {

            //if not VPN lockdown, then we can let these apps all go by
            if (canBypass) {

                //remove all apps from Tor
                builder.addDisallowedApplication(mService.getPackageName());

                //disallow all apps since we no longer have a default "full device" mode
              //  for (TorifiedApp app : apps)
                //    builder.addDisallowedApplication(app.getPackageName());

                for (String packageName : OrbotConstants.BYPASS_VPN_PACKAGES)
                    builder.addDisallowedApplication(packageName);

            }
            else
            {
                //nothing will work unless they choose it in the choose apps screen
                //remove all apps from Tor

            }

        } else {
            Log.i(TAG, "Skip bypass perApp? " + perAppEnabled + " vpnLockdown? " + !canBypass);


        }
    }

    public boolean isStarted() {
        return isStarted;
    }

    private boolean isVpnLockdown(final VpnService vpn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return vpn.isLockdownEnabled();
        } else {
            return false;
        }
    }
}
