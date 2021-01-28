package org.torproject.android.ui.v3onionservice;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.torproject.android.R;

public class V3ClientAuthActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v3auth);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.fab).setOnClickListener(v -> {
            new AddV3ClientAuthDialogFragment().show(getSupportFragmentManager(), "AddV3ClientAuthDialogFragment");
        });


    }
}
