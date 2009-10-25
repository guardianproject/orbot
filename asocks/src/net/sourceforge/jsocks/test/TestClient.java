package net.sourceforge.jsocks.test;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;

import net.sourceforge.jsocks.socks.*;
import net.sourceforge.jsocks.socks.Proxy;

public class TestClient extends TestService{
   /** Proxy which should be used*/
   Proxy proxy;
   /** Host on which TestServer is running*/
   String testHost;

   int timeout = 15000;
   int acceptTimeout = 0;

   BufferedReader in;
   Writer out;

   public TestClient(Proxy p,String testHost){
      this.proxy = p;
      this.testHost = testHost;
      if(log == null) log = System.out;
   }

   public void start(){
      connectTests(true);
      acceptTests(true);
      udpTests(true);

      connectTests(false);
      acceptTests(false);
      udpTests(false);
   }

   void connectTests(boolean useString){
      try{
         open(ECHO, useString);
         testEcho();
         s.close();

         open(DISCARD, useString);
         testDiscard();
         s.close();

         open(CHARGEN, useString);

         for(int i = 0; i< 3;){
            try{ 
               testChargen();
               break;
            }catch(InterruptedIOException ioe){
               log("IO interrupted:"+i);
               i++;
            }
         }

         s.close();

      }catch(IOException ioe){
         ioe.printStackTrace();
      }

   }

   void acceptTests(boolean useString){
      try{
         testAccept(ECHO, useString);
         testEcho();
         s.close();

         testAccept(DISCARD, useString);
         testDiscard();
         s.close();

         testAccept(CHARGEN, useString);

         for(int i = 0; i< 3;){
            try{ 
               testChargen();
               break;
            }catch(InterruptedIOException ioe){
               log("IO interrupted:"+i);
               i++;
            }
         }
         s.close();

      }catch(IOException ioe){
         ioe.printStackTrace();
      }
   }

   void udpTests(boolean useString){
      log("Udp tests are not yet implemented");
   }

   void testEcho() throws IOException{
      log("Testing echo.");
      for(int i=0;i<5;++i){
         out.write("String number "+i+"\r\n");
         out.flush();
         log("Echo:"+in.readLine());;
      }
      log("Echo finished");
   }

   void testDiscard() throws IOException{
      log("Testing discard");
      for(int i =0; i < 5;++i){
         log("Sending discard message:"+i);
         out.write("Discard message:"+i+"\r\n");
         out.flush();
      }
      log("Discard finished");
   }

   void testChargen() throws IOException{
      log("Testing chargen");
      String s;
      s = in.readLine();
      while(s!=null){
         log("ChGen:"+s);
         s = in.readLine();
      }
      log("Chargen finished.");
   }

   void testAccept(int service,boolean useString)throws IOException{
      open(CONNECT,useString);

      log("Testing accept");
      ServerSocket ss;

      if(useString)
        ss = new SocksServerSocket(proxy,testHost,servicePorts[service]);
      else
        ss = new SocksServerSocket(proxy,InetAddress.getByName(testHost),
                                   servicePorts[service]);
      log("Listenning on "+ss.getInetAddress()+":"+ss.getLocalPort());
      ss.setSoTimeout(acceptTimeout);
 
      out.write(""+ss.getLocalPort()+" "+service+"\r\n");
      out.flush();

      String line = in.readLine();
      if(line != null){
        log("Accept failed:"+line);
      }

      s.close();

      s = ss.accept();
      log("Accepted:"+s);

      s.setSoTimeout(timeout);

      out = new OutputStreamWriter(s.getOutputStream());
      in  = new BufferedReader(new InputStreamReader(s.getInputStream()));

      ss.close();
   }

   void open(int service,boolean useString) throws IOException{

      if(!useString){
         s = new SocksSocket(proxy,InetAddress.getByName(testHost),
                                   servicePorts[service]);
      }else{
         s = new SocksSocket(proxy,testHost,servicePorts[service]);
      }

      s.setSoTimeout(timeout);

      out = new OutputStreamWriter(s.getOutputStream());
      in  = new BufferedReader(new InputStreamReader(s.getInputStream()));
   }

   //Main function
   ///////////////

   static void usage(){
      System.err.println(
      "Usage: java Testclient testhost proxy [directhosts]");
   }

   static Proxy initProxy(String ps){
      java.util.StringTokenizer st = new java.util.StringTokenizer(ps,",;");
      Proxy proxy = null;
      while(st.hasMoreElements()){
         String entry = st.nextToken();
         Proxy p = Proxy.parseProxy(entry);
         if( p == null){
           log("Proxy "+entry+" invalid.");
           return null;
         }
         p.setChainProxy(proxy);
         proxy = p;
      }
      return proxy;
   }
   static void addDirectHosts(Proxy p, String directHosts){
      java.util.StringTokenizer st = new java.util.StringTokenizer(
                                         directHosts,",;");

      while(st.hasMoreElements()){
         String entry = st.nextToken();
         log("Adding direct host:"+entry);
         p.addDirect(entry);
      }
   }

   public static void main(String[] argv){
      if(argv.length < 2){
        usage();
        return;
      }

      log = System.out;

      String testHost = argv[0];
      String proxyHost = argv[1];
      String directHosts = argv.length >2 ? argv[2] : null;

      Proxy p = initProxy(proxyHost);
      if(p == null){
         log("Can't init proxy.");
         return;
      }
      if(directHosts!=null) addDirectHosts(p,directHosts);

      if(p instanceof Socks5Proxy)
         ((Socks5Proxy) p).resolveAddrLocally(false);

      TestClient tc = new TestClient(p,testHost);
      tc.start();
      
   }
}
