package org.torproject.android.ui.wizard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import info.guardianproject.util.Languages;

import org.torproject.android.OrbotApp;
import org.torproject.android.OrbotConstants;
import org.torproject.android.R;
import org.torproject.android.service.TorServiceUtils;

import java.util.Locale;

public class ChooseLocaleWizardActivity extends Activity implements OrbotConstants {

    private ListView listLocales;
    private String[] localeValues;
    
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_wizard_locale);

        listLocales = (ListView)findViewById(R.id.wizard_locale_list);
        Button next = ((Button)findViewById(R.id.btnWizard2));
       // next.setEnabled(false);
        
        Languages languages = OrbotApp.getLanguages(this);
        localeValues = languages.getSupportedLocales();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                languages.getAllNames());
        listLocales.setAdapter(adapter);

        listLocales.setSelection(0);
        listLocales.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                
                setLocalePref(arg2);
                finish();
                startActivity(new Intent(ChooseLocaleWizardActivity.this, PromoAppsActivity.class));
            }
        });
            
        next.setOnClickListener(new View.OnClickListener() {
            
            public void onClick(View v) {
                setLocalePref(0);
                finish();
                startActivity(new Intent(ChooseLocaleWizardActivity.this, PromoAppsActivity.class));

            }
        });
    }
    
    private void setLocalePref(int selId)
    {
        SharedPreferences prefs =  TorServiceUtils.getSharedPrefs(getApplicationContext());

        Configuration config = getResources().getConfiguration();
        
        String lang = localeValues[selId];

        Editor pEdit = prefs.edit();
        pEdit.putString(PREF_DEFAULT_LOCALE, lang);
        pEdit.commit();
        Locale locale = null;
        
        if (lang.equals("xx"))
        {
            locale = Locale.getDefault();
        
        }
        else
            locale = new Locale(lang);
        
        Locale.setDefault(locale);
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    
    //Code to override the back button!
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            Toast.makeText(getApplicationContext(), R.string.wizard_exit_at_first_screen_toast, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}