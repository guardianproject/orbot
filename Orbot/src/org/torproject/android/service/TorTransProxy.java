package org.torproject.android.service;

import org.torproject.android.TorifiedApp;

import android.content.Context;
import android.util.Log;

public class TorTransProxy {
	
	private final static String TAG = "TorTransProxy";
	
	private final static String CMD_NAT_FLUSH = "iptables -t nat -F || exit\n";
	private final static String CMD_DNS_PROXYING_ADD = "iptables -t nat -A PREROUTING -p udp --dport 53 -j DNAT --to 127.0.0.1:5400 || exit\n";
	
	//private final static String CMD_DNS_PROXYING_DELETE = "iptables -t nat -D PREROUTING -p udp --dport 53 -j DNAT --to 127.0.0.1:5400 || exit\n";
	// - just calling a system wide flush of iptables rules
	
	private final static String IPTABLES_ADD = " -A ";
	
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
			String[] cmd = {"whoami"};
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
	
	public static int setDNSProxying () throws Exception
	{
		
    	final StringBuilder log = new StringBuilder();
    	int code;
    	
    	String[] cmds = {CMD_DNS_PROXYING_ADD};
    	
    
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
	
	public static boolean setTransparentProxyingByApp(Context context, TorifiedApp[] apps, boolean forceAll) throws Exception
	{
		
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
					script.append("iptables -t nat");
					script.append(command);
					script.append("OUTPUT -p tcp -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -j DNAT --to 127.0.0.1:9040");
					script.append(" || exit\n");
					
					//UDP
					script.append("iptables -t nat");
					script.append(command);
					script.append("OUTPUT -p udp -m owner --uid-owner ");
					script.append(apps[i].getUid());
					script.append(" -j DROP"); //drop all UDP packets as Tor won't handle them
					script.append(" || exit\n");
				}		
			}
			
	    	
	    	String[] cmd = {script.toString()};
	    	
			code = TorServiceUtils.doShellCommand(cmd, res, true, true);
			
			String msg = res.toString();
			Log.e(TAG, msg);
			
		
		return false;
    }	
	

	public static boolean setTransparentProxyingByPort(Context context, String[] ports) {
		
		String command = null;
		
		command = IPTABLES_ADD; //ADD
		
    	final StringBuilder script = new StringBuilder();
    	
		try {
			int code;
			
			for (int i = 0; i < ports.length; i++)
			{
				Log.i(TAG,"enabling transproxy for port: " + ports[i]);
				 
				//TCP
				script.append("iptables -t nat");
				script.append("-A PREROUTING -p tcp --dport ");
				script.append(ports[i]);
				script.append(" -j DNAT --to 127.0.0.1:9040");
				script.append(" || exit\n");
				
				//UDP
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
				Log.e(TAG, msg);
			
		} catch (Exception e) {
			Log.w(TAG, "error refreshing iptables: " + e);
		}
		return false;
    }

}
