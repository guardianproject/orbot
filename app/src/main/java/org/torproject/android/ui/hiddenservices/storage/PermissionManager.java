package org.torproject.android.ui.hiddenservices.storage;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import org.torproject.android.R;

public class PermissionManager {

    public static boolean usesRuntimePermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @SuppressLint("NewApi")
    public static boolean hasExternalWritePermission(Context context) {
        return (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public static void requestPermissions(FragmentActivity activity, int action) {
        final int mAction = action;
        final FragmentActivity mActivity = activity;

        if (ActivityCompat.shouldShowRequestPermissionRationale
                (mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(mActivity.findViewById(android.R.id.content),
                    R.string.please_grant_permissions_for_external_storage,
                    Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(mActivity,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    mAction);
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    mAction);
        }
    }
}
