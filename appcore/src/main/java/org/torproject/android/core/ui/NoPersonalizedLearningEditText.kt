package org.torproject.android.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText

class NoPersonalizedLearningEditText(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {
    init {
        imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    }
}