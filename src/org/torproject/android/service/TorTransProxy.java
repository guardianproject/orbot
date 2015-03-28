package org.torproject.android.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;
import org.torproject.android.OrbotConstants;
import org.torproject.android.settings.TorifiedApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class TorTransProxy implements TorServiceConstants {
	
	private boolean useSystemIpTables = false;
	private String mSysIptables = null;
	private TorService mTorService = null;
	private File mFileXtables = null;
	
	private final static String ALLOW_LOCAL = " ! -d 127.0.0.1";

	private int mTransProxyPort = TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT;
	private int mDNSPort = TorServiceConstants.TOR_DNS_PORT_DEFAULT;
	
	public TorTransProxy (TorService torService, File fileXTables)
	{
		mTorService = torService;
		mFileXtables = fileXTables;	
	}
	
	public void setTransProxyPort (int transProxyPort)
	{
		mTransProxyPort = transProxyPort;
	}
	
	public void setDNSPort (int dnsPort)
	{
		mDNSPort = dnsPort;
	}
	
	public String getIpTablesPath (Context context)
	{

		String ipTablesPath = null;
		
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(context);

		useSystemIpTables = prefs.getBoolean(OrbotConstants.PREF_USE_SYSTEM_IPTABLES, false);
		
		if (useSystemIpTables)
		{
			ipTablesPath = findSystemIPTables();
		}
		else
		{
			ipTablesPath = mFileXtables.getAbsolutePath();
			ipTablesPath += " iptables"; //append subcommand since we are using xtables now
			
		}
			
		return ipTablesPath;
	}
	
	public String getIp6TablesPath (Context context)
	{

		String ipTablesPath = null;
		
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(context);

		useSystemIpTables = prefs.getBoolean(OrbotConstants.PREF_USE_SYSTEM_IPTABLES, false);
		
		if (useSystemIpTables)
		{
			ipTablesPath = findSystemIP6Tables();
		}
		else
		{
			ipTablesPath = mFileXtables.getAbsolutePath();
			ipTablesPath += " ip6tables"; //append subcommand since we are using xtables now
			
		}
			
		return ipTablesPath;
	
	}
	
	private String findSystemIPTables ()
	{
		if (mSysIptables != null)
		{
			return mSysIptables;
		}
		else
		{
		
			//if the user wants us to use the built-in iptables, then we have to find it
			File fileIpt = new File("/system/xbin/iptables");
			
			if (fileIpt.exists())
				mSysIptables = fileIpt.getAbsolutePath();
			else
			{
			
				fileIpt = new File("/system/bin/iptables");
				
				if (fileIpt.exists())
					mSysIptables = fileIpt.getAbsolutePath();
			}
		}
		
		return mSysIptables;
	}
	

	
	private String findSystemIP6Tables ()
	{
		
		//if the user wants us to use the built-in iptables, then we have to find it
		File fileIpt = new File("/system/xbin/ip6tables");
		
		if (fileIpt.exists())
			mSysIptables = fileIpt.getAbsolutePath();
		else
		{
		
			fileIpt = new File("/system/bin/ip6tables");
			
			if (fileIpt.exists())
				mSysIptables = fileIpt.getAbsolutePath();
		}
		
		
		return mSysIptables;
	}
	
	/*
	public int flushIptablesAll(Context context) throws Exception {
		
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
	
	}*/
	
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
	/*
	public int testOwnerModule(Context context, String ipTablesPath) throws Exception
	{

		TorBinaryInstaller.assertIpTablesBinaries(context, false);
		
		boolean runRoot = true;
    	boolean waitFor = true;
    	
    	int torUid = context.getApplicationInfo().uid;

    	StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
    	// Allow everything for Tor
		script.append(ipTablesPath);
		script.append(" -A OUTPUT");
		script.append(" -t filter");
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		script.append(" || exit\n");
		
		script.append(ipTablesPath);
		script.append(" -D OUTPUT");
		script.append(" -t filter");
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		script.append(" || exit\n");
		
		String[] cmdAdd = {script.toString()};    	
    	
		code = TorServiceUtils.doShellCommand(cmdAdd, res, runRoot, waitFor);
		String msg = res.toString();
		
		if (mTorService != null)
		logMessage(cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
		
		return code;
    }	
	*/
	
	/*
	public int clearTransparentProxyingByApp (Context context, ArrayList<TorifiedApp> apps) throws Exception
	{
		boolean runRoot = true;
    	boolean waitFor = true;
    	
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    	
    	StringBuilder res = new StringBuilder();
    	int code = -1;
    	
    	String chainName = "ORBOT";
		String jumpChainName = "OUTPUT";
		
		script.append(ipTablesPath);
    	script.append(" --flush ").append(chainName); //delete previous user-defined chain
    	script.append(" || exit\n");
    	
		script.append(ipTablesPath);
    	script.append(" -D ").append(jumpChainName);
    	script.append(" -j ").append(chainName);
    	script.append(" || exit\n");
    	
    	script.append(ipTablesPath);
    	script.append(" -X ").append(chainName); //delete previous user-defined chain
    	script.append(" || exit\n");

		String[] cmdAdd = {script.toString()};    	
    		
		code = TorServiceUtils.doShellCommand(cmdAdd, res, runRoot, waitFor);
		String msg = res.toString();
		
		logMessage(cmdAdd[0] + ";errCode=" + code + ";resp=" + msg);
		
		return code;
	}*/
	
	public int setTransparentProxyingByApp(Context context, ArrayList<TorifiedApp> apps, boolean enableRule, Shell shell) throws Exception
	{
		String ipTablesPath = getIpTablesPath(context);
		
    	//StringBuilder script = new StringBuilder();
    	
		String action = " -A ";
    	String srcChainName = "OUTPUT";

		if (!enableRule)
			action = " -D ";
		
    	//run the delete commands in a separate process as it might error out
    	//String[] cmdExecClear = {script.toString()};    	    	
		//code = TorServiceUtils.doShellCommand(cmdExecClear, res, runRoot, waitFor);
		
		//reset script
		
    	int lastExit = -1;
    	StringBuilder script;    	
    	

		// Same for DNS
		script = new StringBuilder();
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(action).append(srcChainName);
		script.append(" -p udp");
		//script.append(" -m owner --uid-owner ");
		//script.append(tApp.getUid());
		//script.append(" -m udp --dport "); 
		script.append(" --dport ");
		script.append(STANDARD_DNS_PORT);
		script.append(" -j REDIRECT --to-ports ");
		script.append(mDNSPort);
		executeCommand (shell, script.toString());
		
    	// Allow everything for Tor
    	
		//build up array of shell cmds to execute under one root context
		for (TorifiedApp tApp:apps)
		{

			if (((!enableRule) || tApp.isTorified())
					&& (!tApp.getUsername().equals(TorServiceConstants.TOR_APP_USERNAME))
					) //if app is set to true
			{
				
				
				logMessage("transproxy for app: " + tApp.getUsername() + " (" + tApp.getUid() + "): enable=" + enableRule);
				
				dropAllIPv6Traffic(context, tApp.getUid(),enableRule, shell);
				
		    	script = new StringBuilder();

				// Allow loopback
		    	/**
				script.append(ipTablesPath);
				script.append(" -t filter");
		        script.append(action).append(srcChainName);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -o lo");
				script.append(" -j ACCEPT");

				executeCommand (shell, script.toString());
				script = new StringBuilder();
				**/
				
				// Set up port redirection
		    	script.append(ipTablesPath);
		    	script.append(" -t nat");
		    	script.append(action).append(srcChainName);				
				script.append(" -p tcp");
				script.append(ALLOW_LOCAL);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -m tcp --syn");
				script.append(" -j REDIRECT --to-ports ");
				script.append(mTransProxyPort);
				
				executeCommand (shell, script.toString());
				
				
				script = new StringBuilder();
				
				// Reject all other outbound packets
				script.append(ipTablesPath);
				script.append(" -t filter");
		        script.append(action).append(srcChainName);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());				
				script.append(ALLOW_LOCAL);
				script.append(" -j REJECT");

				lastExit = executeCommand (shell, script.toString());
				
		
			}		
		}		
		
		return lastExit;
    }	
	
	private int executeCommand (Shell shell, String cmdString) throws IOException, TimeoutException
	{
		SimpleCommand cmd = new SimpleCommand(cmdString);
		shell.add(cmd);
		int exitCode = cmd.getExitCode();
		String output = cmd.getOutput();
		
		logMessage(cmdString + "; exit=" + exitCode + ";output=" + output);
		
		return exitCode;
	}
	
	
	public int enableTetheringRules (Context context, Shell shell) throws Exception
	{
		
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    
    	String[] hwinterfaces = {"usb0","wl0.1"};
    	
    	
    	int lastExit = -1;
    	
    	for (int i = 0; i < hwinterfaces.length; i++)
    	{

			script = new StringBuilder();
	    	script.append(ipTablesPath);
			script.append(" -t nat -A PREROUTING -i ");
			script.append(hwinterfaces[i]);
			script.append(" -p udp --dport 53 -j REDIRECT --to-ports ");
			script.append(mDNSPort);
			
			executeCommand (shell, script.toString());
			script = new StringBuilder();
			
			
			script = new StringBuilder();
			script.append(ipTablesPath);
			script.append(" -t nat -A PREROUTING -i ");
			script.append(hwinterfaces[i]);
			script.append(" -p tcp -j REDIRECT --to-ports ");
			script.append(mTransProxyPort);
			
			lastExit = executeCommand (shell, script.toString());
			script = new StringBuilder();
			
			
    	}
		

		return lastExit;
	}
	
	private void logMessage (String msg)
	{
		if (mTorService != null)
			mTorService.debug(msg);
	}
	

	
	public int fixTransproxyLeak (Context context, Shell shell) throws Exception 
	{
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    	script.append(ipTablesPath);
		script.append(" -I OUTPUT ! -o lo ! -d 127.0.0.1 ! -s 127.0.0.1 -p tcp -m tcp --tcp-flags ACK,FIN ACK,FIN -j DROP");
		
		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		script = new StringBuilder();
		script.append(ipTablesPath);
		script.append(" -I OUTPUT ! -o lo ! -d 127.0.0.1 ! -s 127.0.0.1 -p tcp -m tcp --tcp-flags ACK,RST ACK,RST -j DROP");
		
		int lastExit = executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		return lastExit;
		 
	}
	
	public int dropAllIPv6Traffic (Context context, int appUid, boolean enableDrop, Shell shell) throws Exception
	{

		String action = " -A ";
		String chain = "OUTPUT";
		
		if (!enableDrop)
			action = " -D ";
		
		String ip6tablesPath = getIp6TablesPath(context);
    	
    	StringBuilder script;

		script = new StringBuilder();
		script.append(ip6tablesPath);			
		script.append(action);
		script.append(chain);

		if (appUid != -1)
		{
			script.append(" -m owner --uid-owner ");
			script.append(appUid);	
		}
		
		script.append(" -j DROP");
		
		int lastExit = executeCommand (shell, script.toString());
		
		return lastExit;
	}
	
	/*
	public int clearAllIPv6Filters (Context context) throws Exception
	{

		String ip6tablesPath = getIp6TablesPath(context);
		Shell shell = Shell.startRootShell();
    	
    	StringBuilder script;

		script = new StringBuilder();
		script.append(ip6tablesPath);			
		script.append(" -t filter");
		script.append(" -F OUTPUT");
		int lastExit = executeCommand (shell, script.toString());
		
		shell.close();
		
		return lastExit;
	}*/
	
	public int flushTransproxyRules (Context context) throws Exception 
	{
		int exit = -1;
		
		String ipTablesPath = getIpTablesPath(context);
		Shell shell = Shell.startRootShell();
		
		StringBuilder script = new StringBuilder();
		script.append(ipTablesPath);			
		script.append(" -t nat ");
		script.append(" -F ");
		
    	executeCommand (shell, script.toString());
		
		script = new StringBuilder();
		script.append(ipTablesPath);			
		script.append(" -t filter ");
		script.append(" -F ");
		executeCommand (shell, script.toString());
		
		dropAllIPv6Traffic(context,-1,false, shell);

		return exit;
	}
	
	public int setTransparentProxyingAll(Context context, boolean enable, Shell shell) throws Exception 
	{
	  	
		String action = " -A ";
    	String srcChainName = "OUTPUT";

		if (!enable)
			action = " -D ";

		dropAllIPv6Traffic(context,-1,enable, shell);
		
		String ipTablesPath = getIpTablesPath(context);
		
    	
    	int torUid = context.getApplicationInfo().uid;
    	
    	StringBuilder script = new StringBuilder();
    	
		// Allow everything for Tor
    	
		script.append(ipTablesPath);			
		script.append(" -t nat");
		script.append(action).append(srcChainName);
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		
		executeCommand (shell, script.toString());
		script = new StringBuilder();

		// Allow loopback
		
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(action).append(srcChainName);
		script.append(" -o lo");
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
    	// Set up port redirection    	
		script.append(ipTablesPath);		
		script.append(" -t nat");
		script.append(action).append(srcChainName);
		script.append(" -p tcp");
		script.append(ALLOW_LOCAL); //allow access to localhost
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -m tcp --syn");
		script.append(" -j REDIRECT --to-ports ");
		script.append(mTransProxyPort);

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		// Same for DNS
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(action).append(srcChainName);
		script.append(" -p udp");
		script.append(ALLOW_LOCAL); //allow access to localhost
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		//script.append(" -m udp --dport "); 
		script.append(" --dport ");
		script.append(STANDARD_DNS_PORT);
		script.append(" -j REDIRECT --to-ports ");
		script.append(mDNSPort);

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		
		if (TorService.ENABLE_DEBUG_LOG)
		{
			//XXX: Comment the following rules for non-debug builds
			script.append(ipTablesPath);			
			script.append(" -t filter");
			script.append(action).append(srcChainName);
			script.append(" -p udp");
			script.append(" --dport ");
			script.append(STANDARD_DNS_PORT);
			script.append(" -j LOG");
			script.append(" --log-prefix='ORBOT_DNSLEAK_PROTECTION'");
			script.append(" --log-uid");

			executeCommand (shell, script.toString());
			script = new StringBuilder();
			
			script.append(ipTablesPath);			
			script.append(" -t filter");
			script.append(action).append(srcChainName);
	    	script.append(" -p tcp");
			script.append(" -j LOG");
			script.append(" --log-prefix='ORBOT_TCPLEAK_PROTECTION'");
			script.append(" --log-uid");

			executeCommand (shell, script.toString());
			script = new StringBuilder();
			
		}

		//allow access to transproxy port
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(action).append(srcChainName);
		script.append(" -p tcp");
		script.append(" -m tcp");
		script.append(" --dport ").append(mTransProxyPort);
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		//allow access to local HTTP port
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(action).append(srcChainName);
		script.append(" -p tcp");
		script.append(" -m tcp");
		script.append(" --dport ").append(mTorService.getHTTPPort());
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		//allow access to local SOCKS port
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(action).append(srcChainName);
		script.append(" -p tcp");
		script.append(" -m tcp");
		script.append(" --dport ").append(mTorService.getSOCKSPort());
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		//allow access to local DNS port
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(action).append(srcChainName);
		script.append(" -p udp");
		script.append(" -m udp");
		script.append(" --dport ").append(mDNSPort);
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		// Reject all other packets
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(action).append(srcChainName);
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(ALLOW_LOCAL); //allow access to localhost
		script.append(" -j REJECT");

		int lastExit = executeCommand (shell, script.toString());
		
	//	fixTransproxyLeak (context);
		
    	return lastExit;
	}	
	

}
