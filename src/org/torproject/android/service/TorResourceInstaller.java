/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;
import org.torproject.android.R;
import org.torproject.android.TorConstants;

import android.content.Context;
import android.util.Log;

public class TorResourceInstaller implements TorServiceConstants {

	
	File installFolder;
	Context context;
	
	public TorResourceInstaller (Context context, File installFolder)
	{
		this.installFolder = installFolder;
		
		this.context = context;
	}
	
	//		
	/*
	 * Extract the Tor resources from the APK file using ZIP
	 */
	public boolean installResources () throws IOException, FileNotFoundException, TimeoutException
	{
		
		InputStream is;
        File outFile;
        
        if (!installFolder.exists())
        	installFolder.mkdirs();
        
        Shell shell = Shell.startShell(new ArrayList<String>(),installFolder.getCanonicalPath());
        
		is = context.getResources().openRawResource(R.raw.torrc);
		outFile = new File(installFolder, TORRC_ASSET_KEY);
		if (outFile.exists())
			shell.add(new SimpleCommand("rm " + outFile.getCanonicalPath())).waitForFinish();
		streamToFile(is,outFile, false, false);

		is = context.getResources().openRawResource(R.raw.torrctether);		
		outFile = new File(installFolder, TORRC_TETHER_KEY);
		if (outFile.exists())
			shell.add(new SimpleCommand("rm " + outFile.getCanonicalPath())).waitForFinish();
		streamToFile(is, outFile, false, false);

		is = context.getResources().openRawResource(R.raw.privoxy_config);
		outFile = new File(installFolder, PRIVOXYCONFIG_ASSET_KEY);
		if (outFile.exists())
			shell.add(new SimpleCommand("rm " + outFile.getCanonicalPath())).waitForFinish();
		streamToFile(is,outFile, false, false);
	
		is = context.getResources().openRawResource(R.raw.tor);
		outFile = new File(installFolder, TOR_ASSET_KEY);
		if (outFile.exists())
			shell.add(new SimpleCommand("rm " + outFile.getCanonicalPath())).waitForFinish();
		streamToFile(is,outFile, false, true);
	
		is = context.getResources().openRawResource(R.raw.privoxy);
		outFile = new File(installFolder, PRIVOXY_ASSET_KEY);
		if (outFile.exists())
			shell.add(new SimpleCommand("rm " + outFile.getCanonicalPath())).waitForFinish();
		streamToFile(is,outFile, false, true);
	
		is = context.getResources().openRawResource(R.raw.obfsproxy);
		outFile = new File(installFolder, OBFSPROXY_ASSET_KEY);
		if (outFile.exists())
			shell.add(new SimpleCommand("rm " + outFile.getCanonicalPath())).waitForFinish();
		streamToFile(is,outFile, false, true);
		
		is = context.getResources().openRawResource(R.raw.xtables);
		outFile = new File(installFolder, IPTABLES_ASSET_KEY);
		if (outFile.exists())
			shell.add(new SimpleCommand("rm " + outFile.getCanonicalPath())).waitForFinish();
		streamToFile(is,outFile, false, true);
	
		return true;
	}
	
	/*
	 * Extract the Tor binary from the APK file using ZIP
	 */
	
	public boolean installGeoIP () throws IOException, FileNotFoundException
	{
		
		InputStream is;
        File outFile;
        
		is = context.getResources().openRawResource(R.raw.geoip);
		outFile = new File(installFolder, GEOIP_ASSET_KEY);
		streamToFile(is, outFile, false, true);
		
		is = context.getResources().openRawResource(R.raw.geoip6);
		outFile = new File(installFolder, GEOIP6_ASSET_KEY);
		streamToFile(is, outFile, false, true);
	
		return true;
	}
	
	/*
	private static void copyAssetFile(Context ctx, String asset, File file) throws IOException, InterruptedException
	{
    	
		DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
		InputStream is = new GZIPInputStream(ctx.getAssets().open(asset));
		
		byte buf[] = new byte[8172];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
	}*/
	
	/*
	 * Write the inputstream contents to the file
	 */
    private static boolean streamToFile(InputStream stm, File outFile, boolean append, boolean zip) throws IOException

    {
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;


    	OutputStream stmOut = new FileOutputStream(outFile, append);
    	ZipInputStream zis = null;
    	
    	if (zip)
    	{
    		zis = new ZipInputStream(stm);    		
    		ZipEntry ze = zis.getNextEntry();
    		stm = zis;
    		
    	}
    	
        while ((bytecount = stm.read(buffer)) > 0)
        {

            stmOut.write(buffer, 0, bytecount);

        }

        stmOut.close();
        stm.close();
        
        if (zis != null)
        	zis.close();
        
        
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
	
	

   
	/**
	 * Copies a raw resource file, given its ID to the given location
	 * @param ctx context
	 * @param resid resource id
	 * @param file destination file
	 * @param mode file permissions (E.g.: "755")
	 * @throws IOException on error
	 * @throws InterruptedException when interrupted
	 */
	private static void copyRawFile(Context ctx, int resid, File file, String mode, boolean isZipd) throws IOException, InterruptedException
	{
		final String abspath = file.getAbsolutePath();
		// Write the iptables binary
		final FileOutputStream out = new FileOutputStream(file);
		InputStream is = ctx.getResources().openRawResource(resid);
		
		if (isZipd)
    	{
    		ZipInputStream zis = new ZipInputStream(is);    		
    		ZipEntry ze = zis.getNextEntry();
    		is = zis;
    	}
		
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
		// Change the permissions
		Runtime.getRuntime().exec("chmod "+mode+" "+abspath).waitFor();
	}
    /**
	 * Asserts that the binary files are installed in the bin directory.
	 * @param ctx context
     * @param showErrors indicates if errors should be alerted
	 * @return false if the binary files could not be installed
	 */
	/*
	public static boolean assertIpTablesBinaries(Context ctx, boolean showErrors) throws Exception {
		boolean changed = false;
		
		// Check iptables_g1
		File file = new File(ctx.getDir("bin",0), "iptables");
		copyRawFile(ctx, R.raw.iptables, file, CHMOD_EXEC, false);
				
		return true;
	}*/
	

}
