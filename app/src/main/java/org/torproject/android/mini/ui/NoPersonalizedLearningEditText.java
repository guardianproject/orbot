package org.torproject.android.mini.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;

public class NoPersonalizedLearningEditText extends AppCompatEditText {
    public NoPersonalizedLearningEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImeOptions(getImeOptions() | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
    }
}
