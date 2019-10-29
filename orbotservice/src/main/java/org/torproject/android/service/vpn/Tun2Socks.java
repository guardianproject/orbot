package org.torproject.android.service.vpn;

/*
 * Copyright (c) 2013, Psiphon Inc.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import org.torproject.android.service.util.TCPSourceApp;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class Tun2Socks
{

    static{
        System.loadLibrary("tun2socks");
    }

    public static interface IProtectSocket
    {
        boolean doVpnProtect(Socket socket);
        boolean doVpnProtect(DatagramSocket socket);
    };

    private static final String TAG = Tun2Socks.class.getSimpleName();
    private static final boolean LOGD = true;

    private static Thread mThread;
    private static ParcelFileDescriptor mVpnInterfaceFileDescriptor;
    private static int mVpnInterfaceMTU;
    private static String mVpnIpAddress;
    private static String mVpnNetMask;
    private static String mSocksServerAddress;
    private static String mUdpgwServerAddress;
    private static boolean mUdpgwTransparentDNS;

    private static HashMap<Integer,String> mAppUidBlacklist = new HashMap<>();
    private static Context mContext;
    // Note: this class isn't a singleton, but you can't run more
    // than one instance due to the use of global state (the lwip
    // module, etc.) in the native code.

    public static void init () {}

    public static void Start(
            Context context,
            ParcelFileDescriptor vpnInterfaceFileDescriptor,
            int vpnInterfaceMTU,
            String vpnIpAddress,
            String vpnNetMask,
            String socksServerAddress,
            String udpgwServerAddress,
            boolean udpgwTransparentDNS)
    {
        mContext = context;

        mVpnInterfaceFileDescriptor = vpnInterfaceFileDescriptor;
        mVpnInterfaceMTU = vpnInterfaceMTU;
        mVpnIpAddress = vpnIpAddress;
        mVpnNetMask = vpnNetMask;
        mSocksServerAddress = socksServerAddress;
        mUdpgwServerAddress = udpgwServerAddress;
        mUdpgwTransparentDNS = udpgwTransparentDNS;

        if (mVpnInterfaceFileDescriptor != null)
            runTun2Socks(
                    mVpnInterfaceFileDescriptor.detachFd(),
                    mVpnInterfaceMTU,
                    mVpnIpAddress,
                    mVpnNetMask,
                    mSocksServerAddress,
                    mUdpgwServerAddress,
                    mUdpgwTransparentDNS ? 1 : 0);
    }
    
    public static void Stop()
    {
       
        terminateTun2Socks();
    
    }

    public static void logTun2Socks(
            String level,
            String channel,
            String msg)
    {
        String logMsg = level + "(" + channel + "): " + msg;
        if (0 == level.compareTo("ERROR"))
        {
            Log.e(TAG, logMsg);
        }
        else
        {
            if (LOGD) Log.d(TAG, logMsg);
        }
    }

    private native static int runTun2Socks(
            int vpnInterfaceFileDescriptor,
            int vpnInterfaceMTU,
            String vpnIpAddress,
            String vpnNetMask,
            String socksServerAddress,
            String udpgwServerAddress,
            int udpgwTransparentDNS);

    private native static void terminateTun2Socks();

    public static boolean checkIsAllowed (int protocol, String sourceAddr, int sourcePort, String destAddr, int destPort) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            return isAllowedQ(protocol, sourceAddr, sourcePort, destAddr, destPort);
        }
        else
            return isAllowed(protocol, sourceAddr, sourcePort, destAddr, destPort);
    }

    public static boolean isAllowed (int protocol, String sourceAddr, int sourcePort, String destAddr, int destPort) {

        TCPSourceApp.AppDescriptor aInfo = TCPSourceApp.getApplicationInfo(mContext, sourceAddr, sourcePort, destAddr, destPort);

        if (aInfo != null) {
            int uid = aInfo.getUid();
            return mAppUidBlacklist.containsKey(uid);
        }
        else
            return true;
    }

    @TargetApi(Build.VERSION_CODES.Q)
    public static boolean isAllowedQ (int protocol, String sourceAddr, int sourcePort, String destAddr, int destPort) {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;

        InetSocketAddress local = new InetSocketAddress(sourceAddr, sourcePort);
        InetSocketAddress remote = new InetSocketAddress(destAddr, destPort);

        int uid = cm.getConnectionOwnerUid(protocol, local, remote);
        return mAppUidBlacklist.containsKey(uid);
    }

    public static void setBlacklist(HashMap<Integer,String> appUidBlacklist)
    {
        mAppUidBlacklist = appUidBlacklist;
    }

    public static void clearBlacklist()
    {
        mAppUidBlacklist.clear();
    }

    public static void addToBlacklist (int uid, String pkgId)
    {
        mAppUidBlacklist.put(uid,pkgId);
    }

    public static void removeFromBlacklist (int uid)
    {
        mAppUidBlacklist.remove(uid);
    }

}