package org.torproject.android;

import org.json.*;

import java.util.ArrayList;

public class ParseJSONData {


    public class DirAuthority  {
        public String nickname;
        public int orport;
        public String v3ident;
        public String flags;
        public int port;
        public String fingerprint;
        JSONObject DirAuthority = parseDirAuhtorities1();
        JSONObject DirAuthority2 = parseDirAuthorities2();
    }

    public class ClientTransportPlugin {
        public String transport;
        public String binary;
        public String options;
        JSONObject ClientTransportPlugin = parseClientTransportPlugin();
    }

    public class Bridge {
        public String transport;
        public String ip;
        public int orport;
        JSONObject Bridge = parseBridge();
    }

    public ArrayList<DirAuthority> dirAuthorities;
    public JSONObject socksPort = parseSocksPort();
    public JSONObject useBridges = parseUseBridges();
    public JSONObject clientTransportPlugin =parseClientTransportPlugin();
    public Bridge bridge;


    public JSONObject parseDirAuhtorities1() {
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
        return DirAuthObj1;
    }

    public JSONObject parseDirAuthorities2() {
        JSONObject DirAuthObj2 = new JSONObject();
        try {
            String nickname = DirAuthObj2.getJSONObject("DirAuthorities").getString("nickname");
            int orport = DirAuthObj2.getJSONObject("DirAuthorities").getInt("orport");
            String v3ident = DirAuthObj2.getJSONObject("DirAuthorities").getString("v3ident");
            String flags = DirAuthObj2.getJSONObject("DirAuthorities").getString("flags");
            String ip = DirAuthObj2.getJSONObject("DirAuthorities").getString("ip");
            int port = DirAuthObj2.getJSONObject("DirAuthorities").getInt("port");
            String fingerprint = DirAuthObj2.getJSONObject("DirAuthorities").getString("fingerprint");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return DirAuthObj2;
    }

    public JSONObject parseSocksPort() {
        JSONObject SocksPortObj = new JSONObject();
        try {
            String SocksPort = SocksPortObj.getJSONObject("SocksPort").getString("SocksPort");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return SocksPortObj;
    }

    public JSONObject parseUseBridges() {
        JSONObject UseBridgesObj = new JSONObject();
        try {
            int UseBridges = UseBridgesObj.getJSONObject("UseBridges").getInt("UseBridges");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return UseBridgesObj;
    }

    public JSONObject parseClientTransportPlugin() {
        JSONObject ClientTransportObj = new JSONObject();
        try {
            String transport = ClientTransportObj.getJSONObject("ClientTransportPlugin").getString("transport");
            String binary = ClientTransportObj.getJSONObject("ClientTransportPlugin").getString("binary");
            String options = ClientTransportObj.getJSONObject("ClientTransportPlugin").getString("options");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ClientTransportObj;
    }

    public JSONObject parseBridge() {
        JSONObject BridgeObj = new JSONObject();
        try {
            String transport = BridgeObj.getJSONObject("Bridge").getString("transport");
            String ip = BridgeObj.getJSONObject("Bridge").getString("ip");
            int orport = BridgeObj.getJSONObject("Bridge").getInt("orport");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return BridgeObj;
    }
}




