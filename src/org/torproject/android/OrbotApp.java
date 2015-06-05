
package org.torproject.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import info.guardianproject.util.Languages;

import org.torproject.android.service.TorServiceUtils;

import java.util.Locale;

public class OrbotApp extends Application implements OrbotConstants
{

    private Locale locale;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
        setNewLocale(prefs.getString(PREF_DEFAULT_LOCALE, Locale.getDefault().getLanguage()));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged " + newConfig.locale.getLanguage());
        setNewLocale(prefs.getString(PREF_DEFAULT_LOCALE, Locale.getDefault().getLanguage()));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setNewLocale(String language) {
        if (TextUtils.isEmpty(language))
            return;

        if (locale != null && TextUtils.equals(locale.getLanguage(), language))
            return; // already configured

        /* handle locales with the country in it, i.e. zh_CN, zh_TW, etc */
        String localeSplit[] = language.split("_");
        if (localeSplit.length > 1)
            locale = new Locale(localeSplit[0], localeSplit[1]);
        else
            locale = new Locale(language);
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= 17)
            config.setLocale(locale);
        else
            config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        /*
         * Set the preference after setting the locale in case something goes
         * wrong. If setting the locale causes an Exception, it should be set in
         * the preferences, otherwise ChatSecure will be stuck in a crash loop.
         */
        SharedPreferences prefs = TorServiceUtils.getSharedPrefs(this);
        Editor prefEdit = prefs.edit();
        prefEdit.putString(PREF_DEFAULT_LOCALE, language);
        prefEdit.apply();
        Log.i(TAG, "setNewLocale complete: locale: " + locale.getLanguage()
                + " Locale.getDefault: " + Locale.getDefault().getLanguage());
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
        return Languages.get(activity, R.string.menu_settings, "Settings");
    }
}
