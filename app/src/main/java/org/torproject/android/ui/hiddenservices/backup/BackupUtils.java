package org.torproject.android.ui.hiddenservices.backup;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.torproject.android.R;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;

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
    private static final String configFileName = "config.json";
    private Context mContext;
    private ContentResolver mResolver;

    public BackupUtils(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
    }

    public String createZipBackup(int port, Uri zipFile) {
        String[] files = createFilesForZipping(port);
        ZipIt zip = new ZipIt(files, zipFile, mResolver);

        if (!zip.zip())
            return null;

        return zipFile.getPath();
    }

    // returns true if domain is v2 onion address or v3 onion address
    public static boolean isOnionAddressValid(String onionToTest) {
        if (onionToTest.matches("([a-z0-9]{16}).onion")) return true;
        return onionToTest.matches("([a-z0-9]{56}).onion");
    }

    private String[] createFilesForZipping(int port) {
        File hsBasePath = getHSBasePath();
        String configFilePath = hsBasePath + "/hs" + port + "/" + configFileName;
        String hostnameFilePath = hsBasePath + "/hs" + port + "/hostname";
        String keyFilePath = hsBasePath + "/hs" + port + "/private_key";

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

            config.put(HSContentProvider.HiddenService.NAME, portData.getString(portData.getColumnIndex(HSContentProvider.HiddenService.NAME)));
            config.put(HSContentProvider.HiddenService.PORT, portData.getInt(portData.getColumnIndex(HSContentProvider.HiddenService.PORT)));
            config.put(HSContentProvider.HiddenService.ONION_PORT, portData.getInt(portData.getColumnIndex(HSContentProvider.HiddenService.ONION_PORT)));
            config.put(HSContentProvider.HiddenService.DOMAIN, portData.getString(portData.getColumnIndex(HSContentProvider.HiddenService.DOMAIN)));
            config.put(HSContentProvider.HiddenService.AUTH_COOKIE, portData.getInt(portData.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE)));
            config.put(HSContentProvider.HiddenService.AUTH_COOKIE_VALUE, portData.getString(portData.getColumnIndex(HSContentProvider.HiddenService.AUTH_COOKIE_VALUE)));
            config.put(HSContentProvider.HiddenService.CREATED_BY_USER, portData.getInt(portData.getColumnIndex(HSContentProvider.HiddenService.CREATED_BY_USER)));
            config.put(HSContentProvider.HiddenService.ENABLED, portData.getInt(portData.getColumnIndex(HSContentProvider.HiddenService.ENABLED)));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        portData.close();

        try {
            FileWriter file = new FileWriter(configFilePath);
            file.write(config.toString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new String[]{hostnameFilePath, keyFilePath, configFilePath};
    }

    private void extractConfigFromUnzippedBackup(String backupName) {
        File mHSBasePath = getHSBasePath();
        int port;
        String hsDir = backupName.substring(0, backupName.lastIndexOf('.'));
        String configFilePath = mHSBasePath + "/" + hsDir + "/" + configFileName;
        String jString;

        File hsPath = new File(mHSBasePath.getAbsolutePath(), hsDir);
        if (!hsPath.isDirectory())
            hsPath.mkdirs();

        File config = new File(configFilePath);
        FileInputStream stream;

        try {
            stream = new FileInputStream(config);
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jString = Charset.defaultCharset().decode(bb).toString();
            stream.close();

            JSONObject savedValues = new JSONObject(jString);
            ContentValues fields = new ContentValues();

            fields.put(HSContentProvider.HiddenService.NAME, savedValues.getString(HSContentProvider.HiddenService.NAME));
            fields.put(HSContentProvider.HiddenService.ONION_PORT, savedValues.getInt(HSContentProvider.HiddenService.ONION_PORT));
            fields.put(HSContentProvider.HiddenService.DOMAIN, savedValues.getString(HSContentProvider.HiddenService.DOMAIN));
            fields.put(HSContentProvider.HiddenService.AUTH_COOKIE, savedValues.getInt(HSContentProvider.HiddenService.AUTH_COOKIE));
            fields.put(HSContentProvider.HiddenService.CREATED_BY_USER, savedValues.getInt(HSContentProvider.HiddenService.CREATED_BY_USER));
            fields.put(HSContentProvider.HiddenService.ENABLED, savedValues.getInt(HSContentProvider.HiddenService.ENABLED));

            port = savedValues.getInt(HSContentProvider.HiddenService.PORT);
            fields.put(HSContentProvider.HiddenService.PORT, port);

            Cursor service = mResolver.query(
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
            Toast.makeText(mContext, R.string.backup_restored, Toast.LENGTH_LONG).show();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
        }
    }

    private File getHSBasePath() {
        return new File(mContext.getFilesDir().getAbsolutePath(), TorServiceConstants.HIDDEN_SERVICES_DIR);
    }

    public void restoreZipBackupLegacy(File zipFile) {
        String backupName = zipFile.getName();
        ZipIt zip = new ZipIt(null, null, mResolver);
        String hsDir = backupName.substring(0, backupName.lastIndexOf('.'));
        File hsPath = new File(getHSBasePath().getAbsolutePath(), hsDir);
        if (zip.unzipLegacy(hsPath.getAbsolutePath(), zipFile))
            extractConfigFromUnzippedBackup(backupName);
        else
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
    }

    public void restoreZipBackup(Uri zipUri) {
        Cursor returnCursor = mResolver.query(zipUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String backupName = returnCursor.getString(nameIndex);
        returnCursor.close();

        String hsDir = backupName.substring(0, backupName.lastIndexOf('.'));
        File hsPath = new File(getHSBasePath().getAbsolutePath(), hsDir);
        if (new ZipIt(null, zipUri, mResolver).unzip(hsPath.getAbsolutePath()))
            extractConfigFromUnzippedBackup(backupName);
        else
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();

    }

    public void restoreKeyBackup(int hsPort, Uri hsKeyPath) {
        File mHSBasePath = new File(
                mContext.getFilesDir().getAbsolutePath(),
                TorServiceConstants.HIDDEN_SERVICES_DIR
        );

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

    public void restoreCookieBackup(String jString) {
        try {
            JSONObject savedValues = new JSONObject(jString);
            ContentValues fields = new ContentValues();

            fields.put(CookieContentProvider.ClientCookie.DOMAIN, savedValues.getString(CookieContentProvider.ClientCookie.DOMAIN));
            fields.put(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE, savedValues.getString(CookieContentProvider.ClientCookie.AUTH_COOKIE_VALUE));
            fields.put(CookieContentProvider.ClientCookie.ENABLED, savedValues.getInt(CookieContentProvider.ClientCookie.ENABLED));

            mResolver.insert(CookieContentProvider.CONTENT_URI, fields);
            Toast.makeText(mContext, R.string.backup_restored, Toast.LENGTH_LONG).show();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
        }
    }
}
