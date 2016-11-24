package org.torproject.android.storage;

import android.os.Environment;

import java.io.File;

public class ExternalStorage {
    private static final String BACKUPS_DIR = "Orbot-HiddenServices";

    public static File getOrCreateBackupDir() {
        if (!isExternalStorageWritable())
            return null;

        File dir = new File(Environment.getExternalStorageDirectory(), BACKUPS_DIR);

        if (!dir.isDirectory() && !dir.mkdirs())
            return null;

        return dir;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
