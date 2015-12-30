package org.torproject.android.vpn;

import org.torproject.android.Prefs;
import org.torproject.android.R;
import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/*
 * To combat background service being stopped/swiped
 */
public class VPNEnableActivity extends Activity {
	
	private final static int REQUEST_VPN = 7777;
	private	Intent intent = null;

	@Override
	public void onCreate( Bundle icicle ) {
		super.onCreate( icicle );

		Log.d("VPNEnableActivity","prompting user to start Orbot VPN");

		intent = VpnService.prepare(this);
		
		if (intent != null)
			promptStartVpnService();
		else
			startVpnService ();
		
	}
	
	public void promptStartVpnService ()
    {
    	 LayoutInflater li = LayoutInflater.from(this);
         View view = li.inflate(R.layout.layout_diag, null); 
         
         TextView versionName = (TextView)view.findViewById(R.id.diaglog);
         versionName.setText(R.string.you_can_enable_all_apps_on_your_device_to_run_through_the_tor_network_using_the_vpn_feature_of_android_);    
         
         new AlertDialog.Builder(this)
         .setTitle(getString(R.string.app_name) + ' ' + getString(R.string.apps_mode))
         .setView(view)
         .setPositiveButton(R.string.activate, new Dialog.OnClickListener ()
         {

			@Override
			public void onClick(DialogInterface dialog, int which) {
		        Prefs.putUseVpn(true);
		        
				startVpnService();
				
			}

        	 
         })
         .setNegativeButton(R.string.btn_cancel, new Dialog.OnClickListener ()
         {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				finish();
			}
        	 
         })
         .show();
    }
	 
	private void startVpnService ()
	{
   		if (intent == null)
   		{
   			Log.d("VPNEnableActivity","VPN enabled, starting Tor...");
            sendIntentToService(TorServiceConstants.CMD_VPN);
            /**
            Handler h = new Handler();
            h.postDelayed(new Runnable () {
            	
            	public void run ()
            	{
            		sendIntentToService(TorServiceConstants.ACTION_START);		
            		finish();
            	}
            }, 1000);
            */
           
   			
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
	            Handler h = new Handler();
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