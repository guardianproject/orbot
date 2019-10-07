/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.mini.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import org.torproject.android.mini.R;

public class SettingsPreferences extends PreferenceActivity {
    private ListPreference prefLocale = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        setNoPersonalizedLearningOnEditTextPreferences();
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);

        prefLocale = (ListPreference) findPreference("pref_default_locale");

        Languages languages = Languages.get(this);
        prefLocale.setEntries(languages.getAllNames());
        prefLocale.setEntryValues(languages.getSupportedLocales());
        prefLocale.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String language = (String) newValue;
                Intent intentResult = new Intent();
                intentResult.putExtra("locale", language);
                setResult(RESULT_OK, intentResult);
                finish();
                return false;
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    private void setNoPersonalizedLearningOnEditTextPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int categoryCount = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < categoryCount; i++) {
            Preference p = preferenceScreen.getPreference(i);
            if (p instanceof PreferenceCategory) {
                PreferenceCategory pc = (PreferenceCategory) p;
                int preferenceCount = pc.getPreferenceCount();
                for (int j = 0; j < preferenceCount; j++) {
                    p = pc.getPreference(j);
                    if (p instanceof EditTextPreference) {
                        EditText editText = ((EditTextPreference) p).getEditText();
                        editText.setImeOptions(editText.getImeOptions() | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
                    }
                }
            }
        }
    }

}
