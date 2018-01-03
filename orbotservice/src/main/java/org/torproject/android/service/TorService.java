/* Copyright (c) 2009-2011, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info/apps/orbot */
/* See LICENSE for licensing information */
/*
 * Code for iptables binary management taken from DroidWall GPLv3
 * Copyright (C) 2009-2010  Rodrigo Zechin Rosauro
 */

package org.torproject.android.service;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import org.torproject.android.binary.TorResourceInstaller;
import org.torproject.android.control.ConfigEntry;
import org.torproject.android.control.TorControlConnection;
import org.torproject.android.service.util.OtherResourceInstaller;
import org.torproject.android.service.vpn.TorifiedApp;
import org.torproject.android.service.util.DummyActivity;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.service.util.TorServiceUtils;
import org.torproject.android.service.util.Utils;
import org.torproject.android.service.vpn.OrbotVpnManager;
import org.torproject.android.service.vpn.TorVpnService;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.torproject.android.binary.TorServiceConstants.BINARY_TOR_VERSION;

public class TorService extends Service implements TorServiceConstants, OrbotConstants
{
    
    private String mCurrentStatus = STATUS_OFF;
    
    private final static int CONTROL_SOCKET_TIMEOUT = 0;
        
    private TorControlConnection conn = null;
    private Socket torConnSocket = null;
    private int mLastProcessId = -1;

    private int mPortHTTP = HTTP_PROXY_PORT_DEFAULT;
    private int mPortSOCKS = SOCKS_PROXY_PORT_DEFAULT;
    
    private static final int NOTIFY_ID = 1;
    private static final int ERROR_NOTIFY_ID = 3;
    private static final int HS_NOTIFY_ID = 4;

    private ArrayList<String> configBuffer = null;
    private ArrayList<String> resetBuffer = null;

    private boolean isTorUpgradeAndConfigComplete = false;

    private File fileControlPort;

    private boolean mConnectivity = true;
    private int mNetworkType = -1;

    private NotificationManager mNotificationManager = null;
    private NotificationCompat.Builder mNotifyBuilder;
    private Notification mNotification;
    private boolean mNotificationShowing = false;

	boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    private ExecutorService mExecutor = Executors.newFixedThreadPool(3);

    TorEventHandler mEventHandler;

    public static File appBinHome;
    public static File appCacheHome;

    public static File fileTor;
    public static File filePolipo;
    public static File fileObfsclient;
    public static File fileTorRc;
    private File mHSBasePath;

    private ArrayList<Bridge> alBridges = null;

    private static final Uri HS_CONTENT_URI = Uri.parse("content://org.torproject.android.ui.hiddenservices.providers/hs");
    private static final Uri COOKIE_CONTENT_URI = Uri.parse("content://org.torproject.android.ui.hiddenservices.providers.cookie/cookie");

    public static final class HiddenService implements BaseColumns {
        public static final String NAME = "name";
        public static final String PORT = "port";
        public static final String ONION_PORT = "onion_port";
        public static final String DOMAIN = "domain";
        public static final String AUTH_COOKIE = "auth_cookie";
        public static final String AUTH_COOKIE_VALUE = "auth_cookie_value";
        public static final String CREATED_BY_USER = "created_by_user";
        public static final String ENABLED = "enabled";

        private HiddenService() {
        }
    }

    public static final class ClientCookie implements BaseColumns {
        public static final String DOMAIN = "domain";
        public static final String AUTH_COOKIE_VALUE = "auth_cookie_value";
        public static final String ENABLED = "enabled";

        private ClientCookie() {
        }
    }

    private String[] hsProjection = new String[]{
			HiddenService._ID,
			HiddenService.NAME,
			HiddenService.DOMAIN,
			HiddenService.PORT,
			HiddenService.AUTH_COOKIE,
			HiddenService.AUTH_COOKIE_VALUE,
			HiddenService.ONION_PORT,
            HiddenService.ENABLED};

