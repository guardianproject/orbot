/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service;

public interface TorServiceConstants {

	public final static String TOR_APP_USERNAME = "org.torproject.android";
	public final static String ORWEB_APP_USERNAME = "info.guardianproject.browser";
	
	public final static String DIRECTORY_TOR_BINARY = "bin";
	public final static String DIRECTORY_TOR_DATA = "data";
	
	//name of the tor C binary
	public final static String TOR_ASSET_KEY = "tor";	
	
	//torrc (tor config file)
	public final static String TORRC_ASSET_KEY = "torrc";
	public final static String TORRCDIAG_ASSET_KEY = "torrcdiag";
	public final static String TORRC_TETHER_KEY = "torrctether";
	
	public final static String TOR_CONTROL_COOKIE = "control_auth_cookie";
	
	//privoxy
	public final static String POLIPO_ASSET_KEY = "polipo";
	
	//privoxy.config
	public final static String POLIPOCONFIG_ASSET_KEY = "torpolipo.conf";
	
	//geoip data file asset key
	public final static String GEOIP_ASSET_KEY = "geoip";
	public final static String GEOIP6_ASSET_KEY = "geoip6";
	
	
	//various console cmds
	public final static String SHELL_CMD_CHMOD = "chmod";
	public final static String SHELL_CMD_KILL = "kill -9";
	public final static String SHELL_CMD_RM = "rm";
	public final static String SHELL_CMD_PS = "toolbox ps";
	public final static String SHELL_CMD_PS_ALT = "ps";
	
	//public final static String SHELL_CMD_PIDOF = "pidof";
	public final static String SHELL_CMD_LINK = "ln -s";
	public final static String SHELL_CMD_CP = "cp";
	

	public final static String CHMOD_EXE_VALUE = "770";

	public final static int FILE_WRITE_BUFFER_SIZE = 1024;
	
	//HTTP Proxy server port
	public static int PORT_HTTP = 8118; //just like Privoxy!
	
	//Socks port client connects to, server is the Tor binary
	public static String PORT_SOCKS_DEFAULT = "9050";
	
	
	//what is says!
	public final static String IP_LOCALHOST = "127.0.0.1";
	public final static int UPDATE_TIMEOUT = 1000;
	public final static int TOR_TRANSPROXY_PORT_DEFAULT = 9040;
	public final static int STANDARD_DNS_PORT = 53;
	public final static int TOR_DNS_PORT_DEFAULT = 5400;
	
	//path to check Tor against
	public final static String URL_TOR_CHECK = "https://check.torproject.org";

    //control port 
    public final static String TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE = "Bootstrapped 100%";
    
    public final static int STATUS_OFF = 0;
    public final static int STATUS_ON = 1;
    public final static int STATUS_CONNECTING = 2;
    
    public static final int STATUS_MSG = 1;
    public static final int ENABLE_TOR_MSG = 2;
    public static final int DISABLE_TOR_MSG = 3;
    public static final int LOG_MSG = 4;
    
    public static final String CMD_START = "start";
    public static final String CMD_STOP = "stop";
    public static final String CMD_FLUSH = "flush";
    public static final String CMD_NEWNYM = "newnym";
    public static final String CMD_INIT = "init";
    public static final String CMD_VPN = "vpn";
    public static final String CMD_VPN_CLEAR = "vpnclear";
    public static final String CMD_UPDATE = "update";
     
    
    public static final String BINARY_TOR_VERSION = "0.2.6-RC6-PT-UPDATE-2";
    public static final String PREF_BINARY_TOR_VERSION_INSTALLED = "BINARY_TOR_VERSION_INSTALLED";
    
    //obfsproxy 
    public static final String OBFSCLIENT_ASSET_KEY = "obfs4proxy";
    
    public static final String MEEK_ASSET_KEY = "meek-client";
    
    
	public static final int MESSAGE_TRAFFIC_COUNT = 5;
	

	//name of the iptables binary
	public final static String IPTABLES_ASSET_KEY = "xtables";	
	
	public final static int DEFAULT_CONTROL_PORT = 9051;
	

}
