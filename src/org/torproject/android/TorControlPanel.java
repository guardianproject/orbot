/* Copyright (c) 2009, Nathan Freitas, The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class TorControlPanel extends Activity implements OnClickListener, TorConstants
{
	
	private final static String LOG_TAG = "Tor";
	
	private Intent torService = null;
	
	private boolean updateLog = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       
        
        showMain();

        
    }
    
    
    
 
    /* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		setUIState ();
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		setUIState ();
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}



	/*
	 * Show the main form UI
	 */
	private void showMain ()
    {
		updateLog = false;
		
    	setContentView(R.layout.layout_main);
    	
    	((Button)findViewById(R.id.btnStart)).setOnClickListener(this);

    	((Button)findViewById(R.id.btnLog)).setOnClickListener(this);
    	
    	((Button)findViewById(R.id.btnSettings)).setOnClickListener(this);
    	

    	setUIState ();
    }
	
	/*
	 * Show the message log UI
	 */
	private void showMessageLog ()
	{
		setContentView(R.layout.layout_log);
		((Button)findViewById(R.id.btnLogClose)).setOnClickListener(this);

		updateLog = true;
		
		Thread thread = new Thread ()
		{
			public void run ()
			{
				
				while (updateLog)
				{
					handler.sendEmptyMessage(0);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
    	
		String output = loadLogFile(TOR_LOG_PATH);
		
		tvLog.setText(output);
	
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
     * Load the basic settings application to display torrc
     * TODO: these needs to be improved into an actual form GUI
     */
	private void showSettings ()
	{
		setContentView(R.layout.layout_settings);
		
		TextView tvLog = (TextView)findViewById(R.id.textSettings);
    	((Button)findViewById(R.id.btnSettingsClose)).setOnClickListener(this);

		String output = loadLogFile(TORRC_INSTALL_PATH);
		
		tvLog.setText(output);
		
	}
	
	
    /*
     * Set the state of the running/not running graphic and label
     */
    public void setUIState ()
    {
    	
    	
    	TextView lblStatus = (TextView)findViewById(R.id.lblStatus);
    	ImageView imgStatus = (ImageView)findViewById(R.id.imgStatus);
    	
    	Button btnStart = (Button)findViewById(R.id.btnStart);
    	
    	if (TorService.isRunning())
    	{
    		btnStart.setText("Stop Tor");
    		imgStatus.setImageResource(R.drawable.toron);
    		lblStatus.setText("Tor is running");

    	}
    	else
    	{
    		btnStart.setText("Start Tor");
    		imgStatus.setImageResource(R.drawable.toroff);
    		lblStatus.setText("Tor is not running");

    	}
    	
        
        
    }
  
    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	public void onClick(View view) {
		
		// the start button
		if (view.getId()==R.id.btnStart)
		{
			//if Tor binary is not running, then start the service up
			if (!TorService.isRunning())
			{
		        torService = new Intent(this, TorService.class);
		        torService.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        TorService.setActivity(this);
				
				startService(torService);
			      
			}
			else
			{
				
				if (torService == null)
					torService = new Intent(this, TorService.class);
				
				
				 TorService.setActivity(this);
				
				stopService(torService);
				
			}
			
			//update the UI
		     setUIState ();
	    
		}
		else if (view.getId()==R.id.btnLog)
		{
			showMessageLog();
			
		}
		else if (view.getId()==R.id.btnSettings)
		{
			showSettings();
			
		}
		else if (view.getId()==R.id.btnLogClose || view.getId()==R.id.btnSettingsClose)
		{
		
			showMain();
			
		}
		
		
	}
	
	/*
	 * Load the log file text
	 */
	 public static String loadLogFile (String path)
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
	  * Get the last line of the log file for status display
	  */
	 public static String getLastLine (String path)
	    {
	    	String line = null;
	    
	    	String lastLine = null;
	    	
	    	try {
		    	BufferedReader reader = new BufferedReader((new FileReader(new File(path))));

				while ((line = reader.readLine()) != null)
				{
					lastLine = line;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return lastLine;
	    	
	    }
	 
	
	
	
}