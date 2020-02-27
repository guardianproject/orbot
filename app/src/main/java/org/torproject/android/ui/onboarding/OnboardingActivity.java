package org.torproject.android.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.View;
import com.github.paolorotolo.appintro.AppIntro;
import org.torproject.android.R;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.settings.LocaleHelper;
import org.torproject.android.ui.AppManagerActivity;
import org.torproject.android.ui.VPNEnableActivity;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;

import java.util.HashMap;
import java.util.LinkedList;

public class OnboardingActivity extends AppIntro {
    CustomSlideBigText welcome, intro2, cs2, cs3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            welcome = (CustomSlideBigText) getSupportFragmentManager().getFragment(savedInstanceState, "welcome");
            intro2 = (CustomSlideBigText) getSupportFragmentManager().getFragment(savedInstanceState, "intro2");
            cs2 = (CustomSlideBigText) getSupportFragmentManager().getFragment(savedInstanceState, "cs2");
            if (PermissionManager.isLollipopOrHigher())
                cs3 = (CustomSlideBigText) getSupportFragmentManager().getFragment(savedInstanceState, "cs3");

        }else{
            // Instead of fragments, you can also use our default slide
            // Just set a title, description, background and image. AppIntro will do the rest.
            welcome = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
            welcome.setTitle(getString(R.string.hello));
            welcome.setSubTitle(getString(R.string.welcome));
            addSlide(welcome);

            intro2 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
            intro2.setTitle(getString(R.string.browser_the_internet));
            intro2.setSubTitle(getString(R.string.no_tracking));
            addSlide(intro2);

            cs2 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
            cs2.setTitle(getString(R.string.bridges_sometimes));
            cs2.showButton(getString(R.string.action_more), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(OnboardingActivity.this,BridgeWizardActivity.class));
                }
            });
            addSlide(cs2);

            if (PermissionManager.isLollipopOrHigher()) {

                cs3 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
                cs3.setTitle(getString(R.string.vpn_setup));
                cs3.setSubTitle(getString(R.string.vpn_setup_sub));
                cs3.showButton(getString(R.string.action_vpn_choose), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(OnboardingActivity.this, VPNEnableActivity.class));
                        startActivityForResult(new Intent(OnboardingActivity.this, AppManagerActivity.class), 9999);

                    }
                });
                addSlide(cs3);

            }
        }


        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(getResources().getColor(R.color.dark_purple));
        setSeparatorColor(getResources().getColor(R.color.panel_background_main));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Setting first time app open flag "connect_firest_time" to false
        SharedPreferences.Editor pEdit = Prefs.getSharedPrefs(getApplicationContext()).edit();
        pEdit.putBoolean("connect_first_time", false);
        pEdit.apply();

        finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        int count = 0;
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            count++;
        }

        //Should check if the fragment exists in the fragment manager or else it'll flag error
        if(count >= 1)
            getSupportFragmentManager().putFragment(outState, "welcome", welcome);
        if(count >= 2)
            getSupportFragmentManager().putFragment(outState, "intro2", intro2);
        if(count >=3)
            getSupportFragmentManager().putFragment(outState, "cs2", cs2);
        if (count >= 4 && PermissionManager.isLollipopOrHigher())
            getSupportFragmentManager().putFragment(outState, "cs3", cs3);
    }
}