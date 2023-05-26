package org.torproject.android.core.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import org.torproject.android.service.util.Prefs

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resetSecureFlags()
    }

    override fun onResume() {
        super.onResume()
        resetSecureFlags()
    }

    open fun resetSecureFlags() {
        if (Prefs.isSecureWindow())
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    }
}