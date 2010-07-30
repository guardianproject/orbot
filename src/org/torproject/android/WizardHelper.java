package org.torproject.android;

import org.torproject.android.service.TorTransProxy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class WizardHelper implements TorConstants {

	private Context context;
	private AlertDialog currentDialog;
	
	public WizardHelper (Context context)
	{
		this.context = context;
	}

	
	public void showWizard ()
	{
		showWizardStep1();
	}
	
	public void showWizardStep1()
	{
		

		String 	title = context.getString(R.string.wizard_title);

		LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.layout_wizard_welcome, null); 
        
       
		showCustomDialog(title, view,context.getString(R.string.btn_next),context.getString(R.string.wizard_btn_tell_me_more),new DialogInterface.OnClickListener() {
					
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				if (which == DialogInterface.BUTTON_NEUTRAL)
				{

					showWizardStep2();
				}
				else if (which == DialogInterface.BUTTON_POSITIVE)
				{
					showAbout();
				}
				
			}
		});
	}
	
	public void showWizardStep2()
	{
		
	
		String title = context.getString(R.string.wizard_permissions_stock);
		
		LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.layout_wizard_stock, null); 
        
        Button btn1 = (Button)view.findViewById(R.id.WizardRootButtonEnable);
        
        btn1.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				
				
				boolean hasRoot = TorTransProxy.hasRootAccess();
				
				if (hasRoot)
				{
					currentDialog.dismiss();
					showWizardStep2Root();
				}
				else
				{
					Toast.makeText(context, "Unable to get root access", Toast.LENGTH_LONG).show();
					view.setEnabled(false);
				}
			}
		});
        
        CheckBox cb1 = (CheckBox)view.findViewById(R.id.CheckBoxConsent);
         
        cb1.setOnCheckedChangeListener(new OnCheckedChangeListener (){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
			
				currentDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(isChecked);
				
				
				
			}
        	
        });
        
 
		showCustomDialog(title, view,context.getString(R.string.btn_next),context.getString(R.string.btn_back),new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				if (which == DialogInterface.BUTTON_NEUTRAL)
				{
					showWizardTipsAndTricks();
				}
				else if (which == DialogInterface.BUTTON_POSITIVE)
				{
					showWizardStep1();
				}
				
			}
		});
		
		currentDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
		
		
	}
	
	public void showWizardStep2Root()
	{
		
		String title = null;
		String msg = null;
		
		
		
			title = context.getString(R.string.wizard_permissions_root);
			msg = context.getString(R.string.wizard_premissions_msg_root);
		
			
			
			showDialog(title, msg,context.getString(R.string.btn_next),context.getString(R.string.btn_back),new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					
					if (which == DialogInterface.BUTTON_NEUTRAL)
					{
						showWizardRootConfigureTorification();
					}
					else if (which == DialogInterface.BUTTON_POSITIVE)
					{
						showWizardStep1();
					}
					
				}
			});
			
		
	}

	public void showWizardTipsAndTricks()
	{
	
		String 	title = context.getString(R.string.wizard_tips_tricks);

		LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.layout_wizard_tips, null); 
        
        Button btn1 = (Button)view.findViewById(R.id.WizardRootButtonInstallOtrchat);
        
        btn1.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {

				String url = context.getString(R.string.otrchat_apk_url);
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

			}
		});
        
        Button btn2 = (Button)view.findViewById(R.id.WizardRootButtonInstallOrweb);

        btn2.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				
				String url = context.getString(R.string.orweb_apk_url);
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

			}
		});
        
		showCustomDialog(title, view,context.getString(R.string.btn_next),context.getString(R.string.btn_back),new DialogInterface.OnClickListener() {
			
			
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				if (which == DialogInterface.BUTTON_NEUTRAL)
				{
					showWizardFinal();
					
				}
				else if (which == DialogInterface.BUTTON_POSITIVE)
				{
					showWizardStep2();
				}
				
			}
		});
	}
	
	public void showWizardRootConfigureTorification()
	{
		
		LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.layout_wizard_root, null); 
        
        CheckBox cb1 = (CheckBox)view.findViewById(R.id.WizardRootCheckBox01);
        Button btn1 = (Button)view.findViewById(R.id.WizardRootButton01);
        
        cb1.setOnCheckedChangeListener(new OnCheckedChangeListener (){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
			
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

				Editor pEdit = prefs.edit();
				
				pEdit.putBoolean(PREF_TRANSPARENT, isChecked);
				pEdit.putBoolean(PREF_TRANSPARENT_ALL, isChecked);
				
				pEdit.commit();
				
				//Button btn1 = (Button)buttonView.getParent().findViewById(R.id.WizardRootButton01);
				//btn1.setEnabled(!isChecked);
				
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
        
		showCustomDialog(context.getString(R.string.wizard_configure),view,context.getString(R.string.btn_next),context.getString(R.string.btn_back),new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.dismiss();
				
				if (which == DialogInterface.BUTTON_NEUTRAL)
				{
					showWizardTipsAndTricks();
					
				}
				else if (which == DialogInterface.BUTTON_POSITIVE)
				{
					showWizardStep2();
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
		
		 new AlertDialog.Builder(context)
		.setIcon(R.drawable.icon)
        .setTitle(title)
        .setPositiveButton(R.string.button_close, null)
        .setMessage(msg)
        .show();
		
	
	
				
		
	}
	
	public void showDialog (String title, String msg, String button1, String button2, DialogInterface.OnClickListener ocListener)
	{
	 
//		dialog.setContentView(R.layout.custom_dialog);

	
		AlertDialog.Builder builder = new AlertDialog.Builder(context)
			.setIcon(R.drawable.icon)
	        .setTitle(title)
	        .setMessage(msg)
	        .setNeutralButton(button1, ocListener)
	        .setPositiveButton(button2, ocListener);
		
		
			currentDialog = builder.show();
			
	
	}
	
	private void showCustomDialog (String title, View view, String button1, String button2, DialogInterface.OnClickListener ocListener)
	{
	
			currentDialog = new AlertDialog.Builder(context)
			.setIcon(R.drawable.icon)
	        .setTitle(title)
	        .setView(view)
	        .setNeutralButton(button1, ocListener)
	        .setPositiveButton(button2, ocListener)
	        .show();	
		
	
	}
	
	private void showAbout ()
	{
		
		LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.layout_about, null); 
        TextView versionName = (TextView)view.findViewById(R.id.versionName);
        versionName.setText(R.string.app_version);    
        
		new AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.button_about))
        .setView(view)
        .setNeutralButton(context.getString(R.string.btn_back), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                      showWizard();
                }
        })
        .show();
	}
	
}

