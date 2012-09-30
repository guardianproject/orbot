package org.torproject.android.service;

import java.io.File;
import java.util.ArrayList;

import org.torproject.android.TorConstants;
import org.torproject.android.settings.TorifiedApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class TorTransProxy implements TorServiceConstants {
	
	private String ipTablesPath;

	public String getIpTablesPath (Context context)
	{
	
		if (ipTablesPath == null)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			if (prefs.getBoolean(TorConstants.PREF_USE_SYSTEM_IPTABLES, false))
			{
				//if the user wants us to use the built-in iptables, then we have to find it
				File fileIpt = new File("/system/bin/iptables");
				
				if (fileIpt.exists())
					 ipTablesPath = fileIpt.getAbsolutePath();
				else
				{
				
					fileIpt = new File("/system/xbin/iptables");
					
					if (fileIpt.exists())
						return (ipTablesPath = fileIpt.getAbsolutePath());
					else
					{
						//use the bundled version
						ipTablesPath = new File(context.getDir("bin", 0),"iptables").getAbsolutePath();
					}
				}				
			}
			else
			{
				//use the bundled version
	
				ipTablesPath = new File(context.getDir("bin", 0),"iptables").getAbsolutePath();
			}
			
		}
			
		return ipTablesPath;
		
	}
	
	public int flushIptables(Context context) throws Exception {
		
		String ipTablesPath = getIpTablesPath(context);
	
    	final StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;

		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -F || exit\n");
	
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -F || exit\n");
    	
    	String[] cmd = {script.toString()};	    	
		code = TorServiceUtils.doShellCommand(cmd, res, true, true);		
		String msg = res.toString();
		
		TorService.logMessage(cmd[0] + ";errCode=" + code + ";resp=" + msg);
			
		
		return code;
	
	}
	
	/*
	public static int purgeIptablesByApp(Context context, TorifiedApp[] apps) throws Exception {

		//restoreDNSResolvConf(); //not working yet
		
		String ipTablesPath = new File(context.getDir("bin", 0),"iptables").getAbsolutePath();
		
    	final StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
		for (int i = 0; i < apps.length; i++)
		{
			//flush nat for every app
			script.append(ipTablesPath);
			script.append(" -t nat -m owner --uid-owner ");
			script.append(tApp.getUid());
			script.append(" -F || exit\n");
		
			script.append(ipTablesPath);
			script.append(" -t filter -m owner --uid-owner ");
			script.append(tApp.getUid());
			script.append(" -F || exit\n");
				
		}
		
    	
    	String[] cmd = {script.toString()};	    	
		code = TorServiceUtils.doShellCommand(cmd, res, true, true);		
		String msg = res.toString();
		logNotice(cmd[0] + ";errCode=" + code + ";resp=" + msg);
			
		
		return code;
		
	}*/
	
	
	/*
	 // 9/19/2010 - NF This code is in process... /etc path on System partition
	 // is read-only on Android for now.
	public static int redirectDNSResolvConf () throws Exception
	{
    	StringBuilder script = new StringBuilder();
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
		//mv resolv.conf to resolve.conf.bak
		String cmd = "mv /etc/resolv.conf /etc/resolv.conf.bak";
		script.append(cmd);
		
		//create new resolve.conf pointing to localhost/127.0.0.1
		cmd = "echo \"nameserver 127.0.0.1\" > /etc/resolv.conf";
		script.append(cmd);
		
		String[] cmdFlush = {script.toString()};
		code = TorServiceUtils.doShellCommand(cmdFlush, res, true, true);
		//String msg = res.toString(); //get stdout from command
		
		
		return code;
	}
	
	public static int restoreDNSResolvConf () throws Exception
	{
		StringBuilder script = new StringBuilder();
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
		//mv resolv.conf to resolve.conf.bak
		String cmd = "mv /etc/resolv.conf.bak /etc/resolv.conf";
		script.append(cmd);
		script.append(" || exit\n");
		
		String[] cmdFlush = {script.toString()};
		code = TorServiceUtils.doShellCommand(cmdFlush, res, true, true);
		//String msg = res.toString(); //get stdout from command
		
		return code;
	}
	*/
	
	public int testOwnerModule(Context context) throws Exception
	{

		TorBinaryInstaller.assertIpTablesBinaries(context, false);
		
		boolean runRoot = true;
    	boolean waitFor = true;
    	
    	int torUid = context.getApplicationInfo().uid;

		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
    	// Allow everything for Tor
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A OUTPUT");
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		script.append(" || exit\n");
		
		String[] cmdAdd = {script.toString()};    	
    	
		code = TorServiceUtils.doShellCommand(cmdAdd, res, runRoot, waitFor);
		String msg = res.toString();
		TorService.logMessage(cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
		
		return code;
    }	
	
	
	
	public int setTransparentProxyingByApp(Context context, ArrayList<TorifiedApp> apps) throws Exception
	{

		boolean runRoot = true;
    	boolean waitFor = true;
    	
		//redirectDNSResolvConf(); //not working yet
		
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
    	flushIptables(context);
				
    	int torUid = context.getApplicationInfo().uid;

		// Allow everything for Tor
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A OUTPUT");
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		script.append(" || exit\n");

		//build up array of shell cmds to execute under one root context
		for (TorifiedApp tApp:apps)
		{

			if (tApp.isTorified()
					&& (!tApp.getUsername().equals(TorServiceConstants.TOR_APP_USERNAME))
					&& (!tApp.getUsername().equals(TorServiceConstants.ORWEB_APP_USERNAME))
					) //if app is set to true
			{
				
				TorService.logMessage("enabling transproxy for app: " + tApp.getUsername() + "(" + tApp.getUid() + ")");

			 	// Set up port redirection
				script.append(ipTablesPath);
				script.append(" -t nat");
				script.append(" -A OUTPUT -p tcp");
				script.append(" ! -d 127.0.0.1"); //allow access to localhost
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -m tcp --syn");
				script.append(" -j REDIRECT --to-ports ");
				script.append(TOR_TRANSPROXY_PORT);
				script.append(" || exit\n");
				
				// Same for DNS
				script.append(ipTablesPath);
				script.append(" -t nat");
				script.append(" -A OUTPUT -p udp");
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -m udp --dport "); 
				script.append(STANDARD_DNS_PORT);
				script.append(" -j REDIRECT --to-ports ");
				script.append(TOR_DNS_PORT);
				script.append(" || exit\n");

				// Allow loopback
				script.append(ipTablesPath);
				script.append(" -t filter");
				script.append(" -A OUTPUT");
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -o lo");
				script.append(" -j ACCEPT");
				script.append(" || exit\n");

				/* 	
				//Outgoing loopback already allowed (23/7/12)

				int[] ports = {TOR_DNS_PORT,TOR_TRANSPROXY_PORT,PORT_SOCKS,PORT_HTTP};
				
				for (int port : ports)
				{
					// Allow packets to localhost (contains all the port-redirected ones)
					script.append(ipTablesPath);
					script.append(" -t filter");
					script.append(" -A OUTPUT");
					script.append(" -m owner --uid-owner ");
					script.append(tApp.getUid());
					script.append(" -p tcp");
					script.append(" -d 127.0.0.1");
					script.append(" --dport ");
					script.append(port);	
					script.append(" -j ACCEPT");
					script.append(" || exit\n");				
				}
				*/

				// Reject all other outbound TCP packets
				script.append(ipTablesPath);
				script.append(" -t filter");
				script.append(" -A OUTPUT");
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -p tcp");
				script.append(" -j REJECT");
				script.append(" || exit\n");
				
			}		
		}			
		

		// Allow everything for Tor
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A OUTPUT");
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		script.append(" || exit\n");
		
		String[] cmdAdd = {script.toString()};    	
    		
		code = TorServiceUtils.doShellCommand(cmdAdd, res, runRoot, waitFor);
		String msg = res.toString();
		TorService.logMessage(cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
		
		return code;
    }	
	
	public int setTransparentProxyingByPort(Context context, int port) throws Exception
	{

		//android.os.Debug.waitForDebugger();
		
		//redirectDNSResolvConf(); //not working yet
		
		//String baseDir = context.getDir("bin",0).getAbsolutePath() + '/';
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	flushIptables(context);
		
		//TCP
		//iptables -t nat -A PREROUTING -i eth0 -p tcp --dport $srcPortNumber -j REDIRECT --to-port $dstPortNumbe

		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -A OUTPUT -p tcp");
		script.append(" --dport ");
		script.append(port);
		//script.append(" -m tcp --syn");
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_TRANSPROXY_PORT);
		script.append(" || exit\n");
		
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -A OUTPUT -p udp");
		script.append(" --dport ");
		script.append(port);
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_TRANSPROXY_PORT);
		script.append(" || exit\n");
		
		//DNS
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -A OUTPUT -p udp ");
		script.append(" -m udp --dport "); 
		script.append(STANDARD_DNS_PORT);
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_DNS_PORT);
		script.append(" || exit\n");
		
    	
    	String[] cmdAdd = {script.toString()};    	
		code = TorServiceUtils.doShellCommand(cmdAdd, res, true, true);
		String msg = res.toString();
		TorService.logMessage(cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
		return code;
    }

	public int enableTetheringRules (Context context) throws Exception
	{
		
		boolean runRoot = true;
    	boolean waitFor = true;
    	
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    
    	String[] hwinterfaces = {"usb0","wl0.1"};
    	
    	for (int i = 0; i < hwinterfaces.length; i++)
    	{
	    	script.append(ipTablesPath);
			script.append(" -t nat -A PREROUTING -i ");
			script.append(hwinterfaces[i]);
			script.append(" -p udp --dport 53 -j REDIRECT --to-ports ");
			script.append(TOR_DNS_PORT);
			script.append(" || exit\n");
			
			script.append(ipTablesPath);
			script.append(" -t nat -A PREROUTING -i ");
			script.append(hwinterfaces[i]);
			script.append(" -p tcp -j REDIRECT --to-ports ");
			script.append(TOR_TRANSPROXY_PORT);
			script.append(" || exit\n");
    	}
		
		String[] cmdAdd = {script.toString()};    	
    	
		code = TorServiceUtils.doShellCommand(cmdAdd, res, runRoot, waitFor);
		String msg = res.toString();
		TorService.logMessage(cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
		
		return code;
	}
	
	public int setTransparentProxyingAll(Context context) throws Exception 
	{
		boolean runRoot = true;
    	boolean waitFor = true;
    	
		//redirectDNSResolvConf(); //not working yet
		
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
    	flushIptables(context);
    	
    	int torUid = context.getApplicationInfo().uid;

		// Set up port redirection
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -A OUTPUT -p tcp");
		script.append(" ! -d 127.0.0.1"); //allow access to localhost
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -m tcp --syn");
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_TRANSPROXY_PORT);
		script.append(" || exit\n");
		
		// Same for DNS
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -A OUTPUT -p udp");
		script.appent(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -m udp --dport "); 
		script.append(STANDARD_DNS_PORT);
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_DNS_PORT);
		script.append(" || exit\n");

		// Allow loopback
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A OUTPUT");
		script.append(" -o lo");
		script.append(" -j ACCEPT");
		script.append(" || exit\n");
		
		// Allow everything for Tor
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A OUTPUT");
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		script.append(" || exit\n");
	

		/* 	
		//Outgoing loopback already allowed (23/7/12)

		int[] ports = {TOR_DNS_PORT,TOR_TRANSPROXY_PORT,PORT_SOCKS,PORT_HTTP};
		
		for (int port : ports)
		{
			// Allow packets to localhost (contains all the port-redirected ones)
			script.append(ipTablesPath);
			script.append(" -t filter");
			script.append(" -A OUTPUT");
			script.append(" -m owner ! --uid-owner ");
			script.append(torUid);
			script.append(" -p tcp");
			script.append(" -d 127.0.0.1");
			script.append(" --dport ");
			script.append(port);	
			script.append(" -j ACCEPT");
			script.append(" || exit\n");
		
		} */
		
		
		if (TorService.ENABLE_DEBUG_LOG)
		{
			//XXX: Comment the following rules for non-debug builds
			script.append(ipTablesPath);
			script.append(" -t filter");
			script.append(" -A OUTPUT");
			script.append(" -p udp");
			script.append(" --dport ");
			script.append(STANDARD_DNS_PORT);
			script.append(" -j LOG");
			script.append(" --log-prefix='ORBOT_DNSLEAK_PROTECTION'");
			script.append(" --log-uid");
			script.append(" || exit\n");
			script.append(ipTablesPath);
			script.append(" -t filter");
			script.append(" -A OUTPUT");
			script.append(" -p tcp");
			script.append(" -j LOG");
			script.append(" --log-prefix='ORBOT_TCPLEAK_PROTECTION'");
			script.append(" --log-uid");
			script.append(" || exit\n");
		}
		
		// Reject DNS that is not from Tor (order is important - first matched rule counts!)
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A OUTPUT");
		script.append(" -p udp");
		script.append(" --dport ");
		script.append(STANDARD_DNS_PORT);
		script.append(" -j REJECT");
		script.append(" || exit\n");
		
		// Reject all other outbound TCP packets
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A OUTPUT");
		script.append(" -p tcp");
		script.append(" -j REJECT");
		script.append(" || exit\n");
		
		String[] cmdAdd = {script.toString()};    	
    	
		code = TorServiceUtils.doShellCommand(cmdAdd, res, runRoot, waitFor);
		String msg = res.toString();
		TorService.logMessage(cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
    	
    	
    	return code;
	}	
	

}
