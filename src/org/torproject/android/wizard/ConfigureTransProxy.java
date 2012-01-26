package org.torproject.android.wizard;

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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class ConfigureTransProxy extends Activity implements TorConstants {

	private Context context;
	private int flag = 0;
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        context = this;
       
	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		setContentView(R.layout.layout_wizard_root);
		
		stepSix();
        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	
		 setupUI();
	
		
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch(resultCode)
	    {
	    case RESULT_CLOSE_ALL:
	        setResult(RESULT_CLOSE_ALL);
	        finish();
	    }
	    super.onActivityResult(requestCode, resultCode, data);
	}

	
	private void setupUI ()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		boolean transEnabled = prefs.getBoolean(PREF_TRANSPARENT, false);
		
		if (transEnabled)
		{
			boolean transAllEnabled = prefs.getBoolean(PREF_TRANSPARENT_ALL, false);
			
			if (transAllEnabled)
			{
				RadioButton rb0 = (RadioButton)findViewById(R.id.radio0);
				rb0.setChecked(true);
				
				
			}
			else
			{
				RadioButton rb1 = (RadioButton)findViewById(R.id.radio1);
        		rb1.setChecked(true);
				
			}
			
		    Button next = ((Button)findViewById(R.id.btnWizard2));
		    next.setEnabled(true);
		}
		
	}
	
	
	private void stepSix(){
		
			String title = context.getString(R.string.wizard_transproxy_title);
			TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
			txtTitle.setText(title);
			
			Button back = ((Button)findViewById(R.id.btnWizard1));
		    Button next = ((Button)findViewById(R.id.btnWizard2));
		    next.setEnabled(false);
		        
		    back.setOnClickListener(new View.OnClickListener() {
					
				public void onClick(View v) {
						
					startActivityForResult(new Intent(getBaseContext(), Permissions.class), 1);
				}
			});
		    	
		    next.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					
						showWizardFinal();
				}
			});
		    

    		RadioButton rb0 = (RadioButton)findViewById(R.id.radio0);
    		RadioButton rb1 = (RadioButton)findViewById(R.id.radio1);
    		RadioButton rb2 = (RadioButton)findViewById(R.id.radio2);

    		rb1.setOnClickListener(new OnClickListener()
    		{

				public void onClick(View v) {
					
						context.startActivity(new Intent(context, AppManager.class));							
					
					
					
				}
    			
    		});
		
			RadioGroup mRadioGroup = (RadioGroup)findViewById(R.id.radioGroup);
	        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener (){
	        
	        
	        	public void onCheckedChanged(RadioGroup group, int checkedId){
	        		Button next = ((Button)findViewById(R.id.btnWizard2));
	        		next.setEnabled(true);
	        		
	        		RadioButton rb0 = (RadioButton)findViewById(R.id.radio0);
	        		RadioButton rb1 = (RadioButton)findViewById(R.id.radio1);
	        		RadioButton rb2 = (RadioButton)findViewById(R.id.radio2);

	        		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

					Editor pEdit = prefs.edit();
					pEdit.putBoolean(PREF_TRANSPARENT, rb0.isChecked());
					pEdit.putBoolean(PREF_TRANSPARENT_ALL, rb0.isChecked());
					pEdit.commit();

					if(rb0.isChecked())
					{ 	
						pEdit.putString("radiobutton","rb0");
						pEdit.commit();
					}
	        		
					else if(rb1.isChecked())
	        		{	
	        			flag++;
	        			
	        			pEdit.putBoolean(PREF_TRANSPARENT, true);
						pEdit.putBoolean(PREF_TRANSPARENT_ALL, false);
						pEdit.putString("radiobutton","rb1");
						pEdit.commit();
					
	        		}
					else if(rb2.isChecked())
					{
						pEdit.putString("radiobutton", "rb2");
						pEdit.commit();
					}
	        		
	        }
	        });

	       
	}
	
	private void showWizardFinal ()
	{
		String title = null;
		String msg = null;
		
		
		title = context.getString(R.string.wizard_final);
		msg = context.getString(R.string.wizard_final_msg);
		
		DialogInterface.OnClickListener ocListener = new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				startActivityForResult(new Intent(getBaseContext(), Orbot.class), 1);

			}
		};
	
		
		 new AlertDialog.Builder(context)
		.setIcon(R.drawable.icon)
        .setTitle(title)
        .setPositiveButton(R.string.button_close, ocListener)
        .setMessage(msg)
        .show();
	
	
				
		
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