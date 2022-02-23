package org.torproject.android.service.util;

import android.content.Context;

import org.torproject.android.service.OrbotConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipInputStream;

public class CustomTorResourceInstaller {

    private final File installFolder;
    private final Context context;

    public CustomTorResourceInstaller(Context context, File installFolder) {
        this.installFolder = installFolder;
        this.context = context;
    }

    /*
     * Write the inputstream contents to the file
     */
    private static void streamToFile(InputStream stm, File outFile, boolean append, boolean zip) throws IOException {
        byte[] buffer = new byte[1024];

        int bytecount;

        OutputStream stmOut = new FileOutputStream(outFile.getAbsolutePath(), append);
        ZipInputStream zis = null;

        if (zip) {
            zis = new ZipInputStream(stm);
            zis.getNextEntry();
            stm = zis;
        }

        while ((bytecount = stm.read(buffer)) > 0) {
            stmOut.write(buffer, 0, bytecount);
        }

        stmOut.close();
        stm.close();

        if (zis != null)
            zis.close();
    }

    /*
     * Extract the Tor resources from the APK file using ZIP
     */
    public void installGeoIP() throws IOException {
        if (!installFolder.exists())
            installFolder.mkdirs();
        assetToFile(OrbotConstants.GEOIP_ASSET_KEY, OrbotConstants.GEOIP_ASSET_KEY, false, false);
        assetToFile(OrbotConstants.GEOIP6_ASSET_KEY, OrbotConstants.GEOIP6_ASSET_KEY, false, false);
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

