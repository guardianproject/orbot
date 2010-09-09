package org.torproject.android.service;

import org.torproject.android.TorifiedApp;

import android.content.Context;
import android.util.Log;

public class TorTransProxy implements TorServiceConstants {
	
	private final static String TAG = TorServiceConstants.TAG;
		
	//private static String BASE_DIR = "/data/data/" + TorServiceConstants.TOR_APP_USERNAME + "/";

	private static void logNotice (String msg)
	{
		if (LOG_OUTPUT_TO_DEBUG)
			Log.d(TAG, msg);
	}
	/**
	 * Check if we have root access
	 * @return boolean true if we have root
	 */
	public static boolean hasRootAccess() {
	

		StringBuilder log = new StringBuilder();
		
		try {
			
			// Run an empty script just to check root access
			String[] cmd = {"exit 0"};
			int exitCode = TorServiceUtils.doShellCommand(cmd, log, true, true);
			if (exitCode == 0) {
				
				return true;
			}
			
		} catch (Exception e) {
			Log.w(TAG,"Error checking for root access: " + e.getMessage() ,e);
		}
		
		logNotice("Could not acquire root access: " + log.toString());
		return false;
	}
	
	/**
	 * Check if we have root access
	 * @return boolean true if we have root
	 */
	public static String getIPTablesVersion() {
	

		StringBuilder log = new StringBuilder();
		
		try {
			
			// Run an empty script just to check root access
			String[] cmd = {"iptables -v"};
			int code = TorServiceUtils.doShellCommand(cmd, log, true, true);
			String msg = log.toString();
			logNotice(cmd[0] + ";errCode=" + code + ";resp=" + msg);
			
			
			String out = log.toString();
			if (out.indexOf(" v")!=-1)
			{
			
				out = out.substring(out.indexOf(" v")+2);
				out = out.substring(0,out.indexOf(":"));
				
				return out;
			}
			
			
		} catch (Exception e) {
			Log.w(TAG,"Error checking iptables version: " + e.getMessage() ,e);
		}
		
		logNotice("Could not acquire check iptables: " + log.toString());
		return null;
	}
	
	
	private static String findBaseDir ()
	{
	
		return ""; //just blank for now
		/*
		String[] cmds = {"/system/bin/iptables -t nat --list"};
    	StringBuilder res = new StringBuilder();

		int code;
		try {
			code = TorServiceUtils.doShellCommand(cmds, res, true, true);
	
		
		if (code != 0) {
			return BASE_DIR;
		}
		else
			return "/system/bin/";
		
		} catch (Exception e) {
			return BASE_DIR;
		}
		
		return "";
		
			*/
	}
	

	public static int purgeIptables(Context context, TorifiedApp[] apps) throws Exception {

		String baseDir = findBaseDir();
		
    	final StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
			for (int i = 0; i < apps.length; i++)
			{

				//flush nat for every app
				script.append(baseDir);
				script.append("iptables -t nat -m owner --uid-owner ");
				script.append(apps[i].getUid());
				script.append(" -F || exit\n");
				script.append("iptables -t filter -m owner --uid-owner ");
				script.append(apps[i].getUid());
				script.append(" -F || exit\n");
					
			}
			
	    	
	    	String[] cmd = {script.toString()};	    	
			code = TorServiceUtils.doShellCommand(cmd, res, true, true);		
			String msg = res.toString();
			logNotice(cmd[0] + ";errCode=" + code + ";resp=" + msg);
			
		
		return code;
		
	}
	

	
	public static int setTransparentProxyingByApp(Context context, TorifiedApp[] apps, boolean forceAll) throws Exception
	{

		//android.os.Debug.waitForDebugger();
		
		String baseDir = findBaseDir();

		String iptablesVersion = getIPTablesVersion();
		logNotice( "iptables version: " + iptablesVersion);
		
		boolean ipTablesOld = false;
		if (iptablesVersion != null && iptablesVersion.startsWith("1.3")){
			ipTablesOld = true;
		}
		
    	StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
		for (int i = 0; i < apps.length; i++)
		{

			//flush nat for every app
			script.append(baseDir);
			script.append("iptables -t nat -m owner --uid-owner ");
			script.append(apps[i].getUid());
			script.append(" -F || exit\n");
			script.append("iptables -t filter -m owner --uid-owner ");
			script.append(apps[i].getUid());
			script.append(" -F || exit\n");
			
		}
		
    	String[] cmdFlush = {script.toString()};
		code = TorServiceUtils.doShellCommand(cmdFlush, res, true, true);
		//String msg = res.toString(); //get stdout from command
		
		script = new StringBuilder();
		
		//build up array of shell cmds to execute under one root context
		for (int i = 0; i < apps.length; i++)
		{

			if (forceAll || apps[i].isTorified()) //if "Tor Everything" on or app is set to true
			{
				
				if (apps[i].getUsername().equals(TorServiceConstants.TOR_APP_USERNAME))
				{
					//should never trans proxy the Orbot app (and Tor or Privoxy) itself
					continue;
				}
				
				logNotice("enabling transproxy for app: " + apps[i].getUsername() + "(" + apps[i].getUid() + ")");
			 
				//TCP
				script.append(baseDir);
				script.append("iptables -t nat");
				script.append(" -A OUTPUT -p tcp");
				script.append(" -m owner --uid-owner ");
				script.append(apps[i].getUid());
				script.append(" -m tcp --syn");
				
				if (ipTablesOld)
					script.append(" -j DNAT --to 127.0.0.1:9040");
				else
					script.append(" -j REDIRECT --to-ports 9040");
				
				script.append(" || exit\n");
				
				//DNS
				script.append(baseDir);
				script.append("iptables -t nat");
				script.append(" -A OUTPUT -p udp -m owner --uid-owner ");
				script.append(apps[i].getUid());
				script.append(" --dport 53"); //drop all UDP packets as Tor won't handle them
				
				if (ipTablesOld)
					script.append(" -j DNAT --to 127.0.0.1:5400");
				else
					script.append(" -j REDIRECT --to-ports 5400");
				
				script.append(" || exit\n");
				
				
				//EVERYTHING ELSE UDP - DROP!
				if (ipTablesOld) //for some reason this doesn't work on iptables 1.3.7
				{
					
					script.append(baseDir);
					script.append("iptables");
					script.append(" -t nat -A OUTPUT -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -j DROP"); //drop all other packets as Tor won't handle them
					script.append(" || exit\n");
				}	
				else
				{
					script.append(baseDir);
					script.append("iptables -t filter");
					script.append(" -A OUTPUT -p tcp");
					script.append(" -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -m tcp --dport 9040 -j ACCEPT");
					script.append(" || exit\n");
					
					script.append(baseDir);
					script.append("iptables -t filter");
					script.append(" -A OUTPUT -p udp");
					script.append(" -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -m udp --dport 5400 -j ACCEPT");
					script.append(" || exit\n");
										
					script.append(baseDir);
					script.append("iptables");
					script.append(" -t filter -A OUTPUT -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -j DROP"); //drop all other packets as Tor won't handle them
					script.append(" || exit\n");
					
				}
				
			}		
		}
		
    	
    	String[] cmdAdd = {script.toString()};    	
		code = TorServiceUtils.doShellCommand(cmdAdd, res, true, true);
		String msg = res.toString();
		logNotice(cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
		return code;
    }	
	

}
