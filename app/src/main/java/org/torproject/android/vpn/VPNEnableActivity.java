package org.torproject.android.vpn;

import org.torproject.android.R;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;

/*
 * To combat background service being stopped/swiped
 */

@TargetApi(14)
public class VPNEnableActivity extends AppCompatActivity {
	
	private final static int REQUEST_VPN = 7777;
	private	Intent intent = null;
	private boolean checkVpn = true;
	private Handler h = new Handler();
	
	@Override
	public void onCreate( Bundle icicle ) {
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate( icicle );

		Log.d("VPNEnableActivity","prompting user to start Orbot VPN");

		
	}
	
	public void onResume ()
	{
		super.onResume();
		
		if (checkVpn)
		{
			intent = VpnService.prepare(this);
			
			if (intent != null)
				promptStartVpnService();
			else
				startVpnService ();
			
			checkVpn = false;
		}
	}
	
	public void promptStartVpnService ()
    {
		startVpnService();
         
    }
	 
	private void startVpnService ()
	{
   		if (intent == null)
   		{
			Prefs.putUseVpn(true);

			Log.d("VPNEnableActivity","VPN enabled, starting Tor...");
            sendIntentToService(TorServiceConstants.CMD_VPN);
            
            Handler h = new Handler();
            h.postDelayed(new Runnable () {
            	
            	public void run ()
            	{
            		sendIntentToService(TorServiceConstants.ACTION_START);		
            		finish();
            	}
            }, 100);


   		}
   		else
   		{
   			Log.w("VPNEnableActivity","prompt for VPN");
            startActivityForResult(intent,REQUEST_VPN);

   		}

	}
	
	  @Override
	    protected void onActivityResult(int request, int response, Intent data) {
	        super.onActivityResult(request, response, data);
	        
	        if (request == REQUEST_VPN && response == RESULT_OK)
	        {
	            sendIntentToService(TorServiceConstants.CMD_VPN);	    
	            
	            h.postDelayed(new Runnable () {
	            	
	            	public void run ()
	            	{
	            		sendIntentToService(TorServiceConstants.ACTION_START);		
	            		 finish();
	            	}
	            }, 1000);
	            
	           
	            
	        }
	  }
	  

		private void sendIntentToService(String action) {
			Intent torService = new Intent(this, TorService.class);    
			torService.setAction(action);
			startService(torService);


		}
    
}
