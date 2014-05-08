/* Copyright (c) 2009-2011, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info/apps/orbot */
/* See LICENSE for licensing information */
/*
 * Code for iptables binary management taken from DroidWall GPLv3
 * Copyright (C) 2009-2010  Rodrigo Zechin Rosauro
 */

package org.torproject.android.service;


import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import net.freehaven.tor.control.ConfigEntry;
import net.freehaven.tor.control.EventHandler;
import net.freehaven.tor.control.TorControlConnection;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;
import org.torproject.android.Orbot;
import org.torproject.android.R;
import org.torproject.android.TorConstants;
import org.torproject.android.Utils;
import org.torproject.android.settings.AppManager;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

public class TorService extends Service implements TorServiceConstants, TorConstants, EventHandler
{
	
	public static boolean ENABLE_DEBUG_LOG = false;
	
	private static int currentStatus = STATUS_OFF;
	
	private final static int CONTROL_SOCKET_TIMEOUT = 3000;
		
	private TorControlConnection conn = null;
	private Socket torConnSocket = null;
	private int mLastProcessId = -1;
	
	private static final int NOTIFY_ID = 1;
	private static final int TRANSPROXY_NOTIFY_ID = 2;
	private static final int ERROR_NOTIFY_ID = 3;
	private static final int HS_NOTIFY_ID = 3;
	
	private boolean prefPersistNotifications = true;
	
	private static final int MAX_START_TRIES = 3;

    private LinkedHashMap<String,String> configBuffer = null;
    private LinkedHashMap<String,String> resetBuffer = null;
    
   //   private String appHome;
    private File appBinHome;
    private File appCacheHome;
    
    private File fileTor;
    private File filePolipo;
    private File fileObfsclient;
    private File fileXtables;
    
    private File fileTorRc;
    
    private TorTransProxy mTransProxy;

	private long mTotalTrafficWritten = 0;
	private long mTotalTrafficRead = 0;
	private boolean mConnectivity = true; 

	private long lastRead = -1;
	private long lastWritten = -1;
	
	private NotificationManager mNotificationManager = null;
	private Builder mNotifyBuilder;

    private boolean mHasRoot = false;
    private boolean mEnableTransparentProxy = false;
    private boolean mTransProxyAll = false;
    private boolean mTransProxyTethering = false;
    

    private ArrayList<String> callbackBuffer = new ArrayList<String>();
    private boolean inCallbackStatus = false;
    private boolean inCallback = false;
    
		
    public void logMessage(String msg)
    {
    	if (ENABLE_DEBUG_LOG)
    	{
    		Log.d(TAG,msg);
    		sendCallbackLogMessage(msg);	

    	}
    }
    
    public void logException(String msg, Exception e)
    {
    	if (ENABLE_DEBUG_LOG)
    	{
    		Log.e(TAG,msg,e);
    	}
    	
		sendCallbackLogMessage(msg);	

    }
    
    
    private boolean findExistingProc () 
    {
    	if (fileTor != null)
    	{
	    	try
	    	{
	
	    		mLastProcessId = initControlConnection(1);
				
	 			if (mLastProcessId != -1)
	 			{

		            sendCallbackLogMessage (getString(R.string.found_existing_tor_process));
		
		 			processSettingsImpl();
		 				
		 			String state = conn.getInfo("dormant");
		 			if (state != null && Integer.parseInt(state) == 0)
		 				currentStatus = STATUS_ON;
		 			else
		 				currentStatus = STATUS_CONNECTING;
						
					return true;
	 			}
		 		
		 		
		 		return false;
	    	}
	    	catch (Exception e)
	    	{
	    		Log.e(TAG,"error finding proc",e);
	    		return false;
	    	}
    	}
    	else
    		return false;
    }
    

    /* (non-Javadoc)
	 * @see android.app.Service#onLowMemory()
	 */
    @Override
	public void onLowMemory() {
		super.onLowMemory();
		
		logNotice( "Low Memory Warning!");
		
	}


