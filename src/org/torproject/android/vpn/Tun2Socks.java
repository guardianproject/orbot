package org.torproject.android.vpn;

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

import java.net.DatagramSocket;
import java.net.Socket;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.ParcelFileDescriptor;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class Tun2Socks
{
    public static interface IProtectSocket
    {
        boolean doVpnProtect(Socket socket);
        boolean doVpnProtect(DatagramSocket socket);
    };


    private static Thread mThread;
    private static ParcelFileDescriptor mVpnInterfaceFileDescriptor;
    private static int mVpnInterfaceMTU;
    private static String mVpnIpAddress;
    private static String mVpnNetMask;
    private static String mSocksServerAddress;
    private static String mUdpgwServerAddress;
    private static boolean mUdpgwTransparentDNS;
    
    // Note: this class isn't a singleton, but you can't run more
    // than one instance due to the use of global state (the lwip
    // module, etc.) in the native code.
    
    public static synchronized void Start(
            ParcelFileDescriptor vpnInterfaceFileDescriptor,
            int vpnInterfaceMTU,
            String vpnIpAddress,
            String vpnNetMask,
            String socksServerAddress,
            String udpgwServerAddress,
            boolean udpgwTransparentDNS)
    {
        
        
        Stop();

        mVpnInterfaceFileDescriptor = vpnInterfaceFileDescriptor;
        mVpnInterfaceMTU = vpnInterfaceMTU;
        mVpnIpAddress = vpnIpAddress;
        mVpnNetMask = vpnNetMask;
        mSocksServerAddress = socksServerAddress;
        mUdpgwServerAddress = udpgwServerAddress;
        mUdpgwTransparentDNS = udpgwTransparentDNS;

        mThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                runTun2Socks(
                        mVpnInterfaceFileDescriptor.detachFd(),
                        mVpnInterfaceMTU,
                        mVpnIpAddress,
                        mVpnNetMask,
                        mSocksServerAddress,
                        mUdpgwServerAddress,
                        mUdpgwTransparentDNS ? 1 : 0);
            	
            }
        });
        mThread.start();
    }
    
    public static synchronized void Stop()
    {
        if (mThread != null)
        {
            terminateTun2Socks();
            try
            {
                mThread.join();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            mThread = null;
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
    
    static
    {
        System.loadLibrary("tun2socks");
    }
}