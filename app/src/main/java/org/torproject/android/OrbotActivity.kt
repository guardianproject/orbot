package org.torproject.android

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import org.torproject.android.service.OrbotConstants

class OrbotActivity : AppCompatActivity(), OrbotConstants {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_orbot)
    }

}