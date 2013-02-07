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
	private SharedPreferences settings;
	
	@Override
    public void onCreate() {
        super.onCreate();
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        Configuration config = getResources().getConfiguration();

        String lang = settings.getString(PREF_DEFAULT_LOCALE, DEFAULT_LOCALE);
        
        if (! "".equals(lang) && ! config.locale.getLanguage().equals(lang))
        {
        	if (lang.equals("xx"))
            {
            	locale = Locale.getDefault();
            
            }
            else
            	locale = new Locale(lang);
        	
            Locale.setDefault(locale);

            Configuration myConfig = new Configuration(config);
        	myConfig.locale = locale;
        	
            getResources().updateConfiguration(myConfig, getResources().getDisplayMetrics());
        }
        
        
    }
	
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        String lang = settings.getString(PREF_DEFAULT_LOCALE, DEFAULT_LOCALE);

        if (! "".equals(lang) && ! newConfig.locale.getLanguage().equals(lang))
        {
            locale = new Locale(lang);
            Locale.setDefault(locale);
            
            Configuration myConfig = new Configuration(newConfig);
        	myConfig.locale = locale;
        	 
            getResources().updateConfiguration(myConfig, getResources().getDisplayMetrics());
        }
    }
}
