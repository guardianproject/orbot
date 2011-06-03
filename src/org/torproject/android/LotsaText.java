package org.torproject.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class LotsaText extends Activity implements TorConstants{
	
	private Context context;
	private int step = -1;
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        context = this;

	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		
		
        if (step == -1)
        {			
        	setContentView(R.layout.scrollingtext_buttons_view);
        	stepOne();
        }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	
		
	}
	
	
	/*public void startWizard(){
		
		switch(step){
		
		case -1 : stepOne();break;
		
		}
	}
	*/
	private void stepOne() {
		
		step++;
		
		//setContentView(R.layout.scrollingtext_buttons_view);
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
    	img.setVisibility(ImageView.VISIBLE);
    	
    	btn2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stepTwo();
			}
		});
		
	}
	
	private void stepTwo() {
		step++;
		
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
    	img.setVisibility(ImageView.INVISIBLE);
    	
    	btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				stepOne();
			}
		});
    	
    	btn2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//stepThree();
			}
		});
		
	}
}