	/* (non-Javadoc)
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		
		if (intent != null)
			logNotice( "onUnbind Called: " + intent.getAction());
		
		return super.onUnbind(intent);
		
		
	}

	public int getTorStatus ()
    {
    	
    	return currentStatus;
    	
    }
	
	private void clearNotifications ()
	{
		if (mNotificationManager != null)
			mNotificationManager.cancelAll();
		
	}
   
 	private void showToolbarNotification (String notifyMsg, int notifyId, int icon, boolean isOngoing)
 	{
 				    
		if (mNotifyBuilder == null)
		{
			
			//Reusable code.
			Intent intent = new Intent(TorService.this, Orbot.class);
			PendingIntent pendIntent = PendingIntent.getActivity(TorService.this, 0, intent, 0);
			
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				
			if (mNotifyBuilder == null)
			{
				mNotifyBuilder = new NotificationCompat.Builder(this)
					.setContentTitle(getString(R.string.app_name))
					.setContentText( getString(R.string.status_activated))
					.setSmallIcon(R.drawable.ic_stat_tor);

				mNotifyBuilder.setContentIntent(pendIntent);
			}		
								
		}

		mNotifyBuilder.setContentText(notifyMsg);
		mNotifyBuilder.setSmallIcon(icon);
		
		if (notifyId == ERROR_NOTIFY_ID)
		{
			mNotifyBuilder.setTicker(notifyMsg);
			mNotifyBuilder.setOngoing(false);
			mNotifyBuilder.setLights(Color.RED, 1000, 1000);
			mNotifyBuilder.setSmallIcon(R.drawable.ic_stat_notifyerr);
		}
		
		if (isOngoing)
		{
			startForeground(notifyId,
	    			mNotifyBuilder.build());
		
		}
		else
		{
			mNotificationManager.notify(
						notifyId,
		    			mNotifyBuilder.build());
		}	
		
 	}
    
    /* (non-Javadoc)
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		
		try
		{
			sendCallbackLogMessage("Welcome back, Carter!");
		}
		catch (Exception e)
		{
			Log.e(TAG,"unable to init Tor",e);
			throw new RuntimeException("Unable to init Tor");
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {

		try
		{
			
			new startTorOperation().execute(intent);
			
		    return START_STICKY;
		    
		}
		catch (Exception e)
		{
			logException ("Error starting service",e);
			return Service.START_REDELIVER_INTENT;
		}

	}
	
    private class startTorOperation extends AsyncTask<Intent, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Intent... params) {
          
        	try
	    	{
        		Intent intent = params[0];
        		
        		initBinaries();
    			
    			
     		   IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
     		   registerReceiver(mNetworkStateReceiver , mNetworkStateFilter);
     	
     			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
     	
     			if (intent != null && intent.getAction()!=null && intent.getAction().equals("onboot"))
     			{
     				
     				boolean startOnBoot = TorServiceUtils.getSharedPrefs(getApplicationContext()).getBoolean("pref_start_boot",false);
     				
     				if (startOnBoot)
     				{
     					setTorProfile(PROFILE_ON);
     				}
     			}
     			else
     			{
     				findExistingProc();
     				
     			}
	    	}
	    	catch (Exception e)
	    	{
	    		Log.e(TAG,"error onBind",e);
	    	}
        	
        	
            return true;
        }

    }

	

	
	
    public void onDestroy ()
    {
    	super.onDestroy();
    	
    	if (currentStatus == STATUS_ON)
    	{
    		this.showToolbarNotification("Tor service stopped unexpectedly", ERROR_NOTIFY_ID, R.drawable.ic_stat_notifyerr, false);
    	}
    	//Log.d(TAG,"onDestroy called");
    	
    	  // Unregister all callbacks.
        mCallbacks.kill();
        
        unregisterReceiver(mNetworkStateReceiver);
    }
    
    private void stopTor ()
    {
    	currentStatus = STATUS_OFF;
    	
    	try
    	{	
    		killTorProcess ();
    		
    		//stop the foreground priority and make sure to remove the persistant notification
    		stopForeground(true);
    		
    		currentStatus = STATUS_OFF;

    		if (mHasRoot && mEnableTransparentProxy)
    			disableTransparentProxy();
    	    
    		clearNotifications();
    		
    		sendCallbackStatusMessage(getString(R.string.status_disabled));

    	}
    	catch (Exception e)
    	{
    		Log.d(TAG, "An error occured stopping Tor",e);
    		logNotice("An error occured stopping Tor: " + e.getMessage());
    		sendCallbackStatusMessage(getString(R.string.something_bad_happened));

    	}
    }
    
 
	private String getHiddenServiceHostname ()
	{

		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
		
        boolean enableHiddenServices = prefs.getBoolean("pref_hs_enable", false);
        
        if (enableHiddenServices)
        {
	    	File file = new File(appCacheHome, "hostname");
	    	
	    	if (file.exists())
	    	{
		    	try {
					String onionHostname = Utils.readString(new FileInputStream(file)).trim();
					showToolbarNotification(getString(R.string.hidden_service_on) + ' ' + onionHostname, HS_NOTIFY_ID, R.drawable.ic_stat_tor, true);
					Editor pEdit = prefs.edit();
					pEdit.putString("pref_hs_hostname",onionHostname);
					pEdit.commit();
				
					return onionHostname;
					
				} catch (FileNotFoundException e) {
					logException("unable to read onion hostname file",e);
					showToolbarNotification(getString(R.string.unable_to_read_hidden_service_name), ERROR_NOTIFY_ID, R.drawable.ic_stat_notifyerr, false);
					return null;
				}
	    	}
	    	else
	    	{
				showToolbarNotification(getString(R.string.unable_to_read_hidden_service_name), HS_NOTIFY_ID, R.drawable.ic_stat_notifyerr, false);
	
	    		
	    	}
        }
        
        return null;
	}
	
	
    private void killTorProcess () throws Exception
    {

		stopTorMinder();
		    	
    	if (conn != null)
		{

    		logNotice("Using control port to shutdown Tor");
    		
    		
			try {
				logNotice("sending SHUTDOWN signal to Tor process");
				conn.shutdownTor("SHUTDOWN");
				
				
			} catch (Exception e) {
				Log.d(TAG,"error shutting down Tor via connection",e);
			}
			
			conn = null;
		}
    	

		killProcess(filePolipo);
		killProcess(fileObfsclient);
		
		
    }
    
    private void killProcess (File fileProcBin) throws IOException
    {
    	int procId = -1;
    	Shell shell = Shell.startShell();
    	
    	while ((procId = TorServiceUtils.findProcessId(fileProcBin.getAbsolutePath())) != -1)
		{
			
			logNotice("Found fileObfsclient PID=" + procId + " - killing now...");
			
			SimpleCommand killCommand = new SimpleCommand("toolbox kill " + procId);
			shell.add(killCommand);
			killCommand = new SimpleCommand("kill " + procId);
			shell.add(killCommand);
		}
    	
    	shell.close();
    }
   
    private void logNotice (String msg)
    {
    	if (msg != null && msg.trim().length() > 0)
    	{
    		if (ENABLE_DEBUG_LOG)        	
        		Log.d(TAG, msg);
    	
    		sendCallbackLogMessage(msg);
    	}
    }
    
    private void initBinaries () throws Exception
    {

    	if (appBinHome == null)
    		appBinHome = getDir(DIRECTORY_TOR_BINARY,Application.MODE_PRIVATE);
    	
    	if (appCacheHome == null)
    		appCacheHome = getDir(DIRECTORY_TOR_DATA,Application.MODE_PRIVATE);
    	
    	fileTor= new File(appBinHome, TOR_ASSET_KEY);
    	
    	filePolipo = new File(appBinHome, POLIPO_ASSET_KEY);
		
    	fileObfsclient = new File(appBinHome, OBFSCLIENT_ASSET_KEY);
		
		fileTorRc = new File(appBinHome, TORRC_ASSET_KEY);
		
		fileXtables = new File(appBinHome, IPTABLES_ASSET_KEY);
		
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
		String version = prefs.getString(PREF_BINARY_TOR_VERSION_INSTALLED,null);

		logNotice("checking binary version: " + version);
		
		if (version == null || (!version.equals(BINARY_TOR_VERSION)))
		{
			stopTor();
			
			logNotice("upgrading binaries to latest version: " + BINARY_TOR_VERSION);
			
			TorResourceInstaller installer = new TorResourceInstaller(this, appBinHome); 
			boolean success = installer.installResources();
			
			if (success)
				prefs.edit().putString(PREF_BINARY_TOR_VERSION_INSTALLED,BINARY_TOR_VERSION).commit();	
		}
		else if (!fileTorRc.exists())
		{
			stopTor();

			logNotice("upgrading binaries to latest version: " + BINARY_TOR_VERSION);
			
			TorResourceInstaller installer = new TorResourceInstaller(this, appBinHome); 
			boolean success = installer.installResources();

			if (success)
				prefs.edit().putString(PREF_BINARY_TOR_VERSION_INSTALLED,BINARY_TOR_VERSION).commit();
				
		}
		
    }

    private boolean enableBinExec (File fileBin) throws Exception
    {
    	
    	logNotice(fileBin.getName() + ": PRE: Is binary exec? " + fileBin.canExecute());
  
    	if (!fileBin.canExecute())
    	{
			logNotice("(re)Setting permission on binary: " + fileBin.getAbsolutePath());	
			Shell shell = Shell.startShell(new ArrayList<String>(), appBinHome.getAbsolutePath());
		
			shell.add(new SimpleCommand("chmod " + CHMOD_EXE_VALUE + ' ' + fileBin.getAbsolutePath())).waitForFinish();
			
			File fileTest = new File(fileBin.getAbsolutePath());
			logNotice(fileTest.getName() + ": POST: Is binary exec? " + fileTest.canExecute());
			
			shell.close();
    	}
    	
		return fileBin.canExecute();
    }
    
    
    private void updateSettings ()
    {
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

    	mHasRoot = prefs.getBoolean(PREF_HAS_ROOT,false);
 		mEnableTransparentProxy = prefs.getBoolean("pref_transparent", false);
 		mTransProxyAll = prefs.getBoolean("pref_transparent_all", false);
	 	mTransProxyTethering = prefs.getBoolean("pref_transparent_tethering", false);
	 	
    	ENABLE_DEBUG_LOG = prefs.getBoolean("pref_enable_logging",false);
    	Log.i(TAG,"debug logging:" + ENABLE_DEBUG_LOG);

    	prefPersistNotifications = prefs.getBoolean(TorConstants.PREF_PERSIST_NOTIFICATIONS, true);
    }
    
    public void initTor () throws Exception
    {
    	
    	try
    	{
    		initBinaries();
    	}
    	catch (IOException e)
    	{
    		logNotice("There was a problem installing the Tor binaries: " + e.getLocalizedMessage());
    		Log.d(TAG,"error installing binaries",e);
    		return;
    	}
    	
    	enableBinExec(fileTor);
		enableBinExec(filePolipo);	
		enableBinExec(fileObfsclient);
		enableBinExec(fileXtables);
		
		updateSettings ();
    	
		currentStatus = STATUS_CONNECTING;

		logNotice(getString(R.string.status_starting_up));
		sendCallbackStatusMessage(getString(R.string.status_starting_up));
		
		runTorShellCmd();
		runPolipoShellCmd();
		
		if (mHasRoot && mEnableTransparentProxy)
			enableTransparentProxy(mTransProxyAll, mTransProxyTethering);
		
		//checkAddressAndCountry();
    }
   
    
    /*
     * activate means whether to apply the users preferences
     * or clear them out
     * 
     * the idea is that if Tor is off then transproxy is off
     */
    protected boolean enableTransparentProxy (boolean proxyAll, boolean enableTether) throws Exception
 	{
    	
 		if (mTransProxy == null)
 		{
 			mTransProxy = new TorTransProxy(this, fileXtables);
 			
 			
 		}
	 		
     	logMessage ("Transparent Proxying: enabling...");

		//TODO: Find a nice place for the next (commented) line
		//TorTransProxy.setDNSProxying(); 
		
		int code = 0; // Default state is "okay"
	
		//clear rules first
		mTransProxy.clearTransparentProxyingAll(this);
		
		if(proxyAll)
		{
			showToolbarNotification(getString(R.string.setting_up_full_transparent_proxying_), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor, false);

			code = mTransProxy.setTransparentProxyingAll(this);
		}
		else
		{
			showToolbarNotification(getString(R.string.setting_up_app_based_transparent_proxying_), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor, false);

			code = mTransProxy.setTransparentProxyingByApp(this,AppManager.getApps(this, TorServiceUtils.getSharedPrefs(getApplicationContext())));
		}
			
	
		logMessage ("TorTransProxy resp code: " + code);
		
		if (code == 0)
		{
			showToolbarNotification(getString(R.string.transparent_proxying_enabled), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor, false);

			if (enableTether)
			{
				showToolbarNotification(getString(R.string.transproxy_enabled_for_tethering_), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor, false);

				mTransProxy.enableTetheringRules(this);
				  
			}
		}
		else
		{
			showToolbarNotification(getString(R.string.warning_error_starting_transparent_proxying_), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor, false);

		}
	
		return true;
 	}
    
