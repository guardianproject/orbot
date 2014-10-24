/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */

package org.torproject.android;

import static org.torproject.android.TorConstants.TAG;

import java.net.URLDecoder;
import java.util.Locale;

import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.TorServiceUtils;
import org.torproject.android.settings.SettingsPreferences;
import org.torproject.android.vpn.OrbotVpnService;
import org.torproject.android.wizard.ChooseLocaleWizardActivity;
import org.torproject.android.wizard.TipsAndTricks;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;


public class Orbot extends ActionBarActivity implements TorConstants, OnLongClickListener, OnTouchListener, OnSharedPreferenceChangeListener
{
	/* Useful UI bits */
	private TextView lblStatus = null; //the main text display widget
	private ImageProgressView imgStatus = null; //the main touchable image for activating Orbot

	private MenuItem mItemOnOff = null;
    private TextView downloadText = null;
    private TextView uploadText = null;
    private TextView mTxtOrbotLog = null;
    private SlidingDrawer mDrawer = null;
    private boolean mDrawerOpen = false;
    private View mViewMain = null;

	/* Some tracking bits */
	private int torStatus = TorServiceConstants.STATUS_OFF; //latest status reported from the tor service
	
	private SharedPreferences mPrefs = null;

	private boolean autoStartFromIntent = false;

    /** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = TorServiceUtils.getSharedPrefs(getApplicationContext());        
        mPrefs.registerOnSharedPreferenceChangeListener(this);
           	
        setLocale();
        
    	doLayout();

    	appConflictChecker ();
    	

  	  // Register to receive messages.
  	  // We are registering an observer (mMessageReceiver) to receive Intents
  	  // with actions named "custom-event-name".
  	  LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
  	      new IntentFilter("status"));
  	  
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
			      new IntentFilter("log"));

		startService("init");
	}
	
	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	  @Override
	  public void onReceive(Context context, Intent intent) {
	    // Get extra data included in the Intent
		  
		if (intent.hasExtra("log"))
		{
			String log = intent.getStringExtra("log");
			updateStatus(log);
		}
		else if (intent.hasExtra("up"))
		{
			long upload = intent.getLongExtra("up",0);
			long download = intent.getLongExtra("down",0);
			long written = intent.getLongExtra("written",0);
			long read = intent.getLongExtra("read",0);
			
			Message msg = mHandler.obtainMessage(TorServiceConstants.MESSAGE_TRAFFIC_COUNT);
			msg.getData().putLong("download", download);
			msg.getData().putLong("upload", upload);
			msg.getData().putLong("readTotal", read);
			msg.getData().putLong("writeTotal", written);
			mHandler.sendMessage(msg);
			
		}
		else if (intent.hasExtra("status"))
		{
			torStatus = intent.getIntExtra("status", TorServiceConstants.STATUS_OFF);
			updateStatus("");
		}
		
	  }
	};

	ProgressDialog mProgressDialog;
	
	private void startService (String action)
	{
		
		Intent torService = new Intent(this, TorService.class);    
		torService.setAction(action);
		startService(torService);
		
        
	}
	
	private void doLayout ()
	{
    	setContentView(R.layout.layout_main);
		
    	mViewMain = findViewById(R.id.viewMain);
    	lblStatus = (TextView)findViewById(R.id.lblStatus);
		lblStatus.setOnLongClickListener(this);
    	imgStatus = (ImageProgressView)findViewById(R.id.imgStatus);
    	imgStatus.setOnLongClickListener(this);
    	imgStatus.setOnTouchListener(this);
    	
    	lblStatus.setText("Initializing the application...");
    	
    	downloadText = (TextView)findViewById(R.id.trafficDown);
        uploadText = (TextView)findViewById(R.id.trafficUp);
        mTxtOrbotLog = (TextView)findViewById(R.id.orbotLog);
        
        mDrawer = ((SlidingDrawer)findViewById(R.id.SlidingDrawer));
    	Button slideButton = (Button)findViewById(R.id.slideButton);
    	if (slideButton != null)
    	{
	    	slideButton.setOnTouchListener(new OnTouchListener (){
	
				@Override
				public boolean onTouch(View v, MotionEvent event) {
	
					if (event.equals(MotionEvent.ACTION_DOWN))
					{
						mDrawerOpen = !mDrawerOpen;
						mTxtOrbotLog.setEnabled(mDrawerOpen);				
					}
					return false;
				}
	    		
	    	});
    	}
    	
    	ScrollingMovementMethod smm = new ScrollingMovementMethod();
    	
        mTxtOrbotLog.setMovementMethod(smm);
        mTxtOrbotLog.setOnLongClickListener(new View.OnLongClickListener() {
         

			@Override
			public boolean onLongClick(View v) {
				  ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
	                cm.setText(mTxtOrbotLog.getText());
	                Toast.makeText(Orbot.this, "LOG COPIED TO CLIPBOARD", Toast.LENGTH_SHORT).show();
	            return true;
			}
        });
        
		downloadText.setText(formatCount(0) + " / " + formatTotal(0));
		uploadText.setText(formatCount(0) + " / " + formatTotal(0));
	
        // Gesture detection
		mGestureDetector = new GestureDetector(this, new MyGestureDetector());
		

    }
	
	GestureDetector mGestureDetector;
    

	@Override
	public boolean onTouch(View v, MotionEvent event) {
	    return mGestureDetector.onTouchEvent(event);

	}
   	
    private void appendLogTextAndScroll(String text)
    {
        if(mTxtOrbotLog != null && text != null && text.length() > 0){
        	
        	if (mTxtOrbotLog.getText().length() > MAX_LOG_LENGTH)
        		mTxtOrbotLog.setText("");
        	
        	mTxtOrbotLog.append(text + "\n");
            final Layout layout = mTxtOrbotLog.getLayout();
            if(layout != null){
                int scrollDelta = layout.getLineBottom(mTxtOrbotLog.getLineCount() - 1) 
                    - mTxtOrbotLog.getScrollY() - mTxtOrbotLog.getHeight();
                if(scrollDelta > 0)
                	mTxtOrbotLog.scrollBy(0, scrollDelta);
            }
        }
    }
    
   /*
    * Create the UI Options Menu (non-Javadoc)
    * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
       
        mItemOnOff = menu.getItem(0);
        
        /*
        startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                // Inflate our menu from a resource file
             //   actionMode.getMenuInflater().inflate(R.menu.action_mode_main, menu);

                // Return true so that the action mode is shown
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                // As we do not need to modify the menu before displayed, we return false.
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                // Similar to menu handling in Activity.onOptionsItemSelected()
                switch (menuItem.getItemId()) {
                    case R.id.menu_start:
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
                        return true;
                    case R.id.menu_settings:
                    	 showSettings();
                    	 return true;
                    case R.id.menu_wizard:
                          startWizard();
                          return true;
                    case R.id.menu_verify:
                          doTorCheck();
                          return true;
                    case R.id.menu_exit:
                          doExit();
                          return true;        
                    case R.id.menu_about:
                          showAbout();
                          return true;
                }

                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                // Allows you to be notified when the action mode is dismissed
            }
        });*/
        
