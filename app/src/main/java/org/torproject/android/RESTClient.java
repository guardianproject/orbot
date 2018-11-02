package org.torproject.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RESTClient {
    public static void requestConnect(String[] args) {
        try {
            URL url = new URL("http://127.0.0.1:8080/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != 200){
                throw new RuntimeException("Failed: HTTP Error Code : " + connection.getResponseCode());
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String output;
            System.out.println("Server Output... \n");
            while((output = bufferedReader.readLine()) != null){
                System.out.println(output);
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
