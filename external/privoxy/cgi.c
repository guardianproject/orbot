const char cgi_rcs[] = "$Id: cgi.c,v 1.145 2011/10/08 17:31:05 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/cgi.c,v $
 *
 * Purpose     :  Declares functions to intercept request, generate
 *                html or gif answers, and to compose HTTP resonses.
 *                This only contains the framework functions, the
 *                actual handler functions are declared elsewhere.
 *
 *                Functions declared include:
 *
 *
 * Copyright   :  Written by and Copyright (C) 2001-2004, 2006-2008
 *                the SourceForge Privoxy team. http://www.privoxy.org/
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
 **********************************************************************/


#include "config.h"

#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <limits.h>
#include <assert.h>

#ifdef FEATURE_COMPRESSION
#include <zlib.h>
#endif

#include "project.h"
#include "cgi.h"
#include "list.h"
#include "encode.h"
#include "ssplit.h"
#include "errlog.h"
#include "filters.h"
#include "miscutil.h"
#include "cgisimple.h"
#include "jbsockets.h"
#if defined(FEATURE_CGI_EDIT_ACTIONS) || defined(FEATURE_TOGGLE)
#include "cgiedit.h"
#endif /* defined(FEATURE_CGI_EDIT_ACTIONS) || defined (FEATURE_TOGGLE) */

/* loadcfg.h is for global_toggle_state only */
#include "loadcfg.h"
/* jcc.h is for mutex semaphore globals only */
#include "jcc.h"

const char cgi_h_rcs[] = CGI_H_VERSION;

/*
 * List of CGI functions: name, handler, description
 * Note: Do NOT use single quotes in the description;
 *       this will break the dynamic "blocked" template!
 */
static const struct cgi_dispatcher cgi_dispatchers[] = {
   { "",
         cgi_default,
         "Privoxy main page",
         TRUE },
#ifdef FEATURE_GRACEFUL_TERMINATION
   { "die",
         cgi_die,
         "<b>Shut down</b> - <em class=\"warning\">Do not deploy this build in a production environment, "
        "this is a one click Denial Of Service attack!!!</em>",
         FALSE },
#endif
   { "show-status",
         cgi_show_status,
#ifdef FEATURE_CGI_EDIT_ACTIONS
        "View &amp; change the current configuration",
#else
        "View the current configuration",
#endif
         TRUE },
   { "show-version",
         cgi_show_version,
         "View the source code version numbers",
          TRUE },
   { "show-request",
         cgi_show_request,
         "View the request headers",
         TRUE },
   { "show-url-info",
         cgi_show_url_info,
         "Look up which actions apply to a URL and why",
         TRUE },
#ifdef FEATURE_TOGGLE
   { "toggle",
         cgi_toggle,
         "Toggle Privoxy on or off",
         FALSE },
#endif /* def FEATURE_TOGGLE */
#ifdef FEATURE_CGI_EDIT_ACTIONS
   { "edit-actions", /* Edit the actions list */
         cgi_edit_actions,
         NULL, FALSE },
   { "eaa", /* Shortcut for edit-actions-add-url-form */
         cgi_edit_actions_add_url_form,
         NULL, FALSE },
   { "eau", /* Shortcut for edit-actions-url-form */
         cgi_edit_actions_url_form,
         NULL, FALSE },
   { "ear", /* Shortcut for edit-actions-remove-url-form */
         cgi_edit_actions_remove_url_form,
         NULL, FALSE },
   { "eal", /* Shortcut for edit-actions-list */
         cgi_edit_actions_list,
         NULL, FALSE },
   { "eafu", /* Shortcut for edit-actions-for-url */
         cgi_edit_actions_for_url,
         NULL, FALSE },
   { "eas", /* Shortcut for edit-actions-submit */
         cgi_edit_actions_submit,
         NULL, FALSE },
   { "easa", /* Shortcut for edit-actions-section-add */
         cgi_edit_actions_section_add,
         NULL, FALSE  },
   { "easr", /* Shortcut for edit-actions-section-remove */
         cgi_edit_actions_section_remove,
         NULL, FALSE  },
   { "eass", /* Shortcut for edit-actions-section-swap */
         cgi_edit_actions_section_swap,
         NULL, FALSE  },
   { "edit-actions-for-url",
         cgi_edit_actions_for_url,
         NULL, FALSE  /* Edit the actions for (a) specified URL(s) */ },
   { "edit-actions-list",
         cgi_edit_actions_list,
         NULL, TRUE /* Edit the actions list */ },
   { "edit-actions-submit",
         cgi_edit_actions_submit,
         NULL, FALSE /* Change the actions for (a) specified URL(s) */ },
   { "edit-actions-url",
         cgi_edit_actions_url,
         NULL, FALSE /* Change a URL pattern in the actionsfile */ },
   { "edit-actions-url-form",
         cgi_edit_actions_url_form,
         NULL, FALSE /* Form to change a URL pattern in the actionsfile */ },
   { "edit-actions-add-url",
         cgi_edit_actions_add_url,
         NULL, FALSE /* Add a URL pattern to the actionsfile */ },
   { "edit-actions-add-url-form",
         cgi_edit_actions_add_url_form,
         NULL, FALSE /* Form to add a URL pattern to the actionsfile */ },
   { "edit-actions-remove-url",
         cgi_edit_actions_remove_url,
         NULL, FALSE /* Remove a URL pattern from the actionsfile */ },
   { "edit-actions-remove-url-form",
         cgi_edit_actions_remove_url_form,
         NULL, FALSE /* Form to remove a URL pattern from the actionsfile */ },
   { "edit-actions-section-add",
         cgi_edit_actions_section_add,
         NULL, FALSE /* Remove a section from the actionsfile */ },
   { "edit-actions-section-remove",
         cgi_edit_actions_section_remove,
         NULL, FALSE /* Remove a section from the actionsfile */ },
   { "edit-actions-section-swap",
         cgi_edit_actions_section_swap,
         NULL, FALSE /* Swap two sections in the actionsfile */ },
#endif /* def FEATURE_CGI_EDIT_ACTIONS */
   { "error-favicon.ico",
         cgi_send_error_favicon,
         NULL, TRUE /* Sends the favicon image for error pages. */ },
   { "favicon.ico",
         cgi_send_default_favicon,
         NULL, TRUE /* Sends the default favicon image. */ },
   { "robots.txt",
         cgi_robots_txt,
         NULL, TRUE /* Sends a robots.txt file to tell robots to go away. */ },
   { "send-banner",
         cgi_send_banner,
         NULL, TRUE /* Send a built-in image */ },
   { "send-stylesheet",
         cgi_send_stylesheet,
         NULL, FALSE /* Send templates/cgi-style.css */ },
   { "t",
         cgi_transparent_image,
         NULL, TRUE /* Send a transparent image (short name) */ },
   { "url-info-osd.xml",
         cgi_send_url_info_osd,
         NULL, TRUE /* Send templates/url-info-osd.xml */ },
   { "user-manual",
          cgi_send_user_manual,
          NULL, TRUE /* Send user-manual */ },
   { NULL, /* NULL Indicates end of list and default page */
         cgi_error_404,
         NULL, TRUE /* Unknown CGI page */ }
};


/*
 * Built-in images for ad replacement
 *
 * Hint: You can encode your own images like this:
 * cat your-image | perl -e 'while (read STDIN, $c, 1) { printf("\\%.3o", unpack("C", $c)); }'
 */

#ifdef FEATURE_NO_GIFS

/*
 * Checkerboard pattern, as a PNG.
 */
const char image_pattern_data[] =
   "\211\120\116\107\015\012\032\012\000\000\000\015\111\110\104"
   "\122\000\000\000\004\000\000\000\004\010\006\000\000\000\251"
   "\361\236\176\000\000\000\006\142\113\107\104\000\000\000\000"
   "\000\000\371\103\273\177\000\000\000\033\111\104\101\124\010"
   "\327\143\140\140\140\060\377\377\377\077\003\234\106\341\060"
   "\060\230\063\020\124\001\000\161\021\031\241\034\364\030\143"
   "\000\000\000\000\111\105\116\104\256\102\140\202";

/*
 * 1x1 transparant PNG.
 */
const char image_blank_data[] =
 "\211\120\116\107\015\012\032\012\000\000\000\015\111\110\104\122"
 "\000\000\000\001\000\000\000\001\001\003\000\000\000\045\333\126"
 "\312\000\000\000\003\120\114\124\105\377\377\377\247\304\033\310"
 "\000\000\000\001\164\122\116\123\000\100\346\330\146\000\000\000"
 "\001\142\113\107\104\000\210\005\035\110\000\000\000\012\111\104"
 "\101\124\170\001\143\140\000\000\000\002\000\001\163\165\001\030"
 "\000\000\000\000\111\105\116\104\256\102\140\202";
