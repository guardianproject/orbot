/* Copyright (c) 2009, Nathan Freitas, The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


import net.freehaven.tor.control.EventHandler;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TorControlPanel extends Activity implements OnClickListener, TorConstants, EventHandler
{
	
	private final static String TAG = "Tor";
	
	private static Intent torService = null;
	
	private boolean updateLog = false;
	private boolean updateStatus = false;
	
	private TextView lblStatus = null;
	private ImageView imgStatus = null;
	private String txtStatus = "";
	private int torStatus = STATUS_OFF;
	
	private Thread threadStatus = null;
	
    private WebView mWebView;

	private int currentView = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	setTheme(android.R.style.Theme_Black);

        
        showMain();

      
    }
    
    
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuItem mItem = menu.add(0, 1, Menu.NONE, "Home");
        MenuItem mItem2 = menu.add(0, 2, Menu.NONE, "Settings");
        MenuItem mItem3 = menu.add(0, 3, Menu.NONE, "Log");
       MenuItem mItem4 = menu.add(0, 4, Menu.NONE, "Help");
       
      mItem.setIcon(R.drawable.ic_menu_home);
       mItem2.setIcon(R.drawable.ic_menu_register);
       mItem3.setIcon(R.drawable.ic_menu_reports);
       mItem4.setIcon(R.drawable.ic_menu_about);
       
        return true;
    }
    
    /* (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		super.onMenuItemSelected(featureId, item);
		
		if (item.getItemId() == 1)
		{
			this.showMain();
		}
		else if (item.getItemId() == 2)
		{
			this.showSettings();
		}
		else if (item.getItemId() == 3)
		{
			this.showMessageLog();
		}
		else if (item.getItemId() == 4)
		{
			this.showWeb(DEFAULT_HOME_PAGE);
		}
		
        return true;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if(keyCode==KeyEvent.KEYCODE_BACK){
			if(currentView != R.layout.layout_main){
					
					showMain ();
					
					return true;
			}
			else{
				return super.onKeyDown(keyCode, event);
			}
		}
	
		return super.onKeyDown(keyCode, event);
		
	}
 
    /* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		TorService.setStatus(torStatus);
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		torStatus = TorService.getStatus();
		
		updateStatus ();
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		torStatus = TorService.getStatus();

		
		updateStatus ();
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
		TorService.setStatus(torStatus);
	}



	/*
	 * Show the main form UI
	 */
	private void showMain ()
    {
		updateLog = false;
		updateStatus = true;
		
		currentView = R.layout.layout_main;
    	setContentView(currentView);
    	
    	findViewById(R.id.imgStatus).setOnClickListener(this);
    	
    	lblStatus = (TextView)findViewById(R.id.lblStatus);
    	imgStatus = (ImageView)findViewById(R.id.imgStatus);
    	
    	updateStatus();
    }
	
	private void showWeb (String url)
	{
		
		
		currentView =R.layout.layout_web;
		 setContentView(currentView);
	        
	        mWebView = (WebView) findViewById(R.id.webview);

	        WebSettings webSettings = mWebView.getSettings();
	        webSettings.setSavePassword(false);
	        webSettings.setSaveFormData(false);
	        webSettings.setJavaScriptEnabled(true);
	      

	        mWebView.setWebChromeClient(new MyWebChromeClient());
	        
	        mWebView.loadUrl(url);
	
		
	}
	
	
	/*
	 * Show the message log UI
	 */
	private void showMessageLog ()
	{
		currentView = R.layout.layout_log;
		setContentView(currentView);
		((Button)findViewById(R.id.btnLogClear)).setOnClickListener(this);

		updateStatus = false;
		updateLog = true;
		
		Thread thread = new Thread ()
		{
			public void run ()
			{
				
				while (updateLog)
				{
					
					try {
						Thread.sleep(UPDATE_TIMEOUT);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					handler.sendEmptyMessage(0);
				}
			}
		};
		
		thread.start();
		
	}
	
	/*
	 * Load the Tor log and display it in a text field
	 */
	private void updateMessageLog ()
	{
				
		TextView tvLog = (TextView)findViewById(R.id.messageLog);
    	
		if (tvLog != null)
		{
			String output = loadTextFile(TOR_LOG_PATH);
		
			tvLog.setText(output);
		}
	
	}
	
	/*
	 * Handle to reload Tor debug log every few seconds while viewing it
	 */
	private Handler handler = new Handler() {
	
	        @Override
		    public void handleMessage(Message msg) {
	
	        	updateMessageLog ();
	
	        }
	
	    };
	    
	    /*
		 * Handle to reload Tor debug log every few seconds while viewing it
		 */
		private Handler handlerStatus = new Handler() {
		
		        @Override
			    public void handleMessage(Message msg) {
		
		        	updateStatus();
		        	
		        	// Toast.makeText(this,txtStatus, Toast.LENGTH_SHORT).show();
		        }
		
		    };
		
	
    /*
     * Load the basic settings application to display torrc
     * TODO: these needs to be improved into an actual form GUI
     */
	private void showSettings ()
	{
		updateStatus = false;
		updateLog = false;
		
		currentView = R.layout.layout_settings;
		setContentView(currentView);

		
		String output = loadTextFile(TORRC_INSTALL_PATH);

		TextView tvSettings = (TextView)findViewById(R.id.textSettings);
    	((Button)findViewById(R.id.btnSettingsSave)).setOnClickListener(this);
		tvSettings.setText(output);
		
	}
	
	
    /*
     * Set the state of the running/not running graphic and label
     */
    public void updateStatus ()
    {
    	
    	if (imgStatus != null)
    	{
    		
	    	if (torStatus == STATUS_ON)
	    	{
	    		imgStatus.setImageResource(R.drawable.toron);
	    		lblStatus.setText("ORbot is running\n- touch the bot to stop -");
	    		updateStatus = false;
	    	}
	    	else if (torStatus == STATUS_STARTING_UP)
	    	{
	    		imgStatus.setImageResource(R.drawable.torstarting);
	    		
	    		lblStatus.setText("ORbot reports:\n\"" + txtStatus + "\"");
	    		
	    	
	    	}
	    	else if (torStatus == STATUS_SHUTTING_DOWN)
	    	{
	    		imgStatus.setImageResource(R.drawable.torstopping);
	    		lblStatus.setText("ORbot is shutting down\nplease wait...");
	    		
	    	}
	    	else
	    	{
	    		imgStatus.setImageResource(R.drawable.toroff);
	    		lblStatus.setText("ORbot is not running\n- touch the bot to start -");
	    		updateStatus = false;
	    	}
    	}
    	
        
        
    }
  
    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	public void onClick(View view) {
		
		// the start button
		if (view.getId()==R.id.imgStatus)
		{
			//if Tor binary is not running, then start the service up
			if (TorService.getStatus()==STATUS_OFF)
			{
				torStatus = STATUS_STARTING_UP;
				txtStatus = "Connecting to Tor...";
				updateStatus();
				
				startTorService ();
				
				
			}
			else
			{
				
				torStatus = STATUS_SHUTTING_DOWN;
				updateStatus();
				
				stopService(torService);
				
				torStatus = STATUS_OFF;
				
				updateStatus();
			}
			
		}
		else if (view.getId()==R.id.btnLogClear)
		{
		
			saveTextFile(TOR_LOG_PATH,"");
		}
		else if (view.getId()==R.id.btnSettingsSave)
		{
		
			TextView tvSettings = (TextView)findViewById(R.id.textSettings);
			String newSettings =  tvSettings.getText().toString();
			saveTextFile(TORRC_INSTALL_PATH, newSettings);
			
		}
		
		
	}
	
	private void startTorService ()
	{
		if (torService == null)
		{
			torService = new Intent(this, TorService.class);
			//torService.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			TorService.setActivity(this);
		}
		
		startService(torService);
		
	
		
	}
	
	/*
	 * Load the log file text
	 */
	 public static String loadTextFile (String path)
	    {
	    	String line = null;
	    
	    	StringBuffer out = new StringBuffer();
	    	
	    	try {
		    	BufferedReader reader = new BufferedReader((new FileReader(new File(path))));

				while ((line = reader.readLine()) != null)
				{
					out.append(line);
					out.append('\n');
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return out.toString();
	    	
	    }
	 

		/*
		 * Load the log file text
		 */
		 public static boolean saveTextFile (String path, String contents)
		    {
			 	
		    	try {
		    		
		    		 FileWriter writer = new FileWriter( path, false );
                     writer.write( contents );
                     
                     writer.close();

                     
		    		
		    		return true;
			    	
				} catch (IOException e) {
				//	Log.i(TAG, "error writing file: " + path, e);
						e.printStackTrace();
					return false;
				}
				
				
		    	
		    }
	

	@Override
	public void bandwidthUsed(long read, long written) {
		Log.i(TAG,"BW Used: read=" + read + " written=" + written);
		
	}


	@Override
	public void circuitStatus(String status, String circID, String path) {
		Log.i(TAG,"CircuitStatus=" + status + ": " + circID);
		
	}


	@Override
	public void message(String severity, String msg) {
		
        	 // Log.println(priority, tag, msg)("["+severity+"] "+msg);
              //Toast.makeText(, text, duration)
        //      Toast.makeText(ACTIVITY, severity + ": " + msg, Toast.LENGTH_SHORT);
              Log.i(TAG, "[Tor Control Port] " + severity + ": " + msg);
              
              if (msg.indexOf(TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE)!=-1)
              {
            	  torStatus = STATUS_ON;
            	  
            	  
            	  
            	  //setupWebProxy(true);

              }
              
      
              txtStatus = msg;
              handlerStatus.sendEmptyMessage(0);
	
             
	}


	@Override
	public void newDescriptors(List<String> orList) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void orConnStatus(String status, String orName) {
		
		Log.i(TAG,"OrConnStatus=" + status + ": " + orName);
		
	}


	@Override
	public void streamStatus(String status, String streamID, String target) {
		Log.i(TAG,"StreamStatus=" + status + ": " + streamID);
		
	}


	@Override
	public void unrecognized(String type, String msg) {
		Log.i(TAG,"unrecognized log=" + type + ": " + msg);
		
	}
	 
	
	/**
     * Provides a hook for calling "alert" from javascript. Useful for
     * debugging your javascript.
     */
    final class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.d(TAG, message);
            result.confirm();
            return true;
        }
        
        
    }
	
}