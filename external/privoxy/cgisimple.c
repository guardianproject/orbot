const char cgisimple_rcs[] = "$Id: cgisimple.c,v 1.91 2009/03/08 14:19:23 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/cgisimple.c,v $
 *
 * Purpose     :  Simple CGIs to get information about Privoxy's
 *                status.
 *                
 *                Functions declared include:
 * 
 *
 * Copyright   :  Written by and Copyright (C) 2001-2008 the SourceForge
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
 *    $Log: cgisimple.c,v $
 *    Revision 1.91  2009/03/08 14:19:23  fabiankeil
 *    Fix justified (but harmless) compiler warnings
 *    on platforms where sizeof(int) < sizeof(long).
 *
 *    Revision 1.90  2009/03/01 18:43:09  fabiankeil
 *    Fix cparser warnings.
 *
 *    Revision 1.89  2008/10/11 11:31:14  fabiankeil
 *    Add FEATURE_CONNECTION_KEEP_ALIVE to the list
 *    of conditional defines on the show-status page.
 *
 *    Revision 1.88  2008/08/30 12:03:07  fabiankeil
 *    Remove FEATURE_COOKIE_JAR.
 *
 *    Revision 1.87  2008/08/29 15:59:22  fabiankeil
 *    Fix two comments.
 *
 *    Revision 1.86  2008/06/28 14:19:05  fabiankeil
 *    Protocol detection is done case-insensitive, fix assertion
 *    to do the same. Yay for Privoxy-Regression-Test and zzuf.
 *
 *    Revision 1.85  2008/05/26 17:30:55  fabiankeil
 *    Provide an OpenSearch Description to access the
 *    show-url-info page through "search engine plugins".
 *
 *    Revision 1.84  2008/05/26 16:16:55  fabiankeil
 *    Spell error correctly.
 *
 *    Revision 1.83  2008/05/12 14:51:30  fabiankeil
 *    Don't complain about an invalid URL if show-url-info is requested
 *    without parameters. Regression introduced in 1.81 by yours truly.
 *
 *    Revision 1.82  2008/05/10 20:01:47  fabiankeil
 *    Fix an assertion that could erroneously
 *    trigger in case of memory shortage.
 *
 *    Revision 1.81  2008/05/05 09:54:39  fabiankeil
 *    In cgi_show_url_info(), make sure ftp URLs are
 *    declared invalid. Also simplify the code that adds
 *    "http://" if no protocol has been specified.
 *
 *    Revision 1.80  2008/05/04 16:18:32  fabiankeil
 *    Provide parse_http_url() with a third parameter to specify
 *    whether or not URLs without protocol are acceptable.
 *
 *    Revision 1.79  2008/05/04 13:30:56  fabiankeil
 *    Streamline parse_http_url()'s prototype.
 *
 *    Revision 1.78  2008/05/03 16:50:11  fabiankeil
 *    Leverage content_filters_enabled() in cgi_show_url_info().
 *
 *    Revision 1.77  2008/05/02 09:47:48  fabiankeil
 *    In cgi_show_url_info, pass an initialized http structure
 *    to parse_http_url() as that will be required soonish and
 *    assert that https URLs are recognized correctly.
 *
 *    Revision 1.76  2008/04/28 09:13:30  fabiankeil
 *    In load_file(), remember the error reason and fclose()
 *    and return later on instead of right away.
 *
 *    Revision 1.75  2008/04/27 13:52:52  fabiankeil
 *    Move CGI file loading code into load_file() and
 *    add checks for unexpected errors.
 *
 *    Revision 1.74  2008/04/26 15:50:56  fabiankeil
 *    Fix macro name in cgi_show_file() error path.
 *
 *    Revision 1.73  2008/04/26 12:21:55  fabiankeil
 *    Forget about JB_ERR_PARSE. JB_ERR_CGI_PARAMS to the rescue.
 *
 *    Revision 1.72  2008/04/26 10:34:15  fabiankeil
 *    If zlib support is unavailable and there are content filters active
 *    but the prevent-compression action is disabled, include a warning
 *    on the show-url-info page that compression might prevent filtering.
 *
 *    Revision 1.71  2008/04/25 13:33:56  fabiankeil
 *    - Factor cgi_show_file() out of cgi_show_status().
 *    - Adjust cgi_show_status()'s parameter description to match reality.
 *
 *    Revision 1.70  2008/04/24 16:12:38  fabiankeil
 *    In cgi_show_status(), load the requested file at once.
 *    Using string_join() for every line really doesn't scale.
 *
 *    Revision 1.69  2008/04/17 14:40:48  fabiankeil
 *    Provide get_http_time() with the buffer size so it doesn't
 *    have to blindly assume that the buffer is big enough.
 *
 *    Revision 1.68  2008/03/27 18:27:21  fabiankeil
 *    Remove kill-popups action.
 *
 *    Revision 1.67  2008/03/27 17:00:05  fabiankeil
 *    Turn the favicon blobs into locals.
 *
 *    Revision 1.66  2008/02/23 16:57:12  fabiankeil
 *    Rename url_actions() to get_url_actions() and let it
 *    use the standard parameter ordering.
 *
 *    Revision 1.65  2008/02/23 16:33:43  fabiankeil
 *    Let forward_url() use the standard parameter ordering
 *    and mark its second parameter immutable.
 *
 *    Revision 1.64  2008/02/03 13:56:07  fabiankeil
 *    Add SOCKS5 support for show-url-info CGI page.
 *
 *    Revision 1.63  2008/02/01 06:04:31  fabiankeil
 *    If edit buttons on the show-url-info CGI page are hidden, explain why.
 *
 *    Revision 1.62  2008/02/01 05:52:40  fabiankeil
 *    Hide edit buttons on the show-url-info CGI page if enable-edit-action
 *    is disabled. Patch by Lee with additional white space adjustments.
 *
 *    Revision 1.61  2008/01/26 11:13:25  fabiankeil
 *    If enable-edit-actions is disabled, hide the edit buttons and explain why.
 *
 *    Revision 1.60  2007/10/27 13:12:13  fabiankeil
 *    Finish 1.49 and check write access before
 *    showing edit buttons on show-url-info page.
 *
 *    Revision 1.59  2007/10/19 16:42:36  fabiankeil
 *    Plug memory leak I introduced five months ago.
 *    Yay Valgrind and Privoxy-Regression-Test.
 *
 *    Revision 1.58  2007/07/21 12:19:50  fabiankeil
 *    If show-url-info is called with an URL that Privoxy
 *    would reject as invalid, don't show unresolved forwarding
 *    variables, "final matches" or claim the site's secure.
 *
 *    Revision 1.57  2007/06/01 16:53:05  fabiankeil
 *    Adjust cgi_show_url_info() to show what forward-override{}
 *    would do with the requested URL (instead of showing how the
 *    request for the CGI page would be forwarded if it wasn't a
 *    CGI request).
 *
 *    Revision 1.56  2007/05/21 10:50:35  fabiankeil
 *    - Use strlcpy() instead of strcpy().
 *    - Stop treating actions files special. Expect a complete file name
 *      (with or without path) like it's done for the rest of the files.
 *      Closes FR#588084.
 *    - Don't rerun sed() in cgi_show_request().
 *
 *    Revision 1.55  2007/04/13 13:36:46  fabiankeil
 *    Reference action files in CGI URLs by id instead
 *    of using the first part of the file name.
 *    Fixes BR 1694250 and BR 1590556.
 *
 *    Revision 1.54  2007/04/09 18:11:35  fabiankeil
 *    Don't mistake VC++'s _snprintf() for a snprintf() replacement.
 *
 *    Revision 1.53  2007/04/08 13:21:04  fabiankeil
 *    Reference action files in CGI URLs by id instead
 *    of using the first part of the file name.
 *    Fixes BR 1694250 and BR 1590556.
 *
 *    Revision 1.52  2007/02/13 15:10:26  fabiankeil
 *    Apparently fopen()ing in "binary" mode doesn't require
 *    #ifdefs, it's already done without them in cgiedit.c.
 *
 *    Revision 1.51  2007/02/10 16:55:22  fabiankeil
 *    - Show forwarding settings on the show-url-info page
 *    - Fix some HTML syntax errors.
 *
 *    Revision 1.50  2007/01/23 15:51:17  fabiankeil
 *    Add favicon delivery functions.
 *
 *    Revision 1.49  2007/01/20 16:29:38  fabiankeil
 *    Suppress edit buttons for action files if Privoxy has
 *    no write access. Suggested by Roland in PR 1564026.
 *
 *    Revision 1.48  2007/01/20 15:31:31  fabiankeil
 *    Display warning if show-url-info CGI page
 *    is used while Privoxy is toggled off.
 *
 *    Revision 1.47  2007/01/12 15:07:10  fabiankeil
 *    Use zalloc in cgi_send_user_manual.
 *
 *    Revision 1.46  2007/01/02 12:49:46  fabiankeil
 *    Add FEATURE_ZLIB to the list of conditional
 *    defines at the show-status page.
 *
 *    Revision 1.45  2006/12/28 18:16:41  fabiankeil
 *    Fixed gcc43 compiler warnings, zero out cgi_send_user_manual's
 *    body memory before using it, replaced sprintf calls with snprintf.
 *
 *    Revision 1.44  2006/12/22 14:19:27  fabiankeil
 *    Removed checks whether or not AF_FILES have
 *    data structures associated with them in cgi_show_status.
 *    It doesn't matter as we're only interested in the file names.
 *
 *    For the action files the checks were always true,
 *    but they prevented empty filter files from being
 *    listed. Fixes parts of BR 1619208.
 *
 *    Revision 1.43  2006/12/17 17:57:56  fabiankeil
 *    - Added FEATURE_GRACEFUL_TERMINATION to the
 *      "conditional #defines" section
 *    - Escaped ampersands in generated HTML.
 *    - Renamed re-filter-filename to re-filter-filenames
 *
 *    Revision 1.42  2006/11/21 15:43:12  fabiankeil
 *    Add special treatment for WIN32 to make sure
 *    cgi_send_user_manual opens the files in binary mode.
 *    Fixes BR 1600411 and unbreaks image delivery.
 *
 *    Remove outdated comment.
 *
 *    Revision 1.41  2006/10/09 19:18:28  roro
 *    Redirect http://p.p/user-manual (without trailing slash) to
 *    http://p.p/user-manual/ (with trailing slash), otherwise links will be broken.
 *
 *    Revision 1.40  2006/09/09 13:05:33  fabiankeil
 *    Modified cgi_send_user_manual to serve binary
 *    content without destroying it first. Should also be
 *    faster now. Added ".jpg" check for Content-Type guessing.
 *
 *    Revision 1.39  2006/09/08 09:49:23  fabiankeil
 *    Deliver documents in the user-manual directory
 *    with "Content-Type text/css" if their filename
 *    ends with ".css".
 *
 *    Revision 1.38  2006/09/06 18:45:03  fabiankeil
 *    Incorporate modified version of Roland Rosenfeld's patch to
 *    optionally access the user-manual via Privoxy. Closes patch 679075.
 *
 *    Formatting changed to Privoxy style, added call to
 *    cgi_error_no_template if the requested file doesn't
 *    exist and modified check whether or not Privoxy itself
 *    should serve the manual. Should work cross-platform now.
 *
 *    Revision 1.37  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.35.2.7  2006/01/29 23:10:56  david__schmidt
 *    Multiple filter file support
 *
 *    Revision 1.35.2.6  2005/07/04 03:13:43  david__schmidt
 *    Undo some damaging memory leak patches
 *
 *    Revision 1.35.2.5  2005/05/07 21:50:55  david__schmidt
 *    A few memory leaks plugged (mostly on error paths)
 *
 *    Revision 1.35.2.4  2005/04/04 02:21:24  david__schmidt
 *    Another instance of:
 *    Don't show "Edit" buttons #ifndef FEATURE_CGI_EDIT_ACTIONS
 *    Thanks to Magnus Holmgren for the patch
 *
 *    Revision 1.35.2.3  2003/12/17 16:34:15  oes
 *     - Prevent line wrap beween "View/Edit" link buttons on status page
 *     - Some (mostly irrelevant) fixes for Out-of-mem-case handling
 *
 *    Revision 1.35.2.2  2003/04/03 13:48:28  oes
 *    Don't show "Edit" buttons #ifndef FEATURE_CGI_EDIT_ACTIONS
 *
 *    Revision 1.35.2.1  2002/07/04 15:02:38  oes
 *    Added ability to send redirects to send-banner CGI, so that it can completely mimic the image blocking action if called with type=auto
 *
 *    Revision 1.35.2.1  2002/07/01 17:32:04  morcego
 *    Applying patch from Andreas as provided by Hal on the list.
 *    Message-ID: <20020701121218.V1606@feenix.burgiss.net>
 *
 *    Revision 1.35  2002/05/12 21:44:44  jongfoster
 *    Adding amiga.[ch] revision information, if on an amiga.
 *
 *    Revision 1.34  2002/04/30 12:06:12  oes
 *    Deleted unused code from default_cgi
 *
 *    Revision 1.33  2002/04/30 11:14:52  oes
 *    Made csp the first parameter in *action_to_html
 *
 *    Revision 1.32  2002/04/26 18:29:13  jongfoster
 *    Fixing this Visual C++ warning:
 *    cgisimple.c(775) : warning C4018: '<' : signed/unsigned mismatch
 *
 *    Revision 1.31  2002/04/26 12:54:36  oes
 *     - Kill obsolete REDIRECT_URL code
 *     - Error handling fixes
 *     - Style sheet related HTML snipplet changes
 *     - cgi_show_url_info:
 *       - Matches now in table, actions on single lines,
 *         linked to help
 *       - standard.action suppressed
 *       - Buttons to View and Edit AFs
 *
 *    Revision 1.30  2002/04/24 02:18:08  oes
 *     - show-status is now the starting point for editing
 *       the actions files, generate list of all AFs with buttons
 *       for viewing and editing, new look for file list (Jon:
 *       buttons now aligned ;-P ), view mode now supports multiple
 *       AFs, name changes, no view links for unspecified files,
 *       no edit link for standard.action.
 *
 *     - Jon's multiple AF patch: cgi_show_url_info now uses all
 *       AFs and marks the output accordingly
 *
 *    Revision 1.29  2002/04/10 13:38:35  oes
 *    load_template signature changed
 *
 *    Revision 1.28  2002/04/07 15:42:12  jongfoster
 *    Fixing send-banner?type=auto when the image-blocker is
 *    a redirect to send-banner
 *
 *    Revision 1.27  2002/04/05 15:50:48  oes
 *    added send-stylesheet CGI
 *
 *    Revision 1.26  2002/04/04 00:36:36  gliptak
 *    always use pcre for matching
 *
 *    Revision 1.25  2002/04/03 22:28:03  gliptak
 *    Removed references to gnu_regex
 *
 *    Revision 1.24  2002/04/02 16:12:47  oes
 *    Fix: moving misplaced lines into #ifdef FEATURE_FORCE
 *
 *    Revision 1.23  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.22  2002/03/24 16:18:15  jongfoster
 *    Removing old logo
 *
 *    Revision 1.21  2002/03/24 15:23:33  jongfoster
 *    Name changes
 *
 *    Revision 1.20  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.19  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.18  2002/03/12 01:44:49  oes
 *    Changed default for "blocked" image from jb logo to checkboard pattern
 *
 *    Revision 1.17  2002/03/08 16:43:18  oes
 *    Added choice beween GIF and PNG built-in images
 *
 *    Revision 1.16  2002/03/07 03:48:38  oes
 *     - Changed built-in images from GIF to PNG
 *       (with regard to Unisys patent issue)
 *     - Added a 4x4 pattern PNG which is less intrusive
 *       than the logo but also clearly marks the deleted banners
 *
 *    Revision 1.15  2002/03/06 22:54:35  jongfoster
 *    Automated function-comment nitpicking.
 *
 *    Revision 1.14  2002/03/02 04:14:50  david__schmidt
 *    Clean up a little CRLF unpleasantness that suddenly appeared
 *
 *    Revision 1.13  2002/02/21 00:10:37  jongfoster
 *    Adding send-banner?type=auto option
 *
 *    Revision 1.12  2002/01/23 01:03:32  jongfoster
 *    Fixing gcc [CygWin] compiler warnings
 *
 *    Revision 1.11  2002/01/23 00:01:04  jongfoster
 *    Adding cgi_transparent_gif() for http://i.j.b/t
 *    Adding missing html_encode() to many CGI functions.
 *    Adding urlmatch.[ch] to http://i.j.b/show-version
 *
 *    Revision 1.10  2002/01/17 21:10:37  jongfoster
 *    Changes to cgi_show_url_info to use new matching code from urlmatch.c.
 *    Also fixing a problem in the same function with improperly quoted URLs
 *    in output HTML, and adding code to handle https:// URLs correctly.
 *
 *    Revision 1.9  2001/11/30 23:09:15  jongfoster
 *    Now reports on FEATURE_CGI_EDIT_ACTIONS
 *    Removing FEATURE_DENY_GZIP from template
 *
 *    Revision 1.8  2001/11/13 00:14:07  jongfoster
 *    Fixing stupid bug now I've figured out what || means.
 *    (It always returns 0 or 1, not one of it's paramaters.)
 *
 *    Revision 1.7  2001/10/23 21:48:19  jongfoster
 *    Cleaning up error handling in CGI functions - they now send back
 *    a HTML error page and should never cause a FATAL error.  (Fixes one
 *    potential source of "denial of service" attacks).
 *
 *    CGI actions file editor that works and is actually useful.
 *
 *    Ability to toggle JunkBuster remotely using a CGI call.
 *
 *    You can turn off both the above features in the main configuration
 *    file, e.g. if you are running a multi-user proxy.
 *
 *    Revision 1.6  2001/10/14 22:00:32  jongfoster
 *    Adding support for a 404 error when an invalid CGI page is requested.
 *
 *    Revision 1.5  2001/10/07 15:30:41  oes
 *    Removed FEATURE_DENY_GZIP
 *
 *    Revision 1.4  2001/10/02 15:31:12  oes
 *    Introduced show-request cgi
 *
 *    Revision 1.3  2001/09/22 16:34:44  jongfoster
 *    Removing unneeded #includes
 *
 *    Revision 1.2  2001/09/19 18:01:11  oes
 *    Fixed comments; cosmetics
 *
 *    Revision 1.1  2001/09/16 17:08:54  jongfoster
 *    Moving simple CGI functions from cgi.c to new file cgisimple.c
 *
 *
 **********************************************************************/


#include "config.h"

#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <assert.h>

#ifdef HAVE_ACCESS
#include <unistd.h>
#endif /* def HAVE_ACCESS */

#include "project.h"
#include "cgi.h"
#include "cgisimple.h"
#include "list.h"
#include "encode.h"
#include "jcc.h"
#include "filters.h"
#include "actions.h"
#include "miscutil.h"
#include "loadcfg.h"
#include "parsers.h"
#include "urlmatch.h"
#include "errlog.h"

const char cgisimple_h_rcs[] = CGISIMPLE_H_VERSION;

static char *show_rcs(void);
static jb_err show_defines(struct map *exports);
static jb_err cgi_show_file(struct client_state *csp,
                            struct http_response *rsp,
                            const struct map *parameters);
static jb_err load_file(const char *filename, char **buffer, size_t *length);

/*********************************************************************
 *
 * Function    :  cgi_default
 *
 * Description :  CGI function that is called for the CGI_SITE_1_HOST
 *                and CGI_SITE_2_HOST/CGI_SITE_2_PATH base URLs.
 *                Boring - only exports the default exports.
 *               
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : none
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory
 *
 *********************************************************************/
jb_err cgi_default(struct client_state *csp,
                   struct http_response *rsp,
                   const struct map *parameters)
{
   struct map *exports;

   (void)parameters;

   assert(csp);
   assert(rsp);

   if (NULL == (exports = default_exports(csp, "")))
   {
      return JB_ERR_MEMORY;
   }

   return template_fill_for_cgi(csp, "default", exports, rsp);
}


/*********************************************************************
 *
 * Function    :  cgi_error_404
 *
 * Description :  CGI function that is called if an unknown action was
 *                given.
 *               
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : none
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_error_404(struct client_state *csp,
                     struct http_response *rsp,
                     const struct map *parameters)
{
   struct map *exports;

   assert(csp);
   assert(rsp);
   assert(parameters);

   if (NULL == (exports = default_exports(csp, NULL)))
   {
      return JB_ERR_MEMORY;
   }

   rsp->status = strdup("404 Privoxy configuration page not found");
   if (rsp->status == NULL)
   {
      free_map(exports);
      return JB_ERR_MEMORY;
   }

   return template_fill_for_cgi(csp, "cgi-error-404", exports, rsp);
}


#ifdef FEATURE_GRACEFUL_TERMINATION
/*********************************************************************
 *
 * Function    :  cgi_die
 *
 * Description :  CGI function to shut down Privoxy.
 *                NOTE: Turning this on in a production build
 *                would be a BAD idea.  An EXTREMELY BAD idea.
 *                In short, don't do it.
 *               
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : none
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_die (struct client_state *csp,
                struct http_response *rsp,
                const struct map *parameters)
{
   assert(csp);
   assert(rsp);
   assert(parameters);

   /* quit */
   g_terminate = 1;

   /*
    * I don't really care what gets sent back to the browser.
    * Take the easy option - "out of memory" page.
    */

   return JB_ERR_MEMORY;
}
#endif /* def FEATURE_GRACEFUL_TERMINATION */


