package org.torproject.android;

import java.io.IOException;
import java.nio.charset.Charset;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.auth.AuthenticationException;
import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.auth.BasicScheme;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

public class BasicAuthentication {
    public void basicAuth(){
        StringBuilder builder = new StringBuilder();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("YOUR WEBSITE HERE");
        try {
            HttpResponse response = client.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String userName = "";
        String password = "";
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);
        Header basicAuthHeader = null;
        try {
            basicAuthHeader = new BasicScheme(Charset.forName("UTF-8")).authenticate(credentials, request, null);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        request.addHeader(basicAuthHeader);   }

}
