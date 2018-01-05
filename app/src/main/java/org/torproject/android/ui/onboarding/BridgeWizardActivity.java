package org.torproject.android.ui.onboarding;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.torproject.android.R;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.settings.LocaleHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class BridgeWizardActivity extends AppCompatActivity {

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridge_wizard);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvStatus = (TextView)findViewById(R.id.lbl_bridge_test_status);
        tvStatus.setVisibility(View.GONE);

        setTitle(getString(R.string.bridges));

        RadioButton btnDirect = (RadioButton)
                findViewById(R.id.btnBridgesDirect);
        btnDirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("");
                Prefs.putBridgesEnabled(false);
                testBridgeConnection();

            }
        });

        RadioButton btnObfs4 = (RadioButton)findViewById(R.id.btnBridgesObfs4);
        btnObfs4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("obfs4");
                Prefs.putBridgesEnabled(true);
                testBridgeConnection();

            }
        });


        RadioButton btnMeek = (RadioButton)findViewById(R.id.btnBridgesMeek);

        btnMeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("meek");
                Prefs.putBridgesEnabled(true);
                testBridgeConnection();

            }
        });


        RadioButton btnNew = (RadioButton)findViewById(R.id.btnBridgesNew);
        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGetBridgePrompt("");
            }
        });

        if (!Prefs.bridgesEnabled())
            btnDirect.setChecked(true);
        else if (Prefs.getBridgesList().equals("meek"))
            btnMeek.setChecked(true);
        else if (Prefs.getBridgesList().equals("obfs4"))
            btnObfs4.setChecked(true);


    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void showGetBridgePrompt (final String type)
    {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.layout_diag, null);

        TextView versionName = (TextView)view.findViewById(R.id.diaglog);
        versionName.setText(R.string.you_must_get_a_bridge_address_by_email_web_or_from_a_friend_once_you_have_this_address_please_paste_it_into_the_bridges_preference_in_orbot_s_setting_and_restart_);

        new AlertDialog.Builder(this)
                .setTitle(R.string.bridge_mode)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, new Dialog.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setNeutralButton(R.string.get_bridges_email, new Dialog.OnClickListener ()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                        sendGetBridgeEmail(type);

                    }


                })
                .setPositiveButton(R.string.get_bridges_web, new Dialog.OnClickListener ()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        openBrowser(OrbotConstants.URL_TOR_BRIDGES + type,true, null);

                    }


                }).show();
    }

    private void sendGetBridgeEmail (String type)
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL  , new String[]{"bridges@torproject.org"});

        if (type != null)
        {
            intent.putExtra(Intent.EXTRA_SUBJECT, "get transport " + type);
            intent.putExtra(Intent.EXTRA_TEXT, "get transport " + type);

        }
        else
        {
            intent.putExtra(Intent.EXTRA_SUBJECT, "get bridges");
            intent.putExtra(Intent.EXTRA_TEXT, "get bridges");

        }

        startActivity(Intent.createChooser(intent, getString(R.string.send_email)));
    }


    /*
     * Launch the system activity for Uri viewing with the provided url
     */
    private void openBrowser(final String browserLaunchUrl,boolean forceExternal, String pkgId)
    {
        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(browserLaunchUrl)));
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

    private void testBridgeConnection ()
    {
        if (TextUtils.isEmpty(Prefs.getBridgesList()) || (!Prefs.bridgesEnabled()))
        {
            new HostTester().execute("check.torproject.org","443");
        }
        else if (Prefs.getBridgesList().equals("meek"))
        {
            new HostTester().execute("meek.azureedge.net","443","d2cly7j4zqgua7.cloudfront.net","443");
        }
        else if (Prefs.getBridgesList().equals("obfs4"))
        {
            new HostTester().execute("85.17.30.79","443","154.35.22.9","443","192.99.11.54","443");
        }
        else
        {
            tvStatus.setText("");
        }
    }

    private class HostTester extends AsyncTask<String, Void, Boolean> {
        protected void onPreExecute() {
            // Pre Code
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(R.string.testing_bridges);
        }
    protected Boolean doInBackground(String... host) {
        // Background Code
        boolean result = false;

        for (int i = 0; i < host.length; i++) {
            String testHost = host[i];
            i++; //move to the port
            int testPort = Integer.parseInt(host[i]);
            result = isHostReachable(testHost, testPort, 10000);
            if (result)
                return result;
        }

        return result;
    }
    protected void onPostExecute(Boolean result) {
        // Post Code
        if (result)
        {
            tvStatus.setText(R.string.testing_bridges_success);

        }
        else
        {
            tvStatus.setText(R.string.testing_bridges_fail);

        }
    }};

    private static boolean isHostReachable(String serverAddress, int serverTCPport, int timeoutMS){
        boolean connected = false;
        Socket socket;
        try {
            socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(serverAddress, serverTCPport);
            socket.connect(socketAddress, timeoutMS);
            if (socket.isConnected()) {
                connected = true;
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
        }
        return connected;
    }
}