/*********************************************************************
 *
 * Function    :  cgi_show_request
 *
 * Description :  Show the client's request and what sed() would have
 *                made of it.
 *               
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : none
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_show_request(struct client_state *csp,
                        struct http_response *rsp,
                        const struct map *parameters)
{
   char *p;
   struct map *exports;

   assert(csp);
   assert(rsp);
   assert(parameters);

   if (NULL == (exports = default_exports(csp, "show-request")))
   {
      return JB_ERR_MEMORY;
   }
   
   /*
    * Repair the damage done to the IOB by get_header()
    */
   for (p = csp->iob->buf; p < csp->iob->eod; p++)
   {
      if (*p == '\0') *p = '\n';
   }

   /*
    * Export the original client's request and the one we would
    * be sending to the server if this wasn't a CGI call
    */

   if (map(exports, "client-request", 1, html_encode(csp->iob->buf), 0))
   {
      free_map(exports);
      return JB_ERR_MEMORY;
   }

   if (map(exports, "processed-request", 1,
         html_encode_and_free_original(list_to_text(csp->headers)), 0))
   {
      free_map(exports);
      return JB_ERR_MEMORY;
   }

   return template_fill_for_cgi(csp, "show-request", exports, rsp);
}


/*********************************************************************
 *
 * Function    :  cgi_send_banner
 *
 * Description :  CGI function that returns a banner. 
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters :
 *           type : Selects the type of banner between "trans", "logo",
 *                  and "auto". Defaults to "logo" if absent or invalid.
 *                  "auto" means to select as if we were image-blocking.
 *                  (Only the first character really counts; b and t are
 *                  equivalent).
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_send_banner(struct client_state *csp,
                       struct http_response *rsp,
                       const struct map *parameters)
{
   char imagetype = lookup(parameters, "type")[0];

   /*
    * If type is auto, then determine the right thing
    * to do from the set-image-blocker action
    */
   if (imagetype == 'a') 
   {
      /*
       * Default to pattern
       */
      imagetype = 'p';

#ifdef FEATURE_IMAGE_BLOCKING
      if ((csp->action->flags & ACTION_IMAGE_BLOCKER) != 0)
      {
         static const char prefix1[] = CGI_PREFIX "send-banner?type=";
         static const char prefix2[] = "http://" CGI_SITE_1_HOST "/send-banner?type=";
         const char *p = csp->action->string[ACTION_STRING_IMAGE_BLOCKER];

         if (p == NULL)
         {
            /* Use default - nothing to do here. */
         }
         else if (0 == strcmpic(p, "blank"))
         {
            imagetype = 'b';
         }
         else if (0 == strcmpic(p, "pattern"))
         {
            imagetype = 'p';
         }

         /*
          * If the action is to call this CGI, determine
          * the argument:
          */
         else if (0 == strncmpic(p, prefix1, sizeof(prefix1) - 1))
         {
            imagetype = p[sizeof(prefix1) - 1];
         }
         else if (0 == strncmpic(p, prefix2, sizeof(prefix2) - 1))
         {
            imagetype = p[sizeof(prefix2) - 1];
         }

         /*
          * Everything else must (should) be a URL to
          * redirect to.
          */
         else
         {
            imagetype = 'r';
         }
      }
#endif /* def FEATURE_IMAGE_BLOCKING */
   }
      
   /*
    * Now imagetype is either the non-auto type we were called with,
    * or it was auto and has since been determined. In any case, we
    * can proceed to actually answering the request by sending a redirect
    * or an image as appropriate:
    */
   if (imagetype == 'r') 
   {
      rsp->status = strdup("302 Local Redirect from Privoxy");
      if (rsp->status == NULL)
      {
         return JB_ERR_MEMORY;
      }
      if (enlist_unique_header(rsp->headers, "Location",
                               csp->action->string[ACTION_STRING_IMAGE_BLOCKER]))
      {
         return JB_ERR_MEMORY;
      }
   }
   else
   {
      if ((imagetype == 'b') || (imagetype == 't')) 
      {
         rsp->body = bindup(image_blank_data, image_blank_length);
         rsp->content_length = image_blank_length;
      }
      else
      {
         rsp->body = bindup(image_pattern_data, image_pattern_length);
         rsp->content_length = image_pattern_length;
      }

      if (rsp->body == NULL)
      {
         return JB_ERR_MEMORY;
      }
      if (enlist(rsp->headers, "Content-Type: " BUILTIN_IMAGE_MIMETYPE))
      {
         return JB_ERR_MEMORY;
      }

      rsp->is_static = 1;
   }

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  cgi_transparent_image
 *
 * Description :  CGI function that sends a 1x1 transparent image.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : None
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_transparent_image(struct client_state *csp,
                             struct http_response *rsp,
                             const struct map *parameters)
{
   (void)csp;
   (void)parameters;

   rsp->body = bindup(image_blank_data, image_blank_length);
   rsp->content_length = image_blank_length;

   if (rsp->body == NULL)
   {
      return JB_ERR_MEMORY;
   }

   if (enlist(rsp->headers, "Content-Type: " BUILTIN_IMAGE_MIMETYPE))
   {
      return JB_ERR_MEMORY;
   }

   rsp->is_static = 1;

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  cgi_send_default_favicon
 *
 * Description :  CGI function that sends the standard favicon.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : None
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_send_default_favicon(struct client_state *csp,
                                struct http_response *rsp,
                                const struct map *parameters)
{
   static const char default_favicon_data[] =
      "\000\000\001\000\001\000\020\020\002\000\000\000\000\000\260"
      "\000\000\000\026\000\000\000\050\000\000\000\020\000\000\000"
      "\040\000\000\000\001\000\001\000\000\000\000\000\100\000\000"
      "\000\000\000\000\000\000\000\000\000\002\000\000\000\000\000"
      "\000\000\377\377\377\000\377\000\052\000\017\360\000\000\077"
      "\374\000\000\161\376\000\000\161\376\000\000\361\377\000\000"
      "\361\377\000\000\360\017\000\000\360\007\000\000\361\307\000"
      "\000\361\307\000\000\361\307\000\000\360\007\000\000\160\036"
      "\000\000\177\376\000\000\077\374\000\000\017\360\000\000\360"
      "\017\000\000\300\003\000\000\200\001\000\000\200\001\000\000"
      "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
      "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
      "\000\000\200\001\000\000\200\001\000\000\300\003\000\000\360"
      "\017\000\000";
   static const size_t favicon_length = sizeof(default_favicon_data) - 1;

   (void)csp;
   (void)parameters;

   rsp->body = bindup(default_favicon_data, favicon_length);
   rsp->content_length = favicon_length;

   if (rsp->body == NULL)
   {
      return JB_ERR_MEMORY;
   }

   if (enlist(rsp->headers, "Content-Type: image/x-icon"))
   {
      return JB_ERR_MEMORY;
   }

   rsp->is_static = 1;

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  cgi_send_error_favicon
 *
 * Description :  CGI function that sends the favicon for error pages.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : None
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_send_error_favicon(struct client_state *csp,
                              struct http_response *rsp,
                              const struct map *parameters)
{
   static const char error_favicon_data[] =
      "\000\000\001\000\001\000\020\020\002\000\000\000\000\000\260"
      "\000\000\000\026\000\000\000\050\000\000\000\020\000\000\000"
      "\040\000\000\000\001\000\001\000\000\000\000\000\100\000\000"
      "\000\000\000\000\000\000\000\000\000\002\000\000\000\000\000"
      "\000\000\377\377\377\000\000\000\377\000\017\360\000\000\077"
      "\374\000\000\161\376\000\000\161\376\000\000\361\377\000\000"
      "\361\377\000\000\360\017\000\000\360\007\000\000\361\307\000"
      "\000\361\307\000\000\361\307\000\000\360\007\000\000\160\036"
      "\000\000\177\376\000\000\077\374\000\000\017\360\000\000\360"
      "\017\000\000\300\003\000\000\200\001\000\000\200\001\000\000"
      "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
      "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
      "\000\000\200\001\000\000\200\001\000\000\300\003\000\000\360"
      "\017\000\000";
   static const size_t favicon_length = sizeof(error_favicon_data) - 1;

   (void)csp;
   (void)parameters;

   rsp->body = bindup(error_favicon_data, favicon_length);
   rsp->content_length = favicon_length;

   if (rsp->body == NULL)
   {
      return JB_ERR_MEMORY;
   }

   if (enlist(rsp->headers, "Content-Type: image/x-icon"))
   {
      return JB_ERR_MEMORY;
   }

   rsp->is_static = 1;

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  cgi_send_stylesheet
 *
 * Description :  CGI function that sends a css stylesheet found
 *                in the cgi-style.css template
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : None
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_send_stylesheet(struct client_state *csp,
                           struct http_response *rsp,
                           const struct map *parameters)
{
   jb_err err;
   
   assert(csp);
   assert(rsp);

   (void)parameters;

   err = template_load(csp, &rsp->body, "cgi-style.css", 0);

   if (err == JB_ERR_FILE)
   {
      /*
       * No way to tell user; send empty stylesheet
       */
      log_error(LOG_LEVEL_ERROR, "Could not find cgi-style.css template");
   }
   else if (err)
   {
      return err; /* JB_ERR_MEMORY */
   }

   if (enlist(rsp->headers, "Content-Type: text/css"))
   {
      return JB_ERR_MEMORY;
   }

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  cgi_send_url_info_osd
 *
 * Description :  CGI function that sends the OpenSearch Description
 *                template for the show-url-info page. It allows to
 *                access the page through "search engine plugins".
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : None
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_send_url_info_osd(struct client_state *csp,
                               struct http_response *rsp,
                               const struct map *parameters)
{
   jb_err err = JB_ERR_MEMORY;
   struct map *exports = default_exports(csp, NULL);

   (void)csp;
   (void)parameters;

   if (NULL != exports)
   {
      err = template_fill_for_cgi(csp, "url-info-osd.xml", exports, rsp);
      if (JB_ERR_OK == err)
      {
         err = enlist(rsp->headers,
            "Content-Type: application/opensearchdescription+xml");
      }
   }

   return err;

}


/*********************************************************************
 *
 * Function    :  cgi_send_user_manual
 *
 * Description :  CGI function that sends a file in the user
 *                manual directory.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : file=name.html, the name of the HTML file
 *                  (relative to user-manual from config)
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_send_user_manual(struct client_state *csp,
                            struct http_response *rsp,
                            const struct map *parameters)
{
   const char * filename;
   char *full_path;
   jb_err err = JB_ERR_OK;
   size_t length;

   assert(csp);
   assert(rsp);
   assert(parameters);

   if (!parameters->first)
   {
      /* requested http://p.p/user-manual (without trailing slash) */
      return cgi_redirect(rsp, CGI_PREFIX "user-manual/");
   }

   get_string_param(parameters, "file", &filename);
   /* Check paramter for hack attempts */
   if (filename && strchr(filename, '/'))
   {
      return JB_ERR_CGI_PARAMS;
   }
   if (filename && strstr(filename, ".."))
   {
      return JB_ERR_CGI_PARAMS;
   }

   full_path = make_path(csp->config->usermanual, filename ? filename : "index.html");
   if (full_path == NULL)
   {
      return JB_ERR_MEMORY;
   }

   err = load_file(full_path, &rsp->body, &rsp->content_length);
   if (JB_ERR_OK != err)
   {
      assert((JB_ERR_FILE == err) || (JB_ERR_MEMORY == err));
      if (JB_ERR_FILE == err)
      {
         err = cgi_error_no_template(csp, rsp, full_path);
      }
      freez(full_path);
      return err;
   }
   freez(full_path);

   /* Guess correct Content-Type based on the filename's ending */
   if (filename)
   {
      length = strlen(filename);
   }
   else
   {
      length = 0;
   } 
   if((length>=4) && !strcmp(&filename[length-4], ".css"))
   {
      err = enlist(rsp->headers, "Content-Type: text/css");
   }
   else if((length>=4) && !strcmp(&filename[length-4], ".jpg"))
   {
      err = enlist(rsp->headers, "Content-Type: image/jpeg");
   }
   else
   {
      err = enlist(rsp->headers, "Content-Type: text/html");
   }

   return err;
}


/*********************************************************************
 *
 * Function    :  cgi_show_version
 *
 * Description :  CGI function that returns a a web page describing the
 *                file versions of Privoxy.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : none
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_show_version(struct client_state *csp,
                        struct http_response *rsp,
                        const struct map *parameters)
{
   struct map *exports;

   assert(csp);
   assert(rsp);
   assert(parameters);

   if (NULL == (exports = default_exports(csp, "show-version")))
   {
      return JB_ERR_MEMORY;
   }

   if (map(exports, "sourceversions", 1, show_rcs(), 0))
   {
      free_map(exports);
      return JB_ERR_MEMORY;
   }

   return template_fill_for_cgi(csp, "show-version", exports, rsp);
}


/*********************************************************************
 *
 * Function    :  cgi_show_status
 *
 * Description :  CGI function that returns a web page describing the
 *                current status of Privoxy.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters :
 *        file :  Which file to show.  Only first letter is checked,
 *                valid values are:
 *                - "a"ction file
 *                - "r"egex
 *                - "t"rust
 *                Default is to show menu and other information.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_show_status(struct client_state *csp,
                       struct http_response *rsp,
                       const struct map *parameters)
{
   char *s = NULL;
   unsigned i;
   int j;

   char buf[BUFFER_SIZE];
#ifdef FEATURE_STATISTICS
   float perc_rej;   /* Percentage of http requests rejected */
   int local_urls_read;
   int local_urls_rejected;
#endif /* ndef FEATURE_STATISTICS */
   jb_err err = JB_ERR_OK;

   struct map *exports;

   assert(csp);
   assert(rsp);
   assert(parameters);

   if ('\0' != *(lookup(parameters, "file")))
   {
      return cgi_show_file(csp, rsp, parameters);
   }

   if (NULL == (exports = default_exports(csp, "show-status")))
   {
      return JB_ERR_MEMORY;
   }

   s = strdup("");
   for (j = 0; (s != NULL) && (j < Argc); j++)
   {
      if (!err) err = string_join  (&s, html_encode(Argv[j]));
      if (!err) err = string_append(&s, " ");
   }
   if (!err) err = map(exports, "invocation", 1, s, 0);

   if (!err) err = map(exports, "options", 1, csp->config->proxy_args, 1);
   if (!err) err = show_defines(exports);

   if (err) 
   {
      free_map(exports);
      return JB_ERR_MEMORY;
   }

#ifdef FEATURE_STATISTICS
   local_urls_read     = urls_read;
   local_urls_rejected = urls_rejected;

   /*
    * Need to alter the stats not to include the fetch of this
    * page.
    *
    * Can't do following thread safely! doh!
    *
    * urls_read--;
    * urls_rejected--; * This will be incremented subsequently *
    */

   if (local_urls_read == 0)
   {
      if (!err) err = map_block_killer(exports, "have-stats");
   }
   else
   {
      if (!err) err = map_block_killer(exports, "have-no-stats");

      perc_rej = (float)local_urls_rejected * 100.0F /
            (float)local_urls_read;

      snprintf(buf, sizeof(buf), "%d", local_urls_read);
      if (!err) err = map(exports, "requests-received", 1, buf, 1);

      snprintf(buf, sizeof(buf), "%d", local_urls_rejected);
      if (!err) err = map(exports, "requests-blocked", 1, buf, 1);

      snprintf(buf, sizeof(buf), "%6.2f", perc_rej);
      if (!err) err = map(exports, "percent-blocked", 1, buf, 1);
   }

#else /* ndef FEATURE_STATISTICS */
   err = err || map_block_killer(exports, "statistics");
#endif /* ndef FEATURE_STATISTICS */
   
   /* 
    * List all action files in use, together with view and edit links,
    * except for standard.action, which should only be viewable. (Not
    * enforced in the editor itself)
    * FIXME: Shouldn't include hardwired HTML here, use line template instead!
    */
   s = strdup("");
   for (i = 0; i < MAX_AF_FILES; i++)
   {
      if (csp->actions_list[i] != NULL)
      {
         if (!err) err = string_append(&s, "<tr><td>");
         if (!err) err = string_join(&s, html_encode(csp->actions_list[i]->filename));
         snprintf(buf, sizeof(buf),
            "</td><td class=\"buttons\"><a href=\"/show-status?file=actions&amp;index=%u\">View</a>", i);
         if (!err) err = string_append(&s, buf);

#ifdef FEATURE_CGI_EDIT_ACTIONS
         if ((csp->config->feature_flags & RUNTIME_FEATURE_CGI_EDIT_ACTIONS)
            && (NULL == strstr(csp->actions_list[i]->filename, "standard.action"))
            && (NULL != csp->config->actions_file_short[i]))
         {
#ifdef HAVE_ACCESS
            if (access(csp->config->actions_file[i], W_OK) == 0)
            {
#endif /* def HAVE_ACCESS */
               snprintf(buf, sizeof(buf), "&nbsp;&nbsp;<a href=\"/edit-actions-list?f=%u\">Edit</a>", i);
               if (!err) err = string_append(&s, buf);
#ifdef HAVE_ACCESS
            }
            else
            {
               if (!err) err = string_append(&s, "&nbsp;&nbsp;<strong>No write access.</strong>");
            }
#endif /* def HAVE_ACCESS */
         }
#endif

         if (!err) err = string_append(&s, "</td></tr>\n");
      }
   }
   if (*s != '\0')   
   {
      if (!err) err = map(exports, "actions-filenames", 1, s, 0);
   }
   else
   {
      if (!err) err = map(exports, "actions-filenames", 1, "<tr><td>None specified</td></tr>", 1);
   }

   /* 
    * List all re_filterfiles in use, together with view options.
    * FIXME: Shouldn't include hardwired HTML here, use line template instead!
    */
   s = strdup("");
   for (i = 0; i < MAX_AF_FILES; i++)
   {
      if (csp->rlist[i] != NULL)
      {
         if (!err) err = string_append(&s, "<tr><td>");
         if (!err) err = string_join(&s, html_encode(csp->rlist[i]->filename));
         snprintf(buf, sizeof(buf),
            "</td><td class=\"buttons\"><a href=\"/show-status?file=filter&amp;index=%u\">View</a>", i);
         if (!err) err = string_append(&s, buf);
         if (!err) err = string_append(&s, "</td></tr>\n");
      }
   }
   if (*s != '\0')   
   {
      if (!err) err = map(exports, "re-filter-filenames", 1, s, 0);
   }
   else
   {
      if (!err) err = map(exports, "re-filter-filenames", 1, "<tr><td>None specified</td></tr>", 1);
      if (!err) err = map_block_killer(exports, "have-filterfile");
   }

#ifdef FEATURE_TRUST
   if (csp->tlist)
   {
      if (!err) err = map(exports, "trust-filename", 1, html_encode(csp->tlist->filename), 0);
   }
   else
   {
      if (!err) err = map(exports, "trust-filename", 1, "None specified", 1);
      if (!err) err = map_block_killer(exports, "have-trustfile");
   }
#else
   if (!err) err = map_block_killer(exports, "trust-support");
#endif /* ndef FEATURE_TRUST */

#ifdef FEATURE_CGI_EDIT_ACTIONS
   if (!err && (csp->config->feature_flags & RUNTIME_FEATURE_CGI_EDIT_ACTIONS))
   {
      err = map_block_killer(exports, "cgi-editor-is-disabled");
   }
#endif /* ndef CGI_EDIT_ACTIONS */

   if (err)
   {
      free_map(exports);
      return JB_ERR_MEMORY;
   }

   return template_fill_for_cgi(csp, "show-status", exports, rsp);
}

 
/*********************************************************************
 *
 * Function    :  cgi_show_url_info
 *
 * Description :  CGI function that determines and shows which actions
 *                Privoxy will perform for a given url, and which
 *                matches starting from the defaults have lead to that.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters :
 *            url : The url whose actions are to be determined.
 *                  If url is unset, the url-given conditional will be
 *                  set, so that all but the form can be suppressed in
 *                  the template.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_show_url_info(struct client_state *csp,
                         struct http_response *rsp,
                         const struct map *parameters)
{
   char *url_param;
   struct map *exports;
   char buf[150];

   assert(csp);
   assert(rsp);
   assert(parameters);

   if (NULL == (exports = default_exports(csp, "show-url-info")))
   {
      return JB_ERR_MEMORY;
   }

   /*
    * Get the url= parameter (if present) and remove any leading/trailing spaces.
    */
   url_param = strdup(lookup(parameters, "url"));
   if (url_param == NULL)
   {
      free_map(exports);
      return JB_ERR_MEMORY;
   }
   chomp(url_param);

   /*
    * Handle prefixes.  4 possibilities:
    * 1) "http://" or "https://" prefix present and followed by URL - OK
    * 2) Only the "http://" or "https://" part is present, no URL - change
    *    to empty string so it will be detected later as "no URL".
    * 3) Parameter specified but doesn't contain "http(s?)://" - add a
    *    "http://" prefix.
    * 4) Parameter not specified or is empty string - let this fall through
    *    for now, next block of code will handle it.
    */
   if (0 == strncmp(url_param, "http://", 7))
   {
      if (url_param[7] == '\0')
      {
         /*
          * Empty URL (just prefix).
          * Make it totally empty so it's caught by the next if()
          */
         url_param[0] = '\0';
      }
   }
   else if (0 == strncmp(url_param, "https://", 8))
   {
      if (url_param[8] == '\0')
      {
         /*
          * Empty URL (just prefix).
          * Make it totally empty so it's caught by the next if()
          */
         url_param[0] = '\0';
      }
   }
   else if ((url_param[0] != '\0') && (NULL == strstr(url_param, "://")))
   {
      /* No prefix - assume http:// */
      char *url_param_prefixed = strdup("http://");

      if (JB_ERR_OK != string_join(&url_param_prefixed, url_param))
      {
         free_map(exports);
         return JB_ERR_MEMORY;
      }
      url_param = url_param_prefixed;
   }

   /*
    * Hide "toggle off" warning if Privoxy is toggled on.
    */
   if (
#ifdef FEATURE_TOGGLE
       (global_toggle_state == 1) &&
#endif /* def FEATURE_TOGGLE */
       map_block_killer(exports, "privoxy-is-toggled-off")
      )
   {
      free_map(exports);
      return JB_ERR_MEMORY;
   }

   if (url_param[0] == '\0')
   {
      /* URL paramater not specified, display query form only. */
      free(url_param);
      if (map_block_killer(exports, "url-given")
        || map(exports, "url", 1, "", 1))
      {
         free_map(exports);
         return JB_ERR_MEMORY;
      }
   }
   else
   {
      /* Given a URL, so query it. */
      jb_err err;
      char *matches;
      char *s;
      int hits = 0;
      struct file_list *fl;
      struct url_actions *b;
      struct http_request url_to_query[1];
      struct current_action_spec action[1];
      int i;
      
      if (map(exports, "url", 1, html_encode(url_param), 0))
      {
         free(url_param);
         free_map(exports);
         return JB_ERR_MEMORY;
      }

      init_current_action(action);

      if (map(exports, "default", 1, current_action_to_html(csp, action), 0))
      {
         free_current_action(action);
         free(url_param);
         free_map(exports);
         return JB_ERR_MEMORY;
      }

      memset(url_to_query, '\0', sizeof(url_to_query));
      err = parse_http_url(url_param, url_to_query, REQUIRE_PROTOCOL);
      assert((err != JB_ERR_OK) || (url_to_query->ssl == !strncmpic(url_param, "https://", 8)));

      free(url_param);

      if (err == JB_ERR_MEMORY)
      {
         free_http_request(url_to_query);
         free_current_action(action);
         free_map(exports);
         return JB_ERR_MEMORY;
      }
      else if (err)
      {
         /* Invalid URL */

         err = map(exports, "matches", 1, "<b>[Invalid URL specified!]</b>" , 1);
         if (!err) err = map(exports, "final", 1, lookup(exports, "default"), 1);
         if (!err) err = map_block_killer(exports, "valid-url");

         free_current_action(action);
         free_http_request(url_to_query);

         if (err)
         {
            free_map(exports);
            return JB_ERR_MEMORY;
         }

         return template_fill_for_cgi(csp, "show-url-info", exports, rsp);
      }

      /*
       * We have a warning about SSL paths.  Hide it for unencrypted sites.
       */
      if (!url_to_query->ssl)
      {
         if (map_block_killer(exports, "https"))
         {
            free_current_action(action);
            free_map(exports);
            free_http_request(url_to_query);
            return JB_ERR_MEMORY;
         }
      }

      matches = strdup("<table summary=\"\" class=\"transparent\">");

      for (i = 0; i < MAX_AF_FILES; i++)
      {
         if (NULL == csp->config->actions_file_short[i]
             || !strcmp(csp->config->actions_file_short[i], "standard.action")) continue;

         b = NULL;
         hits = 1;
         if ((fl = csp->actions_list[i]) != NULL)
         {
            if ((b = fl->f) != NULL)
            {
               /* FIXME: Hardcoded HTML! */
               string_append(&matches, "<tr><th>In file: ");
               string_join  (&matches, html_encode(csp->config->actions_file_short[i]));
               snprintf(buf, sizeof(buf), " <a class=\"cmd\" href=\"/show-status?file=actions&amp;index=%d\">", i);
               string_append(&matches, buf);
               string_append(&matches, "View</a>");
#ifdef FEATURE_CGI_EDIT_ACTIONS
               if (csp->config->feature_flags & RUNTIME_FEATURE_CGI_EDIT_ACTIONS)
               {
#ifdef HAVE_ACCESS
                  if (access(csp->config->actions_file[i], W_OK) == 0)
                  {
#endif /* def HAVE_ACCESS */
                     snprintf(buf, sizeof(buf),
                        " <a class=\"cmd\" href=\"/edit-actions-list?f=%d\">", i);
                     string_append(&matches, buf);
                     string_append(&matches, "Edit</a>");
#ifdef HAVE_ACCESS
                  }
                  else
                  {
                     string_append(&matches, " <strong>No write access.</strong>");
                  }
#endif /* def HAVE_ACCESS */
               }
#endif /* FEATURE_CGI_EDIT_ACTIONS */

               string_append(&matches, "</th></tr>\n");

               hits = 0;
               b = b->next;
            }
         }

         for (; (b != NULL) && (matches != NULL); b = b->next)
         {
            if (url_match(b->url, url_to_query))
            {
               string_append(&matches, "<tr><td>{");
               string_join  (&matches, actions_to_html(csp, b->action));
               string_append(&matches, " }<br>\n<code>");
               string_join  (&matches, html_encode(b->url->spec));
               string_append(&matches, "</code></td></tr>\n");

               if (merge_current_action(action, b->action))
               {
                  freez(matches);
                  free_http_request(url_to_query);
                  free_current_action(action);
                  free_map(exports);
                  return JB_ERR_MEMORY;
               }
               hits++;
            }
         }

         if (!hits)
         {
            string_append(&matches, "<tr><td>(no matches in this file)</td></tr>\n");
         }
      }
      string_append(&matches, "</table>\n");

      /*
       * XXX: Kludge to make sure the "Forward settings" section
       * shows what forward-override{} would do with the requested URL.
       * No one really cares how the CGI request would be forwarded
       * if it wasn't intercepted as CGI request in the first place.
       *
       * From here on the action bitmask will no longer reflect
       * the real url (http://config.privoxy.org/show-url-info?url=.*),
       * but luckily it's no longer required later on anyway.
       */
      free_current_action(csp->action);
      get_url_actions(csp, url_to_query);

      /*
       * Fill in forwarding settings.
       *
       * The possibilities are:
       *  - no forwarding
       *  - http forwarding only
       *  - socks4(a) forwarding only
       *  - socks4(a) and http forwarding.
       *
       * XXX: Parts of this code could be reused for the
       * "forwarding-failed" template which currently doesn't
       * display the proxy port and an eventual second forwarder.
       */
      {
         const struct forward_spec *fwd = forward_url(csp, url_to_query);

         if ((fwd->gateway_host == NULL) && (fwd->forward_host == NULL))
         {
            if (!err) err = map_block_killer(exports, "socks-forwarder");
            if (!err) err = map_block_killer(exports, "http-forwarder");
         }
         else
         {
            char port[10]; /* We save proxy ports as int but need a string here */

            if (!err) err = map_block_killer(exports, "no-forwarder");

            if (fwd->gateway_host != NULL)
            {
               char *socks_type = NULL;

               switch (fwd->type)
               {
                  case SOCKS_4:
                     socks_type = "socks4";
                     break;
                  case SOCKS_4A:
                     socks_type = "socks4a";
                     break;
                  case SOCKS_5:
                     socks_type = "socks5";
                     break;
                  default:
                     log_error(LOG_LEVEL_FATAL, "Unknown socks type: %d.", fwd->type);
               }

               if (!err) err = map(exports, "socks-type", 1, socks_type, 1);
               if (!err) err = map(exports, "gateway-host", 1, fwd->gateway_host, 1);
               snprintf(port, sizeof(port), "%d", fwd->gateway_port);
               if (!err) err = map(exports, "gateway-port", 1, port, 1);
            }
            else
            {
               if (!err) err = map_block_killer(exports, "socks-forwarder");
            }

            if (fwd->forward_host != NULL)
            {
               if (!err) err = map(exports, "forward-host", 1, fwd->forward_host, 1);
               snprintf(port, sizeof(port), "%d", fwd->forward_port);
               if (!err) err = map(exports, "forward-port", 1, port, 1);
            }
            else
            {
               if (!err) err = map_block_killer(exports, "http-forwarder");
            }
         }
      }

      free_http_request(url_to_query);

      if (err || matches == NULL)
      {
         free_current_action(action);
         free_map(exports);
         return JB_ERR_MEMORY;
      }

#ifdef FEATURE_CGI_EDIT_ACTIONS
      if ((csp->config->feature_flags & RUNTIME_FEATURE_CGI_EDIT_ACTIONS))
      {
         err = map_block_killer(exports, "cgi-editor-is-disabled");
      }
#endif /* FEATURE_CGI_EDIT_ACTIONS */

      /*
       * If zlib support is available, if no content filters
       * are enabled or if the prevent-compression action is enabled,
       * suppress the "compression could prevent filtering" warning.
       */
#ifndef FEATURE_ZLIB
      if (!content_filters_enabled(action) ||
         (action->flags & ACTION_NO_COMPRESSION))
#endif
      {
         if (!err) err = map_block_killer(exports, "filters-might-be-ineffective");
      }

      if (err || map(exports, "matches", 1, matches , 0))
      {
         free_current_action(action);
         free_map(exports);
         return JB_ERR_MEMORY;
      }

      s = current_action_to_html(csp, action);

      free_current_action(action);

      if (map(exports, "final", 1, s, 0))
      {
         free_map(exports);
         return JB_ERR_MEMORY;
      }
   }

   return template_fill_for_cgi(csp, "show-url-info", exports, rsp);
}


