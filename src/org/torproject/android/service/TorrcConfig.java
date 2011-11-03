package org.torproject.android.service;

public class TorrcConfig {

}

/*
 * GeoIPFile
 */
/*
HTTPProxy host[:port]
Tor will make all its directory requests through this host:port (or host:80 if port is not specified), rather than connecting directly to any directory servers.

HTTPProxyAuthenticator username:password
If defined, Tor will use this username:password for Basic HTTP proxy authentication, as in RFC 2617. This is currently the only form of HTTP proxy authentication that Tor supports; feel free to submit a patch if you want it to support others.

HTTPSProxy host[:port]
Tor will make all its OR (SSL) connections through this host:port (or host:443 if port is not specified), via HTTP CONNECT rather than connecting directly to servers. You may want to set FascistFirewall to restrict the set of ports you might try to connect to, if your HTTPS proxy only allows connecting to certain ports.

HTTPSProxyAuthenticator username:password
If defined, Tor will use this username:password for Basic HTTPS proxy authentication, as in RFC 2617. This is currently the only form of HTTPS proxy authentication that Tor supports; feel free to submit a patch if you want it to support others.

Socks4Proxy host[:port]
Tor will make all OR connections through the SOCKS 4 proxy at host:port (or host:1080 if port is not specified).

Socks5Proxy host[:port]
Tor will make all OR connections through the SOCKS 5 proxy at host:port (or host:1080 if port is not specified).

Socks5ProxyUsername username

Socks5ProxyPassword password
If defined, authenticate to the SOCKS 5 server using username and password in accordance to RFC 1929. Both username and password must be between 1 and 255 characters.
*/