/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.content.Context;
import android.content.SharedPreferences;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.TorServiceConstants;

public class TorServiceUtils implements TorServiceConstants {

	
	
	public static int findProcessId(String command) throws IOException 
	{
		int procId = findProcessIdWithPS(command);
		return procId;
	}
	
	//use 'pidof' command
	/**
	public static int findProcessIdWithPidOf(String command) throws Exception
	{
		
		int procId = -1;
		
		Runtime r = Runtime.getRuntime();
		    	
		Process procPs = null;
		
		String baseName = new File(command).getName();
		//fix contributed my mikos on 2010.12.10
		procPs = r.exec(new String[] {SHELL_CMD_PIDOF, baseName});
        //procPs = r.exec(SHELL_CMD_PIDOF);
            
        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line = null;

        while ((line = reader.readLine())!=null)
        {
        
        	try
        	{
        		//this line should just be the process id
        		procId = Integer.parseInt(line.trim());
        		break;
        	}
        	catch (NumberFormatException e)
        	{
        		Log.e("TorServiceUtils","unable to parse process pid: " + line,e);
        	}
        }
            
       
        return procId;

	}
	 * @throws IOException */
	
	//use 'ps' command
	public static int findProcessIdWithPS(String command) throws IOException 
	{
		
		int procId = -1;
		
		Runtime r = Runtime.getRuntime();
		    	
		Process procPs = null;
		
        procPs = r.exec(SHELL_CMD_PS); // this is the android ps <name> command
            
        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line = null;
        
        while ((line = reader.readLine())!=null)
        {
        	if (line.contains("PID"))
        		continue;
        		
        	if (line.contains(command))
        	{
        		
        		String[] lineParts = line.split("\\s+");
        		
        	    try { 
        	        
        	        procId = Integer.parseInt(lineParts[1]); //for most devices it is the second number
        	    } catch(NumberFormatException e) {
        	    	procId = Integer.parseInt(lineParts[0]); //but for samsungs it is the first
        	        
        	    }
        		
        		
        		break;
        	}
        }
        
        try { procPs.destroy(); } catch (Exception e) {} // try to destroy just to make sure we clean it up
       
        return procId;

	}
	
	public static SharedPreferences getSharedPrefs (Context context) {
		return context.getSharedPreferences(OrbotConstants.PREF_TOR_SHARED_PREFS,0 | Context.MODE_MULTI_PROCESS);
	}
	
	public static void killProcess(File fileProcBin) throws Exception {
        killProcess(fileProcBin, "-9"); // this is -KILL
    }

    public static void killProcess(File fileProcBin, String signal) throws Exception {
        int procId = -1;
        int killAttempts = 0;

        while ((procId = TorServiceUtils.findProcessId(fileProcBin.getCanonicalPath())) != -1) {
            killAttempts++;
            //logNotice("Found " + fileProcBin.getName() + " PID=" + procId + " - killing now...");
            String pidString = String.valueOf(procId);
            /*
             * first try as the normal app user to be safe, then if that fails,
             * try root since the process might be left over from
             * uninstall/reinstall with different UID.
             */

			/**
            if (Prefs.useRoot() && killAttempts > 2) {
                shell = Shell.startRootShell();
                Log.i(OrbotApp.TAG, "using a root shell");
            } else {
                shell = Shell.startShell();
            }*/

            try { Runtime.getRuntime().exec("busybox killall " + signal + " " + fileProcBin.getName());}catch(IOException ioe){}
			try { Runtime.getRuntime().exec("toolbox kill " + signal + " " + pidString);}catch(IOException ioe){}
			try { Runtime.getRuntime().exec("busybox kill " + signal + " " + pidString);}catch(IOException ioe){}
			try { Runtime.getRuntime().exec("kill " + signal + " " + pidString);}catch(IOException ioe){}

			try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignored
            }

            if (killAttempts > 4)
                throw new Exception("Cannot kill: " + fileProcBin.getAbsolutePath());
        }
    }

    public static boolean isPortOpen(final String ip, final int port, final int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } 

        catch(ConnectException ce){
            //ce.printStackTrace();
            return false;
        }

        catch (Exception ex) {
            //ex.printStackTrace();
            return false;
        }
    }
}
