
package org.torproject.android;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.VpnService;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.TorEventHandler;
import org.torproject.android.service.TorService;
import org.torproject.android.service.util.Prefs;

import org.torproject.android.settings.Languages;

import java.util.Locale;
import java.util.Set;

public class OrbotApp extends Application implements OrbotConstants
{

    private Locale locale;

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.setContext(this);

        Languages.setup(OrbotMainActivity.class, R.string.menu_settings);
        Languages.setLanguage(this, Prefs.getDefaultLocale(), true);

        new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON("https://raw.githubusercontent.com/n8fr8/orbot/master/update.json")
                .setDisplay(Display.NOTIFICATION).start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged " + newConfig.locale.getLanguage());
        Languages.setLanguage(this, Prefs.getDefaultLocale(), true);
    }
	
    public static void forceChangeLanguage(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) // when launched as LAUNCHER
            intent = new Intent(activity, OrbotMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static Languages getLanguages(Activity activity) {
        return Languages.get(activity);
    }


    @SuppressLint("NewApi")
    protected void showToolbarNotification (String shortMsg, String notifyMsg, int notifyId, int icon)
    {

        NotificationCompat.Builder notifyBuilder;

        //Reusable code.
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(org.torproject.android.service.R.string.app_name));


        notifyBuilder.setContentIntent(pendIntent);

        notifyBuilder.setContentText(shortMsg);
        notifyBuilder.setSmallIcon(icon);
        notifyBuilder.setTicker(notifyMsg);

        notifyBuilder.setOngoing(false);

        notifyBuilder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(notifyMsg).setBigContentTitle(getString(org.torproject.android.service.R.string.app_name)));

        Notification notification = notifyBuilder.build();

        notificationManager.notify(notifyId, notification);
            }

}
