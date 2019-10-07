
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

public class NativeLoader {

    private final static String TAG = "TorNativeLoader";

    private static boolean loadFromZip(Context context, String libName, File destLocalFile, String folder) {


        ZipFile zipFile = null;
        InputStream stream = null;
        try {
            zipFile = new ZipFile(context.getApplicationInfo().sourceDir);

            /**
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                Log.d("Zip","entry: " + entry.getName());
            }
            **/

            ZipEntry entry = zipFile.getEntry("lib/" + folder + "/" + libName + ".so");
            if (entry == null) {
                entry = zipFile.getEntry("lib/" + folder + "/" + libName);
                if (entry == null)
                throw new Exception("Unable to find file in apk:" + "lib/" + folder + "/" + libName);
            }
            stream = zipFile.getInputStream(entry);

            OutputStream out = new FileOutputStream(destLocalFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = stream.read(buf)) > 0) {
                Thread.yield();
                out.write(buf, 0, len);
            }
            out.close();

            destLocalFile.setReadable(true, false);
            destLocalFile.setExecutable(true, false);
            destLocalFile.setWritable(true);

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
            String folder = Build.CPU_ABI;

            String javaArch = System.getProperty("os.arch");
            if (javaArch != null && javaArch.contains("686")) {
                folder = "x86";
            }

            return loadFromZip(context, binaryName, destLocalFile, folder);

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }
}
