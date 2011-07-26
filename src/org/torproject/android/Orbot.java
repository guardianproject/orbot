/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.util.Locale;

import org.torproject.android.service.ITorService;
import org.torproject.android.service.ITorServiceCallback;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.settings.ProcessSettingsAsyncTask;
import org.torproject.android.settings.SettingsPreferences;
import org.torproject.android.wizard.ChooseLocaleWizardActivity;
import org.torproject.android.wizard.LotsaText;

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
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Orbot extends Activity implements TorConstants, OnLongClickListener
{
	/* Useful UI bits */
	private TextView lblStatus = null; //the main text display widget
	private ImageView imgStatus = null; //the main touchable image for activating Orbot
	private ProgressDialog progressDialog;
	private MenuItem mItemOnOff = null;
    private RelativeLayout trafficRow = null; // the row showing the traffic
    private TextView downloadText = null;
    private TextView uploadText = null;

	/* Some tracking bits */
	private int torStatus = STATUS_READY; //latest status reported from the tor service
	
	/* Tor Service interaction */
		/* The primary interface we will be calling on the service. */
    ITorService mService = null;
	private boolean autoStartOnBind = false;

	SharedPreferences prefs;
	
	public static Orbot currentInstance = null;
	
    private static void setCurrent(Orbot current){
    	Orbot.currentInstance = current;
    }
    
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Orbot.setCurrent(this);

      //if Tor binary is not running, then start the service up
      //might want to look at whether we need to call this every time
      //or whether binding to the service is enough
           	
        setLocale();
        
	startService(new Intent(INTENT_TOR_SERVICE));
		
    	prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	setContentView(R.layout.layout_main);
		
    	lblStatus = (TextView)findViewById(R.id.lblStatus);
		lblStatus.setOnLongClickListener(this);
    	imgStatus = (ImageView)findViewById(R.id.imgStatus);
    	imgStatus.setOnLongClickListener(this);
    	trafficRow = (RelativeLayout)findViewById(R.id.trafficRow);
    	downloadText = (TextView)findViewById(R.id.trafficDown);
        uploadText = (TextView)findViewById(R.id.trafficUp);
        


    }
    
   /*
    * Create the UI Options Menu (non-Javadoc)
    * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
    */
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
       
        mItemOnOff = menu.getItem(0);
        
        return true;
    }
    

    private void showAbout ()
        {
                
	        LayoutInflater li = LayoutInflater.from(this);
	        View view = li.inflate(R.layout.layout_about, null); 
	        
	        String version = "";
	        
	        try {
	        	version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
	        } catch (NameNotFoundException e) {
	        	version = "Version Not Found";
	        }
	        
	        TextView versionName = (TextView)view.findViewById(R.id.versionName);
	        versionName.setText(version);    
	        
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
                
                if (item.getItemId() == R.id.menu_start)
                {
                        
                        try
                        {
                                
                                if (mService == null)
                                {
                                
                                }
                                else if (mService.getStatus() == TorServiceConstants.STATUS_OFF)
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
                else if (item.getItemId() == R.id.menu_settings)
                {
                        showSettings();
                }
                else if (item.getItemId() == R.id.menu_wizard)
                {
                		startWizard();
                }
                else if (item.getItemId() == R.id.menu_verify)
                {
                        doTorCheck();
                }
                else if (item.getItemId() == R.id.menu_exit)
                {
                        //exit app
                        doExit();
                        
                        
                }
                else if (item.getItemId() == R.id.menu_about)
                {
                        showAbout();
                        
                        
                }
                
        return true;
        }
        
        /**
        * This is our attempt to REALLY exit Orbot, and stop the background service
        * However, Android doesn't like people "quitting" apps, and/or our code may not
        * be quite right b/c no matter what we do, it seems like the TorService still exists
        **/
        private void doExit ()
        {
                try {
                		
                        //one of the confusing things about all of this code is the multiple
                        //places where things like "stopTor" are called, both in the Activity and the Service
                        //not something to tackle in your first iteration, but i thin we can talk about fixing
                        //terminology but also making sure there are clear distinctions in control
                        stopTor();
                        
                        //perhaps this should be referenced as INTENT_TOR_SERVICE as in startService
                        stopService(new Intent(ITorService.class.getName()));
                        
                        //clears all notifications from the status bar
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.cancelAll();
                
                        
                } catch (RemoteException e) {
                        Log.w(TAG, e);
                }
                
                //Kill all the wizard activities
                setResult(RESULT_CLOSE_ALL);
                finish();
                
        }
        
    /* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	protected void onPause() {
		super.onPause();
		
		unbindService();
		
		hideProgressDialog();

		if (aDialog != null)
			aDialog.dismiss();
	}
	
	private void doTorCheck ()
	{
		
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			
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
		
//		setLocale();

    	bindService();
		
		if (getIntent() == null)
			return;
		
		String action = getIntent().getAction();
		
		if (action == null)
			return;
		
		if (action.equals("org.torproject.android.REQUEST_HS_PORT"))
		{
			
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    
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

			String requestMsg = getString(R.string.hidden_service_request, hsPort);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(requestMsg).setPositiveButton("Allow", dialogClickListener)
			    .setNegativeButton("Deny", dialogClickListener).show();
			
		
		}
		else if (action.equals("org.torproject.android.START_TOR"))
		{
			autoStartOnBind = true;
			
			if (mService == null)
			{
				bindService();
			}
			else
			{
				//already running!
			}
		}
		else
		{
		
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	
			boolean showWizard = prefs.getBoolean("show_wizard",true);
			
			if (showWizard)
			{
			
				
				Editor pEdit = prefs.edit();
				pEdit.putBoolean("show_wizard",false);
				pEdit.commit();
	
				startWizard();
				
				//startActivityForResult(new Intent(getBaseContext(), LotsaText.class), 1);

			}
			
		}
		
		updateStatus ("");
		
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	protected void onStop() {
		super.onStop();
		
		unbindService();
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
	private void startWizard ()
	{

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		Editor pEdit = prefs.edit();
		pEdit.putBoolean("wizardscreen1",true);
		pEdit.commit();
		startActivityForResult(new Intent(getBaseContext(), ChooseLocaleWizardActivity.class), 1);
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
            
            //if we get a response from an activity we launched (like from line 527 where we launch the Settings/Prefs screen)
            //and the resultCode matches our arbitrary 1010 value, AND Tor is running
            //then update the preferences in an async background task
            if (requestCode == 1 && resultCode == 1010 && mService != null)
            {
                    new ProcessSettingsAsyncTask().execute(mService);      
                    setLocale();
            }
    }
    
    AlertDialog aDialog = null;
    
    //general alert dialog for mostly Tor warning messages
    //sometimes this can go haywire or crazy with too many error
    //messages from Tor, and the user cannot stop or exit Orbot
    //so need to ensure repeated error messages are not spamming this method
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
                            aDialog = new AlertDialog.Builder(Orbot.this)
                     .setIcon(R.drawable.icon)
             .setTitle(title)
             .setMessage(msg)
             .setPositiveButton(android.R.string.ok, null)
             .show();
             }
             else
             {
                     aDialog = new AlertDialog.Builder(Orbot.this)
                     .setIcon(R.drawable.icon)
             .setTitle(title)
             .setMessage(msg)
             .show();
             }
    
             aDialog.setCanceledOnTouchOutside(true);
    }
    
    /*
     * Set the state of the running/not running graphic and label
     * this all needs to be looked at w/ the shift to progressDialog
     */
    public void updateStatus (String torServiceMsg)
    {
            try
            {
                    //if the serivce is bound, query it for the curren status value (int)
                    if (mService != null)
                            torStatus = mService.getStatus();
                    
                    //now update the layout_main UI based on the status
                    if (imgStatus != null)
                    {
                            
                            if (torStatus == TorServiceConstants.STATUS_ON)
                            {
                                    imgStatus.setImageResource(R.drawable.toron);

                                    hideProgressDialog();
                                    
                                    String lblMsg = getString(R.string.status_activated);
                                     
                                    lblStatus.setText(lblMsg);
                                    
                                    if (torServiceMsg != null && torServiceMsg.length() > 0)
                                    {
                                    //        showAlert("Update", torServiceMsg, false);
                                    	lblStatus.setText(torServiceMsg);
                                    }
                                    
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
                            else if (torStatus == TorServiceConstants.STATUS_CONNECTING)
                            {
                                    
                                    imgStatus.setImageResource(R.drawable.torstarting);
                                    
                                    if (progressDialog != null)
                                            progressDialog.setMessage(torServiceMsg);
                                    
                                    if (mItemOnOff != null)
                                            mItemOnOff.setTitle(R.string.menu_stop);
                                            
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
  
  // guess what? this start's Tor! actually no it just requests via the local ITorService to the remote TorService instance
  // to start Tor
    private void startTor () throws RemoteException
    {
            
            // this is a bit of a strange/old/borrowed code/design i used to change the service state
            // not sure it really makes sense when what we want to say is just "startTor"
            mService.setProfile(TorServiceConstants.PROFILE_ON); //this means turn on
                
            //here we update the UI which is a bit sloppy and mixed up code wise
            //might be best to just call updateStatus() instead of directly manipulating UI in this method - yep makes sense
            imgStatus.setImageResource(R.drawable.torstarting);
            lblStatus.setText(getString(R.string.status_starting_up));
            
            //we send a message here to the progressDialog i believe, but we can clarify that shortly
            Message msg = mHandler.obtainMessage(TorServiceConstants.ENABLE_TOR_MSG);
            msg.getData().putString(HANDLER_TOR_MSG, getString(R.string.status_starting_up));
            mHandler.sendMessage(msg);
            
    	
    }
    
    //now we stop Tor! amazing!
    private void stopTor () throws RemoteException
    {
    	if (mService != null)
    	{
    		mService.setProfile(TorServiceConstants.PROFILE_OFF);
    		Message msg = mHandler.obtainMessage(TorServiceConstants.DISABLE_TOR_MSG);
    		mHandler.sendMessage(msg);
            trafficRow.setVisibility(RelativeLayout.GONE);

    	}
    	
     
    }
    
        /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
        public boolean onLongClick(View view) {
                
                
            try
            {
                    
                if (mService != null && mService.getStatus() == TorServiceConstants.STATUS_OFF)
                {
                        
                        createProgressDialog(getString(R.string.status_starting_up));

                        startTor();
                }
                else
                {
                        
                        stopTor();
                        
                }
                
                return true;
                    
            }
            catch (Exception e)
            {
                    Log.d(TAG,"error onclick",e);
            }
            
            return false;
                    
        }
        

    /**
     * This implementation is used to receive callbacks from the remote
     * service. 
     *
     * If we have this setup probably, we shouldn't have to poll or query status
     * to the service, as it should send it as it changes or when we bind/unbind to it
     * from this activity
     */
    private ITorServiceCallback mCallback = new ITorServiceCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
         
         //receive a new string vaule end-user displayable message from the ITorService
        public void statusChanged(String value) {
           
           //pass it off to the progressDialog
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

		@Override
		public void updateBandwidth(long upload, long download)
				throws RemoteException {
			
        	Message msg = Message.obtain();
			msg.what = TorServiceConstants.MESSAGE_TRAFFIC_COUNT;
			
			
			Bundle data = new Bundle();
			data.putLong("upload", upload);
			data.putLong("download", download);
			
			msg.setData(data);
			mHandler.sendMessage(msg); 

		}
    };
    

// this is what takes messages or values from the callback threads or other non-mainUI threads
//and passes them back into the main UI thread for display to the user
    private Handler mHandler = new Handler() {
    	
    	private String lastServiceMsg = null;
    	
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TorServiceConstants.STATUS_MSG:

                        String torServiceMsg = (String)msg.getData().getString(HANDLER_TOR_MSG);
                        
                        if (lastServiceMsg == null || !lastServiceMsg.equals(torServiceMsg))
                        {
                        	updateStatus(torServiceMsg);
                        
                        	lastServiceMsg = torServiceMsg;
                        }
                        
                    break;
                case TorServiceConstants.LOG_MSG:
                        
                        
                    break;
                case TorServiceConstants.ENABLE_TOR_MSG:
                        
                        
                        updateStatus((String)msg.getData().getString(HANDLER_TOR_MSG));
                        
                        break;
                case TorServiceConstants.DISABLE_TOR_MSG:
                	
                	updateStatus((String)msg.getData().getString(HANDLER_TOR_MSG));
                	
                	break;
                	

            	case TorServiceConstants.MESSAGE_TRAFFIC_COUNT :
                    
            		trafficRow.setVisibility(RelativeLayout.VISIBLE);
            		Bundle data = msg.getData();
            		DataCount datacount =  new DataCount(data.getLong("upload"),data.getLong("download"));      		
            		downloadText.setText(formatCount(datacount.Download));
            		uploadText.setText(formatCount(datacount.Upload));
            		downloadText.invalidate();
            		uploadText.invalidate();
			
            		try {
            			String TotalUpload = mService.getInfo("traffic/written");
            			String TotalDownload = mService.getInfo("traffic/read");
            			StringBuilder sb = new StringBuilder();
            			sb.append("Total Upload " + TotalUpload);
            			sb.append("Total Download" + TotalDownload);
            			Log.d(TAG,sb.toString());
            		} catch (RemoteException e) {
            			Log.d(TAG,"Total bandwidth error"+e.getMessage());
            		}
           		 		
            		break;
                		
                default:
                    super.handleMessage(msg);
            }
        }
        
        
        
    };

    
    /**
     * Class for interacting with the main interface of the service.
     */
     // this is the connection that gets called back when a successfull bind occurs
     // we should use this to activity monitor unbind so that we don't have to call
     // bindService() a million times
     
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
           
                //again with the update status?!? :P
                updateStatus(null);
                
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
    
    //should move this up with all the other class variables
    boolean mIsBound = false;
    
    //this is where we bind! 
    private void bindService ()
    {
         //since its auto create, we prob don't ever need to call startService
         //also we should again be consistent with using either iTorService.class.getName()
         //or the variable constant       
         bindService(new Intent(ITorService.class.getName()),
             mConnection, Context.BIND_AUTO_CREATE);
         
         mIsBound = true;
    
    }
    
    //unbind removes the callback, and unbinds the service
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
            
            //maybe needs this?
            mService = null; 
            
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            
        }
    }
        
    private void createProgressDialog (String msg)
    {
            if (progressDialog != null && progressDialog.isShowing())
            {
            	progressDialog.setMessage(msg);
            }
            else
            {
            	progressDialog = ProgressDialog.show(Orbot.this, "", msg);        
                progressDialog.setCancelable(true);
            }
    }
    
    private void hideProgressDialog ()
    {

        if (progressDialog != null && progressDialog.isShowing())
        {
                progressDialog.dismiss();
                progressDialog = null;
        }
                
    }
    
    private void setLocale ()
    {
    	
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        Configuration config = getResources().getConfiguration();

        String lang = settings.getString(PREF_DEFAULT_LOCALE, "");
        
        if (! "".equals(lang) && ! config.locale.getLanguage().equals(lang))
        {
        	Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }
    }

   	public class DataCount {
   		// data uploaded
   		public long Upload;
   		// data downloaded
   		public long Download;
   		
   		DataCount(long Upload, long Download){
   			this.Upload = Upload;
   			this.Download = Download;
   		}
   	}
   	
   	private String formatCount(long count) {
		// Converts the supplied argument into a string.
		// Under 2Mb, returns "xxx.xKb"
		// Over 2Mb, returns "xxx.xxMb"
		if (count < 1e6 * 2)
			return ((float)((int)(count*10/1024))/10 + " kbps");
		return ((float)((int)(count*100/1024/1024))/100 + " mbps");
		
   		//return count+" kB";
	}
   	
}
