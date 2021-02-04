package org.torproject.android.ui.hiddenservices;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;

import java.io.File;
import java.util.Locale;

public class ClientCookiesActivity extends AppCompatActivity {
    public static final String BUNDLE_KEY_ID = "_id",
            BUNDLE_KEY_DOMAIN = "domain",
            BUNDLE_KEY_COOKIE = "auth_cookie_value",
            BUNDLE_KEY_ENABLED = "enabled";
    private static final int REQUEST_CODE_READ_COOKIE = 54;
    private ContentResolver mResolver;
    private ClientCookiesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_client_cookies);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mResolver = getContentResolver();

        findViewById(R.id.fab).setOnClickListener(view ->
            new AddCookieDialog().show(getSupportFragmentManager(), AddCookieDialog.class.getSimpleName()));

        mAdapter = new ClientCookiesAdapter(this, mResolver.query(CookieContentProvider.CONTENT_URI, CookieContentProvider.PROJECTION, null, null, null), 0);

        mResolver.registerContentObserver(CookieContentProvider.CONTENT_URI, true, new HSObserver(new Handler()));

        ListView cookies = findViewById(R.id.clien_cookies_list);
        cookies.setAdapter(mAdapter);

        cookies.setOnItemClickListener((parent, view, position, id) -> {
            Cursor item = (Cursor) parent.getItemAtPosition(position);

            Bundle arguments = new Bundle();
            arguments.putInt(BUNDLE_KEY_ID, item.getInt(item.getColumnIndex(CookieContentProvider.ClientCookie._ID)));
            arguments.putString(BUNDLE_KEY_DOMAIN, item.getString(item.getColumnIndex(CookieContentProvider.ClientCookie.DOMAIN)));
            arguments.putString(BUNDLE_KEY_COOKIE, item.getString(item.getColumnIndex(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE)));
            arguments.putInt(BUNDLE_KEY_ENABLED, item.getInt(item.getColumnIndex(CookieContentProvider.ClientCookie.ENABLED)));

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.cookie_restore_backup) {
            if (DiskUtils.supportsStorageAccessFramework()) {
                Intent readCookieIntent = DiskUtils.createReadFileIntent("application/json");
                startActivityForResult(readCookieIntent, REQUEST_CODE_READ_COOKIE);
            } else { // api 16,17,18
                restoreBackupLegacy();
            }

        } else if (id == R.id.cookie_from_qr) {
            IntentIntegrator integrator = new IntentIntegrator(ClientCookiesActivity.this);
            integrator.initiateScan();
        }

        return super.onOptionsItemSelected(item);
    }

    private void restoreBackupLegacy() {
        File backupDir = DiskUtils.getOrCreateLegacyBackupDir(getString(R.string.app_name));

        try {
            File[] files = backupDir.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(".json"));
            if (files != null) {
                if (files.length == 0) {
                    Toast.makeText(this, R.string.create_a_backup_first, Toast.LENGTH_LONG).show();
                    return;
                }

                CharSequence[] fileNames = new CharSequence[files.length];
                for (int i = 0; i < files.length; i++) fileNames[i] = files[i].getName();

                new AlertDialog.Builder(this)
                        .setItems(fileNames, (dialog, which) -> {
                            String text = DiskUtils.readFile(mResolver, files[which]);
                            new BackupUtils(this).restoreCookieBackup(text);
                        })
                        .setTitle(R.string.restore_backup)
                        .show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        if (request == REQUEST_CODE_READ_COOKIE && response == RESULT_OK) {
            String cookieStr = DiskUtils.readFileFromInputStream(mResolver, data.getData());
            new BackupUtils(this).restoreCookieBackup(cookieStr);
        } else {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(request, response, data);
            if (scanResult == null) return;

            String results = scanResult.getContents();
            if (results == null || results.length() < 1) return;
            try {
                JSONObject savedValues = new JSONObject(results);
                ContentValues fields = new ContentValues();
                fields.put(CookieContentProvider.ClientCookie.DOMAIN, savedValues.getString(CookieContentProvider.ClientCookie.DOMAIN));
                fields.put(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE, savedValues.getString(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE));
                mResolver.insert(CookieContentProvider.CONTENT_URI, fields);
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
            }
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
