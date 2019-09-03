package org.torproject.android.mini.ui.hiddenservices.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.torproject.android.R;

import java.io.File;
import java.util.List;

public class BackupAdapter extends ArrayAdapter<File> {
    private int mResource;

    public BackupAdapter(Context context, int resource) {
        super(context, resource);
        mResource = resource;
    }

    public BackupAdapter(Context context, int resource, List<File> zips) {
        super(context, resource, zips);
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(mResource, null);
        }

        File p = getItem(position);

        if (p != null) {
            TextView name = (TextView) v.findViewById(R.id.backup_name);

            if (name != null)
                name.setText(p.getName());
        }

        return v;
    }
}
