package org.torproject.android.net;

import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class SOCKSHttpClient extends DefaultHttpClient {

	private final static String DEFAULT_HOST = "127.0.0.1";
	private final static int DEFAULT_PORT = 9050;
	
	private static ClientConnectionManager ccm = null;
	private static HttpParams params = null;
	
	public SOCKSHttpClient ()
	{
		
       super(initConnectionManager(), initParams());


	}
	
	private void setSystemProperties ()
	{
//       System.getProperties().put("socks.proxySet","true");
  //     System.getProperties().put("socks.proxyHost",DEFAULT_HOST);
	//	System.getProperties().put("socks.proxyPort", DEFAULT_PORT+"");

	}
	
	private static ClientConnectionManager initConnectionManager ()
	{
		if (ccm == null)
		{
		SchemeRegistry supportedSchemes = new SchemeRegistry();
		
		
		 supportedSchemes.register(new Scheme("http", 
	                SocksSocketFactory.getSocketFactory(), 80));
	    
		 supportedSchemes.register(new Scheme("https", 
	                ModSSLSocketFactory.getSocketFactory(), 443));
	
			ccm = new MyThreadSafeClientConnManager(initParams(), supportedSchemes);
		}
		
      return ccm;
	}
	
	private static HttpParams initParams ()
	{
	    if (params == null)
	    {
	      // prepare parameters
	      params = new BasicHttpParams();
	      HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	      HttpProtocolParams.setContentCharset(params, "UTF-8");
	      HttpProtocolParams.setUseExpectContinue(params, true);
	    }
	    
	    return params;
	}
}
