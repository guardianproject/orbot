/* Copyright (c) 2009, Nathan Freitas, The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

public interface TorConstants {

	//home directory of Android application
	public final static String TOR_HOME = "/data/data/org.torproject.android/";

	//name of the tor C binary
	public final static String TOR_BINARY_ASSET_KEY = "tor";	
	
	//path to install the Tor binary too
	public final static String TOR_BINARY_INSTALL_PATH = TOR_HOME + TOR_BINARY_ASSET_KEY;
	
	//key of the tor binary in the Zip file
	public final static String TOR_BINARY_ZIP_KEY = "assets/" + TOR_BINARY_ASSET_KEY;
	
	//torrc file name
	public final static String TORRC_ASSET_KEY = "torrc";
	
	//path to install torrc to within the android app data folder
	public final static String TORRC_INSTALL_PATH = TOR_HOME + TORRC_ASSET_KEY;
	
	//key of the torrc file in the Zip file
	public final static String TORRC_ZIP_KEY = "assets/" + TORRC_ASSET_KEY;

	//where to send the notices log
	public final static String TOR_LOG_PATH = TOR_HOME + "notices.log";
	
	//control port cookie path
	public final static String TOR_CONTROL_AUTH_COOKIE = TOR_HOME + "data/control_auth_cookie";

	//how to launch tor
	public final static String TOR_COMMAND_LINE_ARGS = "-f " + TORRC_INSTALL_PATH;
	
	//various console cmds
	public final static String SHELL_CMD_CHMOD = "/system/bin/chmod";
	public final static String SHELL_CMD_KILL = "/system/bin/kill";
	public final static String SHELL_CMD_RM = "/system/bin/rm";
	public final static String SHELL_CMD_PS = "ps";
	public final static String CHMOD_EXE_VALUE = "777";
	
	//path of the installed APK file
	public final static String APK_PATH = "/data/app/org.torproject.android.apk";
	
	//path to check Tor against
	public final static String URL_TOR_CHECK = "http://check.torproject.org";
	
	public final static int FILE_WRITE_BUFFER_SIZE = 2048;
	
	//HTTP Proxy server port
	public final static int PORT_HTTP = 8118; //just like Privoxy!
	
	//Socks port client connects to, server is the Tor binary
	public final static int PORT_SOCKS = 9050;
	
	//what is says!
	public final static String IP_LOCALHOST = "127.0.0.1";
	public final static int TOR_CONTROL_PORT = 9051;
	public final static int UPDATE_TIMEOUT = 3000;
	
	public final static String DEFAULT_HOME_PAGE = "file:///android_asset/help.html";// "http://check.torproject.org";
	
	//status to communicate state
    public final static int STATUS_OFF = 0;
    public final static int STATUS_ON = 1;
    public final static int STATUS_STARTING_UP = 2;
    public final static int STATUS_SHUTTING_DOWN = 3;
    
    //control port 
    public final static String TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE = "Bootstrapped 100%";
    
}
