const char jcc_rcs[] = "$Id: jcc.c,v 1.375 2011/12/10 17:26:11 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/jcc.c,v $
 *
 * Purpose     :  Main file.  Contains main() method, main loop, and
 *                the main connection-handling function.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2010 the
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
 *********************************************************************/


#include "config.h"

#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <fcntl.h>
#include <errno.h>
#include <assert.h>

#ifdef _WIN32
# ifndef FEATURE_PTHREAD
#  ifndef STRICT
#   define STRICT
#  endif
#  include <windows.h>
#  include <process.h>
# endif /* ndef FEATURE_PTHREAD */

# include "win32.h"
# ifndef _WIN_CONSOLE
#  include "w32log.h"
# endif /* ndef _WIN_CONSOLE */
# include "w32svrapi.h"

#else /* ifndef _WIN32 */

# if !defined (__OS2__)
# include <unistd.h>
# include <sys/wait.h>
# endif /* ndef __OS2__ */
# include <sys/time.h>
# include <sys/stat.h>
# include <sys/ioctl.h>

#ifdef sun
#include <sys/termios.h>
#endif /* sun */

#ifdef unix
#include <pwd.h>
#include <grp.h>
#endif

# include <signal.h>

# ifdef __BEOS__
#  include <socket.h>  /* BeOS has select() for sockets only. */
#  include <OS.h>      /* declarations for threads and stuff. */
# endif

# if defined(__EMX__) || defined(__OS2__)
#  include <sys/select.h>  /* OS/2/EMX needs a little help with select */
# endif
# ifdef __OS2__
#define INCL_DOS
# include <os2.h>
#define bzero(B,N) memset(B,0x00,n)
# endif

# ifndef FD_ZERO
#  include <select.h>
# endif

#endif

#include "project.h"
#include "list.h"
#include "jcc.h"
#include "filters.h"
#include "loaders.h"
#include "parsers.h"
#include "miscutil.h"
#include "errlog.h"
#include "jbsockets.h"
#include "gateway.h"
#include "actions.h"
#include "cgi.h"
#include "loadcfg.h"
#include "urlmatch.h"

const char jcc_h_rcs[] = JCC_H_VERSION;
const char project_h_rcs[] = PROJECT_H_VERSION;

int daemon_mode = 1;
struct client_states clients[1];
struct file_list     files[1];

#ifdef FEATURE_STATISTICS
int urls_read     = 0;     /* total nr of urls read inc rejected */
int urls_rejected = 0;     /* total nr of urls rejected */
#endif /* def FEATURE_STATISTICS */

#ifdef FEATURE_GRACEFUL_TERMINATION
int g_terminate = 0;
#endif

#if !defined(_WIN32) && !defined(__OS2__) && !defined(AMIGA)
static void sig_handler(int the_signal);
#endif
static int client_protocol_is_unsupported(const struct client_state *csp, char *req);
static jb_err get_request_destination_elsewhere(struct client_state *csp, struct list *headers);
static jb_err get_server_headers(struct client_state *csp);
static const char *crunch_reason(const struct http_response *rsp);
static void send_crunch_response(const struct client_state *csp, struct http_response *rsp);
static char *get_request_line(struct client_state *csp);
static jb_err receive_client_request(struct client_state *csp);
static jb_err parse_client_request(struct client_state *csp);
static void build_request_line(struct client_state *csp, const struct forward_spec *fwd, char **request_line);
static jb_err change_request_destination(struct client_state *csp);
static void chat(struct client_state *csp);
static void serve(struct client_state *csp);
#if !defined(_WIN32) || defined(_WIN_CONSOLE)
static void usage(const char *myname);
#endif
static void initialize_mutexes(void);
static jb_socket bind_port_helper(const char *haddr, int hport);
static void bind_ports_helper(struct configuration_spec *config, jb_socket sockets[]);
static void close_ports_helper(jb_socket sockets[]);
static void listen_loop(void);

#ifdef AMIGA
void serve(struct client_state *csp);
#else /* ifndef AMIGA */
static void serve(struct client_state *csp);
#endif /* def AMIGA */

#ifdef __BEOS__
static int32 server_thread(void *data);
#endif /* def __BEOS__ */

#ifdef _WIN32
#define sleep(N)  Sleep(((N) * 1000))
#endif

#ifdef __OS2__
#define sleep(N)  DosSleep(((N) * 100))
#endif

#ifdef MUTEX_LOCKS_AVAILABLE
/*
 * XXX: Does the locking stuff really belong in this file?
 */
privoxy_mutex_t log_mutex;
privoxy_mutex_t log_init_mutex;
privoxy_mutex_t connection_reuse_mutex;

#if !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_GETHOSTBYNAME_R)
privoxy_mutex_t resolver_mutex;
#endif /* !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_GETHOSTBYNAME_R) */

#ifndef HAVE_GMTIME_R
privoxy_mutex_t gmtime_mutex;
#endif /* ndef HAVE_GMTIME_R */

#ifndef HAVE_LOCALTIME_R
privoxy_mutex_t localtime_mutex;
#endif /* ndef HAVE_GMTIME_R */

#ifndef HAVE_RANDOM
privoxy_mutex_t rand_mutex;
#endif /* ndef HAVE_RANDOM */

#endif /* def MUTEX_LOCKS_AVAILABLE */

#if defined(unix)
const char *basedir = NULL;
const char *pidfile = NULL;
static int received_hup_signal = 0;
#endif /* defined unix */

/* HTTP snipplets. */
static const char CSUCCEED[] =
   "HTTP/1.1 200 Connection established\r\n"
   "Proxy-Agent: Privoxy/" VERSION "\r\n\r\n";

static const char CHEADER[] =
   "HTTP/1.1 400 Invalid header received from client\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Invalid header received from client.\r\n";

static const char FTP_RESPONSE[] =
   "HTTP/1.1 400 Invalid request received from client\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Invalid request. Privoxy doesn't support FTP.\r\n";

static const char GOPHER_RESPONSE[] =
   "HTTP/1.1 400 Invalid request received from client\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Invalid request. Privoxy doesn't support gopher.\r\n";

/* XXX: should be a template */
static const char MISSING_DESTINATION_RESPONSE[] =
   "HTTP/1.1 400 Bad request received from client\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Bad request. Privoxy was unable to extract the destination.\r\n";

/* XXX: should be a template */
static const char INVALID_SERVER_HEADERS_RESPONSE[] =
   "HTTP/1.1 502 Server or forwarder response invalid\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Bad response. The server or forwarder response doesn't look like HTTP.\r\n";

/* XXX: should be a template */
static const char MESSED_UP_REQUEST_RESPONSE[] =
   "HTTP/1.1 400 Malformed request after rewriting\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Bad request. Messed up with header filters.\r\n";

static const char TOO_MANY_CONNECTIONS_RESPONSE[] =
   "HTTP/1.1 503 Too many open connections\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Maximum number of open connections reached.\r\n";

static const char CLIENT_CONNECTION_TIMEOUT_RESPONSE[] =
   "HTTP/1.1 504 Connection timeout\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "The connection timed out because the client request didn't arrive in time.\r\n";

/* A function to crunch a response */
typedef struct http_response *(*crunch_func_ptr)(struct client_state *);

/* Crunch function flags */
#define CF_NO_FLAGS        0
/* Cruncher applies to forced requests as well */
#define CF_IGNORE_FORCE    1
/* Crunched requests are counted for the block statistics */
#define CF_COUNT_AS_REJECT 2

/* A crunch function and its flags */
struct cruncher
{
   const crunch_func_ptr cruncher;
   const int flags;
};

static int crunch_response_triggered(struct client_state *csp, const struct cruncher crunchers[]);

/* Complete list of cruncher functions */
static const struct cruncher crunchers_all[] = {
   { direct_response, CF_COUNT_AS_REJECT|CF_IGNORE_FORCE},
   { block_url,       CF_COUNT_AS_REJECT },
#ifdef FEATURE_TRUST
   { trust_url,       CF_COUNT_AS_REJECT },
#endif /* def FEATURE_TRUST */
   { redirect_url,    CF_NO_FLAGS  },
   { dispatch_cgi,    CF_IGNORE_FORCE},
   { NULL,            0 }
};

/* Light version, used after tags are applied */
static const struct cruncher crunchers_light[] = {
   { block_url,       CF_COUNT_AS_REJECT },
   { redirect_url,    CF_NO_FLAGS },
   { NULL,            0 }
};


/*
 * XXX: Don't we really mean
 *
 * #if defined(unix)
 *
 * here?
 */
#if !defined(_WIN32) && !defined(__OS2__) && !defined(AMIGA)
/*********************************************************************
 *
 * Function    :  sig_handler
 *
 * Description :  Signal handler for different signals.
 *                Exit gracefully on TERM and INT
 *                or set a flag that will cause the errlog
 *                to be reopened by the main thread on HUP.
 *
 * Parameters  :
 *          1  :  the_signal = the signal cause this function to call
 *
 * Returns     :  -
 *
 *********************************************************************/
static void sig_handler(int the_signal)
{
   switch(the_signal)
   {
      case SIGTERM:
      case SIGINT:
         log_error(LOG_LEVEL_INFO, "exiting by signal %d .. bye", the_signal);
#if defined(unix)
         if(pidfile)
         {
            unlink(pidfile);
         }
#endif /* unix */
         exit(the_signal);
         break;

      case SIGHUP:
#if defined(unix)
         received_hup_signal = 1;
#endif
         break;

      default:
         /*
          * We shouldn't be here, unless we catch signals
          * in main() that we can't handle here!
          */
         log_error(LOG_LEVEL_FATAL, "sig_handler: exiting on unexpected signal %d", the_signal);
   }
   return;

}
#endif


/*********************************************************************
 *
 * Function    :  client_protocol_is_unsupported
 *
 * Description :  Checks if the client used a known unsupported
 *                protocol and deals with it by sending an error
 *                response.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  req = the first request line send by the client
 *
 * Returns     :  TRUE if an error response has been generated, or
 *                FALSE if the request doesn't look invalid.
 *
 *********************************************************************/
static int client_protocol_is_unsupported(const struct client_state *csp, char *req)
{
   /*
    * If it's a FTP or gopher request, we don't support it.
    *
    * These checks are better than nothing, but they might
    * not work in all configurations and some clients might
    * have problems digesting the answer.
    *
    * They should, however, never cause more problems than
    * Privoxy's old behaviour (returning the misleading HTML
    * error message:
    *
    * "Could not resolve http://(ftp|gopher)://example.org").
    */
   if (!strncmpic(req, "GET ftp://", 10) || !strncmpic(req, "GET gopher://", 13))
   {
      const char *response = NULL;
      const char *protocol = NULL;

      if (!strncmpic(req, "GET ftp://", 10))
      {
         response = FTP_RESPONSE;
         protocol = "FTP";
      }
      else
      {
         response = GOPHER_RESPONSE;
         protocol = "GOPHER";
      }
      log_error(LOG_LEVEL_ERROR,
         "%s tried to use Privoxy as %s proxy: %s",
         csp->ip_addr_str, protocol, req);
      log_error(LOG_LEVEL_CLF,
         "%s - - [%T] \"%s\" 400 0", csp->ip_addr_str, req);
      freez(req);
      write_socket(csp->cfd, response, strlen(response));

      return TRUE;
   }

   return FALSE;
}


/*********************************************************************
 *
 * Function    :  get_request_destination_elsewhere
 *
 * Description :  If the client's request was redirected into
 *                Privoxy without the client's knowledge,
 *                the request line lacks the destination host.
 *
 *                This function tries to get it elsewhere,
 *                provided accept-intercepted-requests is enabled.
 *
 *                "Elsewhere" currently only means "Host: header",
 *                but in the future we may ask the redirecting
 *                packet filter to look the destination up.
 *
 *                If the destination stays unknown, an error
 *                response is send to the client and headers
 *                are freed so that chat() can return directly.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  headers = a header list
 *
 * Returns     :  JB_ERR_OK if the destination is now known, or
 *                JB_ERR_PARSE if it isn't.
 *
 *********************************************************************/
static jb_err get_request_destination_elsewhere(struct client_state *csp, struct list *headers)
{
   char *req;

   if (!(csp->config->feature_flags & RUNTIME_FEATURE_ACCEPT_INTERCEPTED_REQUESTS))
   {
      log_error(LOG_LEVEL_ERROR, "%s's request: \'%s\' is invalid."
         " Privoxy isn't configured to accept intercepted requests.",
         csp->ip_addr_str, csp->http->cmd);
      /* XXX: Use correct size */
      log_error(LOG_LEVEL_CLF, "%s - - [%T] \"%s\" 400 0",
         csp->ip_addr_str, csp->http->cmd);

      write_socket(csp->cfd, CHEADER, strlen(CHEADER));
      destroy_list(headers);

      return JB_ERR_PARSE;
   }
   else if (JB_ERR_OK == get_destination_from_headers(headers, csp->http))
   {
#ifndef FEATURE_EXTENDED_HOST_PATTERNS
      /* Split the domain we just got for pattern matching */
      init_domain_components(csp->http);
#endif

      return JB_ERR_OK;
   }
   else
   {
      /* We can't work without destination. Go spread the news.*/

      req = list_to_text(headers);
      chomp(req);
      /* XXX: Use correct size */
      log_error(LOG_LEVEL_CLF, "%s - - [%T] \"%s\" 400 0",
         csp->ip_addr_str, csp->http->cmd);
      log_error(LOG_LEVEL_ERROR,
         "Privoxy was unable to get the destination for %s's request:\n%s\n%s",
         csp->ip_addr_str, csp->http->cmd, req);
      freez(req);

      write_socket(csp->cfd, MISSING_DESTINATION_RESPONSE, strlen(MISSING_DESTINATION_RESPONSE));
      destroy_list(headers);

      return JB_ERR_PARSE;
   }
   /*
    * TODO: If available, use PF's ioctl DIOCNATLOOK as last resort
    * to get the destination IP address, use it as host directly
    * or do a reverse DNS lookup first.
    */
}


