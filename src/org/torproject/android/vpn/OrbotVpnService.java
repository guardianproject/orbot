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
import java.util.Set;

import org.sandroproxy.ony.R;
import org.torproject.android.Orbot;
import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;

import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class OrbotVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = "DrobotVpnService";

    private PendingIntent mConfigureIntent;

    private Handler mHandler;
    private Thread mThreadVPN;

    private String mSessionName = "OrbotVPN";
    private ParcelFileDescriptor mInterface;

    private int mSocksProxyPort = 9999;
    private ProxyServer mProxyServer;
    
    private final static int VPN_MTU = 1500;
    
    private static final int NOTIFY_ID = 10;
    private static final int TRANSPROXY_NOTIFY_ID = 20;
    private static final int ERROR_NOTIFY_ID = 30;
    private static final int HS_NOTIFY_ID = 40;
    
    private boolean prefPersistNotifications = true;
    
    private NotificationManager mNotificationManager = null;
    private android.support.v4.app.NotificationCompat.Builder mNotifyBuilder;
    private Notification mNotification;
    private boolean mShowExpandedNotifications = false;
    private boolean mNotificationShowing = false;
    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Stop the previous session by interrupting the thread.
        if (mThreadVPN == null || (!mThreadVPN.isAlive()))
        {
       	    enableAppRouting ();
            setupTun2Socks();               
        }
     
        
        return START_STICKY;
    }
    
    private void enableAppRouting ()
    {
    	
    	boolean isLollipop = false;
    	
    	if (isLollipop)
    	{
    		//allow for specific apps to be sent through VPN based on list selection
    	}
    	else
    	{
    		//do socks bypass trick
    	}
    }

    private void startSocksBypass(){
        mThreadProxy = new Thread ()
        {
            public void run ()
            {
        
                try {
                    mProxyServer = new ProxyServer(new ServerAuthenticatorNone(null, null));
                    ProxyServer.setVpnService(OrbotVpnService.this);
                    mProxyServer.start(mSocksProxyPort, 5, InetAddress.getLocalHost());
                } catch (Exception e) {
                    Log.d(TAG,"proxy server error: " + e.getLocalizedMessage(),e);
                }
            }
        };
        
        mThreadProxy.start();
    }

    @Override
    public void onDestroy() {
        if (mProxyServer != null){
            mProxyServer.stop();
        }
        if (mInterface != null){
            try {
                mInterface.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
        clearNotifications();
        super.onRevoke();
    }
    
    
}
