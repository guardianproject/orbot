/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */


package org.torproject.android.service.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

public class Utils {
    public static boolean isPortOpen(final String ip, final int port, final int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } catch (Exception ex) {
            //ex.printStackTrace();
            return false;
        }
    }

    public static String readInputStreamAsString(InputStream stream) {
        String line;

        StringBuilder out = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append('\n');

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }

    public static String convertCountryCodeToFlagEmoji(String countryCode) {
        countryCode = countryCode.toUpperCase(Locale.getDefault());
        int flagOffset = 0x1F1E6;
        int asciiOffset = 0x41;
        int firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset;
        int secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }
}
