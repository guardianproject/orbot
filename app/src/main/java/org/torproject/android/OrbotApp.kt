package org.torproject.android

import android.app.Application
import android.content.res.Configuration
import org.torproject.android.core.Languages
import org.torproject.android.core.LocaleHelper
import org.torproject.android.service.OrbotConstants
import org.torproject.android.service.util.Prefs
import java.util.Locale

class OrbotApp : Application(), OrbotConstants {

    override fun onCreate() {
        super.onCreate()
        Prefs.setContext(applicationContext)
        LocaleHelper.onAttach(applicationContext)

        Languages.setup(OrbotActivity::class.java, R.string.menu_settings)

        if (Prefs.getDefaultLocale() != Locale.getDefault().language ) {
            Languages.setLanguage(this, Prefs.getDefaultLocale(), true)
        }

        // If it exists, remove v2 onion service data
        deleteDatabase("hidden_services")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (Prefs.getDefaultLocale() != Locale.getDefault().language ) {
            Languages.setLanguage(this, Prefs.getDefaultLocale(), true)
        }
    }

    fun setLocale() {
        val appLocale = Prefs.getDefaultLocale()
        val systemLoc = Locale.getDefault().language

        if (appLocale != systemLoc) {
            Languages.setLanguage(this, Prefs.getDefaultLocale(), true)
        }
    }
}
