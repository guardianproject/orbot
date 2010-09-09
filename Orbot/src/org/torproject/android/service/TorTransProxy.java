package org.torproject.android.service;

import org.torproject.android.TorifiedApp;

import android.content.Context;
import android.util.Log;

public class TorTransProxy {
	
	private final static String TAG = TorServiceConstants.TAG;
	
	
	//private static String BASE_DIR = "/data/data/" + TorServiceConstants.TOR_APP_USERNAME + "/";
	
	/*
	private final static String CMD_NAT_FLUSH = "iptables -t nat -F || exit\n";
	private final static String CMD_FILTER_FLUSH = "iptables -t filter -F || exit\n";
	
	private final static String CMD_DNS_PROXYING_ADD = "iptables -t nat -A PREROUTING -p udp --dport 53 -j DNAT --to 127.0.0.1:5400 || exit\n";
	
	private final static String IPTABLES_ADD = " -A ";
	*/
	
	//private final static String CMD_DNS_PROXYING_DELETE = "iptables -t nat -D PREROUTING -p udp --dport 53 -j DNAT --to 127.0.0.1:5400 || exit\n";
	// - just calling a system wide flush of iptables rules
	//private final static String IPTABLES_DELETE = " -D "; //not deleting manually anymore - just calling a system wide flush of iptables rules
   // private final static String IPTABLES_DROP_ALL = " -j DROP ";
	
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
		Log.w(TAG, "Could not acquire root access: " + log.toString());
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
			Log.d(TAG,cmd[0] + ";errCode=" + code + ";resp=" + msg);
			
			
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
		
		Log.w(TAG, "Could not acquire check iptables: " + log.toString());
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
	
	
	/*
	public static int setDNSProxying () throws Exception
	{
		String baseDir = findBaseDir();
		
    	final StringBuilder log = new StringBuilder();
    	int code;
    	
    	String[] cmds = {baseDir + CMD_DNS_PROXYING_ADD};
    	
    
    	code = TorServiceUtils.doShellCommand(cmds, log, true, true);
    	
    	return code;
    	
    	
	}*/

    /*
    public static int setIptablesDropAll() {
        // iptables -A OUTPUT -j DROP
    }

    public static int setTransparentProxying() {
        // Flush everything from iptables first
        purgeNatIptables();
        // Setup DNS redirection
        setDNSProxying();
        //

        //
    }
    */

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
			Log.d(TAG,cmd[0] + ";errCode=" + code + ";resp=" + msg);
			
		
		return code;
		
	}
	
	/*
	public static boolean purgeIptables() {
		
		String baseDir = findBaseDir();

		
    	StringBuilder res = new StringBuilder();
		try {
			String[] cmds = {baseDir + CMD_NAT_FLUSH, baseDir + CMD_FILTER_FLUSH};
			int code = TorServiceUtils.doShellCommand(cmds, res, true, true);
			if (code != 0) {
				Log.w(TAG, "error purging iptables. exit code: " + code + "\n" + res);
				return false;
			}
			
			
			return true;
		} catch (Exception e) {
			Log.w(TAG,"error purging iptables: " + e);
			return false;
		}
    }*/
	
	public static int setTransparentProxyingByApp(Context context, TorifiedApp[] apps, boolean forceAll) throws Exception
	{

		android.os.Debug.waitForDebugger();
		
		String baseDir = findBaseDir();

		String iptablesVersion = getIPTablesVersion();
		Log.d(TAG, "iptables version: " + iptablesVersion);
		
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
				
				Log.d(TAG,"enabling transproxy for app: " + apps[i].getUsername() + "(" + apps[i].getUid() + ")");
			 
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
					script.append(" -j DNAT --to 127.0.0.1:9040");
				else
					script.append(" -j REDIRECT --to-ports 9040");
				script.append(" || exit\n");
				
				
				//EVERYTHING ELSE UDP - DROP!
				if (!ipTablesOld) //for some reason this doesn't work on iptables 1.3.7
				{
					script.append(baseDir);
					script.append("iptables");
					script.append(" -A OUTPUT -p udp -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -j DROP"); //drop all other packets as Tor won't handle them
					script.append(" || exit\n");
				}	
				
				
			}		
		}
		
    	
    	String[] cmdAdd = {script.toString()};    	
		code = TorServiceUtils.doShellCommand(cmdAdd, res, true, true);
		String msg = res.toString();
		Log.d(TAG,cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
		return code;
    }	
	

	public static boolean setTransparentProxyingByPort(Context context, String[] ports) {
		
		String baseDir = findBaseDir();

		
    	final StringBuilder script = new StringBuilder();
    	
		try {
			int code;
			
			for (int i = 0; i < ports.length; i++)
			{
				Log.d(TAG,"enabling transproxy for port: " + ports[i]);
				 
				//TCP

				script.append(baseDir);
				script.append("iptables -t nat");
				script.append("-A PREROUTING -p tcp --dport ");
				script.append(ports[i]);
				script.append(" -j DNAT --to 127.0.0.1:9040");
				script.append(" || exit\n");
				
				//UDP

				script.append(baseDir);
				script.append("iptables -t nat");
				script.append("-A PREROUTING -p udp --dport ");
				script.append(ports[i]);
				script.append(" -j DNAT --to 127.0.0.1:9040");
				script.append(" || exit\n");
					
			}
			
	    	StringBuilder res = new StringBuilder();
	    	
	    	String[] cmd = {script.toString()};	    	
			code = TorServiceUtils.doShellCommand(cmd, res, true, true);
			String msg = res.toString();
			Log.d(TAG,cmd[0] + ";errCode=" + code + ";resp=" + msg);
			
		
		} catch (Exception e) {
			Log.w(TAG, "error refreshing iptables: " + e);
		}
		return false;
    }

}