/*********************************************************************
 *
 * Function    :  cgi_robots_txt
 *
 * Description :  CGI function to return "/robots.txt".
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters : None
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
jb_err cgi_robots_txt(struct client_state *csp,
                      struct http_response *rsp,
                      const struct map *parameters)
{
   char buf[100];
   jb_err err;

   (void)csp;
   (void)parameters;

   rsp->body = strdup(
      "# This is the Privoxy control interface.\n"
      "# It isn't very useful to index it, and you're likely to break stuff.\n"
      "# So go away!\n"
      "\n"
      "User-agent: *\n"
      "Disallow: /\n"
      "\n");
   if (rsp->body == NULL)
   {
      return JB_ERR_MEMORY;
   }

   err = enlist_unique(rsp->headers, "Content-Type: text/plain", 13);

   rsp->is_static = 1;

   get_http_time(7 * 24 * 60 * 60, buf, sizeof(buf)); /* 7 days into future */
   if (!err) err = enlist_unique_header(rsp->headers, "Expires", buf);

   return (err ? JB_ERR_MEMORY : JB_ERR_OK);
}


/*********************************************************************
 *
 * Function    :  show_defines
 *
 * Description :  Add to a map the state od all conditional #defines
 *                used when building
 *
 * Parameters  :
 *          1  :  exports = map to extend
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
static jb_err show_defines(struct map *exports)
{
   jb_err err = JB_ERR_OK;

#ifdef FEATURE_ACL
   if (!err) err = map_conditional(exports, "FEATURE_ACL", 1);
#else /* ifndef FEATURE_ACL */
   if (!err) err = map_conditional(exports, "FEATURE_ACL", 0);
