package org.torproject.android.ui.hs.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.torproject.android.R;
import org.torproject.android.ui.hs.providers.HSContentProvider;

public class OnionListAdapter extends CursorAdapter {
    private LayoutInflater cursorInflater;

    public OnionListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        cursorInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView name = (TextView) view.findViewById(R.id.hs_name);
        name.setText(cursor.getString(cursor.getColumnIndex(HSContentProvider.HiddenService.NAME)));
        TextView domain = (TextView) view.findViewById(R.id.hs_onion);
        domain.setText(cursor.getString(cursor.getColumnIndex(HSContentProvider.HiddenService.DOMAIN)));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return cursorInflater.inflate(R.layout.layout_hs_list_item, parent, false);
    }
}
