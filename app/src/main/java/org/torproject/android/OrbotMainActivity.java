/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.TorServiceUtils;
import org.torproject.android.settings.LocaleHelper;
import org.torproject.android.settings.SettingsPreferences;
import org.torproject.android.ui.AppManagerActivity;
import org.torproject.android.ui.onboarding.BridgeWizardActivity;
import org.torproject.android.ui.onboarding.OnboardingActivity;
import org.torproject.android.ui.hiddenservices.ClientCookiesActivity;
import org.torproject.android.ui.hiddenservices.HiddenServicesActivity;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;
import org.torproject.android.vpn.VPNEnableActivity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;

import static android.support.v4.content.FileProvider.getUriForFile;
import static org.torproject.android.binary.TorServiceConstants.BINARY_TOR_VERSION;

public class OrbotMainActivity extends AppCompatActivity
        implements OrbotConstants, OnLongClickListener {

    /* Useful UI bits */
    private TextView lblStatus = null; //the main text display widget
    private ImageView imgStatus = null; //the main touchable image for activating Orbot

    private TextView downloadText = null;
    private TextView uploadText = null;
    private TextView mTxtOrbotLog = null;

	private Button mBtnStart = null;

	private SwitchCompat mBtnVPN = null;
    private SwitchCompat mBtnBridges = null;
    
    private Spinner spnCountries = null;

	private DrawerLayout mDrawer;

    /* Some tracking bits */
    private String torStatus = null; //latest status reported from the tor service
    private Intent lastStatusIntent;  // the last ACTION_STATUS Intent received

    private SharedPreferences mPrefs = null;

    private boolean autoStartFromIntent = false;
    
    private final static int REQUEST_VPN = 8888;
    private final static int REQUEST_SETTINGS = 0x9874;
    private final static int REQUEST_VPN_APPS_SELECT = 8889;

    private final static int LOG_DRAWER_GRAVITY = Gravity.END;

    // message types for mStatusUpdateHandler
    private final static int STATUS_UPDATE = 1;
    private static final int MESSAGE_TRAFFIC_COUNT = 2;

	public final static String INTENT_ACTION_REQUEST_HIDDEN_SERVICE = "org.torproject.android.REQUEST_HS_PORT";
	public final static String INTENT_ACTION_REQUEST_START_TOR = "org.torproject.android.START_TOR";


    PulsatorLayout mPulsator;


    //this is needed for backwards compat back to Android 2.3.*
    @SuppressLint("NewApi")
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs)
    {
        if(Build.VERSION.SDK_INT >= 11)
          return super.onCreateView(parent, name, context, attrs);
        return null;
    }

    private void migratePreferences() {
        String hsPortString = mPrefs.getString("pref_hs_ports", "");
        if (hsPortString.length() > 0) {
            StringTokenizer st = new StringTokenizer(hsPortString, ",");
            ContentResolver cr = getContentResolver();
            while (st.hasMoreTokens()) {
                int hsPort = Integer.parseInt(st.nextToken().split(" ")[0]);
                ContentValues fields = new ContentValues();
                fields.put("name", hsPort);
                fields.put("port", hsPort);
                fields.put("onion_port", hsPort);
                cr.insert(HSContentProvider.CONTENT_URI, fields);
            }

            Editor pEdit = mPrefs.edit();
            pEdit.remove("pref_hs_ports");
            pEdit.commit();
        }
    }

    /**
     * Called when the activity is first created.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        migratePreferences(); // Migrate old preferences

        /* Create the widgets before registering for broadcasts to guarantee
         * that the widgets exist when the status updates try to update them */
    	doLayout();

    	/* receive the internal status broadcasts, which are separate from the public
    	 * status broadcasts to prevent other apps from sending fake/wrong status
    	 * info to this app */
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mLocalBroadcastReceiver,
                new IntentFilter(TorServiceConstants.ACTION_STATUS));
        lbm.registerReceiver(mLocalBroadcastReceiver,
                new IntentFilter(TorServiceConstants.LOCAL_ACTION_BANDWIDTH));
        lbm.registerReceiver(mLocalBroadcastReceiver,
                new IntentFilter(TorServiceConstants.LOCAL_ACTION_LOG));

        boolean showFirstTime = mPrefs.getBoolean("connect_first_time", true);

        if (showFirstTime)
        {
            Editor pEdit = mPrefs.edit();
            pEdit.putBoolean("connect_first_time", false);
            pEdit.commit();
            startActivity(new Intent(this,OnboardingActivity.class));
        }


    }

	private void sendIntentToService(final String action) {

		Intent torService = new Intent(OrbotMainActivity.this, TorService.class);
        torService.setAction(action);
        startService(torService);

	}

    private void stopTor() {

//        requestTorStatus();

        Intent torService = new Intent(OrbotMainActivity.this, TorService.class);
        stopService(torService);

    }

    /**
     * The state and log info from {@link TorService} are sent to the UI here in
     * the form of a local broadcast. Regular broadcasts can be sent by any app,
     * so local ones are used here so other apps cannot interfere with Orbot's
     * operation.
     */
    private BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;

            if (action.equals(TorServiceConstants.LOCAL_ACTION_LOG)) {
                Message msg = mStatusUpdateHandler.obtainMessage(STATUS_UPDATE);
                msg.obj = intent.getStringExtra(TorServiceConstants.LOCAL_EXTRA_LOG);
                msg.getData().putString("status", intent.getStringExtra(TorServiceConstants.EXTRA_STATUS));
                mStatusUpdateHandler.sendMessage(msg);

            } else if (action.equals(TorServiceConstants.LOCAL_ACTION_BANDWIDTH)) {
                long upload = intent.getLongExtra("up", 0);
                long download = intent.getLongExtra("down", 0);
                long written = intent.getLongExtra("written", 0);
                long read = intent.getLongExtra("read", 0);

                Message msg = mStatusUpdateHandler.obtainMessage(MESSAGE_TRAFFIC_COUNT);
                msg.getData().putLong("download", download);
                msg.getData().putLong("upload", upload);
                msg.getData().putLong("readTotal", read);
                msg.getData().putLong("writeTotal", written);
                msg.getData().putString("status", intent.getStringExtra(TorServiceConstants.EXTRA_STATUS));

                mStatusUpdateHandler.sendMessage(msg);

            } else if (action.equals(TorServiceConstants.ACTION_STATUS)) {
                lastStatusIntent = intent;
                
                Message msg = mStatusUpdateHandler.obtainMessage(STATUS_UPDATE);
                msg.getData().putString("status", intent.getStringExtra(TorServiceConstants.EXTRA_STATUS));

                mStatusUpdateHandler.sendMessage(msg);
            }
        }
    };
 
    private void doLayout ()
    {
        setContentView(R.layout.layout_main);
        
        setTitle(R.string.app_name);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        mTxtOrbotLog = (TextView)findViewById(R.id.orbotLog);
        
        lblStatus = (TextView)findViewById(R.id.lblStatus);
        lblStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawer.openDrawer(LOG_DRAWER_GRAVITY);
            }
        });

        imgStatus = (ImageView)findViewById(R.id.imgStatus);
        imgStatus.setOnLongClickListener(this);

        downloadText = (TextView)findViewById(R.id.trafficDown);
        uploadText = (TextView)findViewById(R.id.trafficUp);

        downloadText.setText(formatCount(0) + " / " + formatTotal(0));
        uploadText.setText(formatCount(0) + " / " + formatTotal(0));

		mBtnStart =(Button)findViewById(R.id.btnStart);
		mBtnStart.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v) {

				if (torStatus == TorServiceConstants.STATUS_OFF) {
					lblStatus.setText(getString(R.string.status_starting_up));
					startTor();
				} else {
					lblStatus.setText(getString(R.string.status_shutting_down));
					stopTor();
				}
			}
		});

		mBtnVPN = (SwitchCompat)findViewById(R.id.btnVPN);
		
		boolean canDoVPN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

		if (!canDoVPN)
		{
			//if not SDK 14 or higher, we can't use the VPN feature
			mBtnVPN.setVisibility(View.GONE);
		}
		else
		{
			boolean useVPN = Prefs.useVpn();
			mBtnVPN.setChecked(useVPN);
			
			//auto start VPN if VPN is enabled
			if (useVPN)
			{
				startActivity(new Intent(OrbotMainActivity.this,VPNEnableActivity.class));
			}
			
			mBtnVPN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    enableVPN(isChecked);
                }
            });

		}

		mBtnBridges = (SwitchCompat)findViewById(R.id.btnBridges);
		mBtnBridges.setChecked(Prefs.bridgesEnabled());
		mBtnBridges.setOnClickListener(new View.OnClickListener ()
		{

			@Override
			public void onClick(View v) {
				promptSetupBridges (); //if ARM processor, show all bridge options
			}

			
		});

		String currentExit = Prefs.getExitNodes();
		int selIdx = -1;
		
		ArrayList<String> cList = new ArrayList<String>();
		cList.add(0, getString(R.string.vpn_default_world));
	
		for (int i = 0; i < TorServiceConstants.COUNTRY_CODES.length; i++)
		{
			Locale locale = new Locale("",TorServiceConstants.COUNTRY_CODES[i]);
			cList.add(locale.getDisplayCountry());
			
			if (currentExit.contains(TorServiceConstants.COUNTRY_CODES[i]))
				selIdx = i+1;
		}
		
		spnCountries = (Spinner)findViewById(R.id.spinnerCountry);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, cList);
		spnCountries.setAdapter(adapter);
		
		if (selIdx != -1)
			spnCountries.setSelection(selIdx);
		
		spnCountries.setOnItemSelectedListener(new OnItemSelectedListener() {
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
		        // your code here
		    	
		    	String country = null;
		    	
		    	if (position == 0)
		    		country = "";
		    	else
		    		country =  '{' + TorServiceConstants.COUNTRY_CODES[position-1] + '}';
		    	
		    	Intent torService = new Intent(OrbotMainActivity.this, TorService.class);    
				torService.setAction(TorServiceConstants.CMD_SET_EXIT);
				torService.putExtra("exit",country);
				startService(torService);
	    	
		    }

		    @Override
		    public void onNothingSelected(AdapterView<?> parentView) {
		        // your code here
		    }

		});


        mPulsator = (PulsatorLayout) findViewById(R.id.pulsator);

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

   /*
    * Create the UI Options Menu (non-Javadoc)
    * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.orbot_main, menu);

        //We don't support these on per SDK 23
        if (BuildConfig.FLAVOR.equals("minimalperm")) {
            menu.findItem(R.id.menu_hidden_services_main).setVisible(false);
        }

        return true;
    }
    
    

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
    	 if (item.getItemId() == R.id.menu_settings)
         {
             Intent intent = new Intent(OrbotMainActivity.this, SettingsPreferences.class);
             startActivityForResult(intent, REQUEST_SETTINGS);
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
         else if (item.getItemId() == R.id.menu_scan)
         {
         	IntentIntegrator integrator = new IntentIntegrator(OrbotMainActivity.this);
         	integrator.initiateScan();
         }
         else if (item.getItemId() == R.id.menu_share_bridge)
         {
         	
     		String bridges = Prefs.getBridgesList();
         	
     		if (bridges != null && bridges.length() > 0)
     		{
         		try {
						bridges = "bridge://" + URLEncoder.encode(bridges,"UTF-8");
	            		
	                	IntentIntegrator integrator = new IntentIntegrator(OrbotMainActivity.this);
	                	integrator.shareText(bridges);
	                	
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
     		}

         } else if (item.getItemId() == R.id.menu_hidden_services) {
             startActivity(new Intent(this, HiddenServicesActivity.class));
         } else if (item.getItemId() == R.id.menu_client_cookies) {
             startActivity(new Intent(this, ClientCookiesActivity.class));
         }
     
		return super.onOptionsItemSelected(item);
	}

	private void showAbout ()
        {
                
            LayoutInflater li = LayoutInflater.from(this);
            View view = li.inflate(R.layout.layout_about, null); 
            
            String version = "";
            
            try {
                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " (Tor " + BINARY_TOR_VERSION + ")";
            } catch (NameNotFoundException e) {
                version = "Version Not Found";
            }
            
            TextView versionName = (TextView)view.findViewById(R.id.versionName);
            versionName.setText(version);

            TextView aboutOther = (TextView)view.findViewById(R.id.aboutother);

            try
            {
                String aboutText = readFromAssets(this,"LICENSE");
                aboutText = aboutText.replace("\n","<br/>");
                aboutOther.setText(Html.fromHtml(aboutText));
            }
            catch (Exception e){}
            
                    new AlertDialog.Builder(this)
            .setTitle(getString(R.string.button_about))
            .setView(view)
            .show();
        }

    private static String readFromAssets(Context context, String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

        // do reading, usually loop until end of file reading
        StringBuilder sb = new StringBuilder();
        String mLine = reader.readLine();
        while (mLine != null) {
            sb.append(mLine + '\n'); // process line
            mLine = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }


    /**
     * This is our attempt to REALLY exit Orbot, and stop the background service
     * However, Android doesn't like people "quitting" apps, and/or our code may
     * not be quite right b/c no matter what we do, it seems like the TorService
     * still exists
     **/
    private void doExit() {
        stopTor();

        // Kill all the wizard activities
        setResult(RESULT_CLOSE_ALL);
        finish();
    }

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


    @Override
    public void onBackPressed() {
        // check to see if the log is open, if so close it
        if (mDrawer.isDrawerOpen(LOG_DRAWER_GRAVITY)) {
            mDrawer.closeDrawers();
        }
        else {
            super.onBackPressed();
        }
    }

	private void refreshVPNApps ()
    {
        stopVpnService();
        startActivity(new Intent(OrbotMainActivity.this, VPNEnableActivity.class));
    }

    private void enableVPN (boolean enable)
    {
        Prefs.putUseVpn(enable);

        if (enable) {
            startActivity(new Intent(OrbotMainActivity.this, VPNEnableActivity.class));
        } else
            stopVpnService();

        addAppShortcuts();
    }

    private void enableHiddenServicePort(
            String hsName, final int hsPort, int hsRemotePort,
            final String backupToPackage, final Uri hsKeyPath,
            final Boolean authCookie
    ) throws RemoteException, InterruptedException {

        String onionHostname = null;

        if (hsName == null)
            hsName = "hs" + hsPort;

        if (hsRemotePort == -1)
            hsRemotePort = hsPort;

        ContentValues fields = new ContentValues();
        fields.put(HSContentProvider.HiddenService.NAME, hsName);
        fields.put(HSContentProvider.HiddenService.PORT, hsPort);
        fields.put(HSContentProvider.HiddenService.ONION_PORT, hsRemotePort);
        fields.put(HSContentProvider.HiddenService.AUTH_COOKIE, authCookie);

        ContentResolver cr = getContentResolver();

        Cursor row = cr.query(
                HSContentProvider.CONTENT_URI,
                HSContentProvider.PROJECTION,
                HSContentProvider.HiddenService.ONION_PORT + "=" + hsPort,
                null,
                null
        );

        if (row == null || row.getCount() < 1) {
            cr.insert(HSContentProvider.CONTENT_URI, fields);
        } else if (row.moveToFirst()) {
            onionHostname = row.getString(row.getColumnIndex(HSContentProvider.HiddenService.DOMAIN));
            row.close();
        }

        if (onionHostname == null || onionHostname.length() < 1) {

            if (hsKeyPath != null) {
                BackupUtils hsutils = new BackupUtils(getApplicationContext());
                hsutils.restoreKeyBackup(hsPort, hsKeyPath);
            }

            if (torStatus.equals(TorServiceConstants.STATUS_OFF)) {
                startTor();
            } else {
                stopTor();
                Toast.makeText(
                        this, R.string.start_tor_again_for_finish_the_process, Toast.LENGTH_LONG
                ).show();
            }

            new Thread() {

                public void run() {
                    String hostname = null;
                    Intent nResult = new Intent();

                    while (hostname == null) {
                        try {
                            Thread.sleep(3000); //wait three seconds
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        Cursor onion = getContentResolver().query(
                                HSContentProvider.CONTENT_URI,
                                HSContentProvider.PROJECTION,
                                HSContentProvider.HiddenService.ONION_PORT + "=" + hsPort,
                                null,
                                null
                        );

                        if (onion != null && onion.getCount() > 0) {
                            onion.moveToNext();
                            hostname = onion.getString(onion.getColumnIndex(HSContentProvider.HiddenService.DOMAIN));

                            if(hostname == null || hostname.length() < 1)
                                continue;

                            nResult.putExtra("hs_host", hostname);

                            if (authCookie) {
                                nResult.putExtra(
                                        "hs_auth_cookie",
                                        onion.getString(onion.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE_VALUE))
                                );
                            }

                            if (backupToPackage != null && backupToPackage.length() > 0) {
                                String servicePath = getFilesDir() + "/" + TorServiceConstants.HIDDEN_SERVICES_DIR + "/hs" + hsPort;
                                File hidden_service_key = new File(servicePath, "private_key");
                                Context context = getApplicationContext();

                                Uri contentUri = getUriForFile(
                                        context,
                                        "org.torproject.android.ui.hiddenservices.storage",
                                        hidden_service_key
                                );

                                context.grantUriPermission(backupToPackage, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                nResult.setData(contentUri);
                                nResult.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            }

                            onion.close();
                            setResult(RESULT_OK, nResult);
                            finish();
                        }
                    }
                }
            }.start();

        } else {
            Intent nResult = new Intent();
            nResult.putExtra("hs_host", onionHostname);
            setResult(RESULT_OK, nResult);
            finish();
        }
    }

    private synchronized void handleIntents() {
        if (getIntent() == null)
            return;

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();

        if (action == null)
            return;

        switch (action) {
            case INTENT_ACTION_REQUEST_HIDDEN_SERVICE:
                final int hiddenServicePort = intent.getIntExtra("hs_port", -1);
                final int hiddenServiceRemotePort = intent.getIntExtra("hs_onion_port", -1);
                final String hiddenServiceName = intent.getStringExtra("hs_name");
                final String backupToPackage = intent.getStringExtra("hs_backup_to_package");
                final Boolean authCookie = intent.getBooleanExtra("hs_auth_cookie", false);
                final Uri mKeyUri = intent.getData();

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                try {
                                    enableHiddenServicePort(
                                            hiddenServiceName, hiddenServicePort,
                                            hiddenServiceRemotePort, backupToPackage,
                                            mKeyUri, authCookie
                                    );
                                } catch (RemoteException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                                break;
                        }
                    }
                };

                String requestMsg = getString(R.string.hidden_service_request, hiddenServicePort);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(requestMsg).setPositiveButton("Allow", dialogClickListener)
                        .setNegativeButton("Deny", dialogClickListener).show();

                return; //don't null the setIntent() as we need it later

            case INTENT_ACTION_REQUEST_START_TOR:
                autoStartFromIntent = true;
                startTor();

                break;
            case Intent.ACTION_VIEW:
                String urlString = intent.getDataString();

                if (urlString != null) {

                    if (urlString.toLowerCase().startsWith("bridge://"))

                    {
                        String newBridgeValue = urlString.substring(9); //remove the bridge protocol piece
                        newBridgeValue = URLDecoder.decode(newBridgeValue); //decode the value here

                        showAlert(getString(R.string.bridges_updated), getString(R.string.restart_orbot_to_use_this_bridge_) + newBridgeValue, false);

                        setNewBridges(newBridgeValue);
                    }
                }
                break;
        }

        updateStatus(null, torStatus);

        setIntent(null);

    }

    private void setNewBridges(String newBridgeValue) {

        Prefs.setBridgesList(newBridgeValue); //set the string to a preference
        Prefs.putBridgesEnabled(true);

        setResult(RESULT_OK);

        mBtnBridges.setChecked(true);

        enableBridges(true);
    }

	/*
	 * Launch the system activity for Uri viewing with the provided url
	 */
	private void openBrowser(final String browserLaunchUrl,boolean forceExternal, String pkgId)
	{
        boolean isBrowserInstalled = appInstalledOrNot(TorServiceConstants.BROWSER_APP_USERNAME);

		if (pkgId != null)
        {
            startIntent(pkgId,Intent.ACTION_VIEW,Uri.parse(browserLaunchUrl));
        }
        else if (isBrowserInstalled)
        {
            startIntent(TorServiceConstants.BROWSER_APP_USERNAME,Intent.ACTION_VIEW,Uri.parse(browserLaunchUrl));
        }
		else if (mBtnVPN.isChecked()||forceExternal)
		{
			//use the system browser since VPN is on
			startIntent(null,Intent.ACTION_VIEW, Uri.parse(browserLaunchUrl));
		}
		
	}

	private void promptInstallOrfox ()
    {
        AlertDialog aDialog = new AlertDialog.Builder(OrbotMainActivity.this)
                .setTitle(R.string.install_apps_)
                .setMessage(R.string.it_doesn_t_seem_like_you_have_orweb_installed_want_help_with_that_or_should_we_just_open_the_browser_)
                .setPositiveButton(R.string.install_orweb, new Dialog.OnClickListener ()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        startActivity(OnboardingActivity.getInstallIntent(TorServiceConstants.BROWSER_APP_USERNAME,OrbotMainActivity.this));


                    }

                })
                .setNegativeButton(R.string.btn_cancel, new Dialog.OnClickListener ()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                    }

                })
                .show();
    }


    private void startIntent (String pkg, String action, Uri data)
    {
        Intent i;
		PackageManager pm = getPackageManager();

        try {
			if (pkg != null) {
				i = pm.getLaunchIntentForPackage(pkg);
				if (i == null)
					throw new PackageManager.NameNotFoundException();
			}
			else
			{
				i = new Intent();
			}

            i.setAction(action);
            i.setData(data);

			if (i.resolveActivity(pm)!=null)
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
    
    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        if (request == REQUEST_SETTINGS && response == RESULT_OK)
        {
            if (data != null && (!TextUtils.isEmpty(data.getStringExtra("locale")))) {
                LocaleHelper.setLocale(getApplicationContext(), Prefs.getDefaultLocale());
                finish();
                startActivity(new Intent(this,OrbotMainActivity.class));
            }
        }
        else if (request == REQUEST_VPN)
        {
			if (response == RESULT_OK) {
                sendIntentToService(TorServiceConstants.CMD_VPN);
            }
			else
			{
				Prefs.putUseVpn(false);
			}
        }
        else if (request == REQUEST_VPN_APPS_SELECT)
        {
            if (response == RESULT_OK &&
                    torStatus == TorServiceConstants.STATUS_ON)
                refreshVPNApps();

        }
        
        IntentResult scanResult = IntentIntegrator.parseActivityResult(request, response, data);
        if (scanResult != null) {
             // handle scan result
        	
        	String results = scanResult.getContents();
        	
        	if (results != null && results.length() > 0)
        	{
	        	try {
					
					int urlIdx = results.indexOf("://");
					
					if (urlIdx!=-1)
					{
						results = URLDecoder.decode(results, "UTF-8");
						results = results.substring(urlIdx+3);

						showAlert(getString(R.string.bridges_updated),getString(R.string.restart_orbot_to_use_this_bridge_) + results,false);	
						
						setNewBridges(results);
					}
					else
					{
						JSONArray bridgeJson = new JSONArray(results);
						StringBuffer bridgeLines = new StringBuffer();
						
						for (int i = 0; i < bridgeJson.length(); i++)
						{
							String bridgeLine = bridgeJson.getString(i);
							bridgeLines.append(bridgeLine).append("\n");
						}
						
						setNewBridges(bridgeLines.toString());
					}
					
					
				} catch (Exception e) {
					Log.e(TAG,"unsupported",e);
				}
        	}
        	
          }
        
    }
    
    public void promptSetupBridges ()
    {

        if (mBtnBridges.isChecked())
        {
            Prefs.putBridgesEnabled(true);
            startActivity(new Intent(this, BridgeWizardActivity.class));
        }
        else
        {
        	enableBridges(false);
        }
        
    }
    

    private void enableBridges (boolean enable)
    {
		Prefs.putBridgesEnabled(enable);

		if (torStatus == TorServiceConstants.STATUS_ON)
		{
			String bridgeList = Prefs.getBridgesList();
			if (bridgeList != null && bridgeList.length() > 0)
			{
				requestTorRereadConfig ();
			}
		}
    }

    private void requestTorRereadConfig() {
        sendIntentToService(TorServiceConstants.CMD_SIGNAL_HUP);
    }
    
    public void stopVpnService ()
    {    	
        sendIntentToService(TorServiceConstants.CMD_VPN_CLEAR);
    }

       @Override
    protected void onResume() {
        super.onResume();

        mBtnBridges.setChecked(Prefs.bridgesEnabled());
        mBtnVPN.setChecked(Prefs.useVpn());

		requestTorStatus();

		if (torStatus == null)
		    updateStatus("", TorServiceConstants.STATUS_STOPPING);
        else
            updateStatus(null, torStatus);

           if (Prefs.useTransparentProxying())
           {
               showAlert(getString(R.string.no_transproxy_warning_short),getString(R.string.no_transproxy_warning),true);
               Prefs.disableTransparentProxying();
           }

           addAppShortcuts();

           //now you can handle the intents properly
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
                            aDialog = new AlertDialog.Builder(OrbotMainActivity.this)
                     .setIcon(R.drawable.onion32)
             .setTitle(title)
             .setMessage(msg)
             .setPositiveButton(R.string.btn_okay, null)
             .show();
             }
             else
             {
                     aDialog = new AlertDialog.Builder(OrbotMainActivity.this)
                     .setIcon(R.drawable.onion32)
             .setTitle(title)
             .setMessage(msg)
             .show();
             }
    
             aDialog.setCanceledOnTouchOutside(true);
    }

    /**
     * Update the layout_main UI based on the status of {@link TorService}.
     * {@code torServiceMsg} must never be {@code null}
     */
    private synchronized void updateStatus(String torServiceMsg, String newTorStatus) {

        if (!TextUtils.isEmpty(torServiceMsg))
        {
            if (torServiceMsg.contains(TorServiceConstants.LOG_NOTICE_HEADER)) {
                lblStatus.setText(torServiceMsg);
            }

            mTxtOrbotLog.append(torServiceMsg + '\n');

        }

        if (torStatus == null || newTorStatus.equals(torStatus)) {
            torStatus = newTorStatus;
            return;
        }
    	else
    	    torStatus = newTorStatus;

        if (torStatus == TorServiceConstants.STATUS_ON) {
        	
            imgStatus.setImageResource(R.drawable.toron);

            mBtnStart.setText(R.string.menu_stop);
            mPulsator.stop();

            lblStatus.setText(getString(R.string.status_activated));

            if (autoStartFromIntent)
            {
                autoStartFromIntent = false;
                Intent resultIntent = lastStatusIntent;

                if (resultIntent == null)
                    resultIntent = new Intent(TorServiceConstants.ACTION_START);

                resultIntent.putExtra(
                        TorServiceConstants.EXTRA_STATUS,
                        torStatus == null?TorServiceConstants.STATUS_OFF:torStatus
                );

                setResult(RESULT_OK, resultIntent);

                finish();
                Log.d(TAG, "autoStartFromIntent finish");
            }
            
            

        } else if (torStatus == TorServiceConstants.STATUS_STARTING) {

            imgStatus.setImageResource(R.drawable.torstarting);

            if (torServiceMsg != null)
            {
            	if (torServiceMsg.contains(TorServiceConstants.LOG_NOTICE_BOOTSTRAPPED))
            		lblStatus.setText(torServiceMsg);            	            
            }
            else
            	lblStatus.setText(getString(R.string.status_starting_up));

			mBtnStart.setText("...");

        } else if (torStatus == TorServiceConstants.STATUS_STOPPING) {

        	  if (torServiceMsg != null && torServiceMsg.contains(TorServiceConstants.LOG_NOTICE_HEADER))
              	lblStatus.setText(torServiceMsg);	
        	  
            imgStatus.setImageResource(R.drawable.torstarting);
            lblStatus.setText(torServiceMsg);

        } else if (torStatus == TorServiceConstants.STATUS_OFF) {

            imgStatus.setImageResource(R.drawable.toroff);
            lblStatus.setText("Tor v" + BINARY_TOR_VERSION);
			mBtnStart.setText(R.string.menu_start);
            mPulsator.start();

        }


    }

    /**
     * Starts tor and related daemons by sending an
     * {@link TorServiceConstants#ACTION_START} {@link Intent} to
     * {@link TorService}
     */
    private void startTor() {
        sendIntentToService(TorServiceConstants.ACTION_START);
        mTxtOrbotLog.setText("");
    }
    
    /**
     * Request tor status without starting it
     * {@link TorServiceConstants#ACTION_START} {@link Intent} to
     * {@link TorService}
     */
    private void requestTorStatus() {
        sendIntentToService(TorServiceConstants.ACTION_STATUS);
    }

    private boolean isTorServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (TorService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean onLongClick(View view) {

        if (torStatus == TorServiceConstants.STATUS_OFF) {
            lblStatus.setText(getString(R.string.status_starting_up));
            startTor();
        } else {
        	lblStatus.setText(getString(R.string.status_shutting_down));
        	
            stopTor();
        }
        
        return true;
                
    }

// this is what takes messages or values from the callback threads or other non-mainUI threads
//and passes them back into the main UI thread for display to the user
    private Handler mStatusUpdateHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
        	


            switch (msg.what) {
                case MESSAGE_TRAFFIC_COUNT:

                    Bundle data = msg.getData();
                    DataCount datacount =  new DataCount(data.getLong("upload"),data.getLong("download"));     
                    
                    long totalRead = data.getLong("readTotal");
                    long totalWrite = data.getLong("writeTotal");
                
                    downloadText.setText(formatCount(datacount.Download) + " / " + formatTotal(totalRead));
                    uploadText.setText(formatCount(datacount.Upload) + " / " + formatTotal(totalWrite));

                    break;
                default:
                    String newTorStatus = msg.getData().getString("status");
                    String log = (String)msg.obj;

                    if (torStatus == null && newTorStatus != null) //first time status
                    {
                        findViewById(R.id.frameMain).setVisibility(View.VISIBLE);
                        updateStatus(log, newTorStatus);

                    }
                    else
                        updateStatus(log, newTorStatus);
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
          LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);

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
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());
        // Converts the supplied argument into a string.
        // Under 2Mb, returns "xxx.xKb"
        // Over 2Mb, returns "xxx.xxMb"
        if (count < 1e6)
            return numberFormat.format(Math.round(((float) ((int) (count * 10 / 1024)) / 10)))
                    + getString(R.string.kbps);
        else
            return numberFormat.format(Math
                    .round(((float) ((int) (count * 100 / 1024 / 1024)) / 100)))
                    + getString(R.string.mbps);
    }

    private String formatTotal(long count) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());
        // Converts the supplied argument into a string.
        // Under 2Mb, returns "xxx.xKb"
        // Over 2Mb, returns "xxx.xxMb"
        if (count < 1e6)
            return numberFormat.format(Math.round(((float) ((int) (count * 10 / 1024)) / 10)))
                    + getString(R.string.kb);
        else
            return numberFormat.format(Math
                    .round(((float) ((int) (count * 100 / 1024 / 1024)) / 100)))
                    + getString(R.string.mb);
    }

    /**
      private static final float ROTATE_FROM = 0.0f;
        private static final float ROTATE_TO = 360.0f*4f;// 3.141592654f * 32.0f;

    public void spinOrbot (float direction)
    {
            sendIntentToService (TorServiceConstants.CMD_NEWNYM);
        
        
            Toast.makeText(this, R.string.newnym, Toast.LENGTH_SHORT).show();
            
        //    Rotate3dAnimation rotation = new Rotate3dAnimation(ROTATE_FROM, ROTATE_TO*direction, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
             Rotate3dAnimation rotation = new Rotate3dAnimation(ROTATE_FROM, ROTATE_TO*direction, imgStatus.getWidth()/2f,imgStatus.getWidth()/2f,20f,false);
             rotation.setFillAfter(true);
              rotation.setInterpolator(new AccelerateInterpolator());
              rotation.setDuration((long) 2*1000);
              rotation.setRepeatCount(0);
              imgStatus.startAnimation(rotation);
              
        
    }**/

    private void addAppShortcuts ()
    {
        LinearLayout llBoxShortcuts = (LinearLayout)findViewById(R.id.boxAppShortcuts);

        PackageManager pMgr = getPackageManager();

        llBoxShortcuts.removeAllViews();

        //first add Orfox shortcut
        try {

            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(3, 3, 3, 3);
            iv.setLayoutParams(params);
            iv.setImageResource(R.drawable.orfox64);
            llBoxShortcuts.addView(iv);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!appInstalledOrNot(TorServiceConstants.BROWSER_APP_USERNAME))
                        promptInstallOrfox();
                    else
                        openBrowser(URL_TOR_CHECK,false, TorServiceConstants.BROWSER_APP_USERNAME);

                }
            });
        }
        catch (Exception e)
        {
            //package not installed?
        }

        if (PermissionManager.isLollipopOrHigher()) {

            if (Prefs.useVpn()) {
                ArrayList<String> pkgIds = new ArrayList<>();
                String tordAppString = mPrefs.getString(PREFS_KEY_TORIFIED, "");

                if (TextUtils.isEmpty(tordAppString)) {
                    TextView tv = new TextView(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(12, 3, 3, 3);
                    tv.setLayoutParams(params);
                    tv.setText(R.string.full_device_vpn);
                    llBoxShortcuts.addView(tv);
                } else {
                    StringTokenizer st = new StringTokenizer(tordAppString, "|");
                    while (st.hasMoreTokens() && pkgIds.size() < 4)
                        pkgIds.add(st.nextToken());

                    for (final String pkgId : pkgIds) {
                        try {
                            ImageView iv = new ImageView(this);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(3, 3, 3, 3);
                            iv.setLayoutParams(params);
                            iv.setImageDrawable(pMgr.getApplicationIcon(pkgId));

                            iv.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    openBrowser(URL_TOR_CHECK, false, pkgId);
                                }
                            });

                            llBoxShortcuts.addView(iv);

                        } catch (Exception e) {
                            //package not installed?
                        }
                    }


                }
            }
            else
            {
                TextView tv = new TextView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(12, 3, 3, 3);
                tv.setLayoutParams(params);
                tv.setText(R.string.vpn_disabled);
                llBoxShortcuts.addView(tv);
            }

            //now add app edit/add shortcut
            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(3, 3, 3, 3);
            iv.setLayoutParams(params);
            iv.setImageDrawable(getResources().getDrawable(R.drawable.ic_settings_white_24dp));
            llBoxShortcuts.addView(iv);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(OrbotMainActivity.this, AppManagerActivity.class), REQUEST_VPN_APPS_SELECT);

                }
            });
        }

    }

}
