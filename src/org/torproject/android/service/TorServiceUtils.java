/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.torproject.android.TorifiedApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class TorServiceUtils implements TorServiceConstants {

	private static TorifiedApp[] apps = null;
	
	private final static String PREFS_KEY = "OrbotPrefs";
	private final static String PREFS_KEY_TORIFIED = "PrefTord";
	
	public static void saveAppSettings (Context context)
	{
		if (apps == null)
			return;
		
		final SharedPreferences prefs = context.getSharedPreferences(PREFS_KEY, 0);

		StringBuilder tordApps = new StringBuilder();
		
		for (int i = 0; i < apps.length; i++)
		{
			if (apps[i].isTorified())
			{
				tordApps.append(apps[i].getUsername());
				tordApps.append("|");
			}
		}
		
		Editor edit = prefs.edit();
		edit.putString(PREFS_KEY_TORIFIED, tordApps.toString());
		edit.commit();
		
	}
	
	public static TorifiedApp[] getApps (Context context)
	{
		if (apps != null)
			return apps;
	
		final SharedPreferences prefs = context.getSharedPreferences(PREFS_KEY, 0);

		String tordAppString = prefs.getString(PREFS_KEY_TORIFIED, "");
		String[] tordApps;
		
		StringTokenizer st = new StringTokenizer(tordAppString,"|");
		tordApps = new String[st.countTokens()];
		int tordIdx = 0;
		while (st.hasMoreTokens())
		{
			tordApps[tordIdx++] = st.nextToken();
		}
		
		Arrays.sort(tordApps);
		
		//else load the apps up
		PackageManager pMgr = context.getPackageManager();
		
		List<ApplicationInfo> lAppInfo = pMgr.getInstalledApplications(0);
		
		Iterator<ApplicationInfo> itAppInfo = lAppInfo.iterator();
		
		apps = new TorifiedApp[lAppInfo.size()];
		
		ApplicationInfo aInfo = null;
		
		int appIdx = 0;
		
		while (itAppInfo.hasNext())
		{
			aInfo = itAppInfo.next();
			
			apps[appIdx] = new TorifiedApp();
			
			apps[appIdx].setEnabled(aInfo.enabled);
			apps[appIdx].setUid(aInfo.uid);
			apps[appIdx].setUsername(pMgr.getNameForUid(apps[appIdx].getUid()));
			apps[appIdx].setProcname(aInfo.processName);
			apps[appIdx].setName(pMgr.getApplicationLabel(aInfo).toString());
			
			// check if this application is allowed
			if (Arrays.binarySearch(tordApps, apps[appIdx].getUsername()) >= 0) {
				apps[appIdx].setTorified(true);
			}
			else
			{
				apps[appIdx].setTorified(false);
			}
			
			appIdx++;
		}
		
		return apps;
	}
	
	public static int findProcessId(String command) 
	{
		Log.i(TAG, "finding process id for: " + command);
		
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
	
	
	public static boolean hasRoot ()
	{
		String[] cmds = {"exit 0"};
		
		int code = doShellCommand(cmds,null,true, true);
		
		return (code == 0);
	}
	
	public static int doShellCommand(String[] cmds, StringBuilder log, boolean isRoot, boolean waitFor) 
	{
		Log.i(TAG,"executing shell cmds: " + cmds[0] + "; isRoot=" + isRoot);
		
		 	
		Process proc = null;
		int exitCode = -1;
		
        try {
            
        	if (isRoot)
        		proc = Runtime.getRuntime().exec("su");
        	else
        		proc = Runtime.getRuntime().exec("sh");
        	
        	OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());
            
            for (int i = 0; i < cmds.length; i++)
            {
            	out.write(cmds[i]);
            	out.write("\n");
            }
            
            out.flush();
			out.write("exit\n");
			out.flush();
		
			if (waitFor)
			{
				
				exitCode = proc.waitFor();
				
				
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
				Log.i(TAG,"command process exit value: " + exitCode);
			}
            
        } catch (Exception e) {
            Log.w(TAG, "Error executing shell cmd: " + e.getMessage());
        }
        
        return exitCode;

	}
}
