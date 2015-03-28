package org.torproject.android;

import java.util.Locale;

import org.torproject.android.service.TorServiceUtils;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

public class OrbotApp extends Application implements OrbotConstants
{

	private Locale locale;
	private final static String DEFAULT_LOCALE = "en";
	private SharedPreferences settings;
	
	@Override
    public void onCreate() {
        super.onCreate();
        
        settings =  TorServiceUtils.getSharedPrefs(getApplicationContext());

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
