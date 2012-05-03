const char urlmatch_rcs[] = "$Id: urlmatch.c,v 1.47 2009/03/02 19:18:10 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/urlmatch.c,v $
 *
 * Purpose     :  Declares functions to match URLs against URL
 *                patterns.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2009
 *                the Privoxy team. http://www.privoxy.org/
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
 *    $Log: urlmatch.c,v $
 *    Revision 1.47  2009/03/02 19:18:10  fabiankeil
 *    Streamline parse_http_request()'s prototype. As
 *    cparser pointed out it doesn't actually use csp.
 *
 *    Revision 1.46  2009/02/11 19:31:32  fabiankeil
 *    Reject request lines that end with neither HTTP/1.0 nor HTTP/1.1.
 *
 *    Revision 1.45  2008/06/21 21:19:18  fabiankeil
 *    Silence bogus compiler warning.
 *
 *    Revision 1.44  2008/05/04 16:18:32  fabiankeil
 *    Provide parse_http_url() with a third parameter to specify
 *    whether or not URLs without protocol are acceptable.
 *
 *    Revision 1.43  2008/05/04 13:30:55  fabiankeil
 *    Streamline parse_http_url()'s prototype.
 *
 *    Revision 1.42  2008/05/04 13:24:16  fabiankeil
 *    If the method isn't CONNECT, reject URLs without protocol.
 *
 *    Revision 1.41  2008/05/02 09:51:34  fabiankeil
 *    In parse_http_url(), don't muck around with values
 *    that are none of its business: require an initialized
 *    http structure and never unset http->ssl.
 *
 *    Revision 1.40  2008/04/23 16:12:28  fabiankeil
 *    Free with freez().
 *
 *    Revision 1.39  2008/04/22 16:27:42  fabiankeil
 *    In parse_http_request(), remove a pointless
 *    temporary variable and free the buffer earlier.
 *
 *    Revision 1.38  2008/04/18 05:17:18  fabiankeil
 *    Mark simplematch()'s parameters as immutable.
 *
 *    Revision 1.37  2008/04/17 14:53:29  fabiankeil
 *    Move simplematch() into urlmatch.c as it's only
 *    used to match (old-school) domain patterns.
 *
 *    Revision 1.36  2008/04/14 18:19:48  fabiankeil
 *    Remove now-pointless cast in create_url_spec().
 *
 *    Revision 1.35  2008/04/14 18:11:21  fabiankeil
 *    The compiler might not notice it, but the buffer passed to
 *    create_url_spec() is modified later on and thus shouldn't
 *    be declared immutable.
 *
 *    Revision 1.34  2008/04/13 13:32:07  fabiankeil
 *    Factor URL pattern compilation out of create_url_spec().
 *
 *    Revision 1.33  2008/04/12 14:03:13  fabiankeil
 *    Remove an obvious comment and improve another one.
 *
 *    Revision 1.32  2008/04/12 12:38:06  fabiankeil
 *    Factor out duplicated code to compile host, path and tag patterns.
 *
 *    Revision 1.31  2008/04/10 14:41:04  fabiankeil
 *    Ditch url_spec's path member now that it's no longer used.
 *
 *    Revision 1.30  2008/04/10 04:24:24  fabiankeil
 *    Stop duplicating the plain text representation of the path regex
 *    (and keeping the copy around). Once the regex is compiled it's no
 *    longer useful.
 *
 *    Revision 1.29  2008/04/10 04:17:56  fabiankeil
 *    In url_match(), check the right member for NULL when determining
 *    whether there's a path regex to execute. Looking for a plain-text
 *    representation works as well, but it looks "interesting" and that
 *    member will be removed soonish anyway.
 *
 *    Revision 1.28  2008/04/08 16:07:39  fabiankeil
 *    Make it harder to mistake url_match()'s
 *    second parameter for an url_spec.
 *
 *    Revision 1.27  2008/04/08 15:44:33  fabiankeil
 *    Save a bit of memory (and a few cpu cycles) by not bothering to
 *    compile slash-only path regexes that don't affect the result.
 *
 *    Revision 1.26  2008/04/07 16:57:18  fabiankeil
 *    - Use free_url_spec() more consistently.
 *    - Let it reset url->dcount just in case.
 *
 *    Revision 1.25  2008/04/06 15:18:38  fabiankeil
 *    Oh well, rename the --enable-pcre-host-patterns option to
 *    --enable-extended-host-patterns as it's not really PCRE syntax.
 *
 *    Revision 1.24  2008/04/06 14:54:26  fabiankeil
 *    Use PCRE syntax in host patterns when configured
 *    with --enable-pcre-host-patterns.
 *
 *    Revision 1.23  2008/04/05 12:19:20  fabiankeil
 *    Factor compile_host_pattern() out of create_url_spec().
 *
 *    Revision 1.22  2008/03/30 15:02:32  fabiankeil
 *    SZitify unknown_method().
 *
 *    Revision 1.21  2007/12/24 16:34:23  fabiankeil
 *    Band-aid (and micro-optimization) that makes it less likely to run out of
 *    stack space with overly-complex path patterns. Probably masks the problem
 *    reported by Lee in #1856679. Hohoho.
 *
 *    Revision 1.20  2007/09/02 15:31:20  fabiankeil
 *    Move match_portlist() from filter.c to urlmatch.c.
 *    It's used for url matching, not for filtering.
 *
 *    Revision 1.19  2007/09/02 13:42:11  fabiankeil
 *    - Allow port lists in url patterns.
 *    - Ditch unused url_spec member pathlen.
 *
 *    Revision 1.18  2007/07/30 16:42:21  fabiankeil
 *    Move the method check into unknown_method()
 *    and loop through the known methods instead
 *    of using a screen-long OR chain.
 *
 *    Revision 1.17  2007/04/15 16:39:21  fabiankeil
 *    Introduce tags as alternative way to specify which
 *    actions apply to a request. At the moment tags can be
 *    created based on client and server headers.
 *
 *    Revision 1.16  2007/02/13 13:59:24  fabiankeil
 *    Remove redundant log message.
 *
 *    Revision 1.15  2007/01/28 16:11:23  fabiankeil
 *    Accept WebDAV methods for subversion
 *    in parse_http_request(). Closes FR 1581425.
 *
 *    Revision 1.14  2007/01/06 14:23:56  fabiankeil
 *    Fix gcc43 warnings. Mark *csp as immutable
 *    for parse_http_url() and url_match().
 *    Replace a sprintf call with snprintf.
 *
 *    Revision 1.13  2006/12/06 19:50:54  fabiankeil
 *    parse_http_url() now handles intercepted
 *    HTTP request lines as well. Moved parts
 *    of parse_http_url()'s code into
 *    init_domain_components() so that it can
 *    be reused in chat().
 *
 *    Revision 1.12  2006/07/18 14:48:47  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.10.2.7  2003/05/17 15:57:24  oes
 *     - parse_http_url now checks memory allocation failure for
 *       duplication of "*" URL and rejects "*something" URLs
 *       Closes bug #736344
 *     - Added a comment to what might look like a bug in
 *       create_url_spec (see !bug #736931)
 *     - Comment cosmetics
 *
 *    Revision 1.10.2.6  2003/05/07 12:39:48  oes
 *    Fix typo: Default port for https URLs is 443, not 143.
 *    Thanks to Scott Tregear for spotting this one.
 *
 *    Revision 1.10.2.5  2003/02/28 13:09:29  oes
 *    Fixed a rare double free condition as per Bug #694713
 *
 *    Revision 1.10.2.4  2003/02/28 12:57:44  oes
 *    Moved freeing of http request structure to its owner
 *    as per Dan Price's observations in Bug #694713
 *
 *    Revision 1.10.2.3  2002/11/12 16:50:40  oes
 *    Fixed memory leak in parse_http_request() reported by Oliver Stoeneberg. Fixes bug #637073
 *
 *    Revision 1.10.2.2  2002/09/25 14:53:15  oes
 *    Added basic support for OPTIONS and TRACE HTTP methods:
 *    parse_http_url now recognizes the "*" URI as well as
 *    the OPTIONS and TRACE method keywords.
 *
 *    Revision 1.10.2.1  2002/06/06 19:06:44  jongfoster
 *    Adding support for proprietary Microsoft WebDAV extensions
 *
 *    Revision 1.10  2002/05/12 21:40:37  jongfoster
 *    - Removing some unused code
 *
 *    Revision 1.9  2002/04/04 00:36:36  gliptak
 *    always use pcre for matching
 *
 *    Revision 1.8  2002/04/03 23:32:47  jongfoster
 *    Fixing memory leak on error
 *
 *    Revision 1.7  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.6  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.5  2002/03/13 00:27:05  jongfoster
 *    Killing warnings
 *
 *    Revision 1.4  2002/03/07 03:46:17  oes
 *    Fixed compiler warnings
 *
 *    Revision 1.3  2002/03/03 14:51:11  oes
 *    Fixed CLF logging: Added ocmd member for client's request to struct http_request
 *
 *    Revision 1.2  2002/01/21 00:14:09  jongfoster
 *    Correcting comment style
 *    Fixing an uninitialized memory bug in create_url_spec()
 *
 *    Revision 1.1  2002/01/17 20:53:46  jongfoster
 *    Moving all our URL and URL pattern parsing code to the same file - it
 *    was scattered around in filters.c, loaders.c and parsers.c.
 *
 *    Providing a single, simple url_match(pattern,url) function - rather than
 *    the 3-line match routine which was repeated all over the place.
 *
 *    Renaming free_url to free_url_spec, since it frees a struct url_spec.
 *
 *    Providing parse_http_url() so that URLs can be parsed without faking a
 *    HTTP request line for parse_http_request() or repeating the parsing
 *    code (both of which were techniques that were actually in use).
 *
 *    Standardizing that struct http_request is used to represent a URL, and
 *    struct url_spec is used to represent a URL pattern.  (Before, URLs were
 *    represented as seperate variables and a partially-filled-in url_spec).
 *
 *
 *********************************************************************/


#include "config.h"

#ifndef _WIN32
#include <stdio.h>
#include <sys/types.h>
#endif

#include <stdlib.h>
#include <ctype.h>
#include <assert.h>
#include <string.h>

#if !defined(_WIN32) && !defined(__OS2__)
#include <unistd.h>
#endif

#include "project.h"
#include "urlmatch.h"
#include "ssplit.h"
#include "miscutil.h"
#include "errlog.h"

const char urlmatch_h_rcs[] = URLMATCH_H_VERSION;

enum regex_anchoring {NO_ANCHORING, LEFT_ANCHORED, RIGHT_ANCHORED};
static jb_err compile_host_pattern(struct url_spec *url, const char *host_pattern);

/*********************************************************************
 *
 * Function    :  free_http_request
 *
 * Description :  Freez a http_request structure
 *
 * Parameters  :
 *          1  :  http = points to a http_request structure to free
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void free_http_request(struct http_request *http)
{
   assert(http);

   freez(http->cmd);
   freez(http->ocmd);
   freez(http->gpc);
   freez(http->host);
   freez(http->url);
   freez(http->hostport);
   freez(http->path);
   freez(http->ver);
   freez(http->host_ip_addr_str);
   freez(http->dbuffer);
   freez(http->dvec);
   http->dcount = 0;
}


/*********************************************************************
 *
 * Function    :  init_domain_components
 *
 * Description :  Splits the domain name so we can compare it
 *                against wildcards. It used to be part of
 *                parse_http_url, but was separated because the
 *                same code is required in chat in case of
 *                intercepted requests.
 *
 * Parameters  :
 *          1  :  http = pointer to the http structure to hold elements.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out of memory
 *                JB_ERR_PARSE on malformed command/URL
 *                             or >100 domains deep.
 *
 *********************************************************************/
jb_err init_domain_components(struct http_request *http)
{
   char *vec[BUFFER_SIZE];
   size_t size;
   char *p;

   http->dbuffer = strdup(http->host);
   if (NULL == http->dbuffer)
   {
      return JB_ERR_MEMORY;
   }

   /* map to lower case */
   for (p = http->dbuffer; *p ; p++)
   {
      *p = (char)tolower((int)(unsigned char)*p);
   }

   /* split the domain name into components */
   http->dcount = ssplit(http->dbuffer, ".", vec, SZ(vec), 1, 1);

   if (http->dcount <= 0)
   {
      /*
       * Error: More than SZ(vec) components in domain
       *    or: no components in domain
       */
      log_error(LOG_LEVEL_ERROR, "More than SZ(vec) components in domain or none at all.");
      return JB_ERR_PARSE;
   }

   /* save a copy of the pointers in dvec */
   size = (size_t)http->dcount * sizeof(*http->dvec);

   http->dvec = (char **)malloc(size);
   if (NULL == http->dvec)
   {
      return JB_ERR_MEMORY;
   }

   memcpy(http->dvec, vec, size);

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  parse_http_url
 *
 * Description :  Parse out the host and port from the URL.  Find the
 *                hostname & path, port (if ':'), and/or password (if '@')
 *
 * Parameters  :
 *          1  :  url = URL (or is it URI?) to break down
 *          2  :  http = pointer to the http structure to hold elements.
 *                       Must be initialized with valid values (like NULLs).
 *          3  :  require_protocol = Whether or not URLs without
 *                                   protocol are acceptable.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out of memory
 *                JB_ERR_PARSE on malformed command/URL
 *                             or >100 domains deep.
 *
 *********************************************************************/
jb_err parse_http_url(const char *url, struct http_request *http, int require_protocol)
{
   int host_available = 1; /* A proxy can dream. */

   /*
    * Save our initial URL
    */
   http->url = strdup(url);
   if (http->url == NULL)
   {
      return JB_ERR_MEMORY;
   }


   /*
    * Check for * URI. If found, we're done.
    */  
   if (*http->url == '*')
   {
      if  ( NULL == (http->path = strdup("*"))
         || NULL == (http->hostport = strdup("")) ) 
      {
         return JB_ERR_MEMORY;
      }
      if (http->url[1] != '\0')
      {
         return JB_ERR_PARSE;
      }
      return JB_ERR_OK;
   }


   /*
    * Split URL into protocol,hostport,path.
    */
   {
      char *buf;
      char *url_noproto;
      char *url_path;

      buf = strdup(url);
      if (buf == NULL)
      {
         return JB_ERR_MEMORY;
      }

      /* Find the start of the URL in our scratch space */
      url_noproto = buf;
      if (strncmpic(url_noproto, "http://",  7) == 0)
      {
         url_noproto += 7;
      }
      else if (strncmpic(url_noproto, "https://", 8) == 0)
      {
         /*
          * Should only happen when called from cgi_show_url_info().
          */
         url_noproto += 8;
         http->ssl = 1;
      }
      else if (*url_noproto == '/')
      {
        /*
         * Short request line without protocol and host.
         * Most likely because the client's request
         * was intercepted and redirected into Privoxy.
         */
         http->host = NULL;
         host_available = 0;
      }
      else if (require_protocol)
      {
         freez(buf);
         return JB_ERR_PARSE;
      }

      url_path = strchr(url_noproto, '/');
      if (url_path != NULL)
      {
         /*
          * Got a path.
          *
          * NOTE: The following line ignores the path for HTTPS URLS.
          * This means that you get consistent behaviour if you type a
          * https URL in and it's parsed by the function.  (When the
          * URL is actually retrieved, SSL hides the path part).
          */
         http->path = strdup(http->ssl ? "/" : url_path);
         *url_path = '\0';
         http->hostport = strdup(url_noproto);
      }
      else
      {
         /*
          * Repair broken HTTP requests that don't contain a path,
          * or CONNECT requests
          */
         http->path = strdup("/");
         http->hostport = strdup(url_noproto);
      }

      freez(buf);

      if ( (http->path == NULL)
        || (http->hostport == NULL))
      {
         return JB_ERR_MEMORY;
      }
   }

   if (!host_available)
   {
      /* Without host, there is nothing left to do here */
      return JB_ERR_OK;
   }

   /*
    * Split hostport into user/password (ignored), host, port.
    */
   {
      char *buf;
      char *host;
      char *port;

      buf = strdup(http->hostport);
      if (buf == NULL)
      {
         return JB_ERR_MEMORY;
      }

      /* check if url contains username and/or password */
      host = strchr(buf, '@');
      if (host != NULL)
      {
         /* Contains username/password, skip it and the @ sign. */
         host++;
      }
      else
      {
         /* No username or password. */
         host = buf;
      }

      /* check if url contains port */
      port = strchr(host, ':');
      if (port != NULL)
      {
         /* Contains port */
         /* Terminate hostname and point to start of port string */
         *port++ = '\0';
         http->port = atoi(port);
      }
      else
      {
         /* No port specified. */
         http->port = (http->ssl ? 443 : 80);
      }

      http->host = strdup(host);

      freez(buf);

      if (http->host == NULL)
      {
         return JB_ERR_MEMORY;
      }
   }

   /*
    * Split domain name so we can compare it against wildcards
    */
   return init_domain_components(http);

}


/*********************************************************************
 *
 * Function    :  unknown_method
 *
 * Description :  Checks whether a method is unknown.
 *
 * Parameters  :
 *          1  :  method = points to a http method
 *
 * Returns     :  TRUE if it's unknown, FALSE otherwise.
 *
 *********************************************************************/
static int unknown_method(const char *method)
{
   static const char *known_http_methods[] = {
      /* Basic HTTP request type */
      "GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "TRACE", "CONNECT",
      /* webDAV extensions (RFC2518) */
      "PROPFIND", "PROPPATCH", "MOVE", "COPY", "MKCOL", "LOCK", "UNLOCK",
      /*
       * Microsoft webDAV extension for Exchange 2000.  See:
       * http://lists.w3.org/Archives/Public/w3c-dist-auth/2002JanMar/0001.html
       * http://msdn.microsoft.com/library/en-us/wss/wss/_webdav_methods.asp
       */ 
      "BCOPY", "BMOVE", "BDELETE", "BPROPFIND", "BPROPPATCH",
      /*
       * Another Microsoft webDAV extension for Exchange 2000.  See:
       * http://systems.cs.colorado.edu/grunwald/MobileComputing/Papers/draft-cohen-gena-p-base-00.txt
       * http://lists.w3.org/Archives/Public/w3c-dist-auth/2002JanMar/0001.html
       * http://msdn.microsoft.com/library/en-us/wss/wss/_webdav_methods.asp
       */ 
      "SUBSCRIBE", "UNSUBSCRIBE", "NOTIFY", "POLL",
      /*
       * Yet another WebDAV extension, this time for
       * Web Distributed Authoring and Versioning (RFC3253)
       */
      "VERSION-CONTROL", "REPORT", "CHECKOUT", "CHECKIN", "UNCHECKOUT",
      "MKWORKSPACE", "UPDATE", "LABEL", "MERGE", "BASELINE-CONTROL", "MKACTIVITY",
   };
   int i;

   for (i = 0; i < SZ(known_http_methods); i++)
   {
      if (0 == strcmpic(method, known_http_methods[i]))
      {
         return FALSE;
      }
   }

   return TRUE;

}


/*********************************************************************
 *
 * Function    :  parse_http_request
 *
 * Description :  Parse out the host and port from the URL.  Find the
 *                hostname & path, port (if ':'), and/or password (if '@')
 *
 * Parameters  :
 *          1  :  req = HTTP request line to break down
 *          2  :  http = pointer to the http structure to hold elements
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out of memory
 *                JB_ERR_CGI_PARAMS on malformed command/URL
 *                                  or >100 domains deep.
 *
 *********************************************************************/
jb_err parse_http_request(const char *req, struct http_request *http)
{
   char *buf;
   char *v[10]; /* XXX: Why 10? We should only need three. */
   int n;
   jb_err err;

   memset(http, '\0', sizeof(*http));

   buf = strdup(req);
   if (buf == NULL)
   {
      return JB_ERR_MEMORY;
   }

   n = ssplit(buf, " \r\n", v, SZ(v), 1, 1);
   if (n != 3)
   {
      freez(buf);
      return JB_ERR_PARSE;
   }

   /*
    * Fail in case of unknown methods
    * which we might not handle correctly.
    *
    * XXX: There should be a config option
    * to forward requests with unknown methods
    * anyway. Most of them don't need special
    * steps.
    */
   if (unknown_method(v[0]))
   {
      log_error(LOG_LEVEL_ERROR, "Unknown HTTP method detected: %s", v[0]);
      freez(buf);
      return JB_ERR_PARSE;
   }

   if (strcmpic(v[2], "HTTP/1.1") && strcmpic(v[2], "HTTP/1.0"))
   {
      log_error(LOG_LEVEL_ERROR, "The only supported HTTP "
         "versions are 1.0 and 1.1. This rules out: %s", v[2]);
      freez(buf);
      return JB_ERR_PARSE;
   }

   http->ssl = !strcmpic(v[0], "CONNECT");

   err = parse_http_url(v[1], http, !http->ssl);
   if (err)
   {
      freez(buf);
      return err;
   }

   /*
    * Copy the details into the structure
    */
   http->cmd = strdup(req);
   http->gpc = strdup(v[0]);
   http->ver = strdup(v[2]);

   freez(buf);

   if ( (http->cmd == NULL)
     || (http->gpc == NULL)
     || (http->ver == NULL) )
   {
      return JB_ERR_MEMORY;
   }

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  compile_pattern
 *
 * Description :  Compiles a host, domain or TAG pattern.
 *
 * Parameters  :
 *          1  :  pattern = The pattern to compile.
 *          2  :  anchoring = How the regex should be anchored.
 *                            Can be either one of NO_ANCHORING,
 *                            LEFT_ANCHORED or RIGHT_ANCHORED.
 *          3  :  url     = In case of failures, the spec member is
 *                          logged and the structure freed.
 *          4  :  regex   = Where the compiled regex should be stored.
 *
 * Returns     :  JB_ERR_OK - Success
 *                JB_ERR_MEMORY - Out of memory
 *                JB_ERR_PARSE - Cannot parse regex
 *
 *********************************************************************/
static jb_err compile_pattern(const char *pattern, enum regex_anchoring anchoring,
                              struct url_spec *url, regex_t **regex)
{
   int errcode;
   char rebuf[BUFFER_SIZE];
   const char *fmt = NULL;

   assert(pattern);
   assert(strlen(pattern) < sizeof(rebuf) - 2);

   if (pattern[0] == '\0')
   {
      *regex = NULL;
      return JB_ERR_OK;
   }

   switch (anchoring)
   {
      case NO_ANCHORING:
         fmt = "%s";
         break;
      case RIGHT_ANCHORED:
         fmt = "%s$";
         break;
      case LEFT_ANCHORED:
         fmt = "^%s";
         break;
      default:
         log_error(LOG_LEVEL_FATAL,
            "Invalid anchoring in compile_pattern %d", anchoring);
   }

   *regex = zalloc(sizeof(**regex));
   if (NULL == *regex)
   {
      free_url_spec(url);
      return JB_ERR_MEMORY;
   }

   snprintf(rebuf, sizeof(rebuf), fmt, pattern);

   errcode = regcomp(*regex, rebuf, (REG_EXTENDED|REG_NOSUB|REG_ICASE));

   if (errcode)
   {
      size_t errlen = regerror(errcode, *regex, rebuf, sizeof(rebuf));
      if (errlen > (sizeof(rebuf) - (size_t)1))
      {
         errlen = sizeof(rebuf) - (size_t)1;
      }
      rebuf[errlen] = '\0';
      log_error(LOG_LEVEL_ERROR, "error compiling %s from %s: %s",
         pattern, url->spec, rebuf);
      free_url_spec(url);

      return JB_ERR_PARSE;
   }

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  compile_url_pattern
 *
 * Description :  Compiles the three parts of an URL pattern.
 *
 * Parameters  :
 *          1  :  url = Target url_spec to be filled in.
 *          2  :  buf = The url pattern to compile. Will be messed up.
 *
 * Returns     :  JB_ERR_OK - Success
 *                JB_ERR_MEMORY - Out of memory
 *                JB_ERR_PARSE - Cannot parse regex
 *
 *********************************************************************/
static jb_err compile_url_pattern(struct url_spec *url, char *buf)
{
   char *p;

   p = strchr(buf, '/');
   if (NULL != p)
   {
      /*
       * Only compile the regex if it consists of more than
       * a single slash, otherwise it wouldn't affect the result.
       */
      if (p[1] != '\0')
      {
         /*
          * XXX: does it make sense to compile the slash at the beginning?
          */
         jb_err err = compile_pattern(p, LEFT_ANCHORED, url, &url->preg);

         if (JB_ERR_OK != err)
         {
            return err;
         }
      }
      *p = '\0';
   }

   p = strchr(buf, ':');
   if (NULL != p)
   {
      *p++ = '\0';
      url->port_list = strdup(p);
      if (NULL == url->port_list)
      {
         return JB_ERR_MEMORY;
      }
   }
   else
   {
      url->port_list = NULL;
   }

   if (buf[0] != '\0')
   {
      return compile_host_pattern(url, buf);
   }

   return JB_ERR_OK;

}


#ifdef FEATURE_EXTENDED_HOST_PATTERNS
/*********************************************************************
 *
 * Function    :  compile_host_pattern
 *
 * Description :  Parses and compiles a host pattern..
 *
 * Parameters  :
 *          1  :  url = Target url_spec to be filled in.
 *          2  :  host_pattern = Host pattern to compile.
 *
 * Returns     :  JB_ERR_OK - Success
 *                JB_ERR_MEMORY - Out of memory
 *                JB_ERR_PARSE - Cannot parse regex
 *
 *********************************************************************/
static jb_err compile_host_pattern(struct url_spec *url, const char *host_pattern)
{
   return compile_pattern(host_pattern, RIGHT_ANCHORED, url, &url->host_regex);
}

#else

/*********************************************************************
 *
 * Function    :  compile_host_pattern
 *
 * Description :  Parses and "compiles" an old-school host pattern.
 *
 * Parameters  :
 *          1  :  url = Target url_spec to be filled in.
 *          2  :  host_pattern = Host pattern to parse.
 *
 * Returns     :  JB_ERR_OK - Success
 *                JB_ERR_MEMORY - Out of memory
 *                JB_ERR_PARSE - Cannot parse regex
 *
 *********************************************************************/
static jb_err compile_host_pattern(struct url_spec *url, const char *host_pattern)
{
   char *v[150];
   size_t size;
   char *p;

   /*
    * Parse domain part
    */
   if (host_pattern[strlen(host_pattern) - 1] == '.')
   {
      url->unanchored |= ANCHOR_RIGHT;
   }
   if (host_pattern[0] == '.')
   {
      url->unanchored |= ANCHOR_LEFT;
   }

   /* 
    * Split domain into components
    */
   url->dbuffer = strdup(host_pattern);
   if (NULL == url->dbuffer)
   {
      free_url_spec(url);
      return JB_ERR_MEMORY;
   }

   /* 
    * Map to lower case
    */
   for (p = url->dbuffer; *p ; p++)
   {
      *p = (char)tolower((int)(unsigned char)*p);
   }

   /* 
    * Split the domain name into components
    */
   url->dcount = ssplit(url->dbuffer, ".", v, SZ(v), 1, 1);

   if (url->dcount < 0)
   {
      free_url_spec(url);
      return JB_ERR_MEMORY;
   }
   else if (url->dcount != 0)
   {
      /* 
       * Save a copy of the pointers in dvec
       */
      size = (size_t)url->dcount * sizeof(*url->dvec);
      
      url->dvec = (char **)malloc(size);
      if (NULL == url->dvec)
      {
         free_url_spec(url);
         return JB_ERR_MEMORY;
      }

      memcpy(url->dvec, v, size);
   }
   /*
    * else dcount == 0 in which case we needn't do anything,
    * since dvec will never be accessed and the pattern will
    * match all domains.
    */
   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  simplematch
 *
 * Description :  String matching, with a (greedy) '*' wildcard that
 *                stands for zero or more arbitrary characters and
 *                character classes in [], which take both enumerations
 *                and ranges.
 *
 * Parameters  :
 *          1  :  pattern = pattern for matching
 *          2  :  text    = text to be matched
 *
 * Returns     :  0 if match, else nonzero
 *
 *********************************************************************/
static int simplematch(const char *pattern, const char *text)
{
   const unsigned char *pat = (const unsigned char *)pattern;
   const unsigned char *txt = (const unsigned char *)text;
   const unsigned char *fallback = pat; 
   int wildcard = 0;
  
   unsigned char lastchar = 'a';
   unsigned i;
   unsigned char charmap[32];
  
   while (*txt)
   {

      /* EOF pattern but !EOF text? */
      if (*pat == '\0')
      {
         if (wildcard)
         {
            pat = fallback;
         }
         else
         {
            return 1;
         }
      }

      /* '*' in the pattern?  */
      if (*pat == '*') 
      {
     
         /* The pattern ends afterwards? Speed up the return. */
         if (*++pat == '\0')
         {
            return 0;
         }
     
         /* Else, set wildcard mode and remember position after '*' */
         wildcard = 1;
         fallback = pat;
      }

      /* Character range specification? */
      if (*pat == '[')
      {
         memset(charmap, '\0', sizeof(charmap));

         while (*++pat != ']')
         {
            if (!*pat)
            { 
               return 1;
            }
            else if (*pat == '-')
            {
               if ((*++pat == ']') || *pat == '\0')
               {
                  return(1);
               }
               for (i = lastchar; i <= *pat; i++)
               {
                  charmap[i / 8] |= (unsigned char)(1 << (i % 8));
               } 
            }
            else
            {
               charmap[*pat / 8] |= (unsigned char)(1 << (*pat % 8));
               lastchar = *pat;
            }
         }
      } /* -END- if Character range specification */


      /* 
       * Char match, or char range match? 
       */
      if ( (*pat == *txt)
      ||   (*pat == '?')
      ||   ((*pat == ']') && (charmap[*txt / 8] & (1 << (*txt % 8)))) )
      {
         /* 
          * Sucess: Go ahead
          */
         pat++;
      }
      else if (!wildcard)
      {
         /* 
          * No match && no wildcard: No luck
          */
         return 1;
      }
      else if (pat != fallback)
      {
         /*
          * Increment text pointer if in char range matching
          */
         if (*pat == ']')
         {
            txt++;
         }
         /*
          * Wildcard mode && nonmatch beyond fallback: Rewind pattern
          */
         pat = fallback;
         /*
          * Restart matching from current text pointer
          */
         continue;
      }
      txt++;
   }

   /* Cut off extra '*'s */
   if(*pat == '*')  pat++;

   /* If this is the pattern's end, fine! */
   return(*pat);

}


/*********************************************************************
 *
 * Function    :  simple_domaincmp
 *
 * Description :  Domain-wise Compare fqdn's.  The comparison is
 *                both left- and right-anchored.  The individual
 *                domain names are compared with simplematch().
 *                This is only used by domain_match.
 *
 * Parameters  :
 *          1  :  pv = array of patterns to compare
 *          2  :  fv = array of domain components to compare
 *          3  :  len = length of the arrays (both arrays are the
 *                      same length - if they weren't, it couldn't
 *                      possibly be a match).
 *
 * Returns     :  0 => domains are equivalent, else no match.
 *
 *********************************************************************/
static int simple_domaincmp(char **pv, char **fv, int len)
{
   int n;

   for (n = 0; n < len; n++)
   {
      if (simplematch(pv[n], fv[n]))
      {
         return 1;
      }
   }

   return 0;

}


/*********************************************************************
 *
 * Function    :  domain_match
 *
 * Description :  Domain-wise Compare fqdn's. Governed by the bimap in
 *                pattern->unachored, the comparison is un-, left-,
 *                right-anchored, or both.
 *                The individual domain names are compared with
 *                simplematch().
 *
 * Parameters  :
 *          1  :  pattern = a domain that may contain a '*' as a wildcard.
 *          2  :  fqdn = domain name against which the patterns are compared.
 *
 * Returns     :  0 => domains are equivalent, else no match.
 *
 *********************************************************************/
static int domain_match(const struct url_spec *pattern, const struct http_request *fqdn)
{
   char **pv, **fv;  /* vectors  */
   int    plen, flen;
   int unanchored = pattern->unanchored & (ANCHOR_RIGHT | ANCHOR_LEFT);

   plen = pattern->dcount;
   flen = fqdn->dcount;

   if (flen < plen)
   {
      /* fqdn is too short to match this pattern */
      return 1;
   }

   pv   = pattern->dvec;
   fv   = fqdn->dvec;

   if (unanchored == ANCHOR_LEFT)
   {
      /*
       * Right anchored.
       *
       * Convert this into a fully anchored pattern with
       * the fqdn and pattern the same length
       */
      fv += (flen - plen); /* flen - plen >= 0 due to check above */
      return simple_domaincmp(pv, fv, plen);
   }
   else if (unanchored == 0)
   {
      /* Fully anchored, check length */
      if (flen != plen)
      {
         return 1;
      }
      return simple_domaincmp(pv, fv, plen);
   }
   else if (unanchored == ANCHOR_RIGHT)
   {
      /* Left anchored, ignore all extra in fqdn */
      return simple_domaincmp(pv, fv, plen);
   }
   else
   {
      /* Unanchored */
      int n;
      int maxn = flen - plen;
      for (n = 0; n <= maxn; n++)
      {
         if (!simple_domaincmp(pv, fv, plen))
         {
            return 0;
         }
         /*
          * Doesn't match from start of fqdn
          * Try skipping first part of fqdn
          */
         fv++;
      }
      return 1;
   }

}
#endif /* def FEATURE_EXTENDED_HOST_PATTERNS */


/*********************************************************************
 *
 * Function    :  create_url_spec
 *
 * Description :  Creates a "url_spec" structure from a string.
 *                When finished, free with free_url_spec().
 *
 * Parameters  :
 *          1  :  url = Target url_spec to be filled in.  Will be
 *                      zeroed before use.
 *          2  :  buf = Source pattern, null terminated.  NOTE: The
 *                      contents of this buffer are destroyed by this
 *                      function.  If this function succeeds, the
 *                      buffer is copied to url->spec.  If this
 *                      function fails, the contents of the buffer
 *                      are lost forever.
 *
 * Returns     :  JB_ERR_OK - Success
 *                JB_ERR_MEMORY - Out of memory
 *                JB_ERR_PARSE - Cannot parse regex (Detailed message
 *                               written to system log)
 *
 *********************************************************************/
jb_err create_url_spec(struct url_spec *url, char *buf)
{
   assert(url);
   assert(buf);

   memset(url, '\0', sizeof(*url));

   /* Remember the original specification for the CGI pages. */
   url->spec = strdup(buf);
   if (NULL == url->spec)
   {
      return JB_ERR_MEMORY;
   }

   /* Is it tag pattern? */
   if (0 == strncmpic("TAG:", url->spec, 4))
   {
      /* The pattern starts with the first character after "TAG:" */
      const char *tag_pattern = buf + 4;
      return compile_pattern(tag_pattern, NO_ANCHORING, url, &url->tag_regex);
   }

   /* If it isn't a tag pattern it must be a URL pattern. */
   return compile_url_pattern(url, buf);
}


/*********************************************************************
 *
 * Function    :  free_url_spec
 *
 * Description :  Called from the "unloaders".  Freez the url
 *                structure elements.
 *
 * Parameters  :
 *          1  :  url = pointer to a url_spec structure.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void free_url_spec(struct url_spec *url)
{
   if (url == NULL) return;

   freez(url->spec);
#ifdef FEATURE_EXTENDED_HOST_PATTERNS
   if (url->host_regex)
   {
      regfree(url->host_regex);
      freez(url->host_regex);
   }
#else
   freez(url->dbuffer);
   freez(url->dvec);
   url->dcount = 0;
#endif /* ndef FEATURE_EXTENDED_HOST_PATTERNS */
   freez(url->port_list);
   if (url->preg)
   {
      regfree(url->preg);
      freez(url->preg);
   }
   if (url->tag_regex)
   {
      regfree(url->tag_regex);
      freez(url->tag_regex);
   }
}


/*********************************************************************
 *
 * Function    :  url_match
 *
 * Description :  Compare a URL against a URL pattern.
 *
 * Parameters  :
 *          1  :  pattern = a URL pattern
 *          2  :  url = URL to match
 *
 * Returns     :  Nonzero if the URL matches the pattern, else 0.
 *
 *********************************************************************/
int url_match(const struct url_spec *pattern,
              const struct http_request *http)
{
   /* XXX: these should probably be functions. */
#define PORT_MATCHES ((NULL == pattern->port_list) || match_portlist(pattern->port_list, http->port))
#ifdef FEATURE_EXTENDED_HOST_PATTERNS
#define DOMAIN_MATCHES ((NULL == pattern->host_regex) || (0 == regexec(pattern->host_regex, http->host, 0, NULL, 0)))
#else
#define DOMAIN_MATCHES ((NULL == pattern->dbuffer) || (0 == domain_match(pattern, http)))
#endif
#define PATH_MATCHES ((NULL == pattern->preg) || (0 == regexec(pattern->preg, http->path, 0, NULL, 0)))

   if (pattern->tag_regex != NULL)
   {
      /* It's a tag pattern and shouldn't be matched against URLs */
      return 0;
   } 

   return (PORT_MATCHES && DOMAIN_MATCHES && PATH_MATCHES);

}


/*********************************************************************
 *
 * Function    :  match_portlist
 *
 * Description :  Check if a given number is covered by a comma
 *                separated list of numbers and ranges (a,b-c,d,..)
 *
 * Parameters  :
 *          1  :  portlist = String with list
 *          2  :  port = port to check
 *
 * Returns     :  0 => no match
 *                1 => match
 *
 *********************************************************************/
int match_portlist(const char *portlist, int port)
{
   char *min, *max, *next, *portlist_copy;

   min = next = portlist_copy = strdup(portlist);

   /*
    * Zero-terminate first item and remember offset for next
    */
   if (NULL != (next = strchr(portlist_copy, (int) ',')))
   {
      *next++ = '\0';
   }

   /*
    * Loop through all items, checking for match
    */
   while(min)
   {
      if (NULL == (max = strchr(min, (int) '-')))
      {
         /*
          * No dash, check for equality
          */
         if (port == atoi(min))
         {
            freez(portlist_copy);
            return(1);
         }
      }
      else
      {
         /*
          * This is a range, so check if between min and max,
          * or, if max was omitted, between min and 65K
          */
         *max++ = '\0';
         if(port >= atoi(min) && port <= (atoi(max) ? atoi(max) : 65535))
         {
            freez(portlist_copy);
            return(1);
         }

      }

      /*
       * Jump to next item
       */
      min = next;

      /*
       * Zero-terminate next item and remember offset for n+1
       */
      if ((NULL != next) && (NULL != (next = strchr(next, (int) ','))))
      {
         *next++ = '\0';
      }
   }

   freez(portlist_copy);
   return 0;

}


/*
  Local Variables:
  tab-width: 3
  end:
*/
