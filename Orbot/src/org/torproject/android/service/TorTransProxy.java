package org.torproject.android.service;

import java.io.File;

import org.torproject.android.TorifiedApp;

import android.content.Context;
import android.util.Log;

public class TorTransProxy {
	
	private final static String TAG = "TorTransProxy";
	
	private static String BASE_DIR = "/data/data/" + TorServiceConstants.TOR_APP_USERNAME + "/";
	
	private final static String CMD_NAT_FLUSH = "iptables -t nat -F || exit\n";
	private final static String CMD_FILTER_FLUSH = "iptables -t filter -F || exit\n";
	
	private final static String CMD_DNS_PROXYING_ADD = "iptables -t nat -A PREROUTING -p udp --dport 53 -j DNAT --to 127.0.0.1:5400 || exit\n";
	
	private final static String IPTABLES_ADD = " -A ";
	
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
	
	private static String findBaseDir ()
	{
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
		
			
	}
	public static int setDNSProxying () throws Exception
	{
		String baseDir = findBaseDir();
		
    	final StringBuilder log = new StringBuilder();
    	int code;
    	
    	String[] cmds = {baseDir + CMD_DNS_PROXYING_ADD};
    	
    
    	code = TorServiceUtils.doShellCommand(cmds, log, true, true);
    	
    	return code;
    	
    	
	}

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
    }
	
	public static boolean setTransparentProxyingByApp(Context context, TorifiedApp[] apps, boolean forceAll) throws Exception
	{
	
		String baseDir = findBaseDir();

		String command = null;
		
		command = IPTABLES_ADD; //ADD
		
    	final StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
			for (int i = 0; i < apps.length; i++)
			{
				if (forceAll || apps[i].isTorified())
				{
					
					if (apps[i].getUsername().equals(TorServiceConstants.TOR_APP_USERNAME))
					{
						Log.i(TAG,"detected Orbot app - will not transproxy");
						
						continue;
					}
					
					Log.i(TAG,"enabling transproxy for app: " + apps[i].getUsername() + "(" + apps[i].getUid() + ")");
				 
					//TCP
					script.append(baseDir);
					script.append("iptables -t nat");
					script.append(" -A OUTPUT -p tcp -m owner --uid-owner ");
					script.append(apps[i].getUid());
				//	script.append(" -j DNAT --to 127.0.0.1:9040");
					script.append(" -m tcp --syn -j REDIRECT --to-ports 9040");
					script.append(" || exit\n");
					
					//UDP
					script.append(baseDir);
					script.append("iptables -t nat");
					script.append(" -A OUTPUT -p udp -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" --dport 53 -j REDIRECT --to-ports 5400"); //drop all UDP packets as Tor won't handle them
					script.append(" || exit\n");
					
					script.append(baseDir);
					script.append("iptables -t nat");
					script.append(" -A OUTPUT -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -j DROP"); //drop all other packets as Tor won't handle them
					script.append(" || exit\n");
					
					
					/*
					 * iptables -t nat -A OUTPUT -p tcp -m owner --uid-owner anonymous -m tcp -j REDIRECT --to-ports 9040 
iptables -t nat -A OUTPUT -p udp -m owner --uid-owner anonymous -m udp --dport 53 -j REDIRECT --to-ports 53 
iptables -t filter -A OUTPUT -p tcp -m owner --uid-owner anonymous -m tcp --dport 9040 -j ACCEPT
iptables -t filter -A OUTPUT -p udp -m owner --uid-owner anonymous -m udp --dport 53 -j ACCEPT
iptables -t filter -A OUTPUT -m owner --uid-owner anonymous -j DROP

					 */
				}		
			}
			
	    	
	    	String[] cmd = {script.toString()};
	    	Log.i(TAG, cmd[0]);
			
			code = TorServiceUtils.doShellCommand(cmd, res, true, true);
			
			String msg = res.toString();
			Log.i(TAG, msg);
			
		
		return false;
    }	
	

	public static boolean setTransparentProxyingByPort(Context context, String[] ports) {
		
		String baseDir = findBaseDir();

		
    	final StringBuilder script = new StringBuilder();
    	
		try {
			int code;
			
			for (int i = 0; i < ports.length; i++)
			{
				Log.i(TAG,"enabling transproxy for port: " + ports[i]);
				 
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
	    	Log.i(TAG, cmd[0]);
			
			code = TorServiceUtils.doShellCommand(cmd, res, true, true);
			
				String msg = res.toString();
				Log.e(TAG, msg);
			
		} catch (Exception e) {
			Log.w(TAG, "error refreshing iptables: " + e);
		}
		return false;
    }

}
