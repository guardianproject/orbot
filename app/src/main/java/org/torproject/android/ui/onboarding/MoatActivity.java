/* Copyright (c) 2020, Benjamin Erhart, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */
package org.torproject.android.ui.onboarding;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.freehaven.tor.control.TorControlCommands;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.ui.dialog.MoatErrorDialogFragment;

import java.io.File;

import IPtProxy.IPtProxy;

/**
 * Implements the MOAT protocol: Fetches OBFS4 bridges via Meek Azure.
 * <p>
 * The bare minimum of the communication is implemented. E.g. no check, if OBFS4 is possible or which
 * protocol version the server wants to speak. The first should be always good, as OBFS4 is the most widely
 * supported bridge type, the latter should be the same as we requested (0.1.0) anyway.
 * <p>
 * API description:
 * https://github.com/NullHypothesis/bridgedb#accessing-the-moat-interface
 */
public class MoatActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {

    private static final String moatBaseUrl = "https://bridges.torproject.org/moat";

    private ImageView mIvCaptcha;
    private ProgressBar mProgressBar;
    private EditText mEtSolution;
    private Button mBtRequest;

    private String mChallenge;
    private byte[] mCaptcha;

    private RequestQueue mQueue;

    private String mOriginalBridges;
    private boolean mOriginalBridgeStatus;

    private boolean mSuccess;

    private boolean mRequestInProgress = true;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String host = intent.getStringExtra(OrbotService.EXTRA_SOCKS_PROXY_HOST);
            int port = intent.getIntExtra(OrbotService.EXTRA_SOCKS_PROXY_PORT, -1);
            String status = intent.getStringExtra(OrbotConstants.EXTRA_STATUS);

            if (TextUtils.isEmpty(host)) {
                host = OrbotConstants.IP_LOCALHOST;
            }

            if (port < 1) {
                port = Integer.parseInt(OrbotConstants.SOCKS_PROXY_PORT_DEFAULT);
            }

            if (TextUtils.isEmpty(status)) {
                status = OrbotConstants.STATUS_OFF;
            }

            setUp(host, port, status);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mIvCaptcha = findViewById(R.id.captchaIv);
        mProgressBar = findViewById(R.id.progressBar);
        mEtSolution = findViewById(R.id.solutionEt);
        mBtRequest = findViewById(R.id.requestBt);

        mEtSolution.setOnEditorActionListener(this);
        mBtRequest.setOnClickListener(this);

        if (savedInstanceState != null) {
            mOriginalBridges = savedInstanceState.getString("originalBridges");
            mOriginalBridgeStatus = savedInstanceState.getBoolean("originalBridgeStatus");
            mChallenge = savedInstanceState.getString("challenge");
            mCaptcha = savedInstanceState.getByteArray("captcha");

            if (mCaptcha != null) {
                mProgressBar.setVisibility(View.GONE);
                mIvCaptcha.setImageBitmap(BitmapFactory.decodeByteArray(mCaptcha, 0, mCaptcha.length));
                mRequestInProgress = false;
                mEtSolution.setEnabled(true);
            }
        } else {
            mIvCaptcha.setVisibility(View.GONE);
            mEtSolution.setEnabled(false);
            mBtRequest.setEnabled(false);

            mOriginalBridges = Prefs.getBridgesList();
            mOriginalBridgeStatus = Prefs.bridgesEnabled();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(OrbotConstants.ACTION_STATUS));

        OrbotService.loadCdnFronts(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.moat, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_refresh).setVisible(!mRequestInProgress);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();

        File fileCacheDir = new File(getCacheDir(),"pt");
        if (!fileCacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            fileCacheDir.mkdir();
        }

        IPtProxy.setStateLocation(fileCacheDir.getAbsolutePath());

        IPtProxy.startObfs4Proxy("DEBUG", false, false, null);

        ProxiedHurlStack phs = new ProxiedHurlStack("127.0.0.1", (int) IPtProxy.meekPort(),
          "url=" + OrbotService.getCdnFront("moat-url")
                  + ";front=" + OrbotService.getCdnFront("moat-front"), "\0");

        mQueue = Volley.newRequestQueue(this, phs);

