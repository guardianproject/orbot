package org.torproject.android.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.os.ConfigurationCompat;

import org.tensorflow.lite.support.label.Category;
import org.torproject.android.R;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.tensor.OrbotMLManager;
import org.torproject.android.tensor.TextClassificationHelper;

import java.util.List;
import java.util.Locale;

public class BridgeWizardActivity extends AppCompatActivity {

    private static final int MOAT_REQUEST_CODE = 666;
    private static final int CUSTOM_BRIDGES_REQUEST_CODE = 1312;
    private RadioButton mBtDirect;
    private RadioButton mBtObfs4;
    private RadioButton mBtCustom;
    private RadioButton mBtSnowflake;
    private RadioButton mBtnSnowflakeAmp;
    private View mBtnConfgiureCustomBridges;

    private static boolean noBridgesSet() {
        return !Prefs.bridgesEnabled() || Prefs.getBridgesList().trim().equals("");
    }

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
        if(android.os.Build.VERSION.SDK_INT < 28){
            findViewById(R.id.btnMoat).setVisibility(View.GONE);
            findViewById(R.id.btnBridgesSnowflake).setVisibility(View.GONE);
            findViewById(R.id.btnSnowflakeAmp).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.btnMoat).setOnClickListener(v -> startActivityForResult(new Intent(BridgeWizardActivity.this, MoatActivity.class), MOAT_REQUEST_CODE));
        }

        mBtDirect = findViewById(R.id.btnBridgesDirect);
        mBtDirect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            Prefs.setBridgesList("");
            Prefs.putBridgesEnabled(false);
            useMLConnectionClassifier();
        });

        mBtObfs4 = findViewById(R.id.btnBridgesObfs4);
        mBtObfs4.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            Prefs.setBridgesList("obfs4");
            Prefs.putBridgesEnabled(true);
            useMLConnectionClassifier();
        });

        mBtSnowflake = findViewById(R.id.btnBridgesSnowflake);
        mBtSnowflake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            Prefs.setBridgesList("snowflake");
            Prefs.putBridgesEnabled(true);
            useMLConnectionClassifier();
        });

        mBtnSnowflakeAmp = findViewById(R.id.btnSnowflakeAmp);
        mBtnSnowflakeAmp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            Prefs.setBridgesList("snowflake-amp");
            Prefs.putBridgesEnabled(true);
            useMLConnectionClassifier();
        });

        mBtCustom = findViewById(R.id.btnCustomBridges);
        mBtCustom.setOnCheckedChangeListener((buttonView, isChecked) ->
                mBtnConfgiureCustomBridges.setVisibility(isChecked ? View.VISIBLE : View.GONE));

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

    private void evaluateBridgeListState() {
        Log.d(getClass().getSimpleName(), String.format("bridgesEnabled=%b, bridgesList=%s", Prefs.bridgesEnabled(), Prefs.getBridgesList()));
        if (noBridgesSet()) {
            mBtDirect.setChecked(true);
        } else if (Prefs.getBridgesList().equals("obfs4")) {
            mBtObfs4.setChecked(true);
        } else if (Prefs.getBridgesList().equals("snowflake")) {
            mBtSnowflake.setChecked(true);
        } else if (Prefs.getBridgesList().equals("snowflake-amp")) {
            mBtnSnowflakeAmp.setChecked(true);
        } else {
            mBtCustom.setChecked(true);
        }
        useMLConnectionClassifier();
    }

    OrbotMLManager mlManager = null;

    private void useMLConnectionClassifier () {
        if (mlManager == null)
            mlManager = new OrbotMLManager(this);

        String connConfig = OrbotMLManager.generateConnectionConfigurationToken(this);
        Log.d("OrbotML","connConfig="+connConfig);
        mlManager.useMLConnectionClassifier(connConfig);
    }


}
