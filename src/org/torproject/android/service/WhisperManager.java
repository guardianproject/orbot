package org.torproject.android.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.whispersys.providers.Netfilter;

/*
 * Supports interaction with private APIs provided by WhisperSystems SDK
 */
public class WhisperManager {

	
	Context context;
	
	
	public boolean isWhisperCore ()
	{
		boolean result = false;
	
		 Cursor cursor = null;
		 Uri uri       = Uri.withAppendedPath(Netfilter.Filter.CONTENT_URI, Netfilter.Filter.Chains.OUTPUT);
		
		 try {
		   cursor = context.getContentResolver().query(uri, null, null, null, null);
		   cursor.moveToFirst();
		   result = true;
		 }
		 catch (Exception e)
		 {
			 result = false;
		 }
	
		return result;
	}
		/*
		 * Usage
	The Netfilter provider allows you to query, update, insert, or delete rules from a chain of a table. Callers must provide a CONTENT_URI which specifies the chain of the table they would like to query or modify.
	
	To query and print the INPUT rules for the filter table, for instance, would look like this:
	*/
		
	public void query ()
	{
	 Cursor cursor = null;
	 Uri uri       = Uri.withAppendedPath(Netfilter.Filter.CONTENT_URI, Netfilter.Filter.Chains.OUTPUT);
	
	 try {
	   cursor = context.getContentResolver().query(uri, null, null, null, null);

	   while (cursor.moveToNext())
	     for (int i=0;i<cursor.getColumnCount();i++)
	       Log.w("TestApp", "Column: " + cursor.getColumnName(i) + " , value: " + cursor.getString(i));
	 } finally {
	  if (cursor != null)
	    cursor.close();
	 }
	}
	 
	/*
	To append a rule that dropped all UDP traffic to the end of the OUTPUT chain would look like this:
	*/
	
	public void append ()
	{
	 Uri uri              = Uri.withAppendedPath(Netfilter.Filter.CONTENT_URI, 
	                                             Netfilter.Filter.Chains.OUTPUT);
	 ContentValues values = new ContentValues();
	 values.put(Netfilter.Filter.TARGET, Netfilter.Targets.DROP);
	 values.put(Netfilter.Filter.PROTOCOL, Netfilter.Protocols.UDP);
	 
	 context.getContentResolver().insert(uri, values);
	}
	
	/*
	iptables rule indexes start at 1. To insert the same rule in the first position of the OUTPUT chain, rather than appending it to the end of the chain, would look like this:
	*/
	public void insert ()
	{
	 Uri uri              = Uri.withAppendedPath(Netfilter.Filter.CONTENT_URI, 
	                                             Netfilter.Filter.Chains.OUTPUT);
	 ContentValues values = new ContentValues();
	 values.put(Netfilter.Filter.TARGET, Netfilter.Targets.DROP);
	 values.put(Netfilter.Filter.PROTOCOL, Netfilter.Protocols.UDP);
	 values.put(Netfilter.Filter.ROW_ID, 1);
	 
	 context.getContentResolver().insert(uri, values);
	}
}

