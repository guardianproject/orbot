/**
 * Shadow - Anonymous web browser for Android devices
 * Copyright (C) 2009 Connell Gauld
 * 
 * Thanks to University of Cambridge,
 * 		Alastair Beresford and Andrew Rice
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.torproject.android.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import net.sourceforge.jsocks.socks.Socks5Proxy;
import net.sourceforge.jsocks.socks.SocksException;
import net.sourceforge.jsocks.socks.SocksSocket;


import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;


/**
 * Provides sockets for an HttpClient connection.
 * @author cmg47
 *
 */
public class SocksSocketFactory implements SocketFactory {

	SocksSocket server = null;
	private static Socks5Proxy sProxy = null;

	private final static String DEFAULT_HOST = "127.0.0.1";
	private final static int DEFAULT_PORT = 9050;
	
	/**
	 * Construct a SocksSocketFactory that uses the provided SOCKS proxy.
	 * @param proxyaddress the IP address of the SOCKS proxy
	 * @param proxyport the port of the SOCKS proxy
	 */
	public SocksSocketFactory(String proxyaddress, int proxyport) {
		

		try {
			sProxy = new Socks5Proxy(proxyaddress, proxyport);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			Log.i("TOR_SERVICE","SocksSF couldn't connect",e);
		}
		
		sProxy.resolveAddrLocally(false);
	

	}
	
	public Socket connectSocket(Socket sock, String host, int port,
			InetAddress localAddress, int localPort, HttpParams params) throws IOException,
			UnknownHostException, ConnectTimeoutException {
		
		Log.i("TOR_SERVICE","SocksSocketFactory: connectSocket: " + host + ":" + port);
		
		if (host == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null.");
        }

     //   int timeout = HttpConnectionParams.getConnectionTimeout(params);
        
        // Pipe this socket over the proxy
      //  sock = mSocksProxy.connectSocksProxy(sock, host, port, timeout);
        
        
    
		try {
			sock =  new SocksSocket(sProxy,host, port);
			

			
			sock.setSoTimeout(0); //indef
			
			
			 if ((localAddress != null) || (localPort > 0)) {

		            // we need to bind explicitly
		            if (localPort < 0)
		                localPort = 0; // indicates "any"

		            InetSocketAddress isa =
		                new InetSocketAddress(localAddress, localPort);
		            sock.bind(isa);
		        }

			
		} catch (SocksException e) {
			Log.e("TOR_SERVICE","error connecting socks to" + host + ":" + port,e);
		} catch (UnknownHostException e) {
			Log.e("TOR_SERVICE","error connecting socks to" + host + ":" + port,e);
		}
		
        return sock;
		
	}
	
    
    
	public Socket createSocket() throws IOException {
		return new Socket();
	}

	public boolean isSecure(Socket sock) throws IllegalArgumentException {
		return false;
	}
	
	public static SocketFactory getSocketFactory ()
	{
		return new SocksSocketFactory (DEFAULT_HOST, DEFAULT_PORT);
	}

}
