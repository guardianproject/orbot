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

package org.torproject.android.vpn;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Locale;

import org.torproject.android.service.TorServiceConstants;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class OrbotVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = "OrbotVpnService";

    private PendingIntent mConfigureIntent;

    private Handler mHandler;
    private Thread mThreadProxy;
    private Thread mThreadVPN;

    private String mSessionName = "OrbotVPN";
    private ParcelFileDescriptor mInterface;

    private int mSocksProxyPort = 9999;
   // private ProxyServer mProxyServer;
    
    private final static int VPN_MTU = 1500;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

    	// The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Stop the previous session by interrupting the thread.
        if (mThreadVPN == null || (!mThreadVPN.isAlive()))
        {
        	startSocksBypass ();
            setupTun2Socks();               
        }
     
        
        return START_STICKY;
    }
    
    private void startSocksBypass ()
    {
    }

    @Override
    public void onDestroy() {
    	
      
        
        if (mInterface != null)
			try {
				mInterface.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

  
    private void setupTun2Socks()  {
       
    	mThreadVPN = new Thread ()
    	{
    		
    		public void run ()
    		{
		    	if (mInterface == null)
		    	{
		    		// Set the locale to English (or probably any other language that^M
		            // uses Hindu-Arabic (aka Latin) numerals).^M
		            // We have found that VpnService.Builder does something locale-dependent^M
		            // internally that causes errors when the locale uses its own numerals^M
		            // (i.e., Farsi and Arabic).^M
		    		Locale.setDefault(new Locale("en"));
		    		
		    		String vpnName = "OrbotVPN";
		    		String virtualGateway = "10.0.0.1";
		    		String virtualRoute = "10.0.0.0";
		        	String virtualIP = "10.0.0.2";
		        	String virtualNetMask = "255.255.2555.0";
		        	String localSocks = "localhost:" + TorServiceConstants.PORT_SOCKS_DEFAULT;
		        	String localDNS = "localhost:" + TorServiceConstants.TOR_DNS_PORT_DEFAULT;
		        	
		        	
			        Builder builder = new Builder();
			        
			        builder.setMtu(VPN_MTU);
			        builder.addAddress(virtualGateway,8);
			        builder.setSession(vpnName);	 
			        
			        builder.addRoute("0.0.0.0",0);	        
			        builder.addRoute(virtualRoute,8);
			        
			        //builder.addDnsServer("8.8.8.8");
			        
			         // Create a new interface using the builder and save the parameters.
			        mInterface = builder.setSession(mSessionName)
			                .setConfigureIntent(mConfigureIntent)
			                .establish();
			        	    
			        try
			        {
			        	Tun2Socks.Start(mInterface, VPN_MTU, virtualIP, virtualNetMask, localSocks , localDNS , true);
			        }
			        catch (Exception e)
			        {
			        	Log.d(TAG,"tun2Socks has stopped",e);
			        }
		    	}
    		}
    	};
    	
    	mThreadVPN.start();
    }

	@Override
	public void onRevoke() {
		
		new Thread ()
		{
			public void run()
			{
				try
				{
					mInterface.close();
					Tun2Socks.Stop();
				}
				catch (Exception e)
				{
					Log.d(TAG,"error stopping tun2socks",e);
				}
			}
		}.start();
		
		super.onRevoke();
		
	}
    
    /*
    private void debugPacket(ByteBuffer packet)
    {b

    	int buffer = packet.get();
        int version;
        int headerlength;
        version = buffer >> 4;
        headerlength = buffer & 0x0F;
        headerlength *= 4;
        Log.d(TAG, "IP Version:"+version);
        Log.d(TAG, "Header Length:"+headerlength);

        String status = "";
        status += "Header Length:"+headerlength;

        buffer = packet.get();      //DSCP + EN
        buffer = packet.getChar();  //Total Length

        Log.d(TAG, "Total Length:"+buffer);

        buffer = packet.getChar();  //Identification
        Log.d(TAG, "Identification:"+buffer);

        buffer = packet.getChar();  //Flags + Fragment Offset
        buffer = packet.get();      //Time to Live
        buffer = packet.get();      //Protocol

        Log.d(TAG, "Protocol:"+buffer);

        status += "  Protocol:"+buffer;

        buffer = packet.getChar();  //Header checksum

        String sourceIP  = "";
        buffer = packet.get();  //Source IP 1st Octet
        sourceIP += buffer;
        sourceIP += ".";

        buffer = packet.get();  //Source IP 2nd Octet
        sourceIP += buffer;
        sourceIP += ".";

        buffer = packet.get();  //Source IP 3rd Octet
        sourceIP += buffer;
        sourceIP += ".";

        buffer = packet.get();  //Source IP 4th Octet
        sourceIP += buffer;

        Log.d(TAG, "Source IP:"+sourceIP);

        status += "   Source IP:"+sourceIP;

        String destIP  = "";
        buffer = packet.get();  //Destination IP 1st Octet
        destIP += buffer;
        destIP += ".";

        buffer = packet.get();  //Destination IP 2nd Octet
        destIP += buffer;
        destIP += ".";

        buffer = packet.get();  //Destination IP 3rd Octet
        destIP += buffer;
        destIP += ".";

        buffer = packet.get();  //Destination IP 4th Octet
        destIP += buffer;

        Log.d(TAG, "Destination IP:"+destIP);

        status += "   Destination IP:"+destIP;

        //Log.d(TAG, "version:"+packet.getInt());
        //Log.d(TAG, "version:"+packet.getInt());
        //Log.d(TAG, "version:"+packet.getInt());

    }*/

}
