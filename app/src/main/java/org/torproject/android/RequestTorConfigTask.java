package org.torproject.android;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
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


// Network activity cannot be done on the main thread so we do it in an AsyncTask
public class RequestTorConfigTask extends AsyncTask<RequestTorConfigTask.TorRESTRequestParams, Void, PrivateTorNetworkConfig> {
    public static class TorRESTRequestParams {
        String username;
        String password;
        String url;
    }

    protected PrivateTorNetworkConfig doInBackground(TorRESTRequestParams ... params) {
        TorRESTRequestParams param = params[0];
        try {
            StringBuilder builder = new StringBuilder();
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(param.url);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(param.username, param.password);
            Header basicAuthHeader = new BasicScheme(Charset.forName("UTF-8")).authenticate(credentials, request, null);
            request.addHeader(basicAuthHeader);
            request.addHeader("content-type", "application/json");

            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            String data = EntityUtils.toString(entity);

            PrivateTorNetworkConfig config = PrivateTorNetworkConfig.parseJSON(data);

            return config;
        } catch (MalformedURLException e) {
            return null;
        } catch (AuthenticationException e) {
            return null;
        } catch (IOException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }
}