    /*
     * activate means whether to apply the users preferences
     * or clear them out
     * 
     * the idea is that if Tor is off then transproxy is off
     */
    private boolean disableTransparentProxy () throws Exception
 	{
    	
     	logMessage ("Transparent Proxying: disabling...");

 		if (mTransProxy == null)
 			mTransProxy = new TorTransProxy(this, fileXtables);
 		
 		mTransProxy.clearTransparentProxyingAll(this);
	    
     	return true;
 	}
    
    private void runTorShellCmd() throws Exception
    {
    	
		SharedPreferences prefs =TorServiceUtils.getSharedPrefs(getApplicationContext());

		String torrcPath = new File(appBinHome, TORRC_ASSET_KEY).getAbsolutePath();
		
		boolean transProxyTethering = prefs.getBoolean("pref_transparent_tethering", false);
 		
		if (transProxyTethering)
		{
			torrcPath = new File(appBinHome, TORRC_TETHER_KEY).getAbsolutePath();
		}

		int torRetryWaitTimeMS = 1000;
		
		sendCallbackStatusMessage(getString(R.string.status_starting_up));
		
		//start Tor in the background
		Shell shell = Shell.startShell();
		SimpleCommand cmdTor = new SimpleCommand(fileTor.getAbsolutePath() + " DataDirectory " + appCacheHome.getAbsolutePath() + " -f " + torrcPath + " &");
		shell.add(cmdTor);
		
		Thread.sleep(torRetryWaitTimeMS);
		
		//now try to connect
		mLastProcessId = initControlConnection (3);

		if (mLastProcessId == -1)
		{
			logNotice(getString(R.string.couldn_t_start_tor_process_) + "; exit=" + cmdTor.getExitCode() + ": " + cmdTor.getOutput());
			sendCallbackStatusMessage(getString(R.string.couldn_t_start_tor_process_));
			
			throw new Exception ("Unable to start Tor");
		}
		else
		{
		
			logNotice("Tor started; process id=" + mLastProcessId);
			
			processSettingsImpl();
	    }
		
		shell.close();
		
		startTorMinder ();
    }
    
    private void runPolipoShellCmd () throws Exception
    {
    	
    	logNotice( "Starting polipo process");
    	
			int polipoProcId = TorServiceUtils.findProcessId(filePolipo.getAbsolutePath());

			StringBuilder log = null;
			
			int attempts = 0;
			
			Shell shell = Shell.startShell();
			
    		if (polipoProcId == -1)
    		{
    			log = new StringBuilder();
    			
    			String polipoConfigPath = new File(appBinHome, POLIPOCONFIG_ASSET_KEY).getAbsolutePath();
    			SimpleCommand cmdPolipo = new SimpleCommand(filePolipo.getAbsolutePath() + " -c " + polipoConfigPath + " &");
    			
    			shell.add(cmdPolipo);
    			
    			//wait one second to make sure it has started up
    			Thread.sleep(1000);
    			
    			while ((polipoProcId = TorServiceUtils.findProcessId(filePolipo.getAbsolutePath())) == -1  && attempts < MAX_START_TRIES)
    			{
    				logNotice("Couldn't find Polipo process... retrying...\n" + log);
    				Thread.sleep(3000);
    				attempts++;
    			}
    			
    			logNotice(log.toString());
    		}
    		
			sendCallbackLogMessage(getString(R.string.privoxy_is_running_on_port_) + PORT_HTTP);
			
    		logNotice("Polipo process id=" + polipoProcId);
			
    		shell.close();
    		
    }
    
