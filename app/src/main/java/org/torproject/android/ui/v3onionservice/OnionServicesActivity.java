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
import android.widget.ListView;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;

public class OnionServicesActivity extends AppCompatActivity {

    static final String BUNDLE_KEY_ID = "id", BUNDLE_KEY_PORT = "port", BUNDLE_KEY_DOMAIN = "domain";
    private static final String WHERE_SELECTION_CLAUSE = OnionServiceContentProvider.OnionService.CREATED_BY_USER + "=1";
    private static final int REQUEST_CODE_READ_ZIP_BACKUP = 347;
    private RadioButton radioShowUserServices, radioShowAppServices;
    private FloatingActionButton fab;
    private ContentResolver mContentResolver;
    private OnionV3ListAdapter mAdapter;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.layout_hs_list_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> new NewOnionServiceDialogFragment().show(getSupportFragmentManager(), "NewOnionServiceDialogFragment"));

        mContentResolver = getContentResolver();
        mAdapter = new OnionV3ListAdapter(this, mContentResolver.query(OnionServiceContentProvider.CONTENT_URI, OnionServiceContentProvider.PROJECTION, WHERE_SELECTION_CLAUSE, null, null), 0);
        mContentResolver.registerContentObserver(OnionServiceContentProvider.CONTENT_URI, true, new OnionServiceObserver(new Handler()));

        ListView onionList = findViewById(R.id.onion_list);
        onionList.setAdapter(mAdapter);
        onionList.setOnItemClickListener((parent, view, position, id) -> {
            Cursor item = (Cursor) parent.getItemAtPosition(position);
            Bundle arguments = new Bundle();
            arguments.putInt(BUNDLE_KEY_ID, item.getInt(item.getColumnIndex(OnionServiceContentProvider.OnionService._ID)));
            arguments.putString(BUNDLE_KEY_PORT, item.getString(item.getColumnIndex(OnionServiceContentProvider.OnionService.PORT)));
            arguments.putString(BUNDLE_KEY_DOMAIN, item.getString(item.getColumnIndex(OnionServiceContentProvider.OnionService.DOMAIN)));
            OnionServiceActionsDialogFragment dialog = new OnionServiceActionsDialogFragment(arguments);
            dialog.show(getSupportFragmentManager(), OnionServiceActionsDialogFragment.class.getSimpleName());
        });
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_restore_backup) {
            if (DiskUtils.supportsStorageAccessFramework()) {
                Intent readFileIntent = DiskUtils.createReadFileIntent("application/zip");
                startActivityForResult(readFileIntent, REQUEST_CODE_READ_ZIP_BACKUP);
            } else { // 16, 17, 18

            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int result, Intent data) {
        super.onActivityResult(requestCode, result, data);
        if (requestCode == REQUEST_CODE_READ_ZIP_BACKUP && result == RESULT_OK) {
            new BackupUtils(this).restoreZipBackupV3(data.getData());
        }
    }

    private class OnionServiceObserver extends ContentObserver {

        OnionServiceObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mAdapter.changeCursor(mContentResolver.query(OnionServiceContentProvider.CONTENT_URI, OnionServiceContentProvider.PROJECTION, WHERE_SELECTION_CLAUSE, null, null));
            // todo battery optimization stuff if lollipop or higher and running onion services
        }
    }


}
