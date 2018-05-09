package org.torproject.android.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.github.paolorotolo.appintro.AppIntro;

import org.torproject.android.R;
import org.torproject.android.settings.LocaleHelper;
import org.torproject.android.ui.AppManagerActivity;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;
import org.torproject.android.vpn.VPNEnableActivity;

import java.util.List;

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
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public static boolean isAppInstalled(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static Intent getInstallIntent(String packageName, Context context) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(MARKET_URI + packageName));

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);

        String foundPackageName = null;
        for (ResolveInfo r : resInfos) {
            Log.i("Install", "market: " + r.activityInfo.packageName);
            if (TextUtils.equals(r.activityInfo.packageName, FDROID_PACKAGE_NAME)
                    || TextUtils.equals(r.activityInfo.packageName, PLAY_PACKAGE_NAME)) {
                foundPackageName = r.activityInfo.packageName;
                break;
            }
        }

        if (foundPackageName == null) {
            intent.setData(Uri.parse(FDROID_APP_URI + packageName));
        } else {
            intent.setPackage(foundPackageName);
        }
        return intent;
    }

    final static String MARKET_URI = "market://details?id=";
    final static String FDROID_APP_URI = "https://f-droid.org/repository/browse/?fdid=";
    final static String PLAY_APP_URI = "https://play.google.com/store/apps/details?id=";
    final static String FDROID_URI = "https://f-droid.org/repository/browse/?fdfilter=info.guardianproject";
    final static String PLAY_URI = "https://play.google.com/store/apps/developer?id=The+Guardian+Project";

    private final static String FDROID_PACKAGE_NAME = "org.fdroid.fdroid";
    private final static String PLAY_PACKAGE_NAME = "com.android.vending";
}