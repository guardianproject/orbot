package org.torproject.android.mini.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.torproject.android.mini.MiniMainActivity;
import org.torproject.android.mini.R;
import org.torproject.android.service.util.TorServiceUtils;
import org.torproject.android.service.vpn.TorifiedApp;

import static org.torproject.android.mini.MiniMainActivity.getApp;
import static org.torproject.android.service.vpn.VpnPrefs.PREFS_KEY_TORIFIED;

public class AppConfigActivity extends AppCompatActivity {

    TorifiedApp mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_config);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String pkgId = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);

        ApplicationInfo aInfo = null;
        try {
            aInfo = getPackageManager().getApplicationInfo(pkgId, 0);
            mApp = getApp(this, aInfo);

            getSupportActionBar().setIcon(mApp.getIcon());

            setTitle(mApp.getName());
        }
        catch (Exception e){}

    }


    private void removeApp ()
    {
        mApp.setTorified(false);

        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());

        String tordAppString = prefs.getString(PREFS_KEY_TORIFIED, "");

        tordAppString = tordAppString.replace(mApp.getPackageName()+"|","");

        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREFS_KEY_TORIFIED, tordAppString);
        edit.commit();

        Intent response = new Intent();
        setResult(RESULT_OK,response);

        finish();
    }

    /*
     * Create the UI Options Menu (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.menu_remove_app) {
            removeApp();
        }

        return super.onOptionsItemSelected(item);
    }




}
