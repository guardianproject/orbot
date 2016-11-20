package org.torproject.android.ui.hs;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.torproject.android.R;
import org.torproject.android.ui.hs.adapters.OnionListAdapter;
import org.torproject.android.ui.hs.dialogs.HSActionsDialog;
import org.torproject.android.ui.hs.dialogs.HSDataDialog;
import org.torproject.android.ui.hs.providers.HSContentProvider;

public class HiddenServicesActivity extends AppCompatActivity {
    private ContentResolver mCR;
    private OnionListAdapter mAdapter;

    public final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private String[] mProjection = new String[]{
            HSContentProvider.HiddenService._ID,
            HSContentProvider.HiddenService.NAME,
            HSContentProvider.HiddenService.PORT,
            HSContentProvider.HiddenService.DOMAIN};

    class HSObserver extends ContentObserver {
        HSObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mAdapter.changeCursor(mCR.query(
                    HSContentProvider.CONTENT_URI, mProjection, null, null, null
            ));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_hs_list_view);

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
                mCR.query(
                        HSContentProvider.CONTENT_URI, mProjection, null, null, null
                ),
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
                if (usesRuntimePermissions())
                    checkPermissions();

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

    private boolean usesRuntimePermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @SuppressLint("NewApi")
    private boolean hasPermission(String permission) {
        return !usesRuntimePermissions() || (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    private void checkPermissions() {
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (HiddenServicesActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Snackbar.make(findViewById(android.R.id.content),
                        R.string.please_grant_permissions_for_external_storage,
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(HiddenServicesActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                            }
                        }).show();
            } else {
                ActivityCompat.requestPermissions(HiddenServicesActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.permission_granted,
                            Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.permission_denied,
                            Snackbar.LENGTH_LONG).show();
                }
                break;
            }
        }
    }
}
