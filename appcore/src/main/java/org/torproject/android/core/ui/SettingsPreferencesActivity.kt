/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */ /* See LICENSE for licensing information */
package org.torproject.android.core.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.preference.Preference.OnPreferenceChangeListener
import android.view.inputmethod.EditorInfo
import androidx.annotation.XmlRes
import org.torproject.android.core.Languages
import org.torproject.android.core.LocaleHelper

class SettingsPreferencesActivity : PreferenceActivity() {
    private var prefLocale: ListPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(intent.getIntExtra(BUNDLE_KEY_PREFERENCES_XML, 0))
        setNoPersonalizedLearningOnEditTextPreferences()
        preferenceManager.sharedPreferencesMode = MODE_MULTI_PROCESS
        prefLocale = findPreference("pref_default_locale") as ListPreference
        val languages = Languages[this]
        prefLocale?.entries = languages!!.allNames
        prefLocale?.entryValues = languages.supportedLocales
        prefLocale?.onPreferenceChangeListener = OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
            val language = newValue as String?
            val intentResult = Intent()
            intentResult.putExtra("locale", language)
            setResult(RESULT_OK, intentResult)
            finish()
            false
        }

        val bridgesEnabled = getSharedPreferences("org.torproject.android_preferences", MODE_MULTI_PROCESS).getBoolean("pref_bridges_enabled", false)
        findPreference("pref_be_a_snowflake")?.isEnabled = !bridgesEnabled
        findPreference("pref_be_a_snowflake_limit")?.isEnabled = !bridgesEnabled
        findPreference("pref_show_snowflake_proxy_msg")?.isEnabled = !bridgesEnabled

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // if defined in XML, disable the persistent notification preference on Oreo+
            findPreference("pref_persistent_notifications")?.let {
                it.parent?.removePreference(it)
            }
        }

    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleHelper.onAttach(newBase))

    private fun setNoPersonalizedLearningOnEditTextPreferences() {
        val preferenceScreen = preferenceScreen
        val categoryCount = preferenceScreen.preferenceCount
        for (i in 0 until categoryCount) {
            var p = preferenceScreen.getPreference(i)
            if (p is PreferenceCategory) {
                val pc = p
                val preferenceCount = pc.preferenceCount
                for (j in 0 until preferenceCount) {
                    p = pc.getPreference(j)
                    if (p is EditTextPreference) {
                        val editText = p.editText
                        editText.imeOptions = editText.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                    }
                }
            }
        }
    }

    companion object {
        private const val BUNDLE_KEY_PREFERENCES_XML = "prefxml"

        @JvmStatic
        fun createIntent(context: Context?, @XmlRes xmlPrefId: Int): Intent =
                Intent(context, SettingsPreferencesActivity::class.java).apply {
                    putExtra(BUNDLE_KEY_PREFERENCES_XML, xmlPrefId)
                }
    }
}
