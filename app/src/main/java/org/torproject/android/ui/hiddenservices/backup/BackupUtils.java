package org.torproject.android.ui.hiddenservices.backup;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;
import org.torproject.android.ui.hiddenservices.storage.ExternalStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class BackupUtils {
    private final String configFileName = "config.json";
    private File mHSBasePath;
    private Context mContext;
    private ContentResolver mResolver;

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
            if (portData == null || portData.getCount() != 1)
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

    public void restoreZipBackup(File backup) {

        int port;
        Cursor service;
        String backupName = backup.getName();
        String hsDir = backupName.substring(0, backupName.lastIndexOf('.'));
        String configFilePath = mHSBasePath + "/" + hsDir + "/" + configFileName;
        String jString = null;

        File hsPath = new File(mHSBasePath.getAbsolutePath(), hsDir);
        if (!hsPath.isDirectory())
            hsPath.mkdirs();

        ZipIt zip = new ZipIt(null, backup.getAbsolutePath());
        zip.unzip(hsPath.getAbsolutePath());

        File config = new File(configFilePath);
        FileInputStream stream = null;

        try {
            stream = new FileInputStream(config);
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jString = Charset.defaultCharset().decode(bb).toString();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (jString == null)
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();

        try {
            JSONObject savedValues = new JSONObject(jString);
            ContentValues fields = new ContentValues();

            fields.put(
                    HSContentProvider.HiddenService.NAME,
                    savedValues.getString(HSContentProvider.HiddenService.NAME)
            );

            fields.put(
                    HSContentProvider.HiddenService.ONION_PORT,
                    savedValues.getInt(HSContentProvider.HiddenService.ONION_PORT)
            );

            fields.put(
                    HSContentProvider.HiddenService.DOMAIN,
                    savedValues.getString(HSContentProvider.HiddenService.DOMAIN)
            );

            fields.put(
                    HSContentProvider.HiddenService.CREATED_BY_USER,
                    savedValues.getInt(HSContentProvider.HiddenService.CREATED_BY_USER)
            );

            port = savedValues.getInt(HSContentProvider.HiddenService.PORT);
            fields.put(HSContentProvider.HiddenService.PORT, port);

            service = mResolver.query(
                    HSContentProvider.CONTENT_URI,
                    HSContentProvider.PROJECTION,
                    HSContentProvider.HiddenService.PORT + "=" + port,
                    null,
                    null
            );

            if (service == null || service.getCount() == 0) {
                mResolver.insert(HSContentProvider.CONTENT_URI, fields);
            } else {
                mResolver.update(
                        HSContentProvider.CONTENT_URI,
                        fields,
                        HSContentProvider.HiddenService.PORT + "=" + port,
                        null
                );

                service.close();
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
        }

        Toast.makeText(mContext, R.string.backup_restored, Toast.LENGTH_LONG).show();
    }

    public void restoreKeyBackup(int hsPort, Uri hsKeyPath) {

        File serviceDir = new File(mHSBasePath, "hs" + hsPort);

        if (!serviceDir.isDirectory())
            serviceDir.mkdirs();

        try {
            ParcelFileDescriptor mInputPFD = mContext.getContentResolver().openFileDescriptor(hsKeyPath, "r");
            InputStream fileStream = new FileInputStream(mInputPFD.getFileDescriptor());
            OutputStream file = new FileOutputStream(serviceDir.getAbsolutePath() + "/private_key");

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileStream.read(buffer)) > 0) {
                file.write(buffer, 0, length);
            }
            file.close();

        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
