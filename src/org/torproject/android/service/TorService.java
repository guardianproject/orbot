/* Copyright (c) 2009-2011, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info/apps/orbot */
/* See LICENSE for licensing information */
/*
 * Code for iptables binary management taken from DroidWall GPLv3
 * Copyright (C) 2009-2010  Rodrigo Zechin Rosauro
 */

package org.torproject.android.service;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import net.freehaven.tor.control.ConfigEntry;
import net.freehaven.tor.control.EventHandler;
import net.freehaven.tor.control.TorControlConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;
import org.torproject.android.OrbotApp;
import org.torproject.android.OrbotConstants;
import org.torproject.android.OrbotMainActivity;
import org.torproject.android.Prefs;
import org.torproject.android.R;
import org.torproject.android.settings.AppManager;
import org.torproject.android.settings.TorifiedApp;
import org.torproject.android.vpn.OrbotVpnService;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class TorService extends Service implements TorServiceConstants, OrbotConstants, EventHandler
{
    
    private String mCurrentStatus = STATUS_OFF;
    
    private final static int CONTROL_SOCKET_TIMEOUT = 0;
        
    private TorControlConnection conn = null;
    private Socket torConnSocket = null;
    private int mLastProcessId = -1;
    
    private int mPortHTTP = HTTP_PROXY_PORT_DEFAULT;
    private int mPortSOCKS = SOCKS_PROXY_PORT_DEFAULT;
    
    private static final int NOTIFY_ID = 1;
    private static final int TRANSPROXY_NOTIFY_ID = 2;
    private static final int ERROR_NOTIFY_ID = 3;
    private static final int HS_NOTIFY_ID = 4;
    
    private static final int MAX_START_TRIES = 3;

    private ArrayList<String> configBuffer = null;
    private ArrayList<String> resetBuffer = null;

    private boolean isTorUpgradeAndConfigComplete = false;

    private File fileControlPort;
    
    private TorTransProxy mTransProxy;

    private long mTotalTrafficWritten = 0;
    private long mTotalTrafficRead = 0;
    private boolean mConnectivity = true; 
    private int mNetworkType = -1;

    private long lastRead = -1;
    private long lastWritten = -1;
    
    private NotificationManager mNotificationManager = null;
    private Builder mNotifyBuilder;
    private Notification mNotification;
    private boolean mNotificationShowing = false;

	boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    private ExecutorService mExecutor = Executors.newFixedThreadPool(1);

    private NumberFormat mNumberFormat = null;

    public void debug(String msg)
    {
        if (Prefs.useDebugLogging())
        {
            Log.d(TAG,msg);
            sendCallbackLogMessage(msg);    

        }
    }
    
    public void logException(String msg, Exception e)
    {
        if (Prefs.useDebugLogging())
        {
            Log.e(TAG,msg,e);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            
            sendCallbackLogMessage(msg + '\n'+ new String(baos.toByteArray()));
            
        }
        else
            sendCallbackLogMessage(msg);
            

    }
    
    
    private boolean findExistingTorDaemon() {
        try {
            mLastProcessId = initControlConnection(3, true);

            if (mLastProcessId != -1 && conn != null) {
                sendCallbackLogMessage(getString(R.string.found_existing_tor_process));
                sendCallbackStatus(STATUS_ON);
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    /* (non-Javadoc)
     * @see android.app.Service#onLowMemory()
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        
        logNotice( "Low Memory Warning!");
        
    }

    private void clearNotifications ()
    {
        if (mNotificationManager != null)
            mNotificationManager.cancelAll();
        

        hmBuiltNodes.clear();
        mNotificationShowing = false;
    }
	        
    @SuppressLint("NewApi")
    private void showToolbarNotification (String notifyMsg, int notifyType, int icon)
     {        
         
         //Reusable code.
         Intent intent = new Intent(TorService.this, OrbotMainActivity.class);
         PendingIntent pendIntent = PendingIntent.getActivity(TorService.this, 0, intent, 0);
 
        if (mNotifyBuilder == null)
        {
            
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                
            if (mNotifyBuilder == null)
            {
                mNotifyBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_stat_tor);

                mNotifyBuilder.setContentIntent(pendIntent);
            }        
                                
        }

        mNotifyBuilder.setContentText(notifyMsg);
        mNotifyBuilder.setSmallIcon(icon);
        
        if (notifyType != NOTIFY_ID)
        {
            mNotifyBuilder.setTicker(notifyMsg);
        //    mNotifyBuilder.setLights(Color.GREEN, 1000, 1000);
        }
        else
        {
            mNotifyBuilder.setTicker(null);
        }
        
        mNotifyBuilder.setOngoing(Prefs.persistNotifications());
        
        mNotification = mNotifyBuilder.build();
        
        if (Build.VERSION.SDK_INT >= 16 && Prefs.expandedNotifications()) {
            // Create remote view that needs to be set as bigContentView for the notification.
             RemoteViews expandedView = new RemoteViews(this.getPackageName(), 
                     R.layout.layout_notification_expanded);
             
             StringBuffer sbInfo = new StringBuffer();
             
             if (notifyType == NOTIFY_ID)
                 expandedView.setTextViewText(R.id.text, notifyMsg);
             else
             {
                 expandedView.setTextViewText(R.id.info, notifyMsg);
             }

             if (hmBuiltNodes.size() > 0)
             {
                 Set<String> itBuiltNodes = hmBuiltNodes.keySet();
                 for (String key : itBuiltNodes)
                 {
                     Node node = hmBuiltNodes.get(key);
                     
                     if (node.ipAddress != null)
                     {
                         sbInfo.append(node.ipAddress);
                     
                         if (node.country != null)
                             sbInfo.append(' ').append(node.country);
                     
                         if (node.organization != null)
                             sbInfo.append(" (").append(node.organization).append(')');
                     
                         sbInfo.append('\n');
                     }
                     
                 }
                 
                 expandedView.setTextViewText(R.id.text2, sbInfo.toString());
             }
             
             expandedView.setTextViewText(R.id.title, getString(R.string.app_name)); 
             
             expandedView.setImageViewResource(R.id.icon, icon);
            mNotification.bigContentView = expandedView;
        }
        
        if (Prefs.persistNotifications() && (!mNotificationShowing))
        {
            startForeground(NOTIFY_ID, mNotification);
            logNotice("Set background service to FOREGROUND");
        }
        else
        {
            mNotificationManager.notify(NOTIFY_ID, mNotification);
        }
        
        mNotificationShowing = true;
     }
    

    /* (non-Javadoc)
     * @see android.app.Service#onStart(android.content.Intent, int)
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            new Thread (new IncomingIntentRouter(intent)).start();
        else
            Log.d(TAG, "Got null onStartCommand() intent");

        return Service.START_STICKY;
    }
    
    private class IncomingIntentRouter implements Runnable
    {
        Intent mIntent;
        
        public IncomingIntentRouter (Intent intent)
        {
            mIntent = intent;
        }
        
        public void run() {
            String action = mIntent.getAction();

            if (action != null) {
                if (action.equals(ACTION_START)) {
                    replyWithStatus(mIntent);
                    startTor();
                    // stopTor() is called when the Service is destroyed
                }
                else if (action.equals(ACTION_STATUS)) {
                    replyWithStatus(mIntent);                    
                }
                else if (action.equals(CMD_SIGNAL_HUP)) {
                    requestTorRereadConfig();
                } else if (action.equals(CMD_NEWNYM)) {
                    newIdentity();
                } else if (action.equals(CMD_FLUSH)) {
                    flushTransparentProxyRules();
                } else if (action.equals(CMD_UPDATE_TRANS_PROXY)) {
                    processTransparentProxying();
                } else if (action.equals(CMD_VPN)) {
                    enableVpnProxy();
                } else if (action.equals(CMD_VPN_CLEAR)) {
                    clearVpnProxy();
                } else if (action.equals(CMD_SET_EXIT)) {
                	
                	setExitNode(mIntent.getStringExtra("exit"));
                	
                } else {
                    Log.w(TAG, "unhandled TorService Intent: " + action);
                }
            }
        }
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent){
         Log.d(TAG,"task removed");
         Intent intent = new Intent( this, DummyActivity.class );
         intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
         startActivity( intent );
    }

    @Override
    public void onDestroy() {
        stopTor();
        unregisterReceiver(mNetworkStateReceiver);
        super.onDestroy();
    }

    private void stopTor() {
        Log.i("TorService", "stopTor");
        try {
            sendCallbackStatus(STATUS_STOPPING);
            sendCallbackLogMessage(getString(R.string.status_shutting_down));

            killAllDaemons();

            //stop the foreground priority and make sure to remove the persistant notification
            stopForeground(true);

            if (Prefs.useRoot() && Prefs.useTransparentProxying())
            {
            	Shell shellRoot = Shell.startRootShell();
                disableTransparentProxy(shellRoot);
            	shellRoot.close();
            }

            sendCallbackLogMessage(getString(R.string.status_disabled));
        }
        catch (Exception e)
        {
            logNotice("An error occured stopping Tor: " + e.getMessage());
            sendCallbackLogMessage(getString(R.string.something_bad_happened));
        }
        clearNotifications();
        sendCallbackStatus(STATUS_OFF);
    }


    private String getHiddenServiceHostname ()
    {

        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        boolean enableHiddenServices = prefs.getBoolean("pref_hs_enable", false);

        StringBuffer result = new StringBuffer();

        if (enableHiddenServices)
        {
            String hsPorts = prefs.getString("pref_hs_ports","");

            StringTokenizer st = new StringTokenizer (hsPorts,",");
            String hsPortConfig = null;

            while (st.hasMoreTokens())
            {

                int hsPort = Integer.parseInt(st.nextToken().split(" ")[0]);;

                File fileDir = new File(OrbotApp.appCacheHome, "hs" + hsPort);
                File file = new File(fileDir, "hostname");


                if (file.exists())
                {
                    try {
                        String onionHostname = Utils.readString(new FileInputStream(file)).trim();

                        if (result.length() > 0)
                            result.append(",");

                        result.append(onionHostname);


                    } catch (FileNotFoundException e) {
                        logException("unable to read onion hostname file",e);
                        showToolbarNotification(getString(R.string.unable_to_read_hidden_service_name), HS_NOTIFY_ID, R.drawable.ic_stat_notifyerr);
                        return null;
                    }
                }
                else
                {
                    showToolbarNotification(getString(R.string.unable_to_read_hidden_service_name), HS_NOTIFY_ID, R.drawable.ic_stat_notifyerr);
                    return null;

                }
            }

            if (result.length() > 0)
            {
                String onionHostname = result.toString();

                showToolbarNotification(getString(R.string.hidden_service_on) + ' ' + onionHostname, HS_NOTIFY_ID, R.drawable.ic_stat_tor);
                Editor pEdit = prefs.edit();
                pEdit.putString("pref_hs_hostname",onionHostname);
                pEdit.commit();

                return onionHostname;
            }

        }

        return null;
    }


    private void killAllDaemons() throws Exception {
        if (conn != null) {
            logNotice("Using control port to shutdown Tor");

            try {
                logNotice("sending HALT signal to Tor process");
                conn.shutdownTor("HALT");

            } catch (IOException e) {
                Log.d(TAG, "error shutting down Tor via connection", e);
            }

            conn = null;
        }

        // try these separately in case one fails, then it can try the next
        File cannotKillFile = null;
        try {
        	TorServiceUtils.killProcess(OrbotApp.fileObfsclient);
        } catch (IOException e) {
            e.printStackTrace();
            cannotKillFile = OrbotApp.fileObfsclient;
        }
        try {
        	TorServiceUtils.killProcess(OrbotApp.fileMeekclient);
        } catch (IOException e) {
            e.printStackTrace();
            cannotKillFile = OrbotApp.fileMeekclient;
        }
        try {
        	TorServiceUtils.killProcess(OrbotApp.filePolipo);
        } catch (IOException e) {
            e.printStackTrace();
            cannotKillFile = OrbotApp.filePolipo;
        }
        try {
            TorServiceUtils.killProcess(OrbotApp.fileTor);
        } catch (IOException e) {
            e.printStackTrace();
            cannotKillFile = OrbotApp.fileTor;
        }
    }

    private void requestTorRereadConfig() {
        try {
            conn.signal("HUP");
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // if that fails, try again using native utils
        try {
        	TorServiceUtils.killProcess(OrbotApp.fileTor, "-1"); // this is -HUP
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

        private void logNotice (String msg)
    {
        if (msg != null && msg.trim().length() > 0)
        {
            if (Prefs.useDebugLogging())
                Log.d(TAG, msg);
        
            sendCallbackLogMessage(msg);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        try
        {
            mNumberFormat = NumberFormat.getInstance(Locale.getDefault()); //localized numbers!

            if (mNotificationManager == null)
            {
               
               IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);               
                registerReceiver(mNetworkStateReceiver , mNetworkStateFilter);
         
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
             
            }
            
            torUpgradeAndConfig();
        
            new Thread(new Runnable ()
            {
                public void run ()
                {
                    try
                    {
                        findExistingTorDaemon();
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG,"error onBind",e);
                        logNotice("error finding exiting process: " + e.toString());
                    }
                    
                }
            }).start();
        
            
            if (OrbotVpnService.mSocksProxyPort == -1)
            	OrbotVpnService.mSocksProxyPort = (int)((Math.random()*1000)+10000); 
            		
        }
        catch (Exception e)
        {
            //what error here
            Log.e(TAG, "Error installing Orbot binaries",e);
            logNotice("There was an error installing Orbot binaries");
        }
        
        Log.i("TorService", "onCreate end");
    }

    private void torUpgradeAndConfig() throws IOException, TimeoutException {
        if (isTorUpgradeAndConfigComplete)
            return;

        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
        String version = prefs.getString(PREF_BINARY_TOR_VERSION_INSTALLED,null);

        logNotice("checking binary version: " + version);
        
        TorResourceInstaller installer = new TorResourceInstaller(this, OrbotApp.appBinHome);
        
        if (version == null || (!version.equals(BINARY_TOR_VERSION)) || (!OrbotApp.fileTor.exists()))
        {
            logNotice("upgrading binaries to latest version: " + BINARY_TOR_VERSION);
            
            boolean success = installer.installResources();
            
            if (success)
                prefs.edit().putString(PREF_BINARY_TOR_VERSION_INSTALLED,BINARY_TOR_VERSION).commit();    
        }

        updateTorConfigFile ();
        isTorUpgradeAndConfigComplete = true;
    }

    private boolean updateTorConfigFile () throws FileNotFoundException, IOException, TimeoutException
    {
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        TorResourceInstaller installer = new TorResourceInstaller(this, OrbotApp.appBinHome);
        
        StringBuffer extraLines = new StringBuffer();
        
        String TORRC_CONTROLPORT_FILE_KEY = "ControlPortWriteToFile";
        fileControlPort = new File(OrbotApp.appBinHome, "control.txt");
        extraLines.append(TORRC_CONTROLPORT_FILE_KEY).append(' ').append(fileControlPort.getCanonicalPath()).append('\n');

//         extraLines.append("RunAsDaemon 1").append('\n');
 //        extraLines.append("AvoidDiskWrites 1").append('\n');
        
         String socksPortPref = prefs.getString(OrbotConstants.PREF_SOCKS,
                 String.valueOf(TorServiceConstants.SOCKS_PROXY_PORT_DEFAULT));
         if (socksPortPref.indexOf(':')!=-1)
             socksPortPref = socksPortPref.split(":")[1];
         
        if (!socksPortPref.equalsIgnoreCase("auto"))
        {
	        boolean isPortUsed = TorServiceUtils.isPortOpen("127.0.0.1",Integer.parseInt(socksPortPref),500);
	        
	        if (isPortUsed) //the specified port is not available, so let Tor find one instead
	        	socksPortPref = "auto";
        }
        
        extraLines.append("SOCKSPort ").append(socksPortPref).append('\n');
        extraLines.append("SafeSocks 0").append('\n');
        extraLines.append("TestSocks 0").append('\n');
        extraLines.append("WarnUnsafeSocks 1").append('\n');
    
        if (Prefs.useTransparentProxying())
        {

            String transPort = prefs.getString("pref_transport", TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT+"");
            String dnsPort = prefs.getString("pref_dnsport", TorServiceConstants.TOR_DNS_PORT_DEFAULT+"");
            
	        extraLines.append("TransPort ").append(transPort).append('\n');
	    	extraLines.append("DNSPort ").append(dnsPort).append("\n");
	        
	        if (Prefs.transparentTethering())
	        {
	            extraLines.append("TransListenAddress 0.0.0.0").append('\n');
	            extraLines.append("DNSListenAddress 0.0.0.0").append('\n');            
	        }
       
	        extraLines.append("VirtualAddrNetwork 10.192.0.0/10").append('\n');
	        extraLines.append("AutomapHostsOnResolve 1").append('\n');
	       
        }

        extraLines.append("DisableNetwork 0").append('\n');
                
        if (Prefs.useDebugLogging())
        {
        	extraLines.append("Log debug syslog").append('\n');    
        	extraLines.append("Log info syslog").append('\n');
        	extraLines.append("SafeLogging 0").append('\n');   

        }
        
        processSettingsImpl(extraLines);
        
        String torrcCustom = new String(prefs.getString("pref_custom_torrc", "").getBytes("US-ASCII"));
        extraLines.append(torrcCustom).append('\n');

        logNotice("updating torrc custom configuration...");

        debug("torrc.custom=" + extraLines.toString());
        
        File fileTorRcCustom = new File(OrbotApp.fileTorRc.getAbsolutePath() + ".custom");
        boolean success = installer.updateTorConfigCustom(fileTorRcCustom, extraLines.toString());    
        
        if (success)
        {
            logNotice ("success.");
        }
        
        return success;
    }

    /**
     * Send Orbot's status in reply to an
     * {@link TorServiceConstants#ACTION_START} {@link Intent}, targeted only to
     * the app that sent the initial request.
     */
    private void replyWithStatus(Intent startRequest) {
        String packageName = startRequest.getStringExtra(EXTRA_PACKAGE_NAME);

        Intent reply = new Intent(ACTION_STATUS);
        reply.putExtra(EXTRA_STATUS, mCurrentStatus);
        reply.putExtra(EXTRA_SOCKS_PROXY, "socks://127.0.0.1:" + mPortSOCKS);
        reply.putExtra(EXTRA_SOCKS_PROXY_HOST, "127.0.0.1");
        reply.putExtra(EXTRA_SOCKS_PROXY_PORT, mPortSOCKS);
        reply.putExtra(EXTRA_HTTP_PROXY, "http://127.0.0.1" + mPortHTTP);
        reply.putExtra(EXTRA_HTTP_PROXY_HOST, "127.0.0.1");
        reply.putExtra(EXTRA_HTTP_PROXY_PORT, mPortHTTP);
        
        if (packageName != null)
        {
        	reply.setPackage(packageName);
        	sendBroadcast(reply);
        }
        else
        {
            LocalBroadcastManager.getInstance(this).sendBroadcast(reply);

        }

    }

    /**
     * The entire process for starting tor and related services is run from this method.
     */
    private void startTor() {
        // STATUS_STARTING is set in onCreate()
        if (mCurrentStatus == STATUS_STOPPING) {
            // these states should probably be handled better
            sendCallbackLogMessage("Ignoring start request, currently " + mCurrentStatus);
            return;
        } else if (mCurrentStatus == STATUS_ON && findExistingTorDaemon()) {
        
            sendCallbackLogMessage("Ignoring start request, already started.");
            
            return;
        }        
        

        try {
        	
	        // make sure there are no stray daemons running
	        killAllDaemons();
	
	        sendCallbackStatus(STATUS_STARTING);
	        sendCallbackLogMessage(getString(R.string.status_starting_up));
	        logNotice(getString(R.string.status_starting_up));
	        
	        ArrayList<String> customEnv = new ArrayList<String>();
	
	        if (Prefs.bridgesEnabled())
	        	if (Prefs.useVpn() && !mIsLollipop)
	        		customEnv.add("TOR_PT_PROXY=socks5://127.0.0.1:" + OrbotVpnService.mSocksProxyPort); 
	        
	        String baseDirectory = OrbotApp.fileTor.getParent();
	        Shell shellUser = Shell.startShell(customEnv, baseDirectory);
	        
	        boolean success = runTorShellCmd(shellUser);
	        
	        if (success)
	        {
	            if (mPortHTTP != -1)
	                runPolipoShellCmd(shellUser);
	            
	            if (Prefs.useRoot() && Prefs.useTransparentProxying())
	            {
	                 Shell shellRoot = Shell.startRootShell();
	
	                disableTransparentProxy(shellRoot);
	                enableTransparentProxy(shellRoot);
	                
	                shellRoot.close();
	            }
	            
	            /**
	            if (Prefs.useVpn()) //we need to turn on VPN here so the proxy is running
	            {
	            	enableVpnProxy();
	            }*/
	
	            
	            getHiddenServiceHostname ();
	        }
	        else
	        {
	                 showToolbarNotification(getString(R.string.unable_to_start_tor), ERROR_NOTIFY_ID, R.drawable.ic_stat_notifyerr);
	        }
	        shellUser.close();

        } catch (Exception e) {
            logException("Unable to start Tor: " + e.toString(), e);
            showToolbarNotification(
                    getString(R.string.unable_to_start_tor) + ": " + e.getMessage(),
                    ERROR_NOTIFY_ID, R.drawable.ic_stat_notifyerr);
            stopTor();
        }
    }

    private boolean flushTransparentProxyRules () {
        if (Prefs.useRoot())
        {
             if (mTransProxy == null)
                 mTransProxy = new TorTransProxy(this, OrbotApp.fileXtables);

             try {
                 mTransProxy.flushTransproxyRules(this);
             } catch (Exception e) {
                 e.printStackTrace();
                 return false;
             }
         
             return true;
        }
        else
        {
            return false;
        }
    }
    
    /*
     * activate means whether to apply the users preferences
     * or clear them out
     * 
     * the idea is that if Tor is off then transproxy is off
     */
    private boolean enableTransparentProxy (Shell shell) throws Exception
     {
        
         if (mTransProxy == null)
         {
             mTransProxy = new TorTransProxy(this, OrbotApp.fileXtables);
             
         }


        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
        String transProxy = prefs.getString("pref_transport", TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT+"");
        String dnsPort = prefs.getString("pref_dnsport", TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT+"");
        
        if (transProxy.indexOf(':')!=-1) //we just want the port for this
            transProxy = transProxy.split(":")[1];
        
        if (dnsPort.indexOf(':')!=-1) //we just want the port for this
            dnsPort = dnsPort.split(":")[1];
        
        mTransProxy.setTransProxyPort(Integer.parseInt(transProxy));
        mTransProxy.setDNSPort(Integer.parseInt(dnsPort));
        
        int code = 0; // Default state is "okay"
        
        if(Prefs.transparentProxyAll())
        {

            code = mTransProxy.setTransparentProxyingAll(this, true, shell);
        }
        else
        {
            ArrayList<TorifiedApp> apps = AppManager.getApps(this, TorServiceUtils.getSharedPrefs(getApplicationContext()));

            code = mTransProxy.setTransparentProxyingByApp(this,apps, true, shell);
        }
        
        debug ("TorTransProxy resp code: " + code);
        
        if (code == 0)
        {

            if (Prefs.transparentTethering())
            {
                showToolbarNotification(getString(R.string.transproxy_enabled_for_tethering_), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor);

                Shell shellRoot = Shell.startRootShell();
                mTransProxy.enableTetheringRules(this, shellRoot);
                shellRoot.close();
                  
            }
            else
            {
                showToolbarNotification(getString(R.string.transparent_proxying_enabled), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor);

            }
        }
        else
        {
            showToolbarNotification(getString(R.string.warning_error_starting_transparent_proxying_), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor);

        }
    
        return true;
     }
    
    /*
     * activate means whether to apply the users preferences
     * or clear them out
     * 
     * the idea is that if Tor is off then transproxy is off
     */
    private boolean disableTransparentProxy (Shell shell) throws Exception
     {
        
         debug ("Transparent Proxying: disabling...");

         if (mTransProxy == null)
             mTransProxy = new TorTransProxy(this, OrbotApp.fileXtables);
 
         mTransProxy.setTransparentProxyingAll(this, false, shell);    
        ArrayList<TorifiedApp> apps = AppManager.getApps(this, TorServiceUtils.getSharedPrefs(getApplicationContext()));
        mTransProxy.setTransparentProxyingByApp(this, apps, false, shell);
    
         return true;
     }
    
    private boolean runTorShellCmd(final Shell shell) throws Exception
    {

        String torrcPath = new File(OrbotApp.appBinHome, TORRC_ASSET_KEY).getCanonicalPath();

        updateTorConfigFile();
        
        sendCallbackLogMessage(getString(R.string.status_starting_up));
        
        String torCmdString = OrbotApp.fileTor.getCanonicalPath()
                + " DataDirectory " + OrbotApp.appCacheHome.getCanonicalPath()
                + " --defaults-torrc " + torrcPath
                + " -f " + torrcPath + ".custom";
    
        debug(torCmdString);
        
        SimpleCommand shellTorCommand = new SimpleCommand(torCmdString + " --verify-config");
        shell.add(shellTorCommand).waitForFinish();
        
        int exitCode = shellTorCommand.getExitCode();
        String output = shellTorCommand.getOutput();
        
        if (exitCode != 0 && output != null && output.length() > 0)
        {
            logNotice("Tor (" + exitCode + "): " + output);
            throw new Exception ("Torrc config did not verify");
            
        }

        shellTorCommand = new SimpleCommand(torCmdString);
        shell.add(shellTorCommand).waitForFinish();
        exitCode = shellTorCommand.getExitCode();
        output = shellTorCommand.getOutput();
        
        if (exitCode != 0 && output != null && output.length() > 0)
        {
            logNotice("Tor (" + exitCode + "): " + output);
            //throw new Exception ("unable to start");
            return false;
        }
        
        //now try to connect
        mLastProcessId = initControlConnection (100,false);

        if (mLastProcessId == -1)
        {
            logNotice(getString(R.string.couldn_t_start_tor_process_) + "; exit=" + shellTorCommand.getExitCode() + ": " + shellTorCommand.getOutput());
            sendCallbackLogMessage(getString(R.string.couldn_t_start_tor_process_));
        
            throw new Exception ("Unable to start Tor");
        }
        else
        {
        
            logNotice("Tor started; process id=" + mLastProcessId);
           
        }
        
        return true;
    }
    
    private void updatePolipoConfig () throws FileNotFoundException, IOException
    {
        

        File file = new File(OrbotApp.appBinHome, POLIPOCONFIG_ASSET_KEY);
        
        Properties props = new Properties();
        
        props.load(new FileReader(file));
        
        props.put("socksParentProxy", "\"localhost:" + mPortSOCKS + "\"");
        props.put("proxyPort",mPortHTTP+"");
        
        props.store(new FileWriter(file), "updated");
        
    }
    
    
    private void runPolipoShellCmd (Shell shell) throws Exception
    {
        
        logNotice( "Starting polipo process");
        
            int polipoProcId = TorServiceUtils.findProcessId(OrbotApp.filePolipo.getCanonicalPath());

            StringBuilder log = null;
            
            int attempts = 0;
            
            if (polipoProcId == -1)
            {
                log = new StringBuilder();
                
                updatePolipoConfig();
                
                String polipoConfigPath = new File(OrbotApp.appBinHome, POLIPOCONFIG_ASSET_KEY).getCanonicalPath();
                SimpleCommand cmdPolipo = new SimpleCommand(OrbotApp.filePolipo.getCanonicalPath() + " -c " + polipoConfigPath + " &");
                
                shell.add(cmdPolipo);
                
                //wait one second to make sure it has started up
                Thread.sleep(1000);
                
                while ((polipoProcId = TorServiceUtils.findProcessId(OrbotApp.filePolipo.getCanonicalPath())) == -1  && attempts < MAX_START_TRIES)
                {
                    logNotice("Couldn't find Polipo process... retrying...\n" + log);
                    Thread.sleep(3000);
                    attempts++;
                }
                
                logNotice(log.toString());
            }
            
            sendCallbackLogMessage(getString(R.string.privoxy_is_running_on_port_) + mPortHTTP);
            
            logNotice("Polipo process id=" + polipoProcId);
            
    }
    
    private int initControlConnection (int maxTries, boolean isReconnect) throws Exception, RuntimeException
    {
            int controlPort = -1;
            int attempt = 0;

            logNotice( "Waiting for control port...");
            
            while (conn == null && attempt++ < maxTries)
            {
                try
                {
                    
                    controlPort = getControlPort();
                    
                    if (controlPort != -1)
                    {
                        logNotice( "Connecting to control port: " + controlPort);
                        
                        torConnSocket = new Socket(IP_LOCALHOST, controlPort);
                        torConnSocket.setSoTimeout(CONTROL_SOCKET_TIMEOUT);
                        
                        conn = new TorControlConnection(torConnSocket);
                        conn.launchThread(true);//is daemon
                        
                        break;
                    }
                    
                }
                catch (Exception ce)
                {
                    conn = null;
                //    logException( "Error connecting to Tor local control port: " + ce.getMessage(),ce);
                    
                }
                
                
                try {
                //    logNotice("waiting...");
                    Thread.sleep(1000); }
                catch (Exception e){}
            }
            
            if (conn != null)
            {
                    logNotice( "SUCCESS connected to Tor control port.");
                    
                    File fileCookie = new File(OrbotApp.appCacheHome, TOR_CONTROL_COOKIE);
                    
                    if (fileCookie.exists())
                    {
                        byte[] cookie = new byte[(int)fileCookie.length()];
                        DataInputStream fis = new DataInputStream(new FileInputStream(fileCookie));
                        fis.read(cookie);
                        fis.close();
                        conn.authenticate(cookie);
                                
                        logNotice( "SUCCESS - authenticated to control port.");
                        
                        sendCallbackLogMessage(getString(R.string.tor_process_starting) + ' ' + getString(R.string.tor_process_complete));
    
                        addEventHandler();
                    
                        String torProcId = conn.getInfo("process/pid");
                        
                         String confSocks = conn.getInfo("net/listeners/socks");
                         StringTokenizer st = new StringTokenizer(confSocks," ");

                         confSocks = st.nextToken().split(":")[1];
                         confSocks = confSocks.substring(0,confSocks.length()-1);
                         mPortSOCKS = Integer.parseInt(confSocks);
                                                  
                        return Integer.parseInt(torProcId);
                        
                    }
                    else
                    {
                        logNotice ("Tor authentication cookie does not exist yet");
                        conn = null;
                                
                    }
                }
                
            
            return -1;

    }
    
    private int getControlPort ()
    {
        int result = -1;
        
        try
        {
            if (fileControlPort.exists())
            {
                debug("Reading control port config file: " + fileControlPort.getCanonicalPath());
                BufferedReader bufferedReader = new BufferedReader(new FileReader(fileControlPort));
                String line = bufferedReader.readLine();
                
                if (line != null)
                {
                    String[] lineParts = line.split(":");
                    result = Integer.parseInt(lineParts[1]);
                }
                

                bufferedReader.close();

                //store last valid control port
                SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
                prefs.edit().putInt("controlport", result).commit();
                
            }
            else
            {
                debug("Control Port config file does not yet exist (waiting for tor): " + fileControlPort.getCanonicalPath());
                
            }
            
            
        }
        catch (FileNotFoundException e)
        {    
            debug("unable to get control port; file not found");
        }
        catch (Exception e)
        {    
            debug("unable to read control port config file");
        }

        return result;
    }

    public void addEventHandler () throws Exception
    {
           // We extend NullEventHandler so that we don't need to provide empty
           // implementations for all the events we don't care about.
           // ...
        logNotice( "adding control port event handler");

        conn.setEventHandler(this);
        
        conn.setEvents(Arrays.asList(new String[]{
              "ORCONN", "CIRC", "NOTICE", "WARN", "ERR","BW"}));
          // conn.setEvents(Arrays.asList(new String[]{
            //  "DEBUG", "INFO", "NOTICE", "WARN", "ERR"}));

        logNotice( "SUCCESS added control port event handler");
    }
    
        /**
         * Returns the port number that the HTTP proxy is running on
         */
        public int getHTTPPort() throws RemoteException {
            return mPortHTTP;
        }

        
        /**
         * Returns the port number that the HTTP proxy is running on
         */
        public int getSOCKSPort() throws RemoteException {
            return mPortSOCKS;
        }
        
        public void enableVpnProxy () {
        	debug ("enabling VPN Proxy");
            
            Prefs.putUseVpn(true);
            processTransparentProxying();
            
            Intent intent = new Intent(TorService.this, OrbotVpnService.class);
            intent.setAction("start");
            
            intent.putExtra("torSocks", mPortSOCKS);
            
            if (!mIsLollipop)
            	intent.putExtra("proxyPort",OrbotVpnService.mSocksProxyPort);
            
            startService(intent);
           
        }
        
        public void refreshVpnProxy () {
            
        	debug ("refreshing VPN Proxy");
        	
        	try
        	{
	        	conn.setConf("DisableNetwork", "1");
	        	
	            Intent intent = new Intent(TorService.this, OrbotVpnService.class);
	            intent.setAction("refresh");
	            startService(intent);

	            //updateConfiguration("DNSPort",TOR_VPN_DNS_LISTEN_ADDRESS + ":" + TorServiceConstants.TOR_DNS_PORT_DEFAULT,false);
	            //updateConfiguration("DNSPort",TorServiceConstants.TOR_DNS_PORT_DEFAULT+"",false);
	            updateConfiguration("DisableNetwork","0", false);

	            saveConfiguration();
	        
	
	        	conn.setConf("DisableNetwork", "0");
        	}
        	catch (Exception ioe)
        	{
        		Log.e(TAG,"error restarting network",ioe);
        	}
        	
        }
        
        
        
        public void clearVpnProxy ()
        {   
        	debug ("clearing VPN Proxy");
            Prefs.putUseVpn(false);
            processTransparentProxying();
            
            Intent intent = new Intent(TorService.this, OrbotVpnService.class);
            intent.setAction("stop");
            startService(intent);                                              
        }

    @Override
    public void message(String severity, String msg) {
        logNotice(severity + ": " + msg);
    }

    @Override
    public void newDescriptors(List<String> orList) {
    }

    @Override
    public void orConnStatus(String status, String orName) {
        
            StringBuilder sb = new StringBuilder();
            sb.append("orConnStatus (");
            sb.append(parseNodeName(orName) );
            sb.append("): ");
            sb.append(status);
            
            debug(sb.toString());
    }

    @Override
    public void streamStatus(String status, String streamID, String target) {
        
            StringBuilder sb = new StringBuilder();
            sb.append("StreamStatus (");
            sb.append((streamID));
            sb.append("): ");
            sb.append(status);
            
            logNotice(sb.toString());
    }

    @Override
    public void unrecognized(String type, String msg) {
        
            StringBuilder sb = new StringBuilder();
            sb.append("Message (");
            sb.append(type);
            sb.append("): ");
            sb.append(msg);
            
            logNotice(sb.toString());
    }

    @Override
    public void bandwidthUsed(long read, long written) {
    
        if (read != lastRead || written != lastWritten)
        {
            StringBuilder sb = new StringBuilder();                
            sb.append(formatCount(read));
            sb.append(" \u2193");            
            sb.append(" / ");
            sb.append(formatCount(written));
            sb.append(" \u2191");            
               
            int iconId = R.drawable.ic_stat_tor;
            
            if (read > 0 || written > 0)
                iconId = R.drawable.ic_stat_tor_xfer;
            
            if (mConnectivity && Prefs.persistNotifications())
                  showToolbarNotification(sb.toString(), NOTIFY_ID, iconId);

            mTotalTrafficWritten += written;
            mTotalTrafficRead += read;
        }
        
        lastWritten = written;
        lastRead = read;
        
        sendCallbackBandwidth(lastWritten, lastRead, mTotalTrafficWritten, mTotalTrafficRead);
    }
    
    private String formatCount(long count) {
        // Converts the supplied argument into a string.
        // Under 2Mb, returns "xxx.xKb"
        // Over 2Mb, returns "xxx.xxMb"
 	if (mNumberFormat != null)
        	if (count < 1e6)
            		return mNumberFormat.format(Math.round((float)((int)(count*10/1024))/10)) + "Kbps";
        	else
            		return mNumberFormat.format(Math.round((float)((int)(count*100/1024/1024))/100)) + "Mbps";
	else
		return "";
        
           //return count+" kB";
    }
    
    public void circuitStatus(String status, String circID, String path) {
        
        /* once the first circuit is complete, then announce that Orbot is on*/
        if (mCurrentStatus == STATUS_STARTING && TextUtils.equals(status, "BUILT"))
            sendCallbackStatus(STATUS_ON);

        StringBuilder sb = new StringBuilder();
        sb.append("Circuit (");
        sb.append((circID));
        sb.append(") ");
        sb.append(status);
        sb.append(": ");
        
        StringTokenizer st = new StringTokenizer(path,",");
        Node node = null;
        
        while (st.hasMoreTokens())
        {
            String nodePath = st.nextToken();
            node = new Node();
            
            String[] nodeParts;
            
            if (nodePath.contains("="))
                nodeParts = nodePath.split("=");
            else
                nodeParts = nodePath.split("~");
            
            if (nodeParts.length == 1)
            {
                node.id = nodeParts[0].substring(1);
                node.name = node.id;
            }
            else if (nodeParts.length == 2)
            {
                node.id = nodeParts[0].substring(1);
                node.name = nodeParts[1];
            }
            
            node.status = status;
            
            sb.append(node.name);
            
            if (st.hasMoreTokens())
                sb.append (" > ");
        }
        
        if (Prefs.useDebugLogging())
            debug(sb.toString());
        else if(status.equals("BUILT"))
            logNotice(sb.toString());
        else if (status.equals("CLOSED"))
            logNotice(sb.toString());

        if (Prefs.expandedNotifications())
        {
            //get IP from last nodename
            if(status.equals("BUILT")){
                
                if (node.ipAddress == null)
                    mExecutor.execute(new ExternalIPFetcher(node));
                
                hmBuiltNodes.put(node.id, node);
            }
            
            if (status.equals("CLOSED"))
            {
                hmBuiltNodes.remove(node.id);
                
            }
        }
    
    }
    
    private HashMap<String,Node> hmBuiltNodes = new HashMap<String,Node>();
    
    class Node
    {
        String status;
        String id;
        String name;
        String ipAddress;
        String country;
        String organization;
    }
    
    private class ExternalIPFetcher implements Runnable {

        private Node mNode;
        private int MAX_ATTEMPTS = 3;
        private final static String ONIONOO_BASE_URL = "https://onionoo.torproject.org/details?fields=country_name,as_name,or_addresses&lookup=";
        
        public ExternalIPFetcher (Node node)
        {
            mNode = node;
        }
        
        public void run ()
        {
            
            for (int i = 0; i < MAX_ATTEMPTS; i++)
            {
                if (conn != null)
                {
                    try {

                    	URLConnection conn = null;
                    	
                        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8118));
                        conn = new URL(ONIONOO_BASE_URL + mNode.id).openConnection(proxy);
    
                        conn.setRequestProperty("Connection","Close");
                        conn.setConnectTimeout(60000);
                        conn.setReadTimeout(60000);
                        
                        InputStream is = conn.getInputStream();
                        
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    
                        // getting JSON string from URL
                        
                        StringBuffer json = new StringBuffer();
                        String line = null;
    
                        while ((line = reader.readLine())!=null)
                            json.append(line);
                        
                        JSONObject jsonNodeInfo = new org.json.JSONObject(json.toString());
                            
                        JSONArray jsonRelays = jsonNodeInfo.getJSONArray("relays");
                        
                        if (jsonRelays.length() > 0)
                        {
                            mNode.ipAddress = jsonRelays.getJSONObject(0).getJSONArray("or_addresses").getString(0).split(":")[0];
                            mNode.country = jsonRelays.getJSONObject(0).getString("country_name");
                            mNode.organization = jsonRelays.getJSONObject(0).getString("as_name");
                            
                            StringBuffer sbInfo = new StringBuffer();
                            sbInfo.append(mNode.ipAddress);
                             
                             if (mNode.country != null)
                                 sbInfo.append(' ').append(mNode.country);
                         
                             if (mNode.organization != null)
                                 sbInfo.append(" (").append(mNode.organization).append(')');
                         
                             logNotice(sbInfo.toString());
                            
                        }
                        
                        reader.close();
                        is.close();
                        
                        break;
                        
                    } catch (Exception e) {
                        
                        debug ("Error getting node details from onionoo: " + e.getMessage());
                        
                        
                    }
                }
            }
        }
        
        
    }
    
    private String parseNodeName(String node)
    {
        if (node.indexOf('=')!=-1)
        {
            return (node.substring(node.indexOf("=")+1));
        }
        else if (node.indexOf('~')!=-1)
        {
            return (node.substring(node.indexOf("~")+1));
        }
        else
            return node;
    }

        public void processTransparentProxying() {
            try{
                if (Prefs.useRoot())
                {
                     Shell shell = Shell.startRootShell();
                    if (Prefs.useTransparentProxying()){
                        enableTransparentProxy(shell);
                    } else {
                        disableTransparentProxy(shell);                        
                    }
                    shell.close();
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }

        public String getInfo (String key) {
            try {
                if(conn !=null){
                    String m = conn.getInfo(key);
                    return m;
                }
            }
            catch(Exception ioe){
            //    Log.e(TAG,"Unable to get Tor information",ioe);
                logNotice("Unable to get Tor information"+ioe.getMessage());
            }
            return null;
        }
        
        public String getConfiguration (String name)
        {
            try
            {
                if (conn != null)
                {
                    StringBuffer result = new StringBuffer();
                    
                    List<ConfigEntry> listCe = conn.getConf(name);
                    
                    Iterator<ConfigEntry> itCe = listCe.iterator();
                    ConfigEntry ce = null; 
                    
                       
                    
                    while (itCe.hasNext())
                    {
                        ce = itCe.next();
                        
                        result.append(ce.key);
                        result.append(' ');
                        result.append(ce.value);
                        result.append('\n');
                    }
                    
                          return result.toString();
                }
            }
            catch (Exception ioe)
            {
                
                logException("Unable to get Tor configuration: " + ioe.getMessage(),ioe);
            }
            
            return null;
        }
        
        private final static String RESET_STRING = "=\"\"";
        /**
         * Set configuration
         **/
        public boolean updateConfiguration (String name, String value, boolean saveToDisk)
        { 
            
            
            if (configBuffer == null)
                configBuffer = new ArrayList<String>();
            
            if (resetBuffer == null)
                resetBuffer = new ArrayList<String>();
            
            if (value == null || value.length() == 0)
            {
                resetBuffer.add(name + RESET_STRING);
                
            }
            else
            {
                StringBuffer sbConf = new StringBuffer();
                sbConf.append(name);
                sbConf.append(' ');
                sbConf.append(value);
                
                configBuffer.add(sbConf.toString());
            }
            
            return false;
        }
        
        public void setTorNetworkEnabled (final boolean isEnabled)
        {

        	
        	//it is possible to not have a connection yet, and someone might try to newnym
            if (conn != null)
            {
                new Thread ()
                {
                    public void run ()
                    {
                        try { 
                            
                            conn.setConf("DisableNetwork", isEnabled ? "0" : "1");
                        	
                        }
                        catch (Exception ioe){
                            debug("error requesting newnym: " + ioe.getLocalizedMessage());
                        }
                    }
                }.start();
            }
        	
        }
        
        public void newIdentity () 
        {
            //it is possible to not have a connection yet, and someone might try to newnym
            if (conn != null)
            {
                new Thread ()
                {
                    public void run ()
                    {
                        try { 
                            
                            conn.signal("NEWNYM"); 
                        
                        }
                        catch (Exception ioe){
                            debug("error requesting newnym: " + ioe.getLocalizedMessage());
                        }
                    }
                }.start();
            }
        }
        
        public boolean saveConfiguration ()
        {
            try
            {
                if (conn != null)
                {
                    
                     if (resetBuffer != null && resetBuffer.size() > 0)
                        {    
                         for (String value : configBuffer)
                             {
                                 
                              //   debug("removing torrc conf: " + value);
                                 
                                 
                             }
                         
                           // conn.resetConf(resetBuffer);
                            resetBuffer = null;
                        }
                  
                     if (configBuffer != null && configBuffer.size() > 0)
                        {
                             
                                 for (String value : configBuffer)
                                 {
                                     
                                     debug("Setting torrc conf: " + value);
                                     
                                     
                                 }
                                 
                             conn.setConf(configBuffer);
                                 
                            configBuffer = null;
                        }
                  
                          // Flush the configuration to disk.
                    //this is doing bad things right now NF 22/07/10
                          //conn.saveConf();
    
                          return true;
                }
            }
            catch (Exception ioe)
            {
                logException("Unable to update Tor configuration: " + ioe.getMessage(),ioe);
            }
            
            return false;
        }

    private void sendCallbackBandwidth(long upload, long download, long written, long read)    {
        Intent intent = new Intent(LOCAL_ACTION_BANDWIDTH);

        intent.putExtra("up",upload);
	      intent.putExtra("down",download);
	      intent.putExtra("written",written);
	      intent.putExtra("read",read);
	      intent.putExtra(EXTRA_STATUS, mCurrentStatus);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCallbackLogMessage (String logMessage)
    {
        
        Intent intent = new Intent(LOCAL_ACTION_LOG);
          // You can also include some extra data.
          intent.putExtra(LOCAL_EXTRA_LOG, logMessage);
	      intent.putExtra(EXTRA_STATUS, mCurrentStatus);

          LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }
    
    private void sendCallbackStatus(String currentStatus) {
        mCurrentStatus = currentStatus;
        Intent intent = getActionStatusIntent(currentStatus);
        // send for Orbot internals, using secure local broadcast
        sendBroadcastOnlyToOrbot(intent);
        // send for any apps that are interested
        sendBroadcast(intent);
    }

    /**
     * Send a secure broadcast only to Orbot itself
     * @see {@link ContextWrapper#sendBroadcast(Intent)}
     * @see {@link LocalBroadcastManager}
     */
    private boolean sendBroadcastOnlyToOrbot(Intent intent) {
        return LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private Intent getActionStatusIntent(String currentStatus) {
        Intent intent = new Intent(ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS, currentStatus);
        return intent;
    }

    /*
     *  Another way to do this would be to use the Observer pattern by defining the 
     *  BroadcastReciever in the Android manifest.
     */
    private final BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        	if (mCurrentStatus != STATUS_ON)
        		return;
        	
            SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

            boolean doNetworKSleep = prefs.getBoolean(OrbotConstants.PREF_DISABLE_NETWORK, true);
            
            final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = cm.getActiveNetworkInfo();

            boolean newConnectivityState = false;
            int newNetType = -1;
            
            boolean isChanged = false;
            
            if (netInfo!=null)
            	newNetType = netInfo.getType();

            isChanged = ((mNetworkType != newNetType)&&(mConnectivity != newConnectivityState));
            
            if(netInfo != null && netInfo.isConnected()) {
                // WE ARE CONNECTED: DO SOMETHING
            	newConnectivityState = true;
            }   
            else {
                // WE ARE NOT: DO SOMETHING ELSE
            	newConnectivityState = false;
            }
            
            mNetworkType = newNetType;
        	mConnectivity = newConnectivityState;
        	
            if (doNetworKSleep)
            {
	            setTorNetworkEnabled (mConnectivity);
	            
	            if (!mConnectivity)
                {
                    logNotice(context.getString(R.string.no_network_connectivity_putting_tor_to_sleep_));
                    showToolbarNotification(getString(R.string.no_internet_connection_tor),NOTIFY_ID,R.drawable.ic_stat_tor_off);
                    
                }
                else
                {
                    logNotice(context.getString(R.string.network_connectivity_is_good_waking_tor_up_));
                    showToolbarNotification(getString(R.string.status_activated),NOTIFY_ID,R.drawable.ic_stat_tor);
                }
	            
	            //is this a change in state?
	            if (isChanged)
	            {
	                try {
	                    
	                    if (mCurrentStatus != STATUS_OFF)
	                    {
	                        if (mConnectivity)
	                        {
	                            if (Prefs.useRoot() && Prefs.useTransparentProxying() && Prefs.transProxyNetworkRefresh())
	                            {
	                                
	                                Shell shell = Shell.startRootShell();
	                         
	                                disableTransparentProxy(shell);
	                                enableTransparentProxy(shell);
	                                
	                                shell.close();
	                            }
	                        //    else if (Prefs.useVpn()) //we need to turn on VPN here so the proxy is running
	                          //  	refreshVpnProxy();
	            	            
	                        }
	                    }
	                    
	                } catch (Exception e) {
	                    logException ("error updating state after network restart",e);
	                }
		            
	            }
            }

            
        }
    };

    private boolean processSettingsImpl (StringBuffer extraLines) throws IOException
    {
        logNotice(getString(R.string.updating_settings_in_tor_service));
        
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
        
        boolean useBridges = Prefs.bridgesEnabled();

        boolean becomeRelay = prefs.getBoolean(OrbotConstants.PREF_OR, false);
        boolean ReachableAddresses = prefs.getBoolean(OrbotConstants.PREF_REACHABLE_ADDRESSES,false);
        boolean enableHiddenServices = prefs.getBoolean("pref_hs_enable", false);

        boolean enableStrictNodes = prefs.getBoolean("pref_strict_nodes", false);
        String entranceNodes = prefs.getString("pref_entrance_nodes", "");
        String exitNodes = prefs.getString("pref_exit_nodes", "");
        String excludeNodes = prefs.getString("pref_exclude_nodes", "");
        
        if (!useBridges)
        {
           
            extraLines.append("UseBridges 0").append('\n');

	        if (Prefs.useVpn()) //set the proxy here if we aren't using a bridge
	        {
	        	
	        	if (!mIsLollipop)
	        	{
		        	String proxyType = "socks5";
		        	String proxyHost = "127.0.0.1";
		        	extraLines.append(proxyType + "Proxy" + ' ' + proxyHost + ':' + OrbotVpnService.mSocksProxyPort).append('\n');
	        	};
			
	        }
	        else
	        {
		        String proxyType = prefs.getString("pref_proxy_type", null);
		        if (proxyType != null && proxyType.length() > 0)
		        {
		            String proxyHost = prefs.getString("pref_proxy_host", null);
		            String proxyPort = prefs.getString("pref_proxy_port", null);
		            String proxyUser = prefs.getString("pref_proxy_username", null);
		            String proxyPass = prefs.getString("pref_proxy_password", null);
		            
		            if ((proxyHost != null && proxyHost.length()>0) && (proxyPort != null && proxyPort.length() > 0))
		            {
		            	extraLines.append(proxyType + "Proxy" + ' ' + proxyHost + ':' + proxyPort).append('\n');
		                
		                if (proxyUser != null && proxyPass != null)
		                {
		                    if (proxyType.equalsIgnoreCase("socks5"))
		                    {
		                    	extraLines.append("Socks5ProxyUsername" + ' ' + proxyUser).append('\n');
		                    	extraLines.append("Socks5ProxyPassword" + ' ' + proxyPass).append('\n');
		                    }
		                    else
		                    	extraLines.append(proxyType + "ProxyAuthenticator" + ' ' + proxyUser + ':' + proxyPort).append('\n');
		                    
		                }
		                else if (proxyPass != null)
		                	extraLines.append(proxyType + "ProxyAuthenticator" + ' ' + proxyUser + ':' + proxyPort).append('\n');
		                
		                
		
		            }
		        }
	        }
        }
        else
        {
        

            extraLines.append("UseBridges 1").append('\n');
            
            String bridgeList = new String(Prefs.getBridgesList().getBytes("ISO-8859-1"));
            
            if (bridgeList != null && bridgeList.length() > 1) //longer then 1 = some real values here
            {
	            
	            //check if any PT bridges are needed
	            boolean obfsBridges = bridgeList.contains("obfs3")||bridgeList.contains("obfs4")||bridgeList.contains("scramblesuit");
            
	            if (obfsBridges)
	            {
	                extraLines.append("ClientTransportPlugin obfs3 exec " + OrbotApp.fileObfsclient.getCanonicalPath()).append('\n');
	                extraLines.append("ClientTransportPlugin obfs4 exec " + OrbotApp.fileObfsclient.getCanonicalPath()).append('\n');
	                extraLines.append("ClientTransportPlugin scramblesuit exec " + OrbotApp.fileObfsclient.getCanonicalPath()).append('\n');
	            }
	            
	            String[] bridgeListLines = bridgeList.split("\\r?\\n");

	            for (String bridgeConfigLine : bridgeListLines)
	            {
	                if (!TextUtils.isEmpty(bridgeConfigLine))
	                {
	                	extraLines.append("Bridge ");
	                	
	                	//bridgeConfigLine = bridgeConfigLine.replace('�', ' ');
	                	
	                	StringTokenizer st = new StringTokenizer (bridgeConfigLine," ");
	                	while (st.hasMoreTokens())
	                		extraLines.append(st.nextToken()).append(' ');
	                	
	                	extraLines.append("\n");
	                	
	                }
	
	            }
	            
            }
            else
            {
            	//time to do autobridges, aka meek

            	debug ("Using meek bridges");
                
            	String bridgeConfig = "meek exec " + OrbotApp.fileMeekclient.getCanonicalPath();
            	extraLines.append("ClientTransportPlugin" + ' ' + bridgeConfig).append('\n');
            	
            	String[] meekBridge = 
            		{
            			"meek 0.0.2.0:1 url=https://meek-reflect.appspot.com/ front=www.google.com",
            			"meek 0.0.2.0:2 url=https://d2zfqthxsdq309.cloudfront.net/ front=a0.awsstatic.com",
            			"meek 0.0.2.0:3 url=https://az668014.vo.msecnd.net/ front=ajax.aspnetcdn.com"
            		};

            	int meekIdx = 2; //let's use Azure by default
            	
            	if (bridgeList != null && bridgeList.length() == 1)
            	{
            	  try 
            	  {
            		  meekIdx = Integer.parseInt(bridgeList);
            		  
            		  if (meekIdx+1 > meekBridge.length)
            			  throw new Exception("not valid meek idx");
            	  }
            	  catch (Exception e)
            	  {
            		  debug("invalid meek type; please enter 0=Google, 1=AWS, 2=Azure");
            	  }
            	}
            	
            	extraLines.append("Bridge " + meekBridge[meekIdx]).append('\n');            	
            	
            }
 
        }
        
                  
        //only apply GeoIP if you need it
        File fileGeoIP = new File(OrbotApp.appBinHome, GEOIP_ASSET_KEY);
        File fileGeoIP6 = new File(OrbotApp.appBinHome, GEOIP6_ASSET_KEY);
            
        if (fileGeoIP.exists())
        {
	        extraLines.append("GeoIPFile" + ' ' + fileGeoIP.getCanonicalPath()).append('\n');
	        extraLines.append("GeoIPv6File" + ' ' + fileGeoIP6.getCanonicalPath()).append('\n');
        }
        
        if (!TextUtils.isEmpty(entranceNodes))
        	extraLines.append("EntryNodes" + ' ' + entranceNodes).append('\n');
       
        if (!TextUtils.isEmpty(exitNodes))
        	extraLines.append("ExitNodes" + ' ' + exitNodes).append('\n');
        
        if (!TextUtils.isEmpty(excludeNodes))
        	extraLines.append("ExcludeNodes" + ' ' + excludeNodes).append('\n');
        
        extraLines.append("StrictNodes" + ' ' + (enableStrictNodes ? "1" : "0")).append('\n');

        try
        {
            if (ReachableAddresses)
            {
                String ReachableAddressesPorts =
                    prefs.getString(OrbotConstants.PREF_REACHABLE_ADDRESSES_PORTS, "*:80,*:443");
                
                extraLines.append("ReachableAddresses" + ' ' + ReachableAddressesPorts).append('\n');

            }
            
        }
        catch (Exception e)
        {
           showToolbarNotification (getString(R.string.your_reachableaddresses_settings_caused_an_exception_),ERROR_NOTIFY_ID,R.drawable.ic_stat_notifyerr);

           return false;
        }

        try
        {
            if (becomeRelay && (!useBridges) && (!ReachableAddresses))
            {
                int ORPort =  Integer.parseInt(prefs.getString(OrbotConstants.PREF_OR_PORT, "9001"));
                String nickname = prefs.getString(OrbotConstants.PREF_OR_NICKNAME, "Orbot");

                String dnsFile = writeDNSFile ();
                
                extraLines.append("ServerDNSResolvConfFile" + ' ' + dnsFile).append('\n');
                extraLines.append("ORPort" + ' ' + ORPort).append('\n');
                extraLines.append("Nickname" + ' ' + nickname).append('\n');
                extraLines.append("ExitPolicy" + ' ' + "reject *:*").append('\n');

            }
        }
        catch (Exception e)
        {
             showToolbarNotification (getString(R.string.your_relay_settings_caused_an_exception_),ERROR_NOTIFY_ID,R.drawable.ic_stat_notifyerr);

          
            return false;
        }

        if (enableHiddenServices)
        {
            logNotice("hidden services are enabled");
            
            //updateConfiguration("RendPostPeriod", "600 seconds", false); //possible feature to investigate
            
            String hsPorts = prefs.getString("pref_hs_ports","");
            
            StringTokenizer st = new StringTokenizer (hsPorts,",");
            String hsPortConfig = null;
            int hsPort = -1;
            
            while (st.hasMoreTokens())
            {
                try
                {
                    hsPortConfig = st.nextToken().trim();
                    
                    if (hsPortConfig.indexOf(":")==-1) //setup the port to localhost if not specifed
                    {
                        hsPortConfig = hsPortConfig + " 127.0.0.1:" + hsPortConfig;
                    }
                    
                    hsPort = Integer.parseInt(hsPortConfig.split(" ")[0]);

                    String hsDirPath = new File(OrbotApp.appCacheHome,"hs" + hsPort).getCanonicalPath();
                    
                    debug("Adding hidden service on port: " + hsPortConfig);
                    
                    extraLines.append("HiddenServiceDir" + ' ' + hsDirPath).append('\n');
                    extraLines.append("HiddenServicePort" + ' ' + hsPortConfig).append('\n');
                    

                } catch (NumberFormatException e) {
                    Log.e(TAG,"error parsing hsport",e);
                } catch (Exception e) {
                    Log.e(TAG,"error starting share server",e);
                }
            }
            
            
        }
        
        return true;
    }
    
    public static String flattenToAscii(String string) {
        char[] out = new char[string.length()];
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        int j = 0;
        for (int i = 0, n = string.length(); i < n; ++i) {
            char c = string.charAt(i);
            if (c <= '\u007F') out[j++] = c;
        }
        return new String(out);
    }

    //using Google DNS for now as the public DNS server
    private String writeDNSFile () throws IOException
    {
        File file = new File(OrbotApp.appBinHome, "resolv.conf");

        PrintWriter bw = new PrintWriter(new FileWriter(file));
        bw.println("nameserver 8.8.8.8");
        bw.println("nameserver 8.8.4.4");
        bw.close();
    
        return file.getCanonicalPath();
    }

    @SuppressLint("NewApi")
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
        switch (level)
        {
        
            case TRIM_MEMORY_BACKGROUND:
                debug("trim memory requested: app in the background");
            return;
            
        /**
        public static final int TRIM_MEMORY_BACKGROUND
        Added in API level 14
        Level for onTrimMemory(int): the process has gone on to the LRU list. This is a good opportunity to clean up resources that can efficiently and quickly be re-built if the user returns to the app.
        Constant Value: 40 (0x00000028)
        */
        
            case TRIM_MEMORY_COMPLETE:

                debug("trim memory requested: cleanup all memory");
            return;
        /**
        public static final int TRIM_MEMORY_COMPLETE
        Added in API level 14
        Level for onTrimMemory(int): the process is nearing the end of the background LRU list, and if more memory isn't found soon it will be killed.
        Constant Value: 80 (0x00000050)
        */
            case TRIM_MEMORY_MODERATE:

                debug("trim memory requested: clean up some memory");
            return;
                
        /**
        public static final int TRIM_MEMORY_MODERATE
        Added in API level 14
        Level for onTrimMemory(int): the process is around the middle of the background LRU list; freeing memory can help the system keep other processes running later in the list for better overall performance.
        Constant Value: 60 (0x0000003c)
        */
        
            case TRIM_MEMORY_RUNNING_CRITICAL:

                debug("trim memory requested: memory on device is very low and critical");
            return;
        /**
        public static final int TRIM_MEMORY_RUNNING_CRITICAL
        Added in API level 16
        Level for onTrimMemory(int): the process is not an expendable background process, but the device is running extremely low on memory and is about to not be able to keep any background processes running. Your running process should free up as many non-critical resources as it can to allow that memory to be used elsewhere. The next thing that will happen after this is onLowMemory() called to report that nothing at all can be kept in the background, a situation that can start to notably impact the user.
        Constant Value: 15 (0x0000000f)
        */
        
            case TRIM_MEMORY_RUNNING_LOW:

                debug("trim memory requested: memory on device is running low");
            return;
        /**
        public static final int TRIM_MEMORY_RUNNING_LOW
        Added in API level 16
        Level for onTrimMemory(int): the process is not an expendable background process, but the device is running low on memory. Your running process should free up unneeded resources to allow that memory to be used elsewhere.
        Constant Value: 10 (0x0000000a)
        */
            case TRIM_MEMORY_RUNNING_MODERATE:

                debug("trim memory requested: memory on device is moderate");
            return;
        /**
        public static final int TRIM_MEMORY_RUNNING_MODERATE
        Added in API level 16
        Level for onTrimMemory(int): the process is not an expendable background process, but the device is running moderately low on memory. Your running process may want to release some unneeded resources for use elsewhere.
        Constant Value: 5 (0x00000005)
        */
            case TRIM_MEMORY_UI_HIDDEN:

                debug("trim memory requested: app is not showing UI anymore");
            return;
                
        /**
        public static final int TRIM_MEMORY_UI_HIDDEN
        Level for onTrimMemory(int): the process had been showing a user interface, and is no longer doing so. Large allocations with the UI should be released at this point to allow memory to be better managed.
        Constant Value: 20 (0x00000014)
        */
        }
        
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private void setExitNode (String newExits)
    {
    	SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
        
    	if (TextUtils.isEmpty(newExits))
    	{
    		prefs.edit().remove("pref_exit_nodes").apply();
    		
    		if (conn != null)
        	{
    	    	try
    	    	{
    	    		ArrayList<String> resetBuffer = new ArrayList<String>();
    	    		resetBuffer.add("ExitNodes");
    	    		resetBuffer.add("StrictNodes");
    	    		conn.resetConf(resetBuffer);
    	    		conn.setConf("DisableNetwork","1");
    	    		conn.setConf("DisableNetwork","0");
    	    		
    	    	}
    	    	catch (IOException ioe)
    	    	{
    	    		ioe.printStackTrace();
    	    	}
        	}
    	}
    	else
    	{
    		prefs.edit().putString("pref_exit_nodes", newExits).apply();
    		
    		if (conn != null)
        	{
    	    	try
    	    	{
                    File fileGeoIP = new File(OrbotApp.appBinHome, GEOIP_ASSET_KEY);
                    File fileGeoIP6 = new File(OrbotApp.appBinHome, GEOIP6_ASSET_KEY);
                        
                    conn.setConf("GeoIPFile",fileGeoIP.getCanonicalPath());
                    conn.setConf("GeoIPv6File",fileGeoIP6.getCanonicalPath());
                    
    	    		conn.setConf("ExitNodes", newExits);
    	    		conn.setConf("StrictNodes","1");
    	    		
    	    		conn.setConf("DisableNetwork","1");
    	    		conn.setConf("DisableNetwork","0");
    	    		

    	    	}
    	    	catch (IOException ioe)
    	    	{
    	    		ioe.printStackTrace();
    	    	}
        	}
    	}

    }

}
