package net.sourceforge.jsocks.test;

import net.sourceforge.jsocks.socks.*;
import net.sourceforge.jsocks.socks.server.*;

import java.net.Socket;

/** Test file for UserPasswordAuthentictor */

public class UPSOCKS implements UserValidation{
    String user, password;

    UPSOCKS(String user,String password){
       this.user = user;
       this.password = password;
    }

    public boolean isUserValid(String user,String password,Socket s){
       System.err.println("User:"+user+"\tPassword:"+password);
       System.err.println("Socket:"+s);
       return (user.equals(this.user) && password.equals(this.password));
    }

    public static void main(String args[]){
        String user, password;

        if(args.length == 2){
          user = args[0];
          password = args[1];
        }else{
          user = "user";
          password = "password";
        }

        UPSOCKS us = new UPSOCKS(user,password);
        UserPasswordAuthenticator auth = new UserPasswordAuthenticator(us);
        ProxyServer server = new ProxyServer(auth);

        server.start(1080);
    }
}
