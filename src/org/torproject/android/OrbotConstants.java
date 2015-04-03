/* Copyright (c) 2009, Nathan Freitas, Orbot/The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

public interface OrbotConstants {

	public final static String TAG = "Orbot";
	

	public final static String PREFS_KEY = "OrbotPrefs";
	public final static String PREFS_KEY_TORIFIED = "PrefTord";

	public final static int FILE_WRITE_BUFFER_SIZE = 2048;
	
	//path to check Tor against
	public final static String URL_TOR_CHECK = "https://check.torproject.org";
	
    public final static String URL_TOR_BRIDGES = "https://bridges.torproject.org/bridges?transport=";
    
    public final static String NEWLINE = "\n";
    
   // public final static String INTENT_TOR_SERVICE = "org.torproject.android.service.TOR_SERVICE";
    	
    public final static String HANDLER_TOR_MSG = "torServiceMsg";
	
	public final static String PREF_BRIDGES_ENABLED = "pref_bridges_enabled";
	public final static String PREF_BRIDGES_UPDATED = "pref_bridges_enabled";
	public final static String PREF_BRIDGES_LIST = "pref_bridges_list";
	//public final static String PREF_BRIDGES_OBFUSCATED = "pref_bridges_obfuscated";
    public final static String PREF_OR = "pref_or";
    public final static String PREF_OR_PORT = "pref_or_port";
    public final static String PREF_OR_NICKNAME = "pref_or_nickname";
    public final static String PREF_REACHABLE_ADDRESSES = "pref_reachable_addresses";
    public final static String PREF_REACHABLE_ADDRESSES_PORTS = "pref_reachable_addresses_ports";
	public final static String PREF_TRANSPARENT = "pref_transparent";
	public final static String PREF_TRANSPARENT_ALL = "pref_transparent_all";
	
	public final static String PREF_HAS_ROOT = "has_root";
	public final static  int RESULT_CLOSE_ALL = 0;
	
	public final static String PREF_USE_SYSTEM_IPTABLES = "pref_use_sys_iptables";
	
	public final static String PREF_PERSIST_NOTIFICATIONS = "pref_persistent_notifications";
	
	public final static String PREF_DEFAULT_LOCALE = "pref_default_locale";
	
	public final static String PREF_DISABLE_NETWORK = "pref_disable_network";
	
	public final static String PREF_TOR_SHARED_PREFS = "org.torproject.android_preferences";
	
	public final static int MAX_LOG_LENGTH = 10000;
	
	public final static String PREF_SOCKS = "pref_socks";
	
}
