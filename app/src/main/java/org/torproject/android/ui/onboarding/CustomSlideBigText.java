package org.torproject.android.ui.onboarding;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.torproject.android.R;

public class CustomSlideBigText extends Fragment {

    private static final String BUNDLE_KEY_LAYOUT_RES_ID = "layoutResId";
    private static final String BUNDLE_KEY_TITLE = "Title";
    private static final String BUNDLE_KEY_SUBTITLE = "Subtitle";
    private int layoutResId;
    private String mTitle;
    private String mButtonText;
    private String mSubTitle;
    private View.OnClickListener mButtonListener;

    public static CustomSlideBigText newInstance(int layoutResId, String title) {
        return newInstance(layoutResId, title, null);
    }

    public static CustomSlideBigText newInstance(int layoutResId, String title, String subtitle) {
        CustomSlideBigText newSlide = new CustomSlideBigText();
        Bundle args = new Bundle();
        args.putInt(BUNDLE_KEY_LAYOUT_RES_ID, layoutResId);
        args.putString(BUNDLE_KEY_TITLE, title);
        if (subtitle != null) args.putString(BUNDLE_KEY_SUBTITLE, subtitle);
        newSlide.setArguments(args);
        return newSlide;
    }

    public void showButton(String buttonText, View.OnClickListener buttonListener) {
        mButtonText = buttonText;
        mButtonListener = buttonListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;
        layoutResId = args.getInt(BUNDLE_KEY_LAYOUT_RES_ID);
        mTitle = args.getString(BUNDLE_KEY_TITLE);
        mSubTitle = args.getString(BUNDLE_KEY_SUBTITLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layoutResId, container, false);
        TextView title = view.findViewById(R.id.custom_slide_big_text);
        title.setText(mTitle);
        if (!TextUtils.isEmpty(mSubTitle)) {
            TextView bigTextSub = view.findViewById(R.id.custom_slide_big_text_sub);
            bigTextSub.setText(mSubTitle);
            bigTextSub.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(mButtonText)) {
            Button button = view.findViewById(R.id.custom_slide_button);
            button.setVisibility(View.VISIBLE);
            button.setText(mButtonText);
            button.setOnClickListener(mButtonListener);
        }
        return view;
    }
}
