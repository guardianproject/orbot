package org.torproject.android.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
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

public class BridgeWizardActivity extends AppCompatActivity {

    private static final int MOAT_REQUEST_CODE = 666;
    private static final int CUSTOM_BRIDGES_REQUEST_CODE = 1312;

    private static TextView mTvStatus;
    private static HostTester runningHostTest;

    private RadioButton mBtDirect;
    private RadioButton mBtObfs4;
    private RadioButton mBtMeek;
    private RadioButton mBtCustom;

    private View mBtnConfgiureCustomBridges;

    private static final String BUNDLE_KEY_TV_STATUS_VISIBILITY = "visibility";
    private static final String BUNDLE_KEY_TV_STATUS_TEXT = "text";

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
        if (savedInstanceState == null) {
            mTvStatus.setVisibility(View.GONE);
        } else {
            mTvStatus.setVisibility(savedInstanceState.getInt(BUNDLE_KEY_TV_STATUS_VISIBILITY, View.GONE));
            mTvStatus.setText(savedInstanceState.getString(BUNDLE_KEY_TV_STATUS_TEXT, ""));
        }

        setTitle(getString(R.string.bridges));

        findViewById(R.id.btnMoat).setOnClickListener(v -> {
            cancelHostTestIfRunning();
            startActivityForResult(new Intent(BridgeWizardActivity.this, MoatActivity.class), MOAT_REQUEST_CODE);
        });

        mBtDirect = findViewById(R.id.btnBridgesDirect);
        mBtDirect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            Prefs.setBridgesList("");
            Prefs.putBridgesEnabled(false);
            testBridgeConnection();

        });

        mBtObfs4 = findViewById(R.id.btnBridgesObfs4);
        mBtObfs4.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            Prefs.setBridgesList("obfs4");
            Prefs.putBridgesEnabled(true);
            testBridgeConnection();
        });

        mBtMeek = findViewById(R.id.btnBridgesMeek);
        mBtMeek.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            Prefs.setBridgesList("meek");
            Prefs.putBridgesEnabled(true);
            testBridgeConnection();
        });

        mBtCustom = findViewById(R.id.btnCustomBridges);
        mBtCustom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cancelHostTestIfRunning();
                mTvStatus.setVisibility(View.GONE);
                mBtnConfgiureCustomBridges.setVisibility(View.VISIBLE);
            } else {
                mBtnConfgiureCustomBridges.setVisibility(View.GONE);
            }
        });

        mBtnConfgiureCustomBridges = findViewById(R.id.btnConfigureCustomBridges);
        mBtnConfgiureCustomBridges.setOnClickListener(v ->
                startActivityForResult(new Intent(BridgeWizardActivity.this, CustomBridgesActivity.class), CUSTOM_BRIDGES_REQUEST_CODE));
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        } else if (requestCode == CUSTOM_BRIDGES_REQUEST_CODE) {
            if (noBridgesSet()) mBtDirect.setChecked(true);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void testBridgeConnection() {
        cancelHostTestIfRunning();
        HostTester hostTester = new HostTester();
        if (TextUtils.isEmpty(Prefs.getBridgesList()) || (!Prefs.bridgesEnabled())) {
            hostTester.execute("check.torproject.org", "443");
        } else if (Prefs.getBridgesList().equals("meek")) {
            hostTester.execute("meek.azureedge.net", "443", "d2cly7j4zqgua7.cloudfront.net", "443");
        } else if (Prefs.getBridgesList().equals("obfs4")) {
            hostTester.execute("85.17.30.79", "443", "154.35.22.9", "443", "192.99.11.54", "443");
        } else {
            hostTester = null;
            mTvStatus.setText("");
        }
        if (hostTester != null) runningHostTest = hostTester;
    }

    private class HostTester extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            // Pre Code
            mTvStatus.setVisibility(View.VISIBLE);
            mTvStatus.setText(mBtDirect.isChecked() ? R.string.testing_tor_direct : R.string.testing_bridges);
        }

        @Override
        protected Boolean doInBackground(String... host) {
            // Background Code
            for (int i = 0; i < host.length; i++) {
                if (isCancelled()) return null;
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
            runningHostTest = null;
            if (result) {
                int stringRes = mBtDirect.isChecked() ? R.string.testing_tor_direct_success : R.string.testing_bridges_success;
                mTvStatus.setText(stringRes);
            } else {
                mTvStatus.setText(R.string.testing_bridges_fail);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        if (mTvStatus != null) {
            savedInstanceState.putInt(BUNDLE_KEY_TV_STATUS_VISIBILITY, mTvStatus.getVisibility());

            if (!TextUtils.isEmpty(mTvStatus.getText()))
                savedInstanceState.putString(BUNDLE_KEY_TV_STATUS_TEXT, mTvStatus.getText().toString());
        }
        
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        cancelHostTestIfRunning();
        mTvStatus = null;
        super.onDestroy();
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        return connected;
    }

    private void evaluateBridgeListState() {
        Log.d(getClass().getSimpleName(), String.format("bridgesEnabled=%b, bridgesList=%s", Prefs.bridgesEnabled(), Prefs.getBridgesList()));
        if (noBridgesSet()) {
            mBtDirect.setChecked(true);
        } else if (Prefs.getBridgesList().equals("meek")) {
            mBtMeek.setChecked(true);
        } else if (Prefs.getBridgesList().equals("obfs4")) {
            mBtObfs4.setChecked(true);
        } else {
            mBtCustom.setChecked(true);
        }
    }

    private static void cancelHostTestIfRunning() {
        if (runningHostTest != null) {
            runningHostTest.cancel(true);
            runningHostTest = null;
        }
    }

    private static boolean noBridgesSet() {
        return !Prefs.bridgesEnabled() || Prefs.getBridgesList().trim().equals("");
    }
}