#endif /* ndef FEATURE_ACL */

#ifdef FEATURE_CGI_EDIT_ACTIONS
   if (!err) err = map_conditional(exports, "FEATURE_CGI_EDIT_ACTIONS", 1);
#else /* ifndef FEATURE_CGI_EDIT_ACTIONS */
   if (!err) err = map_conditional(exports, "FEATURE_CGI_EDIT_ACTIONS", 0);
#endif /* ndef FEATURE_CGI_EDIT_ACTIONS */

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   if (!err) err = map_conditional(exports, "FEATURE_CONNECTION_KEEP_ALIVE", 1);
#else /* ifndef FEATURE_CGI_EDIT_ACTIONS */
   if (!err) err = map_conditional(exports, "FEATURE_CONNECTION_KEEP_ALIVE", 0);
#endif /* ndef FEATURE_CONNECTION_KEEP_ALIVE */

#ifdef FEATURE_FAST_REDIRECTS
   if (!err) err = map_conditional(exports, "FEATURE_FAST_REDIRECTS", 1);
#else /* ifndef FEATURE_FAST_REDIRECTS */
   if (!err) err = map_conditional(exports, "FEATURE_FAST_REDIRECTS", 0);
#endif /* ndef FEATURE_FAST_REDIRECTS */

#ifdef FEATURE_FORCE_LOAD
   if (!err) err = map_conditional(exports, "FEATURE_FORCE_LOAD", 1);
   if (!err) err = map(exports, "FORCE_PREFIX", 1, FORCE_PREFIX, 1);
