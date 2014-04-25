package org.torproject.android.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;
import org.torproject.android.TorConstants;
import org.torproject.android.settings.TorifiedApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class TorTransProxy implements TorServiceConstants {
	
	private boolean useSystemIpTables = false;
	private String mSysIptables = null;
	private TorService mTorService = null;
	private File mFileXtables = null;
	
	private final static String ALLOW_LOCAL = " ! -o lo ! -d 127.0.0.1 ! -s 127.0.0.1 ";

	public TorTransProxy (TorService torService, File fileXTables)
	{
		mTorService = torService;
		mFileXtables = fileXTables;
	}
	
	public String getIpTablesPath (Context context)
	{

		String ipTablesPath = null;
		
		SharedPreferences prefs = TorServiceUtils.getSharedPrefs(context);

		useSystemIpTables = prefs.getBoolean(TorConstants.PREF_USE_SYSTEM_IPTABLES, false);
		
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

		useSystemIpTables = prefs.getBoolean(TorConstants.PREF_USE_SYSTEM_IPTABLES, false);
		
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
	
	public int setTransparentProxyingByApp(Context context, ArrayList<TorifiedApp> apps) throws Exception
	{
		String ipTablesPath = getIpTablesPath(context);
		
    	//StringBuilder script = new StringBuilder();
    	
    	String srcChainName = "OUTPUT";
		
    	int torUid = context.getApplicationInfo().uid;

    	//run the delete commands in a separate process as it might error out
    	//String[] cmdExecClear = {script.toString()};    	    	
		//code = TorServiceUtils.doShellCommand(cmdExecClear, res, runRoot, waitFor);
		
		//reset script
		
    	Shell shell = Shell.startRootShell();
    	int lastExit = -1;
    	StringBuilder script;    	
		
    	
    	// Allow everything for Tor
    	
		//build up array of shell cmds to execute under one root context
		for (TorifiedApp tApp:apps)
		{

			if (tApp.isTorified()
					&& (!tApp.getUsername().equals(TorServiceConstants.TOR_APP_USERNAME))
					) //if app is set to true
			{
				
				
				logMessage("enabling transproxy for app: " + tApp.getUsername() + "(" + tApp.getUid() + ")");
			 
				dropAllIPv6Traffic(context, tApp.getUid());
				
		    	script = new StringBuilder();

				// Allow loopback
				script.append(ipTablesPath);
				script.append(" -t filter");
		        script.append(" -A ").append(srcChainName);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -o lo");
				script.append(" -j ACCEPT");

				executeCommand (shell, script.toString());
				script = new StringBuilder();
				
				// Set up port redirection
		    	script.append(ipTablesPath);
		    	script.append(" -t nat");
		    	script.append(" -A ").append(srcChainName);				
				script.append(" -p tcp");
				script.append(ALLOW_LOCAL);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -m tcp --syn");
				script.append(" -j REDIRECT --to-ports ");
				script.append(TOR_TRANSPROXY_PORT);
				
				executeCommand (shell, script.toString());
				script = new StringBuilder();
				
				// Same for DNS
				script.append(ipTablesPath);
				script.append(" -t nat");
				script.append(" -A ").append(srcChainName);
				script.append(" -p udp");
				script.append(" -m owner ! --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -m udp --dport "); 
				script.append(STANDARD_DNS_PORT);
				script.append(" -j REDIRECT --to-ports ");
				script.append(TOR_DNS_PORT);

				executeCommand (shell, script.toString());
				script = new StringBuilder();
				
				// Reject all other outbound packets
				script.append(ipTablesPath);
				script.append(" -t filter");
		        script.append(" -A ").append(srcChainName);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());				
				script.append(ALLOW_LOCAL);
				script.append(" -j REJECT");

				lastExit = executeCommand (shell, script.toString());
				
		
			}		
		}		
		
		shell.close();
		
		return lastExit;
    }	
	
	private int executeCommand (Shell shell, String cmdString) throws IOException, TimeoutException
	{
		SimpleCommand cmd = new SimpleCommand(cmdString +  "|| exit");
		shell.add(cmd);
		logMessage(cmdString);// + "; exit=" + cmd.getExitCode() + ";output=" + cmd.getOutput());
		
		return cmd.getExitCode();
	}
	
	
	public int enableTetheringRules (Context context) throws Exception
	{
		
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    
    	String[] hwinterfaces = {"usb0","wl0.1"};
    	
    	Shell shell = Shell.startRootShell();
    	
    	int lastExit = -1;
    	
    	for (int i = 0; i < hwinterfaces.length; i++)
    	{

			script = new StringBuilder();
	    	script.append(ipTablesPath);
			script.append(" -t nat -A PREROUTING -i ");
			script.append(hwinterfaces[i]);
			script.append(" -p udp --dport 53 -j REDIRECT --to-ports ");
			script.append(TOR_DNS_PORT);
			
			executeCommand (shell, script.toString());
			script = new StringBuilder();
			
			
			script = new StringBuilder();
			script.append(ipTablesPath);
			script.append(" -t nat -A PREROUTING -i ");
			script.append(hwinterfaces[i]);
			script.append(" -p tcp -j REDIRECT --to-ports ");
			script.append(TOR_TRANSPROXY_PORT);
			
			lastExit = executeCommand (shell, script.toString());
			script = new StringBuilder();
			
			
    	}
		

		shell.close();
		
		return lastExit;
	}
	
	private void logMessage (String msg)
	{
		if (mTorService != null)
			mTorService.logMessage(msg);
	}
	
	public int clearTransparentProxyingAll(Context context) throws Exception 
	{

		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();    	

    	Shell shell = Shell.startRootShell();
    	
    	String chainName = "OUTPUT";
    	
		script = new StringBuilder();
		script.append(ipTablesPath);
    	script.append(" -t nat");		
    	script.append(" -F ").append(chainName); //delete previous user-defined chain
    	
    	executeCommand (shell, script.toString());
		script = new StringBuilder();
		
    	
		script = new StringBuilder();
		
		script.append(ipTablesPath);
    	script.append(" -t filter");		
    	script.append(" -F ").append(chainName); //delete previous user-defined chain
    	
    	int lastExit = executeCommand (shell, script.toString());
		
    	shell.close();
    	
    	clearAllIPv6Filters(context);
		
    	return lastExit;
	}
	
	public int fixTransproxyLeak (Context context) throws Exception 
	{
		String ipTablesPath = getIpTablesPath(context);
		
    	Shell shell = Shell.startRootShell();
    	
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
		
		shell.close();
		
		return lastExit;
		 
	}
	
	public int dropAllIPv6Traffic (Context context, int appUid) throws Exception
	{

		String ip6tablesPath = getIp6TablesPath(context);
		Shell shell = Shell.startRootShell();
    	
		
    	StringBuilder script;

		script = new StringBuilder();
		script.append(ip6tablesPath);			
		script.append(" -A OUTPUT");

		if (appUid != -1)
		{
			script.append(" -m owner --uid-owner ");
			script.append(appUid);	
		}
		
		script.append(" -j DROP");
		
		int lastExit = executeCommand (shell, script.toString());
		
		shell.close();
		
		return lastExit;
	}
	
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
	}
	
	public int setTransparentProxyingAll(Context context) throws Exception 
	{
		
		dropAllIPv6Traffic(context,-1);
		
		String ipTablesPath = getIpTablesPath(context);
		
    	Shell shell = Shell.startRootShell();
    	
    	int torUid = context.getApplicationInfo().uid;

    	String srcChainName = "OUTPUT";
    	
    	StringBuilder script = new StringBuilder();
    	
		// Allow everything for Tor
    	
		script.append(ipTablesPath);			
		script.append(" -t nat");
		script.append(" -A ").append(srcChainName);
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		
		executeCommand (shell, script.toString());
		script = new StringBuilder();

		// Allow loopback
		
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -A ").append(srcChainName);
		script.append(" -o lo");
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
    	// Set up port redirection    	
		script.append(ipTablesPath);		
		script.append(" -t nat");
		script.append(" -A ").append(srcChainName);
		script.append(" -p tcp");
		script.append(ALLOW_LOCAL); //allow access to localhost
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -m tcp --syn");
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_TRANSPROXY_PORT);

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		// Same for DNS
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -A ").append(srcChainName);
		script.append(" -p udp");
		script.append(ALLOW_LOCAL); //allow access to localhost
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -m udp --dport "); 
		script.append(STANDARD_DNS_PORT);
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_DNS_PORT);

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		
		if (TorService.ENABLE_DEBUG_LOG)
		{
			//XXX: Comment the following rules for non-debug builds
			script.append(ipTablesPath);			
			script.append(" -t filter");
			script.append(" -A ").append(srcChainName);
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
			script.append(" -A ").append(srcChainName);
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
		script.append(" -A ").append(srcChainName);
		script.append(" -p tcp");
		script.append(" -m tcp");
		script.append(" --dport ").append(TOR_TRANSPROXY_PORT);
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		//allow access to local SOCKS port
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -p tcp");
		script.append(" -m tcp");
		script.append(" --dport ").append(PORT_SOCKS);
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		//allow access to local SOCKS port
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -p tcp");
		script.append(" -m tcp");
		script.append(" --dport ").append(PORT_HTTP);
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		//allow access to local DNS port
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -p udp");
		script.append(" -m udp");
		script.append(" --dport ").append(TOR_DNS_PORT);
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		//allow access to local DNS port
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -p udp");
		script.append(" -m udp");
		script.append(" --dport ").append(TOR_DNS_PORT);
		script.append(" -j ACCEPT");

		executeCommand (shell, script.toString());
		script = new StringBuilder();
		
		
		// Reject all other packets
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(ALLOW_LOCAL); //allow access to localhost
		script.append(" -j REJECT");

		int lastExit = executeCommand (shell, script.toString());
		
	//	fixTransproxyLeak (context);
		
		shell.close();
		
    	return lastExit;
	}	
	

}
