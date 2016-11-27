package org.torproject.android.ui.hiddenservices.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.service.R;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;
import org.torproject.android.ui.hiddenservices.storage.ExternalStorage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BackupUtils {
    private File mHSBasePath;
    private Context mContext;
    private ContentResolver mResolver;
    private final String configFileName = "config.json";

    public BackupUtils(Context context) {
        mContext = context;
        mHSBasePath = new File(
                mContext.getFilesDir().getAbsolutePath(),
                TorServiceConstants.HIDDEN_SERVICES_DIR
        );

        mResolver = mContext.getContentResolver();
    }

    public String createZipBackup(Integer port) {

        String configFilePath = mHSBasePath + "/hs" + port + "/" + configFileName;
        String hostnameFilePath = mHSBasePath + "/hs" + port + "/hostname";
        String keyFilePath = mHSBasePath + "/hs" + port + "/private_key";

        File storage_path = ExternalStorage.getOrCreateBackupDir();

        if (storage_path == null)
            return null;

        Cursor portData = mResolver.query(
                HSContentProvider.CONTENT_URI,
                HSContentProvider.PROJECTION,
                HSContentProvider.HiddenService.PORT + "=" + port,
                null,
                null
        );

        JSONObject config = new JSONObject();
        try {
            if (portData.getCount() != 1)
                return null;

            portData.moveToNext();

            config.put(
                    HSContentProvider.HiddenService.NAME,
                    portData.getString(portData.getColumnIndex(HSContentProvider.HiddenService.NAME))
            );

            config.put(
                    HSContentProvider.HiddenService.PORT,
                    portData.getInt(portData.getColumnIndex(HSContentProvider.HiddenService.PORT))
            );

            config.put(
                    HSContentProvider.HiddenService.ONION_PORT,
                    portData.getInt(portData.getColumnIndex(HSContentProvider.HiddenService.ONION_PORT))
            );

            config.put(
                    HSContentProvider.HiddenService.DOMAIN,
                    portData.getString(portData.getColumnIndex(HSContentProvider.HiddenService.DOMAIN))
            );

            config.put(
                    HSContentProvider.HiddenService.CREATED_BY_USER,
                    portData.getInt(portData.getColumnIndex(HSContentProvider.HiddenService.CREATED_BY_USER))
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        try {
            FileWriter file = new FileWriter(configFilePath);
            file.write(config.toString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        portData.close();

        String zip_path = storage_path.getAbsolutePath() + "/hs" + port + ".zip";
        String files[] = {hostnameFilePath, keyFilePath, configFilePath};

        ZipIt zip = new ZipIt(files, zip_path);

        if (!zip.zip())
            return null;

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
