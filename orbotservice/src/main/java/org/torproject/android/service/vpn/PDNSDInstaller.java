package org.torproject.android.service.vpn;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.R;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.CustomNativeLoader;
import org.torproject.android.service.util.NativeLoader;

public class PDNSDInstaller implements TorServiceConstants {

    private final static String LIB_NAME = "pdnsd";
    private final static String LIB_SO_NAME = "pdnsd.so";

    private final static String TAG = "TorNativeLoader";

    private File installFolder;
    private Context context;
    private File filePdnsd;

     public PDNSDInstaller (Context context, File installFolder)
    {
        this.installFolder = installFolder;
        this.context = context;
    }

    //
    /*
     * Extract the Tor resources from the APK file using ZIP
     *
     * @File path to the Tor executable
     */
    public File installResources () throws IOException, TimeoutException
    {

        filePdnsd = new File(installFolder, LIB_NAME);

        if (!installFolder.exists())
            installFolder.mkdirs();

        File fileNativeDir = new File(getNativeLibraryDir(context));
        filePdnsd = new File(fileNativeDir,LIB_NAME + ".so");

        if (filePdnsd.exists())
        {
            if (filePdnsd.canExecute())
                return filePdnsd;
            else
            {
                setExecutable(filePdnsd);

                if (filePdnsd.canExecute())
                    return filePdnsd;
            }
        }

        if (filePdnsd.exists()) {
            InputStream is = new FileInputStream(filePdnsd);
            streamToFile(is, filePdnsd, false, true);
            setExecutable(filePdnsd);

            if (filePdnsd.exists() && filePdnsd.canExecute())
                return filePdnsd;
        }

        //let's try another approach
        filePdnsd = new File(installFolder, LIB_NAME);
        //fileTor = NativeLoader.initNativeLibs(context,fileTor);
        CustomNativeLoader.initNativeLibs(context,filePdnsd);

        setExecutable(filePdnsd);

        if (filePdnsd != null && filePdnsd.exists() && filePdnsd.canExecute())
            return filePdnsd;

        return null;
    }


    // Return Full path to the directory where native JNI libraries are stored.
    private static String getNativeLibraryDir(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        return appInfo.nativeLibraryDir;
    }






    /*
     * Reads file from assetPath/assetKey writes it to the install folder
     */
    private File assetToFile(String assetPath, String assetKey, boolean isZipped, boolean isExecutable) throws IOException {
        InputStream is = context.getAssets().open(assetPath);
        File outFile = new File(installFolder, assetKey);
        streamToFile(is, outFile, false, isZipped);
        if (isExecutable) {
            setExecutable(outFile);
        }
        return outFile;
    }


    /*
     * Write the inputstream contents to the file
     */
    private static boolean streamToFile(InputStream stm, File outFile, boolean append, boolean zip) throws IOException

    {
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;

        OutputStream stmOut = new FileOutputStream(outFile.getAbsolutePath(), append);
        ZipInputStream zis = null;

        if (zip)
        {
            zis = new ZipInputStream(stm);
            ZipEntry ze = zis.getNextEntry();
            stm = zis;

        }

        while ((bytecount = stm.read(buffer)) > 0)
        {

            stmOut.write(buffer, 0, bytecount);

        }

        stmOut.close();
        stm.close();

        if (zis != null)
            zis.close();


        return true;

    }



    private void setExecutable(File fileBin) {
        fileBin.setReadable(true);
        fileBin.setExecutable(true);
        fileBin.setWritable(false);
        fileBin.setWritable(true, true);
    }

    private static File[] listf(String directoryName) {

        // .............list file
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();

        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    Log.d(TAG,file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    listf(file.getAbsolutePath());
                }
            }

        return fList;
    }


}
