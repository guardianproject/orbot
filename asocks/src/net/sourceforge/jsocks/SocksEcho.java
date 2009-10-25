package net.sourceforge.jsocks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import net.sourceforge.jsocks.socks.Proxy;
import net.sourceforge.jsocks.socks.Socks5DatagramSocket;
import net.sourceforge.jsocks.socks.Socks5Proxy;
import net.sourceforge.jsocks.socks.SocksServerSocket;
import net.sourceforge.jsocks.socks.SocksSocket;

public class SocksEcho 
                       implements 
                                  Runnable
                                  {

//Network related members
   Proxy proxy=null;
   int port;
   String host;
   Thread net_thread=null;
   InputStream in=null;
   OutputStream out=null;
   Socket sock=null;
   ServerSocket server_sock = null;
   Socks5DatagramSocket udp_sock;

   Object net_lock = new Object();
   int mode=COMMAND_MODE;

   //Possible mode states.
   static final int LISTEN_MODE		= 0;
   static final int CONNECT_MODE	= 1;
   static final int UDP_MODE		= 2;
   static final int COMMAND_MODE	= 3;
   static final int ABORT_MODE		= 4;

   //Maximum datagram size
   static final int MAX_DATAGRAM_SIZE	= 1024;

// Constructors
////////////////////////////////////
   public SocksEcho(){
    

          
   }

//Runnable interface
///////////////////////////////

public void run(){
    boolean finished_OK = true;
    try{
      switch(mode){
        case UDP_MODE:
           startUDP();
           doUDPPipe();
           break;
        case LISTEN_MODE:
           doAccept();
           doPipe();
           break;
        case CONNECT_MODE:
           doConnect();
           doPipe();
           break;
        default:
          warn("Unexpected mode in run() method");
      }

   }catch(UnknownHostException uh_ex){
        if(mode != ABORT_MODE){
	  finished_OK = false;
	  status("Host "+host+" has no DNS entry.");
	  uh_ex.printStackTrace();
       }
   }catch(IOException io_ex){
       if(mode != ABORT_MODE){
	  finished_OK = false;
          status(""+io_ex);
          io_ex.printStackTrace();
      }
   }finally{
      if(mode == ABORT_MODE) status("Connection closed");
      else if(finished_OK) status("Connection closed by foreign host.");

      onDisconnect();
   }
}

//Private methods
////////////////////////////////////////////////////////////////////////

// GUI event handlers.
//////////////////////////

   private void onConnect(){
      if(mode == CONNECT_MODE){
        status("Diconnecting...");
        abort_connection();
        return;
      }else if(mode != COMMAND_MODE)
        return;

//      if(!readHost()) return;
  //    if(!readPort()) return;

      if(proxy == null){
         warn("Proxy is not set");
         onProxy();
         return;
      }

      startNetThread(CONNECT_MODE);
      status("Connecting to "+host+":"+port+"  ...");

   }

   private void onDisconnect(){
      synchronized(net_lock){
         mode = COMMAND_MODE;
       
         server_sock = null;
         sock = null;
         out = null;
         in = null;
         net_thread = null;
      }
   }

   private void onAccept(){
      if(mode == LISTEN_MODE){
         abort_connection();
         return;
      }else if(mode != COMMAND_MODE) return;

   //   if(!readHost()) return;
     // if(!readPort()) port = 0;

      if(proxy == null){
         warn("Proxy is not set");
         onProxy();
         return;
      }

      startNetThread(LISTEN_MODE);

   }

   private void onUDP(){
      if(mode == UDP_MODE){
        abort_connection();
        return;
      }else if(mode == ABORT_MODE) return;

      if(proxy == null){
         warn("Proxy is not set");
         onProxy();
         return;
      }

      startNetThread(UDP_MODE);
   }

   private void onInput(){
      String send_string = "";
      switch(mode){
        case ABORT_MODE:  //Fall through
        case COMMAND_MODE:
          return;
        case CONNECT_MODE://Fall through
        case LISTEN_MODE:
          synchronized(net_lock){
            if(out == null) return;
            send(send_string);
          }
          break;
        case UDP_MODE:
     //     if(!readHost()) return;
      //    if(!readPort()) return;
          sendUDP(send_string,host,port);
          break;
        default:
          print("Unknown mode in onInput():"+mode);

      }
      print(send_string);
   }

   private void onClear(){
    
   }
   private void onProxy(){
      Proxy p;
      p = null;//socks_dialog.getProxy(proxy);
      if(p != null) proxy = p;
      if( proxy != null && proxy instanceof Socks5Proxy) 
         ((Socks5Proxy) proxy).resolveAddrLocally(false);
   }
   private void onQuit(){
     
      System.exit(0);
   }

//Data retrieval functions
//////////////////////////

   /**
    * Reads the port field, returns false if parsing fails.
    */
   private boolean readPort(int newPort){
      try{
         port = newPort;
      }catch(NumberFormatException nfe){
         warn("Port invalid!");
         return false;
      }
      return true;
   }
   private boolean readHost(){
      host = "";
      host.trim();
      if(host.length() < 1){
        warn("Host is not set");
        return false;
      }
      return true;
   }

//Display functions
///////////////////

   private void status(String status){
      
   }
   private void println(String s){
     
   }
   private void print(String s){
  
   }
   private void warn(String s){
      status(s);
      //System.err.println(s);
   }

//Network related functions
////////////////////////////

   private void startNetThread(int m){
      mode = m;
      net_thread = new Thread(this);
      net_thread.start();
   }

   private void abort_connection(){
      synchronized(net_lock){ 
         if(mode == COMMAND_MODE) return;
         mode = ABORT_MODE;
         if(net_thread!=null){
            try{ 
              if(sock!=null) sock.close();
              if(server_sock!=null) server_sock.close();
              if(udp_sock!=null) udp_sock.close();
            }catch(IOException ioe){
            }
            net_thread.interrupt();
            net_thread = null;
         }
      }
   }

   private void doAccept() throws IOException{

     println("Trying to accept from "+host);
     status("Trying to accept from "+host);
     println("Using proxy:"+proxy);
     server_sock = new SocksServerSocket(proxy,host,port);

     //server_sock.setSoTimeout(30000);

     println("Listenning on: "+server_sock.getInetAddress()+
                          ":" +server_sock.getLocalPort());
     sock = server_sock.accept();
     println("Accepted from:"+sock.getInetAddress()+":"+
                              sock.getPort());

     status("Accepted from:"+sock.getInetAddress().getHostAddress()
                        +":"+sock.getPort());
                                           
     server_sock.close(); //Even though this doesn't do anything
   }
   private void doConnect() throws IOException{
     println("Trying to connect to:"+host+":"+port);
     println("Using proxy:"+proxy);
     sock = new SocksSocket(proxy,host,port);
     println("Connected to:"+sock.getInetAddress()+":"+port);
     status("Connected to: "+sock.getInetAddress().getHostAddress()
                       +":" +port);
     println("Via-Proxy:"+sock.getLocalAddress()+":"+
                          sock.getLocalPort());

   }
   private void doPipe() throws IOException{
      out = sock.getOutputStream();
      in = sock.getInputStream();

      byte[] buf = new byte[1024];
      int bytes_read;
      while((bytes_read = in.read(buf)) > 0){
          print(new String(buf,0,bytes_read));
      }

   }
   private void startUDP() throws IOException{
     udp_sock = new Socks5DatagramSocket(proxy,0,null);
     println("UDP started on "+udp_sock.getLocalAddress()+":"+
                               udp_sock.getLocalPort());
     status("UDP:"+udp_sock.getLocalAddress().getHostAddress()+":"
                  +udp_sock.getLocalPort());
   }

   private void doUDPPipe() throws IOException{
     DatagramPacket dp = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE],
                                            MAX_DATAGRAM_SIZE);
     while(true){
       udp_sock.receive(dp);
       print("UDP\n"+
	     "From:"+dp.getAddress()+":"+dp.getPort()+"\n"+
	     "\n"+
             //Java 1.2
             //new String(dp.getData(),dp.getOffset(),dp.getLength())+"\n"
             //Java 1.1
             new String(dp.getData(),0,dp.getLength())+"\n"
            );
       dp.setLength(MAX_DATAGRAM_SIZE);
     }
   }
   
   private void sendUDP(String message,String host,int port){
      if(!udp_sock.isProxyAlive(100)){
         status("Proxy closed connection");
         abort_connection();
         return;
      }

      try{
         byte[] data = message.getBytes();
         DatagramPacket dp = new DatagramPacket(data,data.length,null,port);
         udp_sock.send(dp,host);
      }catch(UnknownHostException uhe){
         status("Host "+host+" has no DNS entry.");
      }catch(IOException ioe){
         status("IOException:"+ioe);
         abort_connection();
      }

   }

   private void send(String s){
      try{
        out.write(s.getBytes());
      }catch(IOException io_ex){
        println("IOException:"+io_ex);
        abort_connection();
      }
   }



   


// Main 
////////////////////////////////////
   public static void main(String[] args){
      SocksEcho socksecho = new SocksEcho();
    
   }
 }//end class
