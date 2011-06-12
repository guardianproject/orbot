package org.torproject.android;

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
	
		
	}
	
	
	
	private void stepSix(){
		
			String title = context.getString(R.string.wizard_transproxy_title);
			TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
			txtTitle.setText(title);
		
			RadioGroup mRadioGroup = (RadioGroup)findViewById(R.id.radioGroup);
	        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener (){
	        
	        
	        	@Override
	        	public void onCheckedChanged(RadioGroup group, int checkedId){
	        		Button next = ((Button)findViewById(R.id.btnWizard2));
	        		next.setOnClickListener(new View.OnClickListener() {
	    				
	    				@Override
	    				public void onClick(View v) {
	    					
	    						showWizardFinal();
	    				}
	    			});
	        		
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
	        			flag = 1;
	        			
	        			pEdit.putBoolean(PREF_TRANSPARENT, true);
						pEdit.putBoolean(PREF_TRANSPARENT_ALL, false);
						pEdit.putString("radiobutton","rb1");
						pEdit.commit();
						
						next.setOnClickListener(new View.OnClickListener() {
		    				
		    				@Override
		    				public void onClick(View v) {
		    					
		    					context.startActivity(new Intent(context, AppManager.class));
		    						
		    					
		    				}
		    			});
	        		}
					else if(rb2.isChecked())
					{
						pEdit.putString("radiobutton", "rb2");
						pEdit.commit();
					}
	        		
	        }
	        });

	        Button back = ((Button)findViewById(R.id.btnWizard1));
	        Button next = ((Button)findViewById(R.id.btnWizard2));
	        
	        back.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					startActivityForResult(new Intent(getBaseContext(), Permissions.class), 1);
				}
			});
	    	
	    	next.setOnClickListener(new View.OnClickListener() {
			
	    		//Dirty flag variable - improve logic
				@Override
				public void onClick(View v) {
					if( flag == 1 )
						context.startActivity(new Intent(context, AppManager.class));
						
					else 
						showWizardFinal();
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
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				context.startActivity(new Intent(context, Orbot.class));

			}
		};
	
		
		 new AlertDialog.Builder(context)
		.setIcon(R.drawable.icon)
        .setTitle(title)
        .setPositiveButton(R.string.button_close, ocListener)
        .setMessage(msg)
        .show();
	
	
				
		
	}
}