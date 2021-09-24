package org.torproject.android.ui.hiddenservices;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.ui.hiddenservices.adapters.OnionListAdapter;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.backup.ZipUtilities;
import org.torproject.android.ui.hiddenservices.dialogs.HSActionsDialog;
import org.torproject.android.ui.hiddenservices.dialogs.HSDataDialog;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

import java.io.File;
import java.util.Locale;

public class HiddenServicesActivity extends AppCompatActivity {
    public static final String BUNDLE_KEY_ID = "_id",
            BUNDLE_KEY_PORT = "port",
            BUNDLE_KEY_ONION = "onion",
            BUNDLE_KEY_AUTH_COOKIE = "auth_cookie",
            BUNDLE_KEY_AUTH_COOKIE_VALUE = "auth_cookie_value",
            BUNDLE_KEY_PATH = "path";
    private static final int REQUEST_CODE_READ_ZIP_BACKUP = 125;
    private static final String BUNDLE_KEY_SHOW_USER_SERVICES = "show_user_services";
    private ContentResolver mResolver;
    private OnionListAdapter mAdapter;
    private RadioButton radioShowUserServices;
    private FloatingActionButton fab;
    private String mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_hs_list_view);
        radioShowUserServices = findViewById(R.id.radioUserServices);
        RadioButton radioShowAppServices = findViewById(R.id.radioAppServices);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mResolver = getContentResolver();

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> new HSDataDialog().show(getSupportFragmentManager(), HSDataDialog.class.getSimpleName()));

        mAdapter = new OnionListAdapter(this, mResolver.query(HSContentProvider.CONTENT_URI, HSContentProvider.PROJECTION, mWhere, null, null), 0);
        mResolver.registerContentObserver(HSContentProvider.CONTENT_URI, true, new HSObserver(new Handler()));

        ListView onion_list = findViewById(R.id.onion_list);
        boolean selectUserServices = savedInstanceState == null || savedInstanceState.getBoolean(BUNDLE_KEY_SHOW_USER_SERVICES);
        if (selectUserServices) radioShowUserServices.setChecked(true);
        else radioShowAppServices.setChecked(true);
        filterServices(selectUserServices);
        onion_list.setAdapter(mAdapter);

        onion_list.setOnItemClickListener((parent, view, position, id) -> {
            Cursor item = (Cursor) parent.getItemAtPosition(position);

            Bundle arguments = new Bundle();
            arguments.putInt(BUNDLE_KEY_ID, item.getInt(item.getColumnIndex(HSContentProvider.HiddenService._ID)));
            arguments.putString(BUNDLE_KEY_PORT, item.getString(item.getColumnIndex(HSContentProvider.HiddenService.PORT)));
            arguments.putString(BUNDLE_KEY_ONION, item.getString(item.getColumnIndex(HSContentProvider.HiddenService.DOMAIN)));
            arguments.putInt(BUNDLE_KEY_AUTH_COOKIE, item.getInt(item.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE)));
            arguments.putString(BUNDLE_KEY_AUTH_COOKIE_VALUE, item.getString(item.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE_VALUE)));
            arguments.putString(BUNDLE_KEY_PATH, item.getString(item.getColumnIndex(HSContentProvider.HiddenService.PATH)));

            HSActionsDialog dialog = new HSActionsDialog();
            dialog.setArguments(arguments);
            dialog.show(getSupportFragmentManager(), HSActionsDialog.class.getSimpleName());
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        icicle.putBoolean(BUNDLE_KEY_SHOW_USER_SERVICES, radioShowUserServices.isChecked());
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hs_menu, menu);
        return true;
    }

    private void doRestoreLegacy() { // API 16, 17, 18
        File backupDir = DiskUtils.getOrCreateLegacyBackupDir(getString(R.string.app_name));
        File[] files = backupDir.listFiles(ZipUtilities.FILTER_ZIP_FILES);
        if (files != null) {
            if (files.length == 0) {
                Toast.makeText(this, R.string.create_a_backup_first, Toast.LENGTH_LONG).show();
                return;
            }

            CharSequence[] fileNames = new CharSequence[files.length];
            for (int i = 0; i < files.length; i++) fileNames[i] = files[i].getName();

            new AlertDialog.Builder(this)
                    .setTitle(R.string.restore_backup)
                    .setItems(fileNames, (dialog, which) -> new BackupUtils(this).restoreZipBackupV2Legacy(files[which]))
                    .show();

        }
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        if (request == REQUEST_CODE_READ_ZIP_BACKUP) {
            if (response != RESULT_OK) return;
            BackupUtils backupUtils = new BackupUtils(this);
            backupUtils.restoreZipBackupV2(data.getData());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_restore_backup) {
            if (DiskUtils.supportsStorageAccessFramework()) {
                Intent readFile = DiskUtils.createReadFileIntent(ZipUtilities.ZIP_MIME_TYPE);
                startActivityForResult(readFile, REQUEST_CODE_READ_ZIP_BACKUP);
            } else { // API 16, 17, 18
                doRestoreLegacy();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void filterServices(boolean showUserServices) {
        if (showUserServices) {
            mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=1";
            fab.show();
        } else {
            mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=0";
            fab.hide();
        }
        mAdapter.changeCursor(mResolver.query(HSContentProvider.CONTENT_URI, HSContentProvider.PROJECTION, mWhere, null, null));
    }

    public void onRadioButtonClick(View view) {
        if (view.getId() == R.id.radioUserServices) {
            filterServices(true);
        } else if (view.getId() == R.id.radioAppServices) {
            filterServices(false);
        }
    }

    class HSObserver extends ContentObserver {
        HSObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mAdapter.changeCursor(mResolver.query(HSContentProvider.CONTENT_URI, HSContentProvider.PROJECTION, mWhere, null, null));
            if (!PermissionManager.isLollipopOrHigher()) return;
            Cursor active = mResolver.query(HSContentProvider.CONTENT_URI, HSContentProvider.PROJECTION, HSContentProvider.HiddenService.ENABLED + "=1", null, null);
            if (active == null) return;
            if (active.getCount() > 0) // Call only if there running services
                PermissionManager.requestBatteryPermissions(HiddenServicesActivity.this, getApplicationContext());
            else // Drop whe not needed
                PermissionManager.requestDropBatteryPermissions(HiddenServicesActivity.this, getApplicationContext());

            active.close();
        }
    }
}
