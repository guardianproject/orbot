package org.torproject.android.net;

import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;


public class MyThreadSafeClientConnManager extends ThreadSafeClientConnManager {

	public MyThreadSafeClientConnManager(HttpParams params, SchemeRegistry schreg) {
		super(params, schreg);
		
	}

	@Override
	protected ClientConnectionOperator createConnectionOperator(
			SchemeRegistry schreg) {
		return new MyDefaultClientConnectionOperator(schreg);
	}
}