    /*
	public String generateHashPassword ()
	{
		
		PasswordDigest d = PasswordDigest.generateDigest();
	      byte[] s = d.getSecret(); // pass this to authenticate
	      String h = d.getHashedPassword(); // pass this to the Tor on startup.

		return null;
	}*/
	
	private synchronized int initControlConnection (int maxAttempts) throws Exception, RuntimeException
	{
			int i = 0;
			
			if (conn != null)
			{
				 String torProcId = conn.getInfo("process/pid");
				  return Integer.parseInt(torProcId);
			}
			else
			{
			
				while (conn == null && i++ < maxAttempts)
				{
					try
					{
						logNotice( "Connecting to control port: " + TOR_CONTROL_PORT);
						
						torConnSocket = new Socket(IP_LOCALHOST, TOR_CONTROL_PORT);
						torConnSocket.setSoTimeout(CONTROL_SOCKET_TIMEOUT);
						
				        conn = TorControlConnection.getConnection(torConnSocket);
				        
						logNotice( "SUCCESS connected to Tor control port");
				        
				        File fileCookie = new File(appCacheHome, TOR_CONTROL_COOKIE);
				        
				        if (fileCookie.exists())
				        {
					        byte[] cookie = new byte[(int)fileCookie.length()];
					        DataInputStream fis = new DataInputStream(new FileInputStream(fileCookie));
					        fis.read(cookie);
					        fis.close();
					        conn.authenticate(cookie);
					        		
					        logNotice( "SUCCESS - authenticated to control port");
					        
							sendCallbackStatusMessage(getString(R.string.tor_process_starting) + ' ' + getString(R.string.tor_process_complete));
		
					        addEventHandler();
					    
					        String torProcId = conn.getInfo("process/pid");
					        
					        return Integer.parseInt(torProcId);
					        
				        }
				        else
				        {
				        	logNotice ("Tor authentication cookie does not exist yet; trying again...");
				        }
					}
					catch (Exception ce)
					{
						conn = null;
						logNotice( "Error connecting to Tor local control port: " + ce.getLocalizedMessage());
						 
					//	Log.d(TAG,"Attempt: Error connecting to control port: " + ce.getLocalizedMessage(),ce);
					}
					
					sendCallbackStatusMessage(getString(R.string.tor_process_waiting));
					Thread.sleep(3000);
					
				}
			}		
		
			return -1;

	}
	
	private void checkAddressAndCountry () throws IOException
	{

        if (TorService.ENABLE_DEBUG_LOG)
        {
        	String torExternalAddress = conn.getInfo("address");
        	String torCountry = conn.getInfo("ip-to-country/" + torExternalAddress);
        
        	Log.d(TAG,"external address=" + torExternalAddress);
        	Log.d(TAG,"external country=" + torCountry);
        	
        }
        
	}
	
	/*
	private void getTorStatus () throws IOException
	{
		try
		{
			 
			if (conn != null)
			{
				 // get a single value.
			      
			       // get several values
			       
			       if (currentStatus == STATUS_CONNECTING)
			       {
				       //Map vals = conn.getInfo(Arrays.asList(new String[]{
				         // "status/bootstrap-phase", "status","version"}));
			
				       String bsPhase = conn.getInfo("status/bootstrap-phase");
				       Log.d(TAG, "bootstrap-phase: " + bsPhase);
				       
				       
			       }
			       else
			       {
			    	 //  String status = conn.getInfo("status/circuit-established");
			    	 //  Log.d(TAG, "status/circuit-established=" + status);
			       }
			}
		}
		catch (Exception e)
		{
			Log.d(TAG, "Unable to get Tor status from control port");
			currentStatus = STATUS_UNAVAILABLE;
		}
		
	}*/
	
	
	public void addEventHandler () throws IOException
	{
	       // We extend NullEventHandler so that we don't need to provide empty
	       // implementations for all the events we don't care about.
	       // ...
		logNotice( "adding control port event handler");

		conn.setEventHandler(this);
	    
		conn.setEvents(Arrays.asList(new String[]{
	          "ORCONN", "CIRC", "NOTICE", "WARN", "ERR","BW"}));
	      // conn.setEvents(Arrays.asList(new String[]{
	        //  "DEBUG", "INFO", "NOTICE", "WARN", "ERR"}));

		logNotice( "SUCCESS added control port event handler");
	    
	    

	}
	
		/**
		 * Returns the port number that the HTTP proxy is running on
		 */
		public int getHTTPPort() throws RemoteException {
			return TorServiceConstants.PORT_HTTP;
		}

		/**
		 * Returns the port number that the SOCKS proxy is running on
		 */
		public int getSOCKSPort() throws RemoteException {
			return TorServiceConstants.PORT_SOCKS;
		}


		
		
		public int getProfile() throws RemoteException {
			//return mProfile;
			return PROFILE_ON;
		}
		
		public void setTorProfile(int profile)  {
		//	logNotice("Tor profile set to " + profile);
			
			if (profile == PROFILE_ON)
			{
 				
	           new StartStopTorOperation().execute(true);
			}
			else if (profile == PROFILE_OFF)
			{

		       new StartStopTorOperation().execute(false);
				
			}
		}
		
		private class StartStopTorOperation extends AsyncTask<Boolean, Void, Boolean> {
	        @Override
	        protected Boolean doInBackground(Boolean... params) {
	          
	        	if (params[0].booleanValue() == true)
	        	{
	        		
	        		currentStatus = STATUS_CONNECTING;
		            sendCallbackStatusMessage (getString(R.string.status_starting_up));

		            try
		   		     {
		   			   initTor();
		   		     }
		   		     catch (Exception e)
		   		     {				
		   		    	
		   		    	logException("Unable to start Tor: " + e.toString(),e);	
		   		    	 currentStatus = STATUS_OFF;
		   		    	 showToolbarNotification(getString(R.string.unable_to_start_tor) + ": " + e.getMessage(), ERROR_NOTIFY_ID, R.drawable.ic_stat_notifyerr, false);
		   		     }
	        	}
	        	else
	        	{
	        		currentStatus = STATUS_OFF;
		            sendCallbackStatusMessage (getString(R.string.status_shutting_down));
		          
		            stopTor();
		            
	        	}
	        	
	        	
	            return true;
	        }

	    }


	public void message(String severity, String msg) {
		
		
		logNotice(severity + ": " + msg);
          
          if (msg.indexOf(TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE)!=-1)
          {
        	  currentStatus = STATUS_ON;

        	  showToolbarNotification(getString(R.string.status_activated), NOTIFY_ID, R.drawable.ic_stat_tor, prefPersistNotifications);
          }
        
      	
          
	}

	public void newDescriptors(List<String> orList) {
		
	}


	public void orConnStatus(String status, String orName) {
		
		
			StringBuilder sb = new StringBuilder();
			sb.append("orConnStatus (");
			sb.append(parseNodeName(orName) );
			sb.append("): ");
			sb.append(status);
			
			logNotice(sb.toString());
		
	}