#else /* ifndef FEATURE_FORCE_LOAD */
   if (!err) err = map_conditional(exports, "FEATURE_FORCE_LOAD", 0);
   if (!err) err = map(exports, "FORCE_PREFIX", 1, "(none - disabled)", 1);
#endif /* ndef FEATURE_FORCE_LOAD */

#ifdef FEATURE_GRACEFUL_TERMINATION
   if (!err) err = map_conditional(exports, "FEATURE_GRACEFUL_TERMINATION", 1);
#else /* ifndef FEATURE_GRACEFUL_TERMINATION */
   if (!err) err = map_conditional(exports, "FEATURE_GRACEFUL_TERMINATION", 0);
#endif /* ndef FEATURE_GRACEFUL_TERMINATION */

#ifdef FEATURE_IMAGE_BLOCKING
   if (!err) err = map_conditional(exports, "FEATURE_IMAGE_BLOCKING", 1);
#else /* ifndef FEATURE_IMAGE_BLOCKING */
   if (!err) err = map_conditional(exports, "FEATURE_IMAGE_BLOCKING", 0);
#endif /* ndef FEATURE_IMAGE_BLOCKING */

#ifdef FEATURE_IMAGE_DETECT_MSIE
   if (!err) err = map_conditional(exports, "FEATURE_IMAGE_DETECT_MSIE", 1);