/*********************************************************************
 *
 * Function    :  get_server_headers
 *
 * Description :  Parses server headers in iob and fills them
 *                into csp->headers so that they can later be
 *                handled by sed().
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK if everything went fine, or
 *                JB_ERR_PARSE if the headers were incomplete.
 *
 *********************************************************************/
static jb_err get_server_headers(struct client_state *csp)
{
   int continue_hack_in_da_house = 0;
   char * header;

   while (((header = get_header(csp->iob)) != NULL) || continue_hack_in_da_house)
   {
      if (header == NULL)
      {
         /*
          * continue hack in da house. Ignore the ending of
          * this head and continue enlisting header lines.
          * The reason is described below.
          */
         enlist(csp->headers, "");
         continue_hack_in_da_house = 0;
         continue;
      }
      else if (0 == strncmpic(header, "HTTP/1.1 100", 12))
      {
         /*
          * It's a bodyless continue response, don't
          * stop header parsing after reaching its end.
          *
          * As a result Privoxy will concatenate the
          * next response's head and parse and deliver
          * the headers as if they belonged to one request.
          *
          * The client will separate them because of the
          * empty line between them.
          *
          * XXX: What we're doing here is clearly against
          * the intended purpose of the continue header,
          * and under some conditions (HTTP/1.0 client request)
          * it's a standard violation.
          *
          * Anyway, "sort of against the spec" is preferable
          * to "always getting confused by Continue responses"
          * (Privoxy's behaviour before this hack was added)
          */
         log_error(LOG_LEVEL_HEADER, "Continue hack in da house.");
         continue_hack_in_da_house = 1;
      }
      else if (*header == '\0')
      {
         /*
          * If the header is empty, but the Continue hack
          * isn't active, we can assume that we reached the
          * end of the buffer before we hit the end of the
          * head.
          *
          * Inform the caller an let it decide how to handle it.
          */
         return JB_ERR_PARSE;
      }

      if (JB_ERR_MEMORY == enlist(csp->headers, header))
      {
         /*
          * XXX: Should we quit the request and return a
          * out of memory error page instead?
          */
         log_error(LOG_LEVEL_ERROR,
            "Out of memory while enlisting server headers. %s lost.",
            header);
      }
      freez(header);
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  crunch_reason
 *
 * Description :  Translates the crunch reason code into a string.
 *
 * Parameters  :
 *          1  :  rsp = a http_response
 *
 * Returns     :  A string with the crunch reason or an error description.
 *
 *********************************************************************/
static const char *crunch_reason(const struct http_response *rsp)
{
   char * reason = NULL;

   assert(rsp != NULL);
   if (rsp == NULL)
   {
      return "Internal error while searching for crunch reason";
   }

   switch (rsp->crunch_reason)
   {
      case UNSUPPORTED:
         reason = "Unsupported HTTP feature";
         break;
      case BLOCKED:
         reason = "Blocked";
         break;
      case UNTRUSTED:
         reason = "Untrusted";
         break;
      case REDIRECTED:
         reason = "Redirected";
         break;
      case CGI_CALL:
         reason = "CGI Call";
         break;
      case NO_SUCH_DOMAIN:
         reason = "DNS failure";
         break;
      case FORWARDING_FAILED:
         reason = "Forwarding failed";
         break;
      case CONNECT_FAILED:
         reason = "Connection failure";
         break;
      case OUT_OF_MEMORY:
         reason = "Out of memory (may mask other reasons)";
         break;
      case CONNECTION_TIMEOUT:
         reason = "Connection timeout";
         break;
      case NO_SERVER_DATA:
         reason = "No server data received";
         break;
      default:
         reason = "No reason recorded";
         break;
   }

   return reason;
}


/*********************************************************************
 *
 * Function    :  send_crunch_response
 *
 * Description :  Delivers already prepared response for
 *                intercepted requests, logs the interception
 *                and frees the response.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          1  :  rsp = Fully prepared response. Will be freed on exit.
 *
 * Returns     :  Nothing.
 *
 *********************************************************************/
static void send_crunch_response(const struct client_state *csp, struct http_response *rsp)
{
      const struct http_request *http = csp->http;
      char status_code[4];

      assert(rsp != NULL);
      assert(rsp->head != NULL);

      if (rsp == NULL)
      {
         log_error(LOG_LEVEL_FATAL, "NULL response in send_crunch_response.");
      }

      /*
       * Extract the status code from the actual head
       * that will be send to the client. It is the only
       * way to get it right for all requests, including
       * the fixed ones for out-of-memory problems.
       *
       * A head starts like this: 'HTTP/1.1 200...'
       *                           0123456789|11
       *                                     10
       */
      status_code[0] = rsp->head[9];
      status_code[1] = rsp->head[10];
      status_code[2] = rsp->head[11];
      status_code[3] = '\0';

      /* Log that the request was crunched and why. */
      log_error(LOG_LEVEL_CRUNCH, "%s: %s", crunch_reason(rsp), http->url);
      log_error(LOG_LEVEL_CLF, "%s - - [%T] \"%s\" %s %u",
         csp->ip_addr_str, http->ocmd, status_code, rsp->content_length);

      /* Write the answer to the client */
      if (write_socket(csp->cfd, rsp->head, rsp->head_length)
       || write_socket(csp->cfd, rsp->body, rsp->content_length))
      {
         /* There is nothing we can do about it. */
         log_error(LOG_LEVEL_ERROR,
            "Couldn't deliver the error message through client socket %d: %E",
            csp->cfd);
      }

      /* Clean up and return */
      if (cgi_error_memory() != rsp)
      {
         free_http_response(rsp);
      }
      return;
}


/*********************************************************************
 *
 * Function    :  crunch_response_triggered
 *
 * Description :  Checks if the request has to be crunched,
 *                and delivers the crunch response if necessary.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  crunchers = list of cruncher functions to run
 *
 * Returns     :  TRUE if the request was answered with a crunch response
 *                FALSE otherwise.
 *
 *********************************************************************/
static int crunch_response_triggered(struct client_state *csp, const struct cruncher crunchers[])
{
   struct http_response *rsp = NULL;
   const struct cruncher *c;

   /*
    * If CGI request crunching is disabled,
    * check the CGI dispatcher out of order to
    * prevent unintentional blocks or redirects.
    */
   if (!(csp->config->feature_flags & RUNTIME_FEATURE_CGI_CRUNCHING)
       && (NULL != (rsp = dispatch_cgi(csp))))
   {
      /* Deliver, log and free the interception response. */
      send_crunch_response(csp, rsp);
      csp->flags |= CSP_FLAG_CRUNCHED;
      return TRUE;
   }

   for (c = crunchers; c->cruncher != NULL; c++)
   {
      /*
       * Check the cruncher if either Privoxy is toggled
       * on and the request isn't forced, or if the cruncher
       * applies to forced requests as well.
       */
      if (((csp->flags & CSP_FLAG_TOGGLED_ON) &&
          !(csp->flags & CSP_FLAG_FORCED)) ||
          (c->flags & CF_IGNORE_FORCE))
      {
         rsp = c->cruncher(csp);
         if (NULL != rsp)
         {
            /* Deliver, log and free the interception response. */
            send_crunch_response(csp, rsp);
            csp->flags |= CSP_FLAG_CRUNCHED;
#ifdef FEATURE_STATISTICS
            if (c->flags & CF_COUNT_AS_REJECT)
            {
               csp->flags |= CSP_FLAG_REJECTED;
            }
#endif /* def FEATURE_STATISTICS */

            return TRUE;
         }
      }
   }

   return FALSE;
}


/*********************************************************************
 *
 * Function    :  build_request_line
 *
 * Description :  Builds the HTTP request line.
 *
 *                If a HTTP forwarder is used it expects the whole URL,
 *                web servers only get the path.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  fwd = The forwarding spec used for the request
 *                XXX: Should use http->fwd instead.
 *          3  :  request_line = The old request line which will be replaced.
 *
 * Returns     :  Nothing. Terminates in case of memory problems.
 *
 *********************************************************************/
static void build_request_line(struct client_state *csp, const struct forward_spec *fwd, char **request_line)
{
   struct http_request *http = csp->http;

   assert(http->ssl == 0);

   /*
    * Downgrade http version from 1.1 to 1.0
    * if +downgrade action applies.
    */
   if ( (csp->action->flags & ACTION_DOWNGRADE)
     && (!strcmpic(http->ver, "HTTP/1.1")))
   {
      freez(http->ver);
      http->ver = strdup("HTTP/1.0");

      if (http->ver == NULL)
      {
         log_error(LOG_LEVEL_FATAL, "Out of memory downgrading HTTP version");
      }
   }

   /*
    * Rebuild the request line.
    */
   freez(*request_line);
   *request_line = strdup(http->gpc);
   string_append(request_line, " ");

   if (fwd->forward_host)
   {
      string_append(request_line, http->url);
   }
   else
   {
      string_append(request_line, http->path);
   }
   string_append(request_line, " ");
   string_append(request_line, http->ver);

   if (*request_line == NULL)
   {
      log_error(LOG_LEVEL_FATAL, "Out of memory writing HTTP command");
   }
   log_error(LOG_LEVEL_HEADER, "New HTTP Request-Line: %s", *request_line);
}


/*********************************************************************
 *
 * Function    :  change_request_destination
 *
 * Description :  Parse a (rewritten) request line and regenerate
 *                the http request data.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  Forwards the parse_http_request() return code.
 *                Terminates in case of memory problems.
 *
 *********************************************************************/
static jb_err change_request_destination(struct client_state *csp)
{
   struct http_request *http = csp->http;
   jb_err err;

   log_error(LOG_LEVEL_INFO, "Rewrite detected: %s", csp->headers->first->str);
   free_http_request(http);
   err = parse_http_request(csp->headers->first->str, http);
   if (JB_ERR_OK != err)
   {
      log_error(LOG_LEVEL_ERROR, "Couldn't parse rewritten request: %s.",
         jb_err_to_string(err));
   }
   else
   {
      /* XXX: ocmd is a misleading name */
      http->ocmd = strdup(http->cmd);
      if (http->ocmd == NULL)
      {
         log_error(LOG_LEVEL_FATAL,
            "Out of memory copying rewritten HTTP request line");
      }
   }

   return err;
}


#ifdef FEATURE_CONNECTION_KEEP_ALIVE
/*********************************************************************
 *
 * Function    :  server_response_is_complete
 *
 * Description :  Determines whether we should stop reading
 *                from the server socket.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  content_length = Length of content received so far.
 *
 * Returns     :  TRUE if the response is complete,
 *                FALSE otherwise.
 *
 *********************************************************************/
static int server_response_is_complete(struct client_state *csp,
   unsigned long long content_length)
{
   int content_length_known = !!(csp->flags & CSP_FLAG_CONTENT_LENGTH_SET);

   if (!strcmpic(csp->http->gpc, "HEAD"))
   {
      /*
       * "HEAD" implies no body, we are thus expecting
       * no content. XXX: incomplete "list" of methods?
       */
      csp->expected_content_length = 0;
      content_length_known = TRUE;
   }

   if (csp->http->status == 204 || csp->http->status == 304)
   {
      /*
       * Expect no body. XXX: incomplete "list" of status codes?
       */
      csp->expected_content_length = 0;
      content_length_known = TRUE;
   }

   return (content_length_known && ((0 == csp->expected_content_length)
            || (csp->expected_content_length <= content_length)));
}


#ifdef FEATURE_CONNECTION_SHARING
/*********************************************************************
 *
 * Function    :  wait_for_alive_connections
 *
 * Description :  Waits for alive connections to timeout.
 *
 * Parameters  :  N/A
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void wait_for_alive_connections(void)
{
   int connections_alive = close_unusable_connections();

   while (0 < connections_alive)
   {
      log_error(LOG_LEVEL_CONNECT,
         "Waiting for %d connections to timeout.",
         connections_alive);
      sleep(60);
      connections_alive = close_unusable_connections();
   }

   log_error(LOG_LEVEL_CONNECT, "No connections to wait for left.");

}
#endif /* def FEATURE_CONNECTION_SHARING */


/*********************************************************************
 *
 * Function    :  save_connection_destination
 *
 * Description :  Remembers a connection for reuse later on.
 *
 * Parameters  :
 *          1  :  sfd  = Open socket to remember.
 *          2  :  http = The destination for the connection.
 *          3  :  fwd  = The forwarder settings used.
 *          3  :  server_connection  = storage.
 *
 * Returns     : void
 *
 *********************************************************************/
void save_connection_destination(jb_socket sfd,
                                 const struct http_request *http,
                                 const struct forward_spec *fwd,
                                 struct reusable_connection *server_connection)
{
   assert(sfd != JB_INVALID_SOCKET);
   assert(NULL != http->host);

   server_connection->sfd = sfd;
   server_connection->host = strdup(http->host);
   if (NULL == server_connection->host)
   {
      log_error(LOG_LEVEL_FATAL, "Out of memory saving socket.");
   }
   server_connection->port = http->port;

   assert(NULL != fwd);
   assert(server_connection->gateway_host == NULL);
   assert(server_connection->gateway_port == 0);
   assert(server_connection->forwarder_type == 0);
   assert(server_connection->forward_host == NULL);
   assert(server_connection->forward_port == 0);

   server_connection->forwarder_type = fwd->type;
   if (NULL != fwd->gateway_host)
   {
      server_connection->gateway_host = strdup(fwd->gateway_host);
      if (NULL == server_connection->gateway_host)
      {
         log_error(LOG_LEVEL_FATAL, "Out of memory saving gateway_host.");
      }
   }
   else
   {
      server_connection->gateway_host = NULL;
   }
   server_connection->gateway_port = fwd->gateway_port;

   if (NULL != fwd->forward_host)
   {
      server_connection->forward_host = strdup(fwd->forward_host);
      if (NULL == server_connection->forward_host)
      {
         log_error(LOG_LEVEL_FATAL, "Out of memory saving forward_host.");
      }
   }
   else
   {
      server_connection->forward_host = NULL;
   }
   server_connection->forward_port = fwd->forward_port;
}


/*********************************************************************
 *
 * Function    : verify_request_length
 *
 * Description : Checks if we already got the whole client requests
 *               and sets CSP_FLAG_CLIENT_REQUEST_COMPLETELY_READ if
 *               we do.
 *
 *               Data that doesn't belong to the current request is
 *               thrown away to let the client retry on a clean socket.
 *
 *               XXX: This is a hack until we can deal with multiple
 *                    pipelined requests at the same time.
 *
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  void
 *
 *********************************************************************/
static void verify_request_length(struct client_state *csp)
{
   unsigned long long buffered_request_bytes =
      (unsigned long long)(csp->iob->eod - csp->iob->cur);

   if ((csp->expected_client_content_length != 0)
      && (buffered_request_bytes != 0))
   {
      if (csp->expected_client_content_length >= buffered_request_bytes)
      {
         csp->expected_client_content_length -= buffered_request_bytes;
         log_error(LOG_LEVEL_CONNECT, "Reduced expected bytes to %llu "
            "to account for the %llu ones we already got.",
            csp->expected_client_content_length, buffered_request_bytes);
      }
      else
      {
         assert(csp->iob->eod > csp->iob->cur + csp->expected_client_content_length);
         csp->iob->eod = csp->iob->cur + csp->expected_client_content_length;
         log_error(LOG_LEVEL_CONNECT, "Reducing expected bytes to 0. "
            "Marking the server socket tainted after throwing %llu bytes away.",
            buffered_request_bytes - csp->expected_client_content_length);
         csp->expected_client_content_length = 0;
         csp->flags |= CSP_FLAG_SERVER_SOCKET_TAINTED;
      }

      if (csp->expected_client_content_length == 0)
      {
         csp->flags |= CSP_FLAG_CLIENT_REQUEST_COMPLETELY_READ;
      }
   }

   if (!(csp->flags & CSP_FLAG_CLIENT_REQUEST_COMPLETELY_READ)
    && ((csp->iob->cur[0] != '\0') || (csp->expected_client_content_length != 0)))
   {
      csp->flags |= CSP_FLAG_SERVER_SOCKET_TAINTED;
      if (strcmpic(csp->http->gpc, "GET")
         && strcmpic(csp->http->gpc, "HEAD")
         && strcmpic(csp->http->gpc, "TRACE")
         && strcmpic(csp->http->gpc, "OPTIONS")
         && strcmpic(csp->http->gpc, "DELETE"))
      {
         /* XXX: this is an incomplete hack */
         csp->flags &= ~CSP_FLAG_CLIENT_REQUEST_COMPLETELY_READ;
         log_error(LOG_LEVEL_CONNECT,
            "There might be a request body. The connection will not be kept alive.");
      }
      else
      {
         /* XXX: and so is this */
         csp->flags |= CSP_FLAG_CLIENT_REQUEST_COMPLETELY_READ;
         log_error(LOG_LEVEL_CONNECT,
            "Possible pipeline attempt detected. The connection will not "
            "be kept alive and we will only serve the first request.");
         /* Nuke the pipelined requests from orbit, just to be sure. */
         csp->iob->buf[0] = '\0';
         csp->iob->eod = csp->iob->cur = csp->iob->buf;
      }
   }
   else
   {
      csp->flags |= CSP_FLAG_CLIENT_REQUEST_COMPLETELY_READ;
      log_error(LOG_LEVEL_CONNECT, "Complete client request received.");
   }
}
#endif /* FEATURE_CONNECTION_KEEP_ALIVE */


/*********************************************************************
 *
 * Function    :  mark_server_socket_tainted
 *
 * Description :  Makes sure we don't reuse a server socket
 *                (if we didn't read everything the server sent
 *                us reusing the socket would lead to garbage).
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  void.
 *
 *********************************************************************/
static void mark_server_socket_tainted(struct client_state *csp)
{
   /*
    * For consistency we always mark the server socket
    * tainted, however, to reduce the log noise we only
    * emit a log message if the server socket could have
    * actually been reused.
    */
   if ((csp->flags & CSP_FLAG_SERVER_CONNECTION_KEEP_ALIVE)
      && !(csp->flags |= CSP_FLAG_SERVER_SOCKET_TAINTED))
   {
      log_error(LOG_LEVEL_CONNECT,
         "Marking the server socket %d tainted.",
         csp->server_connection.sfd);
   }
   csp->flags |= CSP_FLAG_SERVER_SOCKET_TAINTED;
}

/*********************************************************************
 *
 * Function    :  get_request_line
 *
 * Description : Read the client request line.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  Pointer to request line or NULL in case of errors.
 *
 *********************************************************************/
static char *get_request_line(struct client_state *csp)
{
   char buf[BUFFER_SIZE];
   char *request_line = NULL;
   int len;

   memset(buf, 0, sizeof(buf));

   do
   {
      if (!data_is_available(csp->cfd, csp->config->socket_timeout))
      {
         log_error(LOG_LEVEL_CONNECT,
            "Stopped waiting for the request line. Timeout: %d.",
            csp->config->socket_timeout);
         write_socket(csp->cfd, CLIENT_CONNECTION_TIMEOUT_RESPONSE,
            strlen(CLIENT_CONNECTION_TIMEOUT_RESPONSE));
         return NULL;
      }

      len = read_socket(csp->cfd, buf, sizeof(buf) - 1);

      if (len <= 0) return NULL;

      /*
       * If there is no memory left for buffering the
       * request, there is nothing we can do but hang up
       */
      if (add_to_iob(csp, buf, len))
      {
         return NULL;
      }

      request_line = get_header(csp->iob);

   } while ((NULL != request_line) && ('\0' == *request_line));

   return request_line;

}


/*********************************************************************
 *
 * Function    :  receive_client_request
 *
 * Description : Read the client's request (more precisely the
 *               client headers) and answer it if necessary.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK, JB_ERR_PARSE or JB_ERR_MEMORY
 *
 *********************************************************************/
static jb_err receive_client_request(struct client_state *csp)
{
   char buf[BUFFER_SIZE];
   char *p;
   char *req = NULL;
   struct http_request *http;
   int len;
   jb_err err;

   /* Temporary copy of the client's headers before they get enlisted in csp->headers */
   struct list header_list;
   struct list *headers = &header_list;

   http = csp->http;

   memset(buf, 0, sizeof(buf));

   req = get_request_line(csp);
   if (req == NULL)
   {
      mark_server_socket_tainted(csp);
      return JB_ERR_PARSE;
   }
   assert(*req != '\0');

   if (client_protocol_is_unsupported(csp, req))
   {
      return JB_ERR_PARSE;
   }

#ifdef FEATURE_FORCE_LOAD
   /*
    * If this request contains the FORCE_PREFIX and blocks
    * aren't enforced, get rid of it and set the force flag.
    */
   if (strstr(req, FORCE_PREFIX))
   {
      if (csp->config->feature_flags & RUNTIME_FEATURE_ENFORCE_BLOCKS)
      {
         log_error(LOG_LEVEL_FORCE,
            "Ignored force prefix in request: \"%s\".", req);
      }
      else
      {
         strclean(req, FORCE_PREFIX);
         log_error(LOG_LEVEL_FORCE, "Enforcing request: \"%s\".", req);
         csp->flags |= CSP_FLAG_FORCED;
      }
   }
#endif /* def FEATURE_FORCE_LOAD */

   err = parse_http_request(req, http);
   freez(req);
   if (JB_ERR_OK != err)
   {
      write_socket(csp->cfd, CHEADER, strlen(CHEADER));
      /* XXX: Use correct size */
      log_error(LOG_LEVEL_CLF, "%s - - [%T] \"Invalid request\" 400 0", csp->ip_addr_str);
      log_error(LOG_LEVEL_ERROR,
         "Couldn't parse request line received from %s: %s",
         csp->ip_addr_str, jb_err_to_string(err));

      free_http_request(http);
      return JB_ERR_PARSE;
   }

   /* grab the rest of the client's headers */
   init_list(headers);
   for (;;)
   {
      p = get_header(csp->iob);

      if (p == NULL)
      {
         /* There are no additional headers to read. */
         break;
      }

      if (*p == '\0')
      {
         /*
          * We didn't receive a complete header
          * line yet, get the rest of it.
          */
         if (!data_is_available(csp->cfd, csp->config->socket_timeout))
         {
            log_error(LOG_LEVEL_ERROR,
               "Stopped grabbing the client headers.");
            destroy_list(headers);
            return JB_ERR_PARSE;
         }

         len = read_socket(csp->cfd, buf, sizeof(buf) - 1);
         if (len <= 0)
         {
            log_error(LOG_LEVEL_ERROR, "read from client failed: %E");
            destroy_list(headers);
            return JB_ERR_PARSE;
         }

         if (add_to_iob(csp, buf, len))
         {
            /*
             * If there is no memory left for buffering the
             * request, there is nothing we can do but hang up
             */
            destroy_list(headers);
            return JB_ERR_MEMORY;
         }
      }
      else
      {
         /*
          * We were able to read a complete
          * header and can finally enlist it.
          */
         enlist(headers, p);
         freez(p);
      }
   }

   if (http->host == NULL)
   {
      /*
       * If we still don't know the request destination,
       * the request is invalid or the client uses
       * Privoxy without its knowledge.
       */
      if (JB_ERR_OK != get_request_destination_elsewhere(csp, headers))
      {
         /*
          * Our attempts to get the request destination
          * elsewhere failed or Privoxy is configured
          * to only accept proxy requests.
          *
          * An error response has already been send
          * and we're done here.
          */
         return JB_ERR_PARSE;
      }
   }

   /*
    * Determine the actions for this URL
    */
#ifdef FEATURE_TOGGLE
   if (!(csp->flags & CSP_FLAG_TOGGLED_ON))
   {
      /* Most compatible set of actions (i.e. none) */
      init_current_action(csp->action);
   }
   else
#endif /* ndef FEATURE_TOGGLE */
   {
      get_url_actions(csp, http);
   }

   /*
    * Save a copy of the original request for logging
    */
   http->ocmd = strdup(http->cmd);
   if (http->ocmd == NULL)
   {
      log_error(LOG_LEVEL_FATAL,
         "Out of memory copying HTTP request line");
   }
   enlist(csp->headers, http->cmd);

   /* Append the previously read headers */
   list_append_list_unique(csp->headers, headers);
   destroy_list(headers);

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    : parse_client_request
 *
 * Description : Parses the client's request and decides what to do
 *               with it.
 *
 *               Note that since we're not using select() we could get
 *               blocked here if a client connected, then didn't say
 *               anything!
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK or JB_ERR_PARSE
 *
 *********************************************************************/
static jb_err parse_client_request(struct client_state *csp)
{
   struct http_request *http = csp->http;
   jb_err err;

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   if ((csp->config->feature_flags & RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE)
    && (!strcmpic(csp->http->ver, "HTTP/1.1"))
    && (csp->http->ssl == 0))
   {
      /* Assume persistence until further notice */
      csp->flags |= CSP_FLAG_CLIENT_CONNECTION_KEEP_ALIVE;
   }
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

   err = sed(csp, FILTER_CLIENT_HEADERS);
   if (JB_ERR_OK != err)
   {
      /* XXX: Should be handled in sed(). */
      assert(err == JB_ERR_PARSE);
      log_error(LOG_LEVEL_FATAL, "Failed to parse client headers.");
   }
   csp->flags |= CSP_FLAG_CLIENT_HEADER_PARSING_DONE;

   /* Check request line for rewrites. */
   if ((NULL == csp->headers->first->str)
      || (strcmp(http->cmd, csp->headers->first->str) &&
         (JB_ERR_OK != change_request_destination(csp))))
   {
      /*
       * A header filter broke the request line - bail out.
       */
      write_socket(csp->cfd, MESSED_UP_REQUEST_RESPONSE, strlen(MESSED_UP_REQUEST_RESPONSE));
      /* XXX: Use correct size */
      log_error(LOG_LEVEL_CLF,
         "%s - - [%T] \"Invalid request generated\" 500 0", csp->ip_addr_str);
      log_error(LOG_LEVEL_ERROR,
         "Invalid request line after applying header filters.");
      free_http_request(http);

      return JB_ERR_PARSE;
   }

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   if (csp->http->ssl == 0)
   {
      verify_request_length(csp);
   }
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  chat
 *
 * Description :  Once a connection from the client has been accepted,
 *                this function is called (via serve()) to handle the
 *                main business of the communication.  This function
 *                returns after dealing with a single request. It can
 *                be called multiple times with the same client socket
 *                if the client is keeping the connection alive.
 *
 *                The decision whether or not a client connection will
 *                be kept alive is up to the caller which also must
 *                close the client socket when done.
 *
 *                FIXME: chat is nearly thousand lines long.
 *                Ridiculous.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  Nothing.
 *
 *********************************************************************/
static void chat(struct client_state *csp)
{
   char buf[BUFFER_SIZE];
   char *hdr;
   char *p;
   fd_set rfds;
   int n;
   jb_socket maxfd;
   int server_body;
   int ms_iis5_hack = 0;
   unsigned long long byte_count = 0;
   const struct forward_spec *fwd;
   struct http_request *http;
   long len = 0; /* for buffer sizes (and negative error codes) */
   int buffer_and_filter_content = 0;

   /* Skeleton for HTTP response, if we should intercept the request */
   struct http_response *rsp;
   struct timeval timeout;
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   int watch_client_socket = 1;
#endif

   memset(buf, 0, sizeof(buf));

   http = csp->http;

   if (receive_client_request(csp) != JB_ERR_OK)
   {
      return;
   }
   if (parse_client_request(csp) != JB_ERR_OK)
   {
      return;
   }

   /* decide how to route the HTTP request */
   fwd = forward_url(csp, http);
   if (NULL == fwd)
   {
      log_error(LOG_LEVEL_FATAL, "gateway spec is NULL!?!?  This can't happen!");
      /* Never get here - LOG_LEVEL_FATAL causes program exit */
      return;
   }

   /*
    * build the http request to send to the server
    * we have to do one of the following:
    *
    * create = use the original HTTP request to create a new
    *          HTTP request that has either the path component
    *          without the http://domainspec (w/path) or the
    *          full orininal URL (w/url)
    *          Note that the path and/or the HTTP version may
    *          have been altered by now.
    *
    * connect = Open a socket to the host:port of the server
    *           and short-circuit server and client socket.
    *
    * pass =  Pass the request unchanged if forwarding a CONNECT
    *         request to a parent proxy. Note that we'll be sending
    *         the CFAIL message ourselves if connecting to the parent
    *         fails, but we won't send a CSUCCEED message if it works,
    *         since that would result in a double message (ours and the
    *         parent's). After sending the request to the parent, we simply
    *         tunnel.
    *
    * here's the matrix:
    *                        SSL
    *                    0        1
    *                +--------+--------+
    *                |        |        |
    *             0  | create | connect|
    *                | w/path |        |
    *  Forwarding    +--------+--------+
    *                |        |        |
    *             1  | create | pass   |
    *                | w/url  |        |
    *                +--------+--------+
    *
    */

   if (http->ssl && connect_port_is_forbidden(csp))
   {
      const char *acceptable_connect_ports =
         csp->action->string[ACTION_STRING_LIMIT_CONNECT];
      assert(NULL != acceptable_connect_ports);
      log_error(LOG_LEVEL_INFO, "Request from %s marked for blocking. "
         "limit-connect{%s} doesn't allow CONNECT requests to %s",
         csp->ip_addr_str, acceptable_connect_ports, csp->http->hostport);
      csp->action->flags |= ACTION_BLOCK;
      http->ssl = 0;
   }

   if (http->ssl == 0)
   {
      freez(csp->headers->first->str);
      build_request_line(csp, fwd, &csp->headers->first->str);
   }

   /*
    * We have a request. Check if one of the crunchers wants it.
    */
   if (crunch_response_triggered(csp, crunchers_all))
   {
      /*
       * Yes. The client got the crunch response and we're done here.
       */
      return;
   }

   log_error(LOG_LEVEL_GPC, "%s%s", http->hostport, http->path);

   if (fwd->forward_host)
   {
      log_error(LOG_LEVEL_CONNECT, "via [%s]:%d to: %s",
         fwd->forward_host, fwd->forward_port, http->hostport);
   }
   else
   {
      log_error(LOG_LEVEL_CONNECT, "to %s", http->hostport);
   }

   /* here we connect to the server, gateway, or the forwarder */

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   if ((csp->server_connection.sfd != JB_INVALID_SOCKET)
      && socket_is_still_alive(csp->server_connection.sfd)
      && connection_destination_matches(&csp->server_connection, http, fwd))
   {
      log_error(LOG_LEVEL_CONNECT,
         "Reusing server socket %u. Opened for %s.",
         csp->server_connection.sfd, csp->server_connection.host);
   }
   else
   {
      if (csp->server_connection.sfd != JB_INVALID_SOCKET)
      {
         log_error(LOG_LEVEL_CONNECT,
            "Closing server socket %u. Opened for %s.",
            csp->server_connection.sfd, csp->server_connection.host);
         close_socket(csp->server_connection.sfd);
         mark_connection_closed(&csp->server_connection);
      }
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

      csp->server_connection.sfd = forwarded_connect(fwd, http, csp);

      if (csp->server_connection.sfd == JB_INVALID_SOCKET)
      {
         if (fwd->type != SOCKS_NONE)
         {
            /* Socks error. */
            rsp = error_response(csp, "forwarding-failed");
         }
         else if (errno == EINVAL)
         {
            rsp = error_response(csp, "no-such-domain");
         }
         else
         {
            rsp = error_response(csp, "connect-failed");
         }

         /* Write the answer to the client */
         if (rsp != NULL)
         {
            send_crunch_response(csp, rsp);
         }

         return;
      }
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
      save_connection_destination(csp->server_connection.sfd,
         http, fwd, &csp->server_connection);
      csp->server_connection.keep_alive_timeout =
         (unsigned)csp->config->keep_alive_timeout;
   }
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

   if (fwd->forward_host || (http->ssl == 0))
   {
      hdr = list_to_text(csp->headers);
      if (hdr == NULL)
      {
         /* FIXME Should handle error properly */
         log_error(LOG_LEVEL_FATAL, "Out of memory parsing client header");
      }
      list_remove_all(csp->headers);

      /*
       * Write the client's (modified) header to the server
       * (along with anything else that may be in the buffer)
       */
      if (write_socket(csp->server_connection.sfd, hdr, strlen(hdr))
       || (flush_socket(csp->server_connection.sfd, csp->iob) <  0))
      {
         log_error(LOG_LEVEL_CONNECT,
            "write header to: %s failed: %E", http->hostport);

         rsp = error_response(csp, "connect-failed");
         if (rsp)
         {
            send_crunch_response(csp, rsp);
         }

         freez(hdr);
         return;
      }
      freez(hdr);
   }
   else
   {
      /*
       * We're running an SSL tunnel and we're not forwarding,
       * so just ditch the client headers, send the "connect succeeded"
       * message to the client, flush the rest, and get out of the way.
       */
      list_remove_all(csp->headers);
      if (write_socket(csp->cfd, CSUCCEED, strlen(CSUCCEED)))
      {
         return;
      }
      IOB_RESET(csp);
   }

   log_error(LOG_LEVEL_CONNECT, "to %s successful", http->hostport);

   csp->server_connection.request_sent = time(NULL);

   maxfd = (csp->cfd > csp->server_connection.sfd) ?
      csp->cfd : csp->server_connection.sfd;

   /* pass data between the client and server
    * until one or the other shuts down the connection.
    */

   server_body = 0;

   for (;;)
   {
#ifdef __OS2__
      /*
       * FD_ZERO here seems to point to an errant macro which crashes.
       * So do this by hand for now...
       */
      memset(&rfds,0x00,sizeof(fd_set));
#else
      FD_ZERO(&rfds);
#endif
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
      if (!watch_client_socket)
      {
         maxfd = csp->server_connection.sfd;
      }
      else
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */
      {
         FD_SET(csp->cfd, &rfds);
      }

      FD_SET(csp->server_connection.sfd, &rfds);

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
      if ((csp->flags & CSP_FLAG_CHUNKED)
         && !(csp->flags & CSP_FLAG_CONTENT_LENGTH_SET)
         && ((csp->iob->eod - csp->iob->cur) >= 5)
         && !memcmp(csp->iob->eod-5, "0\r\n\r\n", 5))
      {
         /*
          * XXX: This check should be obsolete now,
          *      but let's wait a while to be sure.
          */
         log_error(LOG_LEVEL_CONNECT,
            "Looks like we got the last chunk together with "
            "the server headers but didn't detect it earlier. "
            "We better stop reading.");
         byte_count = (unsigned long long)(csp->iob->eod - csp->iob->cur);
         csp->expected_content_length = byte_count;
         csp->flags |= CSP_FLAG_CONTENT_LENGTH_SET;
      }
      if (server_body && server_response_is_complete(csp, byte_count))
      {
         if (csp->expected_content_length == byte_count)
         {
            log_error(LOG_LEVEL_CONNECT,
               "Done reading from server. Content length: %llu as expected. "
               "Bytes most recently read: %d.",
               byte_count, len);
         }
         else
         {
            log_error(LOG_LEVEL_CONNECT,
               "Done reading from server. Expected content length: %llu. "
               "Actual content length: %llu. Bytes most recently read: %d.",
               csp->expected_content_length, byte_count, len);
         }
         len = 0;
         /*
          * XXX: should not jump around,
          * chat() is complicated enough already.
          */
         goto reading_done;
      }
#endif  /* FEATURE_CONNECTION_KEEP_ALIVE */

      timeout.tv_sec = csp->config->socket_timeout;
      timeout.tv_usec = 0;
      n = select((int)maxfd+1, &rfds, NULL, NULL, &timeout);

      if (n == 0)
      {
         log_error(LOG_LEVEL_ERROR,
            "Didn't receive data in time: %s", http->url);
         if ((byte_count == 0) && (http->ssl == 0))
         {
            send_crunch_response(csp, error_response(csp, "connection-timeout"));
         }
         mark_server_socket_tainted(csp);
         return;
      }
      else if (n < 0)
      {
         log_error(LOG_LEVEL_ERROR, "select() failed!: %E");
         mark_server_socket_tainted(csp);
         return;
      }

      /*
       * This is the body of the browser's request,
       * just read and write it.
       *
       * XXX: Make sure the client doesn't use pipelining
       * behind Privoxy's back.
       */
      if (FD_ISSET(csp->cfd, &rfds))
      {
         int max_bytes_to_read = sizeof(buf) - 1;

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
         if ((csp->flags & CSP_FLAG_CLIENT_REQUEST_COMPLETELY_READ))
         {
            if (data_is_available(csp->cfd, 0))
            {
               /*
                * If the next request is already waiting, we have
                * to stop select()ing the client socket. Otherwise
                * we would always return right away and get nothing
                * else done.
                */
               watch_client_socket = 0;
               log_error(LOG_LEVEL_CONNECT,
                  "Stopping to watch the client socket. "
                  "There's already another request waiting.");
               continue;
            }
            /*
             * If the client socket is set, but there's no data
             * available on the socket, the client went fishing
             * and continuing talking to the server makes no sense.
             */
            log_error(LOG_LEVEL_CONNECT,
               "The client closed socket %d while "
               "the server socket %d is still open.",
               csp->cfd, csp->server_connection.sfd);
            mark_server_socket_tainted(csp);
            break;
         }
         if (csp->expected_client_content_length != 0)
         {
            if (csp->expected_client_content_length < (sizeof(buf) - 1))
            {
               max_bytes_to_read = (int)csp->expected_client_content_length;
            }
            log_error(LOG_LEVEL_CONNECT,
               "Waiting for up to %d bytes from the client.",
               max_bytes_to_read);
         }
         assert(max_bytes_to_read < sizeof(buf));
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

         len = read_socket(csp->cfd, buf, max_bytes_to_read);

         if (len <= 0)
         {
            /* XXX: not sure if this is necessary. */
            mark_server_socket_tainted(csp);
            break; /* "game over, man" */
         }

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
         if (csp->expected_client_content_length != 0)
         {
            assert(len <= max_bytes_to_read);
            csp->expected_client_content_length -= (unsigned)len;
            log_error(LOG_LEVEL_CONNECT,
               "Expected client content length set to %llu "
               "after reading %d bytes.",
               csp->expected_client_content_length, len);
            if (csp->expected_client_content_length == 0)
            {
               log_error(LOG_LEVEL_CONNECT,
                  "Done reading from the client.");
               csp->flags |= CSP_FLAG_CLIENT_REQUEST_COMPLETELY_READ;
            }
         }
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

         if (write_socket(csp->server_connection.sfd, buf, (size_t)len))
         {
            log_error(LOG_LEVEL_ERROR, "write to: %s failed: %E", http->host);
            mark_server_socket_tainted(csp);
            return;
         }
         continue;
      }

      /*
       * The server wants to talk. It could be the header or the body.
       * If `hdr' is null, then it's the header otherwise it's the body.
       * FIXME: Does `hdr' really mean `host'? No.
       */
      if (FD_ISSET(csp->server_connection.sfd, &rfds))
      {
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
         /*
          * If we are buffering content, we don't want to eat up to
          * buffer-limit bytes if the client no longer cares about them.
          * If we aren't buffering, however, a dead client socket will be
          * noticed pretty much right away anyway, so we can reduce the
          * overhead by skipping the check.
          */
         if (buffer_and_filter_content && !socket_is_still_alive(csp->cfd))
         {
#ifdef _WIN32
            log_error(LOG_LEVEL_CONNECT,
               "The server still wants to talk, but the client may already have hung up on us.");
#else
            log_error(LOG_LEVEL_CONNECT,
               "The server still wants to talk, but the client hung up on us.");
            mark_server_socket_tainted(csp);
            return;
#endif /* def _WIN32 */
         }
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

         fflush(NULL);
         len = read_socket(csp->server_connection.sfd, buf, sizeof(buf) - 1);

         if (len < 0)
         {
            log_error(LOG_LEVEL_ERROR, "read from: %s failed: %E", http->host);

            if (http->ssl && (fwd->forward_host == NULL))
            {
               /*
                * Just hang up. We already confirmed the client's CONNECT
                * request with status code 200 and unencrypted content is
                * no longer welcome.
                */
               log_error(LOG_LEVEL_ERROR,
                  "CONNECT already confirmed. Unable to tell the client about the problem.");
               return;
            }
            else if (byte_count)
            {
               /*
                * Just hang up. We already transmitted the original headers
                * and parts of the original content and therefore missed the
                * chance to send an error message (without risking data corruption).
                *
                * XXX: we could retry with a fancy range request here.
                */
               log_error(LOG_LEVEL_ERROR, "Already forwarded the original headers. "
                  "Unable to tell the client about the problem.");
               mark_server_socket_tainted(csp);
               return;
            }
            /*
             * XXX: Consider handling the cases above the same.
             */
            mark_server_socket_tainted(csp);
            len = 0;
         }

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
         if (csp->flags & CSP_FLAG_CHUNKED)
         {
            if ((len >= 5) && !memcmp(buf+len-5, "0\r\n\r\n", 5))
            {
               /* XXX: this is a temporary hack */
               log_error(LOG_LEVEL_CONNECT,
                  "Looks like we reached the end of the last chunk. "
                  "We better stop reading.");
               csp->expected_content_length = byte_count + (unsigned long long)len;
               csp->flags |= CSP_FLAG_CONTENT_LENGTH_SET;
            }
         }
         reading_done:
#endif  /* FEATURE_CONNECTION_KEEP_ALIVE */

         /*
          * Add a trailing zero to let be able to use string operations.
          * XXX: do we still need this with filter_popups gone?
          */
         buf[len] = '\0';

         /*
          * Normally, this would indicate that we've read
          * as much as the server has sent us and we can
          * close the client connection.  However, Microsoft
          * in its wisdom has released IIS/5 with a bug that
          * prevents it from sending the trailing \r\n in
          * a 302 redirect header (and possibly other headers).
          * To work around this if we've haven't parsed
          * a full header we'll append a trailing \r\n
          * and see if this now generates a valid one.
          *
          * This hack shouldn't have any impacts.  If we've
          * already transmitted the header or if this is a
          * SSL connection, then we won't bother with this
          * hack.  So we only work on partially received
          * headers.  If we append a \r\n and this still
          * doesn't generate a valid header, then we won't
          * transmit anything to the client.
          */
         if (len == 0)
         {

            if (server_body || http->ssl)
            {
               /*
                * If we have been buffering up the document,
                * now is the time to apply content modification
                * and send the result to the client.
                */
               if (buffer_and_filter_content)
               {
                  p = execute_content_filters(csp);
                  /*
                   * If content filtering fails, use the original
                   * buffer and length.
                   * (see p != NULL ? p : csp->iob->cur below)
                   */
                  if (NULL == p)
                  {
                     csp->content_length = (size_t)(csp->iob->eod - csp->iob->cur);
                  }
#ifdef FEATURE_COMPRESSION
                  else if ((csp->flags & CSP_FLAG_CLIENT_SUPPORTS_DEFLATE)
                     && (csp->content_length > LOWER_LENGTH_LIMIT_FOR_COMPRESSION))
                  {
                     char *compressed_content = compress_buffer(p,
                        (size_t *)&csp->content_length, csp->config->compression_level);
                     if (compressed_content != NULL)
                     {
                        freez(p);
                        p = compressed_content;
                        csp->flags |= CSP_FLAG_BUFFERED_CONTENT_DEFLATED;
                     }
                  }
#endif

                  if (JB_ERR_OK != update_server_headers(csp))
                  {
                     log_error(LOG_LEVEL_FATAL,
                        "Failed to update server headers. after filtering.");
                  }

                  hdr = list_to_text(csp->headers);
                  if (hdr == NULL)
                  {
                     /* FIXME Should handle error properly */
                     log_error(LOG_LEVEL_FATAL, "Out of memory parsing server header");
                  }

                  if (write_socket(csp->cfd, hdr, strlen(hdr))
                   || write_socket(csp->cfd,
                         ((p != NULL) ? p : csp->iob->cur), (size_t)csp->content_length))
                  {
                     log_error(LOG_LEVEL_ERROR, "write modified content to client failed: %E");
                     freez(hdr);
                     freez(p);
                     mark_server_socket_tainted(csp);
                     return;
                  }

                  freez(hdr);
                  freez(p);
               }

               break; /* "game over, man" */
            }

            /*
             * This is NOT the body, so
             * Let's pretend the server just sent us a blank line.
             */
            snprintf(buf, sizeof(buf), "\r\n");
            len = (int)strlen(buf);

            /*
             * Now, let the normal header parsing algorithm below do its
             * job.  If it fails, we'll exit instead of continuing.
             */

            ms_iis5_hack = 1;
         }

         /*
          * If this is an SSL connection or we're in the body
          * of the server document, just write it to the client,
          * unless we need to buffer the body for later content-filtering
          */
         if (server_body || http->ssl)
         {
            if (buffer_and_filter_content)
            {
               /*
                * If there is no memory left for buffering the content, or the buffer limit
                * has been reached, switch to non-filtering mode, i.e. make & write the
                * header, flush the iob and buf, and get out of the way.
                */
               if (add_to_iob(csp, buf, len))
               {
                  size_t hdrlen;
                  long flushed;

                  log_error(LOG_LEVEL_INFO,
                     "Flushing header and buffers. Stepping back from filtering.");

                  hdr = list_to_text(csp->headers);
                  if (hdr == NULL)
                  {
                     /*
                      * Memory is too tight to even generate the header.
                      * Send our static "Out-of-memory" page.
                      */
                     log_error(LOG_LEVEL_ERROR, "Out of memory while trying to flush.");
                     rsp = cgi_error_memory();
                     send_crunch_response(csp, rsp);
                     mark_server_socket_tainted(csp);
                     return;
                  }
                  hdrlen = strlen(hdr);

                  if (write_socket(csp->cfd, hdr, hdrlen)
                   || ((flushed = flush_socket(csp->cfd, csp->iob)) < 0)
                   || (write_socket(csp->cfd, buf, (size_t)len)))
                  {
                     log_error(LOG_LEVEL_CONNECT,
                        "Flush header and buffers to client failed: %E");
                     freez(hdr);
                     mark_server_socket_tainted(csp);
                     return;
                  }

                  /*
                   * Reset the byte_count to the amount of bytes
                   * we just flushed. len will be added a few lines below,
                   * hdrlen doesn't matter for LOG_LEVEL_CLF.
                   */
                  byte_count = (unsigned long long)flushed;
                  freez(hdr);
                  buffer_and_filter_content = 0;
                  server_body = 1;
               }
            }
            else
            {
               if (write_socket(csp->cfd, buf, (size_t)len))
               {
                  log_error(LOG_LEVEL_ERROR, "write to client failed: %E");
                  mark_server_socket_tainted(csp);
                  return;
               }
            }
            byte_count += (unsigned long long)len;
            continue;
         }
         else
         {
            /*
             * We're still looking for the end of the server's header.
             * Buffer up the data we just read.  If that fails, there's
             * little we can do but send our static out-of-memory page.
             */
            if (add_to_iob(csp, buf, len))
            {
               log_error(LOG_LEVEL_ERROR, "Out of memory while looking for end of server headers.");
               rsp = cgi_error_memory();
               send_crunch_response(csp, rsp);
               mark_server_socket_tainted(csp);
               return;
            }

            /* Convert iob into something sed() can digest */
            if (JB_ERR_PARSE == get_server_headers(csp))
            {
               if (ms_iis5_hack)
               {
                  /*
                   * Well, we tried our MS IIS/5 hack and it didn't work.
                   * The header is incomplete and there isn't anything
                   * we can do about it.
                   */
                  log_error(LOG_LEVEL_ERROR, "Invalid server headers. "
                     "Applying the MS IIS5 hack didn't help.");
                  log_error(LOG_LEVEL_CLF,
                     "%s - - [%T] \"%s\" 502 0", csp->ip_addr_str, http->cmd);
                  write_socket(csp->cfd, INVALID_SERVER_HEADERS_RESPONSE,
                     strlen(INVALID_SERVER_HEADERS_RESPONSE));
                  mark_server_socket_tainted(csp);
                  return;
               }
               else
               {
                  /*
                   * Since we have to wait for more from the server before
                   * we can parse the headers we just continue here.
                   */
                  log_error(LOG_LEVEL_CONNECT,
                     "Continuing buffering headers. Bytes most recently read: %d.",
                     len);
                  continue;
               }
            }
            else
            {
               /*
                * Account for the content bytes we
                * might have gotten with the headers.
                */
               assert(csp->iob->eod >= csp->iob->cur);
               byte_count = (unsigned long long)(csp->iob->eod - csp->iob->cur);
            }

            /* Did we actually get anything? */
            if (NULL == csp->headers->first)
            {
               if ((csp->flags & CSP_FLAG_REUSED_CLIENT_CONNECTION))
               {
                  log_error(LOG_LEVEL_ERROR,
                     "No server or forwarder response received on socket %d. "
                     "Closing client socket %d without sending data.",
                     csp->server_connection.sfd, csp->cfd);
                  log_error(LOG_LEVEL_CLF,
                     "%s - - [%T] \"%s\" 502 0", csp->ip_addr_str, http->cmd);
               }
               else
               {
                  log_error(LOG_LEVEL_ERROR,
                     "No server or forwarder response received on socket %d.",
                     csp->server_connection.sfd);
                  send_crunch_response(csp, error_response(csp, "no-server-data"));
               }
               free_http_request(http);
               mark_server_socket_tainted(csp);
               return;
            }

            assert(csp->headers->first->str);
            assert(!http->ssl);
            if (strncmpic(csp->headers->first->str, "HTTP", 4) &&
                strncmpic(csp->headers->first->str, "ICY", 3))
            {
               /*
                * It doesn't look like a HTTP (or Shoutcast) response:
                * tell the client and log the problem.
                */
               if (strlen(csp->headers->first->str) > 30)
               {
                  csp->headers->first->str[30] = '\0';
               }
               log_error(LOG_LEVEL_ERROR,
                  "Invalid server or forwarder response. Starts with: %s",
                  csp->headers->first->str);
               log_error(LOG_LEVEL_CLF,
                  "%s - - [%T] \"%s\" 502 0", csp->ip_addr_str, http->cmd);
               write_socket(csp->cfd, INVALID_SERVER_HEADERS_RESPONSE,
                  strlen(INVALID_SERVER_HEADERS_RESPONSE));
               free_http_request(http);
               mark_server_socket_tainted(csp);
               return;
            }

            /*
             * We have now received the entire server header,
             * filter it and send the result to the client
             */
            if (JB_ERR_OK != sed(csp, FILTER_SERVER_HEADERS))
            {
               log_error(LOG_LEVEL_FATAL, "Failed to parse server headers.");
            }
            hdr = list_to_text(csp->headers);
            if (hdr == NULL)
            {
               /* FIXME Should handle error properly */
               log_error(LOG_LEVEL_FATAL, "Out of memory parsing server header");
            }

            if ((csp->flags & CSP_FLAG_CHUNKED)
               && !(csp->flags & CSP_FLAG_CONTENT_LENGTH_SET)
               && ((csp->iob->eod - csp->iob->cur) >= 5)
               && !memcmp(csp->iob->eod-5, "0\r\n\r\n", 5))
            {
               log_error(LOG_LEVEL_CONNECT,
                  "Looks like we got the last chunk together with "
                  "the server headers. We better stop reading.");
               byte_count = (unsigned long long)(csp->iob->eod - csp->iob->cur);
               csp->expected_content_length = byte_count;
               csp->flags |= CSP_FLAG_CONTENT_LENGTH_SET;
            }

            csp->server_connection.response_received = time(NULL);

            if (crunch_response_triggered(csp, crunchers_light))
            {
               /*
                * One of the tags created by a server-header
                * tagger triggered a crunch. We already
                * delivered the crunch response to the client
                * and are done here after cleaning up.
                */
                freez(hdr);
                mark_server_socket_tainted(csp);
                return;
            }
            /* Buffer and pcrs filter this if appropriate. */

            if (!http->ssl) /* We talk plaintext */
            {
               buffer_and_filter_content = content_requires_filtering(csp);
            }
            /*
             * Only write if we're not buffering for content modification
             */
            if (!buffer_and_filter_content)
            {
               /*
                * Write the server's (modified) header to
                * the client (along with anything else that
                * may be in the buffer)
                */

               if (write_socket(csp->cfd, hdr, strlen(hdr))
                || ((len = flush_socket(csp->cfd, csp->iob)) < 0))
               {
                  log_error(LOG_LEVEL_CONNECT, "write header to client failed: %E");

                  /*
                   * The write failed, so don't bother mentioning it
                   * to the client... it probably can't hear us anyway.
                   */
                  freez(hdr);
                  mark_server_socket_tainted(csp);
                  return;
               }
            }

            /* we're finished with the server's header */

            freez(hdr);
            server_body = 1;

            /*
             * If this was a MS IIS/5 hack then it means the server
             * has already closed the connection. Nothing more to read.
             * Time to bail.
             */
            if (ms_iis5_hack)
            {
               log_error(LOG_LEVEL_ERROR,
                  "Closed server connection detected. "
                  "Applying the MS IIS5 hack didn't help.");
               log_error(LOG_LEVEL_CLF,
                  "%s - - [%T] \"%s\" 502 0", csp->ip_addr_str, http->cmd);
               write_socket(csp->cfd, INVALID_SERVER_HEADERS_RESPONSE,
                  strlen(INVALID_SERVER_HEADERS_RESPONSE));
               mark_server_socket_tainted(csp);
               return;
            }
         }
         continue;
      }
      mark_server_socket_tainted(csp);
      return; /* huh? we should never get here */
   }

   if (csp->content_length == 0)
   {
      /*
       * If Privoxy didn't recalculate the Content-Length,
       * byte_count is still correct.
       */
      csp->content_length = byte_count;
   }

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   if ((csp->flags & CSP_FLAG_CONTENT_LENGTH_SET)
      && (csp->expected_content_length != byte_count))
   {
      log_error(LOG_LEVEL_CONNECT,
         "Received %llu bytes while expecting %llu.",
         byte_count, csp->expected_content_length);
      mark_server_socket_tainted(csp);
   }
#endif

   log_error(LOG_LEVEL_CLF, "%s - - [%T] \"%s\" 200 %llu",
      csp->ip_addr_str, http->ocmd, csp->content_length);

   csp->server_connection.timestamp = time(NULL);
}


#ifdef FEATURE_CONNECTION_KEEP_ALIVE
/*********************************************************************
 *
 * Function    :  prepare_csp_for_next_request
 *
 * Description :  Put the csp in a mostly vergin state.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void prepare_csp_for_next_request(struct client_state *csp)
{
   unsigned int toggled_on_flag_set = (0 != (csp->flags & CSP_FLAG_TOGGLED_ON));

   csp->content_type = 0;
   csp->content_length = 0;
   csp->expected_content_length = 0;
   csp->expected_client_content_length = 0;
   list_remove_all(csp->headers);
   freez(csp->iob->buf);
   memset(csp->iob, 0, sizeof(csp->iob));
   freez(csp->error_message);
   free_http_request(csp->http);
   destroy_list(csp->headers);
   destroy_list(csp->tags);
   free_current_action(csp->action);
   if (NULL != csp->fwd)
   {
      unload_forward_spec(csp->fwd);
      csp->fwd = NULL;
   }
   /* XXX: Store per-connection flags someplace else. */
   csp->flags = (CSP_FLAG_ACTIVE | CSP_FLAG_REUSED_CLIENT_CONNECTION);
   if (toggled_on_flag_set)
   {
      csp->flags |= CSP_FLAG_TOGGLED_ON;
   }
}
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */


/*********************************************************************
 *
 * Function    :  serve
 *
 * Description :  This is little more than chat.  We only "serve" to
 *                to close (or remember) any socket that chat may have
 *                opened.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  N/A
 *
 *********************************************************************/
#ifdef AMIGA
void serve(struct client_state *csp)
#else /* ifndef AMIGA */
static void serve(struct client_state *csp)
#endif /* def AMIGA */
{
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
#ifdef FEATURE_CONNECTION_SHARING
   static int monitor_thread_running = 0;
#endif /* def FEATURE_CONNECTION_SHARING */
   int continue_chatting = 0;

   do
   {
      unsigned int latency;
      int config_file_change_detected = 0; /* Only used for debugging */

      chat(csp);

      /*
       * If the request has been crunched,
       * the calculated latency is zero.
       */
      latency = (unsigned)(csp->server_connection.response_received -
         csp->server_connection.request_sent) / 2;

      continue_chatting = (csp->config->feature_flags
         & RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE)
         && !(csp->flags & CSP_FLAG_SERVER_SOCKET_TAINTED)
         && ((csp->flags & CSP_FLAG_SERVER_CONNECTION_KEEP_ALIVE)
             || (csp->flags & CSP_FLAG_CRUNCHED))
         && (csp->cfd != JB_INVALID_SOCKET)
         && ((csp->flags & CSP_FLAG_CLIENT_CONNECTION_KEEP_ALIVE)
             || (csp->config->feature_flags &
                RUNTIME_FEATURE_CONNECTION_SHARING));

      if (continue_chatting && !(csp->flags & CSP_FLAG_CRUNCHED))
      {
         continue_chatting = (csp->server_connection.sfd != JB_INVALID_SOCKET)
            && socket_is_still_alive(csp->server_connection.sfd);
         if (continue_chatting)
         {
            if (!(csp->flags & CSP_FLAG_SERVER_KEEP_ALIVE_TIMEOUT_SET))
            {
               csp->server_connection.keep_alive_timeout = csp->config->default_server_timeout;
               log_error(LOG_LEVEL_CONNECT,
                  "The server didn't specify how long the connection will stay open. "
                  "Assumed timeout is: %u.", csp->server_connection.keep_alive_timeout);
            }
            continue_chatting = (latency < csp->server_connection.keep_alive_timeout);
         }
      }

      if (continue_chatting && any_loaded_file_changed(csp->config->config_file_list))
      {
         continue_chatting = 0;
         config_file_change_detected = 1;
      }

      if (continue_chatting)
      {
         unsigned int client_timeout = 1; /* XXX: Use something else here? */

         if (0 != (csp->flags & CSP_FLAG_CLIENT_CONNECTION_KEEP_ALIVE))
         {
            if (csp->server_connection.sfd != JB_INVALID_SOCKET)
            {
               client_timeout = (unsigned)csp->server_connection.keep_alive_timeout - latency;
               log_error(LOG_LEVEL_CONNECT,
                  "Waiting for the next client request on socket %d. "
                  "Keeping the server socket %d to %s open.",
                  csp->cfd, csp->server_connection.sfd, csp->server_connection.host);
            }
            else
            {
               log_error(LOG_LEVEL_CONNECT,
                  "Waiting for the next client request on socket %d. "
                  "No server socket to keep open.", csp->cfd);
            }
         }
         if ((csp->flags & CSP_FLAG_CLIENT_CONNECTION_KEEP_ALIVE)
            && data_is_available(csp->cfd, (int)client_timeout)
            && socket_is_still_alive(csp->cfd))
         {
            log_error(LOG_LEVEL_CONNECT,
               "Client request arrived in time on socket %d.", csp->cfd);
            prepare_csp_for_next_request(csp);
         }
         else
         {
            if (0 != (csp->flags & CSP_FLAG_CLIENT_CONNECTION_KEEP_ALIVE))
            {
               log_error(LOG_LEVEL_CONNECT,
                  "No additional client request received in time on socket %d.",
                  csp->cfd);
            }
#ifdef FEATURE_CONNECTION_SHARING
            if ((csp->config->feature_flags & RUNTIME_FEATURE_CONNECTION_SHARING)
               && (socket_is_still_alive(csp->server_connection.sfd)))
            {
               time_t time_open = time(NULL) - csp->server_connection.timestamp;

               if (csp->server_connection.keep_alive_timeout < time_open - (time_t)latency)
               {
                  break;
               }

               remember_connection(&csp->server_connection);
               csp->server_connection.sfd = JB_INVALID_SOCKET;
               close_socket(csp->cfd);
               csp->cfd = JB_INVALID_SOCKET;
               privoxy_mutex_lock(&connection_reuse_mutex);
               if (!monitor_thread_running)
               {
                  monitor_thread_running = 1;
                  privoxy_mutex_unlock(&connection_reuse_mutex);
                  wait_for_alive_connections();
                  privoxy_mutex_lock(&connection_reuse_mutex);
                  monitor_thread_running = 0;
               }
               privoxy_mutex_unlock(&connection_reuse_mutex);
            }
#endif /* def FEATURE_CONNECTION_SHARING */
            break;
         }
      }
      else if (csp->server_connection.sfd != JB_INVALID_SOCKET)
      {
         log_error(LOG_LEVEL_CONNECT,
            "The connection on server socket %d to %s isn't reusable. Closing. "
            "Server connection: keep-alive %u, tainted: %u, socket alive %u. "
            "Client connection: socket alive: %u. Server timeout: %u. "
            "Configuration file change detected: %u",
            csp->server_connection.sfd, csp->server_connection.host,
            0 != (csp->flags & CSP_FLAG_SERVER_CONNECTION_KEEP_ALIVE),
            0 != (csp->flags & CSP_FLAG_SERVER_SOCKET_TAINTED),
            socket_is_still_alive(csp->server_connection.sfd),
            socket_is_still_alive(csp->cfd),
            csp->server_connection.keep_alive_timeout,
            config_file_change_detected);
      }
   } while (continue_chatting);

#else
   chat(csp);
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

   if (csp->server_connection.sfd != JB_INVALID_SOCKET)
   {
#ifdef FEATURE_CONNECTION_SHARING
      if (csp->config->feature_flags & RUNTIME_FEATURE_CONNECTION_SHARING)
      {
         forget_connection(csp->server_connection.sfd);
      }
#endif /* def FEATURE_CONNECTION_SHARING */
      close_socket(csp->server_connection.sfd);
   }

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   mark_connection_closed(&csp->server_connection);
#endif

   if (csp->cfd != JB_INVALID_SOCKET)
   {
      close_socket(csp->cfd);
   }

   csp->flags &= ~CSP_FLAG_ACTIVE;

}


#ifdef __BEOS__
/*********************************************************************
 *
 * Function    :  server_thread
 *
 * Description :  We only exist to call `serve' in a threaded environment.
 *
 * Parameters  :
 *          1  :  data = Current client state (buffers, headers, etc...)
 *
 * Returns     :  Always 0.
 *
 *********************************************************************/
static int32 server_thread(void *data)
{
   serve((struct client_state *) data);
   return 0;

}
#endif


#if !defined(_WIN32) || defined(_WIN_CONSOLE)
/*********************************************************************
 *
 * Function    :  usage
 *
 * Description :  Print usage info & exit.
 *
 * Parameters  :  Pointer to argv[0] for identifying ourselves
 *
 * Returns     :  No. ,-)
 *
 *********************************************************************/
static void usage(const char *myname)
{
   printf("Privoxy version " VERSION " (" HOME_PAGE_URL ")\n"
          "Usage: %s "
#if defined(unix)
          "[--chroot] "
#endif /* defined(unix) */
          "[--help] "
#if defined(unix)
          "[--no-daemon] [--pidfile pidfile] [--pre-chroot-nslookup hostname] [--user user[.group]] "
#endif /* defined(unix) */
          "[--version] [configfile]\n"
          "Aborting\n", myname);

   exit(2);

}
#endif /* #if !defined(_WIN32) || defined(_WIN_CONSOLE) */


#ifdef MUTEX_LOCKS_AVAILABLE
/*********************************************************************
 *
 * Function    :  privoxy_mutex_lock
 *
 * Description :  Locks a mutex.
 *
 * Parameters  :
 *          1  :  mutex = The mutex to lock.
 *
 * Returns     :  Void. May exit in case of errors.
 *
 *********************************************************************/
void privoxy_mutex_lock(privoxy_mutex_t *mutex)
{
#ifdef FEATURE_PTHREAD
   int err = pthread_mutex_lock(mutex);
   if (err)
   {
      if (mutex != &log_mutex)
      {
         log_error(LOG_LEVEL_FATAL,
            "Mutex locking failed: %s.\n", strerror(err));
      }
      exit(1);
   }
#else
   EnterCriticalSection(mutex);
#endif /* def FEATURE_PTHREAD */
}


/*********************************************************************
 *
 * Function    :  privoxy_mutex_unlock
 *
 * Description :  Unlocks a mutex.
 *
 * Parameters  :
 *          1  :  mutex = The mutex to unlock.
 *
 * Returns     :  Void. May exit in case of errors.
 *
 *********************************************************************/
void privoxy_mutex_unlock(privoxy_mutex_t *mutex)
{
#ifdef FEATURE_PTHREAD
   int err = pthread_mutex_unlock(mutex);
   if (err)
   {
      if (mutex != &log_mutex)
      {
         log_error(LOG_LEVEL_FATAL,
            "Mutex unlocking failed: %s.\n", strerror(err));
      }
      exit(1);
   }
#else
   LeaveCriticalSection(mutex);
#endif /* def FEATURE_PTHREAD */
}


/*********************************************************************
 *
 * Function    :  privoxy_mutex_init
 *
 * Description :  Prepares a mutex.
 *
 * Parameters  :
 *          1  :  mutex = The mutex to initialize.
 *
 * Returns     :  Void. May exit in case of errors.
 *
 *********************************************************************/
static void privoxy_mutex_init(privoxy_mutex_t *mutex)
{
#ifdef FEATURE_PTHREAD
   int err = pthread_mutex_init(mutex, 0);
   if (err)
   {
      printf("Fatal error. Mutex initialization failed: %s.\n",
         strerror(err));
      exit(1);
   }
#else
   InitializeCriticalSection(mutex);
#endif /* def FEATURE_PTHREAD */
}
#endif /* def MUTEX_LOCKS_AVAILABLE */

/*********************************************************************
 *
 * Function    :  initialize_mutexes
 *
 * Description :  Prepares mutexes if mutex support is available.
 *
 * Parameters  :  None
 *
 * Returns     :  Void, exits in case of errors.
 *
 *********************************************************************/
static void initialize_mutexes(void)
{
#ifdef MUTEX_LOCKS_AVAILABLE
   /*
    * Prepare global mutex semaphores
    */
   privoxy_mutex_init(&log_mutex);
   privoxy_mutex_init(&log_init_mutex);
   privoxy_mutex_init(&connection_reuse_mutex);

   /*
    * XXX: The assumptions below are a bit naive
    * and can cause locks that aren't necessary.
    *
    * For example older FreeBSD versions (< 6.x?)
    * have no gethostbyname_r, but gethostbyname is
    * thread safe.
    */
#if !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_GETHOSTBYNAME_R)
   privoxy_mutex_init(&resolver_mutex);
#endif /* !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_GETHOSTBYNAME_R) */
   /*
    * XXX: should we use a single mutex for
    * localtime() and gmtime() as well?
    */
#ifndef HAVE_GMTIME_R
   privoxy_mutex_init(&gmtime_mutex);
#endif /* ndef HAVE_GMTIME_R */

#ifndef HAVE_LOCALTIME_R
   privoxy_mutex_init(&localtime_mutex);
#endif /* ndef HAVE_GMTIME_R */

#ifndef HAVE_RANDOM
   privoxy_mutex_init(&rand_mutex);
#endif /* ndef HAVE_RANDOM */

#endif /* def MUTEX_LOCKS_AVAILABLE */
}


/*********************************************************************
 *
 * Function    :  main
 *
 * Description :  Load the config file and start the listen loop.
 *                This function is a lot more *sane* with the `load_config'
 *                and `listen_loop' functions; although it stills does
 *                a *little* too much for my taste.
 *
 * Parameters  :
 *          1  :  argc = Number of parameters (including $0).
 *          2  :  argv = Array of (char *)'s to the parameters.
 *
 * Returns     :  1 if : can't open config file, unrecognized directive,
 *                stats requested in multi-thread mode, can't open the
 *                log file, can't open the jar file, listen port is invalid,
 *                any load fails, and can't bind port.
 *
 *                Else main never returns, the process must be signaled
 *                to terminate execution.  Or, on Windows, use the
 *                "File", "Exit" menu option.
 *
 *********************************************************************/
#ifdef __MINGW32__
int real_main(int argc, char **argv)
#else
int main(int argc, char **argv)
#endif
{
   int argc_pos = 0;
   unsigned int random_seed;
#ifdef unix
   struct passwd *pw = NULL;
   struct group *grp = NULL;
   int do_chroot = 0;
   char *pre_chroot_nslookup_to_load_resolver = NULL;
#endif

   Argc = argc;
   Argv = argv;

   configfile =
#if !defined(_WIN32)
   "config"
#else
   "config.txt"
#endif
      ;

   /* Prepare mutexes if supported and necessary. */
   initialize_mutexes();

   /* Enable logging until further notice. */
   init_log_module();

   /*
    * Parse the command line arguments
    *
    * XXX: simply printing usage information in case of
    * invalid arguments isn't particularly user friendly.
    */
   while (++argc_pos < argc)
   {
#ifdef _WIN32
      /* Check to see if the service must be installed or uninstalled */
      if (strncmp(argv[argc_pos], "--install", 9) == 0)
      {
         const char *pName = argv[argc_pos] + 9;
         if (*pName == ':')
            pName++;
         exit( (install_service(pName)) ? 0 : 1 );
      }
      else if (strncmp(argv[argc_pos], "--uninstall", 11) == 0)
      {
         const char *pName = argv[argc_pos] + 11;
         if (*pName == ':')
            pName++;
         exit((uninstall_service(pName)) ? 0 : 1);
      }
      else if (strcmp(argv[argc_pos], "--service" ) == 0)
      {
         bRunAsService = TRUE;
         w32_set_service_cwd();
         atexit(w32_service_exit_notify);
      }
      else
#endif /* defined(_WIN32) */


#if !defined(_WIN32) || defined(_WIN_CONSOLE)

      if (strcmp(argv[argc_pos], "--help") == 0)
      {
         usage(argv[0]);
      }

      else if(strcmp(argv[argc_pos], "--version") == 0)
      {
         printf("Privoxy version " VERSION " (" HOME_PAGE_URL ")\n");
         exit(0);
      }

#if defined(unix)

      else if (strcmp(argv[argc_pos], "--no-daemon" ) == 0)
      {
         set_debug_level(LOG_LEVEL_FATAL | LOG_LEVEL_ERROR | LOG_LEVEL_INFO);
         daemon_mode = 0;
      }

      else if (strcmp(argv[argc_pos], "--pidfile" ) == 0)
      {
         if (++argc_pos == argc) usage(argv[0]);
         pidfile = strdup(argv[argc_pos]);
      }

      else if (strcmp(argv[argc_pos], "--user" ) == 0)
      {
         char *user_arg;
         char *group_name;

         if (++argc_pos == argc) usage(argv[argc_pos]);

         user_arg = strdup(argv[argc_pos]);
         if (NULL == user_arg)
         {
            log_error(LOG_LEVEL_FATAL,
               "Out of memory splitting --user argument '%s'.", argv[argc_pos]);
         }
         group_name = strchr(user_arg, '.');
         if (NULL != group_name)
         {
            /* Nul-terminate the user name */
            *group_name = '\0';

            /* Skip the former delimiter to actually reach the group name */
            group_name++;

            grp = getgrnam(group_name);
            if (NULL == grp)
            {
               log_error(LOG_LEVEL_FATAL, "Group '%s' not found.", group_name);
            }
         }
         pw = getpwnam(user_arg);
         if (NULL == pw)
         {
            log_error(LOG_LEVEL_FATAL, "User '%s' not found.", user_arg);
         }

         freez(user_arg);
      }

      else if (strcmp(argv[argc_pos], "--pre-chroot-nslookup" ) == 0)
      {
         if (++argc_pos == argc) usage(argv[0]);
         pre_chroot_nslookup_to_load_resolver = strdup(argv[argc_pos]);
      }

      else if (strcmp(argv[argc_pos], "--chroot" ) == 0)
      {
         do_chroot = 1;
      }
#endif /* defined(unix) */

      else if (argc_pos + 1 != argc)
      {
         /*
          * This is neither the last command line
          * option, nor was it recognized before,
          * therefore it must be invalid.
          */
         usage(argv[0]);
      }
      else

#endif /* defined(_WIN32) && !defined(_WIN_CONSOLE) */
      {
         configfile = argv[argc_pos];
      }

   } /* -END- while (more arguments) */

   show_version(Argv[0]);

#if defined(unix)
   if ( *configfile != '/' )
   {
      char cwd[BUFFER_SIZE];
      char *abs_file;
      size_t abs_file_size;

      /* make config-filename absolute here */
      if (NULL == getcwd(cwd, sizeof(cwd)))
      {
         perror("failed to get current working directory");
         exit( 1 );
      }

      /* XXX: why + 5? */
      abs_file_size = strlen(cwd) + strlen(configfile) + 5;
      basedir = strdup(cwd);

      if (NULL == basedir ||
          NULL == (abs_file = malloc(abs_file_size)))
      {
         perror("malloc failed");
         exit( 1 );
      }
      strlcpy(abs_file, basedir, abs_file_size);
      strlcat(abs_file, "/", abs_file_size );
      strlcat(abs_file, configfile, abs_file_size);
      configfile = abs_file;
   }
#endif /* defined unix */


   files->next = NULL;
   clients->next = NULL;

   /* XXX: factor out initialising after the next stable release. */
#ifdef AMIGA
   InitAmiga();
#elif defined(_WIN32)
   InitWin32();
#endif

   random_seed = (unsigned int)time(NULL);
#ifdef HAVE_RANDOM
   srandom(random_seed);
#else
   srand(random_seed);
#endif /* ifdef HAVE_RANDOM */

   /*
    * Unix signal handling
    *
    * Catch the abort, interrupt and terminate signals for a graceful exit
    * Catch the hangup signal so the errlog can be reopened.
    * Ignore the broken pipe signals (FIXME: Why?)
    */
#if !defined(_WIN32) && !defined(__OS2__) && !defined(AMIGA)
{
   int idx;
   const int catched_signals[] = { SIGTERM, SIGINT, SIGHUP };

   for (idx = 0; idx < SZ(catched_signals); idx++)
   {
#ifdef sun /* FIXME: Is it safe to check for HAVE_SIGSET instead? */
      if (sigset(catched_signals[idx], sig_handler) == SIG_ERR)
#else
      if (signal(catched_signals[idx], sig_handler) == SIG_ERR)
#endif /* ifdef sun */
      {
         log_error(LOG_LEVEL_FATAL, "Can't set signal-handler for signal %d: %E", catched_signals[idx]);
      }
   }

   if (signal(SIGPIPE, SIG_IGN) == SIG_ERR)
   {
      log_error(LOG_LEVEL_FATAL, "Can't set ignore-handler for SIGPIPE: %E");
   }

}
#else /* ifdef _WIN32 */
# ifdef _WIN_CONSOLE
   /*
    * We *are* in a windows console app.
    * Print a verbose messages about FAQ's and such
    */
   printf("%s", win32_blurb);
# endif /* def _WIN_CONSOLE */
#endif /* def _WIN32 */


   /* Initialize the CGI subsystem */
   cgi_init_error_messages();

   /*
    * If runnig on unix and without the --nodaemon
    * option, become a daemon. I.e. fork, detach
    * from tty and get process group leadership
    */
#if defined(unix)
{
   if (daemon_mode)
   {
      int fd;
      pid_t pid = fork();

      if ( pid < 0 ) /* error */
      {
         perror("fork");
         exit( 3 );
      }
      else if ( pid != 0 ) /* parent */
      {
         int status;
         pid_t wpid;
         /*
          * must check for errors
          * child died due to missing files aso
          */
         sleep( 1 );
         wpid = waitpid( pid, &status, WNOHANG );
         if ( wpid != 0 )
         {
            exit( 1 );
         }
         exit( 0 );
      }
      /* child */

      setsid();

      /*
       * stderr (fd 2) will be closed later on,
       * when the config file has been parsed.
       */
      close(0);
      close(1);

      /*
       * Reserve fd 0 and 1 to prevent abort() and friends
       * from sending stuff to the clients or servers.
       */
      fd = open("/dev/null", O_RDONLY);
      if (fd == -1)
      {
         log_error(LOG_LEVEL_FATAL, "Failed to open /dev/null: %E");
      }
      else if (fd != 0)
      {
         if (dup2(fd, 0) == -1)
         {
            log_error(LOG_LEVEL_FATAL, "Failed to reserve fd 0: %E");
         }
         close(fd);
      }
      fd = open("/dev/null", O_WRONLY);
      if (fd == -1)
      {
         log_error(LOG_LEVEL_FATAL, "Failed to open /dev/null: %E");
      }
      else if (fd != 1)
      {
         if (dup2(fd, 1) == -1)
         {
            log_error(LOG_LEVEL_FATAL, "Failed to reserve fd 1: %E");
         }
         close(fd);
      }

      chdir("/");

   } /* -END- if (daemon_mode) */

   /*
    * As soon as we have written the PID file, we can switch
    * to the user and group ID indicated by the --user option
    */
   write_pid_file();

   if (NULL != pw)
   {
      if (setgid((NULL != grp) ? grp->gr_gid : pw->pw_gid))
      {
         log_error(LOG_LEVEL_FATAL, "Cannot setgid(): Insufficient permissions.");
      }
      if (NULL != grp)
      {
         if (setgroups(1, &grp->gr_gid))
         {
            log_error(LOG_LEVEL_FATAL, "setgroups() failed: %E");
         }
      }
      else if (initgroups(pw->pw_name, pw->pw_gid))
      {
         log_error(LOG_LEVEL_FATAL, "initgroups() failed: %E");
      }
      if (do_chroot)
      {
         if (!pw->pw_dir)
         {
            log_error(LOG_LEVEL_FATAL, "Home directory for %s undefined", pw->pw_name);
         }
         /* Read the time zone file from /etc before doing chroot. */
         tzset();
         if (NULL != pre_chroot_nslookup_to_load_resolver
             && '\0' != pre_chroot_nslookup_to_load_resolver[0])
         {
            /* Initialize resolver library. */
            (void) resolve_hostname_to_ip(pre_chroot_nslookup_to_load_resolver);
         }
         if (chroot(pw->pw_dir) < 0)
         {
            log_error(LOG_LEVEL_FATAL, "Cannot chroot to %s", pw->pw_dir);
         }
         if (chdir ("/"))
         {
            log_error(LOG_LEVEL_FATAL, "Cannot chdir /");
         }
      }
      if (setuid(pw->pw_uid))
      {
         log_error(LOG_LEVEL_FATAL, "Cannot setuid(): Insufficient permissions.");
      }
      if (do_chroot)
      {
         char putenv_dummy[64];

         strlcpy(putenv_dummy, "HOME=/", sizeof(putenv_dummy));
         if (putenv(putenv_dummy) != 0)
         {
            log_error(LOG_LEVEL_FATAL, "Cannot putenv(): HOME");
         }

         snprintf(putenv_dummy, sizeof(putenv_dummy), "USER=%s", pw->pw_name);
         if (putenv(putenv_dummy) != 0)
         {
            log_error(LOG_LEVEL_FATAL, "Cannot putenv(): USER");
         }
      }
   }
   else if (do_chroot)
   {
      log_error(LOG_LEVEL_FATAL, "Cannot chroot without --user argument.");
   }
}
#endif /* defined unix */

#ifdef _WIN32
   /* This will be FALSE unless the command line specified --service
    */
   if (bRunAsService)
   {
      /* Yup, so now we must attempt to establish a connection
       * with the service dispatcher. This will only work if this
       * process was launched by the service control manager to
       * actually run as a service. If this isn't the case, i've
       * known it take around 30 seconds or so for the call to return.
       */

      /* The StartServiceCtrlDispatcher won't return until the service is stopping */
      if (w32_start_service_ctrl_dispatcher(w32ServiceDispatchTable))
      {
         /* Service has run, and at this point is now being stopped, so just return */
         return 0;
      }

#ifdef _WIN_CONSOLE
      printf("Warning: Failed to connect to Service Control Dispatcher\nwhen starting as a service!\n");
#endif
      /* An error occurred. Usually it's because --service was wrongly specified
       * and we were unable to connect to the Service Control Dispatcher because
       * it wasn't expecting us and is therefore not listening.
       *
       * For now, just continue below to call the listen_loop function.
       */
   }
#endif /* def _WIN32 */

   listen_loop();

   /* NOTREACHED */
   return(-1);

}


/*********************************************************************
 *
 * Function    :  bind_port_helper
 *
 * Description :  Bind the listen port.  Handles logging, and aborts
 *                on failure.
 *
 * Parameters  :
 *          1  :  haddr = Host addres to bind to. Use NULL to bind to
 *                        INADDR_ANY.
 *          2  :  hport = Specifies port to bind to.
 *
 * Returns     :  Port that was opened.
 *
 *********************************************************************/
static jb_socket bind_port_helper(const char *haddr, int hport)
{
   int result;
   jb_socket bfd;

   result = bind_port(haddr, hport, &bfd);

   if (result < 0)
   {
      const char *bind_address = (NULL != haddr) ? haddr : "INADDR_ANY";
      switch(result)
      {
         case -3:
            log_error(LOG_LEVEL_FATAL,
               "can't bind to %s:%d: There may be another Privoxy "
               "or some other proxy running on port %d",
               bind_address, hport, hport);

         case -2:
            log_error(LOG_LEVEL_FATAL,
               "can't bind to %s:%d: The hostname is not resolvable",
               bind_address, hport);

         default:
            log_error(LOG_LEVEL_FATAL, "can't bind to %s:%d: %E",
               bind_address, hport);
      }

      /* shouldn't get here */
      return JB_INVALID_SOCKET;
   }

   if (haddr == NULL)
   {
      log_error(LOG_LEVEL_INFO, "Listening on port %d on all IP addresses",
         hport);
   }
   else
   {
      log_error(LOG_LEVEL_INFO, "Listening on port %d on IP address %s",
         hport, haddr);
   }

   return bfd;
}


/*********************************************************************
 *
 * Function    :  bind_ports_helper
 *
 * Description :  Bind the listen ports.  Handles logging, and aborts
 *                on failure.
 *
 * Parameters  :
 *          1  :  config = Privoxy configuration.  Specifies ports
 *                         to bind to.
 *          2  :  sockets = Preallocated array of opened sockets
 *                          corresponding to specification in config.
 *                          All non-opened sockets will be set to
 *                          JB_INVALID_SOCKET.
 *
 * Returns     :  Nothing. Inspect sockets argument.
 *
 *********************************************************************/
static void bind_ports_helper(struct configuration_spec * config,
                              jb_socket sockets[])
{
   int i;

   for (i = 0; i < MAX_LISTENING_SOCKETS; i++)
   {
      if (config->hport[i])
      {
         sockets[i] = bind_port_helper(config->haddr[i], config->hport[i]);
      }
      else
      {
         sockets[i] = JB_INVALID_SOCKET;
      }
   }
   config->need_bind = 0;
}


/*********************************************************************
 *
 * Function    :  close_ports_helper
 *
 * Description :  Close listenings ports.
 *
 * Parameters  :
 *          1  :  sockets = Array of opened and non-opened sockets to
 *                          close. All sockets will be set to
 *                          JB_INVALID_SOCKET.
 *
 * Returns     :  Nothing.
 *
 *********************************************************************/
static void close_ports_helper(jb_socket sockets[])
{
   int i;

   for (i = 0; i < MAX_LISTENING_SOCKETS; i++)
   {
      if (JB_INVALID_SOCKET != sockets[i])
      {
         close_socket(sockets[i]);
      }
      sockets[i] = JB_INVALID_SOCKET;
   }
}


#ifdef _WIN32
/* Without this simple workaround we get this compiler warning from _beginthread
 *     warning C4028: formal parameter 1 different from declaration
 */
void w32_service_listen_loop(void *p)
{
   listen_loop();
}
#endif /* def _WIN32 */


/*********************************************************************
 *
 * Function    :  listen_loop
 *
 * Description :  bind the listen port and enter a "FOREVER" listening loop.
 *
 * Parameters  :  N/A
 *
 * Returns     :  Never.
 *
 *********************************************************************/
static void listen_loop(void)
{
   struct client_states *csp_list = NULL;
   struct client_state *csp = NULL;
   jb_socket bfds[MAX_LISTENING_SOCKETS];
   struct configuration_spec *config;
   unsigned int active_threads = 0;

   config = load_config();

#ifdef FEATURE_CONNECTION_SHARING
   /*
    * XXX: Should be relocated once it no
    * longer needs to emit log messages.
    */
   initialize_reusable_connections();
#endif /* def FEATURE_CONNECTION_SHARING */

   bind_ports_helper(config, bfds);

#ifdef FEATURE_GRACEFUL_TERMINATION
   while (!g_terminate)
#else
   for (;;)
#endif
   {
#if !defined(FEATURE_PTHREAD) && !defined(_WIN32) && !defined(__BEOS__) && !defined(AMIGA) && !defined(__OS2__)
      while (waitpid(-1, NULL, WNOHANG) > 0)
      {
         /* zombie children */
      }
#endif /* !defined(FEATURE_PTHREAD) && !defined(_WIN32) && !defined(__BEOS__) && !defined(AMIGA) */

      /*
       * Free data that was used by died threads
       */
      active_threads = sweep();

#if defined(unix)
      /*
       * Re-open the errlog after HUP signal
       */
      if (received_hup_signal)
      {
         if (NULL != config->logfile)
         {
            init_error_log(Argv[0], config->logfile);
         }
         received_hup_signal = 0;
      }
#endif

      csp_list = (struct client_states *)zalloc(sizeof(*csp_list));
      if (NULL == csp_list)
      {
         log_error(LOG_LEVEL_FATAL,
            "malloc(%d) for csp_list failed: %E", sizeof(*csp_list));
         continue;
      }
      csp = &csp_list->csp;

      log_error(LOG_LEVEL_CONNECT, "Listening for new connections ... ");

      if (!accept_connection(csp, bfds))
      {
         log_error(LOG_LEVEL_CONNECT, "accept failed: %E");

#ifdef AMIGA
         if(!childs)
         {
            exit(1);
         }
#endif
         freez(csp_list);
         continue;
      }
      else
      {
         log_error(LOG_LEVEL_CONNECT,
            "accepted connection from %s on socket %d",
            csp->ip_addr_str, csp->cfd);
      }

      csp->flags |= CSP_FLAG_ACTIVE;
      csp->server_connection.sfd = JB_INVALID_SOCKET;

      csp->config = config = load_config();

      if (config->need_bind)
      {
         /*
          * Since we were listening to the "old port", we will not see
          * a "listen" param change until the next request.  So, at
          * least 1 more request must be made for us to find the new
          * setting.  I am simply closing the old socket and binding the
          * new one.
          *
          * Which-ever is correct, we will serve 1 more page via the
          * old settings.  This should probably be a "show-proxy-args"
          * request.  This should not be a so common of an operation
          * that this will hurt people's feelings.
          */

         close_ports_helper(bfds);

         bind_ports_helper(config, bfds);
      }

#ifdef FEATURE_TOGGLE
      if (global_toggle_state)
#endif /* def FEATURE_TOGGLE */
      {
         csp->flags |= CSP_FLAG_TOGGLED_ON;
      }

      if (run_loader(csp))
      {
         log_error(LOG_LEVEL_FATAL, "a loader failed - must exit");
         /* Never get here - LOG_LEVEL_FATAL causes program exit */
      }

#ifdef FEATURE_ACL
      if (block_acl(NULL,csp))
      {
         log_error(LOG_LEVEL_CONNECT,
            "Connection from %s on socket %d dropped due to ACL", csp->ip_addr_str, csp->cfd);
         close_socket(csp->cfd);
         freez(csp->ip_addr_str);
         freez(csp_list);
         continue;
      }
#endif /* def FEATURE_ACL */

      if ((0 != config->max_client_connections)
         && (active_threads >= config->max_client_connections))
      {
         log_error(LOG_LEVEL_CONNECT,
            "Rejecting connection from %s. Maximum number of connections reached.",
            csp->ip_addr_str);
         write_socket(csp->cfd, TOO_MANY_CONNECTIONS_RESPONSE,
            strlen(TOO_MANY_CONNECTIONS_RESPONSE));
         close_socket(csp->cfd);
         freez(csp->ip_addr_str);
         freez(csp_list);
         continue;
      }

      /* add it to the list of clients */
      csp_list->next = clients->next;
      clients->next = csp_list;

      if (config->multi_threaded)
      {
         int child_id;

/* this is a switch () statement in the C preprocessor - ugh */
#undef SELECTED_ONE_OPTION

/* Use Pthreads in preference to native code */
#if defined(FEATURE_PTHREAD) && !defined(SELECTED_ONE_OPTION)
#define SELECTED_ONE_OPTION
         {
            pthread_t the_thread;
            pthread_attr_t attrs;

            pthread_attr_init(&attrs);
            pthread_attr_setdetachstate(&attrs, PTHREAD_CREATE_DETACHED);
            errno = pthread_create(&the_thread, &attrs,
               (void * (*)(void *))serve, csp);
            child_id = errno ? -1 : 0;
            pthread_attr_destroy(&attrs);
         }
#endif

#if defined(_WIN32) && !defined(_CYGWIN) && !defined(SELECTED_ONE_OPTION)
#define SELECTED_ONE_OPTION
         child_id = _beginthread(
            (void (*)(void *))serve,
            64 * 1024,
            csp);
#endif

#if defined(__OS2__) && !defined(SELECTED_ONE_OPTION)
#define SELECTED_ONE_OPTION
         child_id = _beginthread(
            (void(* _Optlink)(void*))serve,
            NULL,
            64 * 1024,
            csp);
#endif

#if defined(__BEOS__) && !defined(SELECTED_ONE_OPTION)
#define SELECTED_ONE_OPTION
         {
            thread_id tid = spawn_thread
               (server_thread, "server", B_NORMAL_PRIORITY, csp);

            if ((tid >= 0) && (resume_thread(tid) == B_OK))
            {
               child_id = (int) tid;
            }
            else
            {
               child_id = -1;
            }
         }
#endif

#if defined(AMIGA) && !defined(SELECTED_ONE_OPTION)
#define SELECTED_ONE_OPTION
         csp->cfd = ReleaseSocket(csp->cfd, -1);

#ifdef __amigaos4__
         child_id = (int)CreateNewProcTags(NP_Entry, (ULONG)server_thread,
                                           NP_Output, Output(),
                                           NP_CloseOutput, FALSE,
                                           NP_Name, (ULONG)"privoxy child",
                                           NP_Child, TRUE,
                                           TAG_DONE);
#else
         child_id = (int)CreateNewProcTags(NP_Entry, (ULONG)server_thread,
                                           NP_Output, Output(),
                                           NP_CloseOutput, FALSE,
                                           NP_Name, (ULONG)"privoxy child",
                                           NP_StackSize, 200*1024,
                                           TAG_DONE);
#endif
         if(0 != child_id)
         {
            childs++;
            ((struct Task *)child_id)->tc_UserData = csp;
            Signal((struct Task *)child_id, SIGF_SINGLE);
            Wait(SIGF_SINGLE);
         }
#endif

#if !defined(SELECTED_ONE_OPTION)
         child_id = fork();

         /* This block is only needed when using fork().
          * When using threads, the server thread was
          * created and run by the call to _beginthread().
          */
         if (child_id == 0)   /* child */
         {
            int rc = 0;
#ifdef FEATURE_TOGGLE
            int inherited_toggle_state = global_toggle_state;
#endif /* def FEATURE_TOGGLE */

            serve(csp);

            /*
             * If we've been toggled or we've blocked the request, tell Mom
             */

#ifdef FEATURE_TOGGLE
            if (inherited_toggle_state != global_toggle_state)
            {
               rc |= RC_FLAG_TOGGLED;
            }
#endif /* def FEATURE_TOGGLE */

#ifdef FEATURE_STATISTICS
            if (csp->flags & CSP_FLAG_REJECTED)
            {
               rc |= RC_FLAG_BLOCKED;
            }
#endif /* ndef FEATURE_STATISTICS */

            _exit(rc);
         }
         else if (child_id > 0) /* parent */
         {
            /* in a fork()'d environment, the parent's
             * copy of the client socket and the CSP
             * are not used.
             */
            int child_status;
#if !defined(_WIN32) && !defined(__CYGWIN__)

            wait( &child_status );

            /*
             * Evaluate child's return code: If the child has
             *  - been toggled, toggle ourselves
             *  - blocked its request, bump up the stats counter
             */

#ifdef FEATURE_TOGGLE
            if (WIFEXITED(child_status) && (WEXITSTATUS(child_status) & RC_FLAG_TOGGLED))
            {
               global_toggle_state = !global_toggle_state;
            }
#endif /* def FEATURE_TOGGLE */

#ifdef FEATURE_STATISTICS
            urls_read++;
            if (WIFEXITED(child_status) && (WEXITSTATUS(child_status) & RC_FLAG_BLOCKED))
            {
               urls_rejected++;
            }
#endif /* def FEATURE_STATISTICS */

#endif /* !defined(_WIN32) && defined(__CYGWIN__) */
            close_socket(csp->cfd);
            csp->flags &= ~CSP_FLAG_ACTIVE;
         }
#endif

#undef SELECTED_ONE_OPTION
/* end of cpp switch () */

         if (child_id < 0)
         {
            /*
             * Spawning the child failed, assume it's because
             * there are too many children running already.
             * XXX: If you assume ...
             */
            log_error(LOG_LEVEL_ERROR,
               "Unable to take any additional connections: %E");
            write_socket(csp->cfd, TOO_MANY_CONNECTIONS_RESPONSE,
               strlen(TOO_MANY_CONNECTIONS_RESPONSE));
            close_socket(csp->cfd);
            csp->flags &= ~CSP_FLAG_ACTIVE;
         }
      }
      else
      {
         serve(csp);
      }
   }

   /* NOTREACHED unless FEATURE_GRACEFUL_TERMINATION is defined */

   /* Clean up.  Aim: free all memory (no leaks) */
#ifdef FEATURE_GRACEFUL_TERMINATION

   log_error(LOG_LEVEL_ERROR, "Graceful termination requested");

   unload_current_config_file();
   unload_current_actions_file();
   unload_current_re_filterfile();
#ifdef FEATURE_TRUST
   unload_current_trust_file();
#endif

   if (config->multi_threaded)
   {
      int i = 60;
      do
      {
         sleep(1);
         sweep();
      } while ((clients->next != NULL) && (--i > 0));

      if (i <= 0)
      {
         log_error(LOG_LEVEL_ERROR, "Graceful termination failed - still some live clients after 1 minute wait.");
      }
   }
   sweep();
   sweep();

#if defined(unix)
   freez(basedir);
#endif

#if defined(_WIN32) && !defined(_WIN_CONSOLE)
   /* Cleanup - remove taskbar icon etc. */
   TermLogWindow();
#endif

   exit(0);
#endif /* FEATURE_GRACEFUL_TERMINATION */

}


/*
  Local Variables:
  tab-width: 3
  end:
*/
