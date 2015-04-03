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

import java.net.InetAddress;
import java.util.Locale;

import org.torproject.android.service.TorServiceConstants;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)

public class OrbotVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = "OrbotVpnService";

    private PendingIntent mConfigureIntent;

    private Handler mHandler;
    private Thread mThreadVPN;

    private String mSessionName = "OrbotVPN";
    private ParcelFileDescriptor mInterface;

    private int mSocksProxyPort = -1;
    private ProxyServer mSocksProxyServer;
    private Thread mThreadProxy;
    
    private final static int VPN_MTU = 1500;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

    	String action = intent.getAction();
    	
    	if (action.equals("start"))
    	{
	    	Log.d(TAG,"starting OrbotVPNService service!");
	
	    	mSocksProxyPort = intent.getIntExtra("proxyPort", 0);
	    	
	        // The handler is only used to show messages.
	        if (mHandler == null) {
	            mHandler = new Handler(this);
	        }
	
	        // Stop the previous session by interrupting the thread.
	        if (mThreadVPN == null || (!mThreadVPN.isAlive()))
	        {
	        	boolean isLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	        	if (!isLollipop)
	        		startSocksBypass();
	        	
	            setupTun2Socks();               
	        }
    	}
    	else if (action.equals("stop"))
    	{
    		stopVPN();    		
    		if (mHandler != null)
    			mHandler.postDelayed(new Runnable () { public void run () { stopSelf(); }}, 1000);
    	}
     
        
        return START_NOT_STICKY;
    }
    

    private void startSocksBypass(){
        mThreadProxy = new Thread ()
        {
            public void run ()
            {
        
                try {
                    mSocksProxyServer = new ProxyServer(new ServerAuthenticatorNone(null, null));
                    ProxyServer.setVpnService(OrbotVpnService.this);
                    mSocksProxyServer.start(mSocksProxyPort, 5, InetAddress.getLocalHost());
                } catch (Exception e) {
                    Log.d(TAG,"proxy server error: " + e.getLocalizedMessage(),e);
                }
            }
        };
        
        mThreadProxy.start();
        
    }

    @Override
    public void onDestroy() {
    	stopVPN();
    	
    }
    
    private void stopVPN ()
    {
        if (mSocksProxyServer != null){
            mSocksProxyServer.stop();
            mSocksProxyServer = null;
        }
        
        /*
        if (mHttpProxyServer != null)
        {
        	mHttpProxyServer.closeSocket();
        }*/
        
        if (mInterface != null){
            onRevoke();
            
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
		    		try
			        {
		    			
			    		// Set the locale to English (or probably any other language that^M
			            // uses Hindu-Arabic (aka Latin) numerals).^M
			            // We have found that VpnService.Builder does something locale-dependent^M
			            // internally that causes errors when the locale uses its own numerals^M
			            // (i.e., Farsi and Arabic).^M
			    		Locale.setDefault(new Locale("en"));
			    		
			    		boolean isLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
			        	
			    		
			    		//String localhost = InetAddress.getLocalHost().getHostAddress();
			    		
			    		String vpnName = "OrbotVPN";
			    		String virtualGateway = "10.0.0.1";
			        	String virtualIP = "10.0.0.2";
			        	String virtualNetMask = "255.255.255.0";
			        	String localSocks = "127.0.0.1" + ':' + TorServiceConstants.PORT_SOCKS_DEFAULT;
			        	String localDNS = "10.0.0.1" + ':' + TorServiceConstants.TOR_DNS_PORT_DEFAULT;
			        	
			        	
				        Builder builder = new Builder();
				        
				        builder.setMtu(VPN_MTU);
				        builder.addAddress(virtualGateway,28);
				        builder.setSession(vpnName);	 
				        builder.addRoute("0.0.0.0",0);	 
				      //  builder.addDnsServer("8.8.8.8");
				        
				        if (isLollipop)
				        	doLollipopAppRouting(builder);
				        
				         // Create a new interface using the builder and save the parameters.
				        mInterface = builder.setSession(mSessionName)
				                .setConfigureIntent(mConfigureIntent)
				                .establish();
				        	    
				        
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
    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void doLollipopAppRouting (Builder builder) throws NameNotFoundException
    {

        
    	builder.addDisallowedApplication("org.torproject.android");
    
        
    }
    
    @Override
    public void onRevoke() {
    
        try
        {
        	Log.d(TAG,"closing interface, destroying VPN interface");
            
        	//Tun2Socks.Stop();
        	
        	if (mInterface != null)
            {
        		mInterface.close();
        		mInterface = null;
            }
        	
        }
        catch (Exception e)
        {
            Log.d(TAG,"error stopping tun2socks",e);
        }
        catch (Error e)
        {
            Log.d(TAG,"error stopping tun2socks",e);
        }

        super.onRevoke();
    }
    
    
}