#else

/*
 * Checkerboard pattern, as a GIF.
 */
const char image_pattern_data[] =
   "\107\111\106\070\071\141\004\000\004\000\200\000\000\310\310"
   "\310\377\377\377\041\376\016\111\040\167\141\163\040\141\040"
   "\142\141\156\156\145\162\000\041\371\004\001\012\000\001\000"
   "\054\000\000\000\000\004\000\004\000\000\002\005\104\174\147"
   "\270\005\000\073";

/*
 * 1x1 transparant GIF.
 */
const char image_blank_data[] =
   "GIF89a\001\000\001\000\200\000\000\377\377\377\000\000"
   "\000!\371\004\001\000\000\000\000,\000\000\000\000\001"
   "\000\001\000\000\002\002D\001\000;";
#endif

const size_t image_pattern_length = sizeof(image_pattern_data) - 1;
const size_t image_blank_length   = sizeof(image_blank_data) - 1;

#ifdef FEATURE_COMPRESSION
/*
 * Minimum length which a buffer has to reach before
 * we bother to (re-)compress it. Completely arbitrary.
 */
const size_t LOWER_LENGTH_LIMIT_FOR_COMPRESSION = 1024U;
#endif

static struct http_response cgi_error_memory_response[1];

static struct http_response *dispatch_known_cgi(struct client_state * csp,
                                                const char * path);
static struct map *parse_cgi_parameters(char *argstring);


/*********************************************************************
 *
 * Function    :  dispatch_cgi
 *
 * Description :  Checks if a request URL has either the magical
 *                hostname CGI_SITE_1_HOST (usually http://p.p/) or
 *                matches CGI_SITE_2_HOST CGI_SITE_2_PATH (usually
 *                http://config.privoxy.org/). If so, it passes
 *                the (rest of the) path onto dispatch_known_cgi, which
 *                calls the relevant CGI handler function.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  http_response if match, NULL if nonmatch or handler fail
 *
 *********************************************************************/
struct http_response *dispatch_cgi(struct client_state *csp)
{
   const char *host = csp->http->host;
   const char *path = csp->http->path;

   /*
    * Should we intercept ?
    */

   /* Note: "example.com" and "example.com." are equivalent hostnames. */

   /* Either the host matches CGI_SITE_1_HOST ..*/
   if (   ( (0 == strcmpic(host, CGI_SITE_1_HOST))
         || (0 == strcmpic(host, CGI_SITE_1_HOST ".")))
       && (path[0] == '/') )
   {
      /* ..then the path will all be for us.  Remove leading '/' */
      path++;
   }
   /* Or it's the host part CGI_SITE_2_HOST, and the path CGI_SITE_2_PATH */
   else if ( ( (0 == strcmpic(host, CGI_SITE_2_HOST ))
            || (0 == strcmpic(host, CGI_SITE_2_HOST ".")) )
          && (0 == strncmpic(path, CGI_SITE_2_PATH, strlen(CGI_SITE_2_PATH))) )
   {
      /* take everything following CGI_SITE_2_PATH */
      path += strlen(CGI_SITE_2_PATH);
      if (*path == '/')
      {
         /* skip the forward slash after CGI_SITE_2_PATH */
         path++;
      }
      else if (*path != '\0')
      {
         /*
          * weirdness: URL is /configXXX, where XXX is some string
          * Do *NOT* intercept.
          */
         return NULL;
      }
   }
   else
   {
      /* Not a CGI */
      return NULL;
   }

   /*
    * This is a CGI call.
    */

   return dispatch_known_cgi(csp, path);
}


/*********************************************************************
 *
 * Function    :  grep_cgi_referrer
 *
 * Description :  Ugly provisorical fix that greps the value of the
 *                referer HTTP header field out of a linked list of
 *                strings like found at csp->headers. Will disappear
 *                in Privoxy 3.1.
 *
 *                FIXME: csp->headers ought to be csp->http->headers
 *                FIXME: Parsing all client header lines should
 *                       happen right after the request is received!
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  pointer to value (no copy!), or NULL if none found.
 *
 *********************************************************************/
static char *grep_cgi_referrer(const struct client_state *csp)
{
   struct list_entry *p;

   for (p = csp->headers->first; p != NULL; p = p->next)
   {
      if (p->str == NULL) continue;
      if (strncmpic(p->str, "Referer: ", 9) == 0)
      {
         return ((p->str) + 9);
      }
   }
   return NULL;

}


/*********************************************************************
 *
 * Function    :  referrer_is_safe
 *
 * Description :  Decides whether we trust the Referer for
 *                CGI pages which are only meant to be reachable
 *                through Privoxy's web interface directly.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  TRUE  if the referrer is safe, or
 *                FALSE if the referrer is unsafe or not set.
 *
 *********************************************************************/
static int referrer_is_safe(const struct client_state *csp)
{
   char *referrer;
   static const char alternative_prefix[] = "http://" CGI_SITE_1_HOST "/";

   referrer = grep_cgi_referrer(csp);

   if (NULL == referrer)
   {
      /* No referrer, no access  */
      log_error(LOG_LEVEL_ERROR, "Denying access to %s. No referrer found.",
         csp->http->url);
   }
   else if ((0 == strncmp(referrer, CGI_PREFIX, sizeof(CGI_PREFIX)-1)
         || (0 == strncmp(referrer, alternative_prefix, strlen(alternative_prefix)))))
   {
      /* Trustworthy referrer */
      log_error(LOG_LEVEL_CGI, "Granting access to %s, referrer %s is trustworthy.",
         csp->http->url, referrer);

      return TRUE;
   }
   else
   {
      /* Untrustworthy referrer */
      log_error(LOG_LEVEL_ERROR, "Denying access to %s, referrer %s isn't trustworthy.",
         csp->http->url, referrer);
   }

   return FALSE;

}

/*********************************************************************
 *
 * Function    :  dispatch_known_cgi
 *
 * Description :  Processes a CGI once dispatch_cgi has determined that
 *                it matches one of the magic prefixes. Parses the path
 *                as a cgi name plus query string, prepares a map that
 *                maps CGI parameter names to their values, initializes
 *                the http_response struct, and calls the relevant CGI
 *                handler function.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  path = Path of CGI, with the CGI prefix removed.
 *                       Should not have a leading "/".
 *
 * Returns     :  http_response, or NULL on handler failure or out of
 *                memory.
 *
 *********************************************************************/
static struct http_response *dispatch_known_cgi(struct client_state * csp,
                                                const char * path)
{
   const struct cgi_dispatcher *d;
   struct map *param_list;
   struct http_response *rsp;
   char *query_args_start;
   char *path_copy;
   jb_err err;

   if (NULL == (path_copy = strdup(path)))
   {
      return cgi_error_memory();
   }
   query_args_start = path_copy;
   while (*query_args_start && *query_args_start != '?' && *query_args_start != '/')
   {
      query_args_start++;
   }
   if (*query_args_start == '/')
   {
      *query_args_start++ = '\0';
      if ((param_list = new_map()))
      {
         map(param_list, "file", 1, url_decode(query_args_start), 0);
      }
   }
   else
   {
      if (*query_args_start == '?')
      {
         *query_args_start++ = '\0';
      }
      if (NULL == (param_list = parse_cgi_parameters(query_args_start)))
      {
         free(path_copy);
         return cgi_error_memory();
      }
   }

   /*
    * At this point:
    * path_copy        = CGI call name
    * param_list       = CGI params, as map
    */

   /* Get mem for response or fail*/
   if (NULL == (rsp = alloc_http_response()))
   {
      free(path_copy);
      free_map(param_list);
      return cgi_error_memory();
   }

   /*
    * Find and start the right CGI function
    */
   d = cgi_dispatchers;
   for (;;)
   {
      if ((d->name == NULL) || (strcmp(path_copy, d->name) == 0))
      {
         /*
          * If the called CGI is either harmless, or referred
          * from a trusted source, start it.
          */
         if (d->harmless || referrer_is_safe(csp))
         {
            err = (d->handler)(csp, rsp, param_list);
         }
         else
         {
            /*
             * Else, modify toggle calls so that they only display
             * the status, and deny all other calls.
             */
            if (0 == strcmp(path_copy, "toggle"))
            {
               unmap(param_list, "set");
               err = (d->handler)(csp, rsp, param_list);
            }
            else
            {
               err = cgi_error_disabled(csp, rsp);
            }
         }

         free(path_copy);
         free_map(param_list);

         if (err == JB_ERR_CGI_PARAMS)
         {
            err = cgi_error_bad_param(csp, rsp);
         }
         if (err && (err != JB_ERR_MEMORY))
         {
            /* Unexpected error! Shouldn't get here */
            log_error(LOG_LEVEL_ERROR,
               "Unexpected CGI error %d in top-level handler. "
               "Please file a bug report!", err);
            err = cgi_error_unknown(csp, rsp, err);
         }
         if (!err)
         {
            /* It worked */
            rsp->crunch_reason = CGI_CALL;
            return finish_http_response(csp, rsp);
         }
         else
         {
            /* Error in handler, probably out-of-memory */
            free_http_response(rsp);
            return cgi_error_memory();
         }
      }
      d++;
   }
}


