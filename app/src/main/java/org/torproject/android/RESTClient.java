package org.torproject.android;

import android.app.Activity;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.auth.AuthenticationException;
import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.auth.BasicScheme;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.util.EntityUtils;

public class RESTClient{
    public  static void requestConnect() {
        try {
            URL url = new URL("http://127.0.0.1:8080/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            StringBuilder builder = new StringBuilder();
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet("http://127.0.0.1:8080/");
            String userName = "test";
            String password = "test";
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);
            Header basicAuthHeader = null;
            try {
                basicAuthHeader = new BasicScheme(Charset.forName("UTF-8")).authenticate(credentials, request, null);
            } catch (AuthenticationException e) {
                e.printStackTrace();
            }
            request.addHeader(basicAuthHeader);

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity =  response.getEntity();
                String data = EntityUtils.toString(entity);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //set login credentials
           // basicAuth();

            if (connection.getResponseCode() != 200){
                throw new RuntimeException("Failed: HTTP Error Code : " + connection.getResponseCode());
            }

            /*BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String output;

            System.out.println("Server Output... \n");
            while((output = bufferedReader.readLine()) != null){
                System.out.println(output);
            }*/

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    //combined code into requestConnect()
   /* public static void basicAuth(){
        StringBuilder builder = new StringBuilder();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://127.0.0.1:8080/");

        try {
            HttpResponse response = client.execute(request);
            HttpEntity entity =  response.getEntity();
            String data = EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String userName = "test";
        String password = "test";
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);
        Header basicAuthHeader = null;
        try {
            basicAuthHeader = new BasicScheme(Charset.forName("UTF-8")).authenticate(credentials, request, null);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        request.addHeader(basicAuthHeader);
    }
    */
}
