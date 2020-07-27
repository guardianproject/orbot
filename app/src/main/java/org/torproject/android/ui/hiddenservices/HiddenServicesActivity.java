package org.torproject.android.ui.hiddenservices;


import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import org.torproject.android.R;
import org.torproject.android.settings.LocaleHelper;
import org.torproject.android.ui.hiddenservices.adapters.OnionListAdapter;
import org.torproject.android.ui.hiddenservices.dialogs.HSActionsDialog;
import org.torproject.android.ui.hiddenservices.dialogs.HSDataDialog;
import org.torproject.android.ui.hiddenservices.dialogs.SelectHSBackupDialog;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

public class HiddenServicesActivity extends AppCompatActivity {
    public final int WRITE_EXTERNAL_STORAGE_FROM_ACTIONBAR = 1;
    private ContentResolver mResolver;
    private OnionListAdapter mAdapter;
    private FloatingActionButton fab;

    private String mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_hs_list_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mResolver = getContentResolver();

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            HSDataDialog dialog = new HSDataDialog();
            dialog.show(getSupportFragmentManager(), "HSDataDialog");
        });

        mAdapter = new OnionListAdapter(
                this,
                mResolver.query(
                        HSContentProvider.CONTENT_URI, HSContentProvider.PROJECTION, mWhere, null, null
                ),
                0
        );

        mResolver.registerContentObserver(
                HSContentProvider.CONTENT_URI, true, new HSObserver(new Handler())
        );

        ListView onion_list = findViewById(R.id.onion_list);
        onion_list.setAdapter(mAdapter);

        onion_list.setOnItemClickListener((parent, view, position, id) -> {
            Cursor item = (Cursor) parent.getItemAtPosition(position);

            Bundle arguments = new Bundle();
            arguments.putInt(
                    "_id", item.getInt(item.getColumnIndex(HSContentProvider.HiddenService._ID))
            );

            arguments.putString(
                    "port", item.getString(item.getColumnIndex(HSContentProvider.HiddenService.PORT))
            );

            arguments.putString(
                    "onion", item.getString(item.getColumnIndex(HSContentProvider.HiddenService.DOMAIN))
            );

            arguments.putInt(
                    "auth_cookie", item.getInt(item.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE))
            );

            arguments.putString(
                    "auth_cookie_value", item.getString(item.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE_VALUE))
            );

            HSActionsDialog dialog = new HSActionsDialog();
            dialog.setArguments(arguments);
            dialog.show(getSupportFragmentManager(), HSActionsDialog.class.getSimpleName());
        });
    }

    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hs_menu, menu);

        MenuItem item = menu.findItem(R.id.hs_type);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.array_hs_types, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View v, int pos, long id) {
                if (pos == 0) {
                    mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=1";
                    fab.show();
                } else {
                    mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=0";
                    fab.hide();
                }

                mAdapter.changeCursor(mResolver.query(
                        HSContentProvider.CONTENT_URI, HSContentProvider.PROJECTION, mWhere, null, null
                ));
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // Do nothing
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_restore_backup) {
            if (PermissionManager.isLollipopOrHigher()
                    && !PermissionManager.hasExternalWritePermission(this)) {
                PermissionManager.requestExternalWritePermissions(this, WRITE_EXTERNAL_STORAGE_FROM_ACTIONBAR);
                return true;
            }

            SelectHSBackupDialog dialog = new SelectHSBackupDialog();
            dialog.show(getSupportFragmentManager(), "SelectHSBackupDialog");
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
            case WRITE_EXTERNAL_STORAGE_FROM_ACTIONBAR: {
                SelectHSBackupDialog dialog = new SelectHSBackupDialog();
                dialog.show(getSupportFragmentManager(), "SelectHSBackupDialog");
                break;
            }
            case HSActionsDialog.WRITE_EXTERNAL_STORAGE_FROM_ACTION_DIALOG: {
                try {
                    HSActionsDialog activeDialog = (HSActionsDialog) getSupportFragmentManager().findFragmentByTag(HSActionsDialog.class.getSimpleName());
                    activeDialog.doBackup();
                } catch (ClassCastException e) {
                }
                break;
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
                    HSContentProvider.CONTENT_URI, HSContentProvider.PROJECTION, mWhere, null, null
            ));

            if (PermissionManager.isLollipopOrHigher()) {
                Cursor active = mResolver.query(
                        HSContentProvider.CONTENT_URI, HSContentProvider.PROJECTION, HSContentProvider.HiddenService.ENABLED + "=1", null, null
                );

                if (active == null) return;

                if (active.getCount() > 0) // Call only if there running services
                    PermissionManager.requestBatteryPermmssions(HiddenServicesActivity.this, getApplicationContext());
                else // Drop whe not needed
                    PermissionManager.requestDropBatteryPermmssions(HiddenServicesActivity.this, getApplicationContext());

                active.close();
            }
        }
    }
}
