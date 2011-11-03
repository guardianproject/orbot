package org.torproject.android.wizard;

import org.torproject.android.R;
import org.torproject.android.TorConstants;
import org.torproject.android.R.drawable;
import org.torproject.android.R.id;
import org.torproject.android.R.layout;
import org.torproject.android.R.string;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class LotsaText extends Activity implements TorConstants{
	
	private Context context;
	
	protected void onCreate(Bundle savedInstanceState)
	{	
		
		
        super.onCreate(savedInstanceState);
        context = this;
        

	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		setContentView(R.layout.scrollingtext_buttons_view);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		boolean wizardScreen1 = prefs.getBoolean("wizardscreen1",true);
		if(wizardScreen1)
			stepOne();
		else
			stepTwo();
        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	
		
	}
	
	
	
	private void stepOne() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Editor pEdit = prefs.edit();
		pEdit.putBoolean("wizardscreen1",true);
		pEdit.commit();
		
		String title = context.getString(R.string.wizard_title);
		String msg = context.getString(R.string.wizard_title_msg);
		
		TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
		txtTitle.setText(title);
        
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody));
		txtBody.setText(msg);
		
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
        ImageView img = (ImageView) findViewById(R.id.orbot_image);
        
    	btn1.setVisibility(Button.INVISIBLE);
    	img.setImageResource(R.drawable.tor);

    	btn2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stepTwo();
			}
		});
		
	}
	
	private void stepTwo() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Editor pEdit = prefs.edit();
		pEdit.putBoolean("wizardscreen1",false);
		pEdit.commit();
		
		setContentView(R.layout.scrollingtext_buttons_view);
		String title = context.getString(R.string.wizard_warning_title);
		String msg = context.getString(R.string.wizard_warning_msg);
		
		TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
		txtTitle.setText(title);
        
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody));
		txtBody.setText(msg);
		
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
        ImageView img = (ImageView) findViewById(R.id.orbot_image);
        
    	btn1.setVisibility(Button.VISIBLE);
    	img.setImageResource(R.drawable.warning);
    	
    	btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				stepOne();
			}
		});
    	
    	btn2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getBaseContext(), Permissions.class), 1);
			}
		});
		
	}
	
	
}