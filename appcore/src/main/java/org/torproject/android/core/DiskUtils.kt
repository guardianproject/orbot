package org.torproject.android.core

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import java.io.BufferedReader
import java.io.IOException
import java.lang.StringBuilder

object DiskUtils {
    @JvmStatic
    fun createWriteFileIntent(filename: String, mimeType: String): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
        putExtra(Intent.EXTRA_TITLE, filename)
    }

    @JvmStatic
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

}