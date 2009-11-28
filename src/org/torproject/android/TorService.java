/* Copyright (c) 2009, Nathan Freitas, The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.freehaven.tor.control.EventHandler;
import net.freehaven.tor.control.NullEventHandler;
import net.freehaven.tor.control.TorControlConnection;
import net.sourceforge.jsocks.socks.Proxy;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class TorService extends Service implements TorConstants
{
	
	private static TorControlPanel ACTIVITY = null;
	
	private final static String TAG = "Tor";
	
	private static HttpProxy webProxy = null;
	
	private static Process procTor = null;
	
	private static int currentStatus = STATUS_OFF;
	
	private static TorControlConnection conn = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
    	super.onCreate();
       
    }
    

    public static int getStatus ()
    {
    	try {
			getTorStatus();
		} catch (IOException e) {
			Log.i(TAG,"Unable to get tor status",e);
		}
    	
    	return currentStatus;
    	
    }
   
    
    /* (non-Javadoc)
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(Intent intent) {
		// TODO Auto-generated method stub
		super.onRebind(intent);
		
		  Log.i(TAG,"on rebind");
	}


	/* (non-Javadoc)
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		
		  Log.i(TAG,"on start");
		  
		  startService();
	}


	private void startService ()
    {
	   Thread thread = new Thread ()
	   {
		   public void run ()
		   {
	   
		   Log.i(TAG,"Tor thread started");
      
		   initTor();
		   }
	   };
	   
	   thread.start();
     
       
    }
    
  
    
    public void onDestroy ()
    {
    	
    }
    
    public static void stopTor ()
    {
    	currentStatus = STATUS_SHUTTING_DOWN;
    	
    	Thread thread = new Thread ()
    	{
    		public void run ()
    		{
		    	killTorProcess ();
				
				setupWebProxy(false);  
				
				currentStatus = STATUS_OFF;
    		}
    	};
    	
    	thread.start();
    }
    
 
    public static void setActivity(TorControlPanel activity) {
    	ACTIVITY = activity;
    }
   
    private static void setupWebProxy (boolean enabled)
    {
    	if (enabled)
    	{
    		
    		if (webProxy == null)
    		{
		    	Log.i(TAG,"Setting up Web Proxy on port 8888");
		    	//httpd s
		    	webProxy = new HttpProxy(PORT_HTTP);
		    	webProxy.setDoSocks(true);
		    	webProxy.start();
		    	
		    	//socks
		    	try
		    	{
		    		Proxy.setDefaultProxy(IP_LOCALHOST,PORT_SOCKS);
		    		
		    	}
		    	catch (Exception e)
		    	{
		    		Log.w(TAG,e.getMessage());
		    	}
	    	
		    	//Settings.System.putString(getContentResolver(), Settings.System.HTTP_PROXY, proxySetting);//enable proxy
		    	//	Settings.Secure.putString(getContentResolver(), Settings.Secure.HTTP_PROXY, proxySetting);//enable proxy
    		}
    		else
    		{
    			
    			webProxy.setDoSocks(true);
    			Log.i(TAG,"Web Proxy already running...");
    		}
    	}
    	else
    	{
	    	Log.i(TAG,"Turning off Socks/Tor routing on Web Proxy");

    		if (webProxy != null)
    		{
    			//logNotice("Tor is disabled - browsing is not anonymous!");
    			//webProxy.setDoSocks(false);
    			
    		}
    	}
    	
    }
    
    public static void reloadConfig ()
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
    
    private void shutdownTor ()
    {
    	try
		{
        	currentStatus = STATUS_SHUTTING_DOWN;

			if (conn == null)
			{
				initControlConnection ();
			}
		
			if (conn != null)
			{
				 conn.signal("SHUTDOWN");
			}
		}
		catch (Exception e)
		{
		}
    }
    private static void killTorProcess ()
    {
		//doCommand(SHELL_CMD_KILLALL, CHMOD_EXE_VALUE, TOR_BINARY_INSTALL_PATH);
    	
    	/*
    	if (procTor != null)
    	{
    		Log.i(TAG,"shutting down Tor process...");
    		procTor.destroy();
    		
    		
    		try {
    			procTor.waitFor();
    		}
    		catch(Exception e2)
    		{
    			e2.printStackTrace();
    		}
    		
    		int exitStatus = procTor.exitValue();
    		Log.i(TAG,"Tor exit: " + exitStatus);
			
    		
    		procTor = null;
    		
    	}*/
    		
		int procId = findProcessId(TorConstants.TOR_BINARY_INSTALL_PATH);

		if (procId != -1)
		{
			
			Log.i(TAG,"Found Tor PID=" + procId + " - killing now...");
			
			doCommand(SHELL_CMD_KILLALL, procId + "");

		}
		
		conn = null;
		
    }
   
    private static void logNotice (String msg)
    {

    	Log.i(TAG, msg);
		
    }
    
    private void checkBinary ()
    {

		boolean binaryExists = new File(TOR_BINARY_INSTALL_PATH).exists();
		
		if (!binaryExists)
		{
			killTorProcess ();
			
			TorBinaryInstaller installer = new TorBinaryInstaller(); 
			installer.start(true);
		
			binaryExists = new File(TOR_BINARY_INSTALL_PATH).exists();
    		if (binaryExists)
    		{
    			logNotice("Tor binary installed!");
    			
    		}
    		else
    		{
    			logNotice("Tor binary install FAILED!");
    			return;
    		}
		}
		
		Log.i(TAG,"Setting permission on Tor binary");
		doCommand(SHELL_CMD_CHMOD, CHMOD_EXE_VALUE + ' ' + TOR_BINARY_INSTALL_PATH);
    }
    
    public void initTor ()
    {
    	try {
    		
    		currentStatus = STATUS_STARTING_UP;

    		killTorProcess ();
    		
    		checkBinary ();
    		
    		doCommand(SHELL_CMD_RM,TOR_LOG_PATH);
    		
    		Log.i(TAG,"Starting tor process");
    		procTor = doCommand(TOR_BINARY_INSTALL_PATH, TOR_COMMAND_LINE_ARGS);
		
    		//Log.i(TAG,"Tor process id=" + procTor.);
    		
    		currentStatus = STATUS_STARTING_UP;
    		logNotice("Tor is starting up...");
			
			Thread.sleep(2000);
			initControlConnection ();
		
    	} catch (Exception e) {
			
			Log.w(TAG,"unable to start Tor Process",e);
		
			e.printStackTrace();
			
		}
    	
    }
    
    private static void logStream (InputStream is)
    {
    	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    	String line = null;
    
    	
    	try {
			while ((line = reader.readLine()) != null)
			{
				Log.i(TAG, line);
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	
    }
    
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static int findProcessId(String command) 
	{
		int procId = -1;
		
		Runtime r = Runtime.getRuntime();
		    	
		Process procPs = null;
		
        try {
            
            procPs = r.exec(SHELL_CMD_PS);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
            String line = null;
            
            while ((line = reader.readLine())!=null)
            {
            	if (line.indexOf(command)!=-1)
            	{
            		
            		StringTokenizer st = new StringTokenizer(line," ");
            		st.nextToken(); //proc owner
            		
            		procId = Integer.parseInt(st.nextToken().trim());
            		
            		break;
            	}
            }
            
        } catch (Exception e) {
            Log.e(TAG, "error: " + e.getMessage(), e);
        }
        
        return procId;

	}
	
	public static Process doCommand(String command, String arg1) 
	{
		
		Runtime r = Runtime.getRuntime();
		    	
		Process child = null;
		
        try {
            if(child != null) {
            	child.destroy();
            	child = null;
            }
            
            child = r.exec(command + ' ' + arg1);
            
            
            
        } catch (Exception e) {
            Log.e(TAG, "error: " + e.getMessage(), e);
        }
        
        return child;

	}
	
	public static String generateHashPassword ()
	{
		/*
		PasswordDigest d = PasswordDigest.generateDigest();
	      byte[] s = d.getSecret(); // pass this to authenticate
	      String h = d.getHashedPassword(); // pass this to the Tor on startup.
*/
		return null;
	}
	
	public static synchronized void initControlConnection () throws Exception, RuntimeException
	{
		if (conn == null)
		{
			Log.i(TAG,"Connecting to control port: " + TOR_CONTROL_PORT);
			Socket s = new Socket(IP_LOCALHOST, TOR_CONTROL_PORT);
	        conn = TorControlConnection.getConnection(s);
	      //  conn.authenticate(new byte[0]); // See section 3.2
	        
	        Log.i(TAG,"SUCCESS connected to control port");
	        
	        //
	        File fileCookie = new File(TOR_CONTROL_AUTH_COOKIE);
	        byte[] cookie = new byte[(int)fileCookie.length()];
	        new FileInputStream(new File(TOR_CONTROL_AUTH_COOKIE)).read(cookie);
	        conn.authenticate(cookie);
	        
	        Log.i(TAG,"SUCCESS authenticated to control port");
	        
	        addEventHandler();
		}
		

	}
	
	public void modifyConf () throws IOException
	{
	       // Get one configuration variable.
	       List options = conn.getConf("contact");
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
	
	private static void getTorStatus () throws IOException
	{
		try
		{
			 
			if (conn == null && (currentStatus == STATUS_STARTING_UP || currentStatus == STATUS_ON))
			{
				
					initControlConnection ();
				
			}
			
		
			if (conn != null)
			{
				 // get a single value.
			      
			       // get several values
			       
			       if (currentStatus == STATUS_STARTING_UP)
			       {
				       //Map vals = conn.getInfo(Arrays.asList(new String[]{
				         // "status/bootstrap-phase", "status","version"}));
			
				       String bsPhase = conn.getInfo("status/bootstrap-phase");
				    //   Log.i(TAG, "bootstrap-phase: " + bsPhase);
				       
				       if (bsPhase.indexOf("PROGRESS=100")!=-1)
				       {
				    	   currentStatus = STATUS_ON;
				       }
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
		}
		
	}
	
	/*
	 * The recognized signal names are:
       "RELOAD" -- Reload configuration information
       "SHUTDOWN" -- Start a clean shutdown of the Tor process
       "DUMP" -- Write current statistics to the logs
       "DEBUG" -- Switch the logs to debugging verbosity
       "HALT" -- Stop the Tor process immediately.

	 */
	public void sendSignal () throws IOException
	{
		
		 conn.signal("RELOAD");

	}
	
	public static void addEventHandler () throws IOException
	{
	       // We extend NullEventHandler so that we don't need to provide empty
	       // implementations for all the events we don't care about.
	       // ...
        Log.i(TAG,"adding control port event handler");

		
	       EventHandler eh = new NullEventHandler() 
	       {
	          public void message(String severity, String msg) {
	            
	        	 // Log.println(priority, tag, msg)("["+severity+"] "+msg);
	              //Toast.makeText(, text, duration)
	        //      Toast.makeText(ACTIVITY, severity + ": " + msg, Toast.LENGTH_SHORT);
	              Log.i(TAG, "[Tor Control Port] " + severity + ": " + msg);
	              
	              if (msg.indexOf(TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE)!=-1)
	              {
	            	  currentStatus = STATUS_ON;
	            	  setupWebProxy(true);

	              }
	              
	          }
	       };
	       
	       conn.setEventHandler(eh);
	       conn.setEvents(Arrays.asList(new String[]{
	          "ORCONN", "CIRC", "INFO", "NOTICE", "ERR"}));
	      // conn.setEvents(Arrays.asList(new String[]{
	        //  "DEBUG", "INFO", "NOTICE", "WARN", "ERR"}));

	        Log.i(TAG,"SUCCESS added control port event handler");

	}
}