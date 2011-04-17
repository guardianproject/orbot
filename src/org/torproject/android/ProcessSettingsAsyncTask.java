package org.torproject.android;

import org.torproject.android.service.ITorService;

import android.os.AsyncTask;
import android.os.RemoteException;

public class ProcessSettingsAsyncTask extends AsyncTask<ITorService, Integer, Long>
{
	

	@Override
	protected Long doInBackground(ITorService... torService) {

		try {
			torService[0].processSettings();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		return 100L;
	}
	
	 protected void onProgressUpdate(Integer... progress) {
         
     }

     protected void onPostExecute(Long result) {
       
     }

}
