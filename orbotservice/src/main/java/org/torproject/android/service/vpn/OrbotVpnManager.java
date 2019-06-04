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
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.VpnService;
import android.net.VpnService.Builder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;

import org.torproject.android.service.R;
import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.TorServiceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class OrbotVpnManager implements Handler.Callback {
    private static final String TAG = "OrbotVpnService";

    private PendingIntent mConfigureIntent;

    private Thread mThreadVPN;

    private String mSessionName = "OrbotVPN";
    private ParcelFileDescriptor mInterface;

    private int mTorSocks = -1;

    public static int sSocksProxyServerPort = -1;
    public static String sSocksProxyLocalhost = null;
    private ProxyServer mSocksProxyServer;

    private final static int VPN_MTU = 1500;
    
    private final static boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    
    //this is the actual DNS server we talk to over UDP or TCP (now using Tor's DNS port)
    //private final static String DEFAULT_ACTUAL_DNS_HOST = "127.0.0.1";
    //private final static int DEFAULT_ACTUAL_DNS_PORT = TorServiceConstants.TOR_DNS_PORT_DEFAULT;



	File filePdnsd = null;

	private final static int PDNSD_PORT = 8091;

	private boolean isRestart = false;
    
    private VpnService mService;
    

    public OrbotVpnManager (VpnService service) throws IOException, TimeoutException {
    	mService = service;

		filePdnsd = new PDNSDInstaller(service.getApplicationContext(),service.getFilesDir()).installResources();

		Tun2Socks.init();

	}
   
    //public int onStartCommand(Intent intent, int flags, int startId) {
    public int handleIntent(Builder builder, Intent intent) {

    	if (intent != null)
    	{
	    	String action = intent.getAction();
	    	
	    	if (action.equals("start"))
	    	{
		
		        // Stop the previous session by interrupting the thread.
		        if (mThreadVPN == null || (!mThreadVPN.isAlive()))
		        {
		        	Log.d(TAG,"starting OrbotVPNService service!");

		        	mTorSocks = intent.getIntExtra("torSocks", -1);
			    	
		        	if (!mIsLollipop)
		        	{
		        		startSocksBypass();
		        	}
		        	
		            setupTun2Socks(builder);               
		        }
	    	}
	    	else if (action.equals("stop"))
	    	{
	    		Log.d(TAG,"stop OrbotVPNService service!");
	    		
	    		stopVPN();    		
	    		//if (mHandler != null)
	    			//mHandler.postDelayed(new Runnable () { public void run () { stopSelf(); }}, 1000);
	    	}
	    	else if (action.equals("refresh"))
	    	{
	    		Log.d(TAG,"refresh OrbotVPNService service!");
	    		
	    		if (!mIsLollipop)
	    		  startSocksBypass();
	    		
	    		if (!isRestart)
	    			setupTun2Socks(builder);
	    	}
    	}
     
        
        return Service.START_STICKY;
    }
  
    private void startSocksBypass()
    {
       
    	new Thread ()
    	{
    		
    		public void run ()
    		{

                //generate the proxy port that the 
                if (sSocksProxyServerPort == -1)
                {
                	try {
						
                		sSocksProxyLocalhost = "127.0.0.1";// InetAddress.getLocalHost().getHostAddress();
	                	sSocksProxyServerPort = (int)((Math.random()*1000)+10000); 
	                	
					} catch (Exception e) {
						Log.e(TAG,"Unable to access localhost",e);
						throw new RuntimeException("Unable to access localhost: " + e);
						
					}
                	
                }
                
                
		    	if (mSocksProxyServer != null)
		    	{
		    		stopSocksBypass ();
		    	}

		    	try
		    	{
			        mSocksProxyServer = new ProxyServer(new ServerAuthenticatorNone(null, null));
			        ProxyServer.setVpnService(mService);
			        mSocksProxyServer.start(sSocksProxyServerPort, 5, InetAddress.getLocalHost());
			        
		    	}
		    	catch (Exception e)
		    	{
		    		Log.e(TAG,"error getting host",e);
		    	}
    		}
    	}.start();
       
    }

    private synchronized void stopSocksBypass ()
    {

        if (mSocksProxyServer != null){
            mSocksProxyServer.stop();
            mSocksProxyServer = null;
        }
        
        
    }
    
    /**
    @Override
	public void onCreate() {
		super.onCreate();
		
		// Set the locale to English (or probably any other language that^M
        // uses Hindu-Arabic (aka Latin) numerals).^M
        // We have found that VpnService.Builder does something locale-dependent^M
        // internally that causes errors when the locale uses its own numerals^M
        // (i.e., Farsi and Arabic).^M
		Locale.setDefault(new Locale("en"));
		
	}


	@Override
    public void onDestroy() {
    	stopVPN();
    }*/
    
    private void stopVPN ()
    {
    	if (mIsLollipop)
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
        
        Tun2Socks.Stop();
        
        try {
        	TorServiceUtils.killProcess(filePdnsd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        mThreadVPN = null;
        

    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(mService, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

  
    private synchronized void setupTun2Socks(final Builder builder)  {

    	
        if (mInterface != null) //stop tun2socks now to give it time to clean up
        {
        	isRestart = true;
        	Tun2Socks.Stop();
        }

        final int localDns = TorService.mPortDns;
        
    	mThreadVPN = new Thread ()
    	{
    		
    		public void run ()
    		{
	    		try
		        {
	    			
	    			if (isRestart)
	    			{
	    				Log.d(TAG,"is a restart... let's wait for a few seconds");
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
		    		
		    		final String localDNS = virtualGateway + ':' + PDNSD_PORT;

		    		final boolean localDnsTransparentProxy = true;
		        	
			        builder.setMtu(VPN_MTU);
			        builder.addAddress(virtualGateway,32);
			        
			        builder.setSession(vpnName);		        

			        builder.addDnsServer(dummyDNS);
			        builder.addRoute(dummyDNS,32);
				        
			        //route all traffic through VPN (we might offer country specific exclude lists in the future)
			        builder.addRoute(defaultRoute,0);	
			        
			        //handle ipv6
			        //builder.addAddress("fdfe:dcba:9876::1", 126);
					//builder.addRoute("::", 0);
			        
			        if (mIsLollipop)			        
			        	doLollipopAppRouting(builder);			        

			         // Create a new interface using the builder and save the parameters.
			        ParcelFileDescriptor newInterface = builder.setSession(mSessionName)
			                .setConfigureIntent(mConfigureIntent)
			                .establish();
		
			        if (mInterface != null)
			        {
			        	Log.d(TAG,"Stopping existing VPN interface");
			        	mInterface.close();
			        	mInterface = null;
			        }

		        	mInterface = newInterface;

		        	isRestart = false;

					//start PDNSD daemon pointing to actual DNS
					startDNS("127.0.0.1",localDns);

					Tun2Socks.Start(mInterface, VPN_MTU, virtualIP, virtualNetMask, localSocks , localDNS , localDnsTransparentProxy);


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
    	   
        ArrayList<TorifiedApp> apps = TorifiedApp.getApps(mService, TorServiceUtils.getSharedPrefs(mService.getApplicationContext()));
    
        boolean perAppEnabled = false;
        
        for (TorifiedApp app : apps)
        {
        	if (app.isTorified() && (!app.getPackageName().equals(mService.getPackageName())))
        	{
        		builder.addAllowedApplication(app.getPackageName());
        		perAppEnabled = true;
        	}
        	
        }
    
        if (!perAppEnabled)
        	builder.addDisallowedApplication(mService.getPackageName());
    
    }
    
    
    public void onRevoke() {
    
    	Log.w(TAG,"VPNService REVOKED!");
    	
    	if (!isRestart)
    	{
	    	SharedPreferences prefs = TorServiceUtils.getSharedPrefs(mService.getApplicationContext()); 
	        prefs.edit().putBoolean("pref_vpn", false).commit();      
	    	stopVPN();	
    	}
    	
    	isRestart = false;
    	
    	//super.onRevoke();
    
    }


    private void startDNS (String dns, int port) throws IOException, TimeoutException
    {

		File fileConf = makePdnsdConf(mService, dns, port,mService.getFilesDir());
    	
      //  ArrayList<String> customEnv = new ArrayList<String>();

        String[] cmdString = {filePdnsd.getCanonicalPath(),"-c",fileConf.toString()};
        ProcessBuilder pb = new ProcessBuilder(cmdString);
        pb.redirectErrorStream(true);
		Process proc = pb.start();
		try { proc.waitFor();} catch (Exception e){}

        Log.i(TAG,"PDNSD: " + proc.exitValue());

        if (proc.exitValue() != 0)
        {

            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String line = null;
            while ((line = br.readLine ()) != null) {
                Log.d(TAG,"pdnsd: " + line);
            }

        }

        
    }
    
    public static File makePdnsdConf(Context context, String dns, int port, File fileDir) throws FileNotFoundException, IOException {
        String conf = String.format(context.getString(R.string.pdnsd_conf), dns, port, fileDir.getCanonicalPath());

        File f = new File(fileDir,"pdnsd.conf");

        if (f.exists()) {
                f.delete();
        }

        FileOutputStream fos = new FileOutputStream(f, false);
    	PrintStream ps = new PrintStream(fos);
    	ps.print(conf);
    	ps.close();

        File cache = new File(fileDir,"pdnsd.cache");

        if (!cache.exists()) {
                try {
                        cache.createNewFile();
                } catch (Exception e) {

                }
        }

        return f;
	}

    
}