/*********************************************************************
 *
 * Function    :  parse_cgi_parameters
 *
 * Description :  Parse a URL-encoded argument string into name/value
 *                pairs and store them in a struct map list.
 *
 * Parameters  :
 *          1  :  argstring = string to be parsed.  Will be trashed.
 *
 * Returns     :  pointer to param list, or NULL if out of memory.
 *
 *********************************************************************/
static struct map *parse_cgi_parameters(char *argstring)
{
   char *p;
   char *vector[BUFFER_SIZE];
   int pairs, i;
   struct map *cgi_params;

   if (NULL == (cgi_params = new_map()))
   {
      return NULL;
   }

   /*
    * IE 5 does, of course, violate RFC 2316 Sect 4.1 and sends
    * the fragment identifier along with the request, so we must
    * cut it off here, so it won't pollute the CGI params:
    */
   if (NULL != (p = strchr(argstring, '#')))
   {
      *p = '\0';
   }

   pairs = ssplit(argstring, "&", vector, SZ(vector), 1, 1);

   for (i = 0; i < pairs; i++)
   {
      if ((NULL != (p = strchr(vector[i], '='))) && (*(p+1) != '\0'))
      {
         *p = '\0';
         if (map(cgi_params, url_decode(vector[i]), 0, url_decode(++p), 0))
         {
            free_map(cgi_params);
            return NULL;
         }
      }
   }

   return cgi_params;

}


/*********************************************************************
 *
 * Function    :  get_char_param
 *
 * Description :  Get a single-character parameter passed to a CGI
 *                function.
 *
 * Parameters  :
 *          1  :  parameters = map of cgi parameters
 *          2  :  param_name = The name of the parameter to read
 *
 * Returns     :  Uppercase character on success, '\0' on error.
 *
 *********************************************************************/
char get_char_param(const struct map *parameters,
                    const char *param_name)
{
   char ch;

   assert(parameters);
   assert(param_name);

   ch = *(lookup(parameters, param_name));
   if ((ch >= 'a') && (ch <= 'z'))
   {
      ch = (char)(ch - 'a' + 'A');
   }

   return ch;
}


/*********************************************************************
 *
 * Function    :  get_string_param
 *
 * Description :  Get a string paramater, to be used as an
 *                ACTION_STRING or ACTION_MULTI paramater.
 *                Validates the input to prevent stupid/malicious
 *                users from corrupting their action file.
 *
 * Parameters  :
 *          1  :  parameters = map of cgi parameters
 *          2  :  param_name = The name of the parameter to read
 *          3  :  pparam = destination for paramater.  Allocated as
 *                part of the map "parameters", so don't free it.
 *                Set to NULL if not specified.
 *
 * Returns     :  JB_ERR_OK         on success, or if the paramater
 *                                  was not specified.
 *                JB_ERR_MEMORY     on out-of-memory.
 *                JB_ERR_CGI_PARAMS if the paramater is not valid.
 *
 *********************************************************************/
jb_err get_string_param(const struct map *parameters,
                        const char *param_name,
                        const char **pparam)
{
   const char *param;
   const char *s;
   char ch;

   assert(parameters);
   assert(param_name);
   assert(pparam);

   *pparam = NULL;

   param = lookup(parameters, param_name);
   if (!*param)
   {
      return JB_ERR_OK;
   }

   if (strlen(param) >= CGI_PARAM_LEN_MAX)
   {
      /*
       * Too long.
       *
       * Note that the length limit is arbitrary, it just seems
       * sensible to limit it to *something*.  There's no
       * technical reason for any limit at all.
       */
      return JB_ERR_CGI_PARAMS;
   }

   /* Check every character to see if it's legal */
   s = param;
   while ((ch = *s++) != '\0')
   {
      if ( ((unsigned char)ch < (unsigned char)' ')
        || (ch == '}') )
      {
         /* Probable hack attempt, or user accidentally used '}'. */
         return JB_ERR_CGI_PARAMS;
      }
   }

   /* Success */
   *pparam = param;

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  get_number_param
 *
 * Description :  Get a non-negative integer from the parameters
 *                passed to a CGI function.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  parameters = map of cgi parameters
 *          3  :  name = Name of CGI parameter to read
 *          4  :  pvalue = destination for value.
 *                         Set to -1 on error.
 *
 * Returns     :  JB_ERR_OK         on success
 *                JB_ERR_MEMORY     on out-of-memory
 *                JB_ERR_CGI_PARAMS if the parameter was not specified
 *                                  or is not valid.
 *
 *********************************************************************/
jb_err get_number_param(struct client_state *csp,
                        const struct map *parameters,
                        char *name,
                        unsigned *pvalue)
{
   const char *param;
   char ch;
   unsigned value;

   assert(csp);
   assert(parameters);
   assert(name);
   assert(pvalue);

   *pvalue = 0;

   param = lookup(parameters, name);
   if (!*param)
   {
      return JB_ERR_CGI_PARAMS;
   }

   /* We don't use atoi because I want to check this carefully... */

   value = 0;
   while ((ch = *param++) != '\0')
   {
      if ((ch < '0') || (ch > '9'))
      {
         return JB_ERR_CGI_PARAMS;
      }

      ch = (char)(ch - '0');

      /* Note:
       *
       * <limits.h> defines UINT_MAX
       *
       * (UINT_MAX - ch) / 10 is the largest number that
       *     can be safely multiplied by 10 then have ch added.
       */
      if (value > ((UINT_MAX - (unsigned)ch) / 10U))
      {
         return JB_ERR_CGI_PARAMS;
      }

      value = value * 10 + (unsigned)ch;
   }

   /* Success */
   *pvalue = value;

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  error_response
 *
 * Description :  returns an http_response that explains the reason
 *                why a request failed.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  templatename = Which template should be used for the answer
 *
 * Returns     :  A http_response.  If we run out of memory, this
 *                will be cgi_error_memory().
 *
 *********************************************************************/
struct http_response *error_response(struct client_state *csp,
                                     const char *templatename)
{
   jb_err err;
   struct http_response *rsp;
   struct map *exports = default_exports(csp, NULL);
   char *path = NULL;

   if (exports == NULL)
   {
      return cgi_error_memory();
   }

   if (NULL == (rsp = alloc_http_response()))
   {
      free_map(exports);
      return cgi_error_memory();
   }

#ifdef FEATURE_FORCE_LOAD
   if (csp->flags & CSP_FLAG_FORCED)
   {
      path = strdup(FORCE_PREFIX);
   }
   else
#endif /* def FEATURE_FORCE_LOAD */
   {
      path = strdup("");
   }
   err = string_append(&path, csp->http->path);

   if (!err) err = map(exports, "host", 1, html_encode(csp->http->host), 0);
   if (!err) err = map(exports, "hostport", 1, html_encode(csp->http->hostport), 0);
   if (!err) err = map(exports, "path", 1, html_encode_and_free_original(path), 0);
   if (!err) err = map(exports, "protocol", 1, csp->http->ssl ? "https://" : "http://", 1);
   if (!err)
   {
     err = map(exports, "host-ip", 1, html_encode(csp->http->host_ip_addr_str), 0);
     if (err)
     {
       /* Some failures, like "404 no such domain", don't have an IP address. */
       err = map(exports, "host-ip", 1, html_encode(csp->http->host), 0);
     }
   }


   if (err)
   {
      free_map(exports);
      free_http_response(rsp);
      return cgi_error_memory();
   }

   if (!strcmp(templatename, "no-such-domain"))
   {
      rsp->status = strdup("404 No such domain");
      rsp->crunch_reason = NO_SUCH_DOMAIN;
   }
   else if (!strcmp(templatename, "forwarding-failed"))
   {
      const struct forward_spec *fwd = forward_url(csp, csp->http);
      char *socks_type = NULL;
      if (fwd == NULL)
      {
         log_error(LOG_LEVEL_FATAL, "gateway spec is NULL. This shouldn't happen!");
         /* Never get here - LOG_LEVEL_FATAL causes program exit */
      }

      /*
       * XXX: While the template is called forwarding-failed,
       * it currently only handles socks forwarding failures.
       */
      assert(fwd != NULL);
      assert(fwd->type != SOCKS_NONE);

      /*
       * Map failure reason, forwarding type and forwarder.
       */
      if (NULL == csp->error_message)
      {
         /*
          * Either we forgot to record the failure reason,
          * or the memory allocation failed.
          */
         log_error(LOG_LEVEL_ERROR, "Socks failure reason missing.");
         csp->error_message = strdup("Failure reason missing. Check the log file for details.");
      }
      if (!err) err = map(exports, "gateway", 1, fwd->gateway_host, 1);

      /*
       * XXX: this is almost the same code as in cgi_show_url_info()
       * and thus should be factored out and shared.
       */
      switch (fwd->type)
      {
         case SOCKS_4:
            socks_type = "socks4-";
            break;
         case SOCKS_4A:
            socks_type = "socks4a-";
            break;
         case SOCKS_5:
            socks_type = "socks5-";
            break;
         default:
            log_error(LOG_LEVEL_FATAL, "Unknown socks type: %d.", fwd->type);
      }

      if (!err) err = map(exports, "forwarding-type", 1, socks_type, 1);
      if (!err) err = map(exports, "error-message", 1, html_encode(csp->error_message), 0);
      if ((NULL == csp->error_message) || err)
      {
         free_map(exports);
         free_http_response(rsp);
         return cgi_error_memory();
      }

      rsp->status = strdup("503 Forwarding failure");
      rsp->crunch_reason = FORWARDING_FAILED;
   }
   else if (!strcmp(templatename, "connect-failed"))
   {
      rsp->status = strdup("503 Connect failed");
      rsp->crunch_reason = CONNECT_FAILED;
   }
   else if (!strcmp(templatename, "connection-timeout"))
   {
      rsp->status = strdup("504 Connection timeout");
      rsp->crunch_reason = CONNECTION_TIMEOUT;
   }
   else if (!strcmp(templatename, "no-server-data"))
   {
      rsp->status = strdup("502 No data received from server or forwarder");
      rsp->crunch_reason = NO_SERVER_DATA;
   }

   if (rsp->status == NULL)
   {
      free_map(exports);
      free_http_response(rsp);
      return cgi_error_memory();
   }

   err = template_fill_for_cgi(csp, templatename, exports, rsp);
   if (err)
   {
      free_http_response(rsp);
      return cgi_error_memory();
   }

   return finish_http_response(csp, rsp);
}


/*********************************************************************
 *
 * Function    :  cgi_error_disabled
 *
 * Description :  CGI function that is called to generate an error
 *                response if the actions editor or toggle CGI are
 *                accessed despite having being disabled at compile-
 *                or run-time, or if the user followed an untrusted link
 *                to access a unsafe CGI feature that is only reachable
 *                through Privoxy directly.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *
 * CGI Parameters : none
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err cgi_error_disabled(const struct client_state *csp,
                          struct http_response *rsp)
{
   struct map *exports;

   assert(csp);
   assert(rsp);

   if (NULL == (exports = default_exports(csp, "cgi-error-disabled")))
   {
      return JB_ERR_MEMORY;
   }
   if (map(exports, "url", 1, html_encode(csp->http->url), 0))
   {
      /* Not important enough to do anything */
      log_error(LOG_LEVEL_ERROR, "Failed to fill in url.");
   }

   return template_fill_for_cgi(csp, "cgi-error-disabled", exports, rsp);
}


