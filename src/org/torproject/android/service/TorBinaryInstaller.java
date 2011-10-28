/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.torproject.android.R;
import org.torproject.android.TorConstants;

import android.content.Context;
import android.util.Log;

public class TorBinaryInstaller implements TorServiceConstants {

	
	File installFolder;
	Context context;
	
	public TorBinaryInstaller (Context context, File installFolder)
	{
		this.installFolder = installFolder;
		
		this.context = context;
	}
	
	//		
	/*
	 * Extract the Tor binary from the APK file using ZIP
	 */
	public boolean installFromRaw () throws IOException
	{
		
		InputStream is;
		
		is = context.getResources().openRawResource(R.raw.tor);			
		streamToFile(is,installFolder, TOR_BINARY_ASSET_KEY, false, true);
			
		is = context.getResources().openRawResource(R.raw.torrc);			
		streamToFile(is,installFolder, TORRC_ASSET_KEY, false, false);

		is = context.getResources().openRawResource(R.raw.privoxy);			
		streamToFile(is,installFolder, PRIVOXY_ASSET_KEY, false, false);

		is = context.getResources().openRawResource(R.raw.privoxy_config);			
		streamToFile(is,installFolder, PRIVOXYCONFIG_ASSET_KEY, false, false);
		
		is = context.getResources().openRawResource(R.raw.geoip);			
		streamToFile(is,installFolder, GEOIP_ASSET_KEY, false, true);
	
		return true;
	}
	

	/*
	 * Write the inputstream contents to the file
	 */
    private static boolean streamToFile(InputStream stm, File folder, String targetFilename, boolean append, boolean zip) throws IOException

    {
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;

        File outFile = new File(folder, targetFilename);

    	OutputStream stmOut = new FileOutputStream(outFile, append);
    	
    	if (zip)
    	{
    		ZipInputStream zis = new ZipInputStream(stm);    		
    		ZipEntry ze = zis.getNextEntry();
    		stm = zis;
    		
    	}
    	
        while ((bytecount = stm.read(buffer)) > 0)

        {

            stmOut.write(buffer, 0, bytecount);

        }

        stmOut.close();

        
        return true;

    }
	
    //copy the file from inputstream to File output - alternative impl
	public void copyFile (InputStream is, File outputFile)
	{
		
		try {
			outputFile.createNewFile();
			DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFile));
			DataInputStream in = new DataInputStream(is);
			
			int b = -1;
			byte[] data = new byte[1024];
			
			while ((b = in.read(data)) != -1) {
				out.write(data);
			}
			
			if (b == -1); //rejoice
			
			//
			out.flush();
			out.close();
			in.close();
			// chmod?
			
			
			
		} catch (IOException ex) {
			Log.e(TorConstants.TAG, "error copying binary", ex);
		}

	}
	
	

}
