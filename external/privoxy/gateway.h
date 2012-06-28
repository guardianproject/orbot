#ifndef GATEWAY_H_INCLUDED
#define GATEWAY_H_INCLUDED
#define GATEWAY_H_VERSION "$Id: gateway.h,v 1.12 2008/12/24 17:06:19 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/gateway.h,v $
 *
 * Purpose     :  Contains functions to connect to a server, possibly
 *                using a "gateway" (i.e. HTTP proxy and/or SOCKS4
 *                proxy).  Also contains the list of gateway types.
 *
 * Copyright   :  Written by and Copyright (C) 2001 the SourceForge
 *                Privoxy team. http://www.privoxy.org/
 *
 *                Based on the Internet Junkbuster originally written
 *                by and Copyright (C) 1997 Anonymous Coders and 
 *                Junkbusters Corporation.  http://www.junkbusters.com
 *
 *                This program is free software; you can redistribute it 
 *                and/or modify it under the terms of the GNU General
 *                Public License as published by the Free Software
 *                Foundation; either version 2 of the License, or (at
 *                your option) any later version.
 *
 *                This program is distributed in the hope that it will
 *                be useful, but WITHOUT ANY WARRANTY; without even the
 *                implied warranty of MERCHANTABILITY or FITNESS FOR A
 *                PARTICULAR PURPOSE.  See the GNU General Public
 *                License for more details.
 *
 *                The GNU General Public License should be included with
 *                this file.  If not, you can view it at
 *                http://www.gnu.org/copyleft/gpl.html
 *                or write to the Free Software Foundation, Inc., 59
 *                Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Revisions   :
 *    $Log: gateway.h,v $
 *    Revision 1.12  2008/12/24 17:06:19  fabiankeil
 *    Keep a thread around to timeout alive connections
 *    even if no new requests are coming in.
 *
 *    Revision 1.11  2008/11/13 09:08:42  fabiankeil
 *    Add new config option: keep-alive-timeout.
 *
 *    Revision 1.10  2008/10/09 18:21:41  fabiankeil
 *    Flush work-in-progress changes to keep outgoing connections
 *    alive where possible. Incomplete and mostly #ifdef'd out.
 *
 *    Revision 1.9  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.7  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.6  2002/03/25 22:12:45  oes
 *    Added fix for undefined INADDR_NONE on Solaris by Bart Schelstraete
 *
 *    Revision 1.5  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.4  2002/03/09 20:03:52  jongfoster
 *    - Making various functions return int rather than size_t.
 *      (Undoing a recent change).  Since size_t is unsigned on
 *      Windows, functions like read_socket that return -1 on
 *      error cannot return a size_t.
 *
 *      THIS WAS A MAJOR BUG - it caused frequent, unpredictable
 *      crashes, and also frequently caused JB to jump to 100%
 *      CPU and stay there.  (Because it thought it had just
 *      read ((unsigned)-1) == 4Gb of data...)
 *
 *    - The signature of write_socket has changed, it now simply
 *      returns success=0/failure=nonzero.
 *
 *    - Trying to get rid of a few warnings --with-debug on
 *      Windows, I've introduced a new type "jb_socket".  This is
 *      used for the socket file descriptors.  On Windows, this
 *      is SOCKET (a typedef for unsigned).  Everywhere else, it's
 *      an int.  The error value can't be -1 any more, so it's
 *      now JB_INVALID_SOCKET (which is -1 on UNIX, and in
 *      Windows it maps to the #define INVALID_SOCKET.)
 *
 *    - The signature of bind_port has changed.
 *
 *    Revision 1.3  2001/07/29 18:58:15  jongfoster
 *    Removing nested #includes, adding forward declarations for needed
 *    structures, and changing the #define _FILENAME_H to FILENAME_H_INCLUDED.
 *
 *    Revision 1.2  2001/06/07 23:12:14  jongfoster
 *    Removing gateways[] list - no longer used.
 *    Replacing function pointer in struct gateway with a directly
 *    called function forwarded_connect(), which can do the common
 *    task of deciding whether to connect to the web server or HTTP
 *    proxy.
 *    Replacing struct gateway with struct forward_spec
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:54  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

struct forward_spec;
struct http_request;
struct client_state;

extern jb_socket forwarded_connect(const struct forward_spec * fwd, 
                                   struct http_request *http, 
                                   struct client_state *csp);
#ifdef FEATURE_CONNECTION_KEEP_ALIVE

/*
 * Default number of seconds after which an
 * open connection will no longer be reused.
 */
#define DEFAULT_KEEP_ALIVE_TIMEOUT 180

extern void set_keep_alive_timeout(int timeout);
extern void initialize_reusable_connections(void);
extern void forget_connection(jb_socket sfd);
extern void remember_connection(jb_socket sfd,
                                const struct http_request *http,
                                const struct forward_spec *fwd);
extern int close_unusable_connections(void);
#endif /* FEATURE_CONNECTION_KEEP_ALIVE */


/*
 * Solaris fix
 */
#ifndef INADDR_NONE
#define INADDR_NONE -1
#endif

/*
 * Revision control strings from this header and associated .c file
 */
extern const char gateway_rcs[];
extern const char gateway_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef GATEWAY_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
