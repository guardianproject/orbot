package org.torproject.android.service.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import org.torproject.android.binary.NativeLoader;
import org.torproject.android.binary.TorServiceConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class CustomTorResourceInstaller implements TorServiceConstants {


    File installFolder;
    Context context;

    File fileTorrc;
    File fileTor;

    public CustomTorResourceInstaller (Context context, File installFolder)
    {
        this.installFolder = installFolder;
        this.context = context;
    }

    public File getTorrcFile ()
    {
        return fileTorrc;
    }

    public File getTorFile ()
    {
        return fileTor;
    }

    /**
     private void deleteDirectory(File file) {
     if( file.exists() ) {
     if (file.isDirectory()) {
     File[] files = file.listFiles();
     for(int i=0; i<files.length; i++) {
     if(files[i].isDirectory()) {
     deleteDirectory(files[i]);
     }
     else {
     files[i].delete();
     }
     }
     }

     file.delete();
     }
     }**/

    //
    /*
     * Extract the Tor resources from the APK file using ZIP
     *
     * @File path to the Tor executable
     */
    public File installResources () throws IOException, TimeoutException
    {

        fileTor = new File(installFolder, TOR_ASSET_KEY);

        if (!installFolder.exists())
            installFolder.mkdirs();

        installGeoIP();
        fileTorrc = assetToFile(COMMON_ASSET_KEY + TORRC_ASSET_KEY, TORRC_ASSET_KEY, false, false);

        File fileNativeDir = new File(getNativeLibraryDir(context));
        fileTor = new File(fileNativeDir,TOR_ASSET_KEY + ".so");

        if (fileTor.exists())
        {
            if (fileTor.canExecute())
                return fileTor;
            else
            {
                setExecutable(fileTor);

                if (fileTor.canExecute())
                    return fileTor;
            }
        }

        if (fileTor.exists()) {
            InputStream is = new FileInputStream(fileTor);
            streamToFile(is, fileTor, false, true);
            setExecutable(fileTor);

            if (fileTor.exists() && fileTor.canExecute())
                return fileTor;
        }

        //let's try another approach
        fileTor = new File(installFolder, TOR_ASSET_KEY);
        //fileTor = NativeLoader.initNativeLibs(context,fileTor);
        CustomNativeLoader.initNativeLibs(context,fileTor);

        setExecutable(fileTor);

        if (fileTor != null && fileTor.exists() && fileTor.canExecute())
            return fileTor;

        return null;
    }


    // Return Full path to the directory where native JNI libraries are stored.
    private static String getNativeLibraryDir(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        return appInfo.nativeLibraryDir;
    }


    public boolean updateTorConfigCustom (File fileTorRcCustom, String extraLines) throws IOException, FileNotFoundException, TimeoutException
    {
        if (fileTorRcCustom.exists())
        {
            fileTorRcCustom.delete();
            Log.d("torResources","deleting existing torrc.custom");
        }
        else
            fileTorRcCustom.createNewFile();

        FileOutputStream fos = new FileOutputStream(fileTorRcCustom, false);
        PrintStream ps = new PrintStream(fos);
        ps.print(extraLines);
        ps.close();

        return true;
    }

    /*
     * Extract the Tor binary from the APK file using ZIP
     */

    private boolean installGeoIP () throws IOException
    {

        assetToFile(COMMON_ASSET_KEY + GEOIP_ASSET_KEY, GEOIP_ASSET_KEY, false, false);

        assetToFile(COMMON_ASSET_KEY + GEOIP6_ASSET_KEY, GEOIP6_ASSET_KEY, false, false);

        return true;
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

