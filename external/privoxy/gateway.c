const char gateway_rcs[] = "$Id: gateway.c,v 1.48 2009/02/13 17:20:36 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/gateway.c,v $
 *
 * Purpose     :  Contains functions to connect to a server, possibly
 *                using a "forwarder" (i.e. HTTP proxy and/or a SOCKS4
 *                or SOCKS5 proxy).
 *
 * Copyright   :  Written by and Copyright (C) 2001-2009 the SourceForge
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
 *    $Log: gateway.c,v $
 *    Revision 1.48  2009/02/13 17:20:36  fabiankeil
 *    Reword keep-alive support warning and only show
 *    it #if !defined(HAVE_POLL) && !defined(_WIN32).
 *
 *    Revision 1.47  2008/12/24 17:06:19  fabiankeil
 *    Keep a thread around to timeout alive connections
 *    even if no new requests are coming in.
 *
 *    Revision 1.46  2008/12/13 11:07:23  fabiankeil
 *    Remove duplicated debugging checks
 *    in connection_destination_matches().
 *
 *    Revision 1.45  2008/12/04 18:17:07  fabiankeil
 *    Fix some cparser warnings.
 *
 *    Revision 1.44  2008/11/22 11:54:04  fabiankeil
 *    Move log message around to include the socket number.
 *
 *    Revision 1.43  2008/11/13 09:15:51  fabiankeil
 *    Make keep_alive_timeout static.
 *
 *    Revision 1.42  2008/11/13 09:08:42  fabiankeil
 *    Add new config option: keep-alive-timeout.
 *
 *    Revision 1.41  2008/11/08 15:29:58  fabiankeil
 *    Unify two debug messages.
 *
 *    Revision 1.40  2008/11/08 15:14:05  fabiankeil
 *    Fix duplicated debugging check.
 *
 *    Revision 1.39  2008/10/25 11:33:01  fabiankeil
 *    Remove already out-commented line left over from debugging.
 *
 *    Revision 1.38  2008/10/24 17:33:00  fabiankeil
 *    - Tone the "keep-alive support is experimental" warning
 *      down a bit as hackish 0-chunk detection has been
 *      implemented recently.
 *    - Only show the "ndef HAVE_POLL" warning once on start-up.
 *
 *    Revision 1.37  2008/10/23 17:40:53  fabiankeil
 *    Fix forget_connection() and mark_connection_unused(),
 *    which would both under certain circumstances access
 *    reusable_connection[MAX_REUSABLE_CONNECTIONS]. Oops.
 *
 *    Revision 1.36  2008/10/18 19:49:15  fabiankeil
 *    - Factor close_unusable_connections() out of
 *      get_reusable_connection() to make sure we really check
 *      all the remembered connections, not just the ones before
 *      the next reusable one.
 *    - Plug two file descriptor leaks. Internally marking
 *      connections as closed doesn't cut it.
 *
 *    Revision 1.35  2008/10/17 17:12:01  fabiankeil
 *    In socket_is_still_usable(), use select()
 *    and FD_ISSET() if poll() isn't available.
 *
 *    Revision 1.34  2008/10/17 17:07:13  fabiankeil
 *    Add preliminary timeout support.
 *
 *    Revision 1.33  2008/10/16 16:34:21  fabiankeil
 *    Fix two gcc44 warnings.
 *
 *    Revision 1.32  2008/10/16 16:27:22  fabiankeil
 *    Fix compiler warning.
 *
 *    Revision 1.31  2008/10/16 07:31:11  fabiankeil
 *    - Factor socket_is_still_usable() out of get_reusable_connection().
 *    - If poll() isn't available, show a warning and assume the socket
 *      is still usable.
 *
 *    Revision 1.30  2008/10/13 17:31:03  fabiankeil
 *    If a remembered connection is no longer usable and
 *    has been marked closed, don't bother checking if the
 *    destination matches.
 *
 *    Revision 1.29  2008/10/11 16:59:41  fabiankeil
 *    Add missing dots for two log messages.
 *
 *    Revision 1.28  2008/10/09 18:21:41  fabiankeil
 *    Flush work-in-progress changes to keep outgoing connections
 *    alive where possible. Incomplete and mostly #ifdef'd out.
 *
 *    Revision 1.27  2008/09/27 15:05:51  fabiankeil
 *    Return only once in forwarded_connect().
 *
 *    Revision 1.26  2008/08/18 17:42:06  fabiankeil
 *    Fix typo in macro name.
 *
 *    Revision 1.25  2008/02/07 18:09:46  fabiankeil
 *    In socks5_connect:
 *    - make the buffers quite a bit smaller.
 *    - properly report "socks5 server unreachable" failures.
 *    - let strncpy() use the whole buffer. Using a length of 0xffu wasn't actually
 *      wrong, but requires too much thinking as it doesn't depend on the buffer size.
 *    - log a message if the socks5 server sends more data than expected.
 *    - add some assertions and comments.
 *
 *    Revision 1.24  2008/02/04 14:56:29  fabiankeil
 *    - Fix a compiler warning.
 *    - Stop assuming that htonl(INADDR_NONE) equals INADDR_NONE.
 *
 *    Revision 1.23  2008/02/04 13:11:35  fabiankeil
 *    Remember the cause of the SOCKS5 error for the CGI message.
 *
 *    Revision 1.22  2008/02/03 13:46:15  fabiankeil
 *    Add SOCKS5 support. Patch #1862863 by Eric M. Hopper with minor changes.
 *
 *    Revision 1.21  2007/07/28 12:30:03  fabiankeil
 *    Modified patch from Song Weijia (#1762559) to
 *    fix socks requests on big-endian platforms.
 *
 *    Revision 1.20  2007/05/14 10:23:48  fabiankeil
 *    - Use strlcpy() instead of strcpy().
 *    - Use the same buffer for socks requests and socks responses.
 *    - Fix bogus warning about web_server_addr being used uninitialized.
 *
 *    Revision 1.19  2007/01/25 14:09:45  fabiankeil
 *    - Save errors in socks4_connect() to csp->error_message.
 *    - Silence some gcc43 warnings, hopefully the right way.
 *
 *    Revision 1.18  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.16  2002/05/12 21:36:29  jongfoster
 *    Correcting function comments
 *
 *    Revision 1.15  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.14  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.13  2002/03/13 00:29:59  jongfoster
 *    Killing warnings
 *
 *    Revision 1.12  2002/03/09 20:03:52  jongfoster
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
 *    Revision 1.11  2002/03/08 17:46:04  jongfoster
 *    Fixing int/size_t warnings
 *
 *    Revision 1.10  2002/03/07 03:50:19  oes
 *     - Improved handling of failed DNS lookups
 *     - Fixed compiler warnings
 *
 *    Revision 1.9  2001/10/25 03:40:48  david__schmidt
 *    Change in porting tactics: OS/2's EMX porting layer doesn't allow multiple
 *    threads to call select() simultaneously.  So, it's time to do a real, live,
 *    native OS/2 port.  See defines for __EMX__ (the porting layer) vs. __OS2__
 *    (native). Both versions will work, but using __OS2__ offers multi-threading.
 *
 *    Revision 1.8  2001/09/13 20:10:12  jongfoster
 *    Fixing missing #include under Windows
 *
 *    Revision 1.7  2001/09/12 17:58:26  steudten
 *
 *    add #include <string.h>
 *
 *    Revision 1.6  2001/09/10 10:41:16  oes
 *    Added #include in.h
 *
 *    Revision 1.5  2001/07/29 18:47:57  jongfoster
 *    Adding missing #include project.h
 *
 *    Revision 1.4  2001/07/24 12:47:06  oes
 *    Applied BeOS support update by Eugenia
 *
 *    Revision 1.3  2001/06/09 10:55:28  jongfoster
 *    Changing BUFSIZ ==> BUFFER_SIZE
 *
 *    Revision 1.2  2001/06/07 23:11:38  jongfoster
 *    Removing gateways[] list - no longer used.
 *    Replacing function pointer in struct gateway with a directly
 *    called function forwarded_connect(), which can do the common
 *    task of deciding whether to connect to the web server or HTTP
 *    proxy.
 *    Replacing struct gateway with struct forward_spec
 *    Fixing bug with SOCKS4A and HTTP proxy server in combination.
 *    It was a bug which led to the connection being made to the web
 *    server rather than the HTTP proxy, and also a buffer overrun.
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:54  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "config.h"

#include <stdio.h>
#include <sys/types.h>

#ifndef _WIN32
#include <netinet/in.h>
#endif

#include <errno.h>
#include <string.h>
#include "assert.h"

#ifdef _WIN32
#include <winsock2.h>
#endif /* def _WIN32 */

#ifdef __BEOS__
#include <netdb.h>
#endif /* def __BEOS__ */

#ifdef __OS2__
#include <utils.h>
#endif /* def __OS2__ */

#include "project.h"
#include "jcc.h"
#include "errlog.h"
#include "jbsockets.h"
#include "gateway.h"
#include "miscutil.h"
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
#ifdef HAVE_POLL
#ifdef __GLIBC__ 
#include <sys/poll.h>
#else
#include <poll.h>
#endif /* def __GLIBC__ */
#endif /* HAVE_POLL */
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

const char gateway_h_rcs[] = GATEWAY_H_VERSION;

static jb_socket socks4_connect(const struct forward_spec * fwd,
                                const char * target_host,
                                int target_port,
                                struct client_state *csp);

static jb_socket socks5_connect(const struct forward_spec *fwd,
                                const char *target_host,
                                int target_port,
                                struct client_state *csp);


#define SOCKS_REQUEST_GRANTED          90
#define SOCKS_REQUEST_REJECT           91
#define SOCKS_REQUEST_IDENT_FAILED     92
#define SOCKS_REQUEST_IDENT_CONFLICT   93

#define SOCKS5_REQUEST_GRANTED             0
#define SOCKS5_REQUEST_FAILED              1
#define SOCKS5_REQUEST_DENIED              2
#define SOCKS5_REQUEST_NETWORK_UNREACHABLE 3
#define SOCKS5_REQUEST_HOST_UNREACHABLE    4
#define SOCKS5_REQUEST_CONNECTION_REFUSED  5
#define SOCKS5_REQUEST_TTL_EXPIRED         6
#define SOCKS5_REQUEST_PROTOCOL_ERROR      7
#define SOCKS5_REQUEST_BAD_ADDRESS_TYPE    8

/* structure of a socks client operation */
struct socks_op {
   unsigned char vn;          /* socks version number */
   unsigned char cd;          /* command code */
   unsigned char dstport[2];  /* destination port */
   unsigned char dstip[4];    /* destination address */
   char userid;               /* first byte of userid */
   char padding[3];           /* make sure sizeof(struct socks_op) is endian-independent. */
   /* more bytes of the userid follow, terminated by a NULL */
};

/* structure of a socks server reply */
struct socks_reply {
   unsigned char vn;          /* socks version number */
   unsigned char cd;          /* command code */
   unsigned char dstport[2];  /* destination port */
   unsigned char dstip[4];    /* destination address */
};

static const char socks_userid[] = "anonymous";

#ifdef FEATURE_CONNECTION_KEEP_ALIVE

#define MAX_REUSABLE_CONNECTIONS 100
static int keep_alive_timeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

struct reusable_connection
{
   jb_socket sfd;
   int       in_use;
   char      *host;
   int       port;
   time_t    timestamp;

   int       forwarder_type;
   char      *gateway_host;
   int       gateway_port;
   char      *forward_host;
   int       forward_port;
};

static struct reusable_connection reusable_connection[MAX_REUSABLE_CONNECTIONS];

static int mark_connection_unused(jb_socket sfd);
static void mark_connection_closed(struct reusable_connection *closed_connection);
static int socket_is_still_usable(jb_socket sfd);


/*********************************************************************
 *
 * Function    :  initialize_reusable_connections
 *
 * Description :  Initializes the reusable_connection structures.
 *                Must be called with connection_reuse_mutex locked.
 *
 * Parameters  : N/A
 *
 * Returns     : void
 *
 *********************************************************************/
extern void initialize_reusable_connections(void)
{
   unsigned int slot = 0;

#if !defined(HAVE_POLL) && !defined(_WIN32)
   log_error(LOG_LEVEL_INFO,
      "Detecting already dead connections might not work "
      "correctly on your platform. In case of problems, "
      "unset the keep-alive-timeout option.");
#endif

   for (slot = 0; slot < SZ(reusable_connection); slot++)
   {
      mark_connection_closed(&reusable_connection[slot]);
   }

   log_error(LOG_LEVEL_CONNECT, "Initialized %d socket slots.", slot);
}


/*********************************************************************
 *
 * Function    :  remember_connection
 *
 * Description :  Remembers a connection for reuse later on.
 *
 * Parameters  :
 *          1  :  sfd  = Open socket to remember.
 *          2  :  http = The destination for the connection.
 *          3  :  fwd  = The forwarder settings used.
 *
 * Returns     : void
 *
 *********************************************************************/
void remember_connection(jb_socket sfd, const struct http_request *http,
                                        const struct forward_spec *fwd)
{
   unsigned int slot = 0;
   int free_slot_found = FALSE;

   assert(sfd != JB_INVALID_SOCKET);

   if (mark_connection_unused(sfd))
   {
      return;
   }

   privoxy_mutex_lock(&connection_reuse_mutex);

   /* Find free socket slot. */
   for (slot = 0; slot < SZ(reusable_connection); slot++)
   {
      if (reusable_connection[slot].sfd == JB_INVALID_SOCKET)
      {
         assert(reusable_connection[slot].in_use == 0);
         log_error(LOG_LEVEL_CONNECT,
            "Remembering socket %d for %s:%d in slot %d.",
            sfd, http->host, http->port, slot);
         free_slot_found = TRUE;
         break;
      }
   }

   if (!free_slot_found)
   {
      log_error(LOG_LEVEL_CONNECT,
        "No free slots found to remembering socket for %s:%d. Last slot %d.",
        http->host, http->port, slot);
      privoxy_mutex_unlock(&connection_reuse_mutex);
      close_socket(sfd);
      return;
   }

   assert(NULL != http->host);
   reusable_connection[slot].host = strdup(http->host);
   if (NULL == reusable_connection[slot].host)
   {
      log_error(LOG_LEVEL_FATAL, "Out of memory saving socket.");
   }
   reusable_connection[slot].sfd = sfd;
   reusable_connection[slot].port = http->port;
   reusable_connection[slot].in_use = 0;
   reusable_connection[slot].timestamp = time(NULL);

   assert(NULL != fwd);
   assert(reusable_connection[slot].gateway_host == NULL);
   assert(reusable_connection[slot].gateway_port == 0);
   assert(reusable_connection[slot].forwarder_type == SOCKS_NONE);
   assert(reusable_connection[slot].forward_host == NULL);
   assert(reusable_connection[slot].forward_port == 0);

   reusable_connection[slot].forwarder_type = fwd->type;
   if (NULL != fwd->gateway_host)
   {
      reusable_connection[slot].gateway_host = strdup(fwd->gateway_host);
      if (NULL == reusable_connection[slot].gateway_host)
      {
         log_error(LOG_LEVEL_FATAL, "Out of memory saving gateway_host.");
      }
   }
   else
   {
      reusable_connection[slot].gateway_host = NULL;
   }
   reusable_connection[slot].gateway_port = fwd->gateway_port;

   if (NULL != fwd->forward_host)
   {
      reusable_connection[slot].forward_host = strdup(fwd->forward_host);
      if (NULL == reusable_connection[slot].forward_host)
      {
         log_error(LOG_LEVEL_FATAL, "Out of memory saving forward_host.");
      }
   }
   else
   {
      reusable_connection[slot].forward_host = NULL;
   }
   reusable_connection[slot].forward_port = fwd->forward_port;

   privoxy_mutex_unlock(&connection_reuse_mutex);
}


/*********************************************************************
 *
 * Function    :  mark_connection_closed
 *
 * Description : Marks a reused connection closed.
 *               Must be called with connection_reuse_mutex locked.
 *
 * Parameters  :
 *          1  :  closed_connection = The connection to mark as closed.
 *
 * Returns     : void
 *
 *********************************************************************/
static void mark_connection_closed(struct reusable_connection *closed_connection)
{
   closed_connection->in_use = FALSE;
   closed_connection->sfd = JB_INVALID_SOCKET;
   freez(closed_connection->host);
   closed_connection->port = 0;
   closed_connection->timestamp = 0;
   closed_connection->forwarder_type = SOCKS_NONE;
   freez(closed_connection->gateway_host);
   closed_connection->gateway_port = 0;
   freez(closed_connection->forward_host);
   closed_connection->forward_port = 0;
}


/*********************************************************************
 *
 * Function    :  forget_connection
 *
 * Description :  Removes a previously remembered connection from
 *                the list of reusable connections.
 *
 * Parameters  :
 *          1  :  sfd = The socket belonging to the connection in question.
 *
 * Returns     : void
 *
 *********************************************************************/
void forget_connection(jb_socket sfd)
{
   unsigned int slot = 0;

   assert(sfd != JB_INVALID_SOCKET);

   privoxy_mutex_lock(&connection_reuse_mutex);

   for (slot = 0; slot < SZ(reusable_connection); slot++)
   {
      if (reusable_connection[slot].sfd == sfd)
      {
         assert(reusable_connection[slot].in_use);

         log_error(LOG_LEVEL_CONNECT,
            "Forgetting socket %d for %s:%d in slot %d.",
            sfd, reusable_connection[slot].host,
            reusable_connection[slot].port, slot);
         mark_connection_closed(&reusable_connection[slot]);
         privoxy_mutex_unlock(&connection_reuse_mutex);

         return;
      }
   }

   log_error(LOG_LEVEL_CONNECT,
      "Socket %d already forgotten or never remembered.", sfd);

   privoxy_mutex_unlock(&connection_reuse_mutex);
}


/*********************************************************************
 *
 * Function    :  connection_destination_matches
 *
 * Description :  Determines whether a remembered connection can
 *                be reused. That is, whether the destination and
 *                the forwarding settings match.
 *
 * Parameters  :
 *          1  :  connection = The connection to check.
 *          2  :  http = The destination for the connection.
 *          3  :  fwd  = The forwarder settings.
 *
 * Returns     :  TRUE for yes, FALSE otherwise.
 *
 *********************************************************************/
static int connection_destination_matches(const struct reusable_connection *connection,
                                          const struct http_request *http,
                                          const struct forward_spec *fwd)
{
   if ((connection->forwarder_type != fwd->type)
    || (connection->gateway_port   != fwd->gateway_port)
    || (connection->forward_port   != fwd->forward_port)
    || (connection->port           != http->port))
   {
      return FALSE;
   }

   if ((    (NULL != connection->gateway_host)
         && (NULL != fwd->gateway_host)
         && strcmpic(connection->gateway_host, fwd->gateway_host))
       && (connection->gateway_host != fwd->gateway_host))
   {
      log_error(LOG_LEVEL_CONNECT, "Gateway mismatch.");
      return FALSE;
   }

   if ((    (NULL != connection->forward_host)
         && (NULL != fwd->forward_host)
         && strcmpic(connection->forward_host, fwd->forward_host))
      && (connection->forward_host != fwd->forward_host))
   {
      log_error(LOG_LEVEL_CONNECT, "Forwarding proxy mismatch.");
      return FALSE;
   }

   return (!strcmpic(connection->host, http->host));

}


/*********************************************************************
 *
 * Function    :  close_unusable_connections
 *
 * Description :  Closes remembered connections that have timed
 *                out or have been closed on the other side.
 *
 * Parameters  :  none
 *
 * Returns     :  Number of connections that are still alive.
 *
 *********************************************************************/
int close_unusable_connections(void)
{
   unsigned int slot = 0;
   int connections_alive = 0;

   privoxy_mutex_lock(&connection_reuse_mutex);

   for (slot = 0; slot < SZ(reusable_connection); slot++)
   {
      if (!reusable_connection[slot].in_use
         && (JB_INVALID_SOCKET != reusable_connection[slot].sfd))
      {
         time_t time_open = time(NULL) - reusable_connection[slot].timestamp;

         if (keep_alive_timeout < time_open)
         {
            log_error(LOG_LEVEL_CONNECT,
               "The connection to %s:%d in slot %d timed out. "
               "Closing socket %d. Timeout is: %d.",
               reusable_connection[slot].host,
               reusable_connection[slot].port, slot,
               reusable_connection[slot].sfd, keep_alive_timeout);
            close_socket(reusable_connection[slot].sfd);
            mark_connection_closed(&reusable_connection[slot]);
         }
         else if (!socket_is_still_usable(reusable_connection[slot].sfd))
         {
            log_error(LOG_LEVEL_CONNECT,
               "The connection to %s:%d in slot %d is no longer usable. "
               "Closing socket %d.", reusable_connection[slot].host,
               reusable_connection[slot].port, slot,
               reusable_connection[slot].sfd);
            close_socket(reusable_connection[slot].sfd);
            mark_connection_closed(&reusable_connection[slot]);
         }
         else
         {
            connections_alive++;
         }
      }
   }

   privoxy_mutex_unlock(&connection_reuse_mutex);

   return connections_alive;

}


/*********************************************************************
 *
 * Function    :  socket_is_still_usable
 *
 * Description :  Decides whether or not an open socket is still usable.
 *
 * Parameters  :
 *          1  :  sfd = The socket to check.
 *
 * Returns     :  TRUE for yes, otherwise FALSE.
 *
 *********************************************************************/
static int socket_is_still_usable(jb_socket sfd)
{
#ifdef HAVE_POLL
   int poll_result;
   struct pollfd poll_fd[1];

   memset(poll_fd, 0, sizeof(poll_fd));
   poll_fd[0].fd = sfd;
   poll_fd[0].events = POLLIN;

   poll_result = poll(poll_fd, 1, 0);

   if (-1 != poll_result)
   {
      return !(poll_fd[0].revents & POLLIN);
   }
   else
   {
      log_error(LOG_LEVEL_CONNECT, "Polling socket %d failed.", sfd);
      return FALSE;
   }
#else
   fd_set readable_fds;
   struct timeval timeout;
   int ret;
   int socket_is_alive = 0;

   memset(&timeout, '\0', sizeof(timeout));
   FD_ZERO(&readable_fds);
   FD_SET(sfd, &readable_fds);

   ret = select((int)sfd+1, &readable_fds, NULL, NULL, &timeout);
   if (ret < 0)
   {
      log_error(LOG_LEVEL_ERROR, "select() failed!: %E");
   }

   /*
    * XXX: I'm not sure why !FD_ISSET() works,
    * but apparently it does.
    */
   socket_is_alive = !FD_ISSET(sfd, &readable_fds);

   return socket_is_alive;
#endif /* def HAVE_POLL */
}


/*********************************************************************
 *
 * Function    :  get_reusable_connection
 *
 * Description :  Returns an open socket to a previously remembered
 *                open connection (if there is one).
 *
 * Parameters  :
 *          1  :  http = The destination for the connection.
 *          2  :  fwd  = The forwarder settings.
 *
 * Returns     :  JB_INVALID_SOCKET => No reusable connection found,
 *                otherwise a usable socket.
 *
 *********************************************************************/
static jb_socket get_reusable_connection(const struct http_request *http,
                                         const struct forward_spec *fwd)
{
   jb_socket sfd = JB_INVALID_SOCKET;
   unsigned int slot = 0;

   close_unusable_connections();

   privoxy_mutex_lock(&connection_reuse_mutex);

   for (slot = 0; slot < SZ(reusable_connection); slot++)
   {
      if (!reusable_connection[slot].in_use
         && (JB_INVALID_SOCKET != reusable_connection[slot].sfd))
      {
         if (connection_destination_matches(&reusable_connection[slot], http, fwd))
         {
            reusable_connection[slot].in_use = TRUE;
            sfd = reusable_connection[slot].sfd;
            log_error(LOG_LEVEL_CONNECT,
               "Found reusable socket %d for %s:%d in slot %d.",
               sfd, reusable_connection[slot].host, reusable_connection[slot].port, slot);
            break;
         }
      }
   }

   privoxy_mutex_unlock(&connection_reuse_mutex);

   return sfd;

}


/*********************************************************************
 *
 * Function    :  mark_connection_unused
 *
 * Description :  Gives a remembered connection free for reuse.
 *
 * Parameters  :
 *          1  :  sfd = The socket belonging to the connection in question.
 *
 * Returns     :  TRUE => Socket found and marked as unused.
 *                FALSE => Socket not found.
 *
 *********************************************************************/
static int mark_connection_unused(jb_socket sfd)
{
   unsigned int slot = 0;
   int socket_found = FALSE;

   assert(sfd != JB_INVALID_SOCKET);

   privoxy_mutex_lock(&connection_reuse_mutex);

   for (slot = 0; slot < SZ(reusable_connection); slot++)
   {
      if (reusable_connection[slot].sfd == sfd)
      {
         assert(reusable_connection[slot].in_use);
         socket_found = TRUE;
         log_error(LOG_LEVEL_CONNECT,
            "Marking open socket %d for %s:%d in slot %d as unused.",
            sfd, reusable_connection[slot].host,
            reusable_connection[slot].port, slot);
         reusable_connection[slot].in_use = 0;
         reusable_connection[slot].timestamp = time(NULL);
         break;
      }
   }

   privoxy_mutex_unlock(&connection_reuse_mutex);

   return socket_found;

}


/*********************************************************************
 *
 * Function    :  set_keep_alive_timeout
 *
 * Description :  Sets the timeout after which open
 *                connections will no longer be reused.
 *
 * Parameters  :
 *          1  :  timeout = The timeout in seconds.
 *
 * Returns     :  void
 *
 *********************************************************************/
void set_keep_alive_timeout(int timeout)
{
   keep_alive_timeout = timeout;
}
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */


/*********************************************************************
 *
 * Function    :  forwarded_connect
 *
 * Description :  Connect to a specified web server, possibly via
 *                a HTTP proxy and/or a SOCKS proxy.
 *
 * Parameters  :
 *          1  :  fwd = the proxies to use when connecting.
 *          2  :  http = the http request and apropos headers
 *          3  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_INVALID_SOCKET => failure, else it is the socket file descriptor.
 *
 *********************************************************************/
jb_socket forwarded_connect(const struct forward_spec * fwd,
                            struct http_request *http,
                            struct client_state *csp)
{
   const char * dest_host;
   int dest_port;
   jb_socket sfd = JB_INVALID_SOCKET;

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   sfd = get_reusable_connection(http, fwd);
   if (JB_INVALID_SOCKET != sfd)
   {
      return sfd;
   }
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

   /* Figure out if we need to connect to the web server or a HTTP proxy. */
   if (fwd->forward_host)
   {
      /* HTTP proxy */
      dest_host = fwd->forward_host;
      dest_port = fwd->forward_port;
   }
   else
   {
      /* Web server */
      dest_host = http->host;
      dest_port = http->port;
   }

   /* Connect, maybe using a SOCKS proxy */
   switch (fwd->type)
   {
      case SOCKS_NONE:
         sfd = connect_to(dest_host, dest_port, csp);
         break;
      case SOCKS_4:
      case SOCKS_4A:
         sfd = socks4_connect(fwd, dest_host, dest_port, csp);
         break;
      case SOCKS_5:
         sfd = socks5_connect(fwd, dest_host, dest_port, csp);
         break;
      default:
         /* Should never get here */
         log_error(LOG_LEVEL_FATAL,
            "SOCKS4 impossible internal error - bad SOCKS type.");
   }

   if (JB_INVALID_SOCKET != sfd)
   {
      log_error(LOG_LEVEL_CONNECT,
         "Created new connection to %s:%d on socket %d.",
         http->host, http->port, sfd);
   }

   return sfd;

}


/*********************************************************************
 *
 * Function    :  socks4_connect
 *
 * Description :  Connect to the SOCKS server, and connect through
 *                it to the specified server.   This handles
 *                all the SOCKS negotiation, and returns a file
 *                descriptor for a socket which can be treated as a
 *                normal (non-SOCKS) socket.
 *
 *                Logged error messages are saved to csp->error_message
 *                and later reused by error_response() for the CGI
 *                message. strdup allocation failures are handled there.
 *
 * Parameters  :
 *          1  :  fwd = Specifies the SOCKS proxy to use.
 *          2  :  target_host = The final server to connect to.
 *          3  :  target_port = The final port to connect to.
 *          4  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_INVALID_SOCKET => failure, else a socket file descriptor.
 *
 *********************************************************************/
static jb_socket socks4_connect(const struct forward_spec * fwd,
                                const char * target_host,
                                int target_port,
                                struct client_state *csp)
{
   unsigned int web_server_addr;
   char buf[BUFFER_SIZE];
   struct socks_op    *c = (struct socks_op    *)buf;
   struct socks_reply *s = (struct socks_reply *)buf;
   size_t n;
   size_t csiz;
   jb_socket sfd;
   int err = 0;
   char *errstr = NULL;

   if ((fwd->gateway_host == NULL) || (*fwd->gateway_host == '\0'))
   {
      /* XXX: Shouldn't the config file parser prevent this? */
      errstr = "NULL gateway host specified.";
      err = 1;
   }

   if (fwd->gateway_port <= 0)
   {
      errstr = "invalid gateway port specified.";
      err = 1;
   }

   if (err)
   {
      log_error(LOG_LEVEL_CONNECT, "socks4_connect: %s", errstr);
      csp->error_message = strdup(errstr); 
      errno = EINVAL;
      return(JB_INVALID_SOCKET);
   }

   /* build a socks request for connection to the web server */

   strlcpy(&(c->userid), socks_userid, sizeof(buf) - sizeof(struct socks_op));

   csiz = sizeof(*c) + sizeof(socks_userid) - sizeof(c->userid) - sizeof(c->padding);

   switch (fwd->type)
   {
      case SOCKS_4:
         web_server_addr = resolve_hostname_to_ip(target_host);
         if (web_server_addr == INADDR_NONE)
         {
            errstr = "could not resolve target host";
            log_error(LOG_LEVEL_CONNECT, "socks4_connect: %s %s", errstr, target_host);
            err = 1;
         }
         else
         {
            web_server_addr = htonl(web_server_addr);
         }
         break;
      case SOCKS_4A:
         web_server_addr = 0x00000001;
         n = csiz + strlen(target_host) + 1;
         if (n > sizeof(buf))
         {
            errno = EINVAL;
            errstr = "buffer cbuf too small.";
            log_error(LOG_LEVEL_CONNECT, "socks4_connect: %s", errstr);
            err = 1;
         }
         else
         {
            strlcpy(buf + csiz, target_host, sizeof(buf) - sizeof(struct socks_op) - csiz);
            /*
             * What we forward to the socks4a server should have the
             * size of socks_op, plus the length of the userid plus
             * its \0 byte (which we don't have to add because the
             * first byte of the userid is counted twice as it's also
             * part of sock_op) minus the padding bytes (which are part
             * of the userid as well), plus the length of the target_host
             * (which is stored csiz bytes after the beginning of the buffer),
             * plus another \0 byte.
             */
            assert(n == sizeof(struct socks_op) + strlen(&(c->userid)) - sizeof(c->padding) + strlen(buf + csiz) + 1);
            csiz = n;
         }
         break;
      default:
         /* Should never get here */
         log_error(LOG_LEVEL_FATAL,
            "socks4_connect: SOCKS4 impossible internal error - bad SOCKS type.");
         /* Not reached */
         return(JB_INVALID_SOCKET);
   }

   if (err)
   {
      csp->error_message = strdup(errstr);
      return(JB_INVALID_SOCKET);
   }

   c->vn          = 4;
   c->cd          = 1;
   c->dstport[0]  = (unsigned char)((target_port       >> 8  ) & 0xff);
   c->dstport[1]  = (unsigned char)((target_port             ) & 0xff);
   c->dstip[0]    = (unsigned char)((web_server_addr   >> 24 ) & 0xff);
   c->dstip[1]    = (unsigned char)((web_server_addr   >> 16 ) & 0xff);
   c->dstip[2]    = (unsigned char)((web_server_addr   >>  8 ) & 0xff);
   c->dstip[3]    = (unsigned char)((web_server_addr         ) & 0xff);

   /* pass the request to the socks server */
   sfd = connect_to(fwd->gateway_host, fwd->gateway_port, csp);

   if (sfd == JB_INVALID_SOCKET)
   {
      /*
       * XXX: connect_to should fill in the exact reason.
       * Most likely resolving the IP of the forwarder failed.
       */
      errstr = "connect_to failed: see logfile for details";
      err = 1;
   }
   else if (write_socket(sfd, (char *)c, csiz))
   {
      errstr = "SOCKS4 negotiation write failed.";
      log_error(LOG_LEVEL_CONNECT, "socks4_connect: %s", errstr);
      err = 1;
      close_socket(sfd);
   }
   else if (read_socket(sfd, buf, sizeof(buf)) != sizeof(*s))
   {
      errstr = "SOCKS4 negotiation read failed.";
      log_error(LOG_LEVEL_CONNECT, "socks4_connect: %s", errstr);
      err = 1;
      close_socket(sfd);
   }

   if (err)
   {
      csp->error_message = strdup(errstr);      
      return(JB_INVALID_SOCKET);
   }

   switch (s->cd)
   {
      case SOCKS_REQUEST_GRANTED:
         return(sfd);
      case SOCKS_REQUEST_REJECT:
         errstr = "SOCKS request rejected or failed.";
         errno = EINVAL;
         break;
      case SOCKS_REQUEST_IDENT_FAILED:
         errstr = "SOCKS request rejected because "
            "SOCKS server cannot connect to identd on the client.";
         errno = EACCES;
         break;
      case SOCKS_REQUEST_IDENT_CONFLICT:
         errstr = "SOCKS request rejected because "
            "the client program and identd report "
            "different user-ids.";
         errno = EACCES;
         break;
      default:
         errno = ENOENT;
         snprintf(buf, sizeof(buf),
            "SOCKS request rejected for reason code %d.", s->cd);
         errstr = buf;
   }

   log_error(LOG_LEVEL_CONNECT, "socks4_connect: %s", errstr);
   csp->error_message = strdup(errstr);
   close_socket(sfd);

   return(JB_INVALID_SOCKET);

}

/*********************************************************************
 *
 * Function    :  translate_socks5_error
 *
 * Description :  Translates a SOCKS errors to a string.
 *
 * Parameters  :
 *          1  :  socks_error = The error code to translate.
 *
 * Returns     :  The string translation.
 *
 *********************************************************************/
static const char *translate_socks5_error(int socks_error)
{
   switch (socks_error)
   {
      /* XXX: these should be more descriptive */
      case SOCKS5_REQUEST_FAILED:
         return "SOCKS5 request failed";
      case SOCKS5_REQUEST_DENIED:
         return "SOCKS5 request denied";
      case SOCKS5_REQUEST_NETWORK_UNREACHABLE:
         return "SOCKS5 network unreachable";
      case SOCKS5_REQUEST_HOST_UNREACHABLE:
         return "SOCKS5 host unreachable";
      case SOCKS5_REQUEST_CONNECTION_REFUSED:
         return "SOCKS5 connection refused";
      case SOCKS5_REQUEST_TTL_EXPIRED:
         return "SOCKS5 TTL expired";
      case SOCKS5_REQUEST_PROTOCOL_ERROR:
         return "SOCKS5 client protocol error";
      case SOCKS5_REQUEST_BAD_ADDRESS_TYPE:
         return "SOCKS5 domain names unsupported";
      case SOCKS5_REQUEST_GRANTED:
         return "everything's peachy";
      default:
         return "SOCKS5 negotiation protocol error";
   }
}

/*********************************************************************
 *
 * Function    :  socks5_connect
 *
 * Description :  Connect to the SOCKS server, and connect through
 *                it to the specified server.   This handles
 *                all the SOCKS negotiation, and returns a file
 *                descriptor for a socket which can be treated as a
 *                normal (non-SOCKS) socket.
 *
 * Parameters  :
 *          1  :  fwd = Specifies the SOCKS proxy to use.
 *          2  :  target_host = The final server to connect to.
 *          3  :  target_port = The final port to connect to.
 *          4  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_INVALID_SOCKET => failure, else a socket file descriptor.
 *
 *********************************************************************/
static jb_socket socks5_connect(const struct forward_spec *fwd,
                                const char *target_host,
                                int target_port,
                                struct client_state *csp)
{
   int err = 0;
   char cbuf[300];
   char sbuf[30];
   size_t client_pos = 0;
   int server_size = 0;
   size_t hostlen = 0;
   jb_socket sfd;
   const char *errstr = NULL;

   assert(fwd->gateway_host);
   if ((fwd->gateway_host == NULL) || (*fwd->gateway_host == '\0'))
   {
      errstr = "NULL gateway host specified";
      err = 1;
   }

   if (fwd->gateway_port <= 0)
   {
      /*
       * XXX: currently this can't happen because in
       * case of invalid gateway ports we use the defaults.
       * Of course we really shouldn't do that.
       */
      errstr = "invalid gateway port specified";
      err = 1;
   }

   hostlen = strlen(target_host);
   if (hostlen > (size_t)255)
   {
      errstr = "target host name is longer than 255 characters";
      err = 1;
   }

   if (fwd->type != SOCKS_5)
   {
      /* Should never get here */
      log_error(LOG_LEVEL_FATAL,
         "SOCKS5 impossible internal error - bad SOCKS type");
      err = 1;
   }

   if (err)
   {
      errno = EINVAL;
      assert(errstr != NULL);
      log_error(LOG_LEVEL_CONNECT, "socks5_connect: %s", errstr);
      csp->error_message = strdup(errstr);
      return(JB_INVALID_SOCKET);
   }

   /* pass the request to the socks server */
   sfd = connect_to(fwd->gateway_host, fwd->gateway_port, csp);

   if (sfd == JB_INVALID_SOCKET)
   {
      errstr = "socks5 server unreachable";
      log_error(LOG_LEVEL_CONNECT, "socks5_connect: %s", errstr);
      csp->error_message = strdup(errstr);
      return(JB_INVALID_SOCKET);
   }

   client_pos = 0;
   cbuf[client_pos++] = '\x05'; /* Version */
   cbuf[client_pos++] = '\x01'; /* One authentication method supported */
   cbuf[client_pos++] = '\x00'; /* The no authentication authentication method */

   if (write_socket(sfd, cbuf, client_pos))
   {
      errstr = "SOCKS5 negotiation write failed";
      csp->error_message = strdup(errstr);
      log_error(LOG_LEVEL_CONNECT, "%s", errstr);
      close_socket(sfd);
      return(JB_INVALID_SOCKET);
   }

   if (read_socket(sfd, sbuf, sizeof(sbuf)) != 2)
   {
      errstr = "SOCKS5 negotiation read failed";
      err = 1;
   }

   if (!err && (sbuf[0] != '\x05'))
   {
      errstr = "SOCKS5 negotiation protocol version error";
      err = 1;
   }

   if (!err && (sbuf[1] == '\xff'))
   {
      errstr = "SOCKS5 authentication required";
      err = 1;
   }

   if (!err && (sbuf[1] != '\x00'))
   {
      errstr = "SOCKS5 negotiation protocol error";
      err = 1;
   }

   if (err)
   {
      assert(errstr != NULL);
      log_error(LOG_LEVEL_CONNECT, "socks5_connect: %s", errstr);
      csp->error_message = strdup(errstr);
      close_socket(sfd);
      errno = EINVAL;
      return(JB_INVALID_SOCKET);
   }

   client_pos = 0;
   cbuf[client_pos++] = '\x05'; /* Version */
   cbuf[client_pos++] = '\x01'; /* TCP connect */
   cbuf[client_pos++] = '\x00'; /* Reserved, must be 0x00 */
   cbuf[client_pos++] = '\x03'; /* Address is domain name */
   cbuf[client_pos++] = (char)(hostlen & 0xffu);
   assert(sizeof(cbuf) - client_pos > (size_t)255);
   /* Using strncpy because we really want the nul byte padding. */
   strncpy(cbuf + client_pos, target_host, sizeof(cbuf) - client_pos);
   client_pos += (hostlen & 0xffu);
   cbuf[client_pos++] = (char)((target_port >> 8) & 0xff);
   cbuf[client_pos++] = (char)((target_port     ) & 0xff);

   if (write_socket(sfd, cbuf, client_pos))
   {
      errstr = "SOCKS5 negotiation read failed";
      csp->error_message = strdup(errstr);
      log_error(LOG_LEVEL_CONNECT, "%s", errstr);
      close_socket(sfd);
      errno = EINVAL;
      return(JB_INVALID_SOCKET);
   }

   server_size = read_socket(sfd, sbuf, sizeof(sbuf));
   if (server_size < 3)
   {
      errstr = "SOCKS5 negotiation read failed";
      err = 1;
   }
   else if (server_size > 20)
   {
      /* This is somewhat unexpected but doesn't realy matter. */
      log_error(LOG_LEVEL_CONNECT, "socks5_connect: read %d bytes "
         "from socks server. Would have accepted up to %d.",
         server_size, sizeof(sbuf));
   }

   if (!err && (sbuf[0] != '\x05'))
   {
      errstr = "SOCKS5 negotiation protocol version error";
      err = 1;
   }

   if (!err && (sbuf[2] != '\x00'))
   {
      errstr = "SOCKS5 negotiation protocol error";
      err = 1;
   }

   if (!err)
   {
      if (sbuf[1] == SOCKS5_REQUEST_GRANTED)
      {
         return(sfd);
      }
      errstr = translate_socks5_error(sbuf[1]);
      err = 1;
   }

   assert(errstr != NULL);
   csp->error_message = strdup(errstr);
   log_error(LOG_LEVEL_CONNECT, "socks5_connect: %s", errstr);
   close_socket(sfd);
   errno = EINVAL;

   return(JB_INVALID_SOCKET);

}

/*
  Local Variables:
  tab-width: 3
  end:
*/
