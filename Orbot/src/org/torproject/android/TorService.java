/* Copyright (c) 2009-2011, Nathan Freitas, The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
    	super.onCreate();
       
    
    }
    
    public static boolean isRunning ()
    {
    	int procId = findProcessId(TorConstants.TOR_BINARY_INSTALL_PATH);

		return (procId != -1);
		
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
       Log.i(TAG,"Tor thread started");
            
        	
       initTor();
        
       
       setupWebProxy(true);
     
       
    }
    
  
    
    public void onDestroy ()
    {
    	
    	killTorProcess ();
		setupWebProxy(false);  
		
    }
    
 
    public static void setActivity(TorControlPanel activity) {
    	ACTIVITY = activity;
    }
   
    private void setupWebProxy (boolean enabled)
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
    			showToast("Tor is disabled - browsing is not anonymous!");
    			webProxy.setDoSocks(false);
    			
    		}
    	}
    	
    }
    
    private void killTorProcess ()
    {
		//doCommand(SHELL_CMD_KILLALL, CHMOD_EXE_VALUE, TOR_BINARY_INSTALL_PATH);
		
    	if (procTor != null)
    	{
    		Log.i(TAG,"shutting down Tor process...");
    		
    		procTor.destroy();
    		
    		try {
    			procTor.waitFor();
    		}
    		catch(Exception e)
    		{
    			e.printStackTrace();
    		}
    		
    		int exitStatus = procTor.exitValue();
    		Log.i(TAG,"Tor exit: " + exitStatus);

    		procTor = null;
    		
    	}
    		
		int procId = findProcessId(TorConstants.TOR_BINARY_INSTALL_PATH);

		if (procId != -1)
		{
			Log.i(TAG,"Found Tor PID=" + procId + " - killing now...");
			
			doCommand(SHELL_CMD_KILLALL, procId + "");
		}
		
		if (ACTIVITY != null)
			((TorControlPanel)ACTIVITY).setUIState();

    	
    }
   
    private void showToast (String msg)
    {

    	Toast toast = Toast.makeText(ACTIVITY, msg, Toast.LENGTH_LONG);
		toast.show();
		
    }
    
    public void initTor ()
    {
    	try {
    		
    		
    		boolean binaryExists = new File(TOR_BINARY_INSTALL_PATH).exists();
    		
    		if (!binaryExists)
    		{
    			TorBinaryInstaller installer = new TorBinaryInstaller(); 
    			installer.start(false);
    		
    			binaryExists = new File(TOR_BINARY_INSTALL_PATH).exists();
	    		if (binaryExists)
	    		{
	    			showToast("Tor binary installed!");
	    		}
	    		else
	    		{
	    			showToast("Tor binary install FAILED!");
	    			return;
	    		}
    		}
    		
    		Log.i(TAG,"Setting permission on Tor binary");
    		doCommand(SHELL_CMD_CHMOD, CHMOD_EXE_VALUE + ' ' + TOR_BINARY_INSTALL_PATH);
			
    		killTorProcess ();
    		
    		doCommand(SHELL_CMD_RM,TOR_LOG_PATH);
    		
    		Log.i(TAG,"Starting tor process");
    		procTor = doCommand(TOR_BINARY_INSTALL_PATH, TOR_COMMAND_LINE_ARGS);
		
    		//Log.i(TAG,"Tor process id=" + procTor.);
    		
			showToast("Tor is starting up...");
			
			((TorControlPanel)ACTIVITY).setUIState();
			
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
	
	public Process doCommand(String command, String arg1) 
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
	/*
	public static String doCommand(String command, String arg0, String arg1, boolean logOutput) {
		try {
			// android.os.Exec is not included in android.jar so we need to use reflection.
			Class execClass = Class.forName("android.os.Exec");
			Method createSubprocess = execClass.getMethod("createSubprocess",
			String.class, String.class, String.class, int[].class);
			Method waitFor = execClass.getMethod("waitFor", int.class);
			
			// Executes the command.
			// NOTE: createSubprocess() is asynchronous.
			int[] pid = new int[1];
			FileDescriptor fd = (FileDescriptor)createSubprocess.invoke(
			null, command, arg0, arg1, pid);
			
			StringBuffer output = new StringBuffer();

			if (logOutput)
			{
				// Reads stdout.
				// NOTE: You can write to stdin of the command using new FileOutputStream(fd).
				FileInputStream in = new FileInputStream(fd);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				try {
					String line;
					while ((line = reader.readLine()) != null) {
						output.append(line);
						output.append('\n');
					}
				} catch (IOException e) {
					// It seems IOException is thrown when it reaches EOF.
					Log.e(TAG, "error reading output file", e);
	
				}
				
				// Waits for the command to finish.
				waitFor.invoke(null, pid[0]);
			}
			
			
			
			// send output to the textbox
			return output.toString();
			
		} 
		catch (Exception e)
		{
			Log.i(TAG, "unable to execute command",e);
			e.printStackTrace();
			return null;
		}
	}*/
	
}