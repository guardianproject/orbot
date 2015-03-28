package org.torproject.android.ui.wizard;

import java.util.Locale;

import org.torproject.android.R;
import org.torproject.android.OrbotConstants;
import org.torproject.android.service.TorServiceUtils;

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

public class ChooseLocaleWizardActivity extends Activity implements OrbotConstants {

    private int flag = 0;
    private ListView listLocales;
    
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
       
    }
    
    @Override
    protected void onStart() {
        
        super.onStart();
        setContentView(R.layout.layout_wizard_locale);
        
        
        listLocales = (ListView)findViewById(R.id.wizard_locale_list);
        Button next = ((Button)findViewById(R.id.btnWizard2));
       // next.setEnabled(false);
        
        String[] strLangs = getResources().getStringArray(R.array.languages);
        strLangs[0] = Locale.getDefault().getDisplayName();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1,  strLangs);            
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

        String[] localeVals = getResources().getStringArray(R.array.languages_values);
        
        String lang = localeVals[selId];

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

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
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