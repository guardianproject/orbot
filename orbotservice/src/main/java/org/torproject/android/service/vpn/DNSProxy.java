package org.torproject.android.service.vpn;

import static org.xbill.DNS.Section.ANSWER;

import android.util.Log;

import org.torproject.android.service.OrbotService;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DNSProxy {

    private boolean keepRunning = false;
    private final SimpleResolver mResolver;

    public DNSProxy (String localDns, int localPort) throws UnknownHostException, IOException {
        mResolver = new SimpleResolver(localDns);
        mResolver.setPort(localPort);
    }

    public void startProxy (String serverHost, int serverPort) {
        keepRunning = true;

        Thread mThread = new Thread() {
            public void run() {
                startProxyImpl(serverHost, serverPort);
            }
        };

        mThread.start();
    }

    public void stopProxy () {
        keepRunning = false;
    }

    private void startProxyImpl (String serverHost, int serverPort) {
        try {
            DatagramSocket server_socket = new DatagramSocket(serverPort, InetAddress.getByName(serverHost));

            byte[] receive_data = new byte[1024];


            while(keepRunning) {   //waiting for a client request...
                try {
                    //receiving the udp packet of data from client and turning them to string to print them
                    DatagramPacket receive_packet = new DatagramPacket(receive_data, receive_data.length);
                    server_socket.receive(receive_packet);

                    Message msgRequest = new Message(receive_data);
                    String given_hostname = msgRequest.getQuestion().getName().toString();

                    Record queryRecord = Record.newRecord(Name.fromString(given_hostname), Type.A, DClass.IN);
                    Message queryMessage = Message.newQuery(queryRecord);

                    Message answer = mResolver.send(queryMessage);
//                     String found_address = answer.getSection(ANSWER).get(0).rdataToString();
//                     Log.d("DNSProxy","resolved " + given_hostname + " to " + found_address);

                    answer.getHeader().setID(msgRequest.getHeader().getID());
                    final byte[] send_data = answer.toWire();

                    //getting the address and port of client
                    InetAddress client_address = receive_packet.getAddress();
                    int client_port = receive_packet.getPort();
                    DatagramPacket send_packet = new DatagramPacket(send_data, send_data.length, client_address, client_port);
                    server_socket.send(send_packet);
                }
                catch (Exception e) {
                    Log.e("DNSProxy","error resolving host",e);
                }
            }
        } catch (Exception e) {
            Log.e("DNSProxy","error running DNSProxy server",e);
        }
    }
}
