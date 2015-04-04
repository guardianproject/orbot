/* Copyright (c) 2009-2011, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info/apps/orbot */
/* See LICENSE for licensing information */
/*
 * Code for iptables binary management taken from DroidWall GPLv3
 * Copyright (C) 2009-2010  Rodrigo Zechin Rosauro
 */

package org.torproject.android.service;


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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
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

import net.freehaven.tor.control.ConfigEntry;
import net.freehaven.tor.control.EventHandler;
import net.freehaven.tor.control.TorControlConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;
import org.torproject.android.OrbotMainActivity;
import org.torproject.android.R;
import org.torproject.android.OrbotConstants;
import org.torproject.android.settings.AppManager;
import org.torproject.android.settings.TorifiedApp;
import org.torproject.android.vpn.OrbotVpnService;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.util.Log;
import android.widget.RemoteViews;

public class TorService extends Service implements TorServiceConstants, OrbotConstants, EventHandler
{
    
    public static boolean ENABLE_DEBUG_LOG = true;
    
    private int mCurrentStatus = STATUS_OFF;
    
    private final static int CONTROL_SOCKET_TIMEOUT = 0;
        
    private TorControlConnection conn = null;
    private Socket torConnSocket = null;
    private int mLastProcessId = -1;
    

    private int mPortHTTP = 8118;
    private int mPortSOCKS = 9050;
    
    private int mVpnProxyPort = 9099;
    
    private static final int NOTIFY_ID = 1;
    private static final int TRANSPROXY_NOTIFY_ID = 2;
    private static final int ERROR_NOTIFY_ID = 3;
    private static final int HS_NOTIFY_ID = 4;
    
    private boolean prefPersistNotifications = true;
    
    private static final int MAX_START_TRIES = 3;

    private ArrayList<String> configBuffer = null;
    private ArrayList<String> resetBuffer = null;
    
   //   private String appHome;
    private File appBinHome;
    private File appCacheHome;
    
    private File fileTor;
    private File filePolipo;
    private File fileObfsclient;
    private File fileMeekclient;
    private File fileXtables;
    
    private File fileTorRc;
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
    private boolean mShowExpandedNotifications = false;
    private boolean mNotificationShowing = false;
    
    private boolean mHasRoot = false;
    private boolean mEnableTransparentProxy = false;
    private boolean mTransProxyAll = false;
    private boolean mTransProxyTethering = false;
    private boolean mTransProxyNetworkRefresh = false;
    
    private boolean mUseVPN = false;
	boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    
    private ExecutorService mExecutor = Executors.newFixedThreadPool(1);

    private NumberFormat mNumberFormat = null;

    public void debug(String msg)
    {
        if (ENABLE_DEBUG_LOG)  
        {
            Log.d(TAG,msg);
            sendCallbackLogMessage(msg);    

        }
    }
    
