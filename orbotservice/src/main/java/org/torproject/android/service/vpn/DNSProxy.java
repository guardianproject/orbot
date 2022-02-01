package org.torproject.android.service.vpn;

import android.net.VpnService;
import android.text.TextUtils;
import android.util.Log;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Iterator;

public class DNSProxy {

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

    public byte[] processDNS (byte[] receive_data) throws IOException {
        DatagramPacket receive_packet = new DatagramPacket(receive_data, receive_data.length);

        Message msgRequest = new Message(receive_packet.getData());

        if (msgRequest.getQuestion() != null) {
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

            return send_packet.getData();
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

                //    Tun2Socks.sendDNSResponse(pData,pData.length);
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
}
