package net.sourceforge.jsocks.test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.*;

import net.sourceforge.jsocks.socks.*;

/** SOCKS aware UDP echo client.<br>
    Reads input line by line and sends it to given on command line
    host and port, using given proxy, then blocks until reply datagram
    recieved, not really echo, single threaded client, I just used it
    for testing before UDP actually worked.
*/
public class SocksUDPEcho{

   public static void usage(){
      System.err.print(
    "Usage: java SocksUDPEcho host port [socksHost socksPort]\n");
   }

   static final int defaultProxyPort = 1080;           //Default Port
   static final String defaultProxyHost = "www-proxy"; //Default proxy

   public static void main(String args[]){
      int port;
      String host;
      int proxyPort;
      String proxyHost;
      InetAddress ip;

      if(args.length > 1 && args.length < 5){
	 try{

	     host = args[0];
	     port = Integer.parseInt(args[1]);

	     proxyPort =(args.length > 3)? Integer.parseInt(args[3])	     
	                                 : defaultProxyPort;

	     host = args[0];
	     ip = InetAddress.getByName(host);

	     proxyHost =(args.length > 2)? args[2]
	                                 : defaultProxyHost;

	     Proxy.setDefaultProxy(proxyHost,proxyPort);
	     Proxy p = Proxy.getDefaultProxy();
	     p.addDirect("lux");


	     DatagramSocket ds = new Socks5DatagramSocket();
	                             

	     BufferedReader in = new BufferedReader(
				 new InputStreamReader(System.in));
             String s;

             System.out.print("Enter line:");
             s = in.readLine();

	     while(s != null){
                byte[] data = (s+"\r\n").getBytes();
                DatagramPacket dp = new DatagramPacket(data,0,data.length,
                                        ip,port);
                System.out.println("Sending to: "+ip+":"+port);
                ds.send(dp);
	        dp = new DatagramPacket(new byte[1024],1024);

	        System.out.println("Trying to recieve on port:"+
	                            ds.getLocalPort());
	        ds.receive(dp);
	        System.out.print("Recieved:\n"+
	                         "From:"+dp.getAddress()+":"+dp.getPort()+
	                         "\n\n"+
                new String(dp.getData(),dp.getOffset(),dp.getLength())+"\n"
                );
                System.out.print("Enter line:");
                s = in.readLine();

	     }
	     ds.close();
	     System.exit(1);

	 }catch(SocksException s_ex){
	   System.err.println("SocksException:"+s_ex);
	   s_ex.printStackTrace();
	   System.exit(1); 
	 }catch(IOException io_ex){
	   io_ex.printStackTrace();
	   System.exit(1);
	 }catch(NumberFormatException num_ex){
	   usage();
	   num_ex.printStackTrace();
	   System.exit(1);
	 }

      }else{
	usage();
      }
   }
}
