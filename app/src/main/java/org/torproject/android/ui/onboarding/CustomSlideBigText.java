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

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;
    private String mTitle;
    private String mButtonText;
    private String mSubTitle;
    private View.OnClickListener mButtonListener;
    private TextView tv, title;
    private Button button;

    public static CustomSlideBigText newInstance(int layoutResId) {
        CustomSlideBigText sampleSlide = new CustomSlideBigText();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        sampleSlide.setArguments(args);

        return sampleSlide;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setSubTitle(String subTitle) {
        mSubTitle = subTitle;
    }

    public void showButton(String buttonText, View.OnClickListener buttonListener) {
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
        title = ((TextView) view.findViewById(R.id.custom_slide_big_text));
        title.setText(mTitle);
        tv = (TextView) view.findViewById(R.id.custom_slide_big_text_sub);
        if (!TextUtils.isEmpty(mSubTitle)) {

            tv.setText(mSubTitle);
            tv.setVisibility(View.VISIBLE);
        }

        if (mButtonText != null) {
            button = (Button) view.findViewById(R.id.custom_slide_button);
            button.setVisibility(View.VISIBLE);
            button.setText(mButtonText);
            button.setOnClickListener(mButtonListener);
        }
        return view;

    }

    //Restoring the data
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            title.setText(savedInstanceState.getString(getResources().getString(R.string.Title)));
            tv.setText(savedInstanceState.getString(getResources().getString(R.string.SubTitle)));
            if (mButtonText != null) {
                button.setText(savedInstanceState.getString(getResources().getString(R.string.ButtonText)));
            }

        }
    }

    //Saving the data
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(getResources().getString(R.string.Title), mTitle);
        outState.putString(getResources().getString(R.string.SubTitle), mSubTitle);
        if (mButtonText != null) {
            outState.putString(getResources().getString(R.string.ButtonText), mButtonText);
        }
    }

}
