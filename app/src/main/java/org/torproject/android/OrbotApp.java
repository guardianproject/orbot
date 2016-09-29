
package org.torproject.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.VpnService;
import android.os.Build;
import android.util.Log;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.util.Prefs;

import org.torproject.android.settings.Languages;

import java.util.Locale;

public class OrbotApp extends Application implements OrbotConstants
{

    private Locale locale;

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.setContext(this);

        Languages.setup(OrbotMainActivity.class, R.string.menu_settings);
        Languages.setLanguage(this, Prefs.getDefaultLocale(), true);


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
}
