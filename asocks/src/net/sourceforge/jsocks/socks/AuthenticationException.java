/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package net.sourceforge.jsocks.socks;

/**
 *  Exception thrown by various socks classes to indicate errors
 *  with protocol or unsuccessful server responses.
 *  
 *  @author rayc@google.com (Ray Colline)
 */
public class AuthenticationException extends Throwable {
  
   private AuthErrorType errorType;
   private String errorString;
    
   /**
    * Create an AuthenticationException with the specified type
    * 
    * @param errorType an enum denoting what kind of auth error.
    */
   public AuthenticationException(AuthErrorType errorType) {
       this.errorType = errorType;
       this.errorString = errorType.toString();
   }
   
   /**
    * Create an AuthenticationException with both the specified type and
    * a free-form message
    * 
    * @param errorType an enum denoting what kind of auth error.
    * @param errorString a specific string detailing the error.
    */
   public AuthenticationException(AuthErrorType errorType, 
       String errorString) {
       this.errorType = errorType;
       this.errorString = errorString;
   }
   
   /**
    * Get the error type associated with this exception.
    * 
    * @return errorType the type associated with this exception.
    */
   public AuthErrorType getErrorType() {
      return errorType;
   }
   
   /**
    * Get human readable representation of this exception.
    * 
    * @return String representation of this exception.
    */
   @Override
   public String toString() {
      return errorString;
   }

   /**
    * Returns the message associated with this exception
    * 
    * @return String the error string.
    */
   @Override
   public String getMessage() {
     return errorString;
   }

   /**
    * List of Authentication error types.
    * 
    * @author rayc@google.com (Ray Colline)
    */
   public enum AuthErrorType {
     MALFORMED_REQUEST,
     PASSWORD_TOO_LONG;
   }
   
}
