package org.torproject.android.ui.onboarding;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import org.torproject.android.R;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.settings.LocaleHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import static org.torproject.android.MainConstants.URL_TOR_BRIDGES;

public class BridgeWizardActivity extends AppCompatActivity {

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridge_wizard);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvStatus = findViewById(R.id.lbl_bridge_test_status);
        tvStatus.setVisibility(View.GONE);

        setTitle(getString(R.string.bridges));

        RadioButton btnDirect = findViewById(R.id.btnBridgesDirect);
        btnDirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("");
                Prefs.putBridgesEnabled(false);
                testBridgeConnection();
            }
        });

        RadioButton btnObfs4 = findViewById(R.id.btnBridgesObfs4);
        btnObfs4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("obfs4");
                Prefs.putBridgesEnabled(true);
                testBridgeConnection();
            }
        });


        RadioButton btnMeek = findViewById(R.id.btnBridgesMeek);
        btnMeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("meek");
                Prefs.putBridgesEnabled(true);
                testBridgeConnection();
            }
        });


        RadioButton btnNew = findViewById(R.id.btnBridgesNew);
        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGetBridgePrompt();
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
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void showGetBridgePrompt() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bridge_mode)
                .setMessage(R.string.you_must_get_a_bridge_address_by_email_web_or_from_a_friend_once_you_have_this_address_please_paste_it_into_the_bridges_preference_in_orbot_s_setting_and_restart_)
                .setNegativeButton(R.string.btn_cancel, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setNeutralButton(R.string.get_bridges_email, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendGetBridgeEmail();
                    }

                })
                .setPositiveButton(R.string.get_bridges_web, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openBrowser(URL_TOR_BRIDGES, true);
                    }
                }).show();
    }

    private void sendGetBridgeEmail() {
        String email = "bridges@torproject.org";
        Uri emailUri = Uri.parse("mailto:" + email);
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, emailUri);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "get transport");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "get transport");
        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
    }


    /*
     * Launch the system activity for Uri viewing with the provided url
     */
    private void openBrowser(final String browserLaunchUrl, boolean forceExternal) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(browserLaunchUrl)));
    }


    private void testBridgeConnection() {
        if (TextUtils.isEmpty(Prefs.getBridgesList()) || (!Prefs.bridgesEnabled())) {
            new HostTester().execute("check.torproject.org", "443");
        } else if (Prefs.getBridgesList().equals("meek")) {
            new HostTester().execute("meek.azureedge.net", "443", "d2cly7j4zqgua7.cloudfront.net", "443");
        } else if (Prefs.getBridgesList().equals("obfs4")) {
            new HostTester().execute("85.17.30.79", "443", "154.35.22.9", "443", "192.99.11.54", "443");
        } else {
            tvStatus.setText("");
        }
    }

    private class HostTester extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            // Pre Code
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(R.string.testing_bridges);
        }

        @Override
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

        @Override
        protected void onPostExecute(Boolean result) {
            // Post Code
            if (result) {
                tvStatus.setText(R.string.testing_bridges_success);

            } else {
                tvStatus.setText(R.string.testing_bridges_fail);

            }
        }
    }

    private static boolean isHostReachable(String serverAddress, int serverTCPport, int timeoutMS) {
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
