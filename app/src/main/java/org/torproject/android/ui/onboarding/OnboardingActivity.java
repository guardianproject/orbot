package org.torproject.android.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;

import org.torproject.android.R;
import org.torproject.android.core.LocaleHelper;
import org.torproject.android.service.util.Prefs;
import org.torproject.android.ui.AppManagerActivity;
import org.torproject.android.ui.v3onionservice.PermissionManager;

public class OnboardingActivity extends AppIntro {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(CustomSlideBigText.newInstance(R.layout.custom_slide_big_text, getString(R.string.hello), getString(R.string.welcome)));
        addSlide(CustomSlideBigText.newInstance(R.layout.custom_slide_big_text, getString(R.string.browser_the_internet), getString(R.string.no_tracking)));

        CustomSlideBigText cs2 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text, getString(R.string.bridges_sometimes));
        cs2.showButton(getString(R.string.action_more), v -> startActivity(new Intent(OnboardingActivity.this, BridgeWizardActivity.class)));
        cs2.showButton(getString(R.string.action_more), v -> startActivity(new Intent(OnboardingActivity.this, BridgeWizardActivity.class)));
        addSlide(cs2);

        if (PermissionManager.isLollipopOrHigher()) {
            CustomSlideBigText cs3 = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text, getString(R.string.vpn_setup), getString(R.string.vpn_setup_sub));
            cs3.showButton(getString(R.string.action_vpn_choose), v -> startActivity(new Intent(OnboardingActivity.this, AppManagerActivity.class)));
            addSlide(cs3);
        }

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
        // Setting first time app open flag "connect_first_time" to false
        SharedPreferences.Editor pEdit = Prefs.getSharedPrefs(getApplicationContext()).edit();
        pEdit.putBoolean("connect_first_time", false);
        pEdit.apply();
        finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}