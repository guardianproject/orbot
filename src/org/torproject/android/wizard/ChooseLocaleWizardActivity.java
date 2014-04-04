package org.torproject.android.wizard;

import java.util.Locale;

import org.torproject.android.R;
import org.torproject.android.TorConstants;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ChooseLocaleWizardActivity extends Activity implements TorConstants {

	private Context context;
	private int flag = 0;
	private ListView listLocales;
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        context = this;
       
	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		setContentView(R.layout.layout_wizard_locale);
		
		stepSix();
        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	
		 setupUI();
	
		
		
	}

	
	private void setupUI ()
	{
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		
	}
	
	
	private void stepSix(){
		
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
					startActivityForResult(new Intent(getBaseContext(), LotsaText.class), 1);
					
				}
			});
		        
		    next.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					
					
					startActivityForResult(new Intent(getBaseContext(), LotsaText.class), 1);

				}
			});
		    

	       
	}
	
	private void setLocalePref(int selId)
	{

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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
}