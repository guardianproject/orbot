package org.torproject.android.ui.hs;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.torproject.android.R;
import org.torproject.android.ui.hs.adapters.HSAdapter;
import org.torproject.android.ui.hs.dialogs.HSDataDialog;
import org.torproject.android.ui.hs.providers.HSContentProvider;

public class HiddenServicesActivity extends AppCompatActivity {
    private HSAdapter mHiddenServices;
    private ContentResolver mCR;
    private HSObserver mHSObserver;
    private String[] mProjection = new String[]{
            HSContentProvider.HiddenService._ID,
            HSContentProvider.HiddenService.NAME,
            HSContentProvider.HiddenService.DOMAIN,
            HSContentProvider.HiddenService.PORT};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_services);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HSDataDialog dialog = new HSDataDialog();
                dialog.show(getSupportFragmentManager(), "HSDataDialog");
            }
        });

        mCR = getContentResolver();
        // View adapter
        mHiddenServices = new HSAdapter(
                mCR.query(
                        HSContentProvider.CONTENT_URI, mProjection, null, null, null
                ));

        mHSObserver = new HSObserver(new Handler());
        mCR.registerContentObserver(HSContentProvider.CONTENT_URI, true, mHSObserver);

        // Fill view
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.onion_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mHiddenServices);
    }

    class HSObserver extends ContentObserver {
        public HSObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            // New data
            mHiddenServices.changeCursor(mCR.query(
                    HSContentProvider.CONTENT_URI, mProjection, null, null, null
            ));
        }

    }

}
