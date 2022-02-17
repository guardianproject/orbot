package org.torproject.android.ui.v3onionservice;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.core.LocaleHelper;

import java.io.File;

public class OnionServiceActivity extends AppCompatActivity {

    static final String BUNDLE_KEY_ID = "id", BUNDLE_KEY_PORT = "port", BUNDLE_KEY_DOMAIN = "domain", BUNDLE_KEY_PATH = "path";
    private static final String BASE_WHERE_SELECTION_CLAUSE = OnionServiceContentProvider.OnionService.CREATED_BY_USER + "=";
    private static final String BUNDLE_KEY_SHOW_USER_SERVICES = "show_user_key";
    private static final int REQUEST_CODE_READ_ZIP_BACKUP = 347;
    private RadioButton radioShowUserServices;
    private FloatingActionButton fab;
    private ContentResolver mContentResolver;
    private OnionV3ListAdapter mAdapter;
    private CoordinatorLayout mLayoutRoot;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_hosted_services);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLayoutRoot = findViewById(R.id.hostedServiceCoordinatorLayout);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> new OnionServiceCreateDialogFragment().show(getSupportFragmentManager(), OnionServiceCreateDialogFragment.class.getSimpleName()));

        mContentResolver = getContentResolver();
        mAdapter = new OnionV3ListAdapter(this, mContentResolver.query(OnionServiceContentProvider.CONTENT_URI, OnionServiceContentProvider.PROJECTION, BASE_WHERE_SELECTION_CLAUSE + '1', null, null));
        mContentResolver.registerContentObserver(OnionServiceContentProvider.CONTENT_URI, true, new OnionServiceObserver(new Handler()));

        ListView onionList = findViewById(R.id.onion_list);

        radioShowUserServices = findViewById(R.id.radioUserServices);
        RadioButton radioShowAppServices = findViewById(R.id.radioAppServices);
        boolean showUserServices = radioShowAppServices.isChecked() || bundle == null || bundle.getBoolean(BUNDLE_KEY_SHOW_USER_SERVICES, false);
        if (showUserServices) radioShowUserServices.setChecked(true);
        else radioShowAppServices.setChecked(true);
        filterServices(showUserServices);
        onionList.setAdapter(mAdapter);
        onionList.setOnItemClickListener((parent, view, position, id) -> {
            Cursor item = (Cursor) parent.getItemAtPosition(position);
            Bundle arguments = new Bundle();
            arguments.putInt(BUNDLE_KEY_ID, item.getInt(item.getColumnIndex(OnionServiceContentProvider.OnionService._ID)));
            arguments.putString(BUNDLE_KEY_PORT, item.getString(item.getColumnIndex(OnionServiceContentProvider.OnionService.PORT)));
            arguments.putString(BUNDLE_KEY_DOMAIN, item.getString(item.getColumnIndex(OnionServiceContentProvider.OnionService.DOMAIN)));
            arguments.putString(BUNDLE_KEY_PATH, item.getString(item.getColumnIndex(OnionServiceContentProvider.OnionService.PATH)));
            OnionServiceActionsDialogFragment dialog = new OnionServiceActionsDialogFragment(arguments);
            dialog.show(getSupportFragmentManager(), OnionServiceActionsDialogFragment.class.getSimpleName());
        });
    }

    private void filterServices(boolean showUserServices) {
        String predicate;
        if (showUserServices) {
            predicate = "1";
            fab.show();
        } else {
            predicate = "0";
            fab.hide();
        }
        mAdapter.changeCursor(mContentResolver.query(OnionServiceContentProvider.CONTENT_URI, OnionServiceContentProvider.PROJECTION,
                BASE_WHERE_SELECTION_CLAUSE + predicate, null, null));
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle icicle) {
        super.onSaveInstanceState(icicle);
        icicle.putBoolean(BUNDLE_KEY_SHOW_USER_SERVICES, radioShowUserServices.isChecked());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_restore_backup) {
            if (DiskUtils.supportsStorageAccessFramework()) {
                Intent readFileIntent = DiskUtils.createReadFileIntent(ZipUtilities.ZIP_MIME_TYPE);
                startActivityForResult(readFileIntent, REQUEST_CODE_READ_ZIP_BACKUP);
            } else { // APIs 16, 17, 18
                doRestoreLegacy();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void doRestoreLegacy() { // APIs 16, 17, 18
        File backupDir = DiskUtils.getOrCreateLegacyBackupDir(getString(R.string.app_name));
        File[] files = backupDir.listFiles(ZipUtilities.FILTER_ZIP_FILES);
        if (files == null || files.length == 0) {
            Toast.makeText(this, R.string.create_a_backup_first, Toast.LENGTH_LONG).show();
            return;
        }

        CharSequence[] fileNames = new CharSequence[files.length];
        for (int i = 0; i < files.length; i++) fileNames[i] = files[i].getName();

        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_backup)
                .setItems(fileNames, (dialog, which) -> new V3BackupUtils(this).restoreZipBackupV3Legacy(files[which]))
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int result, Intent data) {
        super.onActivityResult(requestCode, result, data);
        if (requestCode == REQUEST_CODE_READ_ZIP_BACKUP && result == RESULT_OK) {
            new V3BackupUtils(this).restoreZipBackupV3(data.getData());
        }
    }

    public void onRadioButtonClick(View view) {
        int id = view.getId();
        if (id == R.id.radioUserServices)
            filterServices(true);
        else if (id == R.id.radioAppServices)
            filterServices(false);
    }

    private class OnionServiceObserver extends ContentObserver {

        OnionServiceObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            filterServices(radioShowUserServices.isChecked()); // updates adapter
            showBatteryOptimizationsMessageIfAppropriate();
        }
    }

    void showBatteryOptimizationsMessageIfAppropriate() {
        if (!PermissionManager.isAndroidM()) return;
        Cursor activeServices = getContentResolver().query(OnionServiceContentProvider.CONTENT_URI, OnionServiceContentProvider.PROJECTION,
                OnionServiceContentProvider.OnionService.ENABLED + "=1", null, null);
        if (activeServices == null) return;
        if (activeServices.getCount() > 0)
            PermissionManager.requestBatteryPermissions(this, mLayoutRoot);
        else
            PermissionManager.requestDropBatteryPermissions(this, mLayoutRoot);
        activeServices.close();
    }
}
