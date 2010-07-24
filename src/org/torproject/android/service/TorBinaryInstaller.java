/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.util.Log;

public class TorBinaryInstaller implements TorServiceConstants {

	
	String installPath = null;
	String apkPath = null;
	
	public TorBinaryInstaller (String installPath, String apkPath)
	{
		this.installPath = installPath;
		this.apkPath = apkPath;
	}
	
	/*
	 * Start the binary installation if the file doesn't exist or is forced
	 */
	public void start (boolean force)
	{
		
		boolean torBinaryExists = new File(installPath + TOR_BINARY_ASSET_KEY).exists();
		Log.i(TAG,"Tor binary exists=" + torBinaryExists);
		
		boolean privoxyBinaryExists = new File(installPath + PRIVOXY_ASSET_KEY).exists();
		Log.i(TAG,"Privoxy binary exists=" + privoxyBinaryExists);
		
		if (!(torBinaryExists && privoxyBinaryExists) || force)
			installFromZip ();
		
	}
	
	/*
	 * Extract the Tor binary from the APK file using ZIP
	 */
	private void installFromZip ()
	{
		
		try
		{
			/*
			String apkPath = APK_PATH;
			
			int apkIdx = 1;
			
			while (!new File(apkPath).exists())
			{
				apkPath = APK_PATH_BASE + '-' + (apkIdx++) + ".apk";
				
				Log.i(TAG,"Could not find APK. Trying new path: " + apkPath);
			}
			*/
			
			
			
			ZipFile zip = new ZipFile(apkPath);
	
			ZipEntry zipen = zip.getEntry(TOR_BINARY_ZIP_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + TOR_BINARY_ASSET_KEY);
			
			zipen = zip.getEntry(TORRC_ZIP_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + TORRC_ASSET_KEY);
			
			zipen = zip.getEntry(PRIVOXY_ZIP_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + PRIVOXY_ASSET_KEY);
			
			zipen = zip.getEntry(PRIVOXYCONFIG_ZIP_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + PRIVOXYCONFIG_ASSET_KEY);
			
			
			zip.close();
			
			Log.i(TAG,"SUCCESS: unzipped tor, privoxy binaries from apk");
	
		}
		catch (IOException ioe)
		{
			Log.i(TAG,"FAIL: unable to unzip binaries from apk",ioe);
		
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

        	Log.i(TAG,"Error opening output file " + targetFilename,e);

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

            Log.i(TAG,"Error writing output file '" + targetFilename + "': " + e.toString());

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
			Log.e(TAG, "error copying binary", ex);
		}

	}
	
	

}
