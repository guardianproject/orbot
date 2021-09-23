package org.torproject.android.ui.v3onionservice;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import org.torproject.android.R;

public class PermissionManager {
    public static final int VERY_LONG_LENGTH = 6000;

    public static boolean isLollipopOrHigher() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void requestBatteryPermissions(FragmentActivity activity, Context context) {
        final String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (pm.isIgnoringBatteryOptimizations(packageName))
            return;

        Snackbar.make(activity.findViewById(android.R.id.content),
                R.string.consider_disable_battery_optimizations,
                VERY_LONG_LENGTH).setAction(R.string.disable,
                v -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    context.startActivity(intent);
                }).show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void requestDropBatteryPermissions(FragmentActivity activity, Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (!pm.isIgnoringBatteryOptimizations(context.getPackageName()))
            return;

        Snackbar.make(activity.findViewById(android.R.id.content),
                R.string.consider_enable_battery_optimizations,
                VERY_LONG_LENGTH).setAction(R.string.enable,
                v -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    context.startActivity(intent);
                }).show();
    }
}

