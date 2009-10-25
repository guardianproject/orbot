package net.sourceforge.jsocks.socks.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
  This class implements SOCKS5 User/Password authentication scheme as
  defined in rfc1929,the server side of it.
*/
public class UserPasswordAuthenticator extends  ServerAuthenticatorNone{

   static final int METHOD_ID = 2;

   UserValidation validator;

   /**
    Construct a new UserPasswordAuthentication object, with given
    UserVlaidation scheme.

    @param validator UserValidation to use for validating users.
   */
   public UserPasswordAuthenticator(UserValidation validator){
      this.validator = validator;
   }

   @Override
   public ServerAuthenticator startSession(Socket s) throws IOException{
     InputStream in = s.getInputStream();
     OutputStream out = s.getOutputStream();

     if(in.read() != 5) return null; //Drop non version 5 messages.

     if(!selectSocks5Authentication(in,out,METHOD_ID)) 
       return null;
     if(!doUserPasswordAuthentication(s,in,out))
       return null;

     return new ServerAuthenticatorNone(in,out);
   }


//Private Methods
//////////////////

   private boolean doUserPasswordAuthentication(Socket s,
                                                InputStream in,
                                                OutputStream out) 
                                                throws IOException{
     int version = in.read();
     if(version != 1) return false;
     int ulen = in.read();
     if(ulen < 0) return false;
     byte[] user = new byte[ulen];
     in.read(user);
     int plen = in.read();
     if(plen < 0) return false;
     byte[] password = new byte[plen];
     in.read(password);

     if(validator.isUserValid(new String(user), new String(password),s)){
       //System.out.println("user valid");
       out.write(new byte[]{1,0});
     }else{
       //System.out.println("user invalid");
       out.write(new byte[]{1,1});
       return false;
     }

     return true;
   }
}
