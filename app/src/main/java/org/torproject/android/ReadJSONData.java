package org.torproject.android;

import com.loopj.android.http.*;
import org.json.*;

import java.io.InputStream;
import java.net.URL;


import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.HttpClient;
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

   public void HTTPClient(){
        HttpGet httpGet = new HttpGet("");
        HttpClient  client = new DefaultHttpClient();
        //auth header
        httpGet.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials("userName", "password"), "UTF-8", false));

        //header types to transfer JSON
       httpGet.setHeader("Content-Type", "application/json");


   }


}


