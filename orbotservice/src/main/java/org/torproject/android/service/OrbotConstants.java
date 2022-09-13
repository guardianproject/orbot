/* Copyright (c) 2009, Nathan Freitas, Orbot/The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.service;

import android.content.Intent;

import java.util.Arrays;
import java.util.List;

public interface OrbotConstants {

    String TAG = "Orbot";

    String PREF_OR = "pref_or";
    String PREF_OR_PORT = "pref_or_port";
    String PREF_OR_NICKNAME = "pref_or_nickname";
    String PREF_REACHABLE_ADDRESSES = "pref_reachable_addresses";
    String PREF_REACHABLE_ADDRESSES_PORTS = "pref_reachable_addresses_ports";

    String PREF_DISABLE_NETWORK = "pref_disable_network";

    String PREF_TOR_SHARED_PREFS = "org.torproject.android_preferences";

    String PREF_SOCKS = "pref_socks";

    String PREF_HTTP = "pref_http";

    String PREF_ISOLATE_DEST = "pref_isolate_dest";

    String PREF_CONNECTION_PADDING = "pref_connection_padding";
    String PREF_REDUCED_CONNECTION_PADDING = "pref_reduced_connection_padding";
    String PREF_CIRCUIT_PADDING = "pref_circuit_padding";
    String PREF_REDUCED_CIRCUIT_PADDING = "pref_reduced_circuit_padding";

    String PREF_PREFER_IPV6 = "pref_prefer_ipv6";
    String PREF_DISABLE_IPV4 = "pref_disable_ipv4";


    String APP_TOR_KEY = "_app_tor";
    String APP_DATA_KEY = "_app_data";
    String APP_WIFI_KEY = "_app_wifi";


    String DIRECTORY_TOR_DATA = "tordata";

    //geoip data file asset key
    String GEOIP_ASSET_KEY = "geoip";
    String GEOIP6_ASSET_KEY = "geoip6";

    String IP_LOCALHOST = "127.0.0.1";
    int TOR_TRANSPROXY_PORT_DEFAULT = 9040;

    int TOR_DNS_PORT_DEFAULT = 5400;

    String HTTP_PROXY_PORT_DEFAULT = "8118"; // like Privoxy!
    String SOCKS_PROXY_PORT_DEFAULT = "9050";

    //control port
    String LOG_NOTICE_HEADER = "NOTICE: ";
    String LOG_NOTICE_BOOTSTRAPPED = "Bootstrapped";

    /**
     * A request to Orbot to transparently start Tor services
     */
    String ACTION_START = "org.torproject.android.intent.action.START";
    String ACTION_STOP = "org.torproject.android.intent.action.STOP";

    // needed when Orbot exits and tor is not running, but the notification is still active
    String ACTION_STOP_FOREGROUND_TASK = "org.torproject.android.intent.action.STOP_FOREGROUND_TASK";

    String ACTION_START_VPN = "org.torproject.android.intent.action.START_VPN";
    String ACTION_STOP_VPN = "org.torproject.android.intent.action.STOP_VPN";
    String ACTION_RESTART_VPN = "org.torproject.android.intent.action.RESTART_VPN";


    String ACTION_UPDATE_ONION_NAMES = "org.torproject.android.intent.action.UPDATE_ONION_NAMES";

    /**
     * {@link Intent} send by Orbot with {@code ON/OFF/STARTING/STOPPING} status
     */
    String ACTION_STATUS = "org.torproject.android.intent.action.STATUS";
    /**
     * {@code String} that contains a status constant: {@link #STATUS_ON},
     * {@link #STATUS_OFF}, {@link #STATUS_STARTING}, or
     * {@link #STATUS_STOPPING}
     */
    String EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS";
    /**
     * A {@link String} {@code packageName} for Orbot to direct its status reply
     * to, used in {@link #ACTION_START} {@link Intent}s sent to Orbot
     */
    String EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME";
    /**
     * The SOCKS proxy settings in URL form.
     */
    String EXTRA_SOCKS_PROXY = "org.torproject.android.intent.extra.SOCKS_PROXY";
    String EXTRA_SOCKS_PROXY_HOST = "org.torproject.android.intent.extra.SOCKS_PROXY_HOST";
    String EXTRA_SOCKS_PROXY_PORT = "org.torproject.android.intent.extra.SOCKS_PROXY_PORT";
    /**
     * The HTTP proxy settings in URL form.
     */
    String EXTRA_HTTP_PROXY = "org.torproject.android.intent.extra.HTTP_PROXY";
    String EXTRA_HTTP_PROXY_HOST = "org.torproject.android.intent.extra.HTTP_PROXY_HOST";
    String EXTRA_HTTP_PROXY_PORT = "org.torproject.android.intent.extra.HTTP_PROXY_PORT";

    String EXTRA_DNS_PORT = "org.torproject.android.intent.extra.DNS_PORT";
    String EXTRA_TRANS_PORT = "org.torproject.android.intent.extra.TRANS_PORT";

    String LOCAL_ACTION_LOG = "log";
    String LOCAL_ACTION_STATUS = "status";
    String LOCAL_ACTION_BANDWIDTH = "bandwidth";
    String LOCAL_EXTRA_TOTAL_READ = "totalRead";
    String LOCAL_EXTRA_TOTAL_WRITTEN = "totalWritten";
    String LOCAL_EXTRA_LAST_WRITTEN = "lastWritten";
    String LOCAL_EXTRA_LAST_READ = "lastRead";
    String LOCAL_EXTRA_LOG = "log";
    String LOCAL_EXTRA_BOOTSTRAP_PERCENT = "percent";
    String LOCAL_ACTION_PORTS = "ports";
    String LOCAL_ACTION_V3_NAMES_UPDATED = "V3_NAMES_UPDATED";
    String LOCAL_ACTION_NOTIFICATION_START = "notification_start";
    String LOCAL_ACTION_SMART_CONNECT_EVENT = "smart";
    String LOCAL_EXTRA_SMART_STATUS = "status";
    String SMART_STATUS_NO_DIRECT = "no_direct";
    String SMART_STATUS_CIRCUMVENTION_ATTEMPT_FAILED = "bad_attempt_suggestion";


    /**
     * All tor-related services and daemons are stopped
     */
    String STATUS_OFF = "OFF";

    /**
     * All tor-related services and daemons have completed starting
     */
    String STATUS_ON = "ON";
    String STATUS_STARTING = "STARTING";
    String STATUS_STOPPING = "STOPPING";

    /**
     * The user has disabled the ability for background starts triggered by
     * apps. Fallback to the old {@link Intent} action that brings up Orbot:
     * {@link #ACTION_START}
     */
    String STATUS_STARTS_DISABLED = "STARTS_DISABLED";

    // actions for internal command Intents
    String CMD_SET_EXIT = "setexit";
    String CMD_ACTIVE = "ACTIVE";
    String CMD_SNOWFLAKE_PROXY = "sf_proxy";

    String ONION_SERVICES_DIR = "v3_onion_services";
    String V3_CLIENT_AUTH_DIR = "v3_client_auth";

    String PREFS_DNS_PORT = "PREFS_DNS_PORT";

    String PREFS_KEY_TORIFIED = "PrefTord";

    /**
     * Include packages here to make the VPNService ignore these apps (On Lollipop+). This is to
     * prevent tor over tor scenarios...
     */
    List<String> BYPASS_VPN_PACKAGES = Arrays.asList("org.torproject.torbrowser_alpha",
            "org.torproject.torbrowser",
            "org.onionshare.android", // issue #618
            "org.briarproject.briar.android" // https://github.com/guardianproject/orbot/issues/474
    );

    String SNOWFLAKE_EMOJI = "❄️";
    String SNOWFLAKE_PROXY_EMOJI = "\uD83D\uDCF2";

}
