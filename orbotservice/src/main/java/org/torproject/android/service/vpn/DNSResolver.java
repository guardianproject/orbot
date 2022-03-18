package org.torproject.android.service.vpn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DNSResolver {

    private final int mPort;
    private InetAddress mLocalhost = null;

    public DNSResolver(int localPort) {
        mPort = localPort;
    }

    public byte[] processDNS(byte[] payload) throws IOException {

        if (mLocalhost == null)
            mLocalhost = InetAddress.getLocalHost();

        DatagramPacket packet = new DatagramPacket(
                payload, payload.length, mLocalhost, mPort
        );
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(packet);

        // Await response from DNS server
        byte[] buf = new byte[1024];
        packet = new DatagramPacket(buf, buf.length);
        datagramSocket.receive(packet);

        return packet.getData();
    }

}
