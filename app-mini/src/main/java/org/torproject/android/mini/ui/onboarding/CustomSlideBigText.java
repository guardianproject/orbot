package org.torproject.android.mini.ui.onboarding;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.torproject.android.mini.R;


public class CustomSlideBigText extends Fragment {

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;
    private String mTitle;
    private String mButtonText;
    private String mSubTitle;
    private View.OnClickListener mButtonListener;

    public static CustomSlideBigText newInstance(int layoutResId) {
        CustomSlideBigText sampleSlide = new CustomSlideBigText();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        sampleSlide.setArguments(args);

        return sampleSlide;
    }

    public void setTitle (String title)
    {
        mTitle = title;
    }

    public void setSubTitle(String subTitle) { mSubTitle = subTitle; }

    public void showButton (String buttonText, View.OnClickListener buttonListener)
    {
        mButtonText = buttonText;
        mButtonListener = buttonListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layoutResId, container, false);
        ((TextView)view.findViewById(R.id.custom_slide_big_text)).setText(mTitle);

        if (!TextUtils.isEmpty(mSubTitle)) {

            TextView tv =
                    (TextView)view.findViewById(R.id.custom_slide_big_text_sub);
            tv.setText(mSubTitle);
            tv.setVisibility(View.VISIBLE);
        }

        if (mButtonText != null)
        {
            Button button = (Button)view.findViewById(R.id.custom_slide_button);
            button.setVisibility(View.VISIBLE);
            button.setText(mButtonText);
            button.setOnClickListener(mButtonListener);
        }
        return view;

    }
}
