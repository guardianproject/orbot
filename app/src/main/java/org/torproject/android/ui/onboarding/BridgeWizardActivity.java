package org.torproject.android.ui.onboarding;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.torproject.android.R;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;

public class BridgeWizardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridge_wizard);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.bridges));

        findViewById(R.id.btnBridgesDirect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("");
                Prefs.putBridgesEnabled(false);
            }
        });

        findViewById(R.id.btnBridgesObfs4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("obfs4");
                Prefs.putBridgesEnabled(true);
            }
        });


        findViewById(R.id.btnBridgesMeek).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBridgesList("meek");
                Prefs.putBridgesEnabled(true);
            }
        });


        findViewById(R.id.btnBridgesNew).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGetBridgePrompt("");
            }
        });
    }



    private void showGetBridgePrompt (final String type)
    {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.layout_diag, null);

        TextView versionName = (TextView)view.findViewById(R.id.diaglog);
        versionName.setText(R.string.you_must_get_a_bridge_address_by_email_web_or_from_a_friend_once_you_have_this_address_please_paste_it_into_the_bridges_preference_in_orbot_s_setting_and_restart_);

        new AlertDialog.Builder(this)
                .setTitle(R.string.bridge_mode)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, new Dialog.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setNeutralButton(R.string.get_bridges_email, new Dialog.OnClickListener ()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                        sendGetBridgeEmail(type);

                    }


                })
                .setPositiveButton(R.string.get_bridges_web, new Dialog.OnClickListener ()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        openBrowser(OrbotConstants.URL_TOR_BRIDGES + type,true, null);

                    }


                }).show();
    }

    private void sendGetBridgeEmail (String type)
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL  , new String[]{"bridges@torproject.org"});

        if (type != null)
        {
            intent.putExtra(Intent.EXTRA_SUBJECT, "get transport " + type);
            intent.putExtra(Intent.EXTRA_TEXT, "get transport " + type);

        }
        else
        {
            intent.putExtra(Intent.EXTRA_SUBJECT, "get bridges");
            intent.putExtra(Intent.EXTRA_TEXT, "get bridges");

        }

        startActivity(Intent.createChooser(intent, getString(R.string.send_email)));
    }


    /*
     * Launch the system activity for Uri viewing with the provided url
     */
    private void openBrowser(final String browserLaunchUrl,boolean forceExternal, String pkgId)
    {
        boolean isBrowserInstalled = appInstalledOrNot(TorServiceConstants.BROWSER_APP_USERNAME);

        if (pkgId != null)
        {
            if (pkgId.equals(TorServiceConstants.BROWSER_APP_USERNAME))
                startIntent(pkgId,Intent.ACTION_VIEW, Uri.parse(browserLaunchUrl));
            else
            {
                if (!Prefs.useVpn())
                {
                    Toast.makeText(this, R.string.please_enable_vpn, Toast.LENGTH_LONG).show();
                }
                else
                {
                    startIntent(pkgId,Intent.ACTION_VIEW,Uri.parse(browserLaunchUrl));
                }
            }
        }
        else if (isBrowserInstalled)
        {
            startIntent(TorServiceConstants.BROWSER_APP_USERNAME,Intent.ACTION_VIEW,Uri.parse(browserLaunchUrl));
        }
        else
        {
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(browserLaunchUrl)));
        }
    }


    private void startIntent (String pkg, String action, Uri data)
    {
        Intent i;
        PackageManager pm = getPackageManager();

        try {
            if (pkg != null) {
                i = pm.getLaunchIntentForPackage(pkg);
                if (i == null)
                    throw new PackageManager.NameNotFoundException();
            }
            else
            {
                i = new Intent();
            }

            i.setAction(action);
            i.setData(data);

            if (i.resolveActivity(pm)!=null)
                startActivity(i);

        } catch (PackageManager.NameNotFoundException e) {

        }
    }

    private boolean appInstalledOrNot(String uri)
    {
        PackageManager pm = getPackageManager();
        try
        {
            PackageInfo pi = pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return pi.applicationInfo.enabled;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

}