	public void streamStatus(String status, String streamID, String target) {
		
			StringBuilder sb = new StringBuilder();
			sb.append("StreamStatus (");
			sb.append((streamID));
			sb.append("): ");
			sb.append(status);
			
			logNotice(sb.toString());
		
	}


	public void unrecognized(String type, String msg) {
		
			StringBuilder sb = new StringBuilder();
			sb.append("Message (");
			sb.append(type);
			sb.append("): ");
			sb.append(msg);
			
			logNotice(sb.toString());
	
		
	}
	
	public void bandwidthUsed(long read, long written) {
		
		if (read != lastRead || written != lastWritten)
		{
			StringBuilder sb = new StringBuilder();
			sb.append(getString(R.string.bandwidth_));
			sb.append(" ");
			sb.append(formatCount(read));
			sb.append(" ");
			sb.append(getString(R.string.down));
			sb.append(" / ");
			sb.append(formatCount(written));
			sb.append(" ");
			sb.append(getString(R.string.up));
		   	
			int iconId = R.drawable.ic_stat_tor;
			
			if (read > 0 || written > 0)
				iconId = R.drawable.ic_stat_tor_xfer;
			
			if (mConnectivity && prefPersistNotifications)
	        	  showToolbarNotification(sb.toString(), NOTIFY_ID, iconId, prefPersistNotifications);

			mTotalTrafficWritten += written;
			mTotalTrafficRead += read;
			
			sendCallbackStatusMessage(written, read, mTotalTrafficWritten, mTotalTrafficRead); 

	//		if(++notificationCounter%10==0)
		//	    startService(new Intent(ITorService.class.getName()));

		}
		
		lastWritten = written;
		lastRead = read;

	}
	
	private String formatCount(long count) {
		// Converts the supplied argument into a string.
		// Under 2Mb, returns "xxx.xKb"
		// Over 2Mb, returns "xxx.xxMb"
		if (count < 1e6)
			return ((float)((int)(count*10/1024))/10 + "kbps");
		return ((float)((int)(count*100/1024/1024))/100 + "mbps");
		
   		//return count+" kB";
	}
	
	/*
	class TotalUpdaterRunnable implements Runnable
	{

		public void run ()
		{
		
			while (currentStatus != STATUS_OFF)
			{
				try
				{
					try
					{
						mTotalTrafficWritten =  Long.parseLong(conn.getInfo("traffic/written"));
						mTotalTrafficRead = Long.parseLong(conn.getInfo("traffic/read"));
						
					}
					catch (Exception ioe)
					{
						Log.e(TAG,"error reading control port traffic",ioe);
					}
					
					Thread.sleep(3000); //wait three seconds
				}
				catch (Exception e)
				{
					//nada
				}
			}
		}
		
	}*/
   	
	public void circuitStatus(String status, String circID, String path) {
		
		if (status.equals("BUILT") || status.equals("CLOSED"))
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Circuit (");
			sb.append((circID));
			sb.append(") ");
			sb.append(status);
			sb.append(": ");
			
			StringTokenizer st = new StringTokenizer(path,",");
			String node = null;
			
			while (st.hasMoreTokens())
			{
				node = st.nextToken();
				
				sb.append(parseNodeName(node));
				
				
				if (st.hasMoreTokens())
					sb.append (" > ");
			}
			
			logNotice(sb.toString());
		}
		
		/**
		if (currentStatus != STATUS_ON)
		{
			try {
				String state;
		
				state = conn.getInfo("dormant");
			
				if (state != null && Integer.parseInt(state) == 0)
					currentStatus = STATUS_ON;
				else
					currentStatus = STATUS_CONNECTING;
				
			} catch (IOException e) {
				logException("Error getting state from Tor control port",e);
			}
		}*/
	
	}
	
	private String parseNodeName(String node)
	{
		if (node.indexOf('=')!=-1)
		{
			return (node.substring(node.indexOf("=")+1));
		}
		else if (node.indexOf('~')!=-1)
		{
			return (node.substring(node.indexOf("~")+1));
		}
		else
			return node;
	}
	
    public IBinder onBind(Intent intent) {
        
    	new initTorOperation().execute(true);
    	
    	return mBinder;
    }
    
