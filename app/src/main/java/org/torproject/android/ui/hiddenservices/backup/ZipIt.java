package org.torproject.android.ui.hiddenservices.backup;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipIt {
    private static final int BUFFER = 2048;

    private String[] files;
    private Uri zipFile;
    private ContentResolver contentResolver;

    public ZipIt(@Nullable String[] files, @NonNull Uri zipFile, @NonNull ContentResolver contentResolver) {
        this.files = files;
        this.zipFile = zipFile;
        this.contentResolver = contentResolver;
    }

    public boolean zip() {
        try {
            BufferedInputStream origin;
            ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(zipFile, "w");
            FileOutputStream dest = new FileOutputStream(pdf.getFileDescriptor());
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte[] data = new byte[BUFFER];
            for (String file : files) {
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            out.close();
            dest.close();
            pdf.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean unzip(String outputPath) {
        InputStream is;
        ZipInputStream zis;

        try {
            String filename;
            is = contentResolver.openInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();

                // Need to create directories if not exists, or it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(outputPath + "/" + filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(outputPath + "/" + filename);

                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}