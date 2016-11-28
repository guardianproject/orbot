package org.torproject.android.ui.hiddenservices;


import android.content.ContentResolver;
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
import android.widget.TextView;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.adapters.OnionListAdapter;
import org.torproject.android.ui.hiddenservices.dialogs.HSActionsDialog;
import org.torproject.android.ui.hiddenservices.dialogs.HSDataDialog;
import org.torproject.android.ui.hiddenservices.dialogs.SelectBackupDialog;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;
import org.torproject.android.ui.hiddenservices.storage.PermissionManager;

public class HiddenServicesActivity extends AppCompatActivity {
    public final int WRITE_EXTERNAL_STORAGE_FROM_ACTIONBAR = 1;
    private ContentResolver mResolver;
    private OnionListAdapter mAdapter;

    private String mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_hs_list_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mResolver = getContentResolver();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HSDataDialog dialog = new HSDataDialog();
                dialog.show(getSupportFragmentManager(), "HSDataDialog");
            }
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

        ListView onion_list = (ListView) findViewById(R.id.onion_list);
        onion_list.setAdapter(mAdapter);

        onion_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor item = (Cursor) parent.getItemAtPosition(position);

                Bundle arguments = new Bundle();
                arguments.putString(
                        "port", item.getString(item.getColumnIndex(HSContentProvider.HiddenService.PORT))
                );

                arguments.putString(
                        "onion",item.getString(item.getColumnIndex(HSContentProvider.HiddenService.DOMAIN))
                );

                HSActionsDialog dialog = new HSActionsDialog();
                dialog.setArguments(arguments);
                dialog.show(getSupportFragmentManager(), "HSActionsDialog");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hs_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_restore_backup) {
            if (PermissionManager.usesRuntimePermissions()
                    && !PermissionManager.hasExternalWritePermission(this)) {
                PermissionManager.requestPermissions(this, WRITE_EXTERNAL_STORAGE_FROM_ACTIONBAR);
                return true;
            }

            SelectBackupDialog dialog = new SelectBackupDialog();
            dialog.show(getSupportFragmentManager(), "SelectBackupDialog");
            return true;
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
                SelectBackupDialog dialog = new SelectBackupDialog();
                dialog.show(getSupportFragmentManager(), "SelectBackupDialog");
                break;
            }
            case HSActionsDialog.WRITE_EXTERNAL_STORAGE_FROM_ACTION_DIALOG: {
                // TODO
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
        }
    }
}
