/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.R;
import org.torproject.android.service.util.TorServiceUtils;
import org.torproject.android.service.transproxy.TorifiedApp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

public class AppManager extends AppCompatActivity implements OnCheckedChangeListener, OnClickListener, OrbotConstants {

    private ListView listApps;
    private final static String TAG = "Orbot";
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        this.setContentView(R.layout.layout_apps);
        setTitle(R.string.apps_mode);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        Button buttonSelectAll, buttonSelectNone, buttonInvert;

        buttonSelectAll =   (Button) findViewById(R.id.button_proxy_all);
        buttonSelectNone =  (Button) findViewById(R.id.button_proxy_none);
        buttonInvert =      (Button) findViewById(R.id.button_invert_selection);

        buttonSelectAll.setOnClickListener(new OnAutoClickListener(0));
        buttonSelectNone.setOnClickListener(new OnAutoClickListener(1));
        buttonInvert.setOnClickListener(new OnAutoClickListener(2));
    }

    class OnAutoClickListener implements Button.OnClickListener {
        private int status;
        public OnAutoClickListener(int status){
            this.status = status;
        }
        @SuppressWarnings("unchecked")
        public void onClick(View button){
            ListView listView;
            ViewGroup viewGroup;
            View parentView, currentView;
            ArrayAdapter<TorifiedApp> adapter;
            TorifiedApp app;
            CheckBox box;
            float buttonId;
            boolean[] isSelected;
            int posI, selectedI, lvSz;

            buttonId = button.getId();
            listView = (ListView) findViewById(R.id.applistview);
            lvSz = listView.getCount();
            isSelected = new boolean[lvSz];

            selectedI = -1;

            if (this.status == 0){
                Log.d(TAG, "Proxifying ALL");
            }else if (this.status == 1){
                Log.d(TAG, "Proxifying NONE");
            }else {
                Log.d(TAG, "Proxifying invert");
            }

            Context context = getApplicationContext();
            SharedPreferences prefs = TorServiceUtils.getSharedPrefs(context);
            ArrayList<TorifiedApp> apps = getApps(context, prefs);
            parentView = (View) findViewById(R.id.applistview);
            viewGroup = (ViewGroup) listView;

            adapter = (ArrayAdapter<TorifiedApp>) listApps.getAdapter();
            if (adapter == null){
                Log.w(TAG, "List adapter is null. Getting apps.");
                loadApps(prefs);
                adapter = (ArrayAdapter<TorifiedApp>) listApps.getAdapter();
            }

            for (int i = 0 ; i < adapter.getCount(); ++i){
                app = (TorifiedApp) adapter.getItem(i);
                currentView = adapter.getView(i, parentView, viewGroup);
                box = (CheckBox) currentView.findViewById(R.id.itemcheck);

                if (this.status == 0){
                    app.setTorified(true);
                }else if (this.status == 1){
                    app.setTorified(false);
                }else {
                    app.setTorified(!app.isTorified());
                }

                if (box != null)
                    box.setChecked(app.isTorified());

            }
            saveAppSettings(context);
            loadApps(prefs);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;


        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        listApps = (ListView)findViewById(R.id.applistview);

        mPrefs = TorServiceUtils.getSharedPrefs(getApplicationContext());
        loadApps(mPrefs);
    }

    SharedPreferences mPrefs = null;
    ArrayList<TorifiedApp> mApps = null;
    
    private void loadApps (SharedPreferences prefs)
    {
        
        mApps = getApps(getApplicationContext(), prefs);
        
        /*
        Arrays.sort(apps, new Comparator<TorifiedApp>() {
            public int compare(TorifiedApp o1, TorifiedApp o2) {
                if (o1.isTorified() == o2.isTorified()) return o1.getName().compareTo(o2.getName());
                if (o1.isTorified()) return -1;
                return 1;
            }
        });*/
        
        final LayoutInflater inflater = getLayoutInflater();
        
        ListAdapter adapter = new ArrayAdapter<TorifiedApp>(this, R.layout.layout_apps_item, R.id.itemtext,mApps) {

            public View getView(int position, View convertView, ViewGroup parent) {

                ListEntry entry = null;

                if (convertView == null)
                    convertView = inflater.inflate(R.layout.layout_apps_item, parent, false);
                else
                    entry = (ListEntry) convertView.getTag();;

                if (entry == null) {
                    // Inflate a new view
                    entry = new ListEntry();
                    entry.icon = (ImageView) convertView.findViewById(R.id.itemicon);
                    entry.box = (CheckBox) convertView.findViewById(R.id.itemcheck);
                    entry.text = (TextView) convertView.findViewById(R.id.itemtext);


                    convertView.setTag(entry);


                }

                final TorifiedApp app = mApps.get(position);

                if (entry.icon != null) {
                    if (app.getIcon() != null)
                        entry.icon.setImageDrawable(app.getIcon());
                    else
                        entry.icon.setVisibility(View.GONE);
                }

                if (entry.text != null) {
                    entry.text.setText(app.getName());
                    entry.text.setOnClickListener(AppManager.this);
                    entry.text.setOnClickListener(AppManager.this);

                    if (entry.box != null)
                        entry.text.setTag(entry.box);
                }


                if (entry.box != null) {
                    entry.box.setOnCheckedChangeListener(AppManager.this);
                    entry.box.setTag(app);
                    entry.box.setChecked(app.isTorified());


                }

                return convertView;
            }
        };
        
        listApps.setAdapter(adapter);
        
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

    
    public static ArrayList<TorifiedApp> getApps (Context context, SharedPreferences prefs)
    {

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
        
        ArrayList<TorifiedApp> apps = new ArrayList<TorifiedApp>();
        
        ApplicationInfo aInfo = null;
        
        int appIdx = 0;
        TorifiedApp app = null;
        
        while (itAppInfo.hasNext())
        {
            aInfo = itAppInfo.next();
            
            app = new TorifiedApp();
            
            try {
                PackageInfo pInfo = pMgr.getPackageInfo(aInfo.packageName, PackageManager.GET_PERMISSIONS);
                
                if (pInfo != null && pInfo.requestedPermissions != null)
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
            
            
            if (!app.usesInternet())
                continue;
            else
            {
                apps.add(app);
            }
            
            
            app.setEnabled(aInfo.enabled);
            app.setUid(aInfo.uid);
            app.setUsername(pMgr.getNameForUid(app.getUid()));
            app.setProcname(aInfo.processName);
            app.setPackageName(aInfo.packageName);
            
            try
            {
                app.setName(pMgr.getApplicationLabel(aInfo).toString());
            }
            catch (Exception e)
            {
                app.setName(aInfo.packageName);
            }
            
            
            //app.setIcon(pMgr.getApplicationIcon(aInfo));
            
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
    
        Collections.sort(apps);
        
        return apps;
    }
    

    public void saveAppSettings (Context context)
    {

        StringBuilder tordApps = new StringBuilder();

        for (TorifiedApp tApp:mApps)
        {
            if (tApp.isTorified())
            {
                tordApps.append(tApp.getUsername());
                tordApps.append("|");
            }
        }
        
        Editor edit = mPrefs.edit();
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
