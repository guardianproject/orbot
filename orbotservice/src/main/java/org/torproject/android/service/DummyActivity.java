package org.torproject.android.service;

import android.app.Activity;
import android.os.Bundle;

/*
 * To combat background service being stopped/swiped
 */
public class DummyActivity extends Activity {
	@Override
	public void onCreate( Bundle icicle ) {
		super.onCreate( icicle );
		finish();
	}
}