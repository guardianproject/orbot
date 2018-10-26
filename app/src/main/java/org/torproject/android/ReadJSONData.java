package org.torproject.android;

import org.json.*;

import java.nio.charset.Charset;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.auth.AuthenticationException;
import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.auth.BasicScheme;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;


public class ReadJSONData {
    public void writeJSONObject(){
       JSONObject object = new JSONObject();
       try {
           String dirAuthority = object.getJSONObject("DirAuthority").getString("");
           String socksPort = object.getJSONObject("SOCKSPORT").getString("");
           String useBridges = object.getJSONObject("SOCKSPORT").getString("");
       }
       catch (JSONException e){
           e.printStackTrace();
       }
   }

   public void basicAuth(){
       StringBuilder builder = new StringBuilder();
       HttpClient client = new DefaultHttpClient();
       HttpGet httpGet = new HttpGet("YOUR WEBSITE HERE");
        String userName = "";
        String password = "";
       UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);
       Header basicAuthHeader = null;
       try {
           basicAuthHeader = new BasicScheme(Charset.forName("UTF-8")).authenticate(credentials, httpGet, null);
       } catch (AuthenticationException e) {
           e.printStackTrace();
       }
       httpGet.addHeader(basicAuthHeader);   }


}


