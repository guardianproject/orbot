/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.torproject.android.R;
import org.torproject.android.TorConstants;
import org.torproject.android.R.id;
import org.torproject.android.R.layout;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class AppManager extends Activity implements OnCheckedChangeListener, OnClickListener, TorConstants {

	private static ArrayList<TorifiedApp> apps = null;

	private ListView listApps;
	
	private AppManager mAppManager;


	private boolean appsLoaded = false;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		this.setContentView(R.layout.layout_apps);
		
		mAppManager = this;

	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		listApps = (ListView)findViewById(R.id.applistview);

		Button btnSave = (Button)findViewById(R.id.btnsave);
		btnSave.setOnClickListener(new OnClickListener()
		{

			public void onClick(View v) {
				finish();
			}
		});
		
		if (!appsLoaded)
			loadApps();
	}



	private void loadApps ()
	{
		resetApps(this);
        final ArrayList<TorifiedApp> apps = getApps(this);
        
        /*
        Arrays.sort(apps, new Comparator<TorifiedApp>() {
			public int compare(TorifiedApp o1, TorifiedApp o2) {
				if (o1.isTorified() == o2.isTorified()) return o1.getName().compareTo(o2.getName());
				if (o1.isTorified()) return -1;
				return 1;
			}
        });
        */
        
        final LayoutInflater inflater = getLayoutInflater();
		
        final ListAdapter adapter = new ArrayAdapter<TorifiedApp>(this,R.layout.layout_apps_item,R.id.itemtext,apps) {
        	public View getView(int position, View convertView, ViewGroup parent) {
       			ListEntry entry;
        		if (convertView == null) {
        			// Inflate a new view
        			convertView = inflater.inflate(R.layout.layout_apps_item, parent, false);
       				entry = new ListEntry();
       				entry.icon = (ImageView) convertView.findViewById(R.id.itemicon);
       				entry.box = (CheckBox) convertView.findViewById(R.id.itemcheck);
       				entry.text = (TextView) convertView.findViewById(R.id.itemtext);
       				
       				entry.text.setOnClickListener(mAppManager);
       				entry.text.setOnClickListener(mAppManager);
       				
       				convertView.setTag(entry);
       			
       				entry.box.setOnCheckedChangeListener(mAppManager);
        		} else {
        			// Convert an existing view
        			entry = (ListEntry) convertView.getTag();
        		}
        		
        		
        		final TorifiedApp app = apps.get(position);
        		
        	
        		entry.icon.setImageDrawable(app.getIcon());
        		entry.text.setText(app.getName());
        		
        		final CheckBox box = entry.box;
        		box.setTag(app);
        		box.setChecked(app.isTorified());
        		
        		entry.text.setTag(box);
        		entry.icon.setTag(box);
        		
       			return convertView;
        	}
        };
        
        listApps.setAdapter(adapter);
        
        appsLoaded = true;
		   
	}
	
	private static class ListEntry {
		private CheckBox box;
		private TextView text;
		private ImageView icon;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
	}

	public static ArrayList<TorifiedApp> getApps (Context context)
	{
		if (apps == null)
			resetApps(context);
		
		return apps;
	}
	
	public static ArrayList<TorifiedApp> resetApps (Context context)
	{

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		String tordAppString = prefs.getString(PREFS_KEY_TORIFIED, "");
		String[] tordApps;
		
		StringTokenizer st = new StringTokenizer(tordAppString,"|");
		tordApps = new String[st.countTokens()];
		int tordIdx = 0;
		while (st.hasMoreTokens())
		{
			tordApps[tordIdx++] = st.nextToken();
		}
		
		Arrays.sort(tordApps);

		//else load the apps up
		PackageManager pMgr = context.getPackageManager();
		
		List<ApplicationInfo> lAppInfo = pMgr.getInstalledApplications(0);
		
		Iterator<ApplicationInfo> itAppInfo = lAppInfo.iterator();
		
		apps = new ArrayList<TorifiedApp>();
		
		ApplicationInfo aInfo = null;
		
		int appIdx = 0;
		TorifiedApp app = null;
		
		while (itAppInfo.hasNext())
		{
			aInfo = itAppInfo.next();
			
			app = new TorifiedApp();
			
			try {
				PackageInfo pInfo = pMgr.getPackageInfo(aInfo.packageName, PackageManager.GET_PERMISSIONS);
				
				if (pInfo != null && pInfo.permissions != null)
				{
					for (String permInfo:pInfo.requestedPermissions)
					{
						if (permInfo.equals("android.permission.INTERNET"))
						{
							app.setUsesInternet(true);
							
						}
					}
					
				}
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if ((aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
		    {
		         //System app
				app.setUsesInternet(true);
		   }
			
			
			/*
			 * TODO n8fr8 June 2012: adding all apps for now b/c the Internet permission concept is not really working
			if (!app.usesInternet())
				continue;
			else
			{
				apps.add(app);
			}*/
			apps.add(app);
			
			app.setEnabled(aInfo.enabled);
			app.setUid(aInfo.uid);
			app.setUsername(pMgr.getNameForUid(app.getUid()));
			app.setProcname(aInfo.processName);
			app.setName(pMgr.getApplicationLabel(aInfo).toString());
			app.setIcon(pMgr.getApplicationIcon(aInfo));
			
			// check if this application is allowed
			if (Arrays.binarySearch(tordApps, app.getUsername()) >= 0) {
				app.setTorified(true);
			}
			else
			{
				app.setTorified(false);
			}
			
			appIdx++;
		}
	
		
		return apps;
	}
	

	public void saveAppSettings (Context context)
	{
		if (apps == null)
			return;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

	//	final SharedPreferences prefs = context.getSharedPreferences(PREFS_KEY, 0);

		StringBuilder tordApps = new StringBuilder();
		
		for (TorifiedApp tApp:apps)
		{
			if (tApp.isTorified())
			{
				tordApps.append(tApp.getUsername());
				tordApps.append("|");
			}
		}
		
		Editor edit = prefs.edit();
		edit.putString(PREFS_KEY_TORIFIED, tordApps.toString());
		edit.commit();
		
	}
	

	/**
	 * Called an application is check/unchecked
	 */
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		final TorifiedApp app = (TorifiedApp) buttonView.getTag();
		if (app != null) {
			app.setTorified(isChecked);
		}
		
		saveAppSettings(this);

	}


	public void onClick(View v) {
		
		CheckBox cbox = (CheckBox)v.getTag();
		
		final TorifiedApp app = (TorifiedApp)cbox.getTag();
		if (app != null) {
			app.setTorified(!app.isTorified());
			cbox.setChecked(app.isTorified());
		}
		
		saveAppSettings(this);
		
	}



	
}
