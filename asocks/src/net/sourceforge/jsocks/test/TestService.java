package net.sourceforge.jsocks.test;

import java.io.*;
import java.net.Socket;

import net.*;

/**
  Server to used perform tests for SOCKS library.
*/
public class TestService implements Runnable{
  static final String chargenSequence = " !\"#$%&'()*+,-./0123456789:;<=>?@"+
  "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefg";

  static final String serviceNames[] = {"echo","discard","chargen","connect"};
  static final int    servicePorts[] = {5678,5679,5680,5681};

  static final int ECHO		= 0;
  static final int DISCARD	= 1;
  static final int CHARGEN	= 2;
  static final int CONNECT	= 3;

  static final int BUF_SIZE	= 1024;

  static final int CHARGEN_WAIT	= 1000;   //1 second
  static final int MAX_WAIT	= 60000;  //1 minute

  static PrintStream log	= null;

  //Class constants
  Socket s;
  int service;

  /**
   Creates new TestService object, which will perform particular
   service on given socket.

   @param s Socket on which to perform service.
   @param service Service which to provide.
  */
  public TestService(Socket s, int service){
    this.s = s;
    this.service = service;
  }

  /**
   Default constructor.
  */
  public TestService(){
    this.s = null;
    this.service = -1;
  }

  public void run(){
     try{
       serve(s,service);
     }catch(IOException ioe){
	 log("Exception:"+ioe);
	 ioe.printStackTrace();
     }
     try{ s.close();}catch(IOException ioe){}
  }

  //Static functions
  /////////////////

  /**
   Maps service name to the integer id, does it in simple
   linear search manner.
   @param serviceName Name of the service whose id one needs.
   @return Integer identifier for this servuce, or -1, if service
           can't be found.
  */
  static public int getServiceId(String serviceName){
     serviceName = serviceName.toLowerCase();
     for(int i = 0;i < serviceNames.length;++i)
        if(serviceName.equals(serviceNames[i]))
           return i;

     //Couldn't find one.
     return -1;
  }

  /**
    Performs given service on given socket.
    <p>
    Simply looks up and calls associated method.
    @param s Socket on which to perform service.
    @param service Id of the service to perform.
    @return true if service have been found, false otherwise.
  */
  static public boolean serve(Socket s, int service) throws IOException{
    switch(service){
      case ECHO:
        echo(s);
      break;
      case DISCARD:
        discard(s);
      break;
      case CHARGEN:
        chargen(s,CHARGEN_WAIT,MAX_WAIT);
      break;
      case CONNECT:
        connect(s);
      break;
      default:
         log("Unknown service:"+service);
	 return false;
    }
     return true;
  }
  /**
    Echos any input on the socket to the output.
    Echo is being done line by line.
    @param s Socket on which to perform service.
  */
  static public void echo(Socket s) throws IOException{
    BufferedReader in = new BufferedReader(new InputStreamReader(
					       s.getInputStream()));
    OutputStream out = s.getOutputStream();

    log("Starting \"echo\" on "+s);

    String line = in.readLine();
    while(line != null){
       out.write((line+"\n").getBytes());
       log(line);
       line = in.readLine();
    }

    log("Echo done.");
  }

  /**
    Reads input from the socket, and does not write anything back.
    logs input in line by line fashion.
    @param s Socket on which to perform service.
  */
  static public void discard(Socket s) throws IOException{
    BufferedReader in = new BufferedReader(new InputStreamReader(
					       s.getInputStream()));
    log("Starting discard on "+s);

    String line = in.readLine();
    while(line != null){
       log(line);
       line = in.readLine();
    }

    log("Discard finished.");
  }

  /**
    Generates characters and sends them to the socket.
    <p>
    Unlike usual chargen (port 19), each next line of the generated
    output is send after increasingly larger time intervals. It starts
    from wait_time (ms), and each next time wait time is doubled.
    Eg. 1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 ... well
    you got the idea.
    <p>
    It starts if either connection is clsoed or the wait time grows
    bigger than max_wait.

    @param s Socket on which to perform service.
    @param wait_time Time in ms, from which timing sequence should begin.
    @param max_wait Time in ms, after reaching timeout greater than this
		    value, chargen will stop.
  */
  static public void chargen(Socket s,long wait_time,long max_wait)
		     throws IOException{
    byte[] buf = chargenSequence.getBytes();
    int pos = 0;
    OutputStream out = s.getOutputStream();
    InputStream in = s.getInputStream();
    s.setSoTimeout(100); //0.1 ms

    log("Starting \"chargen\" on "+s);
    while(true){
       log("Sending message.");
       out.write(buf,pos,buf.length - pos);
       out.write(buf,0,pos);       
       out.write("\n".getBytes());
       pos++;
       try{
	  if(wait_time > max_wait) break;

	  log("Going to sleep for "+wait_time+" ms.");
          Thread.currentThread().sleep(wait_time);
	  wait_time *= 2;
	  if(in.read() < 0) break; //Connection closed
       }catch(InterruptedException ie){
       }catch(InterruptedIOException ioe){
       }
    }
    log("Chargen finished.");
  }

  /**
    Models connect back situation.
    <p>
    Reads a line from the socket connection, line should be in the
    form port service_id. Connects back to the remote host to port
    specified in the line, if successful performs a service speciefied
    by id on that new connection. If accept was successful closes the
    control connection, else outputs error message, and then closes
    the connection.

    @param s Control connection.
  */
  static public void connect(Socket s)throws IOException{
    String line = null;
    Socket sock;
    int port;
    int service_id;

    BufferedReader in = new BufferedReader(new InputStreamReader(
					       s.getInputStream()));
    OutputStream out = s.getOutputStream();

    log("Starting \"connect\" on "+s);
    line = in.readLine();
    if(line == null) return; //They closed connection

    java.util.StringTokenizer st = new java.util.StringTokenizer(line);
    if(st.countTokens() < 2){ //We need at least 'port' and "id"
       out.write("Expect: port serviceId.\n".getBytes());
       log("Invalid arguments.");
       return;
    }
    try{
       port = Integer.parseInt(st.nextToken());
       service_id = Integer.parseInt(st.nextToken());
    }catch(NumberFormatException nfe){
       out.write("Expect: port serviceId.\n".getBytes());
       log("Invalid arguments.");
       return;
    }
    try{
      log("Connecting to "+s.getInetAddress()+":"+port);
      sock = new Socket(s.getInetAddress(),port);
    }catch(IOException ioe){
      out.write(("Connect to "+s.getInetAddress()+
		 ":"+port+" failed").getBytes());
      log("Connect failed.");
      return;
    }
    s.close();
    log("About to serve "+service_id);
    serve(sock,service_id);
  }

  /**
    Pipes data from the input stream to the output.
    @param in  Input stream.
    @param out Output stream.
  */
  static public void pipe(InputStream in, OutputStream out)
                          throws IOException{
    byte[] buf = new byte[BUF_SIZE];
    int bread = 0;
    while(bread >= 0){
       bread = in.read(buf);
       out.write(buf,0,bread);
    }
  }

  /**
    Performs logging.
  */
  static synchronized void log(String s){
     if(log != null) log.println(s);
  }

}