        return true;
    }
    
    
    private void appConflictChecker ()
    {
    	SharedPreferences sprefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
    	
    	boolean showAppConflict = true;//sprefs.getBoolean("pref_show_conflict",true);
    	
    	String[] badApps = {"com.sec.msc.nts.android.proxy:com.sec.msc.nts.android.proxy","com.sec.pcw:Samsung Link"};
    	
    	for (String badApp : badApps)
    	{
    		String[] badAppParts = badApp.split(":");
    		
    		if (appInstalledOrNot(badAppParts[0]))
    		{
    			String msg = getString(R.string.please_disable_this_app_in_android_settings_apps_if_you_are_having_problems_with_orbot_) + badAppParts[1];
    			
    			if (showAppConflict)
    				showAlert(getString(R.string.app_conflict),msg,true);
	    	
	    		appendLogTextAndScroll(msg);
    		}
    	}
    	
    	sprefs.edit().putBoolean("pref_show_conflict", false).commit();
	
    }
    
    
    

    private void showAbout ()
        {
                
	        LayoutInflater li = LayoutInflater.from(this);
	        View view = li.inflate(R.layout.layout_about, null); 
	        
	        String version = "";
	        
	        try {
	        	version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " (Tor " + TorServiceConstants.BINARY_TOR_VERSION + ")";
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
    
    	@Override
        public boolean onOptionsItemSelected(MenuItem item) {
                
                super.onOptionsItemSelected(item);
                
                if (item.getItemId() == R.id.menu_start)
                {
                        
                        try
                        {
                                
                                if (torStatus == TorServiceConstants.STATUS_OFF)
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
            		startActivity(new Intent(this, ChooseLocaleWizardActivity.class));

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
                else if (item.getItemId() == R.id.menu_vpn)
                {
                	this.startVpnService();
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
                        
                        //onDestroy();
                        
                        
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
		try
		{
			super.onPause();
	
			if (aDialog != null)
				aDialog.dismiss();
		}
		catch (IllegalStateException ise)
		{
			//can happen on exit/shutdown
		}
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
		
		Editor pEdit = mPrefs.edit();
		
		String hsPortString = mPrefs.getString("pref_hs_ports", "");
		
		if (hsPortString.length() > 0 && hsPortString.indexOf(hsPort+"")==-1)
			hsPortString += ',' + hsPort;
		else
			hsPortString = hsPort + "";
		
		pEdit.putString("pref_hs_ports", hsPortString);
		pEdit.putBoolean("pref_hs_enable", true);
		
		pEdit.commit();
		
		String onionHostname = mPrefs.getString("pref_hs_hostname","");

		while (onionHostname.length() == 0)
		{
			//we need to stop and start Tor
			try {
				stopTor();
				
				Thread.sleep(3000); //wait three seconds
				
				startTor();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			 onionHostname = mPrefs.getString("pref_hs_hostname","");
		}
		
		Intent nResult = new Intent();
		nResult.putExtra("hs_host", onionHostname);
		setResult(RESULT_OK, nResult);
	
	}


	private synchronized void handleIntents ()
	{
		if (getIntent() == null)
			return;
		
	    // Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();
		
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
			autoStartFromIntent = true;
				
				try {
					startTor();

					Intent nResult = new Intent();
					
					//nResult.putExtra("socks", ); //TODO respond with socks, transport, dns, etc
					
					setResult(RESULT_OK,nResult);
					
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
		}
		else if (action.equals(Intent.ACTION_VIEW))
		{
			String urlString = intent.getDataString();
			
			if (urlString != null)
			{
				
				if (urlString.toLowerCase().startsWith("bridge://"))

				{
					String newBridgeValue = urlString.substring(9); //remove the bridge protocol piece
					newBridgeValue = URLDecoder.decode(newBridgeValue); //decode the value here
		
					showAlert("Bridges Updated","Restart Orbot to use this bridge: " + newBridgeValue,false);	
					
					String bridges = mPrefs.getString(TorConstants.PREF_BRIDGES_LIST, null);
					
					Editor pEdit = mPrefs.edit();
					
					if (bridges != null && bridges.trim().length() > 0)
					{
						if (bridges.indexOf('\n')!=-1)
							bridges += '\n' + newBridgeValue;
						else
							bridges += ',' + newBridgeValue;
					}
					else
						bridges = newBridgeValue;
					
					pEdit.putString(TorConstants.PREF_BRIDGES_LIST,bridges); //set the string to a preference
					pEdit.putBoolean(TorConstants.PREF_BRIDGES_ENABLED,true);
				
					pEdit.commit();
					
					setResult(RESULT_OK);
				}
			}
		}
		else
		{
		
			showWizard = mPrefs.getBoolean("show_wizard",showWizard);
			
			if (showWizard)
			{
				Editor pEdit = mPrefs.edit();
				pEdit.putBoolean("show_wizard",false);
				pEdit.commit();				
				showWizard = false;

				startActivity(new Intent(this, ChooseLocaleWizardActivity.class));

			}
			
		}
		
		setIntent(null);
		
		updateStatus ("");
		
	}

	private boolean showWizard = true;
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		doLayout();
		updateStatus("");
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	protected void onStop() {
		super.onStop();
		
	}



	/*
	 * Launch the system activity for Uri viewing with the provided url
	 */
	private void openBrowser(final String browserLaunchUrl)
	{
		boolean isOrwebInstalled = appInstalledOrNot("info.guardianproject.browser");
		boolean isTransProxy =  mPrefs.getBoolean("pref_transparent", false);
		
		if (isOrwebInstalled)
		{
			startIntent("info.guardianproject.browser",Intent.ACTION_VIEW,Uri.parse(browserLaunchUrl));						
		}
		else if (isTransProxy)
		{
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserLaunchUrl));
			intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		else
		{
			AlertDialog aDialog = new AlertDialog.Builder(Orbot.this)
              .setIcon(R.drawable.onion32)
		      .setTitle(R.string.install_apps_)
		      .setMessage(R.string.it_doesn_t_seem_like_you_have_orweb_installed_want_help_with_that_or_should_we_just_open_the_browser_)
		      .setPositiveButton(android.R.string.ok, new OnClickListener ()
		      {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					//prompt to install Orweb
					Intent intent = new Intent(Orbot.this,TipsAndTricks.class);
					startActivity(intent);
					
				}
		    	  
		      })
		      .setNegativeButton(android.R.string.no, new OnClickListener ()
		      {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserLaunchUrl));
					intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					
				}
		    	  
		      })
		      .show();
			  
		}
		
	}
	
	private void startIntent (String pkg, String action, Uri data)
	{
		Intent i;
		PackageManager manager = getPackageManager();
		try {
		    i = manager.getLaunchIntentForPackage(pkg);
		    if (i == null)
		        throw new PackageManager.NameNotFoundException();		    
		    i.setAction(action);
		    i.setData(data);
		    startActivity(i);
		} catch (PackageManager.NameNotFoundException e) {

		}
	}
	
	private boolean appInstalledOrNot(String uri)
    {
        PackageManager pm = getPackageManager();
        try
        {
               PackageInfo pi = pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);               
               return pi.applicationInfo.enabled;
        }
        catch (PackageManager.NameNotFoundException e)
        {
              return false;
        }
}
	
    /*
     * Load the basic settings application to display torrc
     */
    private void showSettings ()
    {
            
            startActivityForResult(new Intent(this, SettingsPreferences.class), 1);
    }
    
    
    @Override
	protected void onActivityResult(int request, int response, Intent data) {
		super.onActivityResult(request, response, data);
		
		
		if (request == 1 && response == RESULT_OK)
		{
			if (data != null && data.getBooleanExtra("transproxywipe", false))
			{
					
					boolean result = flushTransProxy();
					
					if (result)
					{

			    		Toast.makeText(this, R.string.transparent_proxy_rules_flushed_, Toast.LENGTH_SHORT).show();
				 		
					}
					else
					{

			    		Toast.makeText(this, R.string.you_do_not_have_root_access_enabled, Toast.LENGTH_SHORT).show();
				 		
					}
				
			}
			else if (torStatus == TorServiceConstants.STATUS_ON)
			{
				updateSettings();
	    		Toast.makeText(this, R.string.you_may_need_to_stop_and_start_orbot_for_settings_change_to_be_enabled_, Toast.LENGTH_SHORT).show();

			}
		}
		else if (request == REQUEST_VPN && response == RESULT_OK)
		{
			startService("vpn");
		}
		
	}
    
    private final static int REQUEST_VPN = 8888;
    
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void startVpnService ()
    {
    	 Intent intent = VpnService.prepare(this);
	        if (intent != null) {
	            startActivityForResult(intent,REQUEST_VPN);

	        } 
    }

    private boolean flushTransProxy ()
    {
    	startService("flush");
    	return true;
    }
    
    private boolean updateSettings ()
    {
    	//todo send service command
    	startService("update");
    	return true;
    }

	@Override
	protected void onResume() {
		super.onResume();

		setLocale();
		
		handleIntents();

           
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
                     .setIcon(R.drawable.onion32)
             .setTitle(title)
             .setMessage(msg)
             .setPositiveButton(android.R.string.ok, null)
             .show();
             }
             else
             {
                     aDialog = new AlertDialog.Builder(Orbot.this)
                     .setIcon(R.drawable.onion32)
             .setTitle(title)
             .setMessage(msg)
             .show();
             }
    
             aDialog.setCanceledOnTouchOutside(true);
    }
    
	private void updateStatus (String torServiceMsg)
    {
        
            //now update the layout_main UI based on the status
            if (imgStatus != null)
            {
                    
                    if (torStatus == TorServiceConstants.STATUS_ON)
                    {
                        	
                            imgStatus.setImageResource(R.drawable.toron);
                    		
                            String lblMsg = getString(R.string.status_activated);                                     
                            lblStatus.setText(lblMsg);

                            if (mItemOnOff != null)
                                    mItemOnOff.setTitle(R.string.menu_stop);
                            
                        
                            if (torServiceMsg != null && torServiceMsg.length() > 0)
                            {
                            	appendLogTextAndScroll(torServiceMsg);
                            }
                            
                            boolean showFirstTime = mPrefs.getBoolean("connect_first_time",true);
                            
                            if (showFirstTime)
                            {
                            
                                    Editor pEdit = mPrefs.edit();
                                    
                                    pEdit.putBoolean("connect_first_time",false);
                                    
                                    pEdit.commit();
                                    
                                    showAlert(getString(R.string.status_activated),getString(R.string.connect_first_time),true);
                                    
                            }
                            
                            
                            if (autoStartFromIntent)
                            {
                            	setResult(RESULT_OK);
                            	finish();
                            }

                    }
                    else if (torStatus == TorServiceConstants.STATUS_CONNECTING)
                    {
                    	
                        imgStatus.setImageResource(R.drawable.torstarting);
                
                        if (mItemOnOff != null)
                                mItemOnOff.setTitle(R.string.menu_stop);
                	
                    	
                        if (lblStatus != null && torServiceMsg != null)
                        	if (torServiceMsg.indexOf('%')!=-1)
                        		lblStatus.setText(torServiceMsg);
                        
                        appendLogTextAndScroll(torServiceMsg);
                        
                                    
                    }
                    else if (torStatus == TorServiceConstants.STATUS_OFF)
                    {
                        imgStatus.setImageResource(R.drawable.toroff);
                        lblStatus.setText(getString(R.string.status_disabled) + "\n" + getString(R.string.press_to_start));
                        
                        if (mItemOnOff != null)
                                mItemOnOff.setTitle(R.string.menu_start);
                        
                    }
            }
                
           
    }
		
         
  
  // guess what? this start's Tor! actually no it just requests via the local ITorService to the remote TorService instance
  // to start Tor
    private void startTor () throws RemoteException
    {
            

		startService ("start");
		torStatus = TorServiceConstants.STATUS_CONNECTING;
				
		mTxtOrbotLog.setText("");
		

        // this is a bit of a strange/old/borrowed code/design i used to change the service state
        // not sure it really makes sense when what we want to say is just "startTor"
//        mService.setProfile(TorServiceConstants.STATUS_ON); //this means turn on
            
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
    	
    	startService ("stop");
		torStatus = TorServiceConstants.STATUS_OFF;

    	//	mService.setProfile(TorServiceConstants.STATUS_OFF);
    		Message msg = mHandler.obtainMessage(TorServiceConstants.DISABLE_TOR_MSG);
    		mHandler.sendMessage(msg);
    		
    
    }
    
        /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
        public boolean onLongClick(View view) {
                
        	if (!mDrawerOpen)
        	{
	            try
	            {
	                    
	                if (torStatus == TorServiceConstants.STATUS_OFF)
	                {
	
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

        	}
        	
            return false;
                    
        }

    	Thread threadUpdater = null;
    	boolean mKeepUpdating = false;
    	
        public void initUpdates ()
        {
        	mKeepUpdating = true;
        	
        	if (threadUpdater == null || !threadUpdater.isAlive())
        	{
        		threadUpdater = new Thread(new Runnable()
        		{
        				
        			public void run ()
        			{
        				
        				while (mKeepUpdating)
        				{
        					try
        					{
        						/**
	        					if (mService != null)
	        					{
	        						for (String log : mService.getLog())
	        						{
	        							Message msg = mHandler.obtainMessage(TorServiceConstants.LOG_MSG);
	        				             msg.getData().putString(HANDLER_TOR_MSG, log);
	        				             mHandler.sendMessage(msg);
	        						}
	        						
	        						for (String status : mService.getStatusMessage())
	        						{
	        							Message msg = mHandler.obtainMessage(TorServiceConstants.STATUS_MSG);
	        				             msg.getData().putString(HANDLER_TOR_MSG, status);
	        				             mHandler.sendMessage(msg);
	        						}
	        						
	        						if (mService != null)
	        						{
		        						long[] bws = mService.getBandwidth();
		        						Message msg = mHandler.obtainMessage(TorServiceConstants.MESSAGE_TRAFFIC_COUNT);
		        						msg.getData().putLong("download", bws[0]);
		        						msg.getData().putLong("upload", bws[1]);
		        						msg.getData().putLong("readTotal", bws[2]);
		        						msg.getData().putLong("writeTotal", bws[3]);
		        						mHandler.sendMessage(msg);
	       				             	
		        						try { Thread.sleep(1000); }
		        						catch (Exception e){}
	        						}		        						

	        						if (mService != null)
	        							torStatus = mService.getStatus();
	        					}**/
        					}
        					catch (Exception re)
        					{
        						Log.e(TAG, "error getting service updates",re);
        					}
        				}
        				
        			}
        		});
        		
        		threadUpdater.start();
        		
        	}
        }
        
   

// this is what takes messages or values from the callback threads or other non-mainUI threads
//and passes them back into the main UI thread for display to the user
    private Handler mHandler = new Handler() {
    	
    	private String lastServiceMsg = null;
    	
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TorServiceConstants.STATUS_MSG:
                case TorServiceConstants.LOG_MSG:

                        String torServiceMsg = (String)msg.getData().getString(HANDLER_TOR_MSG);
                        
                        if (lastServiceMsg == null || !lastServiceMsg.equals(torServiceMsg))
                        {
                        	updateStatus(torServiceMsg);
                        
                        	lastServiceMsg = torServiceMsg;
                        }
                        
                    break;
                case TorServiceConstants.ENABLE_TOR_MSG:
                        
                        
                        updateStatus((String)msg.getData().getString(HANDLER_TOR_MSG));
                        
                        break;
                case TorServiceConstants.DISABLE_TOR_MSG:
                	
                	updateStatus((String)msg.getData().getString(HANDLER_TOR_MSG));
                	
                	break;
                	

            	case TorServiceConstants.MESSAGE_TRAFFIC_COUNT :
                    
            		Bundle data = msg.getData();
            		DataCount datacount =  new DataCount(data.getLong("upload"),data.getLong("download"));     
            		
            		long totalRead = data.getLong("readTotal");
            		long totalWrite = data.getLong("writeTotal");
            	
        			downloadText.setText(formatCount(datacount.Download) + " / " + formatTotal(totalRead));
            		uploadText.setText(formatCount(datacount.Upload) + " / " + formatTotal(totalWrite));
            
            		if (torStatus != TorServiceConstants.STATUS_ON)
            		{
            			updateStatus("");
            		}
                		
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
    
    private void setLocale ()
    {
    	

        Configuration config = getResources().getConfiguration();
        String lang = mPrefs.getString(PREF_DEFAULT_LOCALE, "");
        
        if (! "".equals(lang) && ! config.locale.getLanguage().equals(lang))
        {
        	Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }
    }

   	@Override
	protected void onDestroy() {
		super.onDestroy();
		  LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

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
		if (count < 1e6)
			return ((float)((int)(count*10/1024))/10 + "kbps");
		return ((float)((int)(count*100/1024/1024))/100 + "mbps");
		
   		//return count+" kB";
	}
   	
   	private String formatTotal(long count) {
		// Converts the supplied argument into a string.
		// Under 2Mb, returns "xxx.xKb"
		// Over 2Mb, returns "xxx.xxMb"
		if (count < 1e6)
			return ((float)((int)(count*10/1024))/10 + "KB");
		return ((float)((int)(count*100/1024/1024))/100 + "MB");
		
   		//return count+" kB";
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	
		
	}
	
	  private static final float ROTATE_FROM = 0.0f;
	    private static final float ROTATE_TO = 360.0f*4f;// 3.141592654f * 32.0f;

	public void spinOrbot (float direction)
	{
			startService ("newnym");
		
			//mService.newIdentity(); //request a new identity
			//TODO trigger newnym
		
			Toast.makeText(this, R.string.newnym, Toast.LENGTH_SHORT).show();
			
		//	Rotate3dAnimation rotation = new Rotate3dAnimation(ROTATE_FROM, ROTATE_TO*direction, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			 Rotate3dAnimation rotation = new Rotate3dAnimation(ROTATE_FROM, ROTATE_TO*direction, imgStatus.getWidth()/2f,imgStatus.getWidth()/2f,20f,false);
			 rotation.setFillAfter(true);
			  rotation.setInterpolator(new AccelerateInterpolator());
			  rotation.setDuration((long) 2*1000);
			  rotation.setRepeatCount(0);
			  imgStatus.startAnimation(rotation);
			  
		
	}
	
	 class MyGestureDetector extends SimpleOnGestureListener {
	        @Override
	        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	            try {	            	
	            	if (torStatus == TorServiceConstants.STATUS_ON)
	            	{
	            		float direction = 1f;
	            		if (velocityX < 0)
	            			direction = -1f;
	            		spinOrbot (direction);
	            	}
	            } catch (Exception e) {
	                // nothing
	            }
	            return false;
	        }

	    }


}
