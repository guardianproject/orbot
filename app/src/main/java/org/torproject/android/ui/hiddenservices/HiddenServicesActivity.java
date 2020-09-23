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
import org.torproject.android.ui.hiddenservices.dialogs.HSActionsDialog;
import org.torproject.android.ui.hiddenservices.dialogs.HSDataDialog;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

import java.io.File;

public class HiddenServicesActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_READ_ZIP_BACKUP = 125;
    private static final String BUNDLE_KEY_SHOW_USER_SERVICES = "show_user_services";
    private ContentResolver mResolver;
    private OnionListAdapter mAdapter;
    private RadioButton radioShowUserServices, radioShowAppServices;
    private FloatingActionButton fab;
    private String mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_hs_list_view);
        radioShowUserServices = findViewById(R.id.radioUserServices);
        radioShowAppServices = findViewById(R.id.radioAppServices);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mResolver = getContentResolver();

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            HSDataDialog dialog = new HSDataDialog();
            dialog.show(getSupportFragmentManager(), "HSDataDialog");
        });

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
            arguments.putInt("_id", item.getInt(item.getColumnIndex(HSContentProvider.HiddenService._ID)));
            arguments.putString("port", item.getString(item.getColumnIndex(HSContentProvider.HiddenService.PORT)));
            arguments.putString("onion", item.getString(item.getColumnIndex(HSContentProvider.HiddenService.DOMAIN)));
            arguments.putInt("auth_cookie", item.getInt(item.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE)));
            arguments.putString("auth_cookie_value", item.getString(item.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE_VALUE)));

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
        File backupDir = DiskUtils.getOrCreateLegacyBackupDir();
        File[] files = backupDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (files != null) {
            if (files.length == 0) {
                Toast.makeText(this, R.string.create_a_backup_first, Toast.LENGTH_LONG).show();
                return;
            }

            CharSequence[] fileNames = new CharSequence[files.length];
            for (int i = 0; i < files.length; i++) fileNames[i] = files[i].getName();

            new AlertDialog.Builder(this)
                    .setTitle(R.string.restore_backup)
                    .setItems(fileNames, (dialog, which) -> new BackupUtils(HiddenServicesActivity.this).restoreZipBackupLegacy(files[which]))
                    .show();

        }
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        if (request == REQUEST_CODE_READ_ZIP_BACKUP) {
            if (response != RESULT_OK) return;
            BackupUtils backupUtils = new BackupUtils(this);
            backupUtils.restoreZipBackup(data.getData());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_restore_backup) {
            if (DiskUtils.supportsStorageAccessFramework()) {
                Intent readFile = DiskUtils.createReadFileIntent("application/zip");
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
        switch (view.getId()) {
            case R.id.radioUserServices:
                filterServices(true);
                break;
            case R.id.radioAppServices:
                filterServices(false);
                break;
            default:
                break;
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
