/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */


package org.torproject.android.service.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public static String readString(InputStream stream) {
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
        int flagOffset = 0x1F1E6;
        int asciiOffset = 0x41;
        int firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset;
        int secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    /*
     * Zips a file at a location and places the resulting zip file at the toLocation
     * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
     */
    public static void zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        try {
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                zipSubFolder(out, sourceFile, sourceFile.getParent().length());
            } else {
                byte[] data = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                entry.setTime(sourceFile.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void zipSubFolder(ZipOutputStream out, File folder,
                                     int basePathLength) throws IOException {
        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte[] data = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                entry.setTime(file.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    /*
     * gets the last path component
     *
     * Example: getLastPathComponent("downloads/example/fileToZip");
     * Result: "fileToZip"
     */
    public static String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        return segments[segments.length - 1];
    }
}
