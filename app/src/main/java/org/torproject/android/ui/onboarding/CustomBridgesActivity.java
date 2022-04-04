/* Copyright (c) 2020, Benjamin Erhart, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */
package org.torproject.android.ui.onboarding;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import net.freehaven.tor.control.TorControlCommands;

import org.json.JSONArray;
import org.torproject.android.R;
import org.torproject.android.core.ClipboardUtils;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.util.Prefs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class CustomBridgesActivity extends AppCompatActivity implements TextWatcher {

    private static final String EMAIL_TOR_BRIDGES = "bridges@torproject.org";
    private static final String URL_TOR_BRIDGES = "https://bridges.torproject.org/bridges";

    private EditText mEtPastedBridges;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_bridges);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ((TextView) findViewById(R.id.tvDescription)).setText(getString(R.string.in_a_browser, URL_TOR_BRIDGES));

        findViewById(R.id.btCopyUrl).setOnClickListener(v -> ClipboardUtils.copyToClipboard("bridge_url", URL_TOR_BRIDGES, getString(R.string.done), this));

        String bridges = Prefs.getBridgesList().trim();
        if (!Prefs.bridgesEnabled() || userHasSetPreconfiguredBridge(bridges)) {
            bridges = null;
        }

        mEtPastedBridges = findViewById(R.id.etPastedBridges);
        mEtPastedBridges.setOnTouchListener((v, event) -> {
            if (v.hasFocus()) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_SCROLL) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
            }
            return false;
        });

        mEtPastedBridges.setText(bridges);
        mEtPastedBridges.addTextChangedListener(this);
        final IntentIntegrator integrator = new IntentIntegrator(this);

        findViewById(R.id.btScanQr).setOnClickListener(v -> integrator.initiateScan());
        findViewById(R.id.btShareQr).setOnClickListener(v -> {
            String setBridges = Prefs.getBridgesList();
            if (!TextUtils.isEmpty(setBridges)) {
                try {
                    integrator.shareText("bridge://" + URLEncoder.encode(setBridges, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.btEmail).setOnClickListener(v -> {
            String requestText = "get transport";
            String emailUrl = String.format("mailto: %s?subject=%s&body=%s" ,
                    Uri.encode(EMAIL_TOR_BRIDGES),
                    Uri.encode(requestText),
                    Uri.encode(requestText));
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(emailUrl));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, requestText);
            emailIntent.putExtra(Intent.EXTRA_TEXT, requestText);
            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        IntentResult scanResult = IntentIntegrator.parseActivityResult(request, response, data);

        if (scanResult != null) {
            String results = scanResult.getContents();

            if (!TextUtils.isEmpty(results)) {
                try {

                    int urlIdx = results.indexOf("://");

                    if (urlIdx != -1) {
                        results = URLDecoder.decode(results, "UTF-8");
                        results = results.substring(urlIdx + 3);

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
                    Log.e(getClass().getSimpleName(), "unsupported", e);
                }
            }

            setResult(RESULT_OK);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Ignored.
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Ignored.
    }

    @Override
    public void afterTextChanged(Editable editable) {
        setNewBridges(editable.toString(), false);
    }

    private void setNewBridges(String newBridgeValue) {
        setNewBridges(newBridgeValue, true);
    }

    private void setNewBridges(String bridges, boolean updateEditText) {
        if (bridges != null) {
            bridges = bridges.trim();

            if (TextUtils.isEmpty(bridges)) {
                bridges = null;
            }
        }

        if (updateEditText) {
            mEtPastedBridges.setText(bridges);
        }

        Prefs.setBridgesList(bridges);
        Prefs.putBridgesEnabled(bridges != null);

        Intent intent = new Intent(this, OrbotService.class);
        intent.setAction(TorControlCommands.SIGNAL_RELOAD);
        startService(intent);
    }

    private static boolean userHasSetPreconfiguredBridge(String bridges) {
        if (bridges == null) return false;
        return bridges.equals("obfs4") || bridges.equals("meek") || bridges.equals("snowflake") || bridges.equals("snowflake-amp");
    }
}
