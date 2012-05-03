#ifndef FILTERS_H_INCLUDED
#define FILTERS_H_INCLUDED
#define FILTERS_H_VERSION "$Id: filters.h,v 1.36 2008/05/21 15:35:08 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/filters.h,v $
 *
 * Purpose     :  Declares functions to parse/crunch headers and pages.
 *                Functions declared include:
 *                   `acl_addr', `add_stats', `block_acl', `block_imageurl',
 *                   `block_url', `url_actions', `filter_popups', `forward_url'
 *                   `ij_untrusted_url', `intercept_url', `re_process_buffer',
 *                   `show_proxy_args', and `trust_url'
 *
 * Copyright   :  Written by and Copyright (C) 2001, 2004 the SourceForge
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
 *    $Log: filters.h,v $
 *    Revision 1.36  2008/05/21 15:35:08  fabiankeil
 *    - Mark csp as immutable for block_acl().
 *    - Remove an obsolete complaint about filter_popups().
 *
 *    Revision 1.35  2008/05/03 16:40:45  fabiankeil
 *    Change content_filters_enabled()'s parameter from
 *    csp->action to action so it can be also used in the
 *    CGI code. Don't bother checking if there are filters
 *    loaded, as that's somewhat besides the point.
 *
 *    Revision 1.34  2008/03/02 12:25:25  fabiankeil
 *    Also use shiny new connect_port_is_forbidden() in jcc.c.
 *
 *    Revision 1.33  2008/02/23 16:57:12  fabiankeil
 *    Rename url_actions() to get_url_actions() and let it
 *    use the standard parameter ordering.
 *
 *    Revision 1.32  2008/02/23 16:33:43  fabiankeil
 *    Let forward_url() use the standard parameter ordering
 *    and mark its second parameter immutable.
 *
 *    Revision 1.31  2007/10/19 16:53:28  fabiankeil
 *    Add helper function to check if any content filters are enabled.
 *
 *    Revision 1.30  2007/09/29 10:21:16  fabiankeil
 *    - Move get_filter_function() from jcc.c to filters.c
 *      so the filter functions can be static.
 *    - Don't bother filtering body-less responses.
 *
 *    Revision 1.29  2007/09/28 16:38:55  fabiankeil
 *    - Execute content filters through execute_content_filter().
 *    - Add prepare_for_filtering() so filter functions don't have to
 *      care about de-chunking and decompression. As a side effect this enables
 *      decompression for gif_deanimate_response() and jpeg_inspect_response().
 *    - Change remove_chunked_transfer_coding()'s return type to jb_err.
 *      Some clowns feel like chunking empty responses in which case
 *      (size == 0) is valid but previously would be interpreted as error.
 *
 *    Revision 1.28  2007/09/02 15:31:20  fabiankeil
 *    Move match_portlist() from filter.c to urlmatch.c.
 *    It's used for url matching, not for filtering.
 *
 *    Revision 1.27  2007/04/30 15:02:18  fabiankeil
 *    Introduce dynamic pcrs jobs that can resolve variables.
 *
 *    Revision 1.26  2007/03/13 11:28:43  fabiankeil
 *    - Fix port handling in acl_addr() and use a temporary acl spec
 *      copy so error messages don't contain a truncated version.
 *    - Log size of iob before and after decompression.
 *
 *    Revision 1.25  2007/01/12 15:36:44  fabiankeil
 *    Mark *csp as immutable for is_untrusted_url()
 *    and is_imageurl(). Closes FR 1237736.
 *
 *    Revision 1.24  2006/12/29 18:30:46  fabiankeil
 *    Fixed gcc43 conversion warnings,
 *    changed sprintf calls to snprintf.
 *
 *    Revision 1.23  2006/11/28 15:19:43  fabiankeil
 *    Implemented +redirect{s@foo@bar@} to generate
 *    a redirect based on a rewritten version of the
 *    original URL.
 *
 *    Revision 1.22  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.20.2.2  2004/10/03 12:53:32  david__schmidt
 *    Add the ability to check jpeg images for invalid
 *    lengths of comment blocks.  Defensive strategy
 *    against the exploit:
 *       Microsoft Security Bulletin MS04-028
 *       Buffer Overrun in JPEG Processing (GDI+) Could
 *       Allow Code Execution (833987)
 *    Enabled with +inspect-jpegs in actions files.
 *
 *    Revision 1.20.2.1  2002/09/25 14:51:51  oes
 *    Added basic support for OPTIONS and TRACE HTTP methods:
 *    New function direct_response which handles OPTIONS and
 *    TRACE requests whose Max-Forwards header field is zero.
 *
 *    Revision 1.20  2002/04/02 14:56:16  oes
 *    Bugfix: is_untrusted_url() and trust_url() now depend on FEATURE_TRUST, not FEATURE_COOKIE_JAR
 *
 *    Revision 1.19  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.18  2002/03/25 22:12:45  oes
 *    Added fix for undefined INADDR_NONE on Solaris by Bart Schelstraete
 *
 *    Revision 1.17  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.16  2002/01/17 21:01:02  jongfoster
 *    Moving all our URL and URL pattern parsing code to urlmatch.c.
 *
 *    Revision 1.15  2001/10/10 16:44:16  oes
 *    Added match_portlist function
 *
 *    Revision 1.14  2001/10/07 15:41:40  oes
 *    Added prototype for remove_chunked_transfer_coding
 *
 *    Revision 1.13  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.12  2001/07/29 19:01:11  jongfoster
 *    Changed _FILENAME_H to FILENAME_H_INCLUDED.
 *    Added forward declarations for needed structures.
 *
 *    Revision 1.11  2001/07/13 14:00:18  oes
 *     - Introduced gif_deanimate_response
 *     - Renamed re_process_buffer to pcrs_filter_response
 *     - Removed all #ifdef PCRS
 *
 *    Revision 1.10  2001/06/29 13:29:01  oes
 *    Cleaned up and updated to reflect the changesin
 *    filters.c
 *
 *    Revision 1.9  2001/06/07 23:10:53  jongfoster
 *    Replacing struct gateway with struct forward_spec
 *
 *    Revision 1.8  2001/06/03 19:12:00  oes
 *    extracted-CGI relevant stuff
 *
 *    Revision 1.7  2001/05/31 21:21:30  jongfoster
 *    Permissionsfile / actions file changes:
 *    - Changed "permission" to "action" throughout
 *    - changes to file format to allow string parameters
 *    - Moved helper functions to actions.c
 *
 *    Revision 1.6  2001/05/29 09:50:24  jongfoster
 *    Unified blocklist/imagelist/permissionslist.
 *    File format is still under discussion, but the internal changes
 *    are (mostly) done.
 *
 *    Also modified interceptor behaviour:
 *    - We now intercept all URLs beginning with one of the following
 *      prefixes (and *only* these prefixes):
 *        * http://i.j.b/
 *        * http://ijbswa.sf.net/config/
 *        * http://ijbswa.sourceforge.net/config/
 *    - New interceptors "home page" - go to http://i.j.b/ to see it.
 *    - Internal changes so that intercepted and fast redirect pages
 *      are not replaced with an image.
 *    - Interceptors now have the option to send a binary page direct
 *      to the client. (i.e. ijb-send-banner uses this)
 *    - Implemented show-url-info interceptor.  (Which is why I needed
 *      the above interceptors changes - a typical URL is
 *      "http://i.j.b/show-url-info?url=www.somesite.com/banner.gif".
 *      The previous mechanism would not have intercepted that, and
 *      if it had been intercepted then it then it would have replaced
 *      it with an image.)
 *
 *    Revision 1.5  2001/05/27 22:17:04  oes
 *
 *    - re_process_buffer no longer writes the modified buffer
 *      to the client, which was very ugly. It now returns the
 *      buffer, which it is then written by chat.
 *
 *    - content_length now adjusts the Content-Length: header
 *      for modified documents rather than crunch()ing it.
 *      (Length info in csp->content_length, which is 0 for
 *      unmodified documents)
 *
 *    - For this to work, sed() is called twice when filtering.
 *
 *    Revision 1.4  2001/05/26 15:26:15  jongfoster
 *    ACL feature now provides more security by immediately dropping
 *    connections from untrusted hosts.
 *
 *    Revision 1.3  2001/05/22 18:46:04  oes
 *
 *    - Enabled filtering banners by size rather than URL
 *      by adding patterns that replace all standard banner
 *      sizes with the "Junkbuster" gif to the re_filterfile
 *
 *    - Enabled filtering WebBugs by providing a pattern
 *      which kills all 1x1 images
 *
 *    - Added support for PCRE_UNGREEDY behaviour to pcrs,
 *      which is selected by the (nonstandard and therefore
 *      capital) letter 'U' in the option string.
 *      It causes the quantifiers to be ungreedy by default.
 *      Appending a ? turns back to greedy (!).
 *
 *    - Added a new interceptor ijb-send-banner, which
 *      sends back the "Junkbuster" gif. Without imagelist or
 *      MSIE detection support, or if tinygif = 1, or the
 *      URL isn't recognized as an imageurl, a lame HTML
 *      explanation is sent instead.
 *
 *    - Added new feature, which permits blocking remote
 *      script redirects and firing back a local redirect
 *      to the browser.
 *      The feature is conditionally compiled, i.e. it
 *      can be disabled with --disable-fast-redirects,
 *      plus it must be activated by a "fast-redirects"
 *      line in the config file, has its own log level
 *      and of course wants to be displayed by show-proxy-args
 *      Note: Boy, all the #ifdefs in 1001 locations and
 *      all the fumbling with configure.in and acconfig.h
 *      were *way* more work than the feature itself :-(
 *
 *    - Because a generic redirect template was needed for
 *      this, tinygif = 3 now uses the same.
 *
 *    - Moved GIFs, and other static HTTP response templates
 *      to project.h
 *
 *    - Some minor fixes
 *
 *    - Removed some >400 CRs again (Jon, you really worked
 *      a lot! ;-)
 *
 *    Revision 1.2  2001/05/20 01:21:20  jongfoster
 *    Version 2.9.4 checkin.
 *    - Merged popupfile and cookiefile, and added control over PCRS
 *      filtering, in new "permissionsfile".
 *    - Implemented LOG_LEVEL_FATAL, so that if there is a configuration
 *      file error you now get a message box (in the Win32 GUI) rather
 *      than the program exiting with no explanation.
 *    - Made killpopup use the PCRS MIME-type checking and HTTP-header
 *      skipping.
 *    - Removed tabs from "config"
 *    - Moved duplicated url parsing code in "loaders.c" to a new funcition.
 *    - Bumped up version number.
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:52  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "project.h"

#ifdef __cplusplus
extern "C" {
#endif


struct access_control_addr;
struct client_state;
struct http_request;
struct http_response;
struct current_action_spec;
struct url_actions;
struct url_spec;


/*
 * ACL checking
 */
#ifdef FEATURE_ACL
extern int block_acl(const struct access_control_addr *dst, const struct client_state *csp);
extern int acl_addr(const char *aspec, struct access_control_addr *aca);
#endif /* def FEATURE_ACL */

/*
 * Interceptors
 */
extern struct http_response *block_url(struct client_state *csp);
extern struct http_response *redirect_url(struct client_state *csp);
#ifdef FEATURE_TRUST
extern struct http_response *trust_url(struct client_state *csp);
#endif /* def FEATURE_TRUST */

/*
 * Request inspectors
 */
#ifdef FEATURE_TRUST
extern int is_untrusted_url(const struct client_state *csp);
#endif /* def FEATURE_TRUST */
#ifdef FEATURE_IMAGE_BLOCKING
extern int is_imageurl(const struct client_state *csp);
#endif /* def FEATURE_IMAGE_BLOCKING */
extern int connect_port_is_forbidden(const struct client_state *csp);

/*
 * Determining applicable actions
 */
extern void get_url_actions(struct client_state *csp,
                            struct http_request *http);
extern void apply_url_actions(struct current_action_spec *action, 
                              struct http_request *http, 
                              struct url_actions *b);
/*
 * Determining parent proxies
 */
extern const struct forward_spec *forward_url(struct client_state *csp,
                                              const struct http_request *http);

/*
 * Content modification
 */

typedef char *(*filter_function_ptr)();
extern char *execute_content_filter(struct client_state *csp, filter_function_ptr content_filter);

extern filter_function_ptr get_filter_function(struct client_state *csp);
extern char *execute_single_pcrs_command(char *subject, const char *pcrs_command, int *hits);
extern char *rewrite_url(char *old_url, const char *pcrs_command);
extern char *get_last_url(char *subject, const char *redirect_mode);

extern pcrs_job *compile_dynamic_pcrs_job_list(const struct client_state *csp, const struct re_filterfile_spec *b);

extern int content_filters_enabled(const struct current_action_spec *action);

/*
 * Handling Max-Forwards:
 */
extern struct http_response *direct_response(struct client_state *csp);


/*
 * Solaris fix:
 */
#ifndef INADDR_NONE
#define INADDR_NONE -1
#endif     

/* 
 * Revision control strings from this header and associated .c file
 */
extern const char filters_rcs[];
extern const char filters_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef FILTERS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
