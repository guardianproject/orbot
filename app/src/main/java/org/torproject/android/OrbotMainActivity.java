/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */

package org.torproject.android;

import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.torproject.android.core.Languages;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.core.ui.Rotate3dAnimation;
import org.torproject.android.core.ui.SettingsPreferencesActivity;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.service.util.Utils;
import org.torproject.android.service.vpn.VpnPrefs;
import org.torproject.android.ui.AppManagerActivity;
import org.torproject.android.ui.dialog.AboutDialogFragment;
import org.torproject.android.ui.hiddenservices.ClientCookiesActivity;
import org.torproject.android.ui.hiddenservices.HiddenServicesActivity;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;
import org.torproject.android.ui.onboarding.BridgeWizardActivity;
import org.torproject.android.ui.onboarding.OnboardingActivity;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;

import static androidx.core.content.FileProvider.getUriForFile;
import static org.torproject.android.MainConstants.COUNTRY_CODES;
import static org.torproject.android.MainConstants.RESULT_CLOSE_ALL;
import static org.torproject.android.MainConstants.URL_TOR_CHECK;
import static org.torproject.android.service.TorServiceConstants.ACTION_START;
import static org.torproject.android.service.TorServiceConstants.ACTION_START_VPN;
import static org.torproject.android.service.TorServiceConstants.ACTION_STOP_VPN;
import static org.torproject.android.service.TorServiceConstants.DIRECTORY_TOR_DATA;
import static org.torproject.android.service.vpn.VpnPrefs.PREFS_KEY_TORIFIED;

public class OrbotMainActivity extends AppCompatActivity implements OrbotConstants {

