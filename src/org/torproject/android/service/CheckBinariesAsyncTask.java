package org.torproject.android.service;

import org.torproject.android.service.TorService;

import android.os.AsyncTask;
import android.os.RemoteException;

public class CheckBinariesAsyncTask extends AsyncTask<TorService, Integer, Long>
{
	

	@Override
	protected Long doInBackground(TorService... torService) {

		try {
			torService[0].checkTorBinaries(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return 100L;
	}
	
	 protected void onProgressUpdate(Integer... progress) {
         
     }

     protected void onPostExecute(Long result) {
       
     }

}
