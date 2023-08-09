package org.torproject.android.core.ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText

@RequiresApi(Build.VERSION_CODES.O)
class NoPersonalizedLearningEditText(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {
    init {
        imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    }
}