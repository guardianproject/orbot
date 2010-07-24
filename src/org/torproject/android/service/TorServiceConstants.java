/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service;

public interface TorServiceConstants {

	public final static String TAG = "TOR_SERVICE";

	public final static String TOR_APP_USERNAME = "org.torproject.android";
	
	//home directory of Android application
//	public final static String TOR_HOME = "/data/data/" + TOR_APP_USERNAME + "/";

	//public final static String TOR_HOME_DATA_DIR = TOR_HOME + "data/";
	
	//name of the tor C binary
	public final static String TOR_BINARY_ASSET_KEY = "tor";	
//	public final static String TOR_BINARY_INSTALL_PATH = TOR_HOME + TOR_BINARY_ASSET_KEY; //path to install the Tor binary too
	public final static String TOR_BINARY_ZIP_KEY = "assets/" + TOR_BINARY_ASSET_KEY;//key of the tor binary in the Zip file
	
	//torrc (tor config file)
	public final static String TORRC_ASSET_KEY = "torrc";
//	public final static String TORRC_INSTALL_PATH = TOR_HOME + TORRC_ASSET_KEY; //path to install torrc to within the android app data folder
	public final static String TORRC_ZIP_KEY = "assets/" + TORRC_ASSET_KEY; //key of the torrc file in the Zip file
	//how to launch tor
//	public final static String TOR_COMMAND_LINE_ARGS = "-f " + TORRC_INSTALL_PATH  + " || exit\n";
		
	//privoxy
	public final static String PRIVOXY_ASSET_KEY = "privoxy";
//	public final static String PRIVOXY_INSTALL_PATH = TOR_HOME + PRIVOXY_ASSET_KEY; //path to install privoxy to within the android app data folder
	public final static String PRIVOXY_ZIP_KEY = "assets/" + PRIVOXY_ASSET_KEY; //key of the privoxy file in the Zip file
	
	//privoxy.config
	public final static String PRIVOXYCONFIG_ASSET_KEY = "privoxy.config";
//	public final static String PRIVOXYCONFIG_INSTALL_PATH = TOR_HOME + PRIVOXYCONFIG_ASSET_KEY; //path to install privoxy to within the android app data folder
	public final static String PRIVOXYCONFIG_ZIP_KEY = "assets/" + PRIVOXYCONFIG_ASSET_KEY; //key of the privoxy file in the Zip file
	
	//how to launch privoxy
//	public final static String PRIVOXY_COMMAND_LINE_ARGS = ' ' + PRIVOXYCONFIG_INSTALL_PATH + " || exit\n";

	//where to send the notices log
//	public final static String TOR_LOG_PATH = TOR_HOME + "notices.log";
	
	//control port cookie path
//	public final static String TOR_CONTROL_AUTH_COOKIE = TOR_HOME_DATA_DIR + "control_auth_cookie";

	
	//various console cmds
	public final static String SHELL_CMD_CHMOD = "chmod";
	public final static String SHELL_CMD_KILL = "kill";
	public final static String SHELL_CMD_RM = "rm";
	public final static String SHELL_CMD_PS = "ps";
	public final static String SHELL_CMD_PIDOF = "pidof";

	public final static String CHMOD_EXE_VALUE = "777";
	
	//path of the installed APK file
	public final static String APK_PATH = "/data/app/org.torproject.android.apk";
	public final static String APK_PATH_BASE = "/data/app";

	
	
	public final static int FILE_WRITE_BUFFER_SIZE = 2048;
	
	//HTTP Proxy server port
	public final static int PORT_HTTP = 8118; //just like Privoxy!
	
	//Socks port client connects to, server is the Tor binary
	public final static int PORT_SOCKS = 9050;
	
	//what is says!
	public final static String IP_LOCALHOST = "127.0.0.1";
	public final static int TOR_CONTROL_PORT = 9051;
	public final static int UPDATE_TIMEOUT = 1000;
	
	//path to check Tor against
	public final static String URL_TOR_CHECK = "https://check.torproject.org";
		
    //IPTABLES
//	public final static String CMD_IPTABLES_PREROUTING = "iptables -t nat -A OUTPUT -p tcp --dport 80 -j DNAT --to 127.0.0.1:8118 || exit\n";
	//public final static String CMD_IPTABLES_PREROUTING_FLUSH = "iptables -t nat -F || exit\n";

    //control port 
    public final static String TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE = "Bootstrapped 100%";
    
    public final static int STATUS_OFF = -1;
    public final static int STATUS_READY = 0;
    public final static int STATUS_ON = 1;
    public final static int STATUS_CONNECTING = 2;
    
    public final static int PROFILE_OFF = -1;
    public final static int PROFILE_ON = 1;
}
