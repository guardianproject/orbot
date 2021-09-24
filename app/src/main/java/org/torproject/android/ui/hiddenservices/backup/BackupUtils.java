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
import org.torproject.android.service.OrbotService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.ui.hiddenservices.providers.CookieContentProvider;
import org.torproject.android.ui.hiddenservices.providers.HSContentProvider;
import org.torproject.android.ui.v3onionservice.OnionServiceContentProvider;
import org.torproject.android.ui.v3onionservice.clientauth.ClientAuthContentProvider;

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
    private final Context mContext;
    private final ContentResolver mResolver;

    public BackupUtils(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
    }

    public String createV3ZipBackup(String port, String relativePath, Uri zipFile) {
        String[] files = createFilesForZippingV3(relativePath);
        ZipUtilities zip = new ZipUtilities(files, zipFile, mResolver);
        if (!zip.zip()) return null;
        return zipFile.getPath();
    }

    public String createV3AuthBackup(String domain, String keyHash, Uri backupFile) {
        String fileText = OrbotService.buildV3ClientAuthFile(domain, keyHash);
        try {
            ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(backupFile, "w");
            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
            fos.write(fileText.getBytes());
            fos.close();
            pfd.close();
        } catch (IOException ioe) {
            return null;
        }
        return backupFile.getPath();
    }

    public String createV2ZipBackup(int port, String relativePath, Uri zipFile) {
        String[] files = createFilesForZippingV2(relativePath);
        ZipUtilities zip = new ZipUtilities(files, zipFile, mResolver);

        if (!zip.zip())
            return null;

        return zipFile.getPath();
    }

    // todo this doesn't export data for onions that orbot hosts which have authentication (not supported yet...)
    private String[] createFilesForZippingV3(String relativePath) {
        final String v3BasePath = getV3BasePath() + "/" + relativePath + "/";
        final String hostnamePath = v3BasePath + "hostname",
                configFilePath = v3BasePath + configFileName,
                privKeyPath = v3BasePath + "hs_ed25519_secret_key",
                pubKeyPath = v3BasePath + "hs_ed25519_public_key";

        Cursor portData = mResolver.query(OnionServiceContentProvider.CONTENT_URI, OnionServiceContentProvider.PROJECTION,
                OnionServiceContentProvider.OnionService.PATH + "=\"" + relativePath + "\"", null, null);

        JSONObject config = new JSONObject();
        try {
            if (portData == null || portData.getCount() != 1)
                return null;
            portData.moveToNext();


            config.put(OnionServiceContentProvider.OnionService.NAME, portData.getString(portData.getColumnIndex(OnionServiceContentProvider.OnionService.NAME)));
            config.put(OnionServiceContentProvider.OnionService.PORT, portData.getString(portData.getColumnIndex(OnionServiceContentProvider.OnionService.PORT)));
            config.put(OnionServiceContentProvider.OnionService.ONION_PORT, portData.getString(portData.getColumnIndex(OnionServiceContentProvider.OnionService.ONION_PORT)));
            config.put(OnionServiceContentProvider.OnionService.DOMAIN, portData.getString(portData.getColumnIndex(OnionServiceContentProvider.OnionService.DOMAIN)));
            config.put(OnionServiceContentProvider.OnionService.CREATED_BY_USER, portData.getString(portData.getColumnIndex(OnionServiceContentProvider.OnionService.CREATED_BY_USER)));
            config.put(OnionServiceContentProvider.OnionService.ENABLED, portData.getString(portData.getColumnIndex(OnionServiceContentProvider.OnionService.ENABLED)));

            portData.close();

            FileWriter fileWriter = new FileWriter(configFilePath);
            fileWriter.write(config.toString());
            fileWriter.close();
        } catch (JSONException | IOException ioe) {
            ioe.printStackTrace();
            return null;
        }

        return new String[]{hostnamePath, configFilePath, privKeyPath, pubKeyPath};
    }

    private String[] createFilesForZippingV2(String relativePath) {
        final String hsBasePath = getHSBasePath() + "/" + relativePath + "/";
        String configFilePath = hsBasePath + configFileName;
        String hostnameFilePath = hsBasePath + "hostname";
        String keyFilePath = hsBasePath + "private_key";

        Cursor portData = mResolver.query(
                HSContentProvider.CONTENT_URI,
                HSContentProvider.PROJECTION,
                HSContentProvider.HiddenService.PATH + "=\"" + relativePath + "\"",
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
        } catch (JSONException | NullPointerException e) {
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

    private void extractConfigFromUnzippedBackupV3(String backupName) {
        File v3BasePath = getV3BasePath();
        String v3Dir = backupName.substring(0, backupName.lastIndexOf('.'));
        String configFilePath = v3BasePath + "/" + v3Dir + "/" + configFileName;
        File v3Path = new File(v3BasePath.getAbsolutePath(), v3Dir);
        if (!v3Path.isDirectory()) v3Path.mkdirs();

        File configFile = new File(configFilePath);
        try {
            FileInputStream fis = new FileInputStream(configFile);
            FileChannel fc = fis.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            String jsonString = Charset.defaultCharset().decode(bb).toString();
            JSONObject savedValues = new JSONObject(jsonString);
            ContentValues fields = new ContentValues();

            int port = savedValues.getInt(OnionServiceContentProvider.OnionService.PORT);
            fields.put(OnionServiceContentProvider.OnionService.PORT, port);
            fields.put(OnionServiceContentProvider.OnionService.NAME, savedValues.getString(OnionServiceContentProvider.OnionService.NAME));
            fields.put(OnionServiceContentProvider.OnionService.ONION_PORT, savedValues.getInt(OnionServiceContentProvider.OnionService.ONION_PORT));
            fields.put(OnionServiceContentProvider.OnionService.DOMAIN, savedValues.getString(OnionServiceContentProvider.OnionService.DOMAIN));
            fields.put(OnionServiceContentProvider.OnionService.CREATED_BY_USER, savedValues.getInt(OnionServiceContentProvider.OnionService.CREATED_BY_USER));
            fields.put(OnionServiceContentProvider.OnionService.ENABLED, savedValues.getInt(OnionServiceContentProvider.OnionService.ENABLED));

            Cursor dbService = mResolver.query(OnionServiceContentProvider.CONTENT_URI, OnionServiceContentProvider.PROJECTION,
                    OnionServiceContentProvider.OnionService.PORT + "=" + port, null, null);
            if (dbService == null || dbService.getCount() == 0)
                mResolver.insert(OnionServiceContentProvider.CONTENT_URI, fields);
            else
                mResolver.update(OnionServiceContentProvider.CONTENT_URI, fields, OnionServiceContentProvider.OnionService.PORT + "=" + port, null);
            dbService.close();

            configFile.delete();
            if (v3Path.renameTo(new File(v3BasePath, "/v3" + port))) {
                Toast.makeText(mContext, R.string.backup_restored, Toast.LENGTH_LONG).show();
            } else {
                // collision, clean up files
                for (File file : v3Path.listFiles())
                    file.delete();
                v3Path.delete();
                Toast.makeText(mContext, mContext.getString(R.string.backup_port_exist, port), Toast.LENGTH_LONG).show();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
        }
    }

    private void extractConfigFromUnzippedBackupV2(String backupName) {
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

    private File getV3BasePath() {
        return new File(mContext.getFilesDir().getAbsolutePath(), TorServiceConstants.ONION_SERVICES_DIR);
    }

    public void restoreZipBackupV2Legacy(File zipFile) {
        String backupName = zipFile.getName();
        ZipUtilities zip = new ZipUtilities(null, null, mResolver);
        String hsDir = backupName.substring(0, backupName.lastIndexOf('.'));
        File hsPath = new File(getHSBasePath().getAbsolutePath(), hsDir);
        if (zip.unzipLegacy(hsPath.getAbsolutePath(), zipFile))
            extractConfigFromUnzippedBackupV2(backupName);
        else
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
    }

    public void restoreZipBackupV3Legacy(File zipFile) {
        String backupName = zipFile.getName();
        ZipUtilities zip = new ZipUtilities(null, null, mResolver);
        String v3Dir = backupName.substring(0, backupName.lastIndexOf('.'));
        File v3Path = new File(getV3BasePath().getAbsolutePath(), v3Dir);
        if (zip.unzipLegacy(v3Path.getAbsolutePath(), zipFile))
            extractConfigFromUnzippedBackupV3(backupName);
        else
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
    }


    public void restoreZipBackupV2(Uri zipUri) {
        Cursor returnCursor = mResolver.query(zipUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String backupName = returnCursor.getString(nameIndex);
        returnCursor.close();

        String hsDir = backupName.substring(0, backupName.lastIndexOf('.'));
        File hsPath = new File(getHSBasePath().getAbsolutePath(), hsDir);
        if (new ZipUtilities(null, zipUri, mResolver).unzip(hsPath.getAbsolutePath()))
            extractConfigFromUnzippedBackupV2(backupName);
        else
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
    }

    public void restoreZipBackupV3(Uri zipUri) {
        Cursor returnCursor = mResolver.query(zipUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String backupName = returnCursor.getString(nameIndex);
        returnCursor.close();

        String v3Dir = backupName.substring(0, backupName.lastIndexOf('.'));
        File v3Path = new File(getV3BasePath().getAbsolutePath(), v3Dir);
        if (new ZipUtilities(null, zipUri, mResolver).unzip(v3Path.getAbsolutePath()))
            extractConfigFromUnzippedBackupV3(backupName);
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


    public void restoreClientAuthBackup(String authFileContents) {
        ContentValues fields = new ContentValues();
        String[] split = authFileContents.split(":");
        if (split.length != 4) {
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
            return;
        }
        fields.put(ClientAuthContentProvider.V3ClientAuth.DOMAIN, split[0]);
        fields.put(ClientAuthContentProvider.V3ClientAuth.HASH, split[3]);
        mResolver.insert(ClientAuthContentProvider.CONTENT_URI, fields);
        Toast.makeText(mContext, R.string.backup_restored, Toast.LENGTH_LONG).show();
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
