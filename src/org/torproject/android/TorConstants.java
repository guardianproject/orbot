/* Copyright (c) 2009, Nathan Freitas, Orbot/The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android;

public interface TorConstants {

	public final static String TAG = "Orbot";

	public final static int FILE_WRITE_BUFFER_SIZE = 2048;
	
	//path to check Tor against
	public final static String URL_TOR_CHECK = "https://check.torproject.org";
	
    public final static int STATUS_OFF = -1;
    public final static int STATUS_READY = 0;
    public final static int STATUS_ON = 1;
    public final static int STATUS_CONNECTING = 2;
    
    public final static int PROFILE_OFF = -1;
    public final static int PROFILE_ONDEMAND = 0;
    public final static int PROFILE_ON = 1;
    
    public final static String NEWLINE = "\n";
    
    public final static String TORRC_DEFAULT = 
    		"SocksPort 9050\nSocksListenAddress 127.0.0.1\nSafeSocks 1\nDNSPort 5400\nLog notice stdout\nLog debug syslog\nDataDirectory /data/data/org.torproject.android/data\n"
    	+ "ControlPort 9051\nCookieAuthentication 1\nRelayBandwidthRate 20 KBytes\nRelayBandwidthBurst 20 KBytes\nAutomapHostsOnResolve 1\nTransPort 9040\n";
    	
    public final static String INTENT_TOR_SERVICE = "org.torproject.android.service.TOR_SERVICE";
    	
    public final static String HANDLER_TOR_MSG = "torServiceMsg";
	
	public final static String PREF_BRIDGES_ENABLED = "pref_bridges_enabled";
	public final static String PREF_BRIDGES_UPDATED = "pref_bridges_enabled";
	public final static String PREF_BRIDGES_LIST = "pref_bridges_list";
    public final static String PREF_RELAY = "pref_relay";
    public final static String PREF_RELAY_PORT = "pref_relay_port";
    public final static String PREF_FASCIST_FIREWALL = "pref_fascist_firewall";
    public final static String PREF_FASCIST_FIREWALL_PORTS = "pref_fascist_firewall_ports";
	public final static String PREF_TRANSPARENT = "pref_transparent";
	
	
}