    private String[] cookieProjection = new String[]{
            ClientCookie._ID,
            ClientCookie.DOMAIN,
            ClientCookie.AUTH_COOKIE_VALUE,
            ClientCookie.ENABLED};

    public void debug(String msg)
    {
        if (Prefs.useDebugLogging())
        {
            Log.d(OrbotConstants.TAG,msg);
            sendCallbackLogMessage(msg);    

        }
    }
    
    public void logException(String msg, Exception e)
    {
        if (Prefs.useDebugLogging())
        {
            Log.e(OrbotConstants.TAG,msg,e);
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
                showToolbarNotification(getString(R.string.status_activated),NOTIFY_ID,R.drawable.ic_stat_tor);

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
        
        if (mEventHandler != null)
            mEventHandler.getNodes().clear();

        mNotificationShowing = false;
    }
	        
    @SuppressLint("NewApi")
    protected void showToolbarNotification (String notifyMsg, int notifyType, int icon)
     {        
         
         //Reusable code.
         PackageManager pm = getPackageManager();
         Intent intent = pm.getLaunchIntentForPackage(getPackageName());
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
        }
        else
        {
            mNotifyBuilder.setTicker(null);
        }
        
        mNotifyBuilder.setOngoing(Prefs.persistNotifications());

         if (!Prefs.persistNotifications())
            mNotifyBuilder.setPriority(Notification.PRIORITY_LOW);

         mNotifyBuilder.setCategory(Notification.CATEGORY_SERVICE);

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

             if (mEventHandler != null && mEventHandler.getNodes().size() > 0)
             {
                 Set<String> itBuiltNodes = mEventHandler.getNodes().keySet();
                 for (String key : itBuiltNodes)
                 {
                     TorEventHandler.Node node = mEventHandler.getNodes().get(key);
                     
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

            Intent intentRefresh = new Intent();
            intentRefresh.setAction(CMD_NEWNYM);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intentRefresh, PendingIntent.FLAG_UPDATE_CURRENT);
            expandedView.setOnClickPendingIntent(R.id.action_refresh,pendingIntent);
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
            exec (new IncomingIntentRouter(intent));
        else
            Log.d(OrbotConstants.TAG, "Got null onStartCommand() intent");

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

            while (!isTorUpgradeAndConfigComplete)
            {
                try { Thread.sleep (500);}
                catch (Exception e){}
            }

            String action = mIntent.getAction();

            if (action != null) {
                if (action.equals(ACTION_START)) {
                    replyWithStatus(mIntent);
                    startTor();
                }
                else if (action.equals(ACTION_STATUS)) {
                    replyWithStatus(mIntent);                    
                }
                else if (action.equals(CMD_SIGNAL_HUP)) {
                    requestTorRereadConfig();
                } else if (action.equals(CMD_NEWNYM)) {
                    newIdentity();
                } else if (action.equals(CMD_VPN)) {
                    startVPNService();
                } else if (action.equals(CMD_VPN_CLEAR)) {
                    clearVpnProxy();
                } else if (action.equals(CMD_SET_EXIT)) {
                	
                	setExitNode(mIntent.getStringExtra("exit"));
                	
                } else {
                    Log.w(OrbotConstants.TAG, "unhandled TorService Intent: " + action);
                }
            }
        }
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent){
         Log.d(OrbotConstants.TAG,"task removed");
         Intent intent = new Intent( this, DummyActivity.class );
         intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
         startActivity( intent );
    }

    @Override
    public void onDestroy() {

        try {
            unregisterReceiver(mNetworkStateReceiver);
            unregisterReceiver(mActionBroadcastReceiver);
        }
        catch (IllegalArgumentException iae)
        {
            //not registered yet
        }

        stopTor();

        super.onDestroy();
    }

    private void stopTor ()
    {
        new Thread(new Runnable ()
        {
            public void run ()
            {
                stopTorAsync();
            }
        }).start();

    }

