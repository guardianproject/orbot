
package org.torproject.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.torproject.android.service.TorServiceConstants;

import info.guardianproject.util.Languages;

import java.io.File;
import java.util.Locale;

public class OrbotApp extends Application implements OrbotConstants
{

    private Locale locale;

    public static File appBinHome;
    public static File appCacheHome;

    public static File fileTor;
    public static File filePolipo;
    public static File fileObfsclient;
    public static File fileMeekclient;
    public static File fileXtables;
    public static File fileTorRc;
    public static File filePdnsd;
    

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.setContext(this);

        Languages.setup(OrbotMainActivity.class, R.string.menu_settings);
        Languages.setLanguage(this, Prefs.getDefaultLocale(), true);

        appBinHome = getDir(TorServiceConstants.DIRECTORY_TOR_BINARY,Application.MODE_PRIVATE);
        appCacheHome = getDir(TorServiceConstants.DIRECTORY_TOR_DATA,Application.MODE_PRIVATE);

        fileTor= new File(appBinHome, TorServiceConstants.TOR_ASSET_KEY);
        filePolipo = new File(appBinHome, TorServiceConstants.POLIPO_ASSET_KEY);
        fileObfsclient = new File(appBinHome, TorServiceConstants.OBFSCLIENT_ASSET_KEY);
        fileMeekclient = new File(appBinHome, TorServiceConstants.MEEK_ASSET_KEY);
        fileXtables = new File(appBinHome, TorServiceConstants.IPTABLES_ASSET_KEY);
        fileTorRc = new File(appBinHome, TorServiceConstants.TORRC_ASSET_KEY);
        filePdnsd = new File(appBinHome, TorServiceConstants.PDNSD_ASSET_KEY);
        
        
        
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
