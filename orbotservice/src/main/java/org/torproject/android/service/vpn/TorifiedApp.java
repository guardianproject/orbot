package org.torproject.android.service.vpn;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import static org.torproject.android.service.vpn.VpnPrefs.PREFS_KEY_TORIFIED;

public class TorifiedApp implements Comparable {

    private boolean enabled;
    private int uid;
    private String username;
    private String procname;
    private String name;
    private Drawable icon;
    private String packageName;

    private boolean torified = false;
    private boolean usesInternet = false;
    private int[] enabledPorts;

    public static ArrayList<TorifiedApp> getApps(Context context, SharedPreferences prefs) {

        String tordAppString = prefs.getString(PREFS_KEY_TORIFIED, "");
        String[] tordApps;

        StringTokenizer st = new StringTokenizer(tordAppString, "|");
        tordApps = new String[st.countTokens()];
        int tordIdx = 0;
        while (st.hasMoreTokens()) {
            tordApps[tordIdx++] = st.nextToken();
        }

        Arrays.sort(tordApps);

        //else load the apps up
        PackageManager pMgr = context.getPackageManager();

        List<ApplicationInfo> lAppInfo = pMgr.getInstalledApplications(0);

        Iterator<ApplicationInfo> itAppInfo = lAppInfo.iterator();

        ArrayList<TorifiedApp> apps = new ArrayList<>();

        ApplicationInfo aInfo;

        int appIdx = 0;
        TorifiedApp app;

        while (itAppInfo.hasNext()) {
            aInfo = itAppInfo.next();

            app = new TorifiedApp();

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

            if ((aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                //System app
                app.setUsesInternet(true);
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

            try {
                app.setName(pMgr.getApplicationLabel(aInfo).toString());
            } catch (Exception e) {
                app.setName(aInfo.packageName);
            }


            //app.setIcon(pMgr.getApplicationIcon(aInfo));

            // check if this application is allowed
            if (Arrays.binarySearch(tordApps, app.getUsername()) >= 0) {
                app.setTorified(true);
            } else {
                app.setTorified(false);
            }

            appIdx++;
        }

        Collections.sort(apps);

        return apps;
    }

    public static void sortAppsForTorifiedAndAbc(List<TorifiedApp> apps) {
        Collections.sort(apps, (o1, o2) -> {
            /* Some apps start with lowercase letters and without the sorting being case
               insensitive they'd appear at the end of the grid of apps, a position where users
               would likely not expect to find them.
             */
            if (o1.isTorified() == o2.isTorified())
                return o1.getName().compareToIgnoreCase(o2.getName());
            if (o1.isTorified()) return -1;
            return 1;
        });
    }

    public boolean usesInternet() {
        return usesInternet;
    }

    public void setUsesInternet(boolean usesInternet) {
        this.usesInternet = usesInternet;
    }

    /**
     * @return the torified
     */
    public boolean isTorified() {
        return torified;
    }

    /**
     * @param torified the torified to set
     */
    public void setTorified(boolean torified) {
        this.torified = torified;
    }

    /**
     * @return the enabledPorts
     */
    public int[] getEnabledPorts() {
        return enabledPorts;
    }

    /**
     * @param enabledPorts the enabledPorts to set
     */
    public void setEnabledPorts(int[] enabledPorts) {
        this.enabledPorts = enabledPorts;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the uid
     */
    public int getUid() {
        return uid;
    }

    /**
     * @param uid the uid to set
     */
    public void setUid(int uid) {
        this.uid = uid;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the procname
     */
    public String getProcname() {
        return procname;
    }

    /**
     * @param procname the procname to set
     */
    public void setProcname(String procname) {
        this.procname = procname;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    @Override
    public int compareTo(Object another) {
        return this.toString().compareToIgnoreCase(another.toString());
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
