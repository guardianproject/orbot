/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.torproject.android.BuildConfig;
import org.torproject.android.R;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.service.vpn.TorifiedApp;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class AppManagerActivity extends AppCompatActivity implements OnClickListener, OrbotConstants {


    class TorifiedAppWrapper {
        String header = null;
        String subheader = null;
        TorifiedApp app = null;
    }

    private PackageManager pMgr = null;
    private SharedPreferences mPrefs = null;
    private GridView listAppsAll;
    private ListAdapter adapterAppsAll;
    private ProgressBar progressBar;
    private ArrayList<String> alSuggested;

    /**
     * @return true if the app is "enabled", not Orbot, and not in
     * {@link #BYPASS_VPN_PACKAGES}
     */
    public static boolean includeAppInUi(ApplicationInfo applicationInfo) {
        if (!applicationInfo.enabled) return false;
        if (BYPASS_VPN_PACKAGES.contains(applicationInfo.packageName)) return false;
        return !BuildConfig.APPLICATION_ID.equals(applicationInfo.packageName);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pMgr = getPackageManager();

        this.setContentView(R.layout.layout_apps);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        listAppsAll = findViewById(R.id.applistview);
        progressBar = findViewById(R.id.progressBar);

        //need a better way to manage this list
        alSuggested = new ArrayList<>();
        alSuggested.add("org.thoughtcrime.securesms");
        alSuggested.add("com.whatsapp");
        alSuggested.add("com.instagram.android");
        alSuggested.add("im.vector.app");
        alSuggested.add("org.telegram.messenger");
        alSuggested.add("com.twitter.android");
        alSuggested.add("com.facebook.orca");
        alSuggested.add("com.facebook.mlite");
        alSuggested.add("com.brave.browser");
        alSuggested.add("org.mozilla.focus");

    }

    @Override
    protected void onResume() {
        super.onResume();
        mPrefs = Prefs.getSharedPrefs(getApplicationContext());
        reloadApps();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_save_apps) {
            saveAppSettings();
            finish();
        }
        else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void reloadApps() {
        new ReloadAppsAsyncTask(this).execute();
    }

    List<TorifiedApp> allApps, suggestedApps;
    List<TorifiedAppWrapper> uiList = new ArrayList<>();

    private void loadApps() {
        if (allApps == null)
            allApps = getApps(AppManagerActivity.this, mPrefs, null, alSuggested);

        TorifiedApp.sortAppsForTorifiedAndAbc(allApps);

        if (suggestedApps == null)
            suggestedApps = getApps(AppManagerActivity.this, mPrefs, alSuggested, null);

        final LayoutInflater inflater = getLayoutInflater();
        // only show suggested apps, text, etc and other apps header if there are any suggested apps installed...
        if (suggestedApps.size() > 0) {
            TorifiedAppWrapper headerSuggested = new TorifiedAppWrapper();
            headerSuggested.header = getString(R.string.apps_suggested_title);
            uiList.add(headerSuggested);
            TorifiedAppWrapper subheaderSuggested = new TorifiedAppWrapper();
            subheaderSuggested.subheader = getString(R.string.app_suggested_subtitle);
            uiList.add(subheaderSuggested);
            for (TorifiedApp app : suggestedApps) {
                TorifiedAppWrapper taw = new TorifiedAppWrapper();
                taw.app = app;
                uiList.add(taw);
            }
            TorifiedAppWrapper headerAllApps = new TorifiedAppWrapper();
            headerAllApps.header = getString(R.string.apps_other_apps);
            uiList.add(headerAllApps);
        }
        for (TorifiedApp app : allApps) {
            TorifiedAppWrapper taw = new TorifiedAppWrapper();
            taw.app = app;
            uiList.add(taw);
        }

        adapterAppsAll = new ArrayAdapter<>(this, R.layout.layout_apps_item, R.id.itemtext, uiList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                ListEntry entry = null;

                if (convertView == null)
                    convertView = inflater.inflate(R.layout.layout_apps_item, parent, false);
                else
                    entry = (ListEntry) convertView.getTag();

                if (entry == null) {
                    // Inflate a new view
                    entry = new ListEntry();
                    entry.container = convertView.findViewById(R.id.appContainer);
                    entry.icon = convertView.findViewById(R.id.itemicon);
                    entry.box = convertView.findViewById(R.id.itemcheck);
                    entry.text = convertView.findViewById(R.id.itemtext);
                    entry.header = convertView.findViewById(R.id.tvHeader);
                    entry.subheader = convertView.findViewById(R.id.tvSubheader);
                    convertView.setTag(entry);
                }

                final TorifiedAppWrapper taw = uiList.get(position);

                if (taw.header != null) {
                    entry.header.setText(taw.header);
                    entry.header.setVisibility(View.VISIBLE);
                    entry.subheader.setVisibility(View.GONE);
                    entry.container.setVisibility(View.GONE);
                } else if (taw.subheader != null) {
                    entry.subheader.setVisibility(View.VISIBLE);
                    entry.subheader.setText(taw.subheader);
                    entry.container.setVisibility(View.GONE);
                    entry.header.setVisibility(View.GONE);
                } else {
                    TorifiedApp app = taw.app;
                    entry.header.setVisibility(View.GONE);
                    entry.subheader.setVisibility(View.GONE);
                    entry.container.setVisibility(View.VISIBLE);
                    if (entry.icon != null) {

                        try {
                            entry.icon.setImageDrawable(pMgr.getApplicationIcon(app.getPackageName()));
                            entry.icon.setTag(entry.box);
                            entry.icon.setOnClickListener(AppManagerActivity.this);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (entry.text != null) {
                        entry.text.setText(app.getName());
                        entry.text.setTag(entry.box);
                        entry.text.setOnClickListener(AppManagerActivity.this);
                    }

                    if (entry.box != null) {
                        entry.box.setChecked(app.isTorified());
                        entry.box.setTag(app);
                        entry.box.setOnClickListener(AppManagerActivity.this);

                    }
                }

                convertView.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus)
                        v.setBackgroundColor(getResources().getColor(R.color.dark_purple));
                    else
                    {
                        v.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    }
                });

                return convertView;
            }
        };
    }

    public static ArrayList<TorifiedApp> getApps(Context context, SharedPreferences prefs, ArrayList<String> filterInclude, ArrayList<String> filterRemove) {

        PackageManager pMgr = context.getPackageManager();
        String tordAppString = prefs.getString(PREFS_KEY_TORIFIED, "");

        String[] tordApps;

        StringTokenizer st = new StringTokenizer(tordAppString, "|");
        tordApps = new String[st.countTokens()];
        int tordIdx = 0;
        while (st.hasMoreTokens()) {
            tordApps[tordIdx++] = st.nextToken();
        }
        Arrays.sort(tordApps);
        List<ApplicationInfo> lAppInfo = pMgr.getInstalledApplications(0);
        Iterator<ApplicationInfo> itAppInfo = lAppInfo.iterator();
        ArrayList<TorifiedApp> apps = new ArrayList<>();

        while (itAppInfo.hasNext()) {
            ApplicationInfo aInfo = itAppInfo.next();
            if (!includeAppInUi(aInfo)) continue;

            if (filterInclude != null) {
                boolean wasFound = false;
                for (String filterId : filterInclude)
                    if (filterId.equals(aInfo.packageName)) {
                        wasFound = true;
                        break;
                    }

                if (!wasFound)
                     continue;
            }

            if (filterRemove != null) {
                boolean wasFound = false;
                for (String filterId : filterRemove)
                    if (filterId.equals(aInfo.packageName)) {
                        wasFound = true;
                        break;
                    }

                if (wasFound)
                    continue;
            }

            TorifiedApp app = new TorifiedApp();

            try {
                PackageInfo pInfo = pMgr.getPackageInfo(aInfo.packageName, PackageManager.GET_PERMISSIONS);
                if (pInfo != null && pInfo.requestedPermissions != null) {
                    for (String permInfo : pInfo.requestedPermissions) {
                        if (permInfo.equals(Manifest.permission.INTERNET)) {
                            app.setUsesInternet(true);
                        }
                    }
                }


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                app.setName(pMgr.getApplicationLabel(aInfo).toString());
            } catch (Exception e) {
                // no name
                continue; //we only show apps with names
            }


            if (!app.usesInternet())
                continue;
            else {
                apps.add(app);
            }

            app.setEnabled(aInfo.enabled);
            app.setUid(aInfo.uid);
            app.setUsername(pMgr.getNameForUid(app.getUid()));
            app.setProcname(aInfo.processName);
            app.setPackageName(aInfo.packageName);


            // check if this application is allowed
            app.setTorified(Arrays.binarySearch(tordApps, app.getPackageName()) >= 0);

        }

        Collections.sort(apps);

        return apps;
    }

    private void saveAppSettings() {

        StringBuilder tordApps = new StringBuilder();
        Intent response = new Intent();

        for (TorifiedApp tApp : allApps) {
            if (tApp.isTorified()) {
                tordApps.append(tApp.getPackageName());
                tordApps.append("|");
                response.putExtra(tApp.getPackageName(), true);
            }
        }

        for (TorifiedApp tApp : suggestedApps) {
            if (tApp.isTorified()) {
                tordApps.append(tApp.getPackageName());
                tordApps.append("|");
                response.putExtra(tApp.getPackageName(), true);
            }
        }

        Editor edit = mPrefs.edit();
        edit.putString(PREFS_KEY_TORIFIED, tordApps.toString());
        edit.apply();

        setResult(RESULT_OK, response);
    }

    public void onClick(View v) {

        CheckBox cbox = null;

        if (v instanceof CheckBox)
            cbox = (CheckBox) v;
        else if (v.getTag() instanceof CheckBox)
            cbox = (CheckBox) v.getTag();
        else if (v.getTag() instanceof ListEntry)
            cbox = ((ListEntry) v.getTag()).box;

        if (cbox != null) {
            final TorifiedApp app = (TorifiedApp) cbox.getTag();
            if (app != null) {
                app.setTorified(!app.isTorified());
                cbox.setChecked(app.isTorified());
            }

        }
    }

    private static class ReloadAppsAsyncTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<AppManagerActivity> activity;

        ReloadAppsAsyncTask(AppManagerActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            if (shouldStop()) return;
            activity.get().progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (shouldStop()) return null;
            activity.get().loadApps();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if (shouldStop()) return;
            AppManagerActivity ama = activity.get();
            ama.listAppsAll.setAdapter(ama.adapterAppsAll);
//            ama.listAppsAll.setAdapter(ama.adapterAppsAll);
            ama.progressBar.setVisibility(View.GONE);
        }

        private boolean shouldStop() {
            AppManagerActivity ama = activity.get();
            return ama == null || ama.isFinishing();
        }

    }

    private static class ListEntry {
        private CheckBox box;
        private TextView text; // app name
        private ImageView icon;

        private View container;
        private TextView header, subheader;
    }

}