/*********************************************************************
 *
 * Function    :  cgi_init_error_messages
 *
 * Description :  Call at the start of the program to initialize
 *                the error message used by cgi_error_memory().
 *
 * Parameters  :  N/A
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void cgi_init_error_messages(void)
{
   memset(cgi_error_memory_response, '\0', sizeof(*cgi_error_memory_response));
   cgi_error_memory_response->head =
      "HTTP/1.0 500 Internal Privoxy Error\r\n"
      "Content-Type: text/html\r\n"
      "\r\n";
   cgi_error_memory_response->body =
      "<html>\n"
      "<head>\n"
      " <title>500 Internal Privoxy Error</title>\n"
      " <link rel=\"shortcut icon\" href=\"" CGI_PREFIX "error-favicon.ico\" type=\"image/x-icon\">"
      "</head>\n"
      "<body>\n"
      "<h1>500 Internal Privoxy Error</h1>\n"
      "<p>Privoxy <b>ran out of memory</b> while processing your request.</p>\n"
      "<p>Please contact your proxy administrator, or try again later</p>\n"
      "</body>\n"
      "</html>\n";

   cgi_error_memory_response->head_length =
      strlen(cgi_error_memory_response->head);
   cgi_error_memory_response->content_length =
      strlen(cgi_error_memory_response->body);
   cgi_error_memory_response->crunch_reason = OUT_OF_MEMORY;
}


/*********************************************************************
 *
 * Function    :  cgi_error_memory
 *
 * Description :  Called if a CGI function runs out of memory.
 *                Returns a statically-allocated error response.
 *
 * Parameters  :  N/A
 *
 * Returns     :  http_response data structure for output.  This is
 *                statically allocated, for obvious reasons.
 *
 *********************************************************************/
struct http_response *cgi_error_memory(void)
{
   /* assert that it's been initialized. */
   assert(cgi_error_memory_response->head);

   return cgi_error_memory_response;
}