    private void stopTorAsync () {
        Log.i("TorService", "stopTor");
        try {
            sendCallbackStatus(STATUS_STOPPING);
            sendCallbackLogMessage(getString(R.string.status_shutting_down));

            killAllDaemons();

            //stop the foreground priority and make sure to remove the persistant notification
            stopForeground(true);

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


    private void killAllDaemons() throws Exception {
        if (conn != null) {
            logNotice("Using control port to shutdown Tor");

            try {
                logNotice("sending HALT signal to Tor process");
                conn.shutdownTor("HALT");

            } catch (IOException e) {
                Log.d(OrbotConstants.TAG, "error shutting down Tor via connection", e);
            }

            conn = null;
        }

    }

    private void requestTorRereadConfig() {
        try {
            if (conn != null)
                conn.signal("HUP");

            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // if that fails, try again using native utils
        try {
        	TorServiceUtils.killProcess(fileTor, "-1"); // this is -HUP
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    protected void logNotice (String msg)
    {
        if (msg != null && msg.trim().length() > 0)
        {
            if (Prefs.useDebugLogging())
                Log.d(OrbotConstants.TAG, msg);
        
            sendCallbackLogMessage(msg);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        try
        {
            appBinHome = getDir(TorServiceConstants.DIRECTORY_TOR_BINARY, Application.MODE_PRIVATE);
            appCacheHome = getDir(TorServiceConstants.DIRECTORY_TOR_DATA,Application.MODE_PRIVATE);

            //mShell = Shell.startShell();

            fileTor= new File(appBinHome, TorServiceConstants.TOR_ASSET_KEY);
            filePolipo = new File(appBinHome, TorServiceConstants.POLIPO_ASSET_KEY);
            fileObfsclient = new File(appBinHome, TorServiceConstants.OBFSCLIENT_ASSET_KEY);
            fileTorRc = new File(appBinHome, TorServiceConstants.TORRC_ASSET_KEY);

            mHSBasePath = new File(
                    getFilesDir().getAbsolutePath(),
                    TorServiceConstants.HIDDEN_SERVICES_DIR
            );

            if (!mHSBasePath.isDirectory())
                mHSBasePath.mkdirs();

            mEventHandler = new TorEventHandler(this);

            if (mNotificationManager == null)
            {
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }

            IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(mNetworkStateReceiver , mNetworkStateFilter);

            IntentFilter filter = new IntentFilter();
            filter.addAction(CMD_NEWNYM);
            mActionBroadcastReceiver = new ActionBroadcastReceiver();
            registerReceiver(mActionBroadcastReceiver, filter);

            new Thread(new Runnable ()
            {
                public void run ()
                {
                    try
                    {
                        
                        torUpgradeAndConfig();
                    
                        findExistingTorDaemon();
                    }
                    catch (Exception e)
                    {
                        Log.e(OrbotConstants.TAG,"error onBind",e);
                        logNotice("error finding exiting process: " + e.toString());
                    }
                    
                }
            }).start();
        	
        }
        catch (Exception e)
        {
            //what error here
            Log.e(OrbotConstants.TAG, "Error installing Orbot binaries",e);
            logNotice("There was an error installing Orbot binaries");
        }
        
        Log.i("TorService", "onCreate end");
    }

    protected String getCurrentStatus ()
    {
        return mCurrentStatus;
    }

    private void torUpgradeAndConfig() throws IOException, TimeoutException {
        if (isTorUpgradeAndConfigComplete)
            return;

        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
        String version = prefs.getString(PREF_BINARY_TOR_VERSION_INSTALLED,null);

        logNotice("checking binary version: " + version);
        
        TorResourceInstaller installer = new TorResourceInstaller(this, appBinHome);
        
        if (version == null || (!version.equals(BINARY_TOR_VERSION)) || (!fileTor.exists()))
        {
            logNotice("upgrading binaries to latest version: " + BINARY_TOR_VERSION);
            
            boolean success = installer.installResources();
            
            if (success)
                prefs.edit().putString(PREF_BINARY_TOR_VERSION_INSTALLED,BINARY_TOR_VERSION).commit();


            OtherResourceInstaller oInstaller = new OtherResourceInstaller(this, appBinHome);
            oInstaller.installResources();
        }


        updateTorConfigFile ();
        isTorUpgradeAndConfigComplete = true;
    }

    private boolean updateTorConfigFile () throws IOException, TimeoutException
    {
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        TorResourceInstaller installer = new TorResourceInstaller(this, appBinHome);
        
        StringBuffer extraLines = new StringBuffer();
        
        String TORRC_CONTROLPORT_FILE_KEY = "ControlPortWriteToFile";
        fileControlPort = new File(appBinHome, "control.txt");
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

        String isolate = " ";
        if(prefs.getBoolean(OrbotConstants.PREF_ISOLATE_DEST, false))
        {
            isolate += "IsolateDestAddr";
        }
        
        extraLines.append("SOCKSPort ").append(socksPortPref).append(isolate).append('\n');
        extraLines.append("SafeSocks 0").append('\n');
        extraLines.append("TestSocks 0").append('\n');
        extraLines.append("WarnUnsafeSocks 1").append('\n');

	if(prefs.getBoolean(OrbotConstants.PREF_CONNECTION_PADDING, false))
	{
		extraLines.append("ConnectionPadding 1").append('\n');
	}
	if(prefs.getBoolean(OrbotConstants.PREF_REDUCED_CONNECTION_PADDING, true))
	{
		extraLines.append("ReducedConnectionPadding 1").append('\n');
	}

        String transPort = prefs.getString("pref_transport", TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT+"");
        String dnsPort = prefs.getString("pref_dnsport", TorServiceConstants.TOR_DNS_PORT_DEFAULT+"");
            
        extraLines.append("TransPort ").append(transPort).append('\n');
    	extraLines.append("DNSPort ").append(dnsPort).append("\n");
    	
    	if (Prefs.useVpn())
    		extraLines.append("DNSListenAddress 0.0.0.0").append('\n');

        extraLines.append("VirtualAddrNetwork 10.192.0.0/10").append('\n');
        extraLines.append("AutomapHostsOnResolve 1").append('\n');

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
        
        File fileTorRcCustom = new File(fileTorRc.getAbsolutePath() + ".custom");
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
     * the app that sent the initial request. If the user has disabled auto-
     * starts, the reply {@code ACTION_START Intent} will include the extra
     * {@link TorServiceConstants#STATUS_STARTS_DISABLED}
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
        } else if (mCurrentStatus == STATUS_ON) {
        
            sendCallbackLogMessage("Ignoring start request, already started.");
            setTorNetworkEnabled (true);

            return;
        }        
        

        try {
        	
	        // make sure there are no stray daemons running
//	        killAllDaemons();
	
	        sendCallbackStatus(STATUS_STARTING);
            showToolbarNotification(getString(R.string.status_starting_up),NOTIFY_ID,R.drawable.ic_stat_tor);
	        sendCallbackLogMessage(getString(R.string.status_starting_up));
	        logNotice(getString(R.string.status_starting_up));
	        
	        ArrayList<String> customEnv = new ArrayList<String>();
	
	        if (Prefs.bridgesEnabled())
	        	if (Prefs.useVpn() && !mIsLollipop)
	        	{
	        		customEnv.add("TOR_PT_PROXY=socks5://" + OrbotVpnManager.sSocksProxyLocalhost + ":" + OrbotVpnManager.sSocksProxyServerPort);
	        	}

	        boolean success = runTorShellCmd();

            if (mPortHTTP != -1)
                runPolipoShellCmd();

            // Tor is running, update new .onion names at db
            ContentResolver mCR = getApplicationContext().getContentResolver();
            Cursor hidden_services = mCR.query(HS_CONTENT_URI, hsProjection, null, null, null);
            if(hidden_services != null) {
                try {
                    while (hidden_services.moveToNext()) {
                        String HSDomain = hidden_services.getString(hidden_services.getColumnIndex(HiddenService.DOMAIN));
                        Integer HSLocalPort = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.PORT));
                        Integer HSAuthCookie = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.AUTH_COOKIE));
                        String HSAuthCookieValue = hidden_services.getString(hidden_services.getColumnIndex(HiddenService.AUTH_COOKIE_VALUE));

                        // Update only new domains or restored from backup with auth cookie
                        if((HSDomain == null || HSDomain.length() < 1) || (HSAuthCookie == 1 && (HSAuthCookieValue == null || HSAuthCookieValue.length() < 1))) {
                            String hsDirPath = new File(mHSBasePath.getAbsolutePath(),"hs" + HSLocalPort).getCanonicalPath();
                            File file = new File(hsDirPath, "hostname");

                            if (file.exists())
                            {
                                ContentValues fields = new ContentValues();

                                try {
                                    String onionHostname = Utils.readString(new FileInputStream(file)).trim();
                                    if(HSAuthCookie == 1) {
                                        String[] aux = onionHostname.split(" ");
                                        onionHostname = aux[0];
                                        fields.put(HiddenService.AUTH_COOKIE_VALUE, aux[1]);
                                    }
                                    fields.put(HiddenService.DOMAIN, onionHostname);
                                    mCR.update(HS_CONTENT_URI, fields, "port=" + HSLocalPort , null);
                                } catch (FileNotFoundException e) {
                                    logException("unable to read onion hostname file",e);
                                    showToolbarNotification(getString(R.string.unable_to_read_hidden_service_name), HS_NOTIFY_ID, R.drawable.ic_stat_notifyerr);
                                }
                            }
                            else
                            {
                                showToolbarNotification(getString(R.string.unable_to_read_hidden_service_name), HS_NOTIFY_ID, R.drawable.ic_stat_notifyerr);

                            }
                        }
                    }

                } catch (NumberFormatException e) {
                    Log.e(OrbotConstants.TAG,"error parsing hsport",e);
                } catch (Exception e) {
                    Log.e(OrbotConstants.TAG,"error starting share server",e);
                }

                hidden_services.close();
            }

        } catch (Exception e) {
            logException("Unable to start Tor: " + e.toString(), e);
            showToolbarNotification(
                    getString(R.string.unable_to_start_tor) + ": " + e.getMessage(),
                    ERROR_NOTIFY_ID, R.drawable.ic_stat_notifyerr);
            //stopTor();
        }
    }



    private boolean runTorShellCmd() throws Exception
    {
        boolean result = true;

        String torrcPath = new File(appBinHome, TORRC_ASSET_KEY).getCanonicalPath();

        updateTorConfigFile();
        
        sendCallbackLogMessage(getString(R.string.status_starting_up));
        
        String torCmdString = fileTor.getCanonicalPath()
                + " DataDirectory " + appCacheHome.getCanonicalPath()
                + " --defaults-torrc " + torrcPath
                + " -f " + torrcPath + ".custom";
    
        debug(torCmdString);


        int exitCode = -1;

        try {
            exitCode = exec(torCmdString + " --verify-config", true);
        }
        catch (Exception e)
        {
            logNotice("Tor configuration did not verify: " + e.getMessage());
            return false;
        }


        try {
            exitCode = exec(torCmdString, true);
        }
        catch (Exception e)
        {
            logNotice("Tor was unable to start: " + e.getMessage());
            return false;
        }

        if (exitCode != 0)
        {
            logNotice("Tor did not start. Exit:" + exitCode);
            return false;
        }
        
        //now try to connect
        mLastProcessId = initControlConnection (100,false);

        if (mLastProcessId == -1)
        {
            logNotice(getString(R.string.couldn_t_start_tor_process_) + "; exit=" + exitCode);
            sendCallbackLogMessage(getString(R.string.couldn_t_start_tor_process_));
        
            throw new Exception ("Unable to start Tor");
        }
        else
        {
        
            logNotice("Tor started; process id=" + mLastProcessId);
           
        }
        
        return result;
    }


    protected void exec (Runnable runn)
    {
        mExecutor.execute(runn);
    }

    private int exec (String cmd, boolean wait) throws Exception
    {
        CommandResult shellResult = Shell.run(cmd);
        debug("CMD: " + cmd + "; SUCCESS=" + shellResult.isSuccessful());

        if (!shellResult.isSuccessful()) {
            throw new Exception("Error: " + shellResult.exitCode + " ERR=" + shellResult.getStderr() + " OUT=" + shellResult.getStdout());
        }

        /**
        SimpleCommand command = new SimpleCommand(cmd);
        mShell.add(command);
        if (wait)
            command.waitForFinish();
        return command.getExitCode();
         **/
        return shellResult.exitCode;
    }
    
    private void updatePolipoConfig () throws FileNotFoundException, IOException
    {
        

        File file = new File(appBinHome, POLIPOCONFIG_ASSET_KEY);
        
        Properties props = new Properties();
        
        props.load(new FileReader(file));
        
        props.put("socksParentProxy", "\"localhost:" + mPortSOCKS + "\"");
        props.put("proxyPort",mPortHTTP+"");
        
        props.store(new FileWriter(file), "updated");
        
    }
    
    
    private void runPolipoShellCmd () throws Exception
    {
        
        logNotice( "Starting polipo process");

        updatePolipoConfig();

        String polipoConfigPath = new File(appBinHome, POLIPOCONFIG_ASSET_KEY).getCanonicalPath();
        String cmd = (filePolipo.getCanonicalPath() + " -c " + polipoConfigPath);

        CommandResult shellResult = Shell.run(cmd);

        sendCallbackLogMessage(getString(R.string.privoxy_is_running_on_port_) + mPortHTTP);
            
        logNotice("Polipo is running");
            
    }

    protected TorControlConnection getControlConnection ()
    {
        return conn;
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
                    
                    File fileCookie = new File(appCacheHome, TOR_CONTROL_COOKIE);
                    
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

    public synchronized void addEventHandler () throws Exception
    {
           // We extend NullEventHandler so that we don't need to provide empty
           // implementations for all the events we don't care about.
           // ...
        logNotice( "adding control port event handler");

        conn.setEventHandler(mEventHandler);

        
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


                            int iconId = R.drawable.ic_stat_tor;

                            if (hasConnectivity() && Prefs.expandedNotifications())
                                showToolbarNotification(getString(R.string.newnym), getNotifyId(), iconId);

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

    protected void sendCallbackBandwidth(long upload, long download, long written, long read)    {
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
    
    protected void sendCallbackStatus(String currentStatus) {
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

        	if (mCurrentStatus == STATUS_OFF)
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
        	
            if (doNetworKSleep && mCurrentStatus != STATUS_OFF)
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
		        	extraLines.append(proxyType + "Proxy" + ' ' + OrbotVpnManager.sSocksProxyLocalhost + ':' + OrbotVpnManager.sSocksProxyServerPort).append('\n');
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

            loadBridgeDefaults ();

            extraLines.append("UseBridges 1").append('\n');
            
            String bridgeList = new String(Prefs.getBridgesList().getBytes("ISO-8859-1"));
            boolean obfsBridges = bridgeList.contains("obfs3")||bridgeList.contains("obfs4");
            boolean meekBridges = bridgeList.contains("meek");

            //check if any PT bridges are needed
            if (obfsBridges)
            {
                extraLines.append("ClientTransportPlugin obfs3 exec " + fileObfsclient.getCanonicalPath()).append('\n');
                extraLines.append("ClientTransportPlugin obfs4 exec " + fileObfsclient.getCanonicalPath()).append('\n');
            }

            if (meekBridges)
            {
                extraLines.append("ClientTransportPlugin meek_lite exec " + fileObfsclient.getCanonicalPath()).append('\n');
            }

            if (bridgeList != null && bridgeList.length() > 5) //longer then 1 = some real values here
            {
	            String[] bridgeListLines = bridgeList.split("\\r?\\n");

	            for (String bridgeConfigLine : bridgeListLines)
	            {
	                if (!TextUtils.isEmpty(bridgeConfigLine))
	                {
	                	extraLines.append("Bridge ");

	                	StringTokenizer st = new StringTokenizer (bridgeConfigLine," ");
	                	while (st.hasMoreTokens())
	                		extraLines.append(st.nextToken()).append(' ');
	                	
	                	extraLines.append("\n");
	                	
	                }
	
	            }
	            
            }
            else
            {

                String type = "obfs4";

                if (meekBridges)
                    type = "meek_lite";

                getBridges(type,extraLines);

            }
 
        }
        
                  
        //only apply GeoIP if you need it
        File fileGeoIP = new File(appBinHome, GEOIP_ASSET_KEY);
        File fileGeoIP6 = new File(appBinHome, GEOIP6_ASSET_KEY);
            
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

        ContentResolver mCR = getApplicationContext().getContentResolver();

        /* ---- Hidden Services ---- */
        Cursor hidden_services = mCR.query(HS_CONTENT_URI, hsProjection, HiddenService.ENABLED + "=1", null, null);
        if(hidden_services != null) {
            try {
                while (hidden_services.moveToNext()) {
                    String HSname = hidden_services.getString(hidden_services.getColumnIndex(HiddenService.NAME));
                    Integer HSLocalPort = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.PORT));
                    Integer HSOnionPort = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.ONION_PORT));
                    Integer HSAuthCookie = hidden_services.getInt(hidden_services.getColumnIndex(HiddenService.AUTH_COOKIE));
                    String hsDirPath = new File(mHSBasePath.getAbsolutePath(),"hs" + HSLocalPort).getCanonicalPath();

                    debug("Adding hidden service on port: " + HSLocalPort);

                    extraLines.append("HiddenServiceDir" + ' ' + hsDirPath).append('\n');
                    extraLines.append("HiddenServicePort" + ' ' + HSOnionPort + " 127.0.0.1:" + HSLocalPort).append('\n');

                    if(HSAuthCookie == 1)
                        extraLines.append("HiddenServiceAuthorizeClient stealth " + HSname).append('\n');
                }
            } catch (NumberFormatException e) {
                    Log.e(OrbotConstants.TAG,"error parsing hsport",e);
            } catch (Exception e) {
                    Log.e(OrbotConstants.TAG,"error starting share server",e);
            }

            hidden_services.close();
		}

        /* ---- Client Cookies ---- */
        Cursor client_cookies = mCR.query(COOKIE_CONTENT_URI, cookieProjection, ClientCookie.ENABLED + "=1", null, null);
        if(client_cookies != null) {
            try {
                while (client_cookies.moveToNext()) {
                    String domain = client_cookies.getString(client_cookies.getColumnIndex(ClientCookie.DOMAIN));
                    String cookie = client_cookies.getString(client_cookies.getColumnIndex(ClientCookie.AUTH_COOKIE_VALUE));
                    extraLines.append("HidServAuth" + ' ' + domain + ' ' + cookie).append('\n');
                }
            } catch (Exception e) {
                    Log.e(OrbotConstants.TAG,"error starting share server",e);
            }

            client_cookies.close();
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
        File file = new File(appBinHome, "resolv.conf");

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
    public IBinder onBind(Intent intent) {
        Log.e( "CustomNotificationService", "onBind" );
        handleIntent( intent );
        return null;
    }

    private void handleIntent( Intent intent ) {
        if( intent != null && intent.getAction() != null ) {
            Log.e( "CustomNotificationService", intent.getAction().toString() );
        }
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
    	    	catch (Exception ioe)
    	    	{
    	    		Log.e(OrbotConstants.TAG, "Connection exception occured resetting exits",ioe);
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
                    File fileGeoIP = new File(appBinHome, GEOIP_ASSET_KEY);
                    File fileGeoIP6 = new File(appBinHome, GEOIP6_ASSET_KEY);
                        
                    conn.setConf("GeoIPFile",fileGeoIP.getCanonicalPath());
                    conn.setConf("GeoIPv6File",fileGeoIP6.getCanonicalPath());
                    
    	    		conn.setConf("ExitNodes", newExits);
    	    		conn.setConf("StrictNodes","1");
    	    		
    	    		conn.setConf("DisableNetwork","1");
    	    		conn.setConf("DisableNetwork","0");

    	    	}
    	    	catch (Exception ioe)
    	    	{
    	    		Log.e(OrbotConstants.TAG, "Connection exception occured resetting exits",ioe);
    	    	}
        	}
    	}

    }

