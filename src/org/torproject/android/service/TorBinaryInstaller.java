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

import org.torproject.android.R;

import android.content.Context;
import android.util.Log;

public class TorBinaryInstaller implements TorServiceConstants {

	
	String installPath;
	String apkPath;
	Context context;
	
	public TorBinaryInstaller (Context context, String installPath, String apkPath)
	{
		this.installPath = installPath;
		this.apkPath = apkPath;
		this.context = context;
	}
	
	/*
	 * Start the binary installation if the file doesn't exist or is forced
	 */
	public void start (boolean force)
	{
		
		boolean torBinaryExists = new File(installPath + TOR_BINARY_ASSET_KEY).exists();
		Log.d(TAG,"Tor binary exists=" + torBinaryExists);
		
		boolean privoxyBinaryExists = new File(installPath + PRIVOXY_ASSET_KEY).exists();
		Log.d(TAG,"Privoxy binary exists=" + privoxyBinaryExists);
	
		if (!(torBinaryExists && privoxyBinaryExists) || force)
			installFromRaw ();
		
	
		
	}
	
	//		
	/*
	 * Extract the Tor binary from the APK file using ZIP
	 */
	private void installFromRaw () 
	{
		
			
			InputStream is = context.getResources().openRawResource(R.raw.toraa);			
			streamToFile(is,installPath + TOR_BINARY_ASSET_KEY, false);
		
			is = context.getResources().openRawResource(R.raw.torab);			
			streamToFile(is,installPath + TOR_BINARY_ASSET_KEY, true);
		
			is = context.getResources().openRawResource(R.raw.torac);			
			streamToFile(is,installPath + TOR_BINARY_ASSET_KEY, true);
		
			is = context.getResources().openRawResource(R.raw.torad);			
			streamToFile(is,installPath + TOR_BINARY_ASSET_KEY, true);
		
			is = context.getResources().openRawResource(R.raw.torrc);			
			streamToFile(is,installPath + TORRC_ASSET_KEY, false);

			is = context.getResources().openRawResource(R.raw.privoxy);			
			streamToFile(is,installPath + PRIVOXY_ASSET_KEY, false);

			is = context.getResources().openRawResource(R.raw.privoxy_config);			
			streamToFile(is,installPath + PRIVOXYCONFIG_ASSET_KEY, false);

			
			
			Log.d(TAG,"SUCCESS: installed tor, privoxy binaries from raw");
	
		
	}
	
	/*
	private void installFromZip ()
	{
		
		try
		{
			
			ZipFile zip = new ZipFile(apkPath);
	
			ZipEntry zipen = zip.getEntry(ASSETS_BASE + TOR_BINARY_ASSET_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + TOR_BINARY_ASSET_KEY, false);
			
			zipen = zip.getEntry(ASSETS_BASE + TORRC_ASSET_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + TORRC_ASSET_KEY);
			
			zipen = zip.getEntry(ASSETS_BASE + PRIVOXY_ASSET_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + PRIVOXY_ASSET_KEY);
			
			zipen = zip.getEntry(ASSETS_BASE + PRIVOXYCONFIG_ASSET_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + PRIVOXYCONFIG_ASSET_KEY);
			
			zipen = zip.getEntry(ASSETS_BASE + PRIVOXYCONFIG_ASSET_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + PRIVOXYCONFIG_ASSET_KEY);
			
			
			zip.close();
			
			Log.d(TAG,"SUCCESS: unzipped tor, privoxy, iptables binaries from apk");
	
		}
		catch (IOException ioe)
		{
			Log.d(TAG,"FAIL: unable to unzip binaries from apk",ioe);
		
		}
	}
	*/
	
	/*
	 * Write the inputstream contents to the file
	 */
    private static void streamToFile(InputStream stm, String targetFilename, boolean append)

    {

        FileOutputStream stmOut = null;

        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;

       
        File outFile = new File(targetFilename);
        
        try {
           if (!append)
        	   outFile.createNewFile();

        	stmOut = new FileOutputStream(outFile);
        }

        catch (java.io.IOException e)

        {

        	Log.d(TAG,"Error opening output file " + targetFilename,e);

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

            Log.d(TAG,"Error writing output file '" + targetFilename + "': " + e.toString());

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
