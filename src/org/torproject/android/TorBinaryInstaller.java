/* Copyright (c) 2009, Nathan Freitas, The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.util.Log;

public class TorBinaryInstaller implements TorConstants {

	private final static String LOG_TAG = "Tor";

	
	public TorBinaryInstaller ()
	{
	}
	
	/*
	 * Start the binary installation if the file doesn't exist or is forced
	 */
	public void start (boolean force)
	{
		boolean binaryExists = new File(TOR_BINARY_INSTALL_PATH).exists();
		
		Log.i(LOG_TAG,"Tor binary exists=" + binaryExists);
		
		if (!binaryExists || force)
			installFromZip ();
		
	}
	
	/*
	 * Extract the Tor binary from the APK file using ZIP
	 */
	private void installFromZip ()
	{
		
		try
		{
			ZipFile zip = new ZipFile(APK_PATH);
	
			ZipEntry zipen = zip.getEntry(TOR_BINARY_ZIP_KEY);
			streamToFile(zip.getInputStream(zipen),TOR_BINARY_INSTALL_PATH);
			
			zipen = zip.getEntry(TORRC_ZIP_KEY);
			streamToFile(zip.getInputStream(zipen),TORRC_INSTALL_PATH);
			
			zip.close();
	
		}
		catch (IOException ioe)
		{
			Log.i(LOG_TAG,"unable to unzip tor binary from apk",ioe);
		
		}
	}
	
	/*
	 * Write the inputstream contents to the file
	 */
    private static void streamToFile(InputStream stm, String targetFilename)

    {

        FileOutputStream stmOut = null;

        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;

       
        File outFile = new File(targetFilename);
        
        try {
            outFile.createNewFile();

        	stmOut = new FileOutputStream(outFile);
        }

        catch (java.io.IOException e)

        {

        	Log.i(LOG_TAG,"Error opening output file " + targetFilename,e);

        	return;
        }

       

        try

        {

            while ((bytecount = stm.read(buffer)) > 0)

            {

                stmOut.write(buffer, 0, bytecount);

            }

            stmOut.close();

        }

        catch (java.io.IOException e)

        {

            Log.i(LOG_TAG,"Error writing output file '" + targetFilename + "': " + e.toString());

            return;

        }

    }
	
    //copy the file from inputstream to File output - alternative impl
	public void copyFile (InputStream is, File outputFile)
	{
		
		try {
			outputFile.createNewFile();
			DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFile));
			DataInputStream in = new DataInputStream(is);
			
			int b;
			byte[] data = new byte[1024];
			
			while ((b = in.read(data)) != -1) {
				out.write(data);
			}
			//
			out.flush();
			out.close();
			in.close();
			// chmod?
			
			
			
		} catch (IOException ex) {
			Log.e(LOG_TAG, "error copying binary", ex);
		}

	}
	
	

}
