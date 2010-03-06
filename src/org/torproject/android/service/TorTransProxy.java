package org.torproject.android.service;

import java.util.Iterator;
import java.util.List;

import org.torproject.android.TorifiedApp;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class TorTransProxy {
	
	private final static String TAG = "TorTransProxy";
	
	private final static String CMD_NAT_FLUSH = "iptables -t nat -F || exit\n";
//	private final static String CMD_NAT_IPTABLES_ALL = "iptables -t nat -A OUTPUT -j DNAT --to 127.0.0.1:9040 || exit\n";
	
	private final static String CMD_DNS_PROXYING_ADD = "iptables -t nat -A PREROUTING -p udp --dport 53 -j DNAT --to 127.0.0.1:5400 || exit\n";
	private final static String CMD_DNS_PROXYING_DELETE = "iptables -t nat -D PREROUTING -p udp --dport 53 -j DNAT --to 127.0.0.1:5400 || exit\n";

	private final static String IPTABLES_ADD = " -A ";
	private final static String IPTABLES_DELETE = " -D ";

	private static boolean hasRoot = false;
	
	/**
	 * Check if we have root access
	 * @return boolean true if we have root
	 */
	public static boolean hasRootAccess() {
		if (hasRoot) return true;
		try {
			// Run an empty script just to check root access
			String[] cmd = {"exit 0"};
			if (TorServiceUtils.doShellCommand(cmd, null, true, true) == 0) {
				hasRoot = true;
				return true;
			}
		} catch (Exception e) {
		}
		Log.w(TAG, "Could not acquire root access.");
		return false;
	}
	
	public static int setDNSProxying ()
	{
		
    	final StringBuilder log = new StringBuilder();
    	int code;
    	
    	String[] cmds = {CMD_DNS_PROXYING_ADD};
    	
    
    	code = TorServiceUtils.doShellCommand(cmds, log, true, true);
    	
    	return code;
    	
    	
	}
	
	public static boolean purgeNatIptables() {
    	StringBuilder res = new StringBuilder();
		try {
			String[] cmds = {CMD_NAT_FLUSH};
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
	
	public static boolean setTransparentProxying(Context context, TorifiedApp[] apps) {
		
		String command = null;
		
		command = IPTABLES_ADD; //ADD
		
    	final StringBuilder script = new StringBuilder();
    	
		try {
			int code;
			
			for (int i = 0; i < apps.length; i++)
			{
				
				if (apps[i].getUsername().startsWith("org.torproject.android")) //we never want to Tor this!
					continue;
				
				if (apps[i].isTorified())
				{
					Log.i(TAG,"enabling transproxy for app: " + apps[i].getUsername() + "(" + apps[i].getUid() + ")");
				 
					script.append("iptables -t nat");
					script.append(command);
					script.append("OUTPUT -p tcp -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -j DNAT --to 127.0.0.1:9040");
					script.append(" || exit\n");
				}		
			}
			
	    	StringBuilder res = new StringBuilder();
	    	
	    	String[] cmd = {script.toString()};
	    	
			code = TorServiceUtils.doShellCommand(cmd, res, true, true);
			
				String msg = res.toString();
				Log.e(TAG, msg);
			
		} catch (Exception e) {
			Log.w(TAG, "error refreshing iptables: " + e);
		}
		return false;
    }	

}
