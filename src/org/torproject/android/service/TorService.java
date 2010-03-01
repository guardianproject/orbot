/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import net.freehaven.tor.control.ConfigEntry;
import net.freehaven.tor.control.EventHandler;
import net.freehaven.tor.control.TorControlConnection;

import org.torproject.android.Orbot;
import org.torproject.android.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

public class TorService extends Service implements TorServiceConstants, Runnable, EventHandler
{
	
	
	
	private static int currentStatus = STATUS_READY;
		
	private TorControlConnection conn = null;
	
	private static TorService _torInstance;
	
	private static final int NOTIFY_ID = 1;
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
    	super.onCreate();
       
    	Log.i(TAG,"TorService: onCreate");
    	
    	checkTorBinaries();
    	
    	findExistingProc ();
    	
    	_torInstance = this;

    	 
    }
    
    private boolean findExistingProc ()
    {
    	 int procId = TorServiceUtils.findProcessId(TorServiceConstants.TOR_BINARY_INSTALL_PATH);

 		if (procId != -1)
 		{
 			Log.i(TAG,"Found existing Tor process");
 			
            sendCallbackMessage ("found existing Tor process...");

 			try {
 				currentStatus = STATUS_CONNECTING;
				
 				initControlConnection();
				
				
				currentStatus = STATUS_ON;
				
				return true;
 						
			} catch (RuntimeException e) {
				Log.i(TAG,"Unable to connect to existing Tor instance,",e);
				currentStatus = STATUS_OFF;
				this.stopTor();
				
			} catch (Exception e) {
				Log.i(TAG,"Unable to connect to existing Tor instance,",e);
				currentStatus = STATUS_OFF;
				this.stopTor();
				
			}
 		}
 		
 		return false;
    	 
    }
    

    /* (non-Javadoc)
	 * @see android.app.Service#onLowMemory()
	 */
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		
		Log.i(TAG, "Low Memory Called");
		
	}


	/* (non-Javadoc)
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		
		Log.i(TAG, "onUnbind Called: " + intent.getAction());
		
		return super.onUnbind(intent);
		
		
	}

	public int getTorStatus ()
    {
    	
    	return currentStatus;
    	
    }
	
   
	private void showToolbarNotification (String title, String notifyMsg, int icon)
	{
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		
		CharSequence tickerText = title;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = title;
		CharSequence contentText = notifyMsg;
		
		Intent notificationIntent = new Intent(this, Orbot.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);


		mNotificationManager.notify(NOTIFY_ID, notification);


	}
    
    /* (non-Javadoc)
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		
		 Log.i(TAG,"on rebind");
	}


	/* (non-Javadoc)
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
	    /*
	     if (currentStatus == STATUS_ON && conn != null && webProxy != null)
	     {
	    	 //we are good to go
		     Log.i(TAG,"onStart: Tor is running");

	     }
	     else
	     {
		     Log.i(TAG,"onStart: Starting up Tor");

	    	 new Thread(this).start();
	     }
	     */
	}
	 
	public void run ()
	{
		boolean isRunning = _torInstance.findExistingProc ();
		
		if (!isRunning)
		{
	     try
	     {
		   initTor();
		   
		   
	     }
	     catch (Exception e)
	     {
	    	 currentStatus = STATUS_OFF;
	    	 this.showToolbarNotification("Orbot", "Unable to start Tor", R.drawable.tornotification);
	    	 Log.i(TAG,"Unable to start Tor: " + e.getMessage(),e);
	     }
		}
	}

	
    public void onDestroy ()
    {
    	super.onDestroy();
    	
    	  // Unregister all callbacks.
        mCallbacks.kill();
      
        
    	Log.i(TAG,"onDestroy called");
	     
    	stopTor();
    }
    
    private void stopTor ()
    {
    	currentStatus = STATUS_OFF;
    	
    		
    	sendCallbackMessage("Web proxy shutdown");
    	
		killTorProcess ();
				
		currentStatus = STATUS_READY;
    	
		showToolbarNotification ("Orbot","Anonymous browsing is disabled",R.drawable.tornotificationoff);
    	sendCallbackMessage("Anonymous browsing is disabled");

    }
    
 
   
    
    public void reloadConfig ()
    {
    	try
		{
	    	if (conn == null)
			{
				initControlConnection ();
			}
		
			if (conn != null)
			{
				 conn.signal("RELOAD");
			}
		}
    	catch (Exception e)
    	{
    		Log.i(TAG,"Unable to reload configuration",e);
    	}
    }
    
    private void killTorProcess ()
    {
		
    	if (conn != null)
		{
			try {
				Log.i(TAG,"sending SHUTDOWN signal");
				conn.signal("SHUTDOWN");
			} catch (Exception e) {
				Log.i(TAG,"error shutting down Tor via connection",e);
			}
			conn = null;
		}
    	
    	
		int procId = TorServiceUtils.findProcessId(TorServiceConstants.TOR_BINARY_INSTALL_PATH);

		while (procId != -1)
		{
			
			Log.i(TAG,"Found Tor PID=" + procId + " - killing now...");
			
			TorServiceUtils.doCommand(SHELL_CMD_KILL, procId + "");

			procId = TorServiceUtils.findProcessId(TorServiceConstants.TOR_BINARY_INSTALL_PATH);
		}
		
		procId = TorServiceUtils.findProcessId(TorServiceConstants.PRIVOXY_INSTALL_PATH);

		while (procId != -1)
		{
			
			Log.i(TAG,"Found Privoxy PID=" + procId + " - killing now...");
			
			TorServiceUtils.doCommand(SHELL_CMD_KILL, procId + "");

			procId = TorServiceUtils.findProcessId(TorServiceConstants.PRIVOXY_INSTALL_PATH);
		}
		
    }
   
    private void logNotice (String msg)
    {

    	Log.i(TAG, msg);
		
    }
    
    private boolean checkTorBinaries ()
    {

		boolean torBinaryExists = new File(TOR_BINARY_INSTALL_PATH).exists();
		boolean privoxyBinaryExists = new File(PRIVOXY_INSTALL_PATH).exists();

		if (!(torBinaryExists && privoxyBinaryExists))
		{
			killTorProcess ();
			
			TorBinaryInstaller installer = new TorBinaryInstaller(); 
			installer.start(true);
		
			torBinaryExists = new File(TOR_BINARY_INSTALL_PATH).exists();
			privoxyBinaryExists = new File(PRIVOXY_INSTALL_PATH).exists();
			
    		if (torBinaryExists && privoxyBinaryExists)
    		{
    			logNotice("Tor, Privoxy, IPtables binaries installed!");
    	
    			this.showToolbarNotification("Orbot Installed!", "The Tor binary was successfully extracted and installed", R.drawable.tornotification);
    		
    		}
    		else
    		{
    			logNotice("Binary install FAILED!");
    			
    			this.showToolbarNotification("Orbot FAIL!", "The binaries were unable to be installed", R.drawable.tornotification);

    		
    			return false;
    		}
    		
		}
		

		Log.i(TAG,"Setting permission on Tor binary");
		TorServiceUtils.doCommand(SHELL_CMD_CHMOD, CHMOD_EXE_VALUE + ' ' + TOR_BINARY_INSTALL_PATH);
		
		Log.i(TAG,"Setting permission on Privoxy binary");
		TorServiceUtils.doCommand(SHELL_CMD_CHMOD, CHMOD_EXE_VALUE + ' ' + PRIVOXY_INSTALL_PATH);
				
		return true;
    }
    
    public void initTor () throws Exception
    {
    		
    		currentStatus = STATUS_CONNECTING;

    		logNotice("Tor is starting up...");
    		this.sendCallbackMessage("starting...");
    		
    		killTorProcess ();
    		
    		checkTorBinaries ();
    		
    		
    		Log.i(TAG,"Starting tor process");
    		
    		TorServiceUtils.doCommand(TOR_BINARY_INSTALL_PATH, TOR_COMMAND_LINE_ARGS);
		
    		int procId = TorServiceUtils.findProcessId(TorServiceConstants.TOR_BINARY_INSTALL_PATH);

    		while (procId == -1)
    		{
    			TorServiceUtils.doCommand(TOR_BINARY_INSTALL_PATH, TOR_COMMAND_LINE_ARGS);
    			procId = TorServiceUtils.findProcessId(TorServiceConstants.TOR_BINARY_INSTALL_PATH);
    			
    			if (procId == -1)
    			{
    				this.sendCallbackMessage("Couldn't start Tor process... retrying...");
    				Thread.sleep(3000);
    			}
    		}
    		
    		Log.i(TAG,"Tor process id=" + procId);
    		
    		showToolbarNotification("Orbot starting...", "Orbot is starting up", R.drawable.tornotification);
			
			initControlConnection ();
			
			int privoxyProcId = TorServiceUtils.findProcessId(TorServiceConstants.PRIVOXY_INSTALL_PATH);

    		while (privoxyProcId == -1)
    		{
    			TorServiceUtils.doCommand(PRIVOXY_INSTALL_PATH, PRIVOXY_COMMAND_LINE_ARGS);
    			privoxyProcId = TorServiceUtils.findProcessId(TorServiceConstants.PRIVOXY_INSTALL_PATH);
    			
    			if (privoxyProcId == -1)
    			{
    				this.sendCallbackMessage("Couldn't start Privoxy process... retrying...");
    				Thread.sleep(3000);
    			}
    		}
    		
    		Log.i(TAG,"Privoxy process id=" + privoxyProcId);
			
    		
    		
    }
    
    
	
	
	public String generateHashPassword ()
	{
		/*
		PasswordDigest d = PasswordDigest.generateDigest();
	      byte[] s = d.getSecret(); // pass this to authenticate
	      String h = d.getHashedPassword(); // pass this to the Tor on startup.
*/
		return null;
	}
	
	public void initControlConnection () throws Exception, RuntimeException
	{
			while (true)
			{
				try
				{
					Log.i(TAG,"Connecting to control port: " + TOR_CONTROL_PORT);
					
					String baseMessage = getString(R.string.tor_process_connecting);
					sendCallbackMessage(baseMessage);
					
					Socket s = new Socket(IP_LOCALHOST, TOR_CONTROL_PORT);
			        conn = TorControlConnection.getConnection(s);
			      //  conn.authenticate(new byte[0]); // See section 3.2
			        
					sendCallbackMessage(baseMessage + ' ' + getString(R.string.tor_process_connecting_step2));

			        Log.i(TAG,"SUCCESS connected to control port");
			        
			        File fileCookie = new File(TOR_CONTROL_AUTH_COOKIE);
			        byte[] cookie = new byte[(int)fileCookie.length()];
			        new FileInputStream(new File(TOR_CONTROL_AUTH_COOKIE)).read(cookie);
			        conn.authenticate(cookie);
			        
			        Log.i(TAG,"SUCCESS authenticated to control port");
			        
					sendCallbackMessage(baseMessage + ' ' + getString(R.string.tor_process_connecting_step3));

			        addEventHandler();
			        
			        break; //don't need to retry
				}
				catch (Exception ce)
				{
					conn = null;
					Log.i(TAG,"Attempt: Error connecting to control port: " + ce.getLocalizedMessage(),ce);
					
					sendCallbackMessage(getString(R.string.tor_process_connecting_step4));

					Thread.sleep(1000);
					
					
				}	
			}
		
		

	}
	
	public void modifyConf () throws IOException
	{
	       // Get one configuration variable.
	       List<ConfigEntry> options = conn.getConf("contact"); 
	       options.size();
	       // Get a set of configuration variables.
	      // List options = conn.getConf(Arrays.asList(new String[]{
	           //   "contact", "orport", "socksport"}));
	       // Change a single configuration variable
	       conn.setConf("BandwidthRate", "1 MB");
	       // Change several configuration variables
	       conn.setConf(Arrays.asList(new String[]{
	              "HiddenServiceDir /home/tor/service1",
	              "HiddenServicePort 80",
	       }));
	       // Reset some variables to their defaults
	       conn.resetConf(Arrays.asList(new String[]{
	              "contact", "socksport"
	       }));
	       // Flush the configuration to disk.
	       conn.saveConf();

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
				       Log.i(TAG, "bootstrap-phase: " + bsPhase);
				       
				       
			       }
			       else
			       {
			    	 //  String status = conn.getInfo("status/circuit-established");
			    	 //  Log.i(TAG, "status/circuit-established=" + status);
			       }
			}
		}
		catch (Exception e)
		{
			Log.i(TAG, "Unable to get Tor status from control port");
			currentStatus = STATUS_UNAVAILABLE;
		}
		
	}*/
	
	
	public void addEventHandler () throws IOException
	{
	       // We extend NullEventHandler so that we don't need to provide empty
	       // implementations for all the events we don't care about.
	       // ...
        Log.i(TAG,"adding control port event handler");

		conn.setEventHandler(this);
	    
		conn.setEvents(Arrays.asList(new String[]{
	          "ORCONN", "CIRC", "NOTICE", "ERR"}));
	      // conn.setEvents(Arrays.asList(new String[]{
	        //  "DEBUG", "INFO", "NOTICE", "WARN", "ERR"}));

	    Log.i(TAG,"SUCCESS added control port event handler");
	    
	    

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
			
			if (profile == PROFILE_ON)
			{
 				currentStatus = STATUS_CONNECTING;
	            sendCallbackMessage ("starting...");

	            new Thread(_torInstance).start();

			}
			else
			{
				currentStatus = STATUS_OFF;
	            sendCallbackMessage ("shutting down...");
	            
				_torInstance.stopTor();

			}
		}



	@Override
	public void message(String severity, String msg) {
		
              Log.i(TAG, "[Tor Control Port] " + severity + ": " + msg);
              
              if (msg.indexOf(TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE)!=-1)
              {
            	  currentStatus = STATUS_ON;
            	  showToolbarNotification ("Orbot","Anonymous browsing is enabled",R.drawable.tornotification);
      			  
              }
              else
              {
            	  showToolbarNotification("Orbot", msg, R.drawable.tornotification);

              }
             
              sendCallbackMessage (msg);
             
	}


	@Override
	public void newDescriptors(List<String> orList) {
		
	}


	@Override
	public void orConnStatus(String status, String orName) {
		
		Log.i(TAG,"OrConnStatus=" + status + ": " + orName);
	

	}


	@Override
	public void streamStatus(String status, String streamID, String target) {
		Log.i(TAG,"StreamStatus=" + status + ": " + streamID);
		
	}


	@Override
	public void unrecognized(String type, String msg) {
		Log.i(TAG,"unrecognized log=" + type + ": " + msg);
		
	}

	@Override
	public void bandwidthUsed(long read, long written) {
		
	}

	@Override
	public void circuitStatus(String status, String circID, String path) {
	
	}

	 @Override
    public IBinder onBind(Intent intent) {
        // Select the interface to return.  If your service only implements
        // a single interface, you can just return it here without checking
        // the Intent.
        if (ITorService.class.getName().equals(intent.getAction())) {
            return mBinder;
        }
      
        return null;
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
        	sendCallbackMessage("");
        	
        }
        
    };
    
    private void sendCallbackMessage (String status)
    {
    	 
        // Broadcast to all clients the new value.
        final int N = mCallbacks.beginBroadcast();
        for (int i=0; i<N; i++) {
            try {
                mCallbacks.getBroadcastItem(i).statusChanged(status);
            } catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }
}
