package org.torproject.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutActivity extends Activity implements OnClickListener
{
	//WizardHelper wizard = null;
	
	private int title[] = {
			R.string.wizard_title,
			R.string.wizard_warning_title,
			R.string.wizard_permissions_title
	};
	
	private int msg[] = {
			R.string.wizard_title_msg,
			R.string.wizard_warning_msg,
	};
	
	private String buttons[][] =
	{
			{null,"Next"},
			{"Back","Next"},
			{"Back","Next"},
			{"Back","Next"},
			
	};
	
	private View.OnClickListener listener[][] =
	{
			{
				null,
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						nextContent();
						
					}
				}
			},

			{
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						prevContent();
						
					}
				},
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						nextContent();
						
					}
				}
			},

			{
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						prevContent();
						
					}
				},
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						
						//Intent intent = new Intent(getBaseContext(), AccountWizardActivity.class);
						
					
						//startActivity(intent);

					}
				}
			},

			
			
	};
	                                 
	
	private int contentIdx = -1;
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		
        if (contentIdx == -1)
        {
            setContentView(R.layout.scrollingtext_buttons_view);
        
        	nextContent ();
        }
	}
	
	private void prevContent ()
	{
		contentIdx--;
		showContent(contentIdx);
	}
	
	private void nextContent ()
	{
		contentIdx++;
		showContent(contentIdx);
	}
	
	private void showContent (int contentIdx)
	{
		TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
		txtTitle.setText(getString(title[contentIdx]));
        
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody));
		txtBody.setText(getString(msg[contentIdx]));
        
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        
        ImageView img = (ImageView) findViewById(R.id.gibber_image);
        
        if (buttons[contentIdx][0] != null)
        {
        	btn1.setText(buttons[contentIdx][0]);
        	btn1.setOnClickListener(listener[contentIdx][0]);
        	btn1.setVisibility(Button.VISIBLE);
        	
        }
        else
        {
        	btn1.setVisibility(Button.INVISIBLE);
        }
        
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
        if (buttons[contentIdx][1] != null)
        {
        	btn2.setText(buttons[contentIdx][1]);
        	btn2.setOnClickListener(listener[contentIdx][1]);
        	btn2.setVisibility(Button.VISIBLE);
        	
        }
        else
        {
        	btn2.setVisibility(Button.INVISIBLE);
        }
        
        if(contentIdx !=0)
        {
        	img.setVisibility(ImageView.GONE);
        }
        else
        {	
        	img.setVisibility(ImageView.VISIBLE);
        }
      
	}
	
	

	@Override
	protected void onResume() {
		super.onResume();
	
		
	}




	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		
		
	}
	
	
	
}
