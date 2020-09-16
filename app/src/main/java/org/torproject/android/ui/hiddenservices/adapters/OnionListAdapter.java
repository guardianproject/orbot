package org.torproject.android.ui.hiddenservices.adapters;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.appcompat.widget.SwitchCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

public class OnionListAdapter extends CursorAdapter {
    private LayoutInflater cursorInflater;

    public OnionListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        cursorInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final Context mContext = context;
        int id = cursor.getInt(cursor.getColumnIndex(HSContentProvider.HiddenService._ID));
        final String where = HSContentProvider.HiddenService._ID + "=" + id;

        TextView port = view.findViewById(R.id.hs_port);
        port.setText(cursor.getString(cursor.getColumnIndex(HSContentProvider.HiddenService.PORT)));
        TextView name = view.findViewById(R.id.hs_name);
        name.setText(cursor.getString(cursor.getColumnIndex(HSContentProvider.HiddenService.NAME)));
        TextView domain = view.findViewById(R.id.hs_onion);
        domain.setText(cursor.getString(cursor.getColumnIndex(HSContentProvider.HiddenService.DOMAIN)));

        SwitchCompat enabled = view.findViewById(R.id.hs_switch);
        enabled.setChecked(
                cursor.getInt(cursor.getColumnIndex(HSContentProvider.HiddenService.ENABLED)) == 1
        );

        enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ContentResolver resolver = mContext.getContentResolver();
            ContentValues fields = new ContentValues();
            fields.put(HSContentProvider.HiddenService.ENABLED, isChecked);
            resolver.update(
                    HSContentProvider.CONTENT_URI, fields, where, null
            );

            Toast.makeText(
                    mContext, R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG
            ).show();
        });
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return cursorInflater.inflate(R.layout.layout_hs_list_item, parent, false);
    }
}
