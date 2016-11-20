package org.torproject.android.hsutils;

import android.content.Context;

public abstract class GrantedPermissionsAction {
    public abstract void run(Context context, boolean granted);
}