#else /* ifndef FEATURE_IMAGE_DETECT_MSIE */
   if (!err) err = map_conditional(exports, "FEATURE_IMAGE_DETECT_MSIE", 0);
#endif /* ndef FEATURE_IMAGE_DETECT_MSIE */

#ifdef FEATURE_NO_GIFS
   if (!err) err = map_conditional(exports, "FEATURE_NO_GIFS", 1);
#else /* ifndef FEATURE_NO_GIFS */
   if (!err) err = map_conditional(exports, "FEATURE_NO_GIFS", 0);
#endif /* ndef FEATURE_NO_GIFS */

#ifdef FEATURE_PTHREAD
   if (!err) err = map_conditional(exports, "FEATURE_PTHREAD", 1);
#else /* ifndef FEATURE_PTHREAD */
   if (!err) err = map_conditional(exports, "FEATURE_PTHREAD", 0);
#endif /* ndef FEATURE_PTHREAD */

#ifdef FEATURE_STATISTICS
   if (!err) err = map_conditional(exports, "FEATURE_STATISTICS", 1);
#else /* ifndef FEATURE_STATISTICS */
   if (!err) err = map_conditional(exports, "FEATURE_STATISTICS", 0);
#endif /* ndef FEATURE_STATISTICS */

#ifdef FEATURE_TOGGLE
   if (!err) err = map_conditional(exports, "FEATURE_TOGGLE", 1);
