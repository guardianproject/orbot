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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class Orbot extends Activity implements OnLongClickListener, TorConstants
{
	
	/* Useful UI bits */
	private TextView lblStatus = null; //the main text display widget
	private ImageView imgStatus = null; //the main touchable image for activating Orbot
	private ProgressDialog progressDialog;
	private MenuItem mItemOnOff = null;
	
	/* Some tracking bits */
	private int torStatus = STATUS_READY; //latest status reported from the tor service
	
	/* Tor Service interaction */
		/* The primary interface we will be calling on the service. */
    ITorService mService = null;
	private boolean autoStartOnBind = false;

	SharedPreferences prefs;
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
      //if Tor binary is not running, then start the service up
		startService(new Intent(INTENT_TOR_SERVICE));
		

    	setTheme(android.R.style.Theme_Black_NoTitleBar);
    	
    	prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	setContentView(R.layout.layout_main);
		
    	lblStatus = (TextView)findViewById(R.id.lblStatus);
		lblStatus.setOnLongClickListener(this);
    	imgStatus = (ImageView)findViewById(R.id.imgStatus);
    	imgStatus.setOnLongClickListener(this);
    	
    	

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
      
        mItem =  menu.add(0,6, Menu.NONE, getString(R.string.menu_about));
        mItem.setIcon(R.drawable.ic_menu_about);
        
        mItem = menu.add(0, 3, Menu.NONE, getString(R.string.menu_wizard));
        mItem.setIcon(R.drawable.ic_menu_goto);
       
        mItem = menu.add(0, 8, Menu.NONE, getString(R.string.menu_exit));
        mItem.setIcon(R.drawable.ic_menu_exit);
       
        
        return true;
    }
    

    private void showAbout ()
	{
		
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.layout_about, null); 
        
        TextView versionName = (TextView)view.findViewById(R.id.versionName);
        versionName.setText(R.string.app_version);    
        
		new AlertDialog.Builder(this)
        .setTitle(getString(R.string.button_about))
        .setView(view)
        .show();
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
		    		if (mItemOnOff != null)
		    			mItemOnOff.setTitle(R.string.menu_stop);
					startTor();
					
				}
				else
				{
		    		if (mItemOnOff != null)
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
		else if (item.getItemId() == 6)
		{
			showAbout();
			
			
		}
		
        return true;
	}
	
	private void doExit ()
	{
		try {
		
			stopTor();
			
			stopService(new Intent(ITorService.class.getName()));
			
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
	/*
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
		
	}*/
 
    /* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	protected void onPause() {
		super.onPause();
		
		hideProgressDialog();

		if (aDialog != null)
			aDialog.dismiss();
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		  // Save UI state changes to the savedInstanceState.
		  // This bundle will be passed to onCreate if the process is
		  // killed and restarted.
		  // etc.
		  super.onSaveInstanceState(savedInstanceState);
		}
	
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  // Restore UI state from the savedInstanceState.
	  // This bundle has also been passed to onCreate.
	 
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor pEdit = prefs.edit();
		
		String hsPortString = prefs.getString("pref_hs_ports", "");
		
		if (hsPortString.length() > 0 && hsPortString.indexOf(hsPort+"")==-1)
			hsPortString += ',' + hsPort;
		else
			hsPortString = hsPort + "";
		
		pEdit.putString("pref_hs_ports", hsPortString);
		pEdit.putBoolean("pref_hs_enable", true);
		
		pEdit.commit();
		
		String onionHostname = prefs.getString("pref_hs_hostname","");

		Intent nResult = new Intent();
		nResult.putExtra("hs_host", onionHostname);
		setResult(RESULT_OK, nResult);
	
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	protected void onResume() {
		super.onResume();
		
		bindService();
		
		 updateStatus("");
		 
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
			
			//setTitle(getString(R.string.app_name) + ' ' + getString(R.string.app_version));
	    
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancelAll();
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	
			boolean showWizard = prefs.getBoolean("show_wizard",true);
			
			if (showWizard)
			{
			
				Editor pEdit = prefs.edit();
				
				pEdit.putBoolean("show_wizard",false);
				
				pEdit.commit();
				
				startActivityForResult(new Intent(getBaseContext(), LotsaText.class), 1);

			}
			
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	protected void onStart() {
		super.onStart();
		
		
		updateStatus ("");
		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	protected void onStop() {
		super.onStop();
		
		//unbindService();
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		Editor pEdit = prefs.edit();
		pEdit.putBoolean("wizardscreen1",true);
		pEdit.commit();
		startActivityForResult(new Intent(getBaseContext(), LotsaText.class), 1);
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
		
		if (requestCode == 1 && resultCode == 1010 && mService != null)
		{
			new ProcessSettingsAsyncTask().execute(mService);	
		}
	}
	
	AlertDialog aDialog = null;
	
	private void showAlert(String title, String msg, boolean button)
	{
		try
		{
			if (aDialog != null && aDialog.isShowing())
				aDialog.dismiss();
		}
		catch (Exception e){} //swallow any errors
		
		 if (button)
		 {
				aDialog = new AlertDialog.Builder(this)
			 .setIcon(R.drawable.icon)
	         .setTitle(title)
	         .setMessage(msg)
	         .setPositiveButton(android.R.string.ok, null)
	         .show();
		 }
		 else
		 {
			 aDialog = new AlertDialog.Builder(this)
			 .setIcon(R.drawable.icon)
	         .setTitle(title)
	         .setMessage(msg)
	         .show();
		 }
	
		 aDialog.setCanceledOnTouchOutside(true);
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

		    		hideProgressDialog();
		    		
		    		String lblMsg = getString(R.string.status_activated);
		    		//+ "\n" + torServiceMsg;
		    		
		    		lblStatus.setText(lblMsg);
		    		
		    		if (torServiceMsg.length() > 0)
		    			showAlert("Update", torServiceMsg, false);
		    		
		    		boolean showFirstTime = prefs.getBoolean("connect_first_time",true);
		    		
		    		if (showFirstTime)
		    		{
		    		
		    			Editor pEdit = prefs.edit();
		    			
		    			pEdit.putBoolean("connect_first_time",false);
		    			
		    			pEdit.commit();
		    			
		    			showAlert(getString(R.string.status_activated),getString(R.string.connect_first_time),true);
		    			
		    		}
		    		
		    		if (mItemOnOff != null)
		    			mItemOnOff.setTitle(R.string.menu_stop);
		    		

		    	}
		    	else if (torStatus == STATUS_CONNECTING)
		    	{
		    		
		    		imgStatus.setImageResource(R.drawable.torstarting);
		    		
		    		if (progressDialog != null)
		    			progressDialog.setMessage(torServiceMsg);
		    		
		    		if (mItemOnOff != null)
		    			mItemOnOff.setTitle(R.string.menu_stop);
		    			
		    	}
		    	else if (torStatus == STATUS_OFF)
		    	{
		    		imgStatus.setImageResource(R.drawable.toroff);
		    		

		    		hideProgressDialog();
		    		
		    		lblStatus.setText(getString(R.string.status_shutting_down));
		    			
		    		if (mItemOnOff != null)
		    			mItemOnOff.setTitle(R.string.menu_start);
		    	}
		    	else
		    	{


		    		hideProgressDialog();
		    		
		    		imgStatus.setImageResource(R.drawable.toroff);
		    		lblStatus.setText(getString(R.string.status_disabled) + "\n" + getString(R.string.press_to_start));
		    		
		    		if (mItemOnOff != null)
		    			mItemOnOff.setTitle(R.string.menu_start);
		    		
		    	}
	    	}
		    	
    	}
    	catch (RemoteException e)
    	{
    		Log.e(TAG,"remote exception updating status",e);
    	}
    	
        
    }
  
    private void startTor () throws RemoteException
    {
    	
    	bindService();
    	
    	mService.setProfile(TorServiceConstants.PROFILE_ON); //this means turn on
		
		imgStatus.setImageResource(R.drawable.torstarting);
		lblStatus.setText(getString(R.string.status_starting_up));
		
		Message msg = mHandler.obtainMessage(TorServiceConstants.ENABLE_TOR_MSG);
    	mHandler.sendMessage(msg);
    	
    	
    	
    }
    
    private void stopTor () throws RemoteException
    {
    	if (mService != null)
    	{
    		mService.setProfile(TorServiceConstants.PROFILE_OFF);
    		Message msg = mHandler.obtainMessage(TorServiceConstants.DISABLE_TOR_MSG);
    		mHandler.sendMessage(msg);
    	}
    	
     
    }
    
	/*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	public boolean onLongClick(View view) {
		
		
		try
		{
			
			if (mService == null)
			{
			
			}
			else if (mService.getStatus() == STATUS_READY)
			{
				
				createProgressDialog(getString(R.string.status_starting_up));

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
			
		return true;
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
           
        	Message msg = mHandler.obtainMessage(TorServiceConstants.STATUS_MSG);
        	msg.getData().putString(HANDLER_TOR_MSG, value);
        	mHandler.sendMessage(msg);
        }

		@Override
		public void logMessage(String value) throws RemoteException {
			
			Message msg = mHandler.obtainMessage(TorServiceConstants.LOG_MSG);
        	msg.getData().putString(HANDLER_TOR_MSG, value);
        	mHandler.sendMessage(msg);
			
		}
    };
    

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TorServiceConstants.STATUS_MSG:

                	String torServiceMsg = (String)msg.getData().getString(HANDLER_TOR_MSG);
                	
                	updateStatus(torServiceMsg);
                	
                    break;
                case TorServiceConstants.LOG_MSG:
                	
                	
                    break;
                case TorServiceConstants.ENABLE_TOR_MSG:
                	
                	
                	updateStatus((String)msg.getData().getString(HANDLER_TOR_MSG));
                	
                	break;
                case TorServiceConstants.DISABLE_TOR_MSG:
                	
                	updateStatus((String)msg.getData().getString(HANDLER_TOR_MSG));
                	
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
       
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                mService.registerCallback(mCallback);
           
                updateStatus("");
                
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
	
    private void createProgressDialog (String msg)
    {
    	if (progressDialog != null && progressDialog.isShowing())
    		return;
    	
    	progressDialog = ProgressDialog.show(Orbot.this, "", msg);	
		progressDialog.setCancelable(true);

    }
    
    private void hideProgressDialog ()
    {

		if (progressDialog != null && progressDialog.isShowing())
		{
			progressDialog.dismiss();

		}
		
		
    }
}
