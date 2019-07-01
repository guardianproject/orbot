/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.TorServiceConstants;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TorServiceUtils implements TorServiceConstants {

	public static SharedPreferences getSharedPrefs (Context context) {
		return context.getSharedPreferences(OrbotConstants.PREF_TOR_SHARED_PREFS,0 | Context.MODE_MULTI_PROCESS);
	}

    public static boolean isPortOpen(final String ip, final int port, final int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } 

        catch(ConnectException ce){
            //ce.printStackTrace();
            return false;
        }

        catch (Exception ex) {
            //ex.printStackTrace();
            return false;
        }
    }
}