#else /* ifndef FEATURE_TOGGLE */
   if (!err) err = map_conditional(exports, "FEATURE_TOGGLE", 0);
#endif /* ndef FEATURE_TOGGLE */

#ifdef FEATURE_TRUST
   if (!err) err = map_conditional(exports, "FEATURE_TRUST", 1);
#else /* ifndef FEATURE_TRUST */
   if (!err) err = map_conditional(exports, "FEATURE_TRUST", 0);
#endif /* ndef FEATURE_TRUST */

#ifdef FEATURE_ZLIB
   if (!err) err = map_conditional(exports, "FEATURE_ZLIB", 1);
#else /* ifndef FEATURE_ZLIB */
   if (!err) err = map_conditional(exports, "FEATURE_ZLIB", 0);
#endif /* ndef FEATURE_ZLIB */

#ifdef STATIC_PCRE
   if (!err) err = map_conditional(exports, "STATIC_PCRE", 1);
#else /* ifndef STATIC_PCRE */
   if (!err) err = map_conditional(exports, "STATIC_PCRE", 0);
#endif /* ndef STATIC_PCRE */

#ifdef STATIC_PCRS
   if (!err) err = map_conditional(exports, "STATIC_PCRS", 1);
#else /* ifndef STATIC_PCRS */
   if (!err) err = map_conditional(exports, "STATIC_PCRS", 0);
