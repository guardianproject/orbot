package org.torproject.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public class WizardActivity extends Activity implements OnClickListener
{

	protected void onCreate(Bundle savedInstanceState)
	{
	
		this.setContentView(R.layout.layout_help);
		
	}
	
	
	
	@Override
	protected void onStart() {
		
		super.onStart();
	
		
	}



	@Override
	protected void onResume() {
		super.onResume();
		
		showStep1();
	}



	public void showStep1()
	{
		showDialog("Test","","foo","bar",this);
	}
	
	private void showDialog (String title, String msg, String button1, String button2, OnClickListener ocListener)
	{
	 
		new AlertDialog.Builder(this)
		.setInverseBackgroundForced(true)
        .setTitle(title)
        .setMessage(msg)
        .setNeutralButton(button1, ocListener)
        .setNegativeButton(button2, ocListener)
        .show();
	
	
	}



	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		
		
	}
	
	
	
}
