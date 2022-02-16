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
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.UdpPort;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.torproject.android.service.TorServiceConstants.ACTION_START;
import static org.torproject.android.service.TorServiceConstants.ACTION_START_VPN;
import static org.torproject.android.service.TorServiceConstants.ACTION_STOP;
import static org.torproject.android.service.TorServiceConstants.ACTION_STOP_VPN;

import androidx.annotation.ChecksSdkIntAtLeast;

import IPtProxy.IPtProxy;
import IPtProxy.PacketFlow;

public class OrbotVpnManager implements Handler.Callback {
    private static final String TAG = "OrbotVpnService";
    private final static int VPN_MTU = 1500;
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
    private final static boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    public static int sSocksProxyServerPort = -1;
    public static String sSocksProxyLocalhost = null;
    boolean isStarted = false;
    private final static String mSessionName = "OrbotVPN";
    private ParcelFileDescriptor mInterface;
    private int mTorSocks = -1;
    private int mTorDns = -1;
    private ProxyServer mSocksProxyServer;
    private final VpnService mService;
    private final SharedPreferences prefs;
    private DNSProxy mDnsProxy;

    private final ExecutorService mExec = Executors.newFixedThreadPool(10);
    private Thread mThreadPacket;
    private boolean keepRunningPacket = false;

    public OrbotVpnManager(OrbotService service) {
        mService = service;
        prefs = Prefs.getSharedPrefs(mService.getApplicationContext());
    }

    public int handleIntent(VpnService.Builder builder, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (action != null) {
                if (action.equals(ACTION_START_VPN) || action.equals(ACTION_START)) {
                    Log.d(TAG, "starting VPN");

                    isStarted = true;

                    // Stop the previous session by interrupting the thread.
                    //stopVPN();

                } else if (action.equals(ACTION_STOP_VPN) || action.equals(ACTION_STOP)) {
                    isStarted = false;

                    Log.d(TAG, "stopping VPN");

                    stopVPN();
                } else if (action.equals(TorServiceConstants.LOCAL_ACTION_PORTS)) {
                    Log.d(TAG, "setting VPN ports");

                    int torSocks = intent.getIntExtra(OrbotService.EXTRA_SOCKS_PROXY_PORT, -1);
                    int torHttp = intent.getIntExtra(OrbotService.EXTRA_HTTP_PROXY_PORT,-1);
                    int torDns = intent.getIntExtra(OrbotService.EXTRA_DNS_PORT, -1);

                    //if running, we need to restart
                    if ((torSocks != -1 && torSocks != mTorSocks
                            && torDns != -1 && torDns != mTorDns)) {

                        mTorSocks = torSocks;
                        mTorDns = torDns;

                        if (!mIsLollipop) {
                         //   stopSocksBypass();
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
            //   stopSocksBypass();
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

                mInterface.close();
                mInterface = null;

            } catch (Exception e) {
                Log.d(TAG, "error stopping tun2socks", e);
            } catch (Error e) {
                Log.d(TAG, "error stopping tun2socks", e);
            }
        }

        if (mDnsProxy != null) {
            mDnsProxy.stopProxy();
            mDnsProxy = null;
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
    public final static String FAKE_DNS_HEX= "a0a0a0a";

    private synchronized void setupTun2Socks(final VpnService.Builder builder) {
        try {

            final String vpnName = "OrbotVPN";
            final String localhost = "127.0.0.1";
            final String defaultRoute = "0.0.0.0";
            final String virtualGateway = "192.168.50.1";

            builder.setMtu(VPN_MTU);
         //   builder.addAddress(virtualGateway, 32);
            builder.addAddress(virtualGateway, 24);

            builder.addRoute(defaultRoute,0);


            builder.setSession(vpnName);


            //route all traffic through VPN (we might offer country specific exclude lists in the future)
        //    builder.addRoute(defaultRoute, 0);

            builder.addDnsServer(FAKE_DNS); //just setting a value here so DNS is captured by TUN interface
            builder.addRoute(FAKE_DNS,32);

            //handle ipv6
            //builder.addAddress("fdfe:dcba:9876::1", 126);
            //builder.addRoute("::", 0);

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mInterface = builder.setSession(mSessionName)
                        .setConfigureIntent(null) // previously this was set to a null member variable
                        .setBlocking(true)
                        .establish();
            }
            else {
                mInterface = builder.setSession(mSessionName)
                        .setConfigureIntent(null) // previously this was set to a null member variable
                        .establish();
            }


            FileInputStream fis = new FileInputStream(mInterface.getFileDescriptor());
            DataOutputStream fos = new DataOutputStream(new FileOutputStream(mInterface.getFileDescriptor()));

            mDnsProxy = new DNSProxy(localhost, mTorDns);

            //write packets back out to TUN
            PacketFlow pFlow = (PacketFlow) packet -> {
                try {
                    fos.write(packet);
                } catch (IOException e) {
                    Log.e(TAG, "error writing to VPN fd", e);
                }
            };

            IPtProxy.startSocks(pFlow,localhost,mTorSocks);

            //read packets from TUN and send to go-tun2socks
            mThreadPacket = new Thread() {
                public void run () {

                    // Allocate the buffer for a single packet.
                    ByteBuffer buffer = ByteBuffer.allocate(32767);

                    keepRunningPacket = true;
                    while (keepRunningPacket)
                    {
                        try {

                            int pLen = fis.read(buffer.array());
                            if (pLen > 0)
                            {
                                buffer.limit(pLen);
                                byte[] pdata = buffer.array();
                                Packet packet;
                                try {
                                    packet = (Packet) IpSelector.newPacket(pdata,0,pdata.length);

                                    if (packet instanceof IpPacket) {
                                        IpPacket ipPacket = (IpPacket) packet;
                                        if (isPacketDNS(ipPacket))
                                            mExec.execute(new RequestPacketHandler(ipPacket, pFlow, mDnsProxy));
                                        else
                                            IPtProxy.inputPacket(pdata);
                                    }

                                } catch (IllegalRawDataException e) {
                                    return;
                                }
                                buffer.clear();

                            }
                        } catch (Exception e) {
                            Log.d(TAG, "error reading from VPN fd: " +  e.getLocalizedMessage());
                        }
                    }
                }
            };
            mThreadPacket.start();

        } catch (Exception e) {
            Log.d(TAG, "tun2Socks has stopped", e);
        }
    }

    private static boolean isPacketDNS(IpPacket p) {
        if (p.getHeader().getProtocol()== IpNumber.UDP) {
            UdpPacket up = (UdpPacket) p.getPayload();
            return up.getHeader().getDstPort() == UdpPort.DOMAIN;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void doLollipopAppRouting(VpnService.Builder builder) throws NameNotFoundException {
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

        if (!perAppEnabled) {
            builder.addDisallowedApplication(mService.getPackageName());
            for (String packageName : VpnPrefs.BYPASS_VPN_PACKAGES)
                builder.addDisallowedApplication(packageName);
        }

    }

    public boolean isStarted() {
        return isStarted;
    }
}
