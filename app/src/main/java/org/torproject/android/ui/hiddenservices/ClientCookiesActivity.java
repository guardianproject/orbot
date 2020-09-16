package org.torproject.android.ui.hiddenservices;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.ui.hiddenservices.adapters.ClientCookiesAdapter;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.dialogs.AddCookieDialog;
import org.torproject.android.ui.hiddenservices.dialogs.CookieActionsDialog;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

public class ClientCookiesActivity extends AppCompatActivity {
    public final int WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTIONBAR = 3;

    private ContentResolver mResolver;
    private ClientCookiesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_client_cookies);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mResolver = getContentResolver();

        findViewById(R.id.fab).setOnClickListener(view -> {
            AddCookieDialog dialog = new AddCookieDialog();
            dialog.show(getSupportFragmentManager(), "AddCookieDialog");
        });

        mAdapter = new ClientCookiesAdapter(this,
                mResolver.query(CookieContentProvider.CONTENT_URI, CookieContentProvider.PROJECTION, null, null, null), 0);

        mResolver.registerContentObserver(
                CookieContentProvider.CONTENT_URI, true, new HSObserver(new Handler())
        );

        ListView cookies = findViewById(R.id.clien_cookies_list);
        cookies.setAdapter(mAdapter);

        cookies.setOnItemClickListener((parent, view, position, id) -> {
            Cursor item = (Cursor) parent.getItemAtPosition(position);

            Bundle arguments = new Bundle();
            arguments.putInt("_id", item.getInt(item.getColumnIndex(CookieContentProvider.ClientCookie._ID)));

            arguments.putString("domain", item.getString(item.getColumnIndex(CookieContentProvider.ClientCookie.DOMAIN)));
            arguments.putString("auth_cookie_value", item.getString(item.getColumnIndex(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE)));
            arguments.putInt("enabled", item.getInt(item.getColumnIndex(CookieContentProvider.ClientCookie.ENABLED)));

            CookieActionsDialog dialog = new CookieActionsDialog();
            dialog.setArguments(arguments);
            dialog.show(getSupportFragmentManager(), CookieActionsDialog.class.getSimpleName());
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

    private static final int REQUEST_CODE_READ_COOKIE = 54;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.cookie_restore_backup) {
            if (PermissionManager.isLollipopOrHigher()
                    && !PermissionManager.hasExternalWritePermission(this)) {
                PermissionManager.requestExternalWritePermissions(this, WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTIONBAR);
                return true;
            }

            Intent readCookieIntent = DiskUtils.createReadFileIntent("application/json");
            startActivityForResult(readCookieIntent, REQUEST_CODE_READ_COOKIE);

        } else if (id == R.id.cookie_from_qr) {
            IntentIntegrator integrator = new IntentIntegrator(ClientCookiesActivity.this);
            integrator.initiateScan();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length < 1
                || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        switch (requestCode) {
            case CookieActionsDialog.WRITE_EXTERNAL_STORAGE_FROM_COOKIE_ACTION_DIALOG: {
                try {
                    CookieActionsDialog activeDialog = (CookieActionsDialog) getSupportFragmentManager().findFragmentByTag(CookieActionsDialog.class.getSimpleName());
                    activeDialog.doBackup();
                } catch (ClassCastException e) {
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        if (request == REQUEST_CODE_READ_COOKIE) {
            if (response != RESULT_OK) return;
            String cookieStr = DiskUtils.readFileFromInputStream(getContentResolver(), data.getData());
            BackupUtils backup = new BackupUtils(this);
            backup.restoreCookieBackup(cookieStr);
            return;
        }

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
