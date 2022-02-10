package org.torproject.android.service.vpn;

import static org.torproject.android.service.vpn.OrbotVpnManager.FAKE_DNS;
import static org.torproject.android.service.vpn.OrbotVpnManager.FAKE_DNS_HEX;

import android.net.VpnService;
import android.util.Log;

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
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
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    public byte[] processDNS (byte[] payload) throws IOException {

        Message msgRequest = new Message(payload);

        if (msgRequest.getQuestion() != null) {
            Message queryMessage = Message.newQuery(msgRequest.getQuestion());

            Message answer = mResolver.send(queryMessage);
            answer.getHeader().setID(msgRequest.getHeader().getID());
            byte[] respData = answer.toWire();
            DatagramPacket resp = new DatagramPacket(respData, respData.length, InetAddress.getLocalHost(),8883);
            return resp.getData();

        }
        else
            return null;
    }

    private void startProxyImpl (String serverHost, int serverPort) {
        try {
            serverSocket = new DatagramSocket(serverPort, InetAddress.getByName(serverHost));
          //  mVpnService.protect(serverSocket);

            byte[] receive_data = new byte[1024];

            while (true) {   //waiting for a client request...
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

    public void processPacket (byte[] packet)
    {
        Packet.IP ip = new Packet.IP(packet, 0, packet.length);
        Packet.TCP tcp = new Packet.TCP(packet, ip.ihl, packet.length - ip.ihl);

       // Packet.recalcIPCheckSum(packet, 0, ip.ihl);
        //Packet.recalcTCPCheckSum(packet, 0, ip.tot_len);
    }

    public UdpPacket isDNS (byte[] packet)
    {
        try {
            IpV4Packet p = IpV4Packet.newPacket(packet,0,packet.length);
            if (p.getHeader().getProtocol()== IpNumber.UDP) {
                UdpPacket up = UdpPacket.newPacket(packet, 0, packet.length);

               // Log.d(TAG,"dest address: " + p.getHeader().getDstAddr().toString());

                if (p.getHeader().getDstAddr().toString().contains(FAKE_DNS))
                    return up;


                //return up.getHeader().getDstPort().value() == 53;
            }

        } catch (IllegalRawDataException e) {
            e.printStackTrace();
        }

        return null;

        /**
        Packet.IP ip = new Packet.IP(packet, 0, packet.length);
        //return ip.protocol == 17;
        return Integer.toString(ip.daddr,16).equals(FAKE_DNS_HEX);
         **/

    }


    private void debugPacket (Packet.IP ip, Packet.TCP tcp)
    {
        System.out.format("version: %d\n", ip.version);
        System.out.format("ihl: %d\n", ip.ihl);
        System.out.format("tos: %X\n", ip.tos);
        System.out.format("tot_len: %d\n", ip.tot_len);
        System.out.format("id: %X\n", ip.id);
        System.out.format("frag_off: %X\n", ip.frag_off);
        System.out.format("ttl: %d\n", ip.ttl);
        System.out.format("protocol: %d\n", ip.protocol);
        System.out.format("check: %X\n", ip.check);
        System.out.format("saddr: %X\n", ip.saddr);
        System.out.format("daddr: %X\n", ip.daddr);

        if (tcp != null ) {
            System.out.format("source: %d\n", tcp.source);
            System.out.format("dest: %d\n", tcp.dest);
            System.out.format("seq: %d\n", tcp.seq);
            System.out.format("ack_seq: %d\n", tcp.ack_seq);
            System.out.format("doff: %d\n", tcp.doff);
            System.out.format("res1: %X\n", tcp.res1);
            System.out.format("cwr: %d\n", tcp.cwr);
            System.out.format("ecn: %d\n", tcp.ecn);
            System.out.format("urg: %d\n", tcp.urg);
            System.out.format("ack: %d\n", tcp.ack);
            System.out.format("psh: %d\n", tcp.psh);
            System.out.format("rst: %d\n", tcp.rst);
            System.out.format("syn: %d\n", tcp.syn);
            System.out.format("fin: %d\n", tcp.fin);
            System.out.format("window: %d\n", tcp.window);
            System.out.format("tcp check: %X\n", tcp.check);
            System.out.format("urg_ptr: %X\n", tcp.urg_ptr);
        }
    }
}
