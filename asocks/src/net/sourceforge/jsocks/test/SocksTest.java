package net.sourceforge.jsocks.test;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import net.sourceforge.jsocks.socks.*;
//import net.sourceforge.jsocks.socks.Proxy;

/** SOCKS aware echo client*/

public class SocksTest implements Runnable{
  
   private int port;
   private InetAddress hostIP;

   private Socket ss;
   private InputStream in;
   private OutputStream out;

   private static final int BUF_SIZE = 1024;
   static final int defaultProxyPort = 1080;           //Default Port
   static final String defaultProxyHost = "www-proxy"; //Default proxy

   public SocksTest(String host,int port) 
	  throws IOException,UnknownHostException,SocksException{
      this.port = port;

      ss = new SocksSocket(host, port);
      out = ss.getOutputStream();
      in  = ss.getInputStream();
      System.out.println("Connected...");
      System.out.println("TO: "+host+":"+port);
      System.out.println("ViaProxy: "+ss.getLocalAddress().getHostAddress()
                                 +":"+ss.getLocalPort());

   }

   public void close()throws IOException{
     ss.close();
   }
   public void send(String s) throws IOException{
      out.write(s.getBytes());
   }

   public void run(){
      byte[] buf = new byte[1024];
      int bytes_read;
      try{
	  while((bytes_read = in.read(buf)) > 0){
	     System.out.write(buf,0,bytes_read);
	  }
      }catch(IOException io_ex){
	 io_ex.printStackTrace();
      }
   }

   public static void usage(){
      System.err.print(
      "Usage: java SocksTest host port [socksHost socksPort]\n");
   }


   public static void main(String args[]){
      int port;
      String host;
      int proxyPort;
      String proxyHost;

      if(args.length > 1 && args.length < 5){
	 try{

	     host = args[0];
	     port = Integer.parseInt(args[1]);

	     proxyPort =(args.length > 3)? Integer.parseInt(args[3])	     
	                                 : defaultProxyPort;

	     host = args[0];
	     proxyHost =(args.length > 2)? args[2]
	                                 : defaultProxyHost;

	     Proxy.setDefaultProxy(proxyHost,proxyPort,"KOUKY001");
	     //Proxy.setDefaultProxy(proxyHost,proxyPort);
	     InetRange inetRange = new InetRange();
	     inetRange.add(InetAddress.getByName("localhost"));
	     Proxy.getDefaultProxy().setDirect(inetRange);


	     SocksTest st = new SocksTest(host,port);
	     Thread thread = new Thread(st);
	     thread.start();

	     BufferedReader in = new BufferedReader(
				 new InputStreamReader(System.in));
             String s;

             s = in.readLine();
	     while(s != null){
                st.send(s+"\r\n");
		//try{
		   //Thread.currentThread().sleep(10);
		//}catch(InterruptedException i_ex){
		//}
                s = in.readLine();
	     }
	     st.close();
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

}//End of class
