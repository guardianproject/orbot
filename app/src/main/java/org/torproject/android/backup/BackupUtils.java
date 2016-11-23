package org.torproject.android.backup;

import android.app.Application;
import android.content.Context;

import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.storage.ExternalStorage;

import java.io.File;

public class BackupUtils {
    private File mHSBasePath;

    public BackupUtils(Context context) {
        mHSBasePath = context.getDir(
                TorServiceConstants.DIRECTORY_TOR_DATA,
                Application.MODE_PRIVATE
        );
    }

    public String createZipBackup(Integer port) {

        ExternalStorage storage = new ExternalStorage();
        String storage_path = storage.createBackupDir();
        if (storage_path == null) {
            return null;
        }

        String zip_path = storage_path + "/hs" + port + ".zip";
        String files[] = {
                mHSBasePath + "/" + TorServiceConstants.HIDDEN_SERVICES_DIR + "/hs" + port + "/hostname",
                mHSBasePath + "/" + TorServiceConstants.HIDDEN_SERVICES_DIR + "/hs" + port + "/private_key"
        };

        ZipIt zip = new ZipIt(files, zip_path);

        if (!zip.zip()) {
            return null;
        }

        return zip_path;
    }

    public void restoreZipBackup(Integer port, String path) {
        ZipIt zip = new ZipIt(null, path);
        zip.unzip(mHSBasePath + "/hs" + port);
    }
}
