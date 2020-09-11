package org.torproject.android.core

import android.os.Environment
import java.io.File

object ExternalStorage {
    private const val ORBOT_BACKUPS_DIR = "Orbot"
    @JvmStatic
    fun getOrCreateBackupDir(): File? {
        // Checks if external storage is available for read and write
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) return null
        val dir = File(Environment.getExternalStorageDirectory(), ORBOT_BACKUPS_DIR)
        return if (!dir.isDirectory && !dir.mkdirs()) null else dir
    }
}