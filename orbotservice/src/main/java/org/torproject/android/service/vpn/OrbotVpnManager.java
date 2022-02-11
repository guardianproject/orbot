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

import org.pcap4j.packet.DnsPacket;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.util.Inet4NetworkAddress;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static org.torproject.android.service.TorServiceConstants.ACTION_START;
import static org.torproject.android.service.TorServiceConstants.ACTION_START_VPN;
import static org.torproject.android.service.TorServiceConstants.ACTION_STOP;
import static org.torproject.android.service.TorServiceConstants.ACTION_STOP_VPN;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.Nullable;

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
    private int mTorHttp = -1;
    private ProxyServer mSocksProxyServer;
    private final VpnService mService;
    private final SharedPreferences prefs;
    private DNSProxy mDnsProxy;

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
                    if ((torSocks != mTorSocks && torDns != mTorDns)) {

                        mTorSocks = torSocks;
                        mTorDns = torDns;
                        mTorHttp = torHttp;

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

        if (mDnsProxy != null)
            mDnsProxy.stopProxy();
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

            final String virtualGateway = "172.16.0.1";
            final String defaultRoute = "0.0.0.0";

            int dnsProxyPort = 8153;

         //   builder.setMtu(VPN_MTU);
            builder.addAddress(virtualGateway, 32);
            builder.addRoute(defaultRoute,0);

            /**
             * // can't use this since Tor's HTTP port is CONNECT only and not a full PROXY for http traffic
            if (mTorHttp != -1) {
                //extra capability to set local http proxy
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setHttpProxy(ProxyInfo.buildDirectProxy("127.0.0.1", 8118));
                }
            }**/

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
            }




            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mInterface = builder.setSession(mSessionName)
                        .setConfigureIntent(null) // previously this was set to a null member variable
                        .setBlocking(true)
                        .setMtu(1500)
                        .establish();
            }
            else
            {
                mInterface = builder.setSession(mSessionName)
                        .setConfigureIntent(null) // previously this was set to a null member variable
                        .establish();
            }


            FileInputStream fis = new FileInputStream(mInterface.getFileDescriptor());
            FileOutputStream fos = new FileOutputStream(mInterface.getFileDescriptor());

            mDnsProxy = new DNSProxy(localhost, mTorDns, mService);
      //      mDnsProxy.startProxy(localhost, dnsProxyPort);

            //write packets back out to TUN
            PacketFlow pFlow = new PacketFlow() {
                @Override
                public void writePacket(byte[] packet) {
                    try {
                        fos.write(packet);
                    } catch (IOException e) {
                        Log.e(TAG, "error writing to VPN fd", e);

                    }
                }
            };

            IPtProxy.startSocks(pFlow,localhost,mTorSocks);

            Inet4Address loopback;
            try {
                loopback = (Inet4Address)Inet4Address.getByAddress(new byte[]{127,0,0,1});
            } catch (UnknownHostException e) {
                throw new RuntimeException();
            }

            //read packets from TUN and send to go-tun2socks
            new Thread ()
            {
                public void run ()
                {

                    // Allocate the buffer for a single packet.
                    ByteBuffer buffer = ByteBuffer.allocate(32767);

                    while (true)
                    {
                        try {

                            int pLen = fis.read(buffer.array());
                            if (pLen > 0)
                            {
                                buffer.limit(pLen);
                                byte[] pdata = buffer.array();
                                IpPacket packet = (IpPacket)IpSelector.newPacket(pdata,0,pdata.length);

                                if ((mDnsProxy.isDNS(packet))) {
                                    try {
                                        UdpPacket udpPacket = (UdpPacket) packet.getPayload();

                                        byte[] dnsResp = mDnsProxy.processDNS(udpPacket.getPayload().getRawData());

                                        if (dnsResp != null) {

                                            DnsPacket dnsRequest = (DnsPacket) udpPacket.getPayload();

                                            DnsPacket dnsResponse = DnsPacket.newPacket(dnsResp, 0, dnsResp.length);

                                            DnsPacket.Builder dnsBuilder = new DnsPacket.Builder();
                                            dnsBuilder.questions(dnsRequest.getHeader().getQuestions());
                                            dnsBuilder.id(dnsRequest.getHeader().getId());

                                            dnsBuilder.answers(dnsResponse.getHeader().getAnswers());
                                            dnsBuilder.response(dnsResponse.getHeader().isResponse());
                                            dnsBuilder.additionalInfo(dnsResponse.getHeader().getAdditionalInfo());
                                            dnsBuilder.anCount(dnsResponse.getHeader().getAnCount());
                                            dnsBuilder.arCount(dnsResponse.getHeader().getArCount());
                                            dnsBuilder.opCode(dnsResponse.getHeader().getOpCode());
                                            dnsBuilder.rCode(dnsResponse.getHeader().getrCode());
                                            dnsBuilder.authenticData(dnsResponse.getHeader().isAuthenticData());
                                            dnsBuilder.authoritativeAnswer(dnsResponse.getHeader().isAuthoritativeAnswer());
                                            dnsBuilder.authorities(dnsResponse.getHeader().getAuthorities());

                                            UdpPacket.Builder udpBuilder = new UdpPacket.Builder(udpPacket)
                                                    .srcPort(udpPacket.getHeader().getDstPort())
                                                    .dstPort(udpPacket.getHeader().getSrcPort())
                                                    .srcAddr(packet.getHeader().getDstAddr())
                                                    .dstAddr(packet.getHeader().getSrcAddr())
                                                    .correctChecksumAtBuild(true)
                                                    .correctLengthAtBuild(true)
                                                    .payloadBuilder(dnsBuilder);

                                            IpPacket respPacket = null;

                                            if (packet instanceof IpV4Packet) {

                                                IpV4Packet ipPacket = (IpV4Packet)packet;
                                                IpV4Packet.Builder ipv4Builder = new IpV4Packet.Builder();
                                                ipv4Builder
                                                        .version(ipPacket.getHeader().getVersion())
                                                        .protocol(ipPacket.getHeader().getProtocol())
                                                        .tos(ipPacket.getHeader().getTos())
                                                        .srcAddr(ipPacket.getHeader().getDstAddr())
                                                        //        .dstAddr(ipPacket.getHeader().getSrcAddr())
                                                        .dstAddr(loopback)
                                                        .options(ipPacket.getHeader().getOptions())
                                                        .dontFragmentFlag(ipPacket.getHeader().getDontFragmentFlag())
                                                        .identification(ipPacket.getHeader().getIdentification())
                                                        .correctChecksumAtBuild(true)
                                                        .correctLengthAtBuild(true)
                                                        .payloadBuilder(udpBuilder);

                                                respPacket = ipv4Builder.build();

                                            }
                                            else if (packet instanceof IpV6Packet)
                                            {
                                                respPacket = new IpV6Packet.Builder((IpV6Packet) packet)
                                                        .srcAddr((Inet6Address) packet.getHeader().getDstAddr())
                                                        .dstAddr((Inet6Address) packet.getHeader().getSrcAddr())
                                                        .correctLengthAtBuild(true)
                                                        .payloadBuilder(udpBuilder)
                                                        .build();
                                            }


                                            byte[] rawResponse = respPacket.getRawData();
                                            pFlow.writePacket(rawResponse);
                                        }

                                    } catch (Exception ioe) {
                                        Log.e(TAG, "could not parse DNS packet: " + ioe);
                                    }
                                } else {
                                    IPtProxy.inputPacket(pdata);
                                }


                                buffer.clear();

                            }
                        } catch (Exception e) {
                            Log.d(TAG, "error reading from VPN fd: " +  e.getLocalizedMessage());
                        }
                    }

                }
            }.start();

        } catch (Exception e) {
            Log.d(TAG, "tun2Socks has stopped", e);
        }
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
