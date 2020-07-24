/* Copyright (c) 2020, Benjamin Erhart, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */
package org.torproject.android.ui.onboarding;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.torproject.android.R;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import static org.torproject.android.MainConstants.EMAIL_TOR_BRIDGES;
import static org.torproject.android.MainConstants.URL_TOR_BRIDGES;

public class CustomBridgesActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    private EditText mEtPastedBridges;

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

        setTitle(getString(R.string.use_custom_bridges));

        ((TextView) findViewById(R.id.tvDescription)).setText(getString(R.string.in_a_browser, URL_TOR_BRIDGES));

        findViewById(R.id.btCopyUrl).setOnClickListener(this);

        String bridges = Prefs.getBridgesList().trim();
        if (!Prefs.bridgesEnabled() || bridges.equals("obfs4") || bridges.equals("meek")) {
            bridges = null;
        }

        mEtPastedBridges = findViewById(R.id.etPastedBridges);
        configureMultilineEditTextInScrollView(mEtPastedBridges);
        mEtPastedBridges.setText(bridges);
        mEtPastedBridges.addTextChangedListener(this);

        findViewById(R.id.btScanQr).setOnClickListener(this);

        findViewById(R.id.btShareQr).setOnClickListener(this);

        findViewById(R.id.btEmail).setOnClickListener(this);
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
    public void onClick(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);

        switch (view.getId()) {
            case R.id.btCopyUrl:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(URL_TOR_BRIDGES, URL_TOR_BRIDGES));

                    Toast.makeText(this, R.string.done, Toast.LENGTH_LONG).show();
                }

                break;

            case R.id.btScanQr:
                integrator.initiateScan();

                break;

            case R.id.btShareQr:
                String bridges = Prefs.getBridgesList();

                if (!TextUtils.isEmpty(bridges)) {
                    try {
                        bridges = "bridge://" + URLEncoder.encode(bridges, "UTF-8");

                        integrator.shareText(bridges);

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                break;

            case R.id.btEmail:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + EMAIL_TOR_BRIDGES));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "get transport");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "get transport");
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));

                break;
        }
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
        intent.setAction(TorServiceConstants.CMD_SIGNAL_HUP);
        startService(intent);
    }

    // configures an EditText we assume to be multiline and nested in a ScrollView to be independently scrollable
    @SuppressLint("ClickableViewAccessibility")
    private static void configureMultilineEditTextInScrollView(EditText et) {
        et.setVerticalScrollBarEnabled(true);
        et.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        et.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        et.setMovementMethod(ScrollingMovementMethod.getInstance());
        et.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            if ((event.getAction() & MotionEvent.ACTION_UP) != 0 && (event.getActionMasked() & MotionEvent.ACTION_UP) != 0) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
    }
}
