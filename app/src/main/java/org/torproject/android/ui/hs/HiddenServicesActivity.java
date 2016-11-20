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

    private String[] mProjection = new String[]{
            HSContentProvider.HiddenService._ID,
            HSContentProvider.HiddenService.NAME,
            HSContentProvider.HiddenService.PORT,
            HSContentProvider.HiddenService.DOMAIN};

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
                TextView port = (TextView) view.findViewById(R.id.hs_port);
                TextView onion = (TextView) view.findViewById(R.id.hs_onion);

                Bundle arguments = new Bundle();
                arguments.putString("port", port.getText().toString());
                arguments.putString("onion", onion.getText().toString());
                boolean has_write_permission = true;
                if (usesRuntimePermissions())
                    has_write_permission = hasPermission();
                arguments.putBoolean("has_write_permissions", has_write_permission);

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
    private boolean hasPermission() {
        return (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

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
}
