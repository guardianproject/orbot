package org.torproject.android.wizard;

import java.util.Locale;

import org.torproject.android.Orbot;
import org.torproject.android.R;
import org.torproject.android.TorConstants;
import org.torproject.android.R.drawable;
import org.torproject.android.R.id;
import org.torproject.android.R.layout;
import org.torproject.android.R.string;
import org.torproject.android.settings.AppManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

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