package org.torproject.android.ui.onboarding;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.torproject.android.R;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.settings.LocaleHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import static org.torproject.android.MainConstants.URL_TOR_BRIDGES;

public class BridgeWizardActivity extends AppCompatActivity {

    private static int MOAT_REQUEST_CODE = 666;

    private TextView mTvStatus;
    private RadioButton mBtDirect;
    private RadioButton mBtObfs4;
    private RadioButton mBtMeek;
    private RadioButton mBtNew;
    private RadioButton mBtMoat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridge_wizard);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mTvStatus = findViewById(R.id.lbl_bridge_test_status);
        mTvStatus.setVisibility(View.GONE);

        setTitle(getString(R.string.bridges));

        mBtDirect = findViewById(R.id.btnBridgesDirect);
        mBtDirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("");
                Prefs.putBridgesEnabled(false);
                testBridgeConnection();
            }
        });

        mBtObfs4 = findViewById(R.id.btnBridgesObfs4);
        mBtObfs4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("obfs4");
                Prefs.putBridgesEnabled(true);
                testBridgeConnection();
            }
        });


        mBtMeek = findViewById(R.id.btnBridgesMeek);
        mBtMeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("meek");
                Prefs.putBridgesEnabled(true);
                testBridgeConnection();
            }
        });


        mBtNew = findViewById(R.id.btnBridgesNew);
        mBtNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGetBridgePrompt();
            }
        });

        mBtMoat = findViewById(R.id.btnMoat);
        mBtMoat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(BridgeWizardActivity.this, MoatActivity.class),
                        MOAT_REQUEST_CODE);
            }
        });

        evaluateBridgeListState();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // If the MoatActivity could successfully gather OBFS4 bridges,
        // the job is done and we can return immediately.
        if (requestCode == MOAT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                finish();
            }
            // Reset selection to actual value.
            else {
                evaluateBridgeListState();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
                        openBrowser(URL_TOR_BRIDGES);
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
    @SuppressWarnings("SameParameterValue")
    private void openBrowser(final String browserLaunchUrl) {
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
            mTvStatus.setText("");
        }
    }

    private class HostTester extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            // Pre Code
            mTvStatus.setVisibility(View.VISIBLE);
            mTvStatus.setText(R.string.testing_bridges);
        }

        @Override
        protected Boolean doInBackground(String... host) {
            // Background Code
            for (int i = 0; i < host.length; i++) {
                String testHost = host[i];
                i++; //move to the port
                int testPort = Integer.parseInt(host[i]);

                if (isHostReachable(testHost, testPort, 10000)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Post Code
            if (result) {
                mTvStatus.setText(R.string.testing_bridges_success);

            } else {
                mTvStatus.setText(R.string.testing_bridges_fail);

            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean isHostReachable(String serverAddress, int serverTCPport, int timeoutMS) {
        boolean connected = false;

        try {
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(serverAddress, serverTCPport);
            socket.connect(socketAddress, timeoutMS);
            if (socket.isConnected()) {
                connected = true;
                socket.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return connected;
    }

    private void evaluateBridgeListState() {
        if (!Prefs.bridgesEnabled()) {
            mBtDirect.setChecked(true);
        }
        else if (Prefs.getBridgesList().equals("meek")) {
            mBtMeek.setChecked(true);
        }
        else if (Prefs.getBridgesList().equals("obfs4")) {
            mBtObfs4.setChecked(true);
        }
        else {
            mBtDirect.setChecked(false);
            mBtMeek.setChecked(false);
            mBtObfs4.setChecked(false);
        }

        mBtNew.setChecked(false);
        mBtMoat.setChecked(false);
    }
}
