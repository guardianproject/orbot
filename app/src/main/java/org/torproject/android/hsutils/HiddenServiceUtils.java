package org.torproject.android.hsutils;

import android.app.Application;
import android.content.Context;

import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.storage.ExternalStorage;
import org.torproject.android.zip.ZipIt;

import java.io.File;

public class HiddenServiceUtils {
    private static File appCacheHome;

    public HiddenServiceUtils(Context context) {
        appCacheHome = context.getDir(TorServiceConstants.DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);
    }

    public String createOnionBackup(Integer port) {

        ExternalStorage storage = new ExternalStorage();
        String storage_path = storage.createBackupDir();
        if (storage_path == null) {
            return null;
        }

        String zip_path = storage_path + "/hs" + port + ".zip";
        String files[] = {
                appCacheHome + "/hs" + port + "/hostname",
                appCacheHome + "/hs" + port + "/private_key"
        };

        ZipIt zip = new ZipIt(files, zip_path);

        if (!zip.zip()) {
            return null;
        }

        return zip_path;
    }

    public void restoreOnionBackup(Integer port, String path) {
        ZipIt zip = new ZipIt(null, path);
        zip.unzip(appCacheHome + "/hs" + port);
    }
}
