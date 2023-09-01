package org.torproject.android.ui

import android.app.Activity
import android.os.Bundle

// Creates a dummy activity to manage data clearance
class ManageSpaceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kills the activity on creation
        finish()
    }
}