package org.torproject.android.v3onionservice.clientauth;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import org.torproject.android.R;
import org.torproject.android.core.DiskUtils;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.v3onionservice.V3BackupUtils;

import java.util.List;
import java.util.Objects;

public class ClientAuthActivity extends AppCompatActivity {

    public static final String BUNDLE_KEY_ID = "_id",
            BUNDLE_KEY_DOMAIN = "domain",
            BUNDLE_KEY_HASH = "key_hash_value";

    private ContentResolver mResolver;
    private ClientAuthListAdapter mAdapter;

    static final String CLIENT_AUTH_FILE_EXTENSION = ".auth_private",
            CLIENT_AUTH_SAF_MIME_TYPE = "*/*";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v3auth);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        mResolver = getContentResolver();
        mAdapter = new ClientAuthListAdapter(this, mResolver.query(ClientAuthContentProvider.CONTENT_URI, ClientAuthContentProvider.PROJECTION, null, null, null));
        mResolver.registerContentObserver(ClientAuthContentProvider.CONTENT_URI, true, new V3ClientAuthContentObserver(new Handler()));

        findViewById(R.id.fab).setOnClickListener(v ->
                new ClientAuthCreateDialogFragment().show(getSupportFragmentManager(), ClientAuthCreateDialogFragment.class.getSimpleName()));

        ListView auths = findViewById(R.id.auth_hash_list);
        auths.setAdapter(mAdapter);
        auths.setOnItemClickListener((parent, view, position, id) -> {
            Cursor item = (Cursor) parent.getItemAtPosition(position);
            Bundle args = new Bundle();
            args.putInt(BUNDLE_KEY_ID, item.getInt(item.getColumnIndex(ClientAuthContentProvider.V3ClientAuth._ID)));
            args.putString(BUNDLE_KEY_DOMAIN, item.getString(item.getColumnIndex(ClientAuthContentProvider.V3ClientAuth.DOMAIN)));
            args.putString(BUNDLE_KEY_HASH, item.getString(item.getColumnIndex(ClientAuthContentProvider.V3ClientAuth.HASH)));
            new ClientAuthActionsDialogFragment(args).show(getSupportFragmentManager(), ClientAuthActionsDialogFragment.class.getSimpleName());
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_READ_ZIP_BACKUP && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                assert cursor != null;
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                String filename = cursor.getString(nameIndex);
                cursor.close();
                if (!filename.endsWith(CLIENT_AUTH_FILE_EXTENSION)) {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
                    return;
                }
                String authText = DiskUtils.readFileFromInputStream(getContentResolver(), uri);
                new V3BackupUtils(this).restoreClientAuthBackup(authText);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            List<Fragment> frags = getSupportFragmentManager().getFragments();
            for (Fragment f : frags) f.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    private class V3ClientAuthContentObserver extends ContentObserver {
        V3ClientAuthContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mAdapter.changeCursor(mResolver.query(ClientAuthContentProvider.CONTENT_URI, ClientAuthContentProvider.PROJECTION, null, null, null));
        }

    }

    private static final int REQUEST_CODE_READ_ZIP_BACKUP = 12;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_import_auth_priv) {
            // unfortunately no good way to filter .auth_private files
            Intent readFileIntent = DiskUtils.createReadFileIntent(CLIENT_AUTH_SAF_MIME_TYPE);
            startActivityForResult(readFileIntent, REQUEST_CODE_READ_ZIP_BACKUP);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.v3_client_auth_menu, menu);
        return true;
    }
}