    public void logException(String msg, Exception e)
    {
        if (ENABLE_DEBUG_LOG)
        {
            Log.e(TAG,msg,e);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            
            sendCallbackLogMessage(msg + '\n'+ new String(baos.toByteArray()));
            
        }
        else
            sendCallbackLogMessage(msg);
            

    }
    
    
    private boolean findExistingProc () 
    {
        if (fileTor != null)
        {
            try
            {
    
                mLastProcessId = initControlConnection(3,true);
                
                 if (mLastProcessId != -1 && conn != null)
                 {
                    sendCallbackLogMessage (getString(R.string.found_existing_tor_process));
                    
                    mCurrentStatus = STATUS_ON;
                    sendCallbackStatus(mCurrentStatus);
                    
                    return true;
                 }
                 
                 
                 return false;
            }
            catch (Exception e)
            {
                //Log.e(TAG,"error finding proc",e);
                return false;
            }
        }
        else
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


    public int getTorStatus ()
    {
        
        return mCurrentStatus;
        
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
        
        mNotifyBuilder.setOngoing(prefPersistNotifications);
        
        mNotification = mNotifyBuilder.build();
        
        if (Build.VERSION.SDK_INT >= 16 && mShowExpandedNotifications) {
            
            
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
                 //sbInfo.append(getString(R.string.your_tor_public_ips_) + '\n ');
                 
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
        
        if (prefPersistNotifications && (!mNotificationShowing))
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

        new Thread (new TorStarter(intent)).start();
        
        return START_REDELIVER_INTENT;

    }
    
    private class TorStarter implements Runnable
    {
        Intent mIntent;
        
        public TorStarter (Intent intent)
        {
            mIntent = intent;
        }
        
        public void run (){
            try{
                //if this is a start on boot launch turn tor on
                if (mIntent != null){
                    String action = mIntent.getAction();
                    
                    if (action!=null){
                        if(action.equals(Intent.ACTION_BOOT_COMPLETED)||action.equals(CMD_START)){
                            setTorProfile(STATUS_ON);
                        }else if (action.equals(CMD_STOP)){
                            setTorProfile(STATUS_OFF);
                        }else if (action.equals(CMD_INIT)){
                            initialize();
                            sendCallbackStatus(mCurrentStatus);
                        }else if (action.equals(CMD_NEWNYM)){
                            newIdentity();
                        }else if (action.equals(CMD_FLUSH)){
                            flushTransparentProxyRules();
                        }else if (action.equals(CMD_UPDATE)){
                            processSettings();
                        }else if (action.equals(CMD_VPN)){                        	
                            enableVpnProxy();
                        }
                        else if (action.equals(CMD_VPN_CLEAR)){
                            clearVpnProxy();
                        }
                    }
                }else{
                    Log.d(TAG, "Got null onStartCommand() intent");
                }
                
            }catch (Exception e){
                Log.e(TAG,"error onBind",e);
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
    public boolean stopService(Intent name) {
        logNotice("TorService is being stopped: " + name);
        return super.stopService(name);
    }

    @Override
    public void onDestroy ()
    {
        String msg = ("TorService is being DESTROYED... shutting down!");
        Log.d(TAG, msg);
        sendCallbackLogMessage(msg);
        unregisterReceiver(mNetworkStateReceiver);
        clearNotifications ();
        super.onDestroy();
    }
    
    private void stopTor ()
    {
       
        try
        {    
            Log.d(TAG,"Tor is stopping NOW");
            
            shutdownTorProcess ();
            
            //stop the foreground priority and make sure to remove the persistant notification
            stopForeground(true);
            
            mCurrentStatus = STATUS_OFF;
            sendCallbackStatus(mCurrentStatus);
            
            if (mHasRoot && mEnableTransparentProxy)
                disableTransparentProxy(Shell.startRootShell());
            
            clearNotifications();
            
            sendCallbackLogMessage(getString(R.string.status_disabled));

        }
        catch (Exception e)
        {
            Log.d(TAG, "An error occured stopping Tor",e);
            logNotice("An error occured stopping Tor: " + e.getMessage());
            sendCallbackLogMessage(getString(R.string.something_bad_happened));

        }
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
                
                File fileDir = new File(appCacheHome, "hs" + hsPort);
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
    
    
    private void shutdownTorProcess () throws Exception
    {

        if (conn != null)
        {

            logNotice("Using control port to shutdown Tor");
            
            
            try {
                logNotice("sending HALT signal to Tor process");
                conn.shutdownTor("HALT");
                
            } catch (Exception e) {
                Log.d(TAG,"error shutting down Tor via connection",e);
            }
            
            conn = null;
        }
        
        killProcess(fileTor);
        
        killProcess(filePolipo);
        killProcess(fileObfsclient);
        killProcess(fileMeekclient);
        
        
    }
    
    private void killProcess (File fileProcBin) throws IOException
    {
        int procId = -1;
        Shell shell = Shell.startShell();
        
        while ((procId = TorServiceUtils.findProcessId(fileProcBin.getCanonicalPath())) != -1)
        {
            
            logNotice("Found " + fileProcBin.getName() + " PID=" + procId + " - killing now...");
            
            SimpleCommand killCommand = new SimpleCommand("toolbox kill -9 " + procId);
            shell.add(killCommand);
            killCommand = new SimpleCommand("kill -9 " + procId);
            shell.add(killCommand);
        }
        
        shell.close();
    }
   
    private void logNotice (String msg)
    {
        if (msg != null && msg.trim().length() > 0)
        {
            if (ENABLE_DEBUG_LOG)            
                Log.d(TAG, msg);
        
            sendCallbackLogMessage(msg);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }
    
    private void initialize()
    {
        try
        {

            if (mNotificationManager == null)
            {
               
               IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);               
                registerReceiver(mNetworkStateReceiver , mNetworkStateFilter);
         
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
             
            }
            
            
            initBinariesAndDirectories();
            updateSettings();
        
            new Thread(new Runnable ()
            {
                public void run ()
                {
                    try
                    {
                        findExistingProc ();
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG,"error onBind",e);
                        logNotice("error finding exiting process: " + e.toString());
                    }
                    
                }
            }).start();
        
            mNumberFormat = NumberFormat.getInstance(Locale.getDefault()); //localized numbers!
            
        }
        catch (Exception e)
        {
            //what error here
            Log.e(TAG, "Error installing Orbot binaries",e);
            logNotice("There was an error installing Orbot binaries");
        }
        
        
    }

    private void initBinariesAndDirectories () throws Exception
    {

        if (appBinHome == null)
            appBinHome = getDir(DIRECTORY_TOR_BINARY,Application.MODE_PRIVATE);
        
        if (appCacheHome == null)
            appCacheHome = getDir(DIRECTORY_TOR_DATA,Application.MODE_PRIVATE);
        
        fileTor= new File(appBinHome, TOR_ASSET_KEY);
        
        filePolipo = new File(appBinHome, POLIPO_ASSET_KEY);
        
        fileObfsclient = new File(appBinHome, OBFSCLIENT_ASSET_KEY);
        
        fileMeekclient = new File(appBinHome, MEEK_ASSET_KEY);
        
        fileTorRc = new File(appBinHome, TORRC_ASSET_KEY);
        
        fileXtables = new File(appBinHome, IPTABLES_ASSET_KEY);
        
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
        }

        updateTorConfigFile ();
        

    }

    private boolean updateTorConfigFile () throws FileNotFoundException, IOException, TimeoutException
    {
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        TorResourceInstaller installer = new TorResourceInstaller(this, appBinHome); 
        
        StringBuffer extraLines = new StringBuffer();
        
        String TORRC_CONTROLPORT_FILE_KEY = "ControlPortWriteToFile";
        fileControlPort = new File(appBinHome,"control.txt");
        extraLines.append(TORRC_CONTROLPORT_FILE_KEY).append(' ').append(fileControlPort.getCanonicalPath()).append('\n');

         if (mTransProxyTethering)
         {
             extraLines.append("TransListenAddress 0.0.0.0").append('\n');
             extraLines.append("DNSListenAddress 0.0.0.0").append('\n');
             
         }
         
         extraLines.append("RunAsDaemon 1").append('\n');
         extraLines.append("AvoidDiskWrites 1").append('\n');
        
        extraLines.append("SOCKSPort ").append("auto").append('\n');
        extraLines.append("SafeSocks 0").append('\n');
        extraLines.append("TestSocks 0").append('\n');
        extraLines.append("WarnUnsafeSocks 1").append('\n');
    
        extraLines.append("TransPort ").append("auto").append('\n');
        extraLines.append("DNSPort ").append("auto").append('\n');
        extraLines.append("VirtualAddrNetwork 10.192.0.0/10").append('\n');
        extraLines.append("AutomapHostsOnResolve 1").append('\n');
       
        extraLines.append("DisableNetwork 0").append('\n');
        
        
        extraLines.append("CircuitStreamTimeout 60").append('\n');
        
        
        extraLines.append(prefs.getString("pref_custom_torrc", ""));

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
    
    private boolean enableBinExec (File fileBin) throws Exception
    {
        
        logNotice(fileBin.getName() + ": PRE: Is binary exec? " + fileBin.canExecute());
  
        if (!fileBin.canExecute())
        {
            logNotice("(re)Setting permission on binary: " + fileBin.getCanonicalPath());    
            
            Shell shell = Shell.startShell();
            shell.add(new SimpleCommand("chmod " + CHMOD_EXE_VALUE + ' ' + fileBin.getCanonicalPath())).waitForFinish();
            
            File fileTest = new File(fileBin.getCanonicalPath());
            logNotice(fileTest.getName() + ": POST: Is binary exec? " + fileTest.canExecute());
            
            shell.close();
        }
        
        return fileBin.canExecute();
    }
    
    
    private void updateSettings () throws TimeoutException, IOException
    {
        
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        mHasRoot = prefs.getBoolean(PREF_HAS_ROOT,false);
         
        mEnableTransparentProxy = prefs.getBoolean("pref_transparent", false);
         mTransProxyAll = prefs.getBoolean("pref_transparent_all", false);
         mTransProxyTethering = prefs.getBoolean("pref_transparent_tethering", false);
         mTransProxyNetworkRefresh = prefs.getBoolean("pref_transproxy_refresh", false);
         
         mShowExpandedNotifications  = prefs.getBoolean("pref_expanded_notifications", false);
         
        ENABLE_DEBUG_LOG = prefs.getBoolean("pref_enable_logging",false);
        Log.i(TAG,"debug logging:" + ENABLE_DEBUG_LOG);

        prefPersistNotifications = prefs.getBoolean(OrbotConstants.PREF_PERSIST_NOTIFICATIONS, true);
        
        mUseVPN = prefs.getBoolean("pref_vpn", false);
    }
    
    private void startTor () throws Exception
    {
        
        mCurrentStatus = STATUS_CONNECTING;
        sendCallbackStatus(mCurrentStatus);
        
        if (fileTor == null)
            initBinariesAndDirectories();
        
        enableBinExec(fileTor);
        enableBinExec(filePolipo);    
        enableBinExec(fileObfsclient);
        enableBinExec(fileMeekclient);
        enableBinExec(fileXtables);
        
        updateSettings ();

        logNotice(getString(R.string.status_starting_up));
        sendCallbackLogMessage(getString(R.string.status_starting_up));
        
        ArrayList<String> customEnv = new ArrayList<String>();
     
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
        boolean useBridges = prefs.getBoolean(OrbotConstants.PREF_BRIDGES_ENABLED, false);

        if (useBridges)
        	if (mUseVPN && !mIsLollipop)
        		customEnv.add("TOR_PT_PROXY=socks5://127.0.0.1:" + mVpnProxyPort); 
        
        String baseDirectory = fileTor.getParent();
        Shell shellUser = Shell.startShell(customEnv, baseDirectory);
        
        boolean success = runTorShellCmd(shellUser);
        
        if (success)
        {
            if (mPortHTTP != -1)
                runPolipoShellCmd(shellUser);
            
            if (mHasRoot && mEnableTransparentProxy)
            {
                 Shell shellRoot = Shell.startRootShell();

                disableTransparentProxy(shellRoot);
                enableTransparentProxy(shellRoot);
                
                shellRoot.close();
            }
            
            if (mUseVPN) //we need to turn on VPN here so the proxy is running
            {
            	enableVpnProxy();
            }

            
            getHiddenServiceHostname ();
        }
        else
        {
                 showToolbarNotification(getString(R.string.unable_to_start_tor), ERROR_NOTIFY_ID, R.drawable.ic_stat_notifyerr);

        }
        
        shellUser.close();
    }

    private boolean flushTransparentProxyRules () throws Exception 
    {

        if (mHasRoot)
        {
             if (mTransProxy == null)
             {
                 mTransProxy = new TorTransProxy(this, fileXtables);
                 
             }
    
             mTransProxy.flushTransproxyRules(this);
         
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
             mTransProxy = new TorTransProxy(this, fileXtables);
             
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
        
        if(mTransProxyAll)
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

            if (mTransProxyTethering)
            {
                showToolbarNotification(getString(R.string.transproxy_enabled_for_tethering_), TRANSPROXY_NOTIFY_ID, R.drawable.ic_stat_tor);

                mTransProxy.enableTetheringRules(this, Shell.startRootShell());
                  
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
             mTransProxy = new TorTransProxy(this, fileXtables);
 
         mTransProxy.setTransparentProxyingAll(this, false, shell);    
        ArrayList<TorifiedApp> apps = AppManager.getApps(this, TorServiceUtils.getSharedPrefs(getApplicationContext()));
        mTransProxy.setTransparentProxyingByApp(this, apps, false, shell);
    
         return true;
     }
    
    private boolean runTorShellCmd(Shell shell) throws Exception
    {

        String torrcPath = new File(appBinHome, TORRC_ASSET_KEY).getCanonicalPath();

        updateTorConfigFile();
        
        sendCallbackLogMessage(getString(R.string.status_starting_up));
        
        String torCmdString = fileTor.getCanonicalPath() 
                + " DataDirectory " + appCacheHome.getCanonicalPath() 
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
            
            processSettingsImpl();
            

        }
        
        return true;
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
    
    
    private void runPolipoShellCmd (Shell shell) throws Exception
    {
        
        logNotice( "Starting polipo process");
        
            int polipoProcId = TorServiceUtils.findProcessId(filePolipo.getCanonicalPath());

            StringBuilder log = null;
            
            int attempts = 0;
            
            if (polipoProcId == -1)
            {
                log = new StringBuilder();
                
                updatePolipoConfig();
                
                String polipoConfigPath = new File(appBinHome, POLIPOCONFIG_ASSET_KEY).getCanonicalPath();
                SimpleCommand cmdPolipo = new SimpleCommand(filePolipo.getCanonicalPath() + " -c " + polipoConfigPath + " &");
                
                shell.add(cmdPolipo);
                
                //wait one second to make sure it has started up
                Thread.sleep(1000);
                
                while ((polipoProcId = TorServiceUtils.findProcessId(filePolipo.getCanonicalPath())) == -1  && attempts < MAX_START_TRIES)
                {
                    logNotice("Couldn't find Polipo process... retrying...\n" + log);
                    Thread.sleep(3000);
                    attempts++;
                }
                
                logNotice(log.toString());
            }
            
            sendCallbackLogMessage(getString(R.string.privoxy_is_running_on_port_) + PORT_HTTP);
            
            logNotice("Polipo process id=" + polipoProcId);
            
    }
    
    private int initControlConnection (int maxTries, boolean isReconnect) throws Exception, RuntimeException
    {
            int i = 0;
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
                        
                        //remove this for now until we can make a clean way to share logs from internal storage
                        /**
                        if (ENABLE_DEBUG_LOG)
                        {
                            File fileLog2 = new File(getFilesDir(),"orbot-tor-log.txt");
                            fileLog2.setReadable(true);
                            conn.setConf("Log", "debug file " + fileLog2.getCanonicalPath());                                
                        }*/
                        
                        mCurrentStatus = STATUS_CONNECTING;
                        sendCallbackStatus(mCurrentStatus);
                        
                         String confSocks = conn.getInfo("net/listeners/socks");
                         StringTokenizer st = new StringTokenizer(confSocks," ");

                         confSocks = st.nextToken().split(":")[1];
                         confSocks = confSocks.substring(0,confSocks.length()-1);
                         mPortSOCKS = Integer.parseInt(confSocks);
                         
                        if (!isReconnect) //if we are reconnected then we don't need to reset the ports
                        {

                            SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
                            
                             String socksPortPref = prefs.getString(OrbotConstants.PREF_SOCKS, TorServiceConstants.PORT_SOCKS_DEFAULT);
                             if (socksPortPref.indexOf(':')!=-1)
                                 socksPortPref = socksPortPref.split(":")[1];
                             
                              String transPort = prefs.getString("pref_transport", TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT+"");
                              if (transPort.indexOf(':')!=-1)
                                  transPort = transPort.split(":")[1];
                             
                              String dnsPort = prefs.getString("pref_dnsport", TorServiceConstants.TOR_DNS_PORT_DEFAULT+"");
                              if (dnsPort.indexOf(':')!=-1)
                                  dnsPort = dnsPort.split(":")[1];
                             
                             try
                             {
                                 int newSocksPort = Integer.parseInt(socksPortPref);
                                 ServerSocket ss = new ServerSocket(newSocksPort);
                                 ss.close();
                                 
                                 ArrayList<String> socksLines = new ArrayList<String>();
                                 socksLines.add("SOCKSPort " + mPortSOCKS);
                                 socksLines.add("SOCKSPort " + socksPortPref);
                             
                                 conn.setConf(socksLines);
                                 
                                 mPortSOCKS = newSocksPort;
                                 
                                sendCallbackLogMessage("Local SOCKS port: " + socksPortPref);

                             }
                             catch (Exception e)
                             {
                                sendCallbackLogMessage("Error setting TransProxy port to: " + socksPortPref);

                                
                             }
                             
                             try
                             {
                                 int newPort = Integer.parseInt(transPort);
                                 ServerSocket ss = new ServerSocket(newPort);
                                 ss.close();
                                 
                                 ArrayList<String> confLines = new ArrayList<String>();
                             
                                 confLines.add("TransPort " + transPort);
                                 
                                 conn.setConf(confLines);
                                 
                                sendCallbackLogMessage("Local TransProxy port: " + transPort);

                             }
                             catch (Exception e)
                             {
                                sendCallbackLogMessage("ERROR setting TransProxy port to: " + transPort);

                                

                             }
                             
                             try
                             {
                                 int newPort = Integer.parseInt(dnsPort);
                                 ServerSocket ss = new ServerSocket(newPort);
                                 ss.close();
                                 
                                 ArrayList<String> confLines = new ArrayList<String>();
                             
                                 confLines.add("DNSPort " + dnsPort);
                                 
                                 conn.setConf(confLines);
                                 
                                sendCallbackLogMessage("Local DNSPort port: " + transPort);

                             }
                             catch (Exception e)
                             {
                                sendCallbackLogMessage("ERROR setting DNSport to: " + dnsPort);
                                

                             }
                        }
                         
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
    
    /*
    private void getTorStatus () throws IOException
    {
        try
        {
             
            if (conn != null)
            {
                 // get a single value.
                  
                   // get several values
                   
                   if (mCurrentStatus == STATUS_CONNECTING)
                   {
                       //Map vals = conn.getInfo(Arrays.asList(new String[]{
                         // "status/bootstrap-phase", "status","version"}));
            
                       String bsPhase = conn.getInfo("status/bootstrap-phase");
                       Log.d(TAG, "bootstrap-phase: " + bsPhase);
                       
                       
                   }
                   else
                   {
                     //  String status = conn.getInfo("status/circuit-established");
                     //  Log.d(TAG, "status/circuit-established=" + status);
                   }
            }
        }
        catch (Exception e)
        {
            Log.d(TAG, "Unable to get Tor status from control port");
            mCurrentStatus = STATUS_UNAVAILABLE;
        }
        
    }*/
    
    
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


        public void setTorProfile(int newState)  {
        
            if (newState == STATUS_ON)
            {
                
                if (mCurrentStatus == STATUS_OFF)
                {
                    sendCallbackLogMessage (getString(R.string.status_starting_up));
    
                
                    try
                        {

	                        boolean found = findExistingProc ();
	                        
	                        if (!found)
	                        {
	                            killProcess(fileTor);
	                            killProcess(filePolipo);
	                            
	                               startTor();
	                        }
                        }
                        catch (Exception e)
                        {                
                           
                           logException("Unable to start Tor: " + e.toString(),e);    
                            mCurrentStatus = STATUS_OFF;
                           sendCallbackStatus(mCurrentStatus);
                           
                            showToolbarNotification(getString(R.string.unable_to_start_tor) + ": " + e.getMessage(), ERROR_NOTIFY_ID, R.drawable.ic_stat_notifyerr);
                           stopTor();
                        }
                        
                }
            }
            else if (newState == STATUS_OFF)
            {
                sendCallbackLogMessage (getString(R.string.status_shutting_down));
              
                stopTor();

                mCurrentStatus = STATUS_OFF;  
                sendCallbackStatus(mCurrentStatus);
            
                
            }
        }
        
        public void enableVpnProxy () {
            
        	debug ("enabling VPN Proxy");
        	
        	mUseVPN = true;
            
            SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
            Editor ePrefs = prefs.edit();          
            ePrefs.putBoolean("pref_vpn", true);            
            ePrefs.commit();
            
            processSettings();
            
            Intent intent = new Intent(TorService.this, OrbotVpnService.class);
            intent.setAction("start");
            
            if (!mIsLollipop)
            	intent.putExtra("proxyPort",mVpnProxyPort);
            
            startService(intent);
           
        }
        
        public void refreshVpnProxy () {
            
        	debug ("refreshing VPN Proxy");
        	
            Intent intent = new Intent(TorService.this, OrbotVpnService.class);
            intent.setAction("refresh");
            startService(intent);
           
        }
        
        
        
        public void clearVpnProxy ()
        {   
        	debug ("clearing VPN Proxy");
        	
            mUseVPN = false;

            
            SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
            Editor ePrefs = prefs.edit();  
            ePrefs.putBoolean("pref_vpn", false);
            ePrefs.commit();

            processSettings();
            
            Intent intent = new Intent(TorService.this, OrbotVpnService.class);
            intent.setAction("stop");
            startService(intent);                                              
        }

        

    public void message(String severity, String msg) {
        
        
        logNotice(severity + ": " + msg);
          
          if (msg.indexOf(TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE)!=-1)
          {
              mCurrentStatus = STATUS_ON;
              sendCallbackStatus(mCurrentStatus);
              

              showToolbarNotification(getString(R.string.status_activated), NOTIFY_ID, R.drawable.ic_stat_tor);
          }
        
          
          
    }


    public void newDescriptors(List<String> orList) {
        
    }


    public void orConnStatus(String status, String orName) {
        
        
            StringBuilder sb = new StringBuilder();
            sb.append("orConnStatus (");
            sb.append(parseNodeName(orName) );
            sb.append("): ");
            sb.append(status);
            
            debug(sb.toString());
        
    }


    public void streamStatus(String status, String streamID, String target) {
        
            StringBuilder sb = new StringBuilder();
            sb.append("StreamStatus (");
            sb.append((streamID));
            sb.append("): ");
            sb.append(status);
            
            logNotice(sb.toString());
        
    }


    public void unrecognized(String type, String msg) {
        
            StringBuilder sb = new StringBuilder();
            sb.append("Message (");
            sb.append(type);
            sb.append("): ");
            sb.append(msg);
            
            logNotice(sb.toString());
    
        
    }
    
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
            
            if (mConnectivity && prefPersistNotifications)
                  showToolbarNotification(sb.toString(), NOTIFY_ID, iconId);

            mTotalTrafficWritten += written;
            mTotalTrafficRead += read;
            

        }
        
        lastWritten = written;
        lastRead = read;
        
        sendCallbackStatusMessage(lastWritten, lastRead, mTotalTrafficWritten, mTotalTrafficRead);

    }
    
    private String formatCount(long count) {
        // Converts the supplied argument into a string.
        // Under 2Mb, returns "xxx.xKb"
        // Over 2Mb, returns "xxx.xxMb"
        if (count < 1e6)
            return mNumberFormat.format(Math.round((float)((int)(count*10/1024))/10)) + "Kbps";
        else
        	return mNumberFormat.format(Math.round((float)((int)(count*100/1024/1024))/100)) + "Mbps";
        
           //return count+" kB";
    }
    
    public void circuitStatus(String status, String circID, String path) {
        
        
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
            
            if (ENABLE_DEBUG_LOG)
                debug(sb.toString());
            else if(status.equals("BUILT"))
            {

                if (mCurrentStatus == STATUS_CONNECTING)
                    mCurrentStatus = STATUS_ON;
                
                sendCallbackStatus(mCurrentStatus);
                
             
                logNotice(sb.toString());
                        
            }
            else if (status.equals("CLOSED"))
            {
                logNotice(sb.toString());

            }

            if (mShowExpandedNotifications)
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

                        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8118));
    
                        URLConnection conn = new URL(ONIONOO_BASE_URL + mNode.id).openConnection(proxy);
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
    

        
        public void processSettings (){
            try{
                boolean hadEnableTransparentProxy = mEnableTransparentProxy;
                updateSettings ();
                if (mHasRoot)
                {
                     Shell shell = Shell.startRootShell();
                    if (hadEnableTransparentProxy){
                        disableTransparentProxy(shell);
                    }
                    if (mEnableTransparentProxy){
                        enableTransparentProxy(shell);
                    }
                    shell.close();
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            Thread thread = new Thread(){
                public void run (){
                    try {
                        processSettingsImpl();
                    } catch (Exception e) {
                        logException ("error applying mPrefs",e);
                    }
                }
            };
            thread.start();
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

        
        
    
   
    private void sendCallbackStatusMessage (long upload, long download, long written, long read)
    {
         
        
        Intent intent = new Intent("log");
        // You can also include some extra data.
        intent.putExtra("up",upload);
      intent.putExtra("down",download);
      intent.putExtra("written",written);
      intent.putExtra("read",read);
      
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }
    
 //   private ArrayList<String> mLogBuffer = new ArrayList<String>();
    
    private void sendCallbackLogMessage (String logMessage)
    {
         
        
        Intent intent = new Intent("log");
          // You can also include some extra data.
          intent.putExtra("log", logMessage);
          LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }
    
    private void sendCallbackStatus (int currentStatus)
    {
         
        
        Intent intent = new Intent("status");
          // You can also include some extra data.
          intent.putExtra("status", currentStatus);
          LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    
    /*
     *  Another way to do this would be to use the Observer pattern by defining the 
     *  BroadcastReciever in the Android manifest.
     */
    
    private final BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

            boolean doNetworKSleep = prefs.getBoolean(OrbotConstants.PREF_DISABLE_NETWORK, true);
            
            final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = cm.getActiveNetworkInfo();

            boolean newConnectivityState = false;
            int newNetType = -1;
            
            boolean isChanged = false;
            
            if(netInfo != null && netInfo.isConnected()) {
                // WE ARE CONNECTED: DO SOMETHING
            	newConnectivityState = true;
            }   
            else {
                // WE ARE NOT: DO SOMETHING ELSE
            	newConnectivityState = false;
            }
            
            if (netInfo!=null)
            	newNetType = netInfo.getType();
            
            isChanged = ((mNetworkType != newNetType)||(mConnectivity != newConnectivityState));
            
            //is this a change in state?
            if (isChanged)
            {
            	mNetworkType = newNetType;
            	mConnectivity = newConnectivityState;
            	
	            if (doNetworKSleep)
	            {
	                try {
	                    
	                    setTorNetworkEnabled (mConnectivity);
	                    
	                    if (mCurrentStatus != STATUS_OFF)
	                    {
	                        if (!mConnectivity)
	                        {
	                            logNotice(context.getString(R.string.no_network_connectivity_putting_tor_to_sleep_));
	                            showToolbarNotification(getString(R.string.no_internet_connection_tor),NOTIFY_ID,R.drawable.ic_stat_tor_off);
	                            
	                        }
	                        else
	                        {
	                            logNotice(context.getString(R.string.network_connectivity_is_good_waking_tor_up_));
	                            showToolbarNotification(getString(R.string.status_activated),NOTIFY_ID,R.drawable.ic_stat_tor);
	
	                            if (mHasRoot && mEnableTransparentProxy && mTransProxyNetworkRefresh)
	                            {
	                                
	                                 Shell shell = Shell.startRootShell();
	                         
	                                disableTransparentProxy(shell);
	                                enableTransparentProxy(shell);
	                                
	                                shell.close();
	                            }
	                            
	                            if (mUseVPN) //we need to turn on VPN here so the proxy is running
	                            	refreshVpnProxy();
	            	            
	                            
	                        }
	                    }
	                    
	                    saveConfiguration();
	                    
	                } catch (Exception e) {
	                    logException ("error updating state after network restart",e);
	                }
	            }
	            
	            
            }

            
        }
    };

    private boolean processSettingsImpl () throws Exception
    {
        logNotice(getString(R.string.updating_settings_in_tor_service));
        
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        /*
        String socksConfig = prefs.getString(TorConstants.PREF_SOCKS, TorServiceConstants.PORT_SOCKS_DEFAULT);

        enableSocks (socksConfig,false);
        
        String transPort = prefs.getString("pref_transport", TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT+"");
        String dnsPort = prefs.getString("pref_dnsport", TorServiceConstants.TOR_DNS_PORT_DEFAULT+"");
        
        enableTransProxyAndDNSPorts(transPort, dnsPort);
        */
        
        boolean useBridges = prefs.getBoolean(OrbotConstants.PREF_BRIDGES_ENABLED, false);

        boolean becomeRelay = prefs.getBoolean(OrbotConstants.PREF_OR, false);
        boolean ReachableAddresses = prefs.getBoolean(OrbotConstants.PREF_REACHABLE_ADDRESSES,false);
        boolean enableHiddenServices = prefs.getBoolean("pref_hs_enable", false);

        boolean enableStrictNodes = prefs.getBoolean("pref_strict_nodes", false);
        String entranceNodes = prefs.getString("pref_entrance_nodes", "");
        String exitNodes = prefs.getString("pref_exit_nodes", "");
        String excludeNodes = prefs.getString("pref_exclude_nodes", "");
        
        if (!useBridges)
        {
           
            updateConfiguration("UseBridges", "0", false);

	        if (mUseVPN) //set the proxy here if we aren't using a bridge
	        {
	        	if (!mIsLollipop)
	        	{
		        	String proxyType = "socks5";
		        	String proxyHost = "127.0.0.1";
		            updateConfiguration(proxyType + "Proxy", proxyHost + ':' + mVpnProxyPort, false);
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
		                updateConfiguration(proxyType + "Proxy", proxyHost + ':' + proxyPort, false);
		                
		                if (proxyUser != null && proxyPass != null)
		                {
		                    if (proxyType.equalsIgnoreCase("socks5"))
		                    {
		                        updateConfiguration("Socks5ProxyUsername", proxyUser, false);
		                        updateConfiguration("Socks5ProxyPassword", proxyPass, false);
		                    }
		                    else
		                        updateConfiguration(proxyType + "ProxyAuthenticator", proxyUser + ':' + proxyPort, false);
		                    
		                }
		                else if (proxyPass != null)
		                    updateConfiguration(proxyType + "ProxyAuthenticator", proxyUser + ':' + proxyPort, false);
		                
		                
		
		            }
		        }
	        }
        }
        else
        {
        
            debug ("Using bridges");
            String bridgeCfgKey = "Bridge";

            updateConfiguration("UseBridges", "1", false);
            
            String bridgeList = prefs.getString(OrbotConstants.PREF_BRIDGES_LIST,null);
            
            if (bridgeList != null && bridgeList.length() > 1) //longer then 1 = some real values here
            {
	            String bridgeDelim = "\n";
	            
	            if (bridgeList.indexOf(",") != -1)
	            {
	                bridgeDelim = ",";
	            }
	  
	            StringTokenizer st = new StringTokenizer(bridgeList,bridgeDelim);
	            while (st.hasMoreTokens())
	            {
	                String bridgeConfigLine = st.nextToken().trim();
	                
	                if (bridgeConfigLine != null && bridgeConfigLine.length() > 0)
	                {
	               	 debug("Adding bridge: " + bridgeConfigLine);
	               	 updateConfiguration(bridgeCfgKey, bridgeConfigLine, false);
	                }
	
	            }
            
	            //check if any PT bridges are needed
	            boolean obfsBridges = bridgeList.contains("obfs2")||bridgeList.contains("obfs3")||bridgeList.contains("scramblesuit");
            
	            if (obfsBridges)
	            {
	              //  String bridgeConfig = "obfs3,scramblesuit,obfs4 exec " + fileObfsclient.getCanonicalPath();
	            	String bridgeConfig = "obfs3,obfs4 exec " + fileObfsclient.getCanonicalPath();
		                
	                debug ("Using OBFUSCATED bridges: " + bridgeConfig);
	                
	                updateConfiguration("ClientTransportPlugin",bridgeConfig, false);
	            }
            }
            else
            {
            	//time to do autobridges, aka meek

            	debug ("Using meek bridges");
                
            	String bridgeConfig = "meek exec " + fileMeekclient.getCanonicalPath();
             	updateConfiguration("ClientTransportPlugin",bridgeConfig, false);
            	
		        
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
            	
            	updateConfiguration(bridgeCfgKey, meekBridge[meekIdx], false);            	
            	
            }


//            updateConfiguration("UpdateBridgesFromAuthority", "0", false);
                
            
        }
        
        if (entranceNodes.length() > 0 || exitNodes.length() > 0 || excludeNodes.length() > 0)
        {
            //only apply GeoIP if you need it
            File fileGeoIP = new File(appBinHome,GEOIP_ASSET_KEY);
            File fileGeoIP6 = new File(appBinHome,GEOIP6_ASSET_KEY);
                
            try
            {
                if ((!fileGeoIP.exists()))
                {
                    TorResourceInstaller installer = new TorResourceInstaller(this, appBinHome); 
                    boolean success = installer.installGeoIP();
                    
                }
                
                updateConfiguration("GeoIPFile", fileGeoIP.getCanonicalPath(), false);
                updateConfiguration("GeoIPv6File", fileGeoIP6.getCanonicalPath(), false);

            }
            catch (Exception e)
            {
                 showToolbarNotification (getString(R.string.error_installing_binares),ERROR_NOTIFY_ID,R.drawable.ic_stat_notifyerr);

                return false;
            }
        }

        updateConfiguration("EntryNodes", entranceNodes, false);
        updateConfiguration("ExitNodes", exitNodes, false);
        updateConfiguration("ExcludeNodes", excludeNodes, false);
        updateConfiguration("StrictNodes", enableStrictNodes ? "1" : "0", false);
        
        

        try
        {
            if (ReachableAddresses)
            {
                String ReachableAddressesPorts =
                    prefs.getString(OrbotConstants.PREF_REACHABLE_ADDRESSES_PORTS, "*:80,*:443");
                
                updateConfiguration("ReachableAddresses", ReachableAddressesPorts, false);

            }
            else
            {
                updateConfiguration("ReachableAddresses", "", false);
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
                
                updateConfiguration("ServerDNSResolvConfFile", dnsFile, false);
                updateConfiguration("ORPort", ORPort + "", false);
                updateConfiguration("Nickname", nickname, false);
                updateConfiguration("ExitPolicy", "reject *:*", false);

            }
            else
            {
                updateConfiguration("ORPort", "", false);
                updateConfiguration("Nickname", "", false);
                updateConfiguration("ExitPolicy", "", false);
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

                    String hsDirPath = new File(appCacheHome,"hs" + hsPort).getCanonicalPath();
                    
                    debug("Adding hidden service on port: " + hsPortConfig);
                    
                    updateConfiguration("HiddenServiceDir",hsDirPath, false);
                    updateConfiguration("HiddenServicePort",hsPortConfig, false);
                    

                } catch (NumberFormatException e) {
                    Log.e(TAG,"error parsing hsport",e);
                } catch (Exception e) {
                    Log.e(TAG,"error starting share server",e);
                }
            }
            
            
        }
        else
        {
            updateConfiguration("HiddenServiceDir","", false);
            
        }
        
        if (mUseVPN)
        {
        	updateConfiguration("DNSListenAddress","10.0.0.1:" + TorServiceConstants.TOR_DNS_PORT_DEFAULT,false);
        }
        
       // updateConfiguration("DisableNetwork","0", false);

        saveConfiguration();
    
        return true;
    }
    
    /*
    private void enableSocks (String socks, boolean safeSocks) throws RemoteException
    {    
        updateConfiguration("SOCKSPort", socks, false);
        updateConfiguration("SafeSocks", safeSocks ? "1" : "0", false);
        updateConfiguration("TestSocks", "1", false);
        updateConfiguration("WarnUnsafeSocks", "1", false);
        saveConfiguration();
        
    }
    
    private void enableTransProxyAndDNSPorts (String transPort, String dnsPort) throws RemoteException
    {
        logMessage ("Transparent Proxying: enabling port...");
         
         updateConfiguration("TransPort",transPort,false);
         updateConfiguration("DNSPort",dnsPort,false);
         updateConfiguration("VirtualAddrNetwork","10.192.0.0/10",false);
         updateConfiguration("AutomapHostsOnResolve","1",false);
         saveConfiguration();
    }*/
    
    private void blockPlaintextPorts (String portList) throws RemoteException
    {
        updateConfiguration("RejectPlaintextPorts",portList,false);
    }
    
    //using Google DNS for now as the public DNS server
    private String writeDNSFile () throws IOException
    {
        File file = new File(appBinHome,"resolv.conf");
        
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

}
