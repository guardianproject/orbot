/* Copyright (c) 2009, Nathan Freitas, The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TorControlPanel extends Activity implements OnClickListener, TorConstants
{
	
	private final static String LOG_TAG = "Tor";
	
	private Intent torService = null;
	
	private boolean updateLog = false;
	private boolean updateStatus = false;
	
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
       MenuItem mItem4 = menu.add(0, 4, Menu.NONE, "Browser");
       
      mItem.setIcon(R.drawable.ic_menu_home);
       mItem2.setIcon(R.drawable.ic_menu_register);
       mItem3.setIcon(R.drawable.ic_menu_reports);
       mItem4.setIcon(R.drawable.ic_menu_goto);
       
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
			Toast.makeText(this, "Not yet implemented!", Toast.LENGTH_SHORT);
		}
		
        return true;
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
		
		checkStatus ();
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		checkStatus ();
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
		updateStatus = true;
		
    	setContentView(R.layout.layout_main);
    	
    	findViewById(R.id.imgStatus).setOnClickListener(this);
    	    	
		
		Thread thread = new Thread ()
		{
			public void run ()
			{
				
				while (updateStatus)
				{
					handlerStatus.sendEmptyMessage(0);
					try {
						Thread.sleep(UPDATE_TIMEOUT);
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
	 * Show the message log UI
	 */
	private void showMessageLog ()
	{
		setContentView(R.layout.layout_log);
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
		
		        	checkStatus();
		
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
		
		setContentView(R.layout.layout_settings);

		
		String output = loadTextFile(TORRC_INSTALL_PATH);

		TextView tvSettings = (TextView)findViewById(R.id.textSettings);
    	((Button)findViewById(R.id.btnSettingsSave)).setOnClickListener(this);
		tvSettings.setText(output);
		
	}
	
	
    /*
     * Set the state of the running/not running graphic and label
     */
    public void checkStatus ()
    {
    	
    	
    	TextView lblStatus = (TextView)findViewById(R.id.lblStatus);
    	ImageView imgStatus = (ImageView)findViewById(R.id.imgStatus);
    	
    	if (imgStatus != null)
    	{
    		int torStatus = TorService.getStatus();
    		
	    	if (torStatus == STATUS_ON)
	    	{
	    		imgStatus.setImageResource(R.drawable.toron);
	    		lblStatus.setText("Tor is running\n- touch to stop -");
	    		updateStatus = false;
	    	}
	    	else if (torStatus == STATUS_STARTING_UP)
	    	{
	    		imgStatus.setImageResource(R.drawable.torstarting);
	    		lblStatus.setText("Tor is starting up\n(this might take a little bit)");
	
	    	
	    	}
	    	else if (torStatus == STATUS_SHUTTING_DOWN)
	    	{
	    		imgStatus.setImageResource(R.drawable.torstopping);
	    		lblStatus.setText("Tor is shutting down\nplease wait...");
	    		
	    	}
	    	else
	    	{
	    		imgStatus.setImageResource(R.drawable.toroff);
	    		lblStatus.setText("Tor is not running\n- touch to start -");
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
		        torService = new Intent(this, TorService.class);
		        torService.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        TorService.setActivity(this);
				
				startService(torService);
				
				
			}
			else if (TorService.getStatus()==STATUS_ON)
			{
				
				
				//stopService(torService);
				
				TorService.stopTor ();
				
			}
			
			
			showMain ();
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