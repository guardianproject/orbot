/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.service;

import android.content.Intent;

public interface TorServiceConstants {

	public final static String TOR_APP_USERNAME = "org.torproject.android";
	public final static String BROWSER_APP_USERNAME = "info.guardianproject.orfox";
	
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

	public final static String IP_LOCALHOST = "127.0.0.1";
	public final static int UPDATE_TIMEOUT = 1000;
	public final static int TOR_TRANSPROXY_PORT_DEFAULT = 9040;
	
	public final static int STANDARD_DNS_PORT = 53;
	public final static int TOR_DNS_PORT_DEFAULT = 5400;
	public final static String TOR_VPN_DNS_LISTEN_ADDRESS = "127.0.0.1";
	
	public final static int CONTROL_PORT_DEFAULT = 9051;
    public final static int HTTP_PROXY_PORT_DEFAULT = 8118; // like Privoxy!
    public final static int SOCKS_PROXY_PORT_DEFAULT = 9050;

    
	//path to check Tor against
	public final static String URL_TOR_CHECK = "https://check.torproject.org";

    //control port 
    public final static String TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE = "Bootstrapped 100%";
    public final static String LOG_NOTICE_HEADER = "NOTICE";
    public final static String LOG_NOTICE_BOOTSTRAPPED = "Bootstrapped";
    
    /**
     * A request to Orbot to transparently start Tor services
     */
    public final static String ACTION_START = "org.torproject.android.intent.action.START";
    /**
     * {@link Intent} send by Orbot with {@code ON/OFF/STARTING/STOPPING} status
     */
    public final static String ACTION_STATUS = "org.torproject.android.intent.action.STATUS";
    /**
     * {@code String} that contains a status constant: {@link #STATUS_ON},
     * {@link #STATUS_OFF}, {@link #STATUS_STARTING}, or
     * {@link #STATUS_STOPPING}
     */
    public final static String EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS";
    /**
     * A {@link String} {@code packageName} for Orbot to direct its status reply
     * to, used in {@link #ACTION_START} {@link Intent}s sent to Orbot
     */
    public final static String EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME";
    /**
     * The SOCKS proxy settings in URL form.
     */
    public final static String EXTRA_SOCKS_PROXY = "org.torproject.android.intent.extra.SOCKS_PROXY";
    public final static String EXTRA_SOCKS_PROXY_HOST = "org.torproject.android.intent.extra.SOCKS_PROXY_HOST";
    public final static String EXTRA_SOCKS_PROXY_PORT = "org.torproject.android.intent.extra.SOCKS_PROXY_PORT";
    /**
     * The HTTP proxy settings in URL form.
     */
    public final static String EXTRA_HTTP_PROXY = "org.torproject.android.intent.extra.HTTP_PROXY";
    public final static String EXTRA_HTTP_PROXY_HOST = "org.torproject.android.intent.extra.HTTP_PROXY_HOST";
    public final static String EXTRA_HTTP_PROXY_PORT = "org.torproject.android.intent.extra.HTTP_PROXY_PORT";

    public final static String LOCAL_ACTION_LOG = "log";
    public final static String LOCAL_ACTION_BANDWIDTH = "bandwidth";
    public final static String LOCAL_EXTRA_LOG = "log";

    /**
     * All tor-related services and daemons are stopped
     */
    public final static String STATUS_OFF = "OFF";
    /**
     * All tor-related services and daemons have completed starting
     */
    public final static String STATUS_ON = "ON";
    public final static String STATUS_STARTING = "STARTING";
    public final static String STATUS_STOPPING = "STOPPING";
    /**
     * The user has disabled the ability for background starts triggered by
     * apps. Fallback to the old Intent that brings up Orbot.
     */
    public final static String STATUS_STARTS_DISABLED = "STARTS_DISABLED";

    // actions for internal command Intents
    public static final String CMD_SIGNAL_HUP = "signal_hup";
    public static final String CMD_STATUS = "status";
    public static final String CMD_FLUSH = "flush";
    public static final String CMD_NEWNYM = "newnym";
    public static final String CMD_VPN = "vpn";
    public static final String CMD_VPN_CLEAR = "vpnclear";
    public static final String CMD_UPDATE_TRANS_PROXY = "update";
    public static final String CMD_SET_EXIT = "setexit";
    
    
    
    public static final String BINARY_TOR_VERSION = "0.2.7.5-020120160125";
    public static final String PREF_BINARY_TOR_VERSION_INSTALLED = "BINARY_TOR_VERSION_INSTALLED";
    
    //obfsproxy 
    public static final String OBFSCLIENT_ASSET_KEY = "obfs4proxy";
    
   // public static final String MEEK_ASSET_KEY = "meek-client";
    
	//name of the iptables binary
	public final static String IPTABLES_ASSET_KEY = "xtables";	

	//DNS daemon for TCP DNS over TOr
	public final static String PDNSD_ASSET_KEY = "pdnsd";

	//EXIT COUNTRY CODES
	public final static String[] COUNTRY_CODES = {"AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AS", "AT", "AU", "AW", "AX", "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL", "BM", "BN", "BO", "BQ", "BR", "BS", "BT", "BV", "BW", "BY", "BZ", "CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN", "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM", "DO", "DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK", "FM", "FO", "FR", "GA", "GB", "GD", "GE", "GF", "GG", "GH", "GI", "GL", "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HM", "HN", "HR", "HT", "HU", "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR", "IS", "IT", "JE", "JM", "JO", "JP", "KE", "KG", "KH", "KI", "KM", "KN", "KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC", "LI", "LK", "LR", "LS", "LT", "LU", "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MH", "MK", "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA", "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP", "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG", "PH", "PK", "PL", "PM", "PN", "PR", "PS", "PT", "PW", "PY", "QA", "RE", "RO", "RS", "RU", "RW", "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SR", "SS", "ST", "SV", "SX", "SY", "SZ", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TLa", "TM", "TN", "TO", "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "UM", "US", "UY", "UZ", "VA", "VC", "VE", "VG", "VI", "VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW"};
	
	//MEEK BRIDGEs	
	public final static String[] BRIDGES_MEEK = 
		{
			"meek_lite 0.0.2.0:1 url=https://meek-reflect.appspot.com/ front=www.google.com",
			"meek_lite 0.0.2.0:2 url=https://d2zfqthxsdq309.cloudfront.net/ front=a0.awsstatic.com",
			"meek_lite 0.0.2.0:3 url=https://az668014.vo.msecnd.net/ front=ajax.aspnetcdn.com"
		};

}