    private class initTorOperation extends AsyncTask<Boolean, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Boolean... params) {
          
        	try
	    	{
	    		initBinaries();
	    		findExistingProc ();
	    	}
	    	catch (Exception e)
	    	{
	    		Log.e(TAG,"error onBind",e);
	    	}
        	
        	
            return true;
        }

    }
	
	
    /**
     * This is a list of callbacks that have been registered with the
     * service.  Note that this is package scoped (instead of private) so
     * that it can be accessed more efficiently from inner classes.
     */
    final RemoteCallbackList<ITorServiceCallback> mCallbacks
            = new RemoteCallbackList<ITorServiceCallback>();


    /**
     * The IRemoteInterface is defined through IDL
     */
    private final ITorService.Stub mBinder = new ITorService.Stub() {
    	
        
		public void registerCallback(ITorServiceCallback cb) {
            if (cb != null) mCallbacks.register(cb);
        }
        public void unregisterCallback(ITorServiceCallback cb) {
            if (cb != null) mCallbacks.unregister(cb);
        }
        public int getStatus () {
        	return getTorStatus();
        }
        
        public void setProfile (int profile)
        {
        	setTorProfile(profile);
        	
        }
        
        public void processSettings ()
        {
        	
        	Thread thread = new Thread()
        	{
        	
        		public void run ()
        		{
		        	try {
		        	 	
		        		processSettingsImpl ();
		
				    	
					} catch (Exception e) {
						logException ("error applying mPrefs",e);
					}
        		}
        	};
        	
        	thread.start();
        }
 

    	public String getInfo (String key) {
    		try
    		{
    			if(conn !=null)
    			{
    				String m = conn.getInfo(key);
					return m;
					
    			}
    		}
    		catch(IOException ioe)
    		{
    			Log.e(TAG,"Unable to get Tor information",ioe);
    			logNotice("Unable to get Tor information"+ioe.getMessage());
    		}
			return null;
        }
        
        public String getConfiguration (String name)
        {
        	try
        	{
	        	if (conn != null)
	        	{
	        		StringBuffer result = new StringBuffer();
	        		
	        		List<ConfigEntry> listCe = conn.getConf(name);
	        		
	        		Iterator<ConfigEntry> itCe = listCe.iterator();
	        		ConfigEntry ce = null;
	        		
	        		while (itCe.hasNext())
	        		{
	        			ce = itCe.next();
	        			
	        			result.append(ce.key);
	        			result.append(' ');
	        			result.append(ce.value);
	        			result.append('\n');
	        		}
	        		
	   	       		return result.toString();
	        	}
        	}
        	catch (IOException ioe)
        	{
        		
        		logException("Unable to get Tor configuration: " + ioe.getMessage(),ioe);
        	}
        	
        	return null;
        }
        
        /**
         * Set configuration
         **/
        public boolean updateConfiguration (String name, String value, boolean saveToDisk)
        {
        	if (configBuffer == null)
        		configBuffer = new LinkedHashMap<String,String>();
	        
        	if (resetBuffer == null)
        		resetBuffer = new LinkedHashMap<String,String>();
	        
        	if (value == null || value.length() == 0)
        	{
        		resetBuffer.put(name,"");
        		
        	}
        	else
        	{
        		configBuffer.put(name,value);
        	}
	        
        	return false;
        }
        
        public void newIdentity () 
        {
        	//it is possible to not have a connection yet, and someone might try to newnym
        	if (conn != null)
        	{
	        	new Thread ()
	        	{
	        		public void run ()
	        		{
	        			try { conn.signal("NEWNYM"); 
	        			
	        			//checkAddressAndCountry();
	        			
	        			}
	        			catch (IOException ioe){
	        				logMessage("error requesting newnym: " + ioe.getLocalizedMessage());
	        			}
	        		}
	        	}.start();
        	}
        }
        
	    public boolean saveConfiguration ()
	    {
	    	try
        	{
	        	if (conn != null)
	        	{
	        		
	        		 if (resetBuffer != null && resetBuffer.size() > 0)
				        {	
				        	conn.resetConf(resetBuffer.keySet());
				        	resetBuffer = null;
				        }
	   	       
	        		 if (configBuffer != null && configBuffer.size() > 0)
				        {
	        			 	
	        			 	for (String key : configBuffer.keySet())
	        			 	{
	        			 		
	        			 		String value = configBuffer.get(key);
	        			 		
	        			 		if (TorService.ENABLE_DEBUG_LOG)
	        			 			logMessage("Setting conf: " + key + "=" + value);
	        			 		
	        			 		conn.setConf(key, value);
	        			 		
	        			 	}
				        	configBuffer = null;
				        }
	   	       
	   	       		// Flush the configuration to disk.
	        		//this is doing bad things right now NF 22/07/10
	   	       		//conn.saveConf();
	
	   	       		return true;
	        	}
        	}
        	catch (Exception ioe)
        	{
        		
        		logException("Unable to update Tor configuration: " + ioe.getMessage(),ioe);

        	}
        	
        	return false;
        	
	    }
	    
    };
    
    private synchronized void sendCallbackStatusMessage (String newStatus)
    {
    	 
    	if (mCallbacks == null)
    		return;
    	
        // Broadcast to all clients the new value.
        final int N = mCallbacks.beginBroadcast();
        
        inCallback = true;
        
        if (N > 0)
        {
        	 for (int i=0; i<N; i++) {
		            try {
		                mCallbacks.getBroadcastItem(i).statusChanged(newStatus);
		                
		                
		            } catch (RemoteException e) {
		                // The RemoteCallbackList will take care of removing
		                // the dead object for us.
		            }
		        }
        }
        
        mCallbacks.finishBroadcast();
        inCallback = false;
    }
   
    private synchronized void sendCallbackStatusMessage (long upload, long download, long written, long read)
    {
    	 
    	if (mCallbacks == null)
    		return;
    	
        // Broadcast to all clients the new value.
        final int N = mCallbacks.beginBroadcast();
        
        inCallback = true;
        
        if (N > 0)
        {
        	 for (int i=0; i<N; i++) {
		            try {
		                mCallbacks.getBroadcastItem(i).updateBandwidth(upload, download, written, read);
		                
		            } catch (RemoteException e) {
		                // The RemoteCallbackList will take care of removing
		                // the dead object for us.
		            }
		        }
        }
        
        mCallbacks.finishBroadcast();
        inCallback = false;
    }
    
    
    private synchronized void sendCallbackLogMessage (String logMessage)
    {
    	 
    	if (mCallbacks == null)
    		return;
    	
    	callbackBuffer.add(logMessage);

    	if (!inCallback)
    	{

	        inCallback = true;
	        // Broadcast to all clients the new value.
	        final int N = mCallbacks.beginBroadcast();
	        
	
	        if (N > 0)
	        {
	        
	        	Iterator<String> it = callbackBuffer.iterator();
	        	String status = null;
	        	
	        	while (it.hasNext())
	        	{
	        		status = it.next();
	        		
			        for (int i=0; i<N; i++) {
			            try {
			                mCallbacks.getBroadcastItem(i).logMessage(status);
			                
			            } catch (RemoteException e) {
			                // The RemoteCallbackList will take care of removing
			                // the dead object for us.
			            }
			        }
	        	}
		        
		        callbackBuffer.clear();
	        }
	        
	        mCallbacks.finishBroadcast();
	        inCallback = false;
    	}
    	
    }
    
    /*
     *  Another way to do this would be to use the Observer pattern by defining the 
     *  BroadcastReciever in the Android manifest.
     */
    private final BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent) {

    		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

    		boolean doNetworKSleep = prefs.getBoolean(TorConstants.PREF_DISABLE_NETWORK, true);
    		
    		if (doNetworKSleep && mBinder != null)
    		{
    			final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        	    final NetworkInfo netInfo = cm.getActiveNetworkInfo();

        	    if(netInfo != null && netInfo.isConnected()) {
        	        // WE ARE CONNECTED: DO SOMETHING
        	    	mConnectivity = true;
        	    }   
        	    else {
        	        // WE ARE NOT: DO SOMETHING ELSE
        	    	mConnectivity = false;
        	    }
        		
	    		try {
					mBinder.updateConfiguration("DisableNetwork", mConnectivity ? "0" : "1", false);
					mBinder.saveConfiguration();
					
					if (currentStatus == STATUS_ON)
					{
						if (!mConnectivity)
						{
							logNotice(context.getString(R.string.no_network_connectivity_putting_tor_to_sleep_));
							showToolbarNotification(getString(R.string.no_internet_connection_tor),NOTIFY_ID,R.drawable.ic_stat_tor_off,prefPersistNotifications);
							
						}
						else
						{
							logNotice(context.getString(R.string.network_connectivity_is_good_waking_tor_up_));
							showToolbarNotification(getString(R.string.status_activated),NOTIFY_ID,R.drawable.ic_stat_tor,prefPersistNotifications);

							if (mHasRoot && mEnableTransparentProxy)
								enableTransparentProxy(mTransProxyAll, mTransProxyTethering);
							
				        }
					}
					
	    		} catch (Exception e) {
					logException ("error updating state after network restart",e);
				}
    		}
    		
    	}
    };

    private boolean processSettingsImpl () throws RemoteException, IOException
    {
    	logNotice(getString(R.string.updating_settings_in_tor_service));
    	
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

		enableSocks ("127.0.0.1",9050,false);
		
		boolean useBridges = prefs.getBoolean(TorConstants.PREF_BRIDGES_ENABLED, false);
		
		//boolean autoUpdateBridges = prefs.getBoolean(TorConstants.PREF_BRIDGES_UPDATED, false);

        boolean becomeRelay = prefs.getBoolean(TorConstants.PREF_OR, false);
        boolean ReachableAddresses = prefs.getBoolean(TorConstants.PREF_REACHABLE_ADDRESSES,false);
        boolean enableHiddenServices = prefs.getBoolean("pref_hs_enable", false);

        boolean enableStrictNodes = prefs.getBoolean("pref_strict_nodes", false);
        String entranceNodes = prefs.getString("pref_entrance_nodes", "");
        String exitNodes = prefs.getString("pref_exit_nodes", "");
        String excludeNodes = prefs.getString("pref_exclude_nodes", "");
        
        String proxyType = prefs.getString("pref_proxy_type", null);
        if (proxyType != null && proxyType.length() > 0)
        {
        	String proxyHost = prefs.getString("pref_proxy_host", null);
        	String proxyPort = prefs.getString("pref_proxy_port", null);
        	String proxyUser = prefs.getString("pref_proxy_username", null);
        	String proxyPass = prefs.getString("pref_proxy_password", null);
        	
        	if ((proxyHost != null && proxyHost.length()>0) && (proxyPort != null && proxyPort.length() > 0))
        	{
        		mBinder.updateConfiguration(proxyType + "Proxy", proxyHost + ':' + proxyPort, false);
        		
        		if (proxyUser != null && proxyPass != null)
        		{
        			if (proxyType.equalsIgnoreCase("socks5"))
        			{
        				mBinder.updateConfiguration("Socks5ProxyUsername", proxyUser, false);
        				mBinder.updateConfiguration("Socks5ProxyPassword", proxyPass, false);
        			}
        			else
        				mBinder.updateConfiguration(proxyType + "ProxyAuthenticator", proxyUser + ':' + proxyPort, false);
        			
        		}
        		else if (proxyPass != null)
        			mBinder.updateConfiguration(proxyType + "ProxyAuthenticator", proxyUser + ':' + proxyPort, false);
        		
        		

        	}
        }
        
        if (entranceNodes.length() > 0 || exitNodes.length() > 0 || excludeNodes.length() > 0)
        {
        	//only apply GeoIP if you need it
	        File fileGeoIP = new File(appBinHome,GEOIP_ASSET_KEY);
	        File fileGeoIP6 = new File(appBinHome,GEOIP6_ASSET_KEY);
		        
	        try
	        {
		        if ((!fileGeoIP.exists()))
		        {
		        	TorResourceInstaller installer = new TorResourceInstaller(this, appBinHome); 
					boolean success = installer.installGeoIP();
		        	
		        }
		        
		        mBinder.updateConfiguration("GeoIPFile", fileGeoIP.getAbsolutePath(), false);
		        mBinder.updateConfiguration("GeoIPv6File", fileGeoIP6.getAbsolutePath(), false);

	        }
	        catch (Exception e)
	        {
	       	  showToolbarNotification (getString(R.string.error_installing_binares),ERROR_NOTIFY_ID,R.drawable.ic_stat_notifyerr, false);

	        	return false;
	        }
        }

        mBinder.updateConfiguration("EntryNodes", entranceNodes, false);
        mBinder.updateConfiguration("ExitNodes", exitNodes, false);
		mBinder.updateConfiguration("ExcludeNodes", excludeNodes, false);
		mBinder.updateConfiguration("StrictNodes", enableStrictNodes ? "1" : "0", false);
        
		if (useBridges)
		{
		
			logMessage ("Using bridges");
			String bridgeCfgKey = "Bridge";

			String bridgeList = prefs.getString(TorConstants.PREF_BRIDGES_LIST,null);

			if (bridgeList == null || bridgeList.length() == 0)
			{
				String msgBridge = getString(R.string.bridge_requires_ip) +
						getString(R.string.send_email_for_bridges);
				showToolbarNotification(msgBridge, ERROR_NOTIFY_ID, R.drawable.ic_stat_tor, false);
				logMessage(msgBridge);
			
				return false;
			}

			
			String bridgeDelim = "\n";
			
			if (bridgeList.indexOf(",") != -1)
			{
				bridgeDelim = ",";
			}
			
			showToolbarNotification(getString(R.string.notification_using_bridges) + ": " + bridgeList, TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor, false);
  
			StringTokenizer st = new StringTokenizer(bridgeList,bridgeDelim);
			while (st.hasMoreTokens())
			{
				String bridgeConfigLine = st.nextToken().trim();
				logMessage("Adding bridge: " + bridgeConfigLine);
				mBinder.updateConfiguration(bridgeCfgKey, bridgeConfigLine, false);

			}

			//check if any PT bridges are needed
			boolean obfsBridges = bridgeList.contains("obfs2")||bridgeList.contains("obfs3")||bridgeList.contains("scramblesuit");

			if (obfsBridges)
			{
				String bridgeConfig = "obfs2,obfs3,scramblesuit exec " + fileObfsclient.getAbsolutePath();
				
				logMessage ("Using OBFUSCATED bridges: " + bridgeConfig);
				
				mBinder.updateConfiguration("ClientTransportPlugin",bridgeConfig, false);
			}
			else
			{
				logMessage ("Using standard bridges");
			}
			


			mBinder.updateConfiguration("UpdateBridgesFromAuthority", "0", false);
			

			mBinder.updateConfiguration("UseBridges", "1", false);
				
			
		}
		else
		{
			mBinder.updateConfiguration("UseBridges", "0", false);

		}

        try
        {
            if (ReachableAddresses)
            {
                String ReachableAddressesPorts =
                    prefs.getString(TorConstants.PREF_REACHABLE_ADDRESSES_PORTS, "*:80,*:443");
                
                mBinder.updateConfiguration("ReachableAddresses", ReachableAddressesPorts, false);

            }
            else
            {
                mBinder.updateConfiguration("ReachableAddresses", "", false);
            }
        }
        catch (Exception e)
        {
     	  showToolbarNotification (getString(R.string.your_reachableaddresses_settings_caused_an_exception_),ERROR_NOTIFY_ID,R.drawable.ic_stat_notifyerr, false);

           return false;
        }

        try
        {
            if (becomeRelay && (!useBridges) && (!ReachableAddresses))
            {
                int ORPort =  Integer.parseInt(prefs.getString(TorConstants.PREF_OR_PORT, "9001"));
                String nickname = prefs.getString(TorConstants.PREF_OR_NICKNAME, "Orbot");

                String dnsFile = writeDNSFile ();
                
                mBinder.updateConfiguration("ServerDNSResolvConfFile", dnsFile, false);
                mBinder.updateConfiguration("ORPort", ORPort + "", false);
    			mBinder.updateConfiguration("Nickname", nickname, false);
    			mBinder.updateConfiguration("ExitPolicy", "reject *:*", false);

            }
            else
            {
            	mBinder.updateConfiguration("ORPort", "", false);
    			mBinder.updateConfiguration("Nickname", "", false);
    			mBinder.updateConfiguration("ExitPolicy", "", false);
            }
        }
        catch (Exception e)
        {
       	  showToolbarNotification (getString(R.string.your_relay_settings_caused_an_exception_),ERROR_NOTIFY_ID,R.drawable.ic_stat_notifyerr, false);

          
            return false;
        }

        if (enableHiddenServices)
        {
        	logNotice("hidden services are enabled");
        	
        	mBinder.updateConfiguration("HiddenServiceDir",appCacheHome.getAbsolutePath(), false);
        	//mBinder.updateConfiguration("RendPostPeriod", "600 seconds", false); //possible feature to investigate
        	
        	String hsPorts = prefs.getString("pref_hs_ports","");
        	
        	StringTokenizer st = new StringTokenizer (hsPorts,",");
        	String hsPortConfig = null;
        	int hsPort = -1;
        	
        	while (st.hasMoreTokens())
        	{
        		try
        		{
	        		hsPortConfig = st.nextToken();
	        		
	        		if (hsPortConfig.indexOf(":")==-1) //setup the port to localhost if not specifed
	        		{
	        			hsPortConfig = hsPortConfig + " 0.0.0.0:" + hsPortConfig;
	        		}
	        		
	        		logMessage("Adding hidden service on port: " + hsPortConfig);
	        		
	        		mBinder.updateConfiguration("HiddenServicePort",hsPortConfig, false);
	        		
	        		hsPort = Integer.parseInt(hsPortConfig.split(" ")[0]);

				} catch (NumberFormatException e) {
					Log.e(this.TAG,"error parsing hsport",e);
				} catch (Exception e) {
					Log.e(this.TAG,"error starting share server",e);
				}
        	}
        	
        	
        }
        else
        {
        	mBinder.updateConfiguration("HiddenServiceDir","", false);
        	
        }

        mBinder.saveConfiguration();
	
        return true;
    }
    
    private void enableSocks (String ip, int port, boolean safeSocks) throws RemoteException
    {
    	mBinder.updateConfiguration("SOCKSPort", ip + ":" + port + "", false);
    	mBinder.updateConfiguration("SafeSocks", safeSocks ? "1" : "0", false);
    	mBinder.updateConfiguration("TestSocks", "1", false);
    	mBinder.updateConfiguration("WarnUnsafeSocks", "1", false);
    	
    }
    
    private void blockPlaintextPorts (String portList) throws RemoteException
    {
    	
    	mBinder.updateConfiguration("RejectPlaintextPorts",portList,false);
    }
    
    //using Google DNS for now as the public DNS server
    private String writeDNSFile () throws IOException
    {
    	File file = new File(appBinHome,"resolv.conf");
    	
    	PrintWriter bw = new PrintWriter(new FileWriter(file));
    	bw.println("nameserver 8.8.8.8");
    	bw.println("nameserver 8.8.4.4");
    	bw.close();
    
    	return file.getAbsolutePath();
    }

	@SuppressLint("NewApi")
	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		
		switch (level)
		{
		
			case TRIM_MEMORY_BACKGROUND:
				logNotice("trim memory requested: app in the background");
			return;
			
		/**
		public static final int TRIM_MEMORY_BACKGROUND
		Added in API level 14
		Level for onTrimMemory(int): the process has gone on to the LRU list. This is a good opportunity to clean up resources that can efficiently and quickly be re-built if the user returns to the app.
		Constant Value: 40 (0x00000028)
		*/
		
			case TRIM_MEMORY_COMPLETE:

				logNotice("trim memory requested: cleanup all memory");
			return;
		/**
		public static final int TRIM_MEMORY_COMPLETE
		Added in API level 14
		Level for onTrimMemory(int): the process is nearing the end of the background LRU list, and if more memory isn't found soon it will be killed.
		Constant Value: 80 (0x00000050)
		*/
			case TRIM_MEMORY_MODERATE:

				logNotice("trim memory requested: clean up some memory");
			return;
				
		/**
		public static final int TRIM_MEMORY_MODERATE
		Added in API level 14
		Level for onTrimMemory(int): the process is around the middle of the background LRU list; freeing memory can help the system keep other processes running later in the list for better overall performance.
		Constant Value: 60 (0x0000003c)
		*/
		
			case TRIM_MEMORY_RUNNING_CRITICAL:

				logNotice("trim memory requested: memory on device is very low and critical");
			return;
		/**
		public static final int TRIM_MEMORY_RUNNING_CRITICAL
		Added in API level 16
		Level for onTrimMemory(int): the process is not an expendable background process, but the device is running extremely low on memory and is about to not be able to keep any background processes running. Your running process should free up as many non-critical resources as it can to allow that memory to be used elsewhere. The next thing that will happen after this is onLowMemory() called to report that nothing at all can be kept in the background, a situation that can start to notably impact the user.
		Constant Value: 15 (0x0000000f)
		*/
		
			case TRIM_MEMORY_RUNNING_LOW:

				logNotice("trim memory requested: memory on device is running low");
			return;
		/**
		public static final int TRIM_MEMORY_RUNNING_LOW
		Added in API level 16
		Level for onTrimMemory(int): the process is not an expendable background process, but the device is running low on memory. Your running process should free up unneeded resources to allow that memory to be used elsewhere.
		Constant Value: 10 (0x0000000a)
		*/
			case TRIM_MEMORY_RUNNING_MODERATE:

				logNotice("trim memory requested: memory on device is moderate");
			return;
		/**
		public static final int TRIM_MEMORY_RUNNING_MODERATE
		Added in API level 16
		Level for onTrimMemory(int): the process is not an expendable background process, but the device is running moderately low on memory. Your running process may want to release some unneeded resources for use elsewhere.
		Constant Value: 5 (0x00000005)
		*/
			case TRIM_MEMORY_UI_HIDDEN:

				logNotice("trim memory requested: app is not showing UI anymore");
			return;
				
		/**
		public static final int TRIM_MEMORY_UI_HIDDEN
		Level for onTrimMemory(int): the process had been showing a user interface, and is no longer doing so. Large allocations with the UI should be released at this point to allow memory to be better managed.
		Constant Value: 20 (0x00000014)
		*/
		}
		
	}
   
	private Timer mTorMinder;
	
	private void startTorMinder ()
	{
		mTorMinder = new Timer(true);
		mTorMinder.scheduleAtFixedRate(
		    new TimerTask() {
		      public void run() { 
		    	  
					if (currentStatus == STATUS_OFF)
					{
						mTorMinder.cancel();
					}
					else
					{
						
						try {
							int foundPrcId = TorServiceUtils.findProcessId(fileTor.getAbsolutePath());
							
							if (foundPrcId != -1)
							{
								mLastProcessId = foundPrcId;
								
								logNotice("Refreshed Tor process id: " + mLastProcessId);
								
							}
							else
							{
								logNotice("restarting Tor after it has been killed");
								killTorProcess();
								initTor();
							}
							
						} catch (Exception e1) {
							logException("Error in Tor heartbeat checker",e1);
						} 
					}
				
		    	  
		      }
		    }, 0, 30 * 1000); //every 30 seconds
	}
	
	private void stopTorMinder ()
	{
		if (mTorMinder != null)
			mTorMinder.cancel();
	}
    
   
}
