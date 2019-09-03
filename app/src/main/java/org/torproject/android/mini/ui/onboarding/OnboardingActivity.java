package org.torproject.android.mini.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.github.paolorotolo.appintro.AppIntro;

import org.torproject.android.R;
import org.torproject.android.mini.settings.LocaleHelper;
import org.torproject.android.mini.ui.AppManagerActivity;
import org.torproject.android.mini.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.mini.vpn.VPNEnableActivity;

public class OnboardingActivity extends AppIntro {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        CustomSlideBigText welcome = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        welcome.setTitle(getString(R.string.hello));
        welcome.setSubTitle(getString(R.string.welcome));
        addSlide(welcome);

        CustomSlideBigText intro2 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        intro2.setTitle(getString(R.string.browser_the_internet));
        intro2.setSubTitle(getString(R.string.no_tracking));
        addSlide(intro2);

        CustomSlideBigText cs2 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        cs2.setTitle(getString(R.string.bridges_sometimes));
        cs2.showButton(getString(R.string.action_more), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OnboardingActivity.this,BridgeWizardActivity.class));
            }
        });
        addSlide(cs2);

        if (PermissionManager.isLollipopOrHigher()) {

            CustomSlideBigText cs3 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
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
        // Do something when users tap on Done button.
        finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}