package org.torproject.android;

import java.util.Locale;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

public class OrbotApp extends Application implements TorConstants
{

	private Locale locale;
	private final static String DEFAULT_LOCALE = "en";
	
	@Override
    public void onCreate() {
        super.onCreate();
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        Configuration config = getResources().getConfiguration();

        String lang = settings.getString(PREF_DEFAULT_LOCALE, DEFAULT_LOCALE);
        
        if (! "".equals(lang) && ! config.locale.getLanguage().equals(lang))
        {
            locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }
        
    }
	
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        if (locale != null)
        {
            newConfig.locale = locale;
            Locale.setDefault(locale);
            getResources().updateConfiguration(newConfig, getResources().getDisplayMetrics());
        }
    }
}
