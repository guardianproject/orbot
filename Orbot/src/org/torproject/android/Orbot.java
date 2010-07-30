/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.util.StringTokenizer;

import org.torproject.android.service.ITorService;
import org.torproject.android.service.ITorServiceCallback;
import org.torproject.android.service.TorTransProxy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
			this.showSettings();
		}
		else if (item.getItemId() == 6)
		{
			this.showMessageLog();
		}
		else if (item.getItemId() == 2)
		{
			openBrowser(URL_TOR_CHECK);
		}
		else if (item.getItemId() == 3)
		{
			showHelp();
		}
		else if (item.getItemId() == 7)
		{
			//launch check.torproject.org
			openBrowser(URL_TOR_CHECK);
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
			
			unbindService();
			
            stopService(new Intent(ITorService.class.getName()));
			
			
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
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	protected void onResume() {
		super.onResume();
		
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		
		if (mService != null)
		{
			try {
				processSettings();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mOrbot);

		boolean showWizard = prefs.getBoolean("show_wizard",true);
		
		if (showWizard)
		{
		
			Editor pEdit = prefs.edit();
			
			pEdit.putBoolean("show_wizard",false);
			
			pEdit.commit();
			
			showHelp();
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
		
		//updateStatus ("");
		
		

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
    	
    	//updateStatus("");
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
		
	
		startActivity(new Intent(this, SettingsPreferences.class));

		
	}
	
	
	
	/*
	 * Read in the Preferences and write then to the .torrc file
	 */

	/*
	private void processSettingsOld ()
	{
		StringBuffer torrcText = new StringBuffer();
		
		torrcText.append(TorConstants.TORRC_DEFAULT);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		boolean useBridges = prefs.getBoolean(PREF_BRIDGES_ENABLED, false);
		
		boolean autoUpdateBridges = prefs.getBoolean(PREF_BRIDGES_UPDATED, false);

        boolean becomeRelay = prefs.getBoolean(PREF_OR, false);

        boolean ReachableAddresses = prefs.getBoolean(PREF_REACHABLE_ADDRESSES,false);

		enableTransparentProxy = prefs.getBoolean(PREF_TRANSPARENT, false);
		
		if (hasRoot)
		{
			if (enableTransparentProxy)
			{
				TorTransProxy.setDNSProxying();
				TorTransProxy.setTransparentProxying(this, TorServiceUtils.getApps(this));
			}
			else
			{
				TorTransProxy.purgeNatIptables();
			}
			
		}
		
		String bridgeList = prefs.getString(PREF_BRIDGES_LIST,"");

		if (useBridges)
		{
			if (bridgeList == null || bridgeList.length() == 0)
			{
			
				showAlert("Bridge Error","In order to use the bridge feature, you must enter at least one bridge IP address." +
						"Send an email to bridges@torproject.org with the line \"get bridges\" by itself in the body of the mail from a gmail account.");
				
				showSettings();
				return;
			}
			
			
			torrcText.append("UseBridges 1");
			torrcText.append('\n');		

			torrcText.append("UpdateBridgesFromAuthority ");
			
			if (autoUpdateBridges)
				torrcText.append("1");
			else
				torrcText.append("0");
			
			torrcText.append('\n');		
			
			String bridgeDelim = "\n";
			
			if (bridgeList.indexOf(",") != -1)
			{
				bridgeDelim = ",";
			}
			
			StringTokenizer st = new StringTokenizer(bridgeList,bridgeDelim);
			while (st.hasMoreTokens())
			{
				torrcText.append("bridge ");
				torrcText.append(st.nextToken());
				torrcText.append('\n');		

			}
		}
		else
		{
			torrcText.append("UseBridges 0");
			torrcText.append('\n');
		}

        try
        {
            if (ReachableAddresses)
            {
                String ReachableAddressesPorts =
                    prefs.getString(PREF_REACHABLE_ADDRESSES_PORTS, "*:80,*:443");
                torrcText.append("ReachableAddresses ");
                // We should verify this and someday, the Exception will matter :-)
                torrcText.append(ReachableAddressesPorts);
                torrcText.append('\n');
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

                torrcText.append("ORPort ");
                torrcText.append(ORPort);
                torrcText.append('\n');

                torrcText.append("Nickname ");
                torrcText.append(nickname);
                torrcText.append('\n');

                torrcText.append("ExitPolicy reject *:*");
                torrcText.append('\n');
            }
        }
        catch (Exception e)
        {
            showAlert("Uh-oh!","Your relay settings caused an exception!");
            showSettings();
            return;
        }

		Utils.saveTextFile(TorServiceConstants.TORRC_INSTALL_PATH, torrcText.toString());
	}
	*/
	
	private void processSettings () throws RemoteException
	{
		
	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		boolean useBridges = prefs.getBoolean(PREF_BRIDGES_ENABLED, false);
		
		boolean autoUpdateBridges = prefs.getBoolean(PREF_BRIDGES_UPDATED, false);

        boolean becomeRelay = prefs.getBoolean(PREF_OR, false);

        boolean ReachableAddresses = prefs.getBoolean(PREF_REACHABLE_ADDRESSES,false);

		boolean enableTransparentProxy = prefs.getBoolean(PREF_TRANSPARENT, false);
		
		
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
        }
        catch (Exception e)
        {
            showAlert("Uh-oh!","Your relay settings caused an exception!");
          
            return;
        }

        mService.saveConfiguration();
        
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
	    	
    		if (this.currentView == R.layout.layout_log)    
    		{
    			txtMessageLog.append(torServiceMsg);
    			txtMessageLog.append("\n");
    			
    		}

	    	if (imgStatus != null)
	    	{
	    		
		    	if (torStatus == STATUS_ON)
		    	{
		    		imgStatus.setImageResource(R.drawable.toron);
		    		imgStatus.clearAnimation();
		    		
		    		lblStatus.setText(getString(R.string.status_activated));
		    		
		    		
		    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mOrbot);

		    		boolean showWizard = prefs.getBoolean("connect_first_time",true);
		    		
		    		if (showWizard)
		    		{
		    		
		    			Editor pEdit = prefs.edit();
		    			
		    			pEdit.putBoolean("connect_first_time",false);
		    			
		    			pEdit.commit();
		    			
		    			showAlert(getString(R.string.status_activated),getString(R.string.connect_first_time));
		    			
		    		}
	    		
	    			/*
		    		if (progressDialog != null)
		    		{
		    			
		    			progressDialog.cancel();
		    			progressDialog.hide();
		    			progressDialog = null;
		    			
		    			
		    		}*/
		    
		    	}
		    	else if (torStatus == STATUS_CONNECTING)
		    	{
		    		
		    		imgStatus.setImageResource(R.drawable.torstarting);
		    		
	    			
		    		/*
		    		if (imgStatus.getAnimation()==null)
		    		{
		    			
		    			imgStatus.setAnimation(AnimationUtils.loadAnimation(this, R.anim.starting));
		    			imgStatus.getAnimation().setRepeatMode(Animation.INFINITE);
		    			
		    			imgStatus.getAnimation().setRepeatCount(Animation.INFINITE);
		    		}*/
		    		
		    		
		    		/*
		    		if (progressDialog == null)
		    		{
			    		progressDialog = new ProgressDialog(this);
			    		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			    		progressDialog.setCancelable(true);
			    		progressDialog.setMessage(getString(R.string.status_starting_up));
			    		progressDialog.show();
			    		
			    		progressDialog.setProgress(10);

		    		}
		    			
	    			progressDialog.setMessage(torServiceMsg);
	    			*/
		    		
		    		lblStatus.setText(torServiceMsg);
		    		
		    		
	    			int idx = torServiceMsg.indexOf("%");
	    			
	    			if (idx != -1)
	    			{	
	    				String pComp = torServiceMsg.substring(idx-2,idx).trim();
	    				int ipComp = Integer.parseInt(pComp);
	    			//	progressDialog.setProgress(ipComp);
	    				
	    			}
		    			
		    	}
		    	else if (torStatus == STATUS_OFF)
		    	{
		    		imgStatus.setImageResource(R.drawable.torstopping);
		    		imgStatus.clearAnimation();
		    		
		    		lblStatus.setText(getString(R.string.status_shutting_down));
		    			
		    	}
		    	else
		    	{

		    		
		    		/*
		    		if (progressDialog != null)
		    		{
		    			
		    			progressDialog.cancel();
		    			progressDialog.hide();
		    			progressDialog = null;
		    		}
		    		*/
		    		imgStatus.clearAnimation();
		    		
		    		imgStatus.setImageResource(R.drawable.toroff);
		    		lblStatus.setText(getString(R.string.status_disabled));
		    		
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
    	
    //	updateStatus("");
    }
    
    private void stopTor () throws RemoteException
    {
    	mService.setProfile(PROFILE_ONDEMAND);	//these means turn off
		
		Message msg = mHandler.obtainMessage(DISABLE_TOR_MSG);
    	mHandler.sendMessage(msg);
    	
    	//updateStatus("");
    	
    }
    
    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	public void onClick(View view) {
		
		// the start button
		if (view.getId()==R.id.imgStatus || view.getId()==R.id.lblStatus)
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
				Log.i(TAG,"error onclick",e);
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
                	
                	
                	if (torServiceMsg.length() > 0 && torServiceMsg.charAt(0)!='>')
                		updateStatus(torServiceMsg);
                	
                    break;
                case LOG_MSG:

                	String torLogMsg = (String)msg.getData().getString(HANDLER_TOR_MSG);
                	
                	logBuffer.append(torLogMsg);
                	logBuffer.append('\n');
                	
                	
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
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
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
