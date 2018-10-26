package org.torproject.android;

import org.json.*;

public class ParseJSONData {
    public void parseDirAuhtorities1(){
        JSONObject DirAuthObj1 = new JSONObject();
        try {
            String nickname = DirAuthObj1.getJSONObject("DirAuthorities").getString("nickname");
            int orport = DirAuthObj1.getJSONObject("DirAuthorities").getInt("orport");
            String v3ident = DirAuthObj1.getJSONObject("DirAuthorities").getString("v3ident");
            String flags = DirAuthObj1.getJSONObject("DirAuthorities").getString("flags");
            String ip = DirAuthObj1.getJSONObject("DirAuthorities").getString("ip");
            int port = DirAuthObj1.getJSONObject("DirAuthorities").getInt("port");
            String fingerprint = DirAuthObj1.getJSONObject("DirAuthorities").getString("fingerprint");
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void parseDirAuthorities2(){
        JSONObject DirAuthObj2 = new JSONObject();
        try {
            String nickname = DirAuthObj2.getJSONObject("DirAuthorities").getString("nickname");
            int orport = DirAuthObj2.getJSONObject("DirAuthorities").getInt("orport");
            String v3ident = DirAuthObj2.getJSONObject("DirAuthorities").getString("v3ident");
            String flags = DirAuthObj2.getJSONObject("DirAuthorities").getString("flags");
            String ip = DirAuthObj2.getJSONObject("DirAuthorities").getString("ip");
            int port = DirAuthObj2.getJSONObject("DirAuthorities").getInt("port");
            String fingerprint = DirAuthObj2.getJSONObject("DirAuthorities").getString("fingerprint");
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void parseSocksPort(){
        JSONObject SocksPortObj = new JSONObject();
        try{
            String SocksPort  = SocksPortObj.getJSONObject("SocksPort").getString("SocksPort");
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void parseUseBridges(){
        JSONObject UseBridgesObj = new JSONObject();
        try {
            int UseBridges  = UseBridgesObj.getJSONObject("UseBridges").getInt("UseBridges");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void parseClientTransportPlugin(){
        JSONObject ClientTransportObj = new JSONObject();
        try{
            String transport  = ClientTransportObj.getJSONObject("ClientTransportPlugin").getString("transport");
            String binary  = ClientTransportObj.getJSONObject("ClientTransportPlugin").getString("binary");
            String options  = ClientTransportObj.getJSONObject("ClientTransportPlugin").getString("options");
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void parseBridge(){
        JSONObject BridgeObj = new JSONObject();
        try {
            String transport  = BridgeObj.getJSONObject("Bridge").getString("transport");
            String ip  = BridgeObj.getJSONObject("Bridge").getString("ip");
            int orport  = BridgeObj.getJSONObject("Bridge").getInt("orport");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}


