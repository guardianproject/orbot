package org.torproject.android.ui.wizard;

import org.torproject.android.R;
import org.torproject.android.OrbotConstants;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class PromoAppsActivity extends Activity implements OrbotConstants {

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

    void stepFive(){
        
        
        String title = getString(R.string.wizard_tips_title);
        
        setTitle(title);
            
        Button btnLink = (Button)findViewById(R.id.WizardRootButtonInstallGibberbot);
        
        btnLink.setOnClickListener(new OnClickListener() {
            
            public void onClick(View view) {

                String url = getString(R.string.gibberbot_apk_url);
                finish();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            }
        });
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallOrweb);

        btnLink.setOnClickListener(new OnClickListener() {
            
            public void onClick(View view) {
                
                String url = getString(R.string.orweb_apk_url);
                finish();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            }
        });
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallDuckgo);

        btnLink.setOnClickListener(new OnClickListener() {
            
            public void onClick(View view) {
                
                String url = getString(R.string.duckgo_apk_url);
                finish();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            }
        });
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallFirefox);

        btnLink.setOnClickListener(new OnClickListener() {
            
            public void onClick(View view) {
                
                String url = getString(R.string.proxymob_setup_url);
                finish();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            }
        });
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallTwitter);

        btnLink.setOnClickListener(new OnClickListener() {
            
            public void onClick(View view) {
                
                String url = getString(R.string.twitter_setup_url);
                finish();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            }
        });
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallStoryMaker);

        btnLink.setOnClickListener(new OnClickListener() {
            
            public void onClick(View view) {
                
                String url = getString(R.string.story_maker_url);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            }
        });
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonInstallMartus);

        btnLink.setOnClickListener(new OnClickListener() {
            
            public void onClick(View view) {
                
                String url = getString(R.string.martus_url);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            }
        });
        
        
        btnLink = (Button)findViewById(R.id.WizardRootButtonGooglePlay);

        btnLink.setOnClickListener(new OnClickListener() {
            
            public void onClick(View view) {
                
                String url = getString(R.string.wizard_tips_play_url);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            }
        });        
        
        
      //  Button back = ((Button)findViewById(R.id.btnWizard1));
        Button next = ((Button)findViewById(R.id.btnWizard2));
        
        /*
        back.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				finish();
				startActivityForResult(new Intent(PromoAppsActivity.this, ChooseLocaleWizardActivity.class), 1);
			}
		});*/
    	
    	next.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				//showWizardFinal();
				
				finish();
			}
		});
        
	}
	
	
	
	//Code to override the back button!
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if(keyCode == KeyEvent.KEYCODE_BACK){
	    	finish();
	    	startActivity(new Intent(getBaseContext(), ChooseLocaleWizardActivity.class));
	    	return true;
	    }
	    return false;
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
