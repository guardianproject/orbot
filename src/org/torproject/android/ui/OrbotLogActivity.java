/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */

package org.torproject.android.ui;

import org.torproject.android.OrbotConstants;
import org.torproject.android.service.TorServiceConstants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;


public class OrbotLogActivity extends Activity implements OrbotConstants
{
	
    /** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	doLayout();
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
			      new IntentFilter(TorServiceConstants.LOCAL_ACTION_LOG));

		
	}
	
	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		
		
		
	  @Override
	  public void onReceive(Context context, Intent intent) {
	    // Get extra data included in the Intent
		  
		if (intent.hasExtra(TorServiceConstants.LOCAL_EXTRA_LOG))
		{
			String log = intent.getStringExtra(TorServiceConstants.LOCAL_EXTRA_LOG);
			updateStatus(log);
		}
		
		
	  }
	};

	
	
	private void doLayout ()
	{
		

    }
	
	
	private void updateStatus(String log)
	{
		
	}
   

   	@Override
	protected void onDestroy() {
		super.onDestroy();
		  LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

	}

	

}