/*********************************************************************
 *
 * Function    :  cgi_error_no_template
 *
 * Description :  Almost-CGI function that is called if a template
 *                cannot be loaded.  Note this is not a true CGI,
 *                it takes a template name rather than a map of
 *                parameters.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  template_name = Name of template that could not
 *                                be loaded.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err cgi_error_no_template(const struct client_state *csp,
                             struct http_response *rsp,
                             const char *template_name)
{
   static const char status[] =
      "500 Internal Privoxy Error";
   static const char body_prefix[] =
      "<html>\n"
      "<head>\n"
      " <title>500 Internal Privoxy Error</title>\n"
      " <link rel=\"shortcut icon\" href=\"" CGI_PREFIX "error-favicon.ico\" type=\"image/x-icon\">"
      "</head>\n"
      "<body>\n"
      "<h1>500 Internal Privoxy Error</h1>\n"
      "<p>Privoxy encountered an error while processing your request:</p>\n"
      "<p><b>Could not load template file <code>";
   static const char body_suffix[] =
      "</code> or one of its included components.</b></p>\n"
      "<p>Please contact your proxy administrator.</p>\n"
      "<p>If you are the proxy administrator, please put the required file(s)"
      "in the <code><i>(confdir)</i>/templates</code> directory.  The "
      "location of the <code><i>(confdir)</i></code> directory "
      "is specified in the main Privoxy <code>config</code> "
      "file.  (It's typically the Privoxy install directory"
#ifndef _WIN32
      ", or <code>/etc/privoxy/</code>"
#endif /* ndef _WIN32 */
      ").</p>\n"
      "</body>\n"
      "</html>\n";
   const size_t body_size = strlen(body_prefix) + strlen(template_name) + strlen(body_suffix) + 1;

   assert(csp);
   assert(rsp);
   assert(template_name);

   /* Reset rsp, if needed */
   freez(rsp->status);
   freez(rsp->head);
   freez(rsp->body);
   rsp->content_length = 0;
   rsp->head_length = 0;
   rsp->is_static = 0;

   rsp->body = malloc(body_size);
   if (rsp->body == NULL)
   {
      return JB_ERR_MEMORY;
   }
   strlcpy(rsp->body, body_prefix, body_size);
   strlcat(rsp->body, template_name, body_size);
   strlcat(rsp->body, body_suffix, body_size);

   rsp->status = strdup(status);
   if (rsp->status == NULL)
   {
      return JB_ERR_MEMORY;
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  cgi_error_unknown
 *
 * Description :  Almost-CGI function that is called if an unexpected
 *                error occurs in the top-level CGI dispatcher.
 *                In this context, "unexpected" means "anything other
 *                than JB_ERR_MEMORY or JB_ERR_CGI_PARAMS" - CGIs are
 *                expected to handle all other errors internally,
 *                since they can give more relavent error messages
 *                that way.
 *
 *                Note this is not a true CGI, it takes an error
 *                code rather than a map of parameters.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  error_to_report = Error code to report.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err cgi_error_unknown(const struct client_state *csp,
                         struct http_response *rsp,
                         jb_err error_to_report)
{
   static const char status[] =
      "500 Internal Privoxy Error";
   static const char body_prefix[] =
      "<html>\n"
      "<head>\n"
      " <title>500 Internal Privoxy Error</title>\n"
      " <link rel=\"shortcut icon\" href=\"" CGI_PREFIX "error-favicon.ico\" type=\"image/x-icon\">"
      "</head>\n"
      "<body>\n"
      "<h1>500 Internal Privoxy Error</h1>\n"
      "<p>Privoxy encountered an error while processing your request:</p>\n"
      "<p><b>Unexpected internal error: ";
   static const char body_suffix[] =
      "</b></p>\n"
      "<p>Please "
      "<a href=\"http://sourceforge.net/tracker/?group_id=11118&amp;atid=111118\">"
      "file a bug report</a>.</p>\n"
      "</body>\n"
      "</html>\n";
   /* Includes room for larger error numbers in the future. */
   const size_t body_size = sizeof(body_prefix) + sizeof(body_suffix) + 5;
   assert(csp);
   assert(rsp);

   /* Reset rsp, if needed */
   freez(rsp->status);
   freez(rsp->head);
   freez(rsp->body);
   rsp->content_length = 0;
   rsp->head_length = 0;
   rsp->is_static = 0;
   rsp->crunch_reason = INTERNAL_ERROR;

   rsp->body = malloc(body_size);
   if (rsp->body == NULL)
   {
      return JB_ERR_MEMORY;
   }

   snprintf(rsp->body, body_size, "%s%d%s", body_prefix, error_to_report, body_suffix);

   rsp->status = strdup(status);
   if (rsp->status == NULL)
   {
      return JB_ERR_MEMORY;
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  cgi_error_bad_param
 *
 * Description :  CGI function that is called if the parameters
 *                (query string) for a CGI were wrong.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *
 * CGI Parameters : none
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err cgi_error_bad_param(const struct client_state *csp,
                           struct http_response *rsp)
{
   struct map *exports;

   assert(csp);
   assert(rsp);

   if (NULL == (exports = default_exports(csp, NULL)))
   {
      return JB_ERR_MEMORY;
   }

   return template_fill_for_cgi(csp, "cgi-error-bad-param", exports, rsp);
}


/*********************************************************************
 *
 * Function    :  cgi_redirect
 *
 * Description :  CGI support function to generate a HTTP redirect
 *                message
 *
 * Parameters  :
 *          1  :  rsp = http_response data structure for output
 *          2  :  target = string with the target URL
 *
 * CGI Parameters : None
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err cgi_redirect (struct http_response * rsp, const char *target)
{
   jb_err err;

   assert(rsp);
   assert(target);

   err = enlist_unique_header(rsp->headers, "Location", target);

   rsp->status = strdup("302 Local Redirect from Privoxy");
   if (rsp->status == NULL)
   {
      return JB_ERR_MEMORY;
   }

   return err;
}


/*********************************************************************
 *
 * Function    :  add_help_link
 *
 * Description :  Produce a copy of the string given as item,
 *                embedded in an HTML link to its corresponding
 *                section (item name in uppercase) in the actions
 *                chapter of the user manual, (whose URL is given in
 *                the config and defaults to our web site).
 *
 *                FIXME: I currently only work for actions, and would
 *                       like to be generalized for other topics.
 *
 * Parameters  :
 *          1  :  item = item (will NOT be free()d.)
 *                       It is assumed to be HTML-safe.
 *          2  :  config = The current configuration.
 *
 * Returns     :  String with item embedded in link, or NULL on
 *                out-of-memory
 *
 *********************************************************************/
char *add_help_link(const char *item,
                    struct configuration_spec *config)
{
   char *result;

   if (!item) return NULL;

   result = strdup("<a href=\"");
   if (!strncmpic(config->usermanual, "file://", 7) ||
       !strncmpic(config->usermanual, "http", 4))
   {
      string_append(&result, config->usermanual);
   }
   else
   {
      string_append(&result, "http://");
      string_append(&result, CGI_SITE_2_HOST);
      string_append(&result, "/user-manual/");
   }
   string_append(&result, ACTIONS_HELP_PREFIX);
   string_join  (&result, string_toupper(item));
   string_append(&result, "\">");
   string_append(&result, item);
   string_append(&result, "</a>");

   return result;
}


/*********************************************************************
 *
 * Function    :  get_http_time
 *
 * Description :  Get the time in a format suitable for use in a
 *                HTTP header - e.g.:
 *                "Sun, 06 Nov 1994 08:49:37 GMT"
 *
 * Parameters  :
 *          1  :  time_offset = Time returned will be current time
 *                              plus this number of seconds.
 *          2  :  buf = Destination for result.
 *          3  :  buffer_size = Size of the buffer above. Must be big
 *                              enough to hold 29 characters plus a
 *                              trailing zero.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void get_http_time(int time_offset, char *buf, size_t buffer_size)
{
   struct tm *t;
   time_t current_time;
#if defined(HAVE_GMTIME_R)
   struct tm dummy;
#endif

   assert(buf);
   assert(buffer_size > (size_t)29);

   time(&current_time);

   current_time += time_offset;

   /* get and save the gmt */
#if HAVE_GMTIME_R
   t = gmtime_r(&current_time, &dummy);
#elif defined(MUTEX_LOCKS_AVAILABLE)
   privoxy_mutex_lock(&gmtime_mutex);
   t = gmtime(&current_time);
   privoxy_mutex_unlock(&gmtime_mutex);
#else
   t = gmtime(&current_time);
#endif

   strftime(buf, buffer_size, "%a, %d %b %Y %H:%M:%S GMT", t);

}

/*********************************************************************
 *
 * Function    :  get_locale_time
 *
 * Description :  Get the time in a date(1)-like format
 *                according to the current locale - e.g.:
 *                "Fri Aug 29 19:37:12 CEST 2008"
 *
 *                XXX: Should we allow the user to change the format?
 *
 * Parameters  :
 *          1  :  buf         = Destination for result.
 *          2  :  buffer_size = Size of the buffer above. Must be big
 *                              enough to hold 29 characters plus a
 *                              trailing zero.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void get_locale_time(char *buf, size_t buffer_size)
{
   struct tm *timeptr;
   time_t current_time;
#if defined(HAVE_LOCALTIME_R)
   struct tm dummy;
#endif

   assert(buf);
   assert(buffer_size > (size_t)29);

   time(&current_time);

#if HAVE_LOCALTIME_R
   timeptr = localtime_r(&current_time, &dummy);
#elif defined(MUTEX_LOCKS_AVAILABLE)
   privoxy_mutex_lock(&localtime_mutex);
   timeptr = localtime(&current_time);
   privoxy_mutex_unlock(&localtime_mutex);
#else
   timeptr = localtime(&current_time);
#endif

   strftime(buf, buffer_size, "%a %b %d %X %Z %Y", timeptr);

}


#ifdef FEATURE_COMPRESSION
/*********************************************************************
 *
 * Function    :  compress_buffer
 *
 * Description :  Compresses the content of a buffer with zlib's deflate
 *                Allocates a new buffer for the result, free'ing it is
 *                up to the caller.
 *
 * Parameters  :
 *          1  :  buffer = buffer whose content should be compressed
 *          2  :  buffer_length = length of the buffer
 *          3  :  compression_level = compression level for compress2()
 *
 * Returns     :  NULL on error, otherwise a pointer to the compressed
 *                content of the input buffer.
 *
 *********************************************************************/
char *compress_buffer(char *buffer, size_t *buffer_length, int compression_level)
{
   char *compressed_buffer;
   uLongf new_length;
   assert(-1 <= compression_level && compression_level <= 9);

   /* Let zlib figure out the maximum length of the compressed data */
   new_length = compressBound((uLongf)*buffer_length);

   compressed_buffer = malloc(new_length);
   if (NULL == compressed_buffer)
   {
      log_error(LOG_LEVEL_FATAL,
         "Out of memory allocation compression buffer.");
   }

   if (Z_OK != compress2((Bytef *)compressed_buffer, &new_length,
         (Bytef *)buffer, *buffer_length, compression_level))
   {
      log_error(LOG_LEVEL_ERROR,
         "compress2() failed. Buffer size: %d, compression level: %d.",
         new_length, compression_level);
      freez(compressed_buffer);
      return NULL;
   }

   log_error(LOG_LEVEL_RE_FILTER,
      "Compressed content from %d to %d bytes. Compression level: %d",
      *buffer_length, new_length, compression_level);

   *buffer_length = (size_t)new_length;

   return compressed_buffer;

}
#endif


/*********************************************************************
 *
 * Function    :  finish_http_response
 *
 * Description :  Fill in the missing headers in an http response,
 *                and flatten the headers to an http head.
 *                For HEAD requests the body is freed once
 *                the Content-Length header is set.
 *
 * Parameters  :
 *          1  :  rsp = pointer to http_response to be processed
 *
 * Returns     :  A http_response, usually the rsp parameter.
 *                On error, free()s rsp and returns cgi_error_memory()
 *
 *********************************************************************/
struct http_response *finish_http_response(const struct client_state *csp, struct http_response *rsp)
{
   char buf[BUFFER_SIZE];
   jb_err err;

   /* Special case - do NOT change this statically allocated response,
    * which is ready for output anyway.
    */
   if (rsp == cgi_error_memory_response)
   {
      return rsp;
   }

   /*
    * Fill in the HTTP Status, using HTTP/1.1
    * unless the client asked for HTTP/1.0.
    */
   snprintf(buf, sizeof(buf), "%s %s",
      strcmpic(csp->http->ver, "HTTP/1.0") ? "HTTP/1.1" : "HTTP/1.0",
      rsp->status ? rsp->status : "200 OK");
   err = enlist_first(rsp->headers, buf);

   /*
    * Set the Content-Length
    */
   if (rsp->content_length == 0)
   {
      rsp->content_length = rsp->body ? strlen(rsp->body) : 0;
   }

#ifdef FEATURE_COMPRESSION
   if (!err && (csp->flags & CSP_FLAG_CLIENT_SUPPORTS_DEFLATE)
      && (rsp->content_length > LOWER_LENGTH_LIMIT_FOR_COMPRESSION))
   {
      char *compressed_content;

      compressed_content = compress_buffer(rsp->body, &rsp->content_length,
         csp->config->compression_level);
      if (NULL != compressed_content)
      {
         freez(rsp->body);
         rsp->body = compressed_content;
         err = enlist_unique_header(rsp->headers, "Content-Encoding", "deflate");
      }
   }
#endif

   if (!err)
   {
      snprintf(buf, sizeof(buf), "Content-Length: %d", (int)rsp->content_length);
      err = enlist(rsp->headers, buf);
   }

   if (0 == strcmpic(csp->http->gpc, "head"))
   {
      /*
       * The client only asked for the head. Dispose
       * the body and log an offensive message.
       *
       * While it may seem to be a bit inefficient to
       * prepare the body if it isn't needed, it's the
       * only way to get the Content-Length right for
       * dynamic pages. We could have disposed the body
       * earlier, but not without duplicating the
       * Content-Length setting code above.
       */
      log_error(LOG_LEVEL_CGI, "Preparing to give head to %s.", csp->ip_addr_str);
      freez(rsp->body);
      rsp->content_length = 0;
   }

   if (strncmpic(rsp->status, "302", 3))
   {
      /*
       * If it's not a redirect without any content,
       * set the Content-Type to text/html if it's
       * not already specified.
       */
      if (!err) err = enlist_unique(rsp->headers, "Content-Type: text/html", 13);
   }

   /*
    * Fill in the rest of the default headers:
    *
    * Date: set to current date/time.
    * Last-Modified: set to date/time the page was last changed.
    * Expires: set to date/time page next needs reloading.
    * Cache-Control: set to "no-cache" if applicable.
    *
    * See http://www.w3.org/Protocols/rfc2068/rfc2068
    */
   if (rsp->is_static)
   {
      /*
       * Set Expires to about 10 min into the future so it'll get reloaded
       * occasionally, e.g. if Privoxy gets upgraded.
       */

      if (!err)
      {
         get_http_time(0, buf, sizeof(buf));
         err = enlist_unique_header(rsp->headers, "Date", buf);
      }

      /* Some date in the past. */
      if (!err) err = enlist_unique_header(rsp->headers, "Last-Modified", "Sat, 17 Jun 2000 12:00:00 GMT");

      if (!err)
      {
         get_http_time(10 * 60, buf, sizeof(buf)); /* 10 * 60sec = 10 minutes */
         err = enlist_unique_header(rsp->headers, "Expires", buf);
      }
   }
   else if (!strncmpic(rsp->status, "302", 3))
   {
      get_http_time(0, buf, sizeof(buf));
      if (!err) err = enlist_unique_header(rsp->headers, "Date", buf);
   }
   else
   {
      /*
       * Setting "Cache-Control" to "no-cache" and  "Expires" to
       * the current time doesn't exactly forbid caching, it just
       * requires the client to revalidate the cached copy.
       *
       * If a temporary problem occurs and the user tries again after
       * getting Privoxy's error message, a compliant browser may set the
       * If-Modified-Since header with the content of the error page's
       * Last-Modified header. More often than not, the document on the server
       * is older than Privoxy's error message, the server would send status code
       * 304 and the browser would display the outdated error message again and again.
       *
       * For documents delivered with status code 403, 404 and 503 we set "Last-Modified"
       * to Tim Berners-Lee's birthday, which predates the age of any page on the web
       * and can be safely used to "revalidate" without getting a status code 304.
       *
       * There is no need to let the useless If-Modified-Since header reach the
       * server, it is therefore stripped by client_if_modified_since in parsers.c.
       */
      if (!err) err = enlist_unique_header(rsp->headers, "Cache-Control", "no-cache");

      get_http_time(0, buf, sizeof(buf));
      if (!err) err = enlist_unique_header(rsp->headers, "Date", buf);
      if (!strncmpic(rsp->status, "403", 3)
       || !strncmpic(rsp->status, "404", 3)
       || !strncmpic(rsp->status, "502", 3)
       || !strncmpic(rsp->status, "503", 3)
       || !strncmpic(rsp->status, "504", 3))
      {
         if (!err) err = enlist_unique_header(rsp->headers, "Last-Modified", "Wed, 08 Jun 1955 12:00:00 GMT");
      }
      else
      {
         if (!err) err = enlist_unique_header(rsp->headers, "Last-Modified", buf);
      }
      if (!err) err = enlist_unique_header(rsp->headers, "Expires", "Sat, 17 Jun 2000 12:00:00 GMT");
      if (!err) err = enlist_unique_header(rsp->headers, "Pragma", "no-cache");
   }

   if (!err && !(csp->flags & CSP_FLAG_CLIENT_CONNECTION_KEEP_ALIVE))
   {
      err = enlist_unique_header(rsp->headers, "Connection", "close");
   }

   /*
    * Write the head
    */
   if (err || (NULL == (rsp->head = list_to_text(rsp->headers))))
   {
      free_http_response(rsp);
      return cgi_error_memory();
   }
   rsp->head_length = strlen(rsp->head);

   return rsp;

}


/*********************************************************************
 *
 * Function    :  alloc_http_response
 *
 * Description :  Allocates a new http_response structure.
 *
 * Parameters  :  N/A
 *
 * Returns     :  pointer to a new http_response, or NULL.
 *
 *********************************************************************/
struct http_response *alloc_http_response(void)
{
   return (struct http_response *) zalloc(sizeof(struct http_response));

}


/*********************************************************************
 *
 * Function    :  free_http_response
 *
 * Description :  Free the memory occupied by an http_response
 *                and its depandant structures.
 *
 * Parameters  :
 *          1  :  rsp = pointer to http_response to be freed
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void free_http_response(struct http_response *rsp)
{
   /*
    * Must special case cgi_error_memory_response, which is never freed.
    */
   if (rsp && (rsp != cgi_error_memory_response))
   {
      freez(rsp->status);
      freez(rsp->head);
      freez(rsp->body);
      destroy_list(rsp->headers);
      free(rsp);
   }

}


/*********************************************************************
 *
 * Function    :  template_load
 *
 * Description :  CGI support function that loads a given HTML
 *                template, ignoring comment lines and following
 *                #include statements up to a depth of 1.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  template_ptr = Destination for pointer to loaded
 *                               template text.
 *          3  :  templatename = name of the HTML template to be used
 *          4  :  recursive = Flag set if this function calls itself
 *                            following an #include statament
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *                JB_ERR_FILE if the template file cannot be read
 *
 *********************************************************************/
jb_err template_load(const struct client_state *csp, char **template_ptr,
                     const char *templatename, int recursive)
{
   jb_err err;
   char *templates_dir_path;
   char *full_path;
   char *file_buffer;
   char *included_module;
   const char *p;
   FILE *fp;
   char buf[BUFFER_SIZE];

   assert(csp);
   assert(template_ptr);
   assert(templatename);

   *template_ptr = NULL;

   /* Validate template name.  Paranoia. */
   for (p = templatename; *p != 0; p++)
   {
      if ( ((*p < 'a') || (*p > 'z'))
        && ((*p < 'A') || (*p > 'Z'))
        && ((*p < '0') || (*p > '9'))
        && (*p != '-')
        && (*p != '.'))
      {
         /* Illegal character */
         return JB_ERR_FILE;
      }
   }

   /*
    * Generate full path using either templdir
    * or confdir/templates as base directory.
    */
   if (NULL != csp->config->templdir)
   {
      templates_dir_path = strdup(csp->config->templdir);
   }
   else
   {
      templates_dir_path = make_path(csp->config->confdir, "templates");
   }

   if (templates_dir_path == NULL)
   {
      log_error(LOG_LEVEL_ERROR, "Out of memory while generating template path for %s.",
         templatename);
      return JB_ERR_MEMORY;
   }

   full_path = make_path(templates_dir_path, templatename);
   free(templates_dir_path);
   if (full_path == NULL)
   {
      log_error(LOG_LEVEL_ERROR, "Out of memory while generating full template path for %s.",
         templatename);
      return JB_ERR_MEMORY;
   }

   /* Allocate buffer */

   file_buffer = strdup("");
   if (file_buffer == NULL)
   {
      log_error(LOG_LEVEL_ERROR, "Not enough free memory to buffer %s.", full_path);
      free(full_path);
      return JB_ERR_MEMORY;
   }

   /* Open template file */

   if (NULL == (fp = fopen(full_path, "r")))
   {
      log_error(LOG_LEVEL_ERROR, "Cannot open template file %s: %E", full_path);
      free(full_path);
      free(file_buffer);
      return JB_ERR_FILE;
   }
   free(full_path);

   /*
    * Read the file, ignoring comments, and honoring #include
    * statements, unless we're already called recursively.
    *
    * XXX: The comment handling could break with lines lengths > sizeof(buf).
    *      This is unlikely in practise.
    */
   while (fgets(buf, sizeof(buf), fp))
   {
      if (!recursive && !strncmp(buf, "#include ", 9))
      {
         if (JB_ERR_OK != (err = template_load(csp, &included_module, chomp(buf + 9), 1)))
         {
            free(file_buffer);
            fclose(fp);
            return err;
         }

         if (string_join(&file_buffer, included_module))
         {
            fclose(fp);
            return JB_ERR_MEMORY;
         }

         continue;
      }

      /* skip lines starting with '#' */
      if (*buf == '#')
      {
         continue;
      }

      if (string_append(&file_buffer, buf))
      {
         fclose(fp);
         return JB_ERR_MEMORY;
      }
   }
   fclose(fp);

   *template_ptr = file_buffer;

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  template_fill
 *
 * Description :  CGI support function that fills in a pre-loaded
 *                HTML template by replacing @name@ with value using
 *                pcrs, for each item in the output map.
 *
 *                Note that a leading '$' character in the export map's
 *                values will be stripped and toggle on backreference
 *                interpretation.
 *
 * Parameters  :
 *          1  :  template_ptr = IN: Template to be filled out.
 *                                   Will be free()d.
 *                               OUT: Filled out template.
 *                                    Caller must free().
 *          2  :  exports = map with fill in symbol -> name pairs
 *
 * Returns     :  JB_ERR_OK on success (and for uncritical errors)
 *                JB_ERR_MEMORY on out-of-memory error
 *
 *********************************************************************/
jb_err template_fill(char **template_ptr, const struct map *exports)
{
   struct map_entry *m;
   pcrs_job *job;
   char buf[BUFFER_SIZE];
   char *tmp_out_buffer;
   char *file_buffer;
   size_t size;
   int error;
   const char *flags;

   assert(template_ptr);
   assert(*template_ptr);
   assert(exports);

   file_buffer = *template_ptr;
   size = strlen(file_buffer) + 1;

   /*
    * Assemble pcrs joblist from exports map
    */
   for (m = exports->first; m != NULL; m = m->next)
   {
      if (*m->name == '$')
      {
         /*
          * First character of name is '$', so remove this flag
          * character and allow backreferences ($1 etc) in the
          * "replace with" text.
          */
         snprintf(buf, sizeof(buf), "%s", m->name + 1);
         flags = "sigU";
      }
      else
      {
         /*
          * Treat the "replace with" text as a literal string -
          * no quoting needed, no backreferences allowed.
          * ("Trivial" ['T'] flag).
          */
         flags = "sigTU";

         /* Enclose name in @@ */
         snprintf(buf, sizeof(buf), "@%s@", m->name);
      }

      log_error(LOG_LEVEL_CGI, "Substituting: s/%s/%s/%s", buf, m->value, flags);

      /* Make and run job. */
      job = pcrs_compile(buf, m->value, flags,  &error);
      if (job == NULL)
      {
         if (error == PCRS_ERR_NOMEM)
         {
            free(file_buffer);
            *template_ptr = NULL;
            return JB_ERR_MEMORY;
         }
         else
         {
            log_error(LOG_LEVEL_ERROR, "Error compiling template fill job %s: %d", m->name, error);
            /* Hope it wasn't important and silently ignore the invalid job */
         }
      }
      else
      {
         error = pcrs_execute(job, file_buffer, size, &tmp_out_buffer, &size);

         pcrs_free_job(job);
         if (NULL == tmp_out_buffer)
         {
            *template_ptr = NULL;
            return JB_ERR_MEMORY;
         }

         if (error < 0)
         {
            /*
             * Substitution failed, keep the original buffer,
             * log the problem and ignore it.
             *
             * The user might see some unresolved @CGI_VARIABLES@,
             * but returning a special CGI error page seems unreasonable
             * and could mask more important error messages.
             */
            free(tmp_out_buffer);
            log_error(LOG_LEVEL_ERROR, "Failed to execute s/%s/%s/%s. %s",
               buf, m->value, flags, pcrs_strerror(error));
         }
         else
         {
            /* Substitution succeeded, use modified buffer. */
            free(file_buffer);
            file_buffer = tmp_out_buffer;
         }
      }
   }

   /*
    * Return
    */
   *template_ptr = file_buffer;
   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  template_fill_for_cgi
 *
 * Description :  CGI support function that loads a HTML template
 *                and fills it in.  Handles file-not-found errors
 *                by sending a HTML error message.  For convenience,
 *                this function also frees the passed "exports" map.
 *
 * Parameters  :
 *          1  :  csp = Client state
 *          2  :  templatename = name of the HTML template to be used
 *          3  :  exports = map with fill in symbol -> name pairs.
 *                          Will be freed by this function.
 *          4  :  rsp = Response structure to fill in.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error
 *
 *********************************************************************/
jb_err template_fill_for_cgi(const struct client_state *csp,
                             const char *templatename,
                             struct map *exports,
                             struct http_response *rsp)
{
   jb_err err;

   assert(csp);
   assert(templatename);
   assert(exports);
   assert(rsp);

   err = template_load(csp, &rsp->body, templatename, 0);
   if (err == JB_ERR_FILE)
   {
      free_map(exports);
      return cgi_error_no_template(csp, rsp, templatename);
   }
   else if (err)
   {
      free_map(exports);
      return err; /* JB_ERR_MEMORY */
   }
   err = template_fill(&rsp->body, exports);
   free_map(exports);
   return err;
}


/*********************************************************************
 *
 * Function    :  default_exports
 *
 * Description :  returns a struct map list that contains exports
 *                which are common to all CGI functions.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  caller = name of CGI who calls us and which should
 *                         be excluded from the generated menu. May be
 *                         NULL.
 * Returns     :  NULL if no memory, else a new map.  Caller frees.
 *
 *********************************************************************/
struct map *default_exports(const struct client_state *csp, const char *caller)
{
   char buf[30];
   jb_err err;
   struct map * exports;
   int local_help_exists = 0;
   char *ip_address = NULL;
   char *port = NULL;
   char *hostname = NULL;

   assert(csp);

   exports = new_map();
   if (exports == NULL)
   {
      return NULL;
   }

   if (csp->config->hostname)
   {
      get_host_information(csp->cfd, &ip_address, &port, NULL);
      hostname = strdup(csp->config->hostname);
   }
   else
   {
      get_host_information(csp->cfd, &ip_address, &port, &hostname);
   }

   err = map(exports, "version", 1, html_encode(VERSION), 0);
   get_locale_time(buf, sizeof(buf));
   if (!err) err = map(exports, "time",          1, html_encode(buf), 0);
   if (!err) err = map(exports, "my-ip-address", 1, html_encode(ip_address ? ip_address : "unknown"), 0);
   freez(ip_address);
   if (!err) err = map(exports, "my-port",       1, html_encode(port ? port : "unkown"), 0);
   freez(port);
   if (!err) err = map(exports, "my-hostname",   1, html_encode(hostname ? hostname : "unknown"), 0);
   freez(hostname);
   if (!err) err = map(exports, "homepage",      1, html_encode(HOME_PAGE_URL), 0);
   if (!err) err = map(exports, "default-cgi",   1, html_encode(CGI_PREFIX), 0);
   if (!err) err = map(exports, "menu",          1, make_menu(caller, csp->config->feature_flags), 0);
   if (!err) err = map(exports, "code-status",   1, CODE_STATUS, 1);
   if (!strncmpic(csp->config->usermanual, "file://", 7) ||
       !strncmpic(csp->config->usermanual, "http", 4))
   {
      /* Manual is located somewhere else, just link to it. */
      if (!err) err = map(exports, "user-manual", 1, html_encode(csp->config->usermanual), 0);
   }
   else
   {
      /* Manual is delivered by Privoxy. */
      if (!err) err = map(exports, "user-manual", 1, html_encode(CGI_PREFIX"user-manual/"), 0);
   }
   if (!err) err = map(exports, "actions-help-prefix", 1, ACTIONS_HELP_PREFIX ,1);
#ifdef FEATURE_TOGGLE
   if (!err) err = map_conditional(exports, "enabled-display", global_toggle_state);
#else
   if (!err) err = map_block_killer(exports, "can-toggle");
#endif

   if(!strcmp(CODE_STATUS, "stable"))
   {
      if (!err) err = map_block_killer(exports, "unstable");
   }

   if (csp->config->admin_address != NULL)
   {
      if (!err) err = map(exports, "admin-address", 1, html_encode(csp->config->admin_address), 0);
      local_help_exists = 1;
   }
   else
   {
      if (!err) err = map_block_killer(exports, "have-adminaddr-info");
   }

   if (csp->config->proxy_info_url != NULL)
   {
      if (!err) err = map(exports, "proxy-info-url", 1, html_encode(csp->config->proxy_info_url), 0);
      local_help_exists = 1;
   }
   else
   {
      if (!err) err = map_block_killer(exports, "have-proxy-info");
   }

   if (local_help_exists == 0)
   {
      if (!err) err = map_block_killer(exports, "have-help-info");
   }

   if (err)
   {
      free_map(exports);
      return NULL;
   }

   return exports;
}


/*********************************************************************
 *
 * Function    :  map_block_killer
 *
 * Description :  Convenience function.
 *                Adds a "killer" for the conditional HTML-template
 *                block <name>, i.e. a substitution of the regex
 *                "if-<name>-start.*if-<name>-end" to the given
 *                export list.
 *
 * Parameters  :
 *          1  :  exports = map to extend
 *          2  :  name = name of conditional block
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err map_block_killer(struct map *exports, const char *name)
{
   char buf[1000]; /* Will do, since the names are hardwired */

   assert(exports);
   assert(name);
   assert(strlen(name) < (size_t)490);

   snprintf(buf, sizeof(buf), "if-%s-start.*if-%s-end", name, name);
   return map(exports, buf, 1, "", 1);
}


/*********************************************************************
 *
 * Function    :  map_block_keep
 *
 * Description :  Convenience function.  Removes the markers used
 *                by map-block-killer, to save a few bytes.
 *                i.e. removes "@if-<name>-start@" and "@if-<name>-end@"
 *
 * Parameters  :
 *          1  :  exports = map to extend
 *          2  :  name = name of conditional block
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err map_block_keep(struct map *exports, const char *name)
{
   jb_err err;
   char buf[500]; /* Will do, since the names are hardwired */

   assert(exports);
   assert(name);
   assert(strlen(name) < (size_t)490);

   snprintf(buf, sizeof(buf), "if-%s-start", name);
   err = map(exports, buf, 1, "", 1);

   if (err)
   {
      return err;
   }

   snprintf(buf, sizeof(buf), "if-%s-end", name);
   return map(exports, buf, 1, "", 1);
}


/*********************************************************************
 *
 * Function    :  map_conditional
 *
 * Description :  Convenience function.
 *                Adds an "if-then-else" for the conditional HTML-template
 *                block <name>, i.e. a substitution of the form:
 *                @if-<name>-then@
 *                   True text
 *                @else-not-<name>@
 *                   False text
 *                @endif-<name>@
 *
 *                The control structure and one of the alternatives
 *                will be hidden.
 *
 * Parameters  :
 *          1  :  exports = map to extend
 *          2  :  name = name of conditional block
 *          3  :  choose_first = nonzero for first, zero for second.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err map_conditional(struct map *exports, const char *name, int choose_first)
{
   char buf[1000]; /* Will do, since the names are hardwired */
   jb_err err;

   assert(exports);
   assert(name);
   assert(strlen(name) < (size_t)480);

   snprintf(buf, sizeof(buf), (choose_first
      ? "else-not-%s@.*@endif-%s"
      : "if-%s-then@.*@else-not-%s"),
      name, name);

   err = map(exports, buf, 1, "", 1);
   if (err)
   {
      return err;
   }

   snprintf(buf, sizeof(buf), (choose_first ? "if-%s-then" : "endif-%s"), name);
   return map(exports, buf, 1, "", 1);
}