#endif /* ndef STATIC_PCRS */

   return err;
}


/*********************************************************************
 *
 * Function    :  show_rcs
 *
 * Description :  Create a string with the rcs info for all sourcefiles
 *
 * Parameters  :  None
 *
 * Returns     :  A string, or NULL on out-of-memory.
 *
 *********************************************************************/
static char *show_rcs(void)
{
   char *result = strdup("");
   char buf[BUFFER_SIZE];

   /* Instead of including *all* dot h's in the project (thus creating a
    * tremendous amount of dependencies), I will concede to declaring them
    * as extern's.  This forces the developer to add to this list, but oh well.
    */

#define SHOW_RCS(__x)              \
   {                               \
      extern const char __x[];     \
      snprintf(buf, sizeof(buf), " %s\n", __x);   \
      string_append(&result, buf); \
   }

   /* In alphabetical order */
   SHOW_RCS(actions_h_rcs)
   SHOW_RCS(actions_rcs)
#ifdef AMIGA
   SHOW_RCS(amiga_h_rcs)
   SHOW_RCS(amiga_rcs)
#endif /* def AMIGA */
   SHOW_RCS(cgi_h_rcs)
   SHOW_RCS(cgi_rcs)
#ifdef FEATURE_CGI_EDIT_ACTIONS
   SHOW_RCS(cgiedit_h_rcs)
   SHOW_RCS(cgiedit_rcs)
#endif /* def FEATURE_CGI_EDIT_ACTIONS */
   SHOW_RCS(cgisimple_h_rcs)
   SHOW_RCS(cgisimple_rcs)
#ifdef __MINGW32__
   SHOW_RCS(cygwin_h_rcs)
#endif
   SHOW_RCS(deanimate_h_rcs)
   SHOW_RCS(deanimate_rcs)
   SHOW_RCS(encode_h_rcs)
   SHOW_RCS(encode_rcs)
   SHOW_RCS(errlog_h_rcs)
   SHOW_RCS(errlog_rcs)
   SHOW_RCS(filters_h_rcs)
   SHOW_RCS(filters_rcs)
   SHOW_RCS(gateway_h_rcs)
   SHOW_RCS(gateway_rcs)
   SHOW_RCS(jbsockets_h_rcs)
   SHOW_RCS(jbsockets_rcs)
   SHOW_RCS(jcc_h_rcs)
   SHOW_RCS(jcc_rcs)
   SHOW_RCS(list_h_rcs)
   SHOW_RCS(list_rcs)
   SHOW_RCS(loadcfg_h_rcs)
   SHOW_RCS(loadcfg_rcs)
   SHOW_RCS(loaders_h_rcs)
   SHOW_RCS(loaders_rcs)
   SHOW_RCS(miscutil_h_rcs)
   SHOW_RCS(miscutil_rcs)
   SHOW_RCS(parsers_h_rcs)
   SHOW_RCS(parsers_rcs)
   SHOW_RCS(pcrs_rcs)
   SHOW_RCS(pcrs_h_rcs)
   SHOW_RCS(project_h_rcs)
   SHOW_RCS(ssplit_h_rcs)
   SHOW_RCS(ssplit_rcs)
   SHOW_RCS(urlmatch_h_rcs)
   SHOW_RCS(urlmatch_rcs)
#ifdef _WIN32
#ifndef _WIN_CONSOLE
   SHOW_RCS(w32log_h_rcs)
   SHOW_RCS(w32log_rcs)
   SHOW_RCS(w32res_h_rcs)
   SHOW_RCS(w32taskbar_h_rcs)
   SHOW_RCS(w32taskbar_rcs)
#endif /* ndef _WIN_CONSOLE */
   SHOW_RCS(win32_h_rcs)
   SHOW_RCS(win32_rcs)
#endif /* def _WIN32 */

#undef SHOW_RCS

   return result;

}


/*********************************************************************
 *
 * Function    :  cgi_show_file
 *
 * Description :  CGI function that shows the content of a
 *                configuration file.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  rsp = http_response data structure for output
 *          3  :  parameters = map of cgi parameters
 *
 * CGI Parameters :
 *        file :  Which file to show.  Only first letter is checked,
 *                valid values are:
 *                - "a"ction file
 *                - "r"egex
 *                - "t"rust
 *                Default is to show menu and other information.
 *
 * Returns     :  JB_ERR_OK on success
 *                JB_ERR_MEMORY on out-of-memory error.  
 *
 *********************************************************************/
static jb_err cgi_show_file(struct client_state *csp,
                            struct http_response *rsp,
                            const struct map *parameters)
{
   unsigned i;
   const char * filename = NULL;
   char * file_description = NULL;

   assert(csp);
   assert(rsp);
   assert(parameters);

   switch (*(lookup(parameters, "file")))
   {
   case 'a':
      if (!get_number_param(csp, parameters, "index", &i) && i < MAX_AF_FILES && csp->actions_list[i])
      {
         filename = csp->actions_list[i]->filename;
         file_description = "Actions File";
      }
      break;

   case 'f':
      if (!get_number_param(csp, parameters, "index", &i) && i < MAX_AF_FILES && csp->rlist[i])
      {
         filename = csp->rlist[i]->filename;
         file_description = "Filter File";
      }
      break;

#ifdef FEATURE_TRUST
   case 't':
      if (csp->tlist)
      {
         filename = csp->tlist->filename;
         file_description = "Trust File";
      }
      break;
#endif /* def FEATURE_TRUST */
   }

   if (NULL != filename)
   {
      struct map *exports;
      char *s;
      jb_err err;
      size_t length;

      exports = default_exports(csp, "show-status");
      if (NULL == exports)
      {
         return JB_ERR_MEMORY;
      }

      if ( map(exports, "file-description", 1, file_description, 1)
        || map(exports, "filepath", 1, html_encode(filename), 0) )
      {
         free_map(exports);
         return JB_ERR_MEMORY;
      }

      err = load_file(filename, &s, &length);
      if (JB_ERR_OK != err)
      {
         if (map(exports, "contents", 1, "<h1>ERROR OPENING FILE!</h1>", 1))
         {
            free_map(exports);
            return JB_ERR_MEMORY;
         }
      }
      else
      {
         s = html_encode_and_free_original(s);
         if (NULL == s)
         {
            return JB_ERR_MEMORY;
         }

         if (map(exports, "contents", 1, s, 0))
         {
            free_map(exports);
            return JB_ERR_MEMORY;
         }
      }

      return template_fill_for_cgi(csp, "show-status-file", exports, rsp);
   }

   return JB_ERR_CGI_PARAMS;
}

 
/*********************************************************************
 *
 * Function    :  load_file
 *
 * Description :  Loads a file into a buffer.
 *
 * Parameters  :
 *          1  :  filename = Name of the file to be loaded.
 *          2  :  buffer   = Used to return the file's content.
 *          3  :  length   = Used to return the size of the file.
 *
 * Returns     :  JB_ERR_OK in case of success,
 *                JB_ERR_FILE in case of ordinary file loading errors
 *                            (fseek() and ftell() errors are fatal)
 *                JB_ERR_MEMORY in case of out-of-memory.
 *
 *********************************************************************/
static jb_err load_file(const char *filename, char **buffer, size_t *length)
{
   FILE *fp;
   long ret;
   jb_err err = JB_ERR_OK;

   fp = fopen(filename, "rb");
   if (NULL == fp)
   {
      return JB_ERR_FILE;
   }

   /* Get file length */
   if (fseek(fp, 0, SEEK_END))
   {
      log_error(LOG_LEVEL_FATAL,
         "Unexpected error while fseek()ing to the end of %s: %E",
         filename);
   }
   ret = ftell(fp);
   if (-1 == ret)
   {
      log_error(LOG_LEVEL_FATAL,
         "Unexpected ftell() error while loading %s: %E",
         filename);
   }
   *length = (size_t)ret;

   /* Go back to the beginning. */
   if (fseek(fp, 0, SEEK_SET))
   {
      log_error(LOG_LEVEL_FATAL,
         "Unexpected error while fseek()ing to the beginning of %s: %E",
         filename);
   }

   *buffer = (char *)zalloc(*length + 1);
   if (NULL == *buffer)
   {
      err = JB_ERR_MEMORY;
   }
   else if (!fread(*buffer, *length, 1, fp))
   {
      /*
       * May happen if the file size changes between fseek() and
       * fread(). If it does, we just log it and serve what we got.
       */
      log_error(LOG_LEVEL_ERROR,
         "Couldn't completely read file %s.", filename);
      err = JB_ERR_FILE;
   }

   fclose(fp);

   return err;

}


/*
  Local Variables:
  tab-width: 3
  end:
*/
