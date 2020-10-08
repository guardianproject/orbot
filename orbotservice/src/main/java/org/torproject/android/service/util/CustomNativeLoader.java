package org.torproject.android.service.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CustomNativeLoader {

    private final static String TAG = "CNL";

    private static boolean loadFromZip(Context context, String libname, File destLocalFile, String arch) {


        ZipFile zipFile = null;
        InputStream stream = null;

        try {
            zipFile = new ZipFile(context.getApplicationInfo().sourceDir);
            ZipEntry entry = zipFile.getEntry("lib/" + arch + "/" + libname + ".so");
            if (entry == null) {
                entry = zipFile.getEntry("jni/" + arch + "/" + libname + ".so");
                if (entry == null)
                    throw new Exception("Unable to find file in apk:" + "lib/" + arch + "/" + libname);
            }

            //how we wrap this in another stream because the native .so is zipped itself
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

    public static File loadNativeBinary(Context context, String libname, File destLocalFile) {

        try {


            File fileNativeBin = new File(getNativeLibraryDir(context), libname + ".so");
            if (!fileNativeBin.exists())
                fileNativeBin = new File(getNativeLibraryDir(context), "lib" + libname + ".so");

            if (fileNativeBin.exists()) {
                if (fileNativeBin.canExecute())
                    return fileNativeBin;
                else {
                    setExecutable(fileNativeBin);

                    if (fileNativeBin.canExecute())
                        return fileNativeBin;
                }
            }

            String folder = Build.CPU_ABI;


            String javaArch = System.getProperty("os.arch");
            if (javaArch != null && javaArch.contains("686")) {
                folder = "x86";
            }

            if (loadFromZip(context, libname, destLocalFile, folder)) {
                return destLocalFile;
            }

        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }


        return null;
    }

    private static void setExecutable(File fileBin) {
        fileBin.setReadable(true);
        fileBin.setExecutable(true);
        fileBin.setWritable(false);
        fileBin.setWritable(true, true);
    }

    // Return Full path to the directory where native JNI libraries are stored.
    private static String getNativeLibraryDir(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        return appInfo.nativeLibraryDir;
    }

}

