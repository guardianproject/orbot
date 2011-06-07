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
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ConfigureTransProxy extends Activity implements TorConstants {

	private Context context;
	
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
		
			CheckBox cb1 = (CheckBox)findViewById(R.id.WizardRootCheckBox01);
	        Button btn1 = (Button)findViewById(R.id.WizardRootButton01);
	        
	        cb1.setOnCheckedChangeListener(new OnCheckedChangeListener (){

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
				
					
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

					Editor pEdit = prefs.edit();
					
					pEdit.putBoolean(PREF_TRANSPARENT, isChecked);
					pEdit.putBoolean(PREF_TRANSPARENT_ALL, isChecked);
					
					pEdit.commit();
					
				}
	        	
	        });
	        
	     
	        
	        btn1.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View view) {
					
					
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

					Editor pEdit = prefs.edit();
					pEdit.putBoolean(PREF_TRANSPARENT, true);
					pEdit.putBoolean(PREF_TRANSPARENT_ALL, false);
					pEdit.commit();
					
					context.startActivity(new Intent(context, AppManager.class));
					
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
				
				@Override
				public void onClick(View v) {
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
				//close wizard - return to orbot
				
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