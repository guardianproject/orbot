package org.torproject.android.service.vpn;

import static org.torproject.android.service.vpn.OrbotVpnManager.FAKE_DNS;
import static org.torproject.android.service.vpn.OrbotVpnManager.FAKE_DNS_HEX;

import android.net.VpnService;
import android.util.Log;

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.UdpPort;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class DNSProxy {

    private static final String TAG = "DNSProxy";

    private final SimpleResolver mResolver;
    private DatagramSocket serverSocket;
    private Thread mThread;
    private VpnService mVpnService;
    private boolean keepRunning = false;

    public DNSProxy (String localDns, int localPort, VpnService service) throws UnknownHostException, IOException {
        mResolver = new SimpleResolver(localDns);
        mResolver.setPort(localPort);
        mVpnService = service;
    }

    public void startProxy (String serverHost, int serverPort) {
        mThread = new Thread() {
            @Override
            public void run() {
                startProxyImpl(serverHost, serverPort);
            }

            @Override
            public void interrupt() {
                super.interrupt();
                if (serverSocket != null) {
                    serverSocket.close();
                }
            }
        };

        mThread.start();
    }

    public void stopProxy() {
        keepRunning = false;

        if (mThread != null) {
            mThread.interrupt();
        }
    }

    public byte[] processDNS (byte[] payload) throws IOException {

        Message msgRequest = new Message(payload);

        if (msgRequest.getQuestion() != null) {
            Message queryMessage = Message.newQuery(msgRequest.getQuestion());
            Message answer = mResolver.send(queryMessage);
            byte[] respData = answer.toWire();
            return respData;

        }
        else
            return null;
    }

    private void startProxyImpl (String serverHost, int serverPort) {
        try {
            serverSocket = new DatagramSocket(serverPort, InetAddress.getByName(serverHost));
          //  mVpnService.protect(serverSocket);

            byte[] receive_data = new byte[1024];

            keepRunning = true;

            while (keepRunning) {   //waiting for a client request...
                try {
                    //receiving the udp packet of data from client and turning them to string to print them
                    DatagramPacket receive_packet = new DatagramPacket(receive_data, receive_data.length);
                    serverSocket.receive(receive_packet);

                    Message msgRequest = new Message(receive_data);
                    String given_hostname = msgRequest.getQuestion().getName().toString();
                    Message queryMessage = Message.newQuery(msgRequest.getQuestion());

                    Message answer = mResolver.send(queryMessage);
//                     String found_address = answer.getSection(ANSWER).get(0).rdataToString();
//                     Log.d("DNSProxy","resolved " + given_hostname + " to " + found_address);

                    answer.getHeader().setID(msgRequest.getHeader().getID());

                    final byte[] send_data = answer.toWire();

                    //getting the address and port of client
                    InetAddress client_address = receive_packet.getAddress();
                    int client_port = receive_packet.getPort();
                    DatagramPacket send_packet = new DatagramPacket(send_data, send_data.length, client_address, client_port);
                    serverSocket.send(send_packet);

                  //  byte[] pData = send_packet.getData();

                }
                catch (SocketException e) {
                    if (e.toString().contains("Socket closed")) {
                        break;
                    }
                    Log.e("DNSProxy","error resolving host",e);
                }
            }
        } catch (Exception e) {
            Log.e("DNSProxy","error running DNSProxy server",e);
        }
    }



    public boolean isDNS (IpPacket p)
    {

        if (p.getHeader().getProtocol()== IpNumber.UDP) {
            UdpPacket up = (UdpPacket) p.getPayload();
            if (up.getHeader().getDstPort() == UdpPort.DOMAIN)
                return true;
        }

        return false;

    }



}
