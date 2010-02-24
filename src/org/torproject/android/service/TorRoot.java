/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Contains shared programming interfaces.
 * All iptables "communication" is handled by this class.
 */
public final class TorRoot {
	private final static String TAG = "TOR_ROOT";
	
	// Do we have root access?
	private static boolean hasroot = false;

	private final static String CMD_NAT_FLUSH = "iptables -t nat -F || exit\n";
	private final static String CMD_NAT_IPTABLES_80 = "iptables -t nat -A OUTPUT -p tcp --dport 80 -j DNAT --to 127.0.0.1:8118 || exit\n";
	private final static String CMD_NAT_IPTABLES_443 = "iptables -t nat -A OUTPUT -p tcp --dport 443 -j DNAT --to 127.0.0.1:9040 || exit\n";

	private final static String CMD_DNS_PROXYING = "iptables -t nat -A PREROUTING -p udp --dport 53 -j DNAT --to 127.0.0.1:5400 || exit\n";
	
	public static boolean enableDNSProxying ()
	{
		
    	final StringBuilder script = new StringBuilder();
    	int code;
    	
    	//Enable UDP Proxying
    	script.append(CMD_DNS_PROXYING);
		StringBuilder res = new StringBuilder();
		
		try
		{
			code = runScriptAsRoot(script.toString(), res);
			
			if (code != 0)
			{
				Log.w(TAG, "error apply DNS proxying: " + res.toString());

			}
		} catch (Exception e) {
			Log.w(TAG, "error apply DNS proxying: " + res.toString(), e);
			return false;
		}
		
		return true;
	}
	
	/**
     * Purge and re-add all rules (internal implementation).
     * @param ctx application context (mandatory)
     * @param uids list of selected uids to allow or disallow (depending on the working mode)
     * @param showErrors indicates if errors should be alerted
     */
	public static boolean enabledWebProxying() {
		
    	final StringBuilder script = new StringBuilder();
		try {
			int code;
			
			script.append(CMD_NAT_IPTABLES_80);
			script.append(CMD_NAT_IPTABLES_443);
			/*
			int uid = android.os.Process.getUidForName("dhcp");
			if (uid != -1) script.append("iptables -A OUTPUT " + itfFilter + " -m owner --uid-owner " + uid + " -j ACCEPT || exit\n");
			uid = android.os.Process.getUidForName("wifi");
			if (uid != -1) script.append("iptables -A OUTPUT " + itfFilter + " -m owner --uid-owner " + uid + " -j ACCEPT || exit\n");
			*/
			
	    	StringBuilder res = new StringBuilder();
			code = runScriptAsRoot(script.toString(), res);
			
				String msg = res.toString();
				Log.e(TAG, msg);
			
		} catch (Exception e) {
			Log.w(TAG, "error refreshing iptables: " + e);
		}
		return false;
    }
   
   
    /**
     * Purge all iptables rules.
     * @return true if the rules were purged
     */
	public static boolean purgeNatIptables() {
    	StringBuilder res = new StringBuilder();
		try {
			int code = runScriptAsRoot(CMD_NAT_FLUSH, res);
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
	
  
	/**
	 * Check if we have root access
	 * @return boolean true if we have root
	 */
	public static boolean hasRootAccess() {
		if (hasroot) return true;
		try {
			// Run an empty script just to check root access
			if (runScriptAsRoot("exit 0", null, 20000) == 0) {
				hasroot = true;
				return true;
			}
		} catch (Exception e) {
		}
		Log.w(TAG, "Could not acquire root access.");
		return false;
	}
	
    /**
     * Runs a script as root (multiple commands separated by "\n").
     * 
     * @param script the script to be executed
     * @param res the script output response (stdout + stderr)
     * @param timeout timeout in milliseconds (-1 for none)
     * @return the script exit code
     */
	public static int runScriptAsRoot(String script, StringBuilder res, final long timeout) {
		Log.i(TAG,"executing script: " + script);
		final ScriptRunner runner = new ScriptRunner(script, res);
		runner.start();
		try {
			if (timeout > 0) {
				runner.join(timeout);
			} else {
				runner.join();
			}
			if (runner.isAlive()) {
				// Timed-out
				runner.interrupt();
				runner.destroy();
				runner.join(50);
			}
		} catch (InterruptedException ex) {}
		return runner.exitcode;
    }
	
    /**
     * Runs a script as root (multiple commands separated by "\n") with a default timeout of 5 seconds.
     * 
     * @param script the script to be executed
     * @param res the script output response (stdout + stderr)
     * @param timeout timeout in milliseconds (-1 for none)
     * @return the script exit code
     * @throws IOException on any error executing the script, or writing it to disk
     */
	public static int runScriptAsRoot(String script, StringBuilder res) throws IOException {
		return runScriptAsRoot(script, res, 5000);
	}
	
	/**
	 * Internal thread used to execute scripts as root.
	 */
	private static final class ScriptRunner extends Thread {
		private final String script;
		private final StringBuilder res;
		public int exitcode = -1;
		private Process exec;
		
		/**
		 * Creates a new script runner.
		 * @param script script to run
		 * @param res response output
		 */
		public ScriptRunner(String script, StringBuilder res) {
			this.script = script;
			this.res = res;
		}
		@Override
		public void run() {
			try {
				// Create the "su" request to run the command
				// note that this will create a shell that we must interact to (using stdin/stdout)
				exec = Runtime.getRuntime().exec("su");
				final OutputStreamWriter out = new OutputStreamWriter(exec.getOutputStream());
				// Write the script to be executed
				out.write(script);
				// Ensure that the last character is an "enter"
				if (!script.endsWith("\n")) out.write("\n");
				out.flush();
				// Terminate the "su" process
				out.write("exit\n");
				out.flush();
				final char buf[] = new char[1024];
				// Consume the "stdout"
				InputStreamReader r = new InputStreamReader(exec.getInputStream());
				int read=0;
				while ((read=r.read(buf)) != -1) {
					if (res != null) res.append(buf, 0, read);
				}
				// Consume the "stderr"
				r = new InputStreamReader(exec.getErrorStream());
				read=0;
				while ((read=r.read(buf)) != -1) {
					if (res != null) res.append(buf, 0, read);
				}
				// get the process exit code
				if (exec != null) this.exitcode = exec.waitFor();
			} catch (InterruptedException ex) {
				if (res != null) res.append("\nOperation timed-out");
			} catch (Exception ex) {
				if (res != null) res.append("\n" + ex);
			} finally {
				destroy();
			}
		}
		/**
		 * Destroy this script runner
		 */
		public synchronized void destroy() {
			if (exec != null) exec.destroy();
			exec = null;
		}
	}
	
	public void getApps (Context context)
	{
		PackageManager pMgr = context.getPackageManager();
		
		List<ApplicationInfo> lAppInfo = pMgr.getInstalledApplications(0);
		
		Iterator<ApplicationInfo> itAppInfo = lAppInfo.iterator();
		
		ApplicationInfo aInfo = null;
		
		while (itAppInfo.hasNext())
		{
			aInfo = itAppInfo.next();
			
			boolean appEnabled = aInfo.enabled;
			int uid = aInfo.uid; //-m owner --uid-owner 
			String username = pMgr.getNameForUid(uid);
			String procName = aInfo.processName;
			String name = aInfo.name;
			
		}
	}
}
