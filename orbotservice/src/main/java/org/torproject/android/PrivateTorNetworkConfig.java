package org.torproject.android;

import org.json.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

public class PrivateTorNetworkConfig implements Serializable {
    public ArrayList<DirAuthority> getDirAuthorities() {
        return dirAuthorities;
    }

    public String getSocksPort() {
        return socksPort;
    }

    public int getUseBridges() {
        return useBridges;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public ClientTransportPlugin getClientTransportPlugin() {
        return clientTransportPlugin;
    }

    // Returns a string representing the config in the same format as the torrc.
    public String toTorrcString() {
        StringBuilder builder = new StringBuilder();

        for (DirAuthority authority : dirAuthorities) {
            builder.append(authority.toTorrcString());
            builder.append("\n");
        }

        builder.append(String.format(Locale.US, "SocksPort %s", socksPort));
        builder.append("\n");

        builder.append(String.format(Locale.US, "UseBridges %d", useBridges));
        builder.append("\n");

        builder.append(clientTransportPlugin.toTorrcString());
        builder.append("\n");

        builder.append(bridge.toTorrcString());
        builder.append("\n");

        return builder.toString();
    }

    public static class DirAuthority implements Serializable  {
        public String nickname;
        public int orport;
        public String v3ident;
        public String flags;
        public String ip;
        public int port;
        public String fingerprint;

        public String toTorrcString() {
            return String.format(Locale.US, "DirAuthority %s orport=%d v3ident=%s %s %s:%d %s",
                    nickname, orport, v3ident, flags, ip, port, fingerprint);
        }
    }

    public static class ClientTransportPlugin implements Serializable {
        public String transport;
        public String binary;
        public String options;

        public String toTorrcString() {
            return String.format(Locale.US, "ClientTransportPlugin %s exec %s %s",
                    transport, binary, options);
        }
    }

    public static class Bridge implements Serializable {
        public String transport;
        public String ip;
        public int orport;

        public String toTorrcString() {
            return String.format(Locale.US, "Bridge %s %s:%d", transport, ip, orport);
        }
    }

    private ArrayList<DirAuthority> dirAuthorities;
    private String socksPort;
    private int useBridges;
    private Bridge bridge;
    private ClientTransportPlugin clientTransportPlugin;

    private static ArrayList<DirAuthority> parseDirAuthorities(JSONObject root) throws JSONException {
        ArrayList<DirAuthority> dirAuthorities = new ArrayList<>();
        JSONArray dirAuthoritiesJSON = root.getJSONArray("DirAuthorities");
        for (int i = 0; i < dirAuthoritiesJSON.length(); i++) {
            JSONObject dirAuthorityJSON = dirAuthoritiesJSON.getJSONObject(i);
            DirAuthority dirAuth = new DirAuthority();
            dirAuth.nickname = dirAuthorityJSON.getString("nickname");
            dirAuth.orport = dirAuthorityJSON.getInt("orport");
            dirAuth.v3ident = dirAuthorityJSON.getString("v3ident");
            dirAuth.flags = dirAuthorityJSON.getString("flags");
            dirAuth.ip = dirAuthorityJSON.getString("ip");
            dirAuth.port = dirAuthorityJSON.getInt("port");
            dirAuth.fingerprint = dirAuthorityJSON.getString("fingerprint");
            dirAuthorities.add(dirAuth);
        }

        return dirAuthorities;
    }

    private static ClientTransportPlugin parseClientTransportPlugin(JSONObject root) throws JSONException {
        ClientTransportPlugin plugin = new ClientTransportPlugin();
        JSONObject clientTransportJSON = root.getJSONObject("ClientTransportPlugin");
        plugin.transport = clientTransportJSON.getString("transport");
        plugin.binary = clientTransportJSON.getString("binary");
        plugin.options = clientTransportJSON.getString("options");
        return plugin;
    }

    private static Bridge parseBridge(JSONObject root) throws JSONException {
        Bridge bridge = new Bridge();
        JSONObject bridgeJSON = root.getJSONObject("Bridge");
        bridge.transport = bridgeJSON.getString("transport");
        bridge.ip = bridgeJSON.getString("ip");
        bridge.orport = bridgeJSON.getInt("orport");
        return bridge;
    }

    public static PrivateTorNetworkConfig parseJSON(String jsonResponse) throws JSONException {
        PrivateTorNetworkConfig config = new PrivateTorNetworkConfig();
        JSONObject root = new JSONObject(jsonResponse);
        config.dirAuthorities = parseDirAuthorities(root);
        config.socksPort = root.getString("SocksPort");
        config.useBridges = root.getInt("UseBridges");
        config.clientTransportPlugin = parseClientTransportPlugin(root);
        config.bridge = parseBridge(root);

        return config;
    }
}
