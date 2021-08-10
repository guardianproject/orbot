package org.torproject.android.service.vpn;

public interface VpnPrefs {

    String PREFS_DNS_PORT = "PREFS_DNS_PORT";

    String PREFS_KEY_TORIFIED = "PrefTord";

    /**
     * Include packages here to make the VPNService ignore these apps (On Lollipop+). This is to
     * prevent tor over tor scenarios...
     */
    String[] BYPASS_VPN_PACKAGES = new String[] {
            "org.torproject.torbrowser_alpha",
            "org.torproject.torbrowser",
            "org.briarproject.briar.android" // https://github.com/guardianproject/orbot/issues/474
    };
}