    public boolean hasConnectivity ()
    {
        return mConnectivity;
    }

    public int getNotifyId ()
    {
        return NOTIFY_ID;
    }

    private void startVPNService ()
    {
        Intent intentVpn = new Intent(this,TorVpnService.class);
        intentVpn.setAction("start");
        intentVpn.putExtra("torSocks",mPortSOCKS);
        startService(intentVpn);
    }


    @TargetApi(14)
    public void clearVpnProxy ()
    {
        debug ("clearing VPN Proxy");
        Prefs.putUseVpn(false);

        Intent intentVpn = new Intent(this,TorVpnService.class);
        intentVpn.setAction("stop");
        startService(intentVpn);

    }


    // for bridge loading from the assets default bridges.txt file
    class Bridge
    {
        String type;
        String config;
    }


    private void loadBridgeDefaults ()
    {
        if (alBridges == null)
        {
            alBridges = new ArrayList<Bridge>();

            try
            {
                BufferedReader in=
                        new BufferedReader(new InputStreamReader(getAssets().open("bridges.txt"), "UTF-8"));
                String str;

                while ((str=in.readLine()) != null) {

                    StringTokenizer st = new StringTokenizer (str," ");
                    Bridge b = new Bridge();
                    b.type = st.nextToken();

                    StringBuffer sbConfig = new StringBuffer();

                    while(st.hasMoreTokens())
                        sbConfig.append(st.nextToken()).append(' ');

                    b.config = sbConfig.toString().trim();

                    alBridges.add(b);

                }

                in.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    //we should randomly sort alBridges so we don't have the same bridge order each time
    Random bridgeSelectRandom = new Random(System.nanoTime());

    private void getBridges (String type, StringBuffer extraLines)
    {

        Collections.shuffle(alBridges, bridgeSelectRandom);

        //let's just pull up to 2 bridges from the defaults at time
        int maxBridges = 2;
        int bridgeCount = 0;

        //now go through the list to find the bridges we want
        for (Bridge b : alBridges)
        {
            if (b.type.equals(type))
            {
                extraLines.append("Bridge ");
                extraLines.append(b.type);
                extraLines.append(' ');
                extraLines.append(b.config);
                extraLines.append('\n');

                bridgeCount++;

                if (bridgeCount > maxBridges)
                    break;
            }
        }

    }

    ActionBroadcastReceiver mActionBroadcastReceiver;

    private class ActionBroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            switch (intent.getAction())
            {
                case CMD_NEWNYM:
                {
                    newIdentity();
                    break;
                }
            }
        }
    }

}
