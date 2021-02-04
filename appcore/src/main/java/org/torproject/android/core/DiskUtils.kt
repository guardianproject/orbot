package org.torproject.android.core

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

object DiskUtils {

    @JvmStatic
    fun supportsStorageAccessFramework() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    @JvmStatic
    @Throws(IOException::class)
    fun readFileFromAssets(assetFilename: String, context: Context): String {
        val reader = BufferedReader(InputStreamReader(context.assets.open(assetFilename)))
        val sb = StringBuilder()
        var mLine = reader.readLine()
        while (mLine != null) {
            sb.append(mLine).append('\n') // process line
            mLine = reader.readLine()
        }
        reader.close()
        return sb.toString()
    }

    @JvmStatic
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun createWriteFileIntent(filename: String, mimeType: String): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
        putExtra(Intent.EXTRA_TITLE, filename)
    }

    @JvmStatic
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun createReadFileIntent(mimeType: String): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
    }

    @JvmStatic
    fun readFileFromInputStream(contentResolver: ContentResolver, file: Uri): String {
        val text = StringBuilder()
        val input = contentResolver.openInputStream(file)
        val reader = BufferedReader(input!!.reader())
        reader.use { reader ->
            var line = reader.readLine()
            while (line != null) {
                text.append(line)
                line = reader.readLine()
            }
        }
        return text.toString()
    }

    @JvmStatic
    fun readFile(contentResolver: ContentResolver, file: File): String = readFileFromInputStream(contentResolver, Uri.fromFile(file))

    @JvmStatic
    fun getOrCreateLegacyBackupDir(directoryName: String): File? {
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) return null
        val dir = File(Environment.getExternalStorageDirectory(), directoryName)
        return if (!dir.isDirectory && !dir.mkdirs()) null else dir
    }

    @JvmStatic
    fun recursivelyDeleteDirectory(directory: File): Boolean {
        val contents = directory.listFiles()
        contents?.forEach { recursivelyDeleteDirectory(it) }
        return directory.delete()
    }

}