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
import android.support.v4.app.ActivityCompat;
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
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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
	
	
	private void stepOne() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		Editor pEdit = prefs.edit();
		pEdit.putBoolean("wizardscreen1",true);
		pEdit.commit();
		
		String title = context.getString(R.string.wizard_title);
		String msg = context.getString(R.string.wizard_title_msg);
		
		setTitle(title);
        
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody));
		txtBody.setText(msg);
		
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
        
    	btn1.setVisibility(Button.INVISIBLE);

    	btn2.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				stepTwo();
			}
		});
		
	}
	
	private void stepTwo() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		Editor pEdit = prefs.edit();
		pEdit.putBoolean("wizardscreen1",false);
		pEdit.commit();
		
		setContentView(R.layout.scrollingtext_buttons_view);
		String title = context.getString(R.string.wizard_warning_title);
		String msg = context.getString(R.string.wizard_warning_msg);
		
		setTitle(title);
		
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody));
		txtBody.setText(msg);
		
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
        
    	btn1.setVisibility(Button.VISIBLE);
    	
    	
    	btn1.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				
				stepOne();
			}
		});
    	
    	btn2.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				startActivityForResult(new Intent(getBaseContext(), Permissions.class), 1);
			}
		});
		
	}

	
}