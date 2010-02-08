/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import android.util.Log;

public class TorServiceUtils implements TorServiceConstants {

	
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
		Log.i(TAG,"executing command: " + command + ' ' + arg1);
		
		Runtime r = Runtime.getRuntime();
		    	
		Process proc = null;
		
		StringBuilder log = new StringBuilder();
		
        try {
            
        	proc = r.exec(command + ' ' + arg1);
            
/*
			final char buf[] = new char[1024];
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
			}*/
            
            Log.i(TAG,"command process exit value: " + proc.exitValue());
            Log.i(TAG, "shell cmd output: " + log.toString());

        } catch (Exception e) {
            Log.e(TAG, "error: " + e.getMessage());
            Log.e(TAG, "shell cmd output: " + log.toString());
        }
        
        
        return proc;

	}
	
	public static boolean hasRoot ()
	{
		String[] cmds = {"exit 0"};
		
		int code = doRootCommand(cmds,null);
		
		return (code == 0);
	}
	
	public static int doRootCommand(String[] cmds, StringBuilder log) 
	{
		Log.i(TAG,"executing commands: " + cmds.length);
		
		Runtime runtime = Runtime.getRuntime();
		    	
		Process proc = null;
		int exitCode = -1;
		
        try {
            
        	proc = runtime.exec(cmds[0]);

            OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());
            
            for (int i = 1; i < cmds.length; i++)
            {
            	out.write(cmds[i]);
            	out.write("\n");
            }
            
            out.flush();
			out.write("exit\n");
			
			final char buf[] = new char[1024];
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
			
            Log.i(TAG,"command process exit value: " + exitCode);

            
        } catch (Exception e) {
            Log.e(TAG, "Error executing shell cmd: " + e.getMessage(),e);
        }
        
        return exitCode;

	}
}
