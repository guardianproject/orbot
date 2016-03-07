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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;
import org.torproject.android.OrbotApp;
import org.torproject.android.R;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.TorServiceUtils;
import org.torproject.android.settings.AppManager;
import org.torproject.android.settings.TorifiedApp;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
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

    private int mTorSocks = TorServiceConstants.SOCKS_PROXY_PORT_DEFAULT;
    
    public static int sSocksProxyServerPort = -1;
    public static String sSocksProxyLocalhost = null;
    private ProxyServer mSocksProxyServer;
   
    
    private final static int VPN_MTU = 1500;
    
    private final static boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    
    //this is the actual DNS server we talk to over UDP or TCP (now using Tor's DNS port)
    private final static String DEFAULT_ACTUAL_DNS_HOST = "127.0.0.1";
    private final static int DEFAULT_ACTUAL_DNS_PORT = TorServiceConstants.TOR_DNS_PORT_DEFAULT;
    
    private boolean isRestart = false;
    

    static{
    	  System.loadLibrary("tun2socks");
    	}
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

    	if (intent != null)
    	{
	    	String action = intent.getAction();
	    	
	    	if (action.equals("start"))
	    	{
		
		        // Stop the previous session by interrupting the thread.
		        if (mThreadVPN == null || (!mThreadVPN.isAlive()))
		        {
		        	Log.d(TAG,"starting OrbotVPNService service!");
		        	
		        	mTorSocks = intent.getIntExtra("torSocks", TorServiceConstants.SOCKS_PROXY_PORT_DEFAULT);
			    	
			        // The handler is only used to show messages.
			        if (mHandler == null) {
			            mHandler = new Handler(this);
			        }
		        	
		        	if (!mIsLollipop)
		        	{
	
		        		startSocksBypass();
		        	}
		        	
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
	    		
	    		if (!mIsLollipop)
	    		  startSocksBypass();
	    		
	    		if (!isRestart)
	    			setupTun2Socks();
	    	}
    	}
     
        
        return START_STICKY;
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
			        ProxyServer.setVpnService(OrbotVpnService.this);
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
    }
    
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
        	TorServiceUtils.killProcess(OrbotApp.filePdnsd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        mThreadVPN = null;
        

    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

  
    private synchronized void setupTun2Socks()  {

    	
        if (mInterface != null) //stop tun2socks now to give it time to clean up
        {
        	isRestart = true;
        	Tun2Socks.Stop();
        }
        
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
	    			
	    			//start PDNSD daemon pointing to OpenDNS
	    			startDNS(DEFAULT_ACTUAL_DNS_HOST,DEFAULT_ACTUAL_DNS_PORT);
	    			
		    		final String vpnName = "OrbotVPN";
		    		final String localhost = "127.0.0.1";

		    		final String virtualGateway = "10.10.10.1";
		    		final String virtualIP = "10.10.10.2";
		    		final String virtualNetMask = "255.255.255.0";
		    		final String dummyDNS = "8.8.8.8"; //this is intercepted by the tun2socks library, but we must put in a valid DNS to start
		    		final String defaultRoute = "0.0.0.0";
		    		
		    		final String localSocks = localhost + ':'
		    		        + String.valueOf(mTorSocks);
		    		
		    		final String localDNS = virtualGateway + ':' + "8091";//String.valueOf(TorServiceConstants.TOR_DNS_PORT_DEFAULT);
		        	final boolean localDnsTransparentProxy = true;
		        	
			        Builder builder = new Builder();
			        
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
			        
		        	Tun2Socks.Start(mInterface, VPN_MTU, virtualIP, virtualNetMask, localSocks , localDNS , localDnsTransparentProxy);
		        	
		        	isRestart = false;
		        	
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
    	   
        ArrayList<TorifiedApp> apps = AppManager.getApps(this, TorServiceUtils.getSharedPrefs(getApplicationContext()));
    
        boolean perAppEnabled = false;
        
        for (TorifiedApp app : apps)
        {
        	if (app.isTorified() && (!app.getPackageName().equals(getPackageName())))
        	{
        		builder.addAllowedApplication(app.getPackageName());
        		perAppEnabled = true;
        	}
        	
        }
    
        if (!perAppEnabled)
        	builder.addDisallowedApplication(getPackageName());
    
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
    
    private void startDNS (String dns, int port) throws IOException, TimeoutException
    {
    	makePdnsdConf(this, dns, port,OrbotApp.filePdnsd.getParentFile() );
    	
        ArrayList<String> customEnv = new ArrayList<String>();
    	String baseDirectory = OrbotApp.filePdnsd.getParent();
        Shell shell = Shell.startShell(customEnv, baseDirectory);
        
        String cmdString = OrbotApp.filePdnsd.getCanonicalPath() + 
        		" -c " + baseDirectory + "/pdnsd.conf";
    
        SimpleCommand shellCommand = new SimpleCommand(cmdString);
        
        shell.add(shellCommand).waitForFinish();
    
        Log.i(TAG,"PDNSD: " + shellCommand.getExitCode() + ": " + shellCommand.getOutput());
        
    }
    
    public static void makePdnsdConf(Context context, String dns, int port, File fileDir) throws FileNotFoundException {
        String conf = String.format(context.getString(R.string.pdnsd_conf), dns, port);

        File f = new File(fileDir,"pdnsd.conf");

        if (f.exists()) {
                f.delete();
        }

        FileOutputStream fos = new FileOutputStream(f, false);
    	PrintStream ps = new PrintStream(fos);
    	ps.print(conf);
    	ps.close();
    	
        //f.withWriter { out -> out.print conf };
        
        
        File cache = new File(fileDir,"pdnsd.cache");

        if (!cache.exists()) {
                try {
                        cache.createNewFile();
                } catch (Exception e) {

                }
        }
}

    
}