        if (mCaptcha == null) {
            new Handler(Looper.getMainLooper()).postDelayed(this::fetchCaptcha, 1000);
        }
    }

    @Override
    public void onClick(View view) {
        Log.d(MoatActivity.class.getSimpleName(), "Request Bridge!");

        requestBridges(mEtSolution.getText().toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_refresh) {
            mEtSolution.setText("");
            fetchCaptcha();
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        Log.d(MoatActivity.class.getSimpleName(), "Editor Action: actionId=" + actionId + ", IME_ACTION_GO=" + EditorInfo.IME_ACTION_GO);

        if (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                }

                onClick(mEtSolution);
            }

            return true;
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("originalBridges", mOriginalBridges);
        outState.putBoolean("originalBridgeStatus", mOriginalBridgeStatus);
        outState.putString("challenge", mChallenge);
        outState.putByteArray("captcha", mCaptcha);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        if (!mSuccess) {
            Prefs.setBridgesList(mOriginalBridges);
            Prefs.putBridgesEnabled(mOriginalBridgeStatus);
        }
    }

    private void fetchCaptcha() {
        JsonObjectRequest request = buildRequest("fetch",
                "\"type\": \"client-transports\", \"supported\": [\"obfs4\"]",
                response -> {
                    mRequestInProgress = false;
                    invalidateOptionsMenu();
                    mProgressBar.setVisibility(View.GONE);

                    try {
                        JSONObject data = response.getJSONArray("data").getJSONObject(0);
                        mChallenge = data.getString("challenge");

                        mCaptcha = Base64.decode(data.getString("image"), Base64.DEFAULT);
                        mIvCaptcha.setImageBitmap(BitmapFactory.decodeByteArray(mCaptcha, 0, mCaptcha.length));
                        mIvCaptcha.setVisibility(View.VISIBLE);
                        mEtSolution.setText(null);
                        mEtSolution.setEnabled(true);
                        mBtRequest.setEnabled(true);

                    } catch (JSONException e) {
                        Log.d(MoatActivity.class.getSimpleName(), "Error decoding answer: " + response.toString());

                        displayError(e, response);
                    }

                });

        if (request != null) {
            mRequestInProgress = true;
            invalidateOptionsMenu();
            mProgressBar.setVisibility(View.VISIBLE);
            mIvCaptcha.setVisibility(View.GONE);
            mBtRequest.setEnabled(false);

            mChallenge = null;
            mCaptcha = null;

            mQueue.add(request);
        }
    }

    private void requestBridges(String solution) {
        JsonObjectRequest request = buildRequest("check",
                "\"id\": \"2\", \"type\": \"moat-solution\", \"transport\": \"obfs4\", \"challenge\": \""
                        + mChallenge + "\", \"solution\": \"" + solution + "\", \"qrcode\": \"false\"",
                response -> {
                    mRequestInProgress = false;
                    invalidateOptionsMenu();
                    mProgressBar.setVisibility(View.GONE);

                    try {
                        JSONArray bridges = response.getJSONArray("data").getJSONObject(0).getJSONArray("bridges");

                        Log.d(MoatActivity.class.getSimpleName(), "Bridges: " + bridges.toString());

                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i < bridges.length(); i++) {
                            sb.append(bridges.getString(i)).append("\n");
                        }

                        Prefs.setBridgesList(sb.toString());
                        Prefs.putBridgesEnabled(true);

                        sendIntentToService(TorControlCommands.SIGNAL_RELOAD);

                        mSuccess = true;
                        setResult(RESULT_OK);
                        finish();
                    } catch (JSONException e) {
                        Log.d(MoatActivity.class.getSimpleName(), "Error decoding answer: " + response.toString());

                        displayError(e, response);
                    }
                });

        if (request != null) {
            mRequestInProgress = true;
            invalidateOptionsMenu();
            mProgressBar.setVisibility(View.VISIBLE);
            mEtSolution.setEnabled(false);
            mBtRequest.setEnabled(false);

            mQueue.add(request);
        }
    }

    private JsonObjectRequest buildRequest(String endpoint, String payload, Response.Listener<JSONObject> listener) {
        JSONObject requestBody;

        try {
            requestBody = new JSONObject("{\"data\": [{\"version\": \"0.1.0\", " + payload + "}]}");
        } catch (JSONException e) {
            return null;
        }

        Log.d(MoatActivity.class.getSimpleName(), "Request: " + requestBody.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                moatBaseUrl + "/" + endpoint,
                requestBody,
                listener,
                error -> {
                    mRequestInProgress = false;
                    invalidateOptionsMenu();
                    mProgressBar.setVisibility(View.GONE);

                    Log.d(MoatActivity.class.getSimpleName(), "Error response.");
                    error.printStackTrace();

                    displayError(error, null);
                }
        ) {
            public String getBodyContentType() {
                return "application/vnd.api+json";
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(30000,
                3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        return request;
    }

    private void sendIntentToService(final String action) {
        Intent intent = new Intent(this, OrbotService.class);
        intent.setAction(action);
        startService(intent);
    }

    private void setUp(String host, int port, String status) {
        Log.d(MoatActivity.class.getSimpleName(), "Tor status=" + status);

        // Ignore after successful bridge request.
        if (mSuccess) {
            return;
        }

        switch (status) {
            case OrbotConstants.STATUS_OFF:
                // We need the Meek bridge.
                Prefs.setBridgesList("meek");
                Prefs.putBridgesEnabled(true);

                sendIntentToService(OrbotConstants.ACTION_START);

                break;

            case OrbotConstants.STATUS_ON:
                // Switch to the Meek bridge, if not done, already.
                Prefs.setBridgesList("meek");
                Prefs.putBridgesEnabled(true);

                Log.d(MoatActivity.class.getSimpleName(), "Set up Volley queue. host=" + host + ", port=" + port);

             //   mQueue = Volley.newRequestQueue(this, new ProxiedHurlStack(host, port));

                sendIntentToService(TorControlCommands.SIGNAL_RELOAD);

                if (mCaptcha == null) {
                    fetchCaptcha();
                }

                break;
        }
    }

    private void displayError(Exception exception, JSONObject response) {

        String detail = null;

        // Try to decode potential error response.
        if (response != null) {
            try {
                detail = response.getJSONArray("errors").getJSONObject(0).getString("detail");
            } catch (JSONException e2) {
                // Ignore. Show first exception instead.
            }
        }

        mProgressBar.setVisibility(View.GONE);
        mEtSolution.setEnabled(mIvCaptcha.getVisibility() == View.VISIBLE);
        mBtRequest.setEnabled(mIvCaptcha.getVisibility() == View.VISIBLE);

        if (!isFinishing()) {
            String message = TextUtils.isEmpty(detail) ? exception.getLocalizedMessage() : detail;
            MoatErrorDialogFragment.newInstance(message).show(getSupportFragmentManager(), MoatErrorDialogFragment.TAG);
        }
    }
}
