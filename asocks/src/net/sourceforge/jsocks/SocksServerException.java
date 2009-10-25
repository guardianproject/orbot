package net.sourceforge.jsocks;

public class SocksServerException extends Exception {

  String message;

  public SocksServerException(Exception e) {
    this.message = e.getMessage();
  }

  public SocksServerException(String message) {
    this.message = message;
  }
  
  @Override
  public String getMessage() {
    return message;
  }
}
