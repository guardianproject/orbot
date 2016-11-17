package org.torproject.android.ui.hs.adapters;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.torproject.android.R;
import org.torproject.android.ui.hs.providers.HSContentProvider;

public class HSAdapter extends CursorRecyclerViewAdapter<HSAdapter.ViewHolder> {

    public HSAdapter(Cursor cursor) {
        super(cursor);
    }

    @Override
    public HSAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.onion_item, parent, false);

        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        viewHolder.id = cursor.getInt(cursor.getColumnIndex(HSContentProvider.HiddenService._ID));

        String name_string = cursor.getString(cursor.getColumnIndex(HSContentProvider.HiddenService.NAME));
        Integer port = cursor.getInt(cursor.getColumnIndex(HSContentProvider.HiddenService.PORT));
        Integer onion_port = cursor.getInt(cursor.getColumnIndex(HSContentProvider.HiddenService.ONION_PORT));

        viewHolder.name.setText(name_string + ": " + port.toString()+ " -> " +onion_port.toString());

        viewHolder.domain.setText(
                cursor.getString(cursor.getColumnIndex(HSContentProvider.HiddenService.DOMAIN))
        );
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView domain;
        Integer id;

        ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.hs_name);
            domain = (TextView) itemView.findViewById(R.id.hs_onion);
        }
    }
}
