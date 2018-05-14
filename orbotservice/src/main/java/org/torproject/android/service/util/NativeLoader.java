
package org.torproject.android.service.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class NativeLoader {

    private final static String TAG = "TorNativeLoader";

    private static boolean loadFromZip(Context context, String libName, File destLocalFile, String folder) {


        ZipFile zipFile = null;
        ZipInputStream stream = null;
        try {
            zipFile = new ZipFile(context.getApplicationInfo().sourceDir);
            ZipEntry entry = zipFile.getEntry("lib/" + folder + "/" + libName + ".so");
            if (entry == null) {
                throw new Exception("Unable to find file in apk:" + "lib/" + folder + "/" + libName);
            }
            //the zip file entry is also zipped itself!
            stream = new ZipInputStream(zipFile.getInputStream(entry));
            stream.getNextEntry();

            OutputStream out = new FileOutputStream(destLocalFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = stream.read(buf)) > 0) {
                Thread.yield();
                out.write(buf, 0, len);
            }
            out.close();

            if (Build.VERSION.SDK_INT >= 9) {
                destLocalFile.setReadable(true, false);
                destLocalFile.setExecutable(true, false);
                destLocalFile.setWritable(true);
            }


            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return false;
    }

    public static synchronized boolean initNativeLibs(Context context, String binaryName, File destLocalFile) {

        try {
            String folder = null;

            try {

                if (Build.CPU_ABI.equalsIgnoreCase("armeabi-v7a")) {
                    folder = "armeabi-v7a";
                } else if (Build.CPU_ABI.startsWith("armeabi")) {
                    folder = "armeabi";
                } else if (Build.CPU_ABI.equalsIgnoreCase("x86")) {
                    folder = "x86";
                } else if (Build.CPU_ABI.equalsIgnoreCase("mips")) {
                    folder = "mips";
                } else {
                    folder = "armeabi";
                    //FileLog.e("tmessages", "Unsupported arch: " + Build.CPU_ABI);
                }
            } catch (Exception e) {
                //  FileLog.e("tmessages", e);
                Log.e(TAG, e.getMessage());
                folder = "armeabi";
            }


            String javaArch = System.getProperty("os.arch");
            if (javaArch != null && javaArch.contains("686")) {
                folder = "x86";
            }

            if (loadFromZip(context, binaryName, destLocalFile, folder)) {
                return true;
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }
}
