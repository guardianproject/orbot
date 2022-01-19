package org.torproject.android.service.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import org.torproject.android.service.OrbotServiceConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CustomTorResourceInstaller {

    private static final String TAG = "CustomTorResourceInstaller";

    private File installFolder;
    private Context context;

    public CustomTorResourceInstaller(Context context, File installFolder) {
        this.installFolder = installFolder;
        this.context = context;
    }

    // Return Full path to the directory where native JNI libraries are stored.
    private static String getNativeLibraryDir(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        return appInfo.nativeLibraryDir;
    }

    /*
     * Write the inputstream contents to the file
     */
    private static boolean streamToFile(InputStream stm, File outFile, boolean append, boolean zip) throws IOException {
        byte[] buffer = new byte[1024];

        int bytecount;

        OutputStream stmOut = new FileOutputStream(outFile.getAbsolutePath(), append);
        ZipInputStream zis = null;

        if (zip) {
            zis = new ZipInputStream(stm);
            ZipEntry ze = zis.getNextEntry();
            stm = zis;

        }

        while ((bytecount = stm.read(buffer)) > 0) {

            stmOut.write(buffer, 0, bytecount);

        }

        stmOut.close();
        stm.close();

        if (zis != null)
            zis.close();


        return true;

    }




    /*
     * Extract the Tor binary from the APK file using ZIP
     */

    private static File[] listf(String directoryName) {

        // .............list file
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();

        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    Log.d(TAG, file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    listf(file.getAbsolutePath());
                }
            }

        return fList;
    }

    /*
     * Extract the Tor resources from the APK file using ZIP
     */
    public void installGeoIP() throws IOException {
        if (!installFolder.exists())
            installFolder.mkdirs();
        assetToFile(OrbotServiceConstants.GEOIP_ASSET_KEY, OrbotServiceConstants.GEOIP_ASSET_KEY, false, false);
        assetToFile(OrbotServiceConstants.GEOIP6_ASSET_KEY, OrbotServiceConstants.GEOIP6_ASSET_KEY, false, false);
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

    private void setExecutable(File fileBin) {
        fileBin.setReadable(true);
        fileBin.setExecutable(true);
        fileBin.setWritable(false);
        fileBin.setWritable(true, true);
    }
}

