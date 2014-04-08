package org.torproject.android.service;

import java.io.File;
import java.util.ArrayList;

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
	
	public TorTransProxy (TorService torService, File fileXTables)
	{
		mTorService = torService;
		mFileXtables = fileXTables;
	}
	
	public String getIpTablesPath (Context context)
	{

		String ipTablesPath = null;
		
		SharedPreferences prefs = TorService.getSharedPrefs(context.getApplicationContext());

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
		
    	//run the delete commands in a separate process as it might error out
    	//String[] cmdExecClear = {script.toString()};    	    	
		//code = TorServiceUtils.doShellCommand(cmdExecClear, res, runRoot, waitFor);
		
		//reset script
		
    	Shell shell = Shell.startRootShell();
    	
		//build up array of shell cmds to execute under one root context
		for (TorifiedApp tApp:apps)
		{

			if (tApp.isTorified()
					&& (!tApp.getUsername().equals(TorServiceConstants.TOR_APP_USERNAME))
					) //if app is set to true
			{
				
				StringBuilder script = new StringBuilder();    	
				
				logMessage("enabling transproxy for app: " + tApp.getUsername() + "(" + tApp.getUid() + ")");
			 
				// Set up port redirection
		    	script.append(ipTablesPath);
		    	script.append(" -t nat");
		    	script.append(" -A ").append(srcChainName);				
				script.append(" -p tcp");
				script.append(" ! -d 127.0.0.1"); //allow access to localhost
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -m tcp --syn");
				script.append(" -j REDIRECT --to-ports ");
				script.append(TOR_TRANSPROXY_PORT);
				
				shell.add(new SimpleCommand(script.toString())); 
				script = new StringBuilder();
				
				// Same for DNS
				script.append(ipTablesPath);
				script.append(" -t nat");				
				script.append(" -A ").append(srcChainName);				
				script.append(" -p udp -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -m udp --dport "); 
				script.append(STANDARD_DNS_PORT);
				script.append(" -j REDIRECT --to-ports ");
				script.append(TOR_DNS_PORT);

				shell.add(new SimpleCommand(script.toString())); 
				script = new StringBuilder();
				
				int[] ports = {TOR_DNS_PORT,TOR_TRANSPROXY_PORT,PORT_SOCKS,PORT_HTTP};
				
				for (int port : ports)
				{
					// Allow packets to localhost (contains all the port-redirected ones)
					script.append(ipTablesPath);
					script.append(" -t filter");
			        script.append(" -A ").append(srcChainName);
					script.append(" -m owner --uid-owner ");
					script.append(tApp.getUid());
					script.append(" -p tcp");
					script.append(" -d 127.0.0.1");
					script.append(" --dport ");
					script.append(port);	
					script.append(" -j ACCEPT");
					
					shell.add(new SimpleCommand(script.toString())); 
					script = new StringBuilder();
		
				}
				
				// Allow loopback
				script.append(ipTablesPath);
				script.append(" -t filter");
		        script.append(" -A ").append(srcChainName);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -p tcp");
				script.append(" -o lo");
				script.append(" -j ACCEPT");

				shell.add(new SimpleCommand(script.toString())); 
				script = new StringBuilder();

				// Reject all other outbound TCP packets
				script.append(ipTablesPath);
				script.append(" -t filter");
		        script.append(" -A ").append(srcChainName);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -p tcp");
				script.append(" ! -d 127.0.0.1"); //allow access to localhost
				script.append(" -j REJECT");

				shell.add(new SimpleCommand(script.toString())); 
				script = new StringBuilder();
				
				// Reject all other outbound UDP packets
				script.append(ipTablesPath);
				script.append(" -t filter");
		        script.append(" -A ").append(srcChainName);
				script.append(" -m owner --uid-owner ");
				script.append(tApp.getUid());
				script.append(" -p udp");
				script.append(" ! -d 127.0.0.1"); //allow access to localhost
				script.append(" -j REJECT");

				shell.add(new SimpleCommand(script.toString()));
		
			}		
		}		
		
		fixTransproxyLeak (context);
		
		shell.close();
		
		return 1;
    }	
	
	
	public int enableTetheringRules (Context context) throws Exception
	{
		
		String ipTablesPath = getIpTablesPath(context);
		
    	StringBuilder script = new StringBuilder();
    
    	String[] hwinterfaces = {"usb0","wl0.1"};
    	
    	Shell shell = Shell.startRootShell();
    	
    	for (int i = 0; i < hwinterfaces.length; i++)
    	{

			script = new StringBuilder();
	    	script.append(ipTablesPath);
			script.append(" -t nat -A PREROUTING -i ");
			script.append(hwinterfaces[i]);
			script.append(" -p udp --dport 53 -j REDIRECT --to-ports ");
			script.append(TOR_DNS_PORT);
			
			shell.add(new SimpleCommand(script.toString()));
			
			script = new StringBuilder();
			script.append(ipTablesPath);
			script.append(" -t nat -A PREROUTING -i ");
			script.append(hwinterfaces[i]);
			script.append(" -p tcp -j REDIRECT --to-ports ");
			script.append(TOR_TRANSPROXY_PORT);
			
			shell.add(new SimpleCommand(script.toString()));
			
    	}
		

		shell.close();
		
		return 0;
	}
	
	private void logMessage (String msg)
	{
		if (mTorService != null)
			mTorService.logMessage(msg);
		else
			Log.w(TorConstants.TAG,msg);
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
    	
    	shell.add(new SimpleCommand(script.toString()));
    	
		script = new StringBuilder();
		
		script.append(ipTablesPath);
    	script.append(" -t filter");		
    	script.append(" -F ").append(chainName); //delete previous user-defined chain
    	
    	SimpleCommand cmd = new SimpleCommand(script.toString());
    	
    	shell.add(cmd);
    	
    	int exitCode = cmd.getExitCode();
    	
    	shell.close();
		
    	return exitCode;
	}
	
	public int fixTransproxyLeak (Context context) throws Exception 
	{
		String ipTablesPath = getIpTablesPath(context);
		
    	Shell shell = Shell.startRootShell();
    	
    	StringBuilder script = new StringBuilder();
    	script.append(ipTablesPath);
		script.append(" -I OUTPUT ! -o lo ! -d 127.0.0.1 ! -s 127.0.0.1 -p tcp -m tcp --tcp-flags ACK,FIN ACK,FIN -j DROP");
		shell.add(new SimpleCommand(script.toString()));
		
		script = new StringBuilder();
		script.append(ipTablesPath);
		script.append(" -I OUTPUT ! -o lo ! -d 127.0.0.1 ! -s 127.0.0.1 -p tcp -m tcp --tcp-flags ACK,RST ACK,RST -j DROP");
		shell.add(new SimpleCommand(script.toString()));
		
		shell.close();
		
		return 1;
		 
	}
	
	public int setTransparentProxyingAll(Context context) throws Exception 
	{
		
    	
		//redirectDNSResolvConf(); //not working yet
		
		String ipTablesPath = getIpTablesPath(context);
		
    	Shell shell = Shell.startRootShell();
    	
    	int torUid = context.getApplicationInfo().uid;

    	String srcChainName = "OUTPUT";
    	
    	StringBuilder script = new StringBuilder();
    	
		// Allow everything for Tor
		script.append(ipTablesPath);			
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -m owner --uid-owner ");
		script.append(torUid);
		script.append(" -j ACCEPT");
		
		shell.add(new SimpleCommand(script.toString()));
		script = new StringBuilder();
		
    	// Set up port redirection    	
		script.append(ipTablesPath);		
		script.append(" -t nat");
		script.append(" -A ").append(srcChainName);
		script.append(" -p tcp");
		script.append(" ! -d 127.0.0.1"); //allow access to localhost
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -m tcp --syn");
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_TRANSPROXY_PORT);

		shell.add(new SimpleCommand(script.toString()));
		script = new StringBuilder();
		
		// Same for DNS
		script.append(ipTablesPath);
		script.append(" -t nat");
		script.append(" -A ").append(srcChainName);
		script.append(" -p udp");
		script.append(" ! -d 127.0.0.1"); //allow access to localhost
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -m udp --dport "); 
		script.append(STANDARD_DNS_PORT);
		script.append(" -j REDIRECT --to-ports ");
		script.append(TOR_DNS_PORT);

		shell.add(new SimpleCommand(script.toString()));
		script = new StringBuilder();

		
		/**
		int[] ports = {TOR_DNS_PORT,TOR_TRANSPROXY_PORT,PORT_SOCKS,PORT_HTTP};
		
		for (int port : ports)
		{
			// Allow packets to localhost (contains all the port-redirected ones)
			script.append(ipTablesPath);			
			script.append(" -t filter");
			script.append(" -A ").append(srcChainName);
			script.append(" -m owner ! --uid-owner ");
			script.append(torUid);
			script.append(" -p tcp");
			script.append(" -d 127.0.0.1");
			script.append(" --dport ");
			script.append(port);	
			script.append(" -j ACCEPT");
			script.append(" || exit\n");
		
		}**/
		
		// Allow loopback
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -p tcp");
		script.append(" -o lo");
		script.append(" -j ACCEPT");

		shell.add(new SimpleCommand(script.toString()));
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

			shell.add(new SimpleCommand(script.toString()));
			script = new StringBuilder();

			script.append(ipTablesPath);			
			script.append(" -t filter");
			script.append(" -A ").append(srcChainName);
	    	script.append(" -p tcp");
			script.append(" -j LOG");
			script.append(" --log-prefix='ORBOT_TCPLEAK_PROTECTION'");
			script.append(" --log-uid");

			shell.add(new SimpleCommand(script.toString()));
			script = new StringBuilder();

		}

		// Reject all other outbound TCP packets
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -p tcp");
		script.append(" ! -d 127.0.0.1"); //allow access to localhost
		script.append(" -j REJECT");

		shell.add(new SimpleCommand(script.toString()));
		script = new StringBuilder();

		// Reject all other outbound UDP packets
		script.append(ipTablesPath);
		script.append(" -t filter");
		script.append(" -A ").append(srcChainName);
		script.append(" -m owner ! --uid-owner ");
		script.append(torUid);
		script.append(" -p udp");
		script.append(" ! -d 127.0.0.1"); //allow access to localhost
		script.append(" -j REJECT");

		SimpleCommand cmd = new SimpleCommand(script.toString());
		shell.add(cmd);
		
		fixTransproxyLeak (context);
		
		int exitCode = cmd.getExitCode();
		
		shell.close();
		
    	return exitCode;
	}	
	

}
