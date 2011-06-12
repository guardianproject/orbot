package org.torproject.android;

import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceUtils;
import org.torproject.android.service.TorTransProxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Permissions extends Activity implements TorConstants {

	private Context context;
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        context = this;

	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		setContentView(R.layout.layout_wizard_permissions);
		
		stepThree();
        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	
		
	}
	
	private void stepThree(){
		
		boolean hasRoot = TorServiceUtils.checkRootAccess();
		
		if (hasRoot)
		{
			try {
				int resp = TorTransProxy.testOwnerModule(context);
				
				if (resp < 0)
				{
					hasRoot = false;
					Toast.makeText(context, "ERROR: IPTables OWNER module not available", Toast.LENGTH_LONG).show();

					Log.i(TorService.TAG,"ERROR: IPTables OWNER module not available");
				}
				
			} catch (Exception e) {
				
				hasRoot = false;
				Log.d(TorService.TAG,"ERROR: IPTables OWNER module not available",e);
			}
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Editor pEdit = prefs.edit();
		pEdit.putBoolean("has_root",hasRoot);
		pEdit.commit();
		
		if (hasRoot)
		{
			stepFourRoot();
		}
		else
		{
			stepFour();
		}
		
	}
	
	private void stepFourRoot(){
				
		String title = context.getString(R.string.wizard_permissions_title);
		String msg1 = context.getString(R.string.wizard_permissions_root_msg1);
		String msg2 = context.getString(R.string.wizard_permissions_root_msg2);
		
		TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
		txtTitle.setText(title);
        
        TextView txtBody1 = ((TextView)findViewById(R.id.WizardTextBody1));
	    txtBody1.setText(msg1);
		

        TextView txtBody2 = ((TextView)findViewById(R.id.WizardTextBody2));
		txtBody2.setText(msg2);
		txtBody2.setVisibility(TextView.VISIBLE);
		
		Button grantPermissions = ((Button)findViewById(R.id.grantPermissions));
		grantPermissions.setVisibility(Button.VISIBLE);
		
        Button back = ((Button)findViewById(R.id.btnWizard1));
        Button next = ((Button)findViewById(R.id.btnWizard2));
        next.setEnabled(false);
        
        CheckBox consent = (CheckBox)findViewById(R.id.checkBox);
        consent.setVisibility(CheckBox.VISIBLE);
        
        consent.setOnCheckedChangeListener(new OnCheckedChangeListener (){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
			
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

				Editor pEdit = prefs.edit();
				
				pEdit.putBoolean(PREF_TRANSPARENT, !isChecked);
				pEdit.putBoolean(PREF_TRANSPARENT_ALL, !isChecked);
				
				pEdit.commit();
				
				Button next = ((Button)findViewById(R.id.btnWizard2));
				if(isChecked)
					next.setEnabled(true);
				else
					next.setEnabled(false);
				
				
			}
        	
        });
        
        
        grantPermissions.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Check and Install iptables - TorTransProxy.testOwnerModule(this)
				startActivityForResult(new Intent(getBaseContext(), ConfigureTransProxy.class), 1);

				
			}
		});
        
    	back.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				startActivityForResult(new Intent(getBaseContext(), LotsaText.class), 1);
			}
		});
    	
    	
    	  next.setOnClickListener(new View.OnClickListener() {
    	 
			
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getBaseContext(), TipsAndTricks.class), 1);
			}
		});
		
	}
	
	private void stepFour(){
		
		Toast.makeText(context, "NON ROOT FUNC", Toast.LENGTH_SHORT).show();
		String title = context.getString(R.string.wizard_permissions_title);
		String msg = context.getString(R.string.wizard_permissions_msg);
		
		TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
		txtTitle.setText(title);
        
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody));
		txtBody.setText(msg);
		
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
            	
    	btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				startActivityForResult(new Intent(getBaseContext(), LotsaText.class), 1);
			}
		});
    	
    	btn2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getBaseContext(), TipsAndTricks.class), 1);
			}
		});
	}
		
	
}