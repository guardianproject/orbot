package org.torproject.android.mini.ui.hiddenservices;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.mini.settings.LocaleHelper;
import org.torproject.android.mini.ui.hiddenservices.adapters.ClientCookiesAdapter;
import org.torproject.android.mini.ui.hiddenservices.dialogs.AddCookieDialog;
import org.torproject.android.mini.ui.hiddenservices.dialogs.CookieActionsDialog;
import org.torproject.android.mini.ui.hiddenservices.dialogs.SelectCookieBackupDialog;
import org.torproject.android.mini.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.mini.ui.hiddenservices.providers.CookieContentProvider;

public class ClientCookiesActivity extends AppCompatActivity {
    public final int WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTIONBAR = 3;

    private ContentResolver mResolver;
    private ClientCookiesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_client_cookies);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mResolver = getContentResolver();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddCookieDialog dialog = new AddCookieDialog();
                dialog.show(getSupportFragmentManager(), "AddCookieDialog");
            }
        });

        mAdapter = new ClientCookiesAdapter(
                this,
                mResolver.query(CookieContentProvider.CONTENT_URI, CookieContentProvider.PROJECTION, null, null, null)
                , 0);

        mResolver.registerContentObserver(
                CookieContentProvider.CONTENT_URI, true, new HSObserver(new Handler())
        );

        ListView cookies = (ListView) findViewById(R.id.clien_cookies_list);
        cookies.setAdapter(mAdapter);

        cookies.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor item = (Cursor) parent.getItemAtPosition(position);

                Bundle arguments = new Bundle();
                arguments.putInt(
                        "_id", item.getInt(item.getColumnIndex(CookieContentProvider.ClientCookie._ID))
                );

                arguments.putString(
                        "domain", item.getString(item.getColumnIndex(CookieContentProvider.ClientCookie.DOMAIN))
                );

                arguments.putString(
                        "auth_cookie_value", item.getString(item.getColumnIndex(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE))
                );

                arguments.putInt(
                        "enabled", item.getInt(item.getColumnIndex(CookieContentProvider.ClientCookie.ENABLED))
                );

                CookieActionsDialog dialog = new CookieActionsDialog();
                dialog.setArguments(arguments);
                dialog.show(getSupportFragmentManager(), "CookieActionsDialog");
            }
        });

    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cookie_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.cookie_restore_backup) {
            if (PermissionManager.isLollipopOrHigher()
                    && !PermissionManager.hasExternalWritePermission(this)) {
                PermissionManager.requestExternalWritePermissions(this, WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTIONBAR);
                return true;
            }

            SelectCookieBackupDialog dialog = new SelectCookieBackupDialog();
            dialog.show(getSupportFragmentManager(), "SelectCookieBackupDialog");

        } else if (id == R.id.cookie_from_qr) {
            IntentIntegrator integrator = new IntentIntegrator(ClientCookiesActivity.this);
            integrator.initiateScan();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (grantResults.length < 1
                || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTIONBAR: {
                SelectCookieBackupDialog dialog = new SelectCookieBackupDialog();
                dialog.show(getSupportFragmentManager(), "SelectCookieBackupDialog");
                break;
            }
            case CookieActionsDialog.WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTION_DIALOG: {
                Toast.makeText(this, R.string.click_again_for_backup, Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        IntentResult scanResult = IntentIntegrator.parseActivityResult(request, response, data);

        if (scanResult == null) return;

        String results = scanResult.getContents();

        if (results == null || results.length() < 1) return;

        try {
            JSONObject savedValues = new JSONObject(results);
            ContentValues fields = new ContentValues();

            fields.put(
                    CookieContentProvider.ClientCookie.DOMAIN,
                    savedValues.getString(CookieContentProvider.ClientCookie.DOMAIN)
            );

            fields.put(
                    CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE,
                    savedValues.getString(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE)
            );

            mResolver.insert(CookieContentProvider.CONTENT_URI, fields);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
        }
    }

    class HSObserver extends ContentObserver {
        HSObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mAdapter.changeCursor(mResolver.query(
                    CookieContentProvider.CONTENT_URI, CookieContentProvider.PROJECTION, null, null, null
            ));
        }
    }

}
