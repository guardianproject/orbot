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
import org.torproject.android.service.TorServiceUtils;

import android.annotation.TargetApi;
import android.app.PendingIntent;
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
    
    private final static boolean isLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    
    private boolean isRestart = false;
    
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
	        	
	        	if (!isLollipop)
	        		startSocksBypass();
	        	
	            setupTun2Socks();               
	        }
    	}
    	else if (action.equals("stop"))
    	{
    		Log.d(TAG,"stop OrbotVPNService service!");
    		
    		stopVPN();    		
    		if (mHandler != null)
    			mHandler.postDelayed(new Runnable () { public void run () { stopSelf(); }}, 1000);
    	}
    	else if (action.equals("refresh"))
    	{
    		Log.d(TAG,"refresh OrbotVPNService service!");
    		
    	//	if (!isLollipop)
    		//	startSocksBypass();
    		
    		setupTun2Socks();
    	}
     
        
        return START_NOT_STICKY;
    }
    

    private void startSocksBypass(){
        mThreadProxy = new Thread ()
        {
            public void run ()
            {
        
                try {
                	
                	if (mSocksProxyServer != null)
                	{
                		stopSocksBypass ();
                	}
                	
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

    private void stopSocksBypass ()
    {

        if (mSocksProxyServer != null){
            mSocksProxyServer.stop();
            mSocksProxyServer = null;
        }
        
        
    }
    
    @Override
    public void onDestroy() {
    	stopVPN();
    }
    
    private void stopVPN ()
    {

        Tun2Socks.Stop();
        
        stopSocksBypass ();
        
        if (mInterface != null){
            try
            {
            	Log.d(TAG,"closing interface, destroying VPN interface");
                
        		mInterface.close();
        		mInterface = null;
            	
            }
            catch (Exception e)
            {
                Log.d(TAG,"error stopping tun2socks",e);
            }
            catch (Error e)
            {
                Log.d(TAG,"error stopping tun2socks",e);
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
	    		try
		        {
	    			
		    		// Set the locale to English (or probably any other language that^M
		            // uses Hindu-Arabic (aka Latin) numerals).^M
		            // We have found that VpnService.Builder does something locale-dependent^M
		            // internally that causes errors when the locale uses its own numerals^M
		            // (i.e., Farsi and Arabic).^M
		    		Locale.setDefault(new Locale("en"));
		    		
		    		String localhost = InetAddress.getLocalHost().getHostAddress();
		    		
		    		String vpnName = "OrbotVPN";
		    		String virtualGateway = "10.0.0.1";
		        	String virtualIP = "10.0.0.2";
		        	String virtualNetMask = "255.255.255.0";
		        	String localSocks = localhost + ':' + TorServiceConstants.PORT_SOCKS_DEFAULT;//+ "127.0.0.1" 
		        	String localDNS = "10.0.0.1" + ':' + TorServiceConstants.TOR_DNS_PORT_DEFAULT;
		        	
		        	
			        Builder builder = new Builder();
			        
			        builder.setMtu(VPN_MTU);
			        builder.addAddress(virtualGateway,28);
			        builder.setSession(vpnName);	 
			        builder.addRoute("0.0.0.0",0);	 
			      //  builder.addDnsServer("8.8.8.8");
			        
			        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			        {
			        	doLollipopAppRouting(builder);
			        }
			        
			        if (mInterface != null)
			        {
			        	Log.d(TAG,"Stopping existing VPN interface");
			        	isRestart = true;
			        	mInterface.close();
			        	mInterface = null;

			        	Tun2Socks.Stop();
			        }
			        

			         // Create a new interface using the builder and save the parameters.
			        ParcelFileDescriptor newInterface = builder.setSession(mSessionName)
			                .setConfigureIntent(mConfigureIntent)
			                .establish();
			        	    

		        	mInterface = newInterface;
			        
		        	Tun2Socks.Start(mInterface, VPN_MTU, virtualIP, virtualNetMask, localSocks , localDNS , true);
		        }
		        catch (Exception e)
		        {
		        	Log.d(TAG,"tun2Socks has stopped",e);
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
    
    	if (!isRestart)
    	{
	    	SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext()); 
	        prefs.edit().putBoolean("pref_vpn", false).commit();      
	    	stopVPN();
	    	
    	}
    	
    	isRestart = false;
    	
        super.onRevoke();
    }
    
    
}
