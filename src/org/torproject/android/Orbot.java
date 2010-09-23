/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.StringTokenizer;

import org.torproject.android.service.ITorService;
import org.torproject.android.service.ITorServiceCallback;
import org.torproject.android.service.TorServiceConstants;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class Orbot extends Activity implements OnClickListener, TorConstants
{
	
	/* Useful UI bits */
	private TextView txtMessageLog = null; //the full screen log view of Tor control messages
	private TextView lblStatus = null; //the main text display widget
	private ImageView imgStatus = null; //the main touchable image for activating Orbot
//	private ProgressDialog progressDialog;
	private MenuItem mItemOnOff = null;
	
	/* Some tracking bits */
	private int torStatus = STATUS_READY; //latest status reported from the tor service
	private int currentView = 0; //the currently displayed UI view
	private StringBuffer logBuffer = new StringBuffer(); //the output of the service log messages
	
	/* Tor Service interaction */
		/* The primary interface we will be calling on the service. */
    ITorService mService = null;
	private boolean autoStartOnBind = false;
	
    Orbot mOrbot = null;
    
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOrbot = this;
        
    	setTheme(android.R.style.Theme_Black_NoTitleBar);
    	//setTitle(getString(R.string.app_name) + ' ' + getString(R.string.app_version));
        showMain();

    }
    
   /*
    * Create the UI Options Menu (non-Javadoc)
    * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
    */
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuItem mItem = null;
        
        mItemOnOff = menu.add(0, 1, Menu.NONE, getString(R.string.menu_start));
        mItemOnOff.setIcon(android.R.drawable.ic_menu_share);
        mItemOnOff.setAlphabeticShortcut('t');
        
        mItem = menu.add(0, 4, Menu.NONE, getString(R.string.menu_settings));
        mItem.setIcon(R.drawable.ic_menu_register);
       
        mItem = menu.add(0, 7, Menu.NONE, getString(R.string.menu_verify));
        mItem.setIcon(R.drawable.ic_menu_check);
      
        mItem =  menu.add(0,6, Menu.NONE, getString(R.string.menu_log));
        mItem.setIcon(R.drawable.ic_menu_reports);
        
        mItem = menu.add(0, 3, Menu.NONE, getString(R.string.menu_info));
        mItem.setIcon(R.drawable.ic_menu_about);
       
        mItem = menu.add(0, 8, Menu.NONE, getString(R.string.menu_exit));
        mItem.setIcon(R.drawable.ic_menu_exit);
       
        return true;
    }
    
    /* When a menu item is selected launch the appropriate view or activity
     * (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		super.onMenuItemSelected(featureId, item);
		
		if (item.getItemId() == 1)
		{
			
			try
			{
				
				if (mService == null)
				{
				
				}
				else if (mService.getStatus() == STATUS_READY)
				{
					mItemOnOff.setTitle(R.string.menu_stop);
					startTor();
					
				}
				else
				{
					mItemOnOff.setTitle(R.string.menu_start);
					stopTor();
					
				}
				
			}
			catch (RemoteException re)
			{
				Log.w(TAG, "Unable to start/top Tor from menu UI", re);
			}
		}
		else if (item.getItemId() == 4)
		{
			showSettings();
		}
		else if (item.getItemId() == 6)
		{
			showMessageLog();
		}
		else if (item.getItemId() == 3)
		{
			showHelp();
		}
		else if (item.getItemId() == 7)
		{
			doTorCheck();
		}
		else if (item.getItemId() == 8)
		{
			//exit app
			doExit();
			
			
		}
		
        return true;
	}
	
	private void doExit ()
	{
		try {
		
			stopTor();
			
			
			
        	NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancelAll();
		
			
		} catch (RemoteException e) {
			Log.w(TAG, e);
		}
		
		finish();
		
	}
	
	/* Return to the main view when the back key is pressed
	 * (non-Javadoc)
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event){
		
		if(keyCode==KeyEvent.KEYCODE_BACK){

			if(currentView != R.layout.layout_main){
					
					showMain ();
					return true;
			}
			else{
				return super.onKeyDown(keyCode, event);
			}
		}
	
		return super.onKeyDown(keyCode, event);
		
	}
 
    /* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	protected void onPause() {
		super.onPause();
		

	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		  // Save UI state changes to the savedInstanceState.
		  // This bundle will be passed to onCreate if the process is
		  // killed and restarted.
		  savedInstanceState.putString("log", logBuffer.toString());
		  // etc.
		  super.onSaveInstanceState(savedInstanceState);
		}
	
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  // Restore UI state from the savedInstanceState.
	  // This bundle has also been passed to onCreate.
	 
	  String logText = savedInstanceState.getString("log");
	  logBuffer.append(logText);
	}
	
	private void doTorCheck ()
	{
		
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		            
		    		openBrowser(URL_TOR_CHECK);

					
		        	
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		        
		        	//do nothing
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.tor_check).setPositiveButton(R.string.btn_okay, dialogClickListener)
		    .setNegativeButton(R.string.btn_cancel, dialogClickListener).show();

	}
	
	private void enableHiddenServicePort (int hsPort)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mOrbot);
		Editor pEdit = prefs.edit();
		
		String hsPortString = prefs.getString("pref_hs_ports", "");
		
		if (hsPortString.length() > 0 && hsPortString.indexOf(hsPort+"")==-1)
			hsPortString += ',' + hsPort;
		else
			hsPortString = hsPort + "";
		
		pEdit.putString("pref_hs_ports", hsPortString);
		pEdit.putBoolean("pref_hs_enable", true);
		
		pEdit.commit();
		
		try {
			processSettings();
		
			String onionHostname = getHiddenServiceHostname();
	
			Intent nResult = new Intent();
			nResult.putExtra("hs_host", onionHostname);
			setResult(RESULT_OK, nResult);
			
		} catch (RemoteException e) {
			Log.e(TAG, "error accessing hidden service", e);
		}
		
		
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	protected void onResume() {
		super.onResume();
		
		
		if (getIntent() == null)
			return;
		
		String action = getIntent().getAction();
		
		if (action == null)
			return;
		
		if (action.equals("org.torproject.android.REQUEST_HS_PORT"))
		{
			
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			            
			        	int hsPort = getIntent().getIntExtra("hs_port", -1);
						
			        	enableHiddenServicePort (hsPort);
			        	
						finish();
						
			        	
			            break;

			        case DialogInterface.BUTTON_NEGATIVE:
			            //No button clicked
			        	finish();
			            break;
			        }
			    }
			};

        	int hsPort = getIntent().getIntExtra("hs_port", -1);

			String requestMsg = "An app wants to open a server port (" + hsPort + ") to the Tor network. This is safe if you trust the app.";
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(requestMsg).setPositiveButton("Allow", dialogClickListener)
			    .setNegativeButton("Deny", dialogClickListener).show();
			
		
		}
		else if (action.equals("org.torproject.android.START_TOR"))
		{
			autoStartOnBind = true;
			
			if (mService == null)
				bindService();
			
		}
		else
		{
			
		
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancelAll();
			
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mOrbot);
	
			boolean showWizard = prefs.getBoolean("show_wizard",true);
			
			if (showWizard)
			{
			
				Editor pEdit = prefs.edit();
				
				pEdit.putBoolean("show_wizard",false);
				
				pEdit.commit();
				
			    new WizardHelper(this).showWizard();

			}
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	protected void onStart() {
		super.onStart();
		
		//if Tor binary is not running, then start the service up
		startService(new Intent(INTENT_TOR_SERVICE));
		bindService ();
		
		updateStatus ("");
		

	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	protected void onStop() {
		super.onStop();
		
		unbindService();
	}



	/*
	 * Show the main form UI
	 */
	private void showMain ()
    {
		bindService(); //connect the UI activity to the remote service
		
		currentView = R.layout.layout_main;
		setContentView(currentView);
		
		//add touch listeners for the image and the text label
    	findViewById(R.id.imgStatus).setOnClickListener(this); 
    	findViewById(R.id.lblStatus).setOnClickListener(this);
    	
    	lblStatus = (TextView)findViewById(R.id.lblStatus);
    	imgStatus = (ImageView)findViewById(R.id.imgStatus);
    	
    	updateStatus("");
    }
	
	/*
	 * Launch the system activity for Uri viewing with the provided url
	 */
	private void openBrowser(String url)
	{
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		
	}
	
	
	
	/*
	 * Show the help view - a popup dialog
	 */
	private void showHelp ()
	{
		
       new WizardHelper(this).showWizard();
	}
	
	
	/*
	 * Show the message log UI
	 */
	private void showMessageLog ()
	{
		currentView = R.layout.layout_log;
		setContentView(currentView);
		
		txtMessageLog = (TextView)findViewById(R.id.messageLog);
		
		txtMessageLog.setClickable(true);
    	txtMessageLog.setText(logBuffer.toString());
    	
		
	}
	
	
    /*
     * Load the basic settings application to display torrc
     */
	private void showSettings ()
	{
		
	
		startActivityForResult(new Intent(this, SettingsPreferences.class), 1);
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == 1)
		{
			try {
				processSettings();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void processSettings () throws RemoteException
	{
		
		try
		{
			if (mService == null)
				return; //nothing to do if the service isn't connected yet
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			
			boolean useBridges = prefs.getBoolean(PREF_BRIDGES_ENABLED, false);
			
			//boolean autoUpdateBridges = prefs.getBoolean(PREF_BRIDGES_UPDATED, false);
	
	        boolean becomeRelay = prefs.getBoolean(PREF_OR, false);
	
	        boolean ReachableAddresses = prefs.getBoolean(PREF_REACHABLE_ADDRESSES,false);
	
	        boolean enableHiddenServices = prefs.getBoolean("pref_hs_enable", false);
			
			
			boolean enableTransparentProxy = prefs.getBoolean(PREF_TRANSPARENT, false);
			
		
			mService.updateTransProxy();
			
			String bridgeList = prefs.getString(PREF_BRIDGES_LIST,"");
	
			if (useBridges)
			{
				if (bridgeList == null || bridgeList.length() == 0)
				{
				
					showAlert("Bridge Error","In order to use the bridge feature, you must enter at least one bridge IP address." +
							"Send an email to bridges@torproject.org with the line \"get bridges\" by itself in the body of the mail from a gmail account.");
					
				
					return;
				}
				
				
				mService.updateConfiguration("UseBridges", "1", false);
					
				String bridgeDelim = "\n";
				
				if (bridgeList.indexOf(",") != -1)
				{
					bridgeDelim = ",";
				}
				
				StringTokenizer st = new StringTokenizer(bridgeList,bridgeDelim);
				while (st.hasMoreTokens())
				{
	
					mService.updateConfiguration("bridge", st.nextToken(), false);
	
				}
				
				mService.updateConfiguration("UpdateBridgesFromAuthority", "0", false);
				
			}
			else
			{
				mService.updateConfiguration("UseBridges", "0", false);
	
			}
	
	        try
	        {
	            if (ReachableAddresses)
	            {
	                String ReachableAddressesPorts =
	                    prefs.getString(PREF_REACHABLE_ADDRESSES_PORTS, "*:80,*:443");
	                
	    			mService.updateConfiguration("ReachableAddresses", ReachableAddressesPorts, false);
	
	            }
	            else
	            {
	            	mService.updateConfiguration("ReachableAddresses", "", false);
	            }
	        }
	        catch (Exception e)
	        {
	           showAlert("Config Error","Your ReachableAddresses settings caused an exception!");
	        }
	
	        try
	        {
	            if (becomeRelay && (!useBridges) && (!ReachableAddresses))
	            {
	                int ORPort =  Integer.parseInt(prefs.getString(PREF_OR_PORT, "9001"));
	                String nickname = prefs.getString(PREF_OR_NICKNAME, "Orbot");
	
	    			mService.updateConfiguration("ORPort", ORPort + "", false);
	    			mService.updateConfiguration("Nickname", nickname, false);
	    			mService.updateConfiguration("ExitPolicy", "reject *:*", false);
	
	            }
	            else
	            {
	            	mService.updateConfiguration("ORPort", "", false);
	    			mService.updateConfiguration("Nickname", "", false);
	    			mService.updateConfiguration("ExitPolicy", "", false);
	            }
	        }
	        catch (Exception e)
	        {
	            showAlert("Uh-oh!","Your relay settings caused an exception!");
	          
	            return;
	        }
	
	        if (enableHiddenServices)
	        {
	        	mService.updateConfiguration("HiddenServiceDir","/data/data/org.torproject.android/", false);
	        	
	        	String hsPorts = prefs.getString("pref_hs_ports","");
	        	
	        	StringTokenizer st = new StringTokenizer (hsPorts,",");
	        	String hsPortConfig = null;
	        	
	        	while (st.hasMoreTokens())
	        	{
	        		hsPortConfig = st.nextToken();
	        		
	        		if (hsPortConfig.indexOf(":")==-1) //setup the port to localhost if not specifed
	        		{
	        			hsPortConfig = hsPortConfig + " 127.0.0.1:" + hsPortConfig;
	        		}
	        		
	        		mService.updateConfiguration("HiddenServicePort",hsPortConfig, false);
	        	}
	        	
	        	//force save now so the hostname file gets generated
	        	 mService.saveConfiguration();
	        	 
	        	String onionHostname = getHiddenServiceHostname();
	        	
	        	if (onionHostname != null)
	        	{
	        		
	        		Editor pEdit = prefs.edit();
	    			pEdit.putString("pref_hs_hostname",onionHostname);
	    			pEdit.commit();
	        		
	        	}
	        }
	        else
	        {
	        	mService.updateConfiguration("HiddenServiceDir","", false);
	        	
	        }
	        
	        mService.saveConfiguration();
		 }
        catch (Exception e)
        {
            showAlert("Uh-oh!","There was an error updating your settings");
          
            Log.w(TAG, "processSettings()", e);
            
            return;
        }

	}
	
	private String getHiddenServiceHostname ()
	{
    	String appHome = "/data/data/" + TorServiceConstants.TOR_APP_USERNAME + "/";

    	File file = new File(appHome, "hostname");
    	try {
			String onionHostname = Utils.readString(new FileInputStream(file));
			return onionHostname.trim();
		} catch (FileNotFoundException e) {
			Log.d(TAG, "unable to read onion hostname file",e);
			return null;
		}
	}
	
	
	private void showAlert(String title, String msg)
	{
		 
		 new AlertDialog.Builder(this)
		 .setIcon(R.drawable.icon)
         .setTitle(title)
         .setMessage(msg)
         .setPositiveButton(android.R.string.ok, null)
         .show();
	}
    /*
     * Set the state of the running/not running graphic and label
     */
    public void updateStatus (String torServiceMsg)
    {
    	try
    	{
    		
    		if (mService != null)
    			torStatus = mService.getStatus();
    		

	    	if (imgStatus != null)
	    	{
	    		
		    	if (torStatus == STATUS_ON)
		    	{
		    		imgStatus.setImageResource(R.drawable.toron);
		    	//	imgStatus.clearAnimation();
		    		
		    		String lblMsg = getString(R.string.status_activated) + "\n" + torServiceMsg;
		    		
		    		lblStatus.setText(lblMsg);
		    		
		    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mOrbot);

		    		boolean showFirstTime = prefs.getBoolean("connect_first_time",true);
		    		
		    		if (showFirstTime)
		    		{
		    		
		    			Editor pEdit = prefs.edit();
		    			
		    			pEdit.putBoolean("connect_first_time",false);
		    			
		    			pEdit.commit();
		    			
		    			showAlert(getString(R.string.status_activated),getString(R.string.connect_first_time));
		    			
		    		}
		    		
			        boolean enableHiddenServices = prefs.getBoolean("pref_hs_enable", false);

			        if (enableHiddenServices)
			        {
			    		String onionHostname = getHiddenServiceHostname();
			    		
			    		if (onionHostname != null)
			    		{
			    			Editor pEdit = prefs.edit();
			    			pEdit.putString("pref_hs_hostname",onionHostname);
			    			pEdit.commit();
			    			
			    		}
		    		
			        }
	    		
		    
		    	}
		    	else if (torStatus == STATUS_CONNECTING)
		    	{
		    		
		    		imgStatus.setImageResource(R.drawable.torstarting);
		    		
		    		
		    		lblStatus.setText(torServiceMsg);
		    		
		    		/*
	    			int idx = torServiceMsg.indexOf("%");
	    			
	    			if (idx != -1)
	    			{	
	    				String pComp = torServiceMsg.substring(idx-2,idx).trim();
	    				int ipComp = Integer.parseInt(pComp);
	    			//	progressDialog.setProgress(ipComp);
	    				
	    			}*/
		    			
		    	}
		    	else if (torStatus == STATUS_OFF)
		    	{
		    		imgStatus.setImageResource(R.drawable.torstopping);
		    	//	imgStatus.clearAnimation();
		    		
		    		lblStatus.setText(getString(R.string.status_shutting_down));
		    			
		    	}
		    	else
		    	{

		    		
		    	//	imgStatus.clearAnimation();
		    		
		    		imgStatus.setImageResource(R.drawable.toroff);
		    		lblStatus.setText(getString(R.string.status_disabled) + "\n" + getString(R.string.press_to_start));
		    		
		    		
		    		
		    	}
	    	}
		    	
    	}
    	catch (RemoteException e)
    	{
    		e.printStackTrace();
    	}
    	
        
    }
  
    private void startTor () throws RemoteException
    {
    	mService.setProfile(PROFILE_ON); //this means turn on
		
		imgStatus.setImageResource(R.drawable.torstarting);
		lblStatus.setText(getString(R.string.status_starting_up));
		
		Message msg = mHandler.obtainMessage(ENABLE_TOR_MSG);
    	mHandler.sendMessage(msg);
    	
    	logBuffer = new StringBuffer();
    }
    
    private void stopTor () throws RemoteException
    {
    	if (mService != null)
    	{
    		mService.setProfile(PROFILE_ONDEMAND);	//these means turn off
		
    		Message msg = mHandler.obtainMessage(DISABLE_TOR_MSG);
    		mHandler.sendMessage(msg);
    	}
    	
    	//unbindService();
		
        //stopService(new Intent(ITorService.class.getName()));
	
    	
    }
    
    /*
    @Override
	public boolean onTouchEvent(MotionEvent event) {
    	
    	if (currentView == R.layout.layout_main)
    	{
	    	try
			{
				if (mService == null)
				{
				
				}
				else if (mService.getStatus() == STATUS_READY)
				{
					startTor();
					
				}
				else
				{
					stopTor();
					
				}
				
			}
			catch (Exception e)
			{
				Log.d(TAG,"error onclick",e);
			}
    	}
    	
		return super.onTouchEvent(event);
	}*/

	/*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	public void onClick(View view) {
		
		if (currentView == R.layout.layout_main)
    	{
			try
			{
				
				if (mService == null)
				{
				
				}
				else if (mService.getStatus() == STATUS_READY)
				{
					
					startTor();
					
				}
				else
				{
					
					stopTor();
					
				}
				
			}
			catch (Exception e)
			{
				Log.d(TAG,"error onclick",e);
			}
			
		}
		
		
	}
	

    /**
     * This implementation is used to receive callbacks from the remote
     * service.
     */
    private ITorServiceCallback mCallback = new ITorServiceCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        public void statusChanged(String value) {
           
        	Message msg = mHandler.obtainMessage(STATUS_MSG);
        	msg.getData().putString(HANDLER_TOR_MSG, value);
        	mHandler.sendMessage(msg);
        }

		@Override
		public void logMessage(String value) throws RemoteException {
			
			Message msg = mHandler.obtainMessage(LOG_MSG);
        	msg.getData().putString(HANDLER_TOR_MSG, value);
        	mHandler.sendMessage(msg);
			
		}
    };
    
    private static final int STATUS_MSG = 1;
    private static final int ENABLE_TOR_MSG = 2;
    private static final int DISABLE_TOR_MSG = 3;
    private static final int LOG_MSG = 4;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_MSG:

                	String torServiceMsg = (String)msg.getData().getString(HANDLER_TOR_MSG);
                	
                	logBuffer.append(torServiceMsg);
                	logBuffer.append('\n');
                	
                	
                	if (torServiceMsg.length() > 0)
                		updateStatus(torServiceMsg);
                	
                    break;
                case LOG_MSG:

                	String torLogMsg = (String)msg.getData().getString(HANDLER_TOR_MSG);
                	
                	logBuffer.append(torLogMsg);
                	logBuffer.append('\n');
                	
                	if (txtMessageLog != null)
                	{
                		txtMessageLog.append(torLogMsg + '\n');
                	}
                	
                    break;
                case ENABLE_TOR_MSG:
                	
                	
                	break;
                case DISABLE_TOR_MSG:
                	
                	break;
                		
                default:
                    super.handleMessage(msg);
            }
        }
        
    };

    
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = ITorService.Stub.asInterface(service);
       
            updateStatus ("");
       
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                mService.registerCallback(mCallback);
           

	            if (autoStartOnBind)
	            {
	            	autoStartOnBind = false;
	            	
	            	startTor();
	            	
	            }
            
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            	Log.d(TAG,"error registering callback to service",e);
            }
       
          
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
          
        }
    };
    
    boolean mIsBound = false;
    
    private void bindService ()
    {
    	 bindService(new Intent(ITorService.class.getName()),
                 mConnection, Context.BIND_AUTO_CREATE);
    	 
    	 mIsBound = true;
    
    	
    	
    }
    
    private void unbindService ()
    {
    	if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    mService.unregisterCallback(mCallback);
                    
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
            
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            
        }
    }
	
}