/*********************************************************************
 *
 * Function    :  make_menu
 *
 * Description :  Returns an HTML-formatted menu of the available
 *                unhidden CGIs, excluding the one given in <self>
 *                and the toggle CGI if toggling is disabled.
 *
 * Parameters  :
 *          1  :  self = name of CGI to leave out, can be NULL for
 *                complete listing.
 *          2  :  feature_flags = feature bitmap from csp->config
 *
 *
 * Returns     :  menu string, or NULL on out-of-memory error.
 *
 *********************************************************************/
char *make_menu(const char *self, const unsigned feature_flags)
{
   const struct cgi_dispatcher *d;
   char *result = strdup("");

   if (self == NULL)
   {
      self = "NO-SUCH-CGI!";
   }

   /* List available unhidden CGI's and export as "other-cgis" */
   for (d = cgi_dispatchers; d->name; d++)
   {

#ifdef FEATURE_TOGGLE
      if (!(feature_flags & RUNTIME_FEATURE_CGI_TOGGLE) && !strcmp(d->name, "toggle"))
      {
         /*
          * Suppress the toggle link if remote toggling is disabled.
          */
         continue;
      }
#endif /* def FEATURE_TOGGLE */

      if (d->description && strcmp(d->name, self))
      {
         char *html_encoded_prefix;

         /*
          * Line breaks would be great, but break
          * the "blocked" template's JavaScript.
          */
         string_append(&result, "<li><a href=\"");
         html_encoded_prefix = html_encode(CGI_PREFIX);
         if (html_encoded_prefix == NULL)
         {
            return NULL;
         }
         else
         {
            string_append(&result, html_encoded_prefix);
            free(html_encoded_prefix);
         }
         string_append(&result, d->name);
         string_append(&result, "\">");
         string_append(&result, d->description);
         string_append(&result, "</a></li>");
      }
   }

   return result;
}


/*********************************************************************
 *
 * Function    :  dump_map
 *
 * Description :  HTML-dump a map for debugging (as table)
 *
 * Parameters  :
 *          1  :  the_map = map to dump
 *
 * Returns     :  string with HTML
 *
 *********************************************************************/
char *dump_map(const struct map *the_map)
{
   struct map_entry *cur_entry;
   char *ret = strdup("");

   string_append(&ret, "<table>\n");

   for (cur_entry = the_map->first;
        (cur_entry != NULL) && (ret != NULL);
        cur_entry = cur_entry->next)
   {
      string_append(&ret, "<tr><td><b>");
      string_join  (&ret, html_encode(cur_entry->name));
      string_append(&ret, "</b></td><td>");
      string_join  (&ret, html_encode(cur_entry->value));
      string_append(&ret, "</td></tr>\n");
   }

   string_append(&ret, "</table>\n");
   return ret;
}


/*
  Local Variables:
  tab-width: 3
  end:
*/
