package org.torproject.android.share;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.simpleframework.http.Path;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class ShareService implements Container {

	private final static String TAG = "OrbotShare";
	
   public static class Task implements Runnable {
  
      private final Response response;
      private final Request request;
 
      public Task(Request request, Response response) {
         this.response = response;
         this.request = request;
      }

      public void run() {
         try {
        	
        	Path path = request.getPath();
        	String rPath = path.toString();
        	if (rPath.length() > 0)
        		rPath = rPath.substring(1);
        	
        	ShareItem sItem = sShareItems.get(rPath);
        	
        	if (sItem != null)
        	{
        	
	            long time = System.currentTimeMillis();
	            
	            response.setValue("Server", "OrbotShare/1.0 (Orbot 0.0.12-alpha)");
	            response.setDate("Date", time);
	            response.setDate("Last-Modified", time);
	            response.setValue("Content-Type", sItem.mContentType);
	            

				ContentResolver cr = sContext.getContentResolver(); 
				InputStream is = cr.openInputStream(sItem.mUriData);
				
				AssetFileDescriptor fileDesc = cr.openAssetFileDescriptor(sItem.mUriData, "r");
				fileDesc.getLength();
				
				response.setValue("Content-Length", fileDesc.getLength()+"");
				
				String fileName = rPath;
				
				String scheme = sItem.mUriData.getScheme();
				if (scheme.equals("file")) {
				    fileName = sItem.mUriData.getLastPathSegment();
				}
				else if (scheme.equals("content")) {
				    String[] proj = { MediaStore.Images.Media.TITLE };
				    Cursor cursor = cr.query(sItem.mUriData, proj, null, null, null);
				    if (cursor != null && cursor.getCount() != 0) {
				        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
				        cursor.moveToFirst();
				        fileName = cursor.getString(columnIndex);
				    }
				}
				
				//force file to be downloaded
				response.setValue("Content-Disposition","attachment; filename=" + fileName);
				
				BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
				
				byte[] buffer = new byte[1024];
				int len = -1;
				while ( (len = is.read(buffer)) != -1)
				{
					bos.write(buffer, 0, len);
				}
				
				bos.flush();
				bos.close();
        	}
        	else
        	{
        		response.setCode(404);
        		response.close();
        	}
        	
         } catch(Exception e) {
            Log.e(TAG,"error handling request",e);
         }
      }
   } 

   public ShareService(int size, Context context) {
      this.executor = Executors.newFixedThreadPool(size);
      sContext = context;
   }

   public void handle(Request request, Response response) {
      Task task = new Task(request, response);
      
      executor.execute(task);
   }

   
   public void startService (int port) throws Exception {
	   
      mServer = new ContainerServer(this);
      mConnection = new SocketConnection(mServer);
      SocketAddress address = new InetSocketAddress(port);

      mConnection.connect(address);
   }
   
   public void stopService () throws Exception {
	   mConnection.close();
   }
   
   public synchronized String addShare (ShareItem sItem)
   {
	   if (sShareItems == null)
	   {
		   sShareItems = new HashMap<String,ShareItem>();
		   
	   }
	   
	   String guid = java.util.UUID.randomUUID().toString().substring(0,6);//short guid
	   
	   sShareItems.put (guid, sItem);
	   
	   return guid;
   }
   
   private final Executor executor;

   private Server mServer;
   private Connection mConnection;
   
   private static HashMap<String, ShareItem> sShareItems;
   private static Context sContext;
}