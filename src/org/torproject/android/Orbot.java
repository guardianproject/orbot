/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.util.StringTokenizer;

import org.torproject.android.service.ITorService;
import org.torproject.android.service.ITorServiceCallback;
import org.torproject.android.service.TorRoot;
import org.torproject.android.service.TorServiceConstants;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Orbot extends Activity implements OnClickListener, TorConstants
{
	
	/* Useful UI bits */
	private TextView txtMessageLog = null; //the full screen log view of Tor control messages
	private TextView lblStatus = null; //the main text display widget
	private ImageView imgStatus = null; //the main touchable image for activating Orbot
	private ProgressDialog progressDialog;
	
	/* Some tracking bits */
	private int torStatus = STATUS_REQUIRES_DEMAND; //latest status reported from the tor service
	private int currentView = 0; //the currently displayed UI view
	private StringBuffer logBuffer = new StringBuffer(); //the output of the service log messages
	private String lastUrl = null;
	
	/* Tor Service interaction */
		/* The primary interface we will be calling on the service. */
    ITorService mService = null;

    
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        
        mItem = menu.add(0, 1, Menu.NONE, getString(R.string.menu_home));
        mItem.setIcon(R.drawable.ic_menu_home);

        mItem = menu.add(0, 2, Menu.NONE, getString(R.string.menu_browse));
        mItem.setIcon(R.drawable.ic_menu_goto);
        
        mItem = menu.add(0, 3, Menu.NONE, getString(R.string.menu_settings));
        mItem.setIcon(R.drawable.ic_menu_register);
        
        mItem =  menu.add(0, 4, Menu.NONE, getString(R.string.menu_log));
        mItem.setIcon(R.drawable.ic_menu_reports);

        mItem = menu.add(0, 5, Menu.NONE, getString(R.string.menu_info));
        mItem.setIcon(R.drawable.ic_menu_about);
      
        return true;
    }
    
    /* When a menu item is selected launch the appropriate view or activity
     * (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		super.onMenuItemSelected(featureId, item);
		
		if (item.getItemId() == 1)
		{
			this.showMain();
		}
		else if (item.getItemId() == 3)
		{
			this.showSettings();
		}
		else if (item.getItemId() == 4)
		{
			this.showMessageLog();
		}
		else if (item.getItemId() == 2)
		{
			openBrowser(URL_TOR_CHECK);
		}
		else if (item.getItemId() == 5)
		{
			showHelp();
		}
		
        return true;
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
	@Override
	protected void onPause() {
		super.onPause();
		
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		updateStatus (""); //update the status, which checks the service status
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
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
	@Override
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
	 * Show the about view - a popup dialog
	 */
	private void showAbout ()
	{
		
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.layout_about, null); 
        TextView versionName = (TextView)view.findViewById(R.id.versionName);
        versionName.setText(R.string.app_version);    
        
		new AlertDialog.Builder(this)
        .setTitle(getString(R.string.menu_info))
        .setView(view)
        .setNeutralButton(getString(R.string.button_help), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                      showHelp();
                }
        })
        .setNegativeButton(getString(R.string.button_close), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //    Log.d(TAG, "Close pressed");
                }
        })
        .show();
	}
	
	/*
	 * Show the help view - a popup dialog
	 */
	private void showHelp ()
	{
		
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.layout_help, null); 
        
		new AlertDialog.Builder(this)
        .setTitle(getString(R.string.menu_info))
        .setView(view)
        .setNeutralButton(getString(R.string.button_about), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                   
                	showAbout();
                }
        })
        .setNegativeButton(getString(R.string.button_close), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //    Log.d(TAG, "Close pressed");
                }
        })
        .show();
	}
	
	
	/*
	 * Show the message log UI
	 */
	private void showMessageLog ()
	{
		currentView = R.layout.layout_log;
		setContentView(currentView);
		
		txtMessageLog = (TextView)findViewById(R.id.messageLog);
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

	private void processSettings ()
	{
		StringBuffer torrcText = new StringBuffer();
		
		torrcText.append(TorConstants.TORRC_DEFAULT);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		boolean useBridges = prefs.getBoolean(PREF_BRIDGES_ENABLED, false);
		
		boolean autoUpdateBridges = prefs.getBoolean(PREF_BRIDGES_UPDATED, false);
		
		enableTransparentProxy = prefs.getBoolean(PREF_TRANSPARENT, false);
		
		String bridgeList = prefs.getString(PREF_BRIDGES_LIST,"");

		if (useBridges)
		{
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
		}
		
		Utils.saveTextFile(TorServiceConstants.TORRC_INSTALL_PATH, torrcText.toString());
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
		    	
		    		lblStatus.setText(getString(R.string.status_activated));
		    		
		    		if (progressDialog != null)
		    		{
		    			
		    			progressDialog.cancel();
		    			progressDialog.hide();
		    			progressDialog = null;
		    		}
		    		
		    		if (torServiceMsg != null && torServiceMsg.length()>0)
		    			Toast.makeText(this, torServiceMsg, Toast.LENGTH_LONG).show();
		    		
		    	
		    	}
		    	else if (torStatus == STATUS_CONNECTING)
		    	{
		    		
		    		imgStatus.setImageResource(R.drawable.torstarting);
		    		lblStatus.setText(getString(R.string.status_starting_up));
		    		
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
	    			
	    			int idx = torServiceMsg.indexOf("%");
	    			
	    			if (idx != -1)
	    			{	
	    				String pComp = torServiceMsg.substring(idx-2,idx).trim();
	    				int ipComp = Integer.parseInt(pComp);
	    				progressDialog.setProgress(ipComp);
	    			}
		    			
		    	}
		    	else if (torStatus == STATUS_UNAVAILABLE)
		    	{
		    		imgStatus.setImageResource(R.drawable.torstopping);
		    		lblStatus.setText(getString(R.string.status_shutting_down));
		    		
		    		if (torServiceMsg != null && torServiceMsg.length()>0)
		    			Toast.makeText(this, torServiceMsg, Toast.LENGTH_LONG).show();
		    		
		    	
		    		
		    	}
		    	else
		    	{

		    		if (torServiceMsg != null && torServiceMsg.length()>0)
		    			Toast.makeText(this, torServiceMsg, Toast.LENGTH_LONG).show();
		    		
		    	
		    		if (progressDialog != null)
		    		{
		    			
		    			progressDialog.cancel();
		    			progressDialog.hide();
		    			progressDialog = null;
		    		}
		    		
		    		
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
				else if (mService.getStatus() == STATUS_REQUIRES_DEMAND)
				{
					processSettings();
					mService.setProfile(PROFILE_ON);

					if (hasRoot && enableTransparentProxy)
					{
						TorRoot.enableDNSProxying();
						TorRoot.enabledWebProxying();
					}
				}
				else
				{
					
					mService.setProfile(PROFILE_ONDEMAND);	
				
					if (hasRoot && enableTransparentProxy)
					{
						TorRoot.purgeNatIptables();
					}
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
           
        	Message msg = mHandler.obtainMessage(BUMP_MSG);
        	msg.getData().putString(HANDLER_TOR_MSG, value);
        	mHandler.sendMessage(msg);
        }
    };
    
    private static final int BUMP_MSG = 1;
    
    	
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case BUMP_MSG:

                	String torServiceMsg = (String)msg.getData().getString(HANDLER_TOR_MSG);
                	
                	logBuffer.append(torServiceMsg);
                	logBuffer.append('\n');
                	
                	updateStatus(torServiceMsg);
                	
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
    boolean hasRoot = false;
    boolean enableTransparentProxy = false;
    
    private void bindService ()
    {
    	 bindService(new Intent(ITorService.class.getName()),
                 mConnection, Context.BIND_AUTO_CREATE);
    	 
    	 mIsBound = true;
    
    	 hasRoot = TorRoot.hasRootAccess();

    	
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