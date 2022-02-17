package org.torproject.android.ui.v3onionservice;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import org.torproject.android.R;

public class OnionV3ListAdapter extends CursorAdapter {

    private final LayoutInflater mLayoutInflater;

    OnionV3ListAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mLayoutInflater.inflate(R.layout.layout_hs_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(OnionServiceContentProvider.OnionService._ID));
        final String where = OnionServiceContentProvider.OnionService._ID + "=" + id;
        TextView localPort = view.findViewById(R.id.hs_port);
        localPort.setText(String.format("%s\n%s", context.getString(R.string.local_port),
                cursor.getString(cursor.getColumnIndex(OnionServiceContentProvider.OnionService.PORT))));

        TextView onionPort = view.findViewById(R.id.onion_port);
        onionPort.setText(String.format("%s\n%s", context.getString(R.string.onion_port),
                cursor.getString(cursor.getColumnIndex(OnionServiceContentProvider.OnionService.ONION_PORT))));

        TextView name = view.findViewById(R.id.hs_name);
        name.setText(cursor.getString(cursor.getColumnIndex(OnionServiceContentProvider.OnionService.NAME)));
        TextView domain = view.findViewById(R.id.hs_onion);
        domain.setText(cursor.getString(cursor.getColumnIndex(OnionServiceContentProvider.OnionService.DOMAIN)));

        SwitchCompat enabled = view.findViewById(R.id.hs_switch);
        enabled.setChecked(cursor.getInt(cursor.getColumnIndex(OnionServiceContentProvider.OnionService.ENABLED)) == 1);
        enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ContentResolver resolver = context.getContentResolver();
            ContentValues fields = new ContentValues();
            fields.put(OnionServiceContentProvider.OnionService.ENABLED, isChecked);
            resolver.update(OnionServiceContentProvider.CONTENT_URI, fields, where, null);
            Toast.makeText(context, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_SHORT).show();
        });
    }
}