    public final static String INTENT_ACTION_REQUEST_HIDDEN_SERVICE = "org.torproject.android.REQUEST_HS_PORT";
    public final static String INTENT_ACTION_REQUEST_START_TOR = "org.torproject.android.START_TOR";
    private final static int REQUEST_VPN = 8888;
    private final static int REQUEST_SETTINGS = 0x9874;
    private final static int REQUEST_VPN_APPS_SELECT = 8889;
    private final static int LOG_DRAWER_GRAVITY = GravityCompat.END;
    // message types for mStatusUpdateHandler
    private final static int STATUS_UPDATE = 1;
    private static final int MESSAGE_TRAFFIC_COUNT = 2;
    private static final int MESSAGE_PORTS = 3;
    private static final float ROTATE_FROM = 0.0f;
    private static final float ROTATE_TO = 360.0f * 4f;// 3.141592654f * 32.0f;
    PulsatorLayout mPulsator;
    AlertDialog aDialog = null;
    /* Useful UI bits */
    private TextView lblStatus = null; //the main text display widget
    private TextView lblPorts = null;
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
    // this is what takes messages or values from the callback threads or other non-mainUI threads
//and passes them back into the main UI thread for display to the user
    private Handler mStatusUpdateHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {


            Bundle data = msg.getData();

            switch (msg.what) {
                case MESSAGE_TRAFFIC_COUNT:

                    DataCount datacount = new DataCount(data.getLong("upload"), data.getLong("download"));

                    long totalRead = data.getLong("readTotal");
                    long totalWrite = data.getLong("writeTotal");

                    downloadText.setText(String.format("%s / %s", formatCount(datacount.Download), formatTotal(totalRead)));
                    uploadText.setText(String.format("%s / %s", formatCount(datacount.Upload), formatTotal(totalWrite)));

                    break;
                case MESSAGE_PORTS:

                    int socksPort = data.getInt("socks");
                    int httpPort = data.getInt("http");

                    lblPorts.setText(String.format(Locale.getDefault(), "SOCKS: %d | HTTP: %d", socksPort, httpPort));

                    break;
                default:
                    String newTorStatus = msg.getData().getString("status");
                    String log = (String) msg.obj;

                    if (torStatus == null && newTorStatus != null) //first time status
                        findViewById(R.id.frameMain).setVisibility(View.VISIBLE);
                    updateStatus(log, newTorStatus);
                    super.handleMessage(msg);
                    break;
            }
        }
    };
    /**
     * The state and log info from {@link OrbotService} are sent to the UI here in
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

            switch (action) {
                case TorServiceConstants.LOCAL_ACTION_LOG: {
                    Message msg = mStatusUpdateHandler.obtainMessage(STATUS_UPDATE);
                    msg.obj = intent.getStringExtra(TorServiceConstants.LOCAL_EXTRA_LOG);
                    msg.getData().putString("status", intent.getStringExtra(TorServiceConstants.EXTRA_STATUS));
                    mStatusUpdateHandler.sendMessage(msg);

                    break;
                }
                case TorServiceConstants.LOCAL_ACTION_BANDWIDTH: {
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

                    break;
                }
                case TorServiceConstants.ACTION_STATUS: {
                    lastStatusIntent = intent;

                    Message msg = mStatusUpdateHandler.obtainMessage(STATUS_UPDATE);
                    msg.getData().putString("status", intent.getStringExtra(TorServiceConstants.EXTRA_STATUS));

                    mStatusUpdateHandler.sendMessage(msg);
                    break;
                }
                case TorServiceConstants.LOCAL_ACTION_PORTS: {

                    Message msg = mStatusUpdateHandler.obtainMessage(MESSAGE_PORTS);
                    msg.getData().putInt("socks", intent.getIntExtra(OrbotService.EXTRA_SOCKS_PROXY_PORT, -1));
                    msg.getData().putInt("http", intent.getIntExtra(OrbotService.EXTRA_HTTP_PROXY_PORT, -1));

                    mStatusUpdateHandler.sendMessage(msg);

                    break;
                }
                case ACTION_STOP_VPN: {
                    mBtnVPN.setChecked(false);
                    break;
                }
            }
        }
    };

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
            pEdit.apply();
        }
    }

    /**
     * Called when the activity is first created.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = Prefs.getSharedPrefs(getApplicationContext());

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
        lbm.registerReceiver(mLocalBroadcastReceiver,
                new IntentFilter(TorServiceConstants.LOCAL_ACTION_PORTS));


        boolean showFirstTime = mPrefs.getBoolean("connect_first_time", true);

        if (showFirstTime) {
            Editor pEdit = mPrefs.edit();
            pEdit.putBoolean("connect_first_time", false);
            pEdit.apply();
            startActivity(new Intent(this, OnboardingActivity.class));
        }

        // Resets previous DNS Port to the default.
        Prefs.getSharedPrefs(getApplicationContext()).edit().putInt(VpnPrefs.PREFS_DNS_PORT,
                TorServiceConstants.TOR_DNS_PORT_DEFAULT).apply();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Prefs.useDebugLogging())
                exportTorData();
        }

    }

    private void sendIntentToService(final String action) {
        Intent intent = new Intent(OrbotMainActivity.this, OrbotService.class);
        intent.setAction(action);
        startService(intent);
    }

    private void stopTor() {
        if (mBtnVPN.isChecked()) sendIntentToService(ACTION_STOP_VPN);

        Intent intent = new Intent(OrbotMainActivity.this, OrbotService.class);
        stopService(intent);
    }

    private void doLayout() {
        setContentView(R.layout.layout_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = findViewById(R.id.drawer_layout);

        mTxtOrbotLog = findViewById(R.id.orbotLog);

        lblStatus = findViewById(R.id.lblStatus);
        lblStatus.setOnClickListener(v -> mDrawer.openDrawer(LOG_DRAWER_GRAVITY));

        lblPorts = findViewById(R.id.lblPorts);

        imgStatus = findViewById(R.id.imgStatus);
        imgStatus.setOnLongClickListener(v -> {
            toggleTor();
            return true;
        });

        downloadText = findViewById(R.id.trafficDown);
        uploadText = findViewById(R.id.trafficUp);

        String zero = String.format("%s / %s", formatCount(0), formatTotal(0));
        downloadText.setText(zero);
        uploadText.setText(zero);

        mBtnStart = findViewById(R.id.btnStart);
        mBtnStart.setOnClickListener(v -> toggleTor());

        mBtnVPN = findViewById(R.id.btnVPN);

        boolean useVPN = Prefs.useVpn();
        mBtnVPN.setChecked(useVPN);

        //auto start VPN if VPN is enabled
        if (useVPN) {
            sendIntentToService(ACTION_START_VPN);
        }

        mBtnVPN.setOnCheckedChangeListener((buttonView, isChecked) -> enableVPN(isChecked));


        mBtnBridges = findViewById(R.id.btnBridges);
        mBtnBridges.setChecked(Prefs.bridgesEnabled());
        mBtnBridges.setOnClickListener(v -> {
            promptSetupBridges(); //if ARM processor, show all bridge options
        });

        spnCountries = findViewById(R.id.spinnerCountry);
        setCountrySpinner();

        mPulsator = findViewById(R.id.pulsator);
    }

    private void toggleTor() { // UI entry point for  (dis)connecting to Tor
        if (torStatus.equals(TorServiceConstants.STATUS_OFF)) {
            lblStatus.setText(getString(R.string.status_starting_up));
            startTor();
        } else {
            lblStatus.setText(getString(R.string.status_shutting_down));
            stopTor();
        }
    }

    private void setCountrySpinner() {
        String currentExit = Prefs.getExitNodes();
        if (currentExit.length() > 4) {
            //someone put a complex value in, so let's disable
            ArrayList<String> cList = new ArrayList<>();
            cList.add(0, currentExit);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cList);
            spnCountries.setAdapter(adapter);

            spnCountries.setEnabled(false);
        } else {
            int selIdx = -1;

            ArrayList<String> cList = new ArrayList<>();
            cList.add(0, getString(R.string.vpn_default_world));

            for (int i = 0; i < COUNTRY_CODES.length; i++) {
                Locale locale = new Locale("", COUNTRY_CODES[i]);
                cList.add(locale.getDisplayCountry());

                if (currentExit.contains(COUNTRY_CODES[i]))
                    selIdx = i + 1;
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cList);
            spnCountries.setAdapter(adapter);

            if (selIdx > 0)
                spnCountries.setSelection(selIdx, true);

            spnCountries.setOnItemSelectedListener(new OnItemSelectedListener() {

                int mOldPosition = spnCountries.getSelectedItemPosition();

                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    // your code here

                    if (mOldPosition == position)
                        return;

                    mOldPosition = position; //new position!

                    String country;

                    if (position == 0)
                        country = "";
                    else
                        country = '{' + COUNTRY_CODES[position - 1] + '}';

                    Intent intent = new Intent(OrbotMainActivity.this, OrbotService.class);
                    intent.setAction(TorServiceConstants.CMD_SET_EXIT);
                    intent.putExtra("exit", country);
                    startService(intent);

                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }

            });
        }
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_newnym) {
            requestNewTorIdentity();
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = SettingsPreferencesActivity.createIntent(this, R.xml.preferences);
            startActivityForResult(intent, REQUEST_SETTINGS);
        } else if (item.getItemId() == R.id.menu_exit) {
            doExit(); // exit app
        } else if (item.getItemId() == R.id.menu_about) {
            new AboutDialogFragment().show(getSupportFragmentManager(), AboutDialogFragment.TAG);
        } else if (item.getItemId() == R.id.menu_scan) {
            IntentIntegrator integrator = new IntentIntegrator(OrbotMainActivity.this);
            integrator.initiateScan();
        } else if (item.getItemId() == R.id.menu_share_bridge) {

            String bridges = Prefs.getBridgesList();

            if (bridges != null && bridges.length() > 0) {
                try {
                    bridges = "bridge://" + URLEncoder.encode(bridges, "UTF-8");

                    IntentIntegrator integrator = new IntentIntegrator(OrbotMainActivity.this);
                    integrator.shareText(bridges);

                } catch (UnsupportedEncodingException e) {
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

    /**
     * This is our attempt to REALLY exit Orbot, and stop the background service
     * However, Android doesn't like people "quitting" apps, and/or our code may
     * not be quite right b/c no matter what we do, it seems like the OrbotService
     * still exists
     **/
    private void doExit() {
        stopTor();

        // Kill all the wizard activities
        setResult(RESULT_CLOSE_ALL);
        finish();
    }

    protected void onPause() {
        try {
            super.onPause();

            if (aDialog != null)
                aDialog.dismiss();
        } catch (IllegalStateException ise) {
            //can happen on exit/shutdown
        }
    }

    @Override
    public void onBackPressed() {
        // check to see if the log is open, if so close it
        if (mDrawer.isDrawerOpen(LOG_DRAWER_GRAVITY)) {
            mDrawer.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    private void refreshVPNApps() {
        sendIntentToService(ACTION_STOP_VPN);
        sendIntentToService(ACTION_START_VPN);
    }

    private void enableVPN(boolean enable) {
        Prefs.putUseVpn(enable);
        drawAppShortcuts(false);
        if (enable) {

            Intent intentVPN = VpnService.prepare(this);
            if (intentVPN != null)
                startActivityForResult(intentVPN, REQUEST_VPN);
            else {
                startVpn();
            }

        } else {
            //stop the VPN here
            sendIntentToService(ACTION_STOP_VPN);
        }

    }

    private void startVpn() {
        drawAppShortcuts(true);
        sendIntentToService(ACTION_START);
        sendIntentToService(ACTION_START_VPN);
    }

    private void enableHiddenServicePort(
            String hsName, final int hsPort, int hsRemotePort,
            final String backupToPackage, final Uri hsKeyPath,
            final Boolean authCookie
    ) {

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

                            if (hostname == null || hostname.length() < 1)
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

                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        enableHiddenServicePort(
                                hiddenServiceName, hiddenServicePort,
                                hiddenServiceRemotePort, backupToPackage,
                                mKeyUri, authCookie
                        );
                    }
                };

                String requestMsg = getString(R.string.hidden_service_request, String.valueOf(hiddenServicePort));
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

                    if (urlString.toLowerCase().startsWith("bridge://")) {
                        String newBridgeValue = urlString.substring(9); //remove the bridge protocol piece
                        try {
                            newBridgeValue = URLDecoder.decode(newBridgeValue, "UTF-8"); //decode the value here
                        } catch (UnsupportedEncodingException e) {
                            // This cannot happen, UTF-8 is supported since Android 1.
                        }

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
    @SuppressWarnings("SameParameterValue")
    private void openBrowser(final String browserLaunchUrl, boolean forceExternal, String pkgId) {
        if (pkgId != null) {
            startIntent(pkgId, Intent.ACTION_VIEW, Uri.parse(browserLaunchUrl));
        } else if (mBtnVPN.isChecked() || forceExternal) {
            //use the system browser since VPN is on
            startIntent(null, Intent.ACTION_VIEW, Uri.parse(browserLaunchUrl));
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void startIntent(String pkg, String action, Uri data) {
        Intent i;
        PackageManager pm = getPackageManager();

        try {
            if (pkg != null) {
                i = pm.getLaunchIntentForPackage(pkg);
                if (i == null)
                    throw new PackageManager.NameNotFoundException();
            } else {
                i = new Intent();
            }

            i.setAction(action);
            i.setData(data);

            if (i.resolveActivity(pm) != null)
                startActivity(i);

        } catch (PackageManager.NameNotFoundException e) {
            // Should not occur. Ignore.
        }
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        if (request == REQUEST_SETTINGS && response == RESULT_OK) {
            if (data != null && (!TextUtils.isEmpty(data.getStringExtra("locale")))) {

                String newLocale = data.getStringExtra("locale");
                Prefs.setDefaultLocale(newLocale);
                Languages.setLanguage(this, newLocale, true);
                //  Language.setFromPreference(this, "pref_default_locale");

                finish();

                mStatusUpdateHandler.postDelayed(() -> {
                    //Do something after 100ms
                    startActivity(new Intent(OrbotMainActivity.this, OrbotMainActivity.class));

                }, 1000);


            }
        } else if (request == REQUEST_VPN_APPS_SELECT) {
            if (response == RESULT_OK &&
                    torStatus.equals(TorServiceConstants.STATUS_ON))
                refreshVPNApps();

        } else if (request == REQUEST_VPN && response == RESULT_OK) {
            startVpn();
        } else if (request == REQUEST_VPN && response == RESULT_CANCELED) {
            mBtnVPN.setChecked(false);
        }

        IntentResult scanResult = IntentIntegrator.parseActivityResult(request, response, data);
        if (scanResult != null) {
            // handle scan result

            String results = scanResult.getContents();

            if (results != null && results.length() > 0) {
                try {

                    int urlIdx = results.indexOf("://");

                    if (urlIdx != -1) {
                        results = URLDecoder.decode(results, "UTF-8");
                        results = results.substring(urlIdx + 3);

                        showAlert(getString(R.string.bridges_updated), getString(R.string.restart_orbot_to_use_this_bridge_) + results, false);

                        setNewBridges(results);
                    } else {
                        JSONArray bridgeJson = new JSONArray(results);
                        StringBuilder bridgeLines = new StringBuilder();

                        for (int i = 0; i < bridgeJson.length(); i++) {
                            String bridgeLine = bridgeJson.getString(i);
                            bridgeLines.append(bridgeLine).append("\n");
                        }

                        setNewBridges(bridgeLines.toString());
                    }


                } catch (Exception e) {
                    Log.e(TAG, "unsupported", e);
                }
            }

        }

    }

    public void promptSetupBridges() {

        if (mBtnBridges.isChecked()) {
            Prefs.putBridgesEnabled(true);

            startActivity(new Intent(this, BridgeWizardActivity.class));
        } else {
            enableBridges(false);
        }

    }

    private void enableBridges(boolean enable) {
        Prefs.putBridgesEnabled(enable);

        if (torStatus.equals(TorServiceConstants.STATUS_ON)) {
            String bridgeList = Prefs.getBridgesList();
            if (bridgeList != null && bridgeList.length() > 0) {
                requestTorRereadConfig();
            }
        }
    }

    private void requestTorRereadConfig() {
        sendIntentToService(TorServiceConstants.CMD_SIGNAL_HUP);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sendIntentToService(TorServiceConstants.CMD_ACTIVE);

        mBtnBridges.setChecked(Prefs.bridgesEnabled());
        refreshVpnState();

        setCountrySpinner();

        requestTorStatus();

        if (torStatus == null)
            updateStatus("", TorServiceConstants.STATUS_STOPPING);
        else
            updateStatus(null, torStatus);

        drawAppShortcuts(Prefs.useVpn());

        //now you can handle the intents properly
        handleIntents();

    }

    /**
     * After granting VPN permissions in Orbot it's possible to revoke them outside of the app
     * This method ensures that VPN permissions are still granted and if not it updates the state
     * of the UI accordingly. see: https://github.com/guardianproject/orbot/issues/368
     */
    private void refreshVpnState() {
        if (Prefs.useVpn()) {
            Intent enableVpnIntent = VpnService.prepare(this);
            // don't start the Intent, just update Orbot to say that VPN privileges are gone
            if (enableVpnIntent != null) {
                Prefs.putUseVpn(false);
            }
        }
        mBtnVPN.setChecked(Prefs.useVpn());
    }

    //general alert dialog for mostly Tor warning messages
    //sometimes this can go haywire or crazy with too many error
    //messages from Tor, and the user cannot stop or exit Orbot
    //so need to ensure repeated error messages are not spamming this method
    @SuppressWarnings("SameParameterValue")
    private void showAlert(String title, String msg, boolean button) {
        try {
            if (aDialog != null && aDialog.isShowing())
                aDialog.dismiss();
        } catch (Exception e) {
            //swallow any errors
        }

        if (button) {
            aDialog = new AlertDialog.Builder(OrbotMainActivity.this)
                    .setIcon(R.drawable.onion32)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } else {
            aDialog = new AlertDialog.Builder(OrbotMainActivity.this)
                    .setIcon(R.drawable.onion32)
                    .setTitle(title)
                    .setMessage(msg)
                    .show();
        }

        aDialog.setCanceledOnTouchOutside(true);
    }

    /**
     * Update the layout_main UI based on the status of {@link OrbotService}.
     * {@code torServiceMsg} must never be {@code null}
     */
    private synchronized void updateStatus(String torServiceMsg, String newTorStatus) {

        if (!TextUtils.isEmpty(torServiceMsg)) {
            if (torServiceMsg.contains(TorServiceConstants.LOG_NOTICE_HEADER)) {
                lblStatus.setText(torServiceMsg);
            }

            mTxtOrbotLog.append(torServiceMsg + '\n');
        }

        if (torStatus == null || (newTorStatus != null && newTorStatus.equals(torStatus))) {
            torStatus = newTorStatus;
            return;
        } else {
            torStatus = newTorStatus;
        }

        if (torStatus == null) {
            return;
        }

        switch (torStatus) {
            case TorServiceConstants.STATUS_ON:

                imgStatus.setImageResource(R.drawable.toron);

                mBtnStart.setText(R.string.menu_stop);
                mPulsator.stop();

                lblStatus.setText(getString(R.string.status_activated));

                if (autoStartFromIntent) {
                    autoStartFromIntent = false;
                    Intent resultIntent = lastStatusIntent;

                    if (resultIntent == null)
                        resultIntent = new Intent(ACTION_START);

                    resultIntent.putExtra(
                            TorServiceConstants.EXTRA_STATUS,
                            torStatus == null ? TorServiceConstants.STATUS_OFF : torStatus
                    );

                    setResult(RESULT_OK, resultIntent);

                    finish();
                    Log.d(TAG, "autoStartFromIntent finish");
                }

                break;

            case TorServiceConstants.STATUS_STARTING:

                imgStatus.setImageResource(R.drawable.torstarting);

                if (torServiceMsg != null) {
                    if (torServiceMsg.contains(TorServiceConstants.LOG_NOTICE_BOOTSTRAPPED))
                        lblStatus.setText(torServiceMsg);
                } else {
                    lblStatus.setText(getString(R.string.status_starting_up));
                }

                mBtnStart.setText("...");

                break;

            case TorServiceConstants.STATUS_STOPPING:

                if (torServiceMsg != null && torServiceMsg.contains(TorServiceConstants.LOG_NOTICE_HEADER))
                    lblStatus.setText(torServiceMsg);

                imgStatus.setImageResource(R.drawable.torstarting);
                lblStatus.setText(torServiceMsg);

                break;

            case TorServiceConstants.STATUS_OFF:

                imgStatus.setImageResource(R.drawable.toroff);
                lblStatus.setText(String.format("Tor v%s", OrbotService.BINARY_TOR_VERSION));
                mBtnStart.setText(R.string.menu_start);
                mPulsator.start();

                break;
        }
    }

    /**
     * Starts tor and related daemons by sending an
     * {@link TorServiceConstants#ACTION_START} {@link Intent} to
     * {@link OrbotService}
     */
    private void startTor() {
        sendIntentToService(ACTION_START);
        mTxtOrbotLog.setText("");
    }

    /**
     * Request tor status without starting it
     * {@link TorServiceConstants#ACTION_START} {@link Intent} to
     * {@link OrbotService}
     */
    private void requestTorStatus() {
        sendIntentToService(TorServiceConstants.ACTION_STATUS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);
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

    private void requestNewTorIdentity() {
        sendIntentToService(TorServiceConstants.CMD_NEWNYM);

        Rotate3dAnimation rotation = new Rotate3dAnimation(ROTATE_FROM, ROTATE_TO, imgStatus.getWidth() / 2f, imgStatus.getWidth() / 2f, 20f, false);
        rotation.setFillAfter(true);
        rotation.setInterpolator(new AccelerateInterpolator());
        rotation.setDuration((long) 2 * 1000);
        rotation.setRepeatCount(0);
        imgStatus.startAnimation(rotation);
        lblStatus.setText(getString(R.string.newnym));
    }

    private void drawAppShortcuts(boolean showSelectedApps) {
        if (!PermissionManager.isLollipopOrHigher()) {
            findViewById(R.id.boxVpnStatus).setVisibility(View.GONE);
            return;
        }

        LinearLayout llBoxShortcuts = findViewById(R.id.boxAppShortcuts);

        PackageManager pMgr = getPackageManager();

        llBoxShortcuts.removeAllViews();

        if (showSelectedApps) {
            ArrayList<String> pkgIds = new ArrayList<>();
            String tordAppString = mPrefs.getString(PREFS_KEY_TORIFIED, "");

            if (TextUtils.isEmpty(tordAppString)) {
                addFullDeviceVpnView(llBoxShortcuts);
            } else {
                StringTokenizer st = new StringTokenizer(tordAppString, "|");
                while (st.hasMoreTokens() && pkgIds.size() < 4)
                    pkgIds.add(st.nextToken());
                int appsAdded = 0;
                for (final String pkgId : pkgIds) {
                    try {
                        ApplicationInfo aInfo = getPackageManager().getApplicationInfo(pkgId, 0);
                        // skip disabled packages
                        if (!aInfo.enabled) continue;
                        ImageView iv = new ImageView(this);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(3, 3, 3, 3);
                        iv.setLayoutParams(params);
                        iv.setImageDrawable(pMgr.getApplicationIcon(pkgId));
                        iv.setOnClickListener(v -> openBrowser(URL_TOR_CHECK, false, pkgId));
                        llBoxShortcuts.addView(iv);
                        appsAdded++;
                    } catch (Exception e) {
                        //package not installed?
                    }
                }
                if (appsAdded == 0) {
                        /* if a user uninstalled or disabled all apps that were set on the device
                           then we want to have the no apps added view appear even though
                           the tordAppString variable is not empty */
                    addFullDeviceVpnView(llBoxShortcuts);
                }
            }
        } else {
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
        iv.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_settings_white_24dp, null));
        llBoxShortcuts.addView(iv);
        iv.setOnClickListener(v -> startActivityForResult(new Intent(OrbotMainActivity.this, AppManagerActivity.class), REQUEST_VPN_APPS_SELECT));
    }

    private void addFullDeviceVpnView(LinearLayout llBoxShortcuts) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(12, 3, 3, 3);
        tv.setLayoutParams(params);
        tv.setText(R.string.full_device_vpn);
        llBoxShortcuts.addView(tv);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void exportTorData() {
        File fileTorData;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            fileTorData = new File(getDataDir(), DIRECTORY_TOR_DATA);
        } else {
            fileTorData = getDir(DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);
        }

        File fileZip = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "orbotdata" + new Date().getTime() + ".zip");
        Utils.zipFileAtPath(fileTorData.getAbsolutePath(), fileZip.getAbsolutePath());
        fileZip.setReadable(true, false);
        Log.d(TAG, "debugdata: " + fileZip.getAbsolutePath());

    }

    public static class DataCount {
        // data uploaded
        final long Upload;
        // data downloaded
        final long Download;

        DataCount(long Upload, long Download) {
            this.Upload = Upload;
            this.Download = Download;
        }
    }
}
