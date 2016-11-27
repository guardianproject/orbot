package org.torproject.android.ui.hiddenservices.backup;

import android.content.Context;
import android.widget.Toast;

import org.torproject.android.service.R;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.ui.hiddenservices.storage.ExternalStorage;

import java.io.File;
import java.io.IOException;

public class BackupUtils {
    private File mHSBasePath;
    private Context mContext;

    public BackupUtils(Context context) {
        mContext = context;
        mHSBasePath = new File(
                mContext.getFilesDir().getAbsolutePath(),
                TorServiceConstants.HIDDEN_SERVICES_DIR
        );
    }

    public String createZipBackup(Integer port) {

        File storage_path = ExternalStorage.getOrCreateBackupDir();

        if (storage_path == null)
            return null;

        String zip_path = storage_path.getAbsolutePath() + "/hs" + port + ".zip";
        String files[] = {
                mHSBasePath + "/hs" + port + "/hostname",
                mHSBasePath + "/hs" + port + "/private_key"
        };

        ZipIt zip = new ZipIt(files, zip_path);

        if (!zip.zip()) {
            return null;
        }

        return zip_path;
    }

    public void restoreZipBackup(Integer port, String path) {
        try {
            File hsPath = new File(mHSBasePath.getCanonicalPath(), "/hs" + port);
            if (hsPath.mkdirs()) {
                ZipIt zip = new ZipIt(null, path);
                zip.unzip(hsPath.getCanonicalPath());
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
    }
}
