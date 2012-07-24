/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import org.torproject.android.TorConstants;

import android.util.Log;

public class TorServiceUtils implements TorServiceConstants {

	
	public static boolean isRootPossible()
	{
		
		StringBuilder log = new StringBuilder();
		
		try {
			
			// Check if Superuser.apk exists
			File fileSU = new File("/system/app/Superuser.apk");
			if (fileSU.exists())
				return true;
			
			fileSU = new File("/system/app/superuser.apk");
			if (fileSU.exists())
				return true;
			
			fileSU = new File("/system/bin/su");
			if (fileSU.exists())
			{
				String[] cmd = {"su"};
				int exitCode = TorServiceUtils.doShellCommand(cmd, log, false, true);
				if (exitCode != 0)
					return false;
				else
					return true;
			}
			
			//Check for 'su' binary 
			String[] cmd = {"which su"};
			int exitCode = TorServiceUtils.doShellCommand(cmd, log, false, true);
			
			if (exitCode == 0) {
				TorService.logMessage("root exists, but not sure about permissions");
		    	 return true;
		     
		    }
		      
		} catch (IOException e) {
			//this means that there is no root to be had (normally) so we won't log anything
			TorService.logException("Error checking for root access",e);
			
		}
		catch (Exception e) {
			TorService.logException("Error checking for root access",e);
			//this means that there is no root to be had (normally)
		}
		
		TorService.logMessage("Could not acquire root permissions");
		
		
		return false;
	}
	
	
	public static int findProcessId(String command) 
	{
		int procId = -1;
		
		try
		{
			procId = findProcessIdWithPidOf(command);
			
			if (procId == -1)
				procId = findProcessIdWithPS(command);
		}
		catch (Exception e)
		{
			try
			{
				procId = findProcessIdWithPS(command);
			}
			catch (Exception e2)
			{
				Log.w(TorConstants.TAG,"Unable to get proc id for: " + command,e2);
			}
		}
		
		return procId;
	}
	
	//use 'pidof' command
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
        		TorService.logException("unable to parse process pid: " + line,e);
        	}
        }
            
       
        return procId;

	}
	
	//use 'ps' command
	public static int findProcessIdWithPS(String command) throws Exception
	{
		
		int procId = -1;
		
		Runtime r = Runtime.getRuntime();
		    	
		Process procPs = null;
		
        procPs = r.exec(SHELL_CMD_PS);
            
        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line = null;
        
        while ((line = reader.readLine())!=null)
        {
        	if (line.indexOf(' ' + command)!=-1)
        	{
        		
        		StringTokenizer st = new StringTokenizer(line," ");
        		st.nextToken(); //proc owner
        		
        		procId = Integer.parseInt(st.nextToken().trim());
        		
        		break;
        	}
        }
        
       
        
        return procId;

	}
	
	
	public static int doShellCommand(String[] cmds, StringBuilder log, boolean runAsRoot, boolean waitFor) throws Exception
	{
		
		Process proc = null;
		int exitCode = -1;
		
    	if (runAsRoot)
    		proc = Runtime.getRuntime().exec("su");
    	else
    		proc = Runtime.getRuntime().exec("sh");
    
    	OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());
        
        for (int i = 0; i < cmds.length; i++)
        {
        	TorService.logMessage("executing shell cmd: " + cmds[i] + "; runAsRoot=" + runAsRoot + ";waitFor=" + waitFor);
    		
        	out.write(cmds[i]);
        	out.write("\n");
        }
        
        out.flush();
		out.write("exit\n");
		out.flush();
	
		if (waitFor)
		{
			
			final char buf[] = new char[10];
			
			// Consume the "stdout"
			InputStreamReader reader = new InputStreamReader(proc.getInputStream());
			int read=0;
			while ((read=reader.read(buf)) != -1) {
				if (log != null) log.append(buf, 0, read);
			}
			
			// Consume the "stderr"
			reader = new InputStreamReader(proc.getErrorStream());
			read=0;
			while ((read=reader.read(buf)) != -1) {
				if (log != null) log.append(buf, 0, read);
			}
			
			exitCode = proc.waitFor();
			
		}
        
        
        return exitCode;

	}
}
