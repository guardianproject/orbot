package org.torproject.android.ui.hiddenservices;


import android.content.ContentResolver;
import android.database.ContentObserver;
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
import org.torproject.android.ui.hiddenservices.storage.PermissionManager;
import org.torproject.android.ui.hiddenservices.adapters.OnionListAdapter;
import org.torproject.android.ui.hiddenservices.dialogs.HSActionsDialog;
import org.torproject.android.ui.hiddenservices.dialogs.HSDataDialog;
import org.torproject.android.ui.hiddenservices.dialogs.SelectBackupDialog;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

public class HiddenServicesActivity extends AppCompatActivity {
    private ContentResolver mCR;
    private OnionListAdapter mAdapter;
    private Toolbar toolbar;

    private String[] mProjection = new String[]{
            HSContentProvider.HiddenService._ID,
            HSContentProvider.HiddenService.NAME,
            HSContentProvider.HiddenService.PORT,
            HSContentProvider.HiddenService.DOMAIN,
            HSContentProvider.HiddenService.CREATED_BY_USER
    };

    private String mWhere = HSContentProvider.HiddenService.CREATED_BY_USER + "=1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_hs_list_view);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCR = getContentResolver();

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
                mCR.query(HSContentProvider.CONTENT_URI, mProjection, mWhere, null, null),
                0
        );

        mCR.registerContentObserver(
                HSContentProvider.CONTENT_URI, true, new HSObserver(new Handler())
        );

        ListView onion_list = (ListView) findViewById(R.id.onion_list);
        onion_list.setAdapter(mAdapter);

        onion_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView port = (TextView) view.findViewById(R.id.hs_port);
                TextView onion = (TextView) view.findViewById(R.id.hs_onion);

                Bundle arguments = new Bundle();
                arguments.putString("port", port.getText().toString());
                arguments.putString("onion", onion.getText().toString());

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
                PermissionManager.requestPermissions(this);
                return true;
            }

            SelectBackupDialog dialog = new SelectBackupDialog();
            dialog.show(getSupportFragmentManager(), "SelectBackupDialog");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class HSObserver extends ContentObserver {
        HSObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mAdapter.changeCursor(mCR.query(
                    HSContentProvider.CONTENT_URI, mProjection, mWhere, null, null
            ));
        }
    }
}
