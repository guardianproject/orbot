package org.torproject.android.storage;


import android.app.Application;
import android.content.Context;

import com.commonsware.cwac.provider.LocalPathStrategy;
import com.commonsware.cwac.provider.StreamProvider;
import com.commonsware.cwac.provider.StreamStrategy;

import org.torproject.android.service.TorServiceConstants;

import java.io.IOException;
import java.util.HashMap;

public class AppDataProvider extends StreamProvider {
    private static final String TAG = "app-data-path";

    @Override
    protected StreamStrategy buildStrategy(Context context,
                                           String tag, String name,
                                           String path,
                                           HashMap<String, String> attrs)
            throws IOException {

        if (TAG.equals(tag)) {
            return (new LocalPathStrategy(
                    name,
                    context.getDir(
                            TorServiceConstants.DIRECTORY_TOR_DATA,
                            Application.MODE_PRIVATE
                    )
            )
            );
        }

        return (super.buildStrategy(context, tag, name, path, attrs));
    }
}
