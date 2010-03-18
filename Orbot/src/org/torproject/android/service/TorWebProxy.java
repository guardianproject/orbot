package org.torproject.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.torproject.android.Utils;

import android.util.Log;

public class TorWebProxy  extends NanoHTTPD implements TorServiceConstants
{
	private final static String BASE_URI = "http://localhost:8888/";
	
	private final static String DEFAULT_URI = "https://check.torproject.org/";
	private java.net.URLDecoder decoder;
	
	private String lastPage = null;
	
	private final static int PORT = 8888;
	
	public TorWebProxy() throws IOException
	{
		super(PORT);
		
		decoder = new java.net.URLDecoder();
		
		Log.i(TAG,"TorWebProxy started on port: " + PORT);
	}

	public Response serve( String uri, String method, Properties header, Properties params )
	{
		Log.i(TAG,"TorWebProxy serve(): " +  method + " '" + uri + "' " );
		
		
		InputStream contentStream = null;
		
		String requestUrl = null;
		
		if (uri.toLowerCase().startsWith("/http"))
		{
			//okay this is cool
			requestUrl = uri.substring(1);
			requestUrl = decoder.decode(requestUrl);
		}
		else if (params.getProperty("url")!=null)
		{
			requestUrl = params.getProperty("url");
		}
		else if (uri.equals("/"))
		{
			requestUrl = DEFAULT_URI;
		}
		else //must be a relative path
		{
			if (lastPage != null)
			{
				
				if (!uri.startsWith("/"))
					uri = "/" + uri;
				
				try {
					URL lastPageUrl = new URL(lastPage);
					
					StringBuilder sb = new StringBuilder();
					sb.append(lastPageUrl.getProtocol());
					sb.append("://");
					sb.append(lastPageUrl.getHost());
					
					if (lastPageUrl.getPort()!=-1)
					{
						sb.append(":");
						sb.append(lastPageUrl.getPort());
					}
					
					sb.append(uri);
					
					requestUrl = sb.toString();
					
				} catch (MalformedURLException e) {
					Log.i(TAG, "TorWebProxy: " + e.getLocalizedMessage(),e);
					
					return new NanoHTTPD.Response(NanoHTTPD.HTTP_INTERNALERROR,"text/plain","Something bad happened: " + e.getLocalizedMessage());

				}
				
				
			}
		}
		
		HttpUriRequest request = null;
		HttpHost host = null;
		

		URI rURI = null;
		try {
			rURI = new URI(requestUrl);
		} catch (URISyntaxException e) {
			Log.e(TAG,"error parsing uri: " + requestUrl,e);
			return new NanoHTTPD.Response(NanoHTTPD.HTTP_INTERNALERROR,"text/plain","error");

		}
		
		int port = rURI.getPort();
		
		if (port == -1)
		{
			if (rURI.getScheme().equalsIgnoreCase("http"))
				port = 80;
			else if (rURI.getScheme().equalsIgnoreCase("https"))
				port = 443;
		}
		
		host = new HttpHost(rURI.getHost(),port, rURI.getScheme());
		
		Log.i(TAG,"TorWebProxy server(): host=" + host.getSchemeName() + "://" + host.getHostName() + ":" + host.getPort());
		
		if (method.equalsIgnoreCase("get"))
		{
			Log.i(TAG,"TorWebProxy serve(): GET: " + rURI.getPath() );
			request = new HttpGet (rURI.getPath());
		}
		else if (method.equalsIgnoreCase("post"))
		{
			Log.i(TAG,"TorWebProxy serve(): POST: " + rURI.getPath() );

			request = new HttpPost(rURI.getPath());
			

			//request = new HttpPost (requestUrl);
			
			Iterator<Entry<Object,Object>> itSet = params.entrySet().iterator();
			
			Entry<Object,Object> entry = null;
			
			HttpParams hParams = request.getParams();
			
			while (itSet.hasNext())
			{
				entry = itSet.next();
				
				hParams.setParameter((String)entry.getKey(), entry.getValue());
			}
			
			request.setParams(hParams);
			
		}
		else
		{
			return new NanoHTTPD.Response(NanoHTTPD.HTTP_NOTIMPLEMENTED,"text/plain","No support for the method: " + method);

		}
		
	//	SOCKSHttpClient client = new SOCKSHttpClient();
		
		HttpHost proxy = new HttpHost("127.0.0.1", 8118, "http");
		SchemeRegistry supportedSchemes = new SchemeRegistry();
		// Register the "http" and "https" protocol schemes, they are
		// required by the default operator to look up socket factories.
		supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		// prepare parameters
		HttpParams hparams = new BasicHttpParams();
		HttpProtocolParams.setVersion(hparams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(hparams, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(hparams, true);
		ClientConnectionManager ccm = new ThreadSafeClientConnManager(hparams, supportedSchemes);

		DefaultHttpClient client = new DefaultHttpClient(ccm, hparams);
		client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

		
		try {
			HttpResponse response = client.execute(host, request);
			
			if (response.getEntity() == null || response.getEntity().getContentType() == null)
			{
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_INTERNALERROR,"text/plain","Something bad happened");

			}
			
			String contentType = response.getEntity().getContentType().getValue();
			
			int respCode = response.getStatusLine().getStatusCode();
			
			Log.i(TAG,"TorWebProxy server(): resp=" + respCode + ";" + contentType);
			
			contentStream = response.getEntity().getContent();
			
			if (contentType.indexOf("text/html")!=-1)
			{
				response.getEntity().getContentLength();
				
				lastPage = requestUrl;
				
				String page = Utils.readString(contentStream);
				
				page = page.replace("href=\"", "href=\"" + BASE_URI);
				page = page.replace("src=\"", "src=\"" + BASE_URI);
				page = page.replace("action=\"", "action=\"" + BASE_URI);
				
				page = page.replace("HREF=\"", "href=\"" + BASE_URI);
				page = page.replace("SRC=\"", "src=\"" + BASE_URI);
				page = page.replace("ACTION=\"", "action=\"" + BASE_URI);
				
				
				return new NanoHTTPD.Response( HTTP_OK, contentType, page );
			}
			else
				return new NanoHTTPD.Response( HTTP_OK, contentType, contentStream );
		
		} catch (ClientProtocolException e) {
			Log.w(TAG,"TorWebProxy",e);
			
		} catch (IOException e) {
			Log.w(TAG,"TorWebProxy",e);
			
		}
		catch (NullPointerException e)
		{
			Log.w(TAG,"TorWebProxy",e);
		}
		
		return new NanoHTTPD.Response(NanoHTTPD.HTTP_INTERNALERROR,"text/plain","Something bad happened");
		
	}


	
}