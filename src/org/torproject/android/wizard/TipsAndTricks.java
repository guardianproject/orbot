package org.torproject.android.wizard;

import org.torproject.android.Orbot;
import org.torproject.android.R;
import org.torproject.android.TorConstants;
import org.torproject.android.R.drawable;
import org.torproject.android.R.id;
import org.torproject.android.R.layout;
import org.torproject.android.R.string;

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
import android.widget.ImageView;
import android.widget.TextView;

public class TipsAndTricks extends Activity implements TorConstants {

	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		setContentView(R.layout.layout_wizard_tips);
		
		stepFive();
        
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

	void stepFive(){
		
		String title = getString(R.string.wizard_tips_title);
		TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
		txtTitle.setText(title);
		 
		   ImageView img = (ImageView) findViewById(R.id.orbot_image);
	    	img.setImageResource(R.drawable.icon);
	    
	    	
        Button btnLink = (Button)findViewById(R.id.WizardRootButtonInstallGibberbot);
        
        btnLink.setOnClickListener(new OnClickListener() {
			
			public void onClick(View view) {

				String url = getString(R.string.gibberbot_apk_url);
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

			}
		});
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallOrweb);

        btnLink.setOnClickListener(new OnClickListener() {
			
			public void onClick(View view) {
				
				String url = getString(R.string.orweb_apk_url);
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

			}
		});
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallDuckgo);

        btnLink.setOnClickListener(new OnClickListener() {
			
			public void onClick(View view) {
				
				String url = getString(R.string.duckgo_apk_url);
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

			}
		});
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallFirefox);

        btnLink.setOnClickListener(new OnClickListener() {
			
			public void onClick(View view) {
				
				String url = getString(R.string.proxymob_setup_url);
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

			}
		});
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallTwitter);

        btnLink.setOnClickListener(new OnClickListener() {
			
			public void onClick(View view) {
				
				String url = getString(R.string.twitter_setup_url);
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

			}
		});
        
        
        Button back = ((Button)findViewById(R.id.btnWizard1));
        Button next = ((Button)findViewById(R.id.btnWizard2));
        
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
        
	}
	
	private void showWizardFinal ()
	{
		setContentView(R.layout.scrollingtext_buttons_view);
		String title =  getString(R.string.wizard_final);
		String msg = getString(R.string.wizard_final_msg);
		
		TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
		txtTitle.setText(title);
        
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody));
		txtBody.setText(msg);
		
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
        ImageView img = (ImageView) findViewById(R.id.orbot_image);
        
        btn2.setText(getString(R.string.btn_finish));
    	btn1.setVisibility(Button.VISIBLE);
    	img.setImageResource(R.drawable.icon);
    	
    	btn1.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				startActivityForResult(new Intent(getBaseContext(), Permissions.class), 1);

			}
		});
    	
    	btn2.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				startActivityForResult(new Intent(getBaseContext(), Orbot.class), 1);

			}
		});
	}
	/*
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
	
	
				
		
	}*/
}