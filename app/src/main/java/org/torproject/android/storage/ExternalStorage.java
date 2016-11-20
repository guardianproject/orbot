package org.torproject.android.storage;

import android.os.Environment;

import java.io.File;

public class ExternalStorage {
    private final String BACKUPS_DIR = "Orbot-HiddenServices";

    public String createBackupDir() {
        if (!isExternalStorageWritable()) {
            return null;
        }

        File path = Environment.getExternalStoragePublicDirectory(BACKUPS_DIR);

        if (!path.mkdirs()) {
            return null;
        }

        return path.getAbsolutePath();
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
