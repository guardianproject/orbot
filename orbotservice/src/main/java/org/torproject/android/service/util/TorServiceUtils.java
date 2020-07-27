/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service.util;

import org.torproject.android.service.TorServiceConstants;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TorServiceUtils implements TorServiceConstants {

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
