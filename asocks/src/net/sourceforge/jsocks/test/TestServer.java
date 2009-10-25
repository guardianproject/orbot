package net.sourceforge.jsocks.test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import net.*;

/**
  Server to used perform tests for SOCKS library.
*/
public class TestServer implements Runnable{
  static PrintStream log	= null;

  int service;


  /**
   Creates a TestServer object which will listen on the port associated
   with given service.

   @param service Service to provide
  */
  public TestServer(int service){
    this.service = service;
  }

  public void run(){
     try{
         server(service);
     }catch(IOException ioe){
	 log("Exception:"+ioe);
	 ioe.printStackTrace();
     }
  }
  //Static functions
  /////////////////


  /**
    Listens on the port associated with given service.
    <p>
    When connection is accepted, speciefied service is performed.
    It is being done in separate thread.
    @return Never returns.
  */
  static public void server(int service) throws IOException{
     ServerSocket ss = new ServerSocket(TestService.servicePorts[service]);
     Socket s;

     s = ss.accept();
     while(s!=null){
	TestService st = new TestService(s,service);
	Thread t = new Thread(st);
	t.start();
	s = ss.accept();
     }
  }


  /**
    Performs logging.
  */
  static synchronized void log(String s){
     if(log != null) log.println(s);
  }

  //Main Function
  ///////////////
  public static void main(String[] args){
      log = System.out;
      TestService.log = log;

      TestServer st;
      for( int i = 0; i< TestService.serviceNames.length;++i){
	 log("Starting service "+TestService.serviceNames[i]+" at port "+
	      TestService.servicePorts[i]+".");
         st = new TestServer(i);
	 Thread t = new Thread(st);
	 t.start();
      }
  }
}
