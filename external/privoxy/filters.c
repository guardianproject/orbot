const char filters_rcs[] = "$Id: filters.c,v 1.113 2009/03/08 14:19:23 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/filters.c,v $
 *
 * Purpose     :  Declares functions to parse/crunch headers and pages.
 *                Functions declared include:
 *                   `acl_addr', `add_stats', `block_acl', `block_imageurl',
 *                   `block_url', `url_actions', `domain_split',
 *                   `filter_popups', `forward_url', 'redirect_url',
 *                   `ij_untrusted_url', `intercept_url', `pcrs_filter_respose',
 *                   `ijb_send_banner', `trust_url', `gif_deanimate_response',
 *                   `execute_single_pcrs_command', `rewrite_url',
 *                   `get_last_url'
 *
 * Copyright   :  Written by and Copyright (C) 2001, 2004-2008 the SourceForge
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
 *    $Log: filters.c,v $
 *    Revision 1.113  2009/03/08 14:19:23  fabiankeil
 *    Fix justified (but harmless) compiler warnings
 *    on platforms where sizeof(int) < sizeof(long).
 *
 *    Revision 1.112  2009/03/01 18:28:23  fabiankeil
 *    Help clang understand that we aren't dereferencing
 *    NULL pointers here.
 *
 *    Revision 1.111  2008/12/04 18:13:46  fabiankeil
 *    Fix a cparser warning.
 *
 *    Revision 1.110  2008/11/10 16:40:25  fabiankeil
 *    Fix a gcc44 warning.
 *
 *    Revision 1.109  2008/11/08 15:48:41  fabiankeil
 *    Mention actual values when complaining about
 *    the chunk size exceeding the buffer size.
 *
 *    Revision 1.108  2008/05/21 15:35:08  fabiankeil
 *    - Mark csp as immutable for block_acl().
 *    - Remove an obsolete complaint about filter_popups().
 *
 *    Revision 1.107  2008/05/04 17:52:56  fabiankeil
 *    Adjust parse_http_url() call to new prototype.
 *
 *    Revision 1.106  2008/05/03 16:40:44  fabiankeil
 *    Change content_filters_enabled()'s parameter from
 *    csp->action to action so it can be also used in the
 *    CGI code. Don't bother checking if there are filters
 *    loaded, as that's somewhat besides the point.
 *
 *    Revision 1.105  2008/03/28 15:13:39  fabiankeil
 *    Remove inspect-jpegs action.
 *
 *    Revision 1.104  2008/03/27 18:27:24  fabiankeil
 *    Remove kill-popups action.
 *
 *    Revision 1.103  2008/03/06 16:33:45  fabiankeil
 *    If limit-connect isn't used, don't limit CONNECT requests to port 443.
 *
 *    Revision 1.102  2008/03/01 14:00:44  fabiankeil
 *    Let the block action take the reason for the block
 *    as argument and show it on the "blocked" page.
 *
 *    Revision 1.101  2008/02/23 16:57:12  fabiankeil
 *    Rename url_actions() to get_url_actions() and let it
 *    use the standard parameter ordering.
 *
 *    Revision 1.100  2008/02/23 16:33:43  fabiankeil
 *    Let forward_url() use the standard parameter ordering
 *    and mark its second parameter immutable.
 *
 *    Revision 1.99  2008/02/03 13:57:58  fabiankeil
 *    Add SOCKS5 support for forward-override{}.
 *
 *    Revision 1.98  2008/01/04 17:43:45  fabiankeil
 *    Improve the warning messages that get logged if the action files
 *    "enable" filters but no filters of that type have been loaded.
 *
 *    Revision 1.97  2007/11/30 15:37:03  fabiankeil
 *    Use freez instead of free.
 *
 *    Revision 1.96  2007/10/19 16:53:28  fabiankeil
 *    Add helper function to check if any content filters are enabled.
 *
 *    Revision 1.95  2007/10/17 19:31:20  fabiankeil
 *    Omitting the zero chunk that ends the chunk transfer encoding seems
 *    to be the new black. Log the problem and continue filtering anyway.
 *
 *    Revision 1.94  2007/09/29 13:20:20  fabiankeil
 *    Remove two redundant and one useless log messages.
 *
 *    Revision 1.93  2007/09/29 10:21:16  fabiankeil
 *    - Move get_filter_function() from jcc.c to filters.c
 *      so the filter functions can be static.
 *    - Don't bother filtering body-less responses.
 *
 *    Revision 1.92  2007/09/28 16:38:55  fabiankeil
 *    - Execute content filters through execute_content_filter().
 *    - Add prepare_for_filtering() so filter functions don't have to
 *      care about de-chunking and decompression. As a side effect this enables
 *      decompression for gif_deanimate_response() and jpeg_inspect_response().
 *    - Change remove_chunked_transfer_coding()'s return type to jb_err.
 *      Some clowns feel like chunking empty responses in which case
 *      (size == 0) is valid but previously would be interpreted as error.
 *
 *    Revision 1.91  2007/09/02 15:31:20  fabiankeil
 *    Move match_portlist() from filter.c to urlmatch.c.
 *    It's used for url matching, not for filtering.
 *
 *    Revision 1.90  2007/09/02 12:44:17  fabiankeil
 *    Remove newline at the end of a log_error() message.
 *
 *    Revision 1.89  2007/08/05 13:42:23  fabiankeil
 *    #1763173 from Stefan Huehner: declare some more functions static.
 *
 *    Revision 1.88  2007/06/01 16:41:11  fabiankeil
 *    Add forward-override{} to change the forwarding settings through
 *    action sections. This is mainly interesting to forward different
 *    clients differently (for example based on User-Agent or request
 *    origin).
 *
 *    Revision 1.87  2007/04/30 15:53:10  fabiankeil
 *    Make sure filters with dynamic jobs actually use them.
 *
 *    Revision 1.86  2007/04/30 15:03:28  fabiankeil
 *    - Introduce dynamic pcrs jobs that can resolve variables.
 *    - Don't run redirect functions more than once,
 *      unless they are activated more than once.
 *
 *    Revision 1.85  2007/03/21 12:24:47  fabiankeil
 *    - Log the content size after decompression in decompress_iob()
 *      instead of pcrs_filter_response().
 *
 *    Revision 1.84  2007/03/20 15:16:34  fabiankeil
 *    Use dedicated header filter actions instead of abusing "filter".
 *    Replace "filter-client-headers" and "filter-client-headers"
 *    with "server-header-filter" and "client-header-filter".
 *
 *    Revision 1.83  2007/03/17 15:20:05  fabiankeil
 *    New config option: enforce-blocks.
 *
 *    Revision 1.82  2007/03/13 11:28:43  fabiankeil
 *    - Fix port handling in acl_addr() and use a temporary acl spec
 *      copy so error messages don't contain a truncated version.
 *    - Log size of iob before and after decompression.
 *
 *    Revision 1.81  2007/03/05 14:40:53  fabiankeil
 *    - Cosmetical changes for LOG_LEVEL_RE_FILTER messages.
 *    - Hide the "Go there anyway" link for blocked CONNECT
 *      requests where going there anyway doesn't work anyway.
 *
 *    Revision 1.80  2007/02/07 10:55:20  fabiankeil
 *    - Save the reason for generating http_responses.
 *    - Block (+block) with status code 403 instead of 404.
 *    - Use a different kludge to remember a failed decompression.
 *
 *    Revision 1.79  2007/01/31 16:21:38  fabiankeil
 *    Search for Max-Forwards headers case-insensitive,
 *    don't generate the "501 unsupported" message for invalid
 *    Max-Forwards values and don't increase negative ones.
 *
 *    Revision 1.78  2007/01/28 13:41:18  fabiankeil
 *    - Add HEAD support to finish_http_response.
 *    - Add error favicon to internal HTML error messages.
 *
 *    Revision 1.77  2007/01/12 15:36:44  fabiankeil
 *    Mark *csp as immutable for is_untrusted_url()
 *    and is_imageurl(). Closes FR 1237736.
 *
 *    Revision 1.76  2007/01/01 19:36:37  fabiankeil
 *    Integrate a modified version of Wil Mahan's
 *    zlib patch (PR #895531).
 *
 *    Revision 1.75  2006/12/29 18:30:46  fabiankeil
 *    Fixed gcc43 conversion warnings,
 *    changed sprintf calls to snprintf.
 *
 *    Revision 1.74  2006/12/24 17:37:38  fabiankeil
 *    Adjust comment in pcrs_filter_response()
 *    to recent pcrs changes. Hohoho.
 *
 *    Revision 1.73  2006/12/23 16:01:02  fabiankeil
 *    Don't crash if pcre returns an error code
 *    that pcrs didn't expect. Fixes BR 1621173.
 *
 *    Revision 1.72  2006/12/22 18:52:53  fabiankeil
 *    Modified is_untrusted_url to complain in case of
 *    write errors and to give a reason when adding new
 *    entries to the trustfile. Closes FR 1097611.
 *
 *    Revision 1.71  2006/12/22 14:24:52  fabiankeil
 *    Skip empty filter files in pcrs_filter_response,
 *    but don't ignore the ones that come afterwards.
 *    Fixes parts of BR 1619208.
 *
 *    Revision 1.70  2006/12/09 13:33:15  fabiankeil
 *    Added some sanity checks for get_last_url().
 *    Fixed possible segfault caused by my last commit.
 *
 *    Revision 1.69  2006/12/08 12:39:13  fabiankeil
 *    Let get_last_url() catch https URLs as well.
 *
 *    Revision 1.68  2006/12/05 14:45:48  fabiankeil
 *    Make sure get_last_url() behaves like advertised
 *    and fast-redirects{} can be combined with redirect{}.
 *
 *    Revision 1.67  2006/11/28 15:19:43  fabiankeil
 *    Implemented +redirect{s@foo@bar@} to generate
 *    a redirect based on a rewritten version of the
 *    original URL.
 *
 *    Revision 1.66  2006/09/23 13:26:38  roro
 *    Replace TABs by spaces in source code.
 *
 *    Revision 1.65  2006/09/21 12:54:43  fabiankeil
 *    Fix +redirect{}. Didn't work with -fast-redirects.
 *
 *    Revision 1.64  2006/08/31 10:55:49  fabiankeil
 *    Block requests for untrusted URLs with status
 *    code 403 instead of 200.
 *
 *    Revision 1.63  2006/08/31 10:11:28  fabiankeil
 *    Don't free p which is still in use and will be later
 *    freed by free_map(). Don't claim the referrer is unknown
 *    when the client didn't set one.
 *
 *    Revision 1.62  2006/08/14 00:27:47  david__schmidt
 *    Feature request 595948: Re-Filter logging in single line
 *
 *    Revision 1.61  2006/08/03 02:46:41  david__schmidt
 *    Incorporate Fabian Keil's patch work:http://www.fabiankeil.de/sourcecode/privoxy/
 *
 *    Revision 1.60  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.58.2.9  2006/01/29 23:10:56  david__schmidt
 *    Multiple filter file support
 *
 *    Revision 1.58.2.8  2005/05/07 21:50:55  david__schmidt
 *    A few memory leaks plugged (mostly on error paths)
 *
 *    Revision 1.58.2.7  2004/10/03 12:53:32  david__schmidt
 *    Add the ability to check jpeg images for invalid
 *    lengths of comment blocks.  Defensive strategy
 *    against the exploit:
 *       Microsoft Security Bulletin MS04-028
 *       Buffer Overrun in JPEG Processing (GDI+) Could
 *       Allow Code Execution (833987)
 *    Enabled with +inspect-jpegs in actions files.
 *
 *    Revision 1.58.2.6  2003/12/06 22:18:27  gliptak
 *    Correcting compile problem with FEATURE_IMAGE_BLOCKING
 *
 *    Revision 1.58.2.5  2003/11/11 13:10:31  oes
 *    Fixed bug #839859: "See why" link URL now gets url-encoded.
 *
 *    Revision 1.58.2.4  2003/02/28 12:52:45  oes
 *    Fixed a typo
 *
 *    Revision 1.58.2.3  2002/09/25 14:51:51  oes
 *    Added basic support for OPTIONS and TRACE HTTP methods:
 *    New function direct_response which handles OPTIONS and
 *    TRACE requests whose Max-Forwards header field is zero.
 *
 *    Revision 1.58.2.2  2002/08/01 17:18:28  oes
 *    Fixed BR 537651 / SR 579724 (MSIE image detect improper for IE/Mac)
 *
 *    Revision 1.58.2.1  2002/07/26 15:18:53  oes
 *    - Bugfix: Executing a filters without jobs no longer results in
 *      turing off *all* filters.
 *    - Security fix: Malicious web servers can't cause a seg fault
 *      through bogus chunk sizes anymore
 *
 *    Revision 1.58  2002/04/24 02:11:17  oes
 *    Jon's multiple AF patch: url_actions now evaluates rules
 *    from all AFs.
 *
 *    Revision 1.57  2002/04/08 20:38:34  swa
 *    fixed JB spelling
 *
 *    Revision 1.56  2002/04/05 15:51:24  oes
 *     - bugfix: error-pages now get correct request protocol
 *     - fix for invalid HTML in trust info
 *
 *    Revision 1.55  2002/04/02 16:13:51  oes
 *    Fix: No "Go there anyway" for SSL
 *
 *    Revision 1.54  2002/04/02 14:55:56  oes
 *    Bugfix: is_untrusted_url() now depends on FEATURE_TRUST, not FEATURE_COOKIE_JAR
 *
 *    Revision 1.53  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.52  2002/03/24 16:35:57  jongfoster
 *    Removing logo
 *
 *    Revision 1.51  2002/03/24 15:23:33  jongfoster
 *    Name changes
 *
 *    Revision 1.50  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.49  2002/03/16 20:29:14  oes
 *    Cosmetics
 *
 *    Revision 1.48  2002/03/13 20:25:34  oes
 *    Better logging for content filters
 *
 *    Revision 1.47  2002/03/13 00:30:52  jongfoster
 *    Killing warnings
 *    Added option of always sending redirect for imageblock,
 *    currently disabled with #if 0.
 *
 *    Revision 1.46  2002/03/12 01:42:49  oes
 *    Introduced modular filters
 *
 *    Revision 1.45  2002/03/08 16:47:50  oes
 *    Added choice beween GIF and PNG built-in images
 *
 *    Revision 1.44  2002/03/07 03:49:31  oes
 *     - Fixed compiler warnings etc
 *     - Changed built-in images from GIF to PNG
 *       (with regard to Unisys patent issue)
 *     - Added a 4x4 pattern PNG which is less intrusive
 *       than the logo but also clearly marks the deleted banners
 *
 *    Revision 1.43  2002/01/22 23:51:59  jongfoster
 *    Replacing strsav() with the safer string_append().
 *
 *    Adding missing html_encode() to error message generators.  Where encoded
 *    and unencoded versions of a string were provided, removing the unencoded
 *    one.
 *
 *    Revision 1.42  2002/01/17 21:00:32  jongfoster
 *    Moving all our URL and URL pattern parsing code to urlmatch.c.
 *
 *    Using a single, simple url_match(pattern,url) function - rather than
 *    the 3-line match routine which was repeated all over the place.
 *
 *    Renaming free_url to free_url_spec, since it frees a struct url_spec.
 *
 *    Using parse_http_url() to parse URLs without faking a HTTP
 *    request line for parse_http_request().
 *
 *    Revision 1.41  2001/11/13 00:14:07  jongfoster
 *    Fixing stupid bug now I've figured out what || means.
 *    (It always returns 0 or 1, not one of it's paramaters.)
 *
 *    Revision 1.40  2001/10/26 17:37:55  oes
 *    - Re-enabled Netscape 200/404 bug workaround in block_url():
 *      - Removed OS/2 special case
 *      - Made block_url() independant from sed() having been run
 *    - Made trust_url independant from sed() having been run
 *    - Made is_imageurl independant from sed() having been run.
 *      It now checks User-Agent: and Accept: by itself.
 *
 *
 *    Revision 1.39  2001/10/25 03:40:48  david__schmidt
 *    Change in porting tactics: OS/2's EMX porting layer doesn't allow multiple
 *    threads to call select() simultaneously.  So, it's time to do a real, live,
 *    native OS/2 port.  See defines for __EMX__ (the porting layer) vs. __OS2__
 *    (native). Both versions will work, but using __OS2__ offers multi-threading.
 *
 *    Revision 1.38  2001/10/23 21:32:33  jongfoster
 *    Adding error-checking to selected functions
 *
 *    Revision 1.37  2001/10/22 15:33:56  david__schmidt
 *    Special-cased OS/2 out of the Netscape-abort-on-404-in-js problem in
 *    filters.c.  Added a FIXME in front of the offending code.  I'll gladly
 *    put in a better/more robust fix for all parties if one is presented...
 *    It seems that just returning 200 instead of 404 would pretty much fix
 *    it for everyone, but I don't know all the history of the problem.
 *
 *    Revision 1.36  2001/10/10 16:44:16  oes
 *    Added match_portlist function
 *
 *    Revision 1.35  2001/10/07 15:41:23  oes
 *    Replaced 6 boolean members of csp with one bitmap (csp->flags)
 *
 *    New function remove_chunked_transfer_coding that strips chunked
 *      transfer coding to plain and is called by pcrs_filter_response
 *      and gif_deanimate_response if neccessary
 *
 *    Improved handling of zero-change re_filter runs
 *
 *    pcrs_filter_response and gif_deanimate_response now remove
 *      chunked transfer codeing before processing the body.
 *
 *    Revision 1.34  2001/09/20 15:49:36  steudten
 *
 *    Fix BUG: Change int size to size_t size in pcrs_filter_response().
 *    See cgi.c fill_template().
 *
 *    Revision 1.33  2001/09/16 17:05:14  jongfoster
 *    Removing unused #include showarg.h
 *
 *    Revision 1.32  2001/09/16 13:21:27  jongfoster
 *    Changes to use new list functions.
 *
 *    Revision 1.31  2001/09/16 11:38:02  jongfoster
 *    Splitting fill_template() into 2 functions:
 *    template_load() loads the file
 *    template_fill() performs the PCRS regexps.
 *    This is because the CGI edit interface has a "table row"
 *    template which is used many times in the page - this
 *    change means it's only loaded from disk once.
 *
 *    Revision 1.30  2001/09/16 11:00:10  jongfoster
 *    New function alloc_http_response, for symmetry with free_http_response
 *
 *    Revision 1.29  2001/09/13 23:32:40  jongfoster
 *    Moving image data to cgi.c rather than cgi.h
 *    Fixing a GPF under Win32 (and any other OS that protects global
 *    constants from being written to).
 *
 *    Revision 1.28  2001/09/10 10:18:51  oes
 *    Silenced compiler warnings
 *
 *    Revision 1.27  2001/08/05 16:06:20  jongfoster
 *    Modifiying "struct map" so that there are now separate header and
 *    "map_entry" structures.  This means that functions which modify a
 *    map no longer need to return a pointer to the modified map.
 *    Also, it no longer reverses the order of the entries (which may be
 *    important with some advanced template substitutions).
 *
 *    Revision 1.26  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.25  2001/07/26 10:09:46  oes
 *    Made browser detection a little less naive
 *
 *    Revision 1.24  2001/07/25 17:22:51  oes
 *    Added workaround for Netscape bug that prevents display of page when loading a component fails.
 *
 *    Revision 1.23  2001/07/23 13:40:12  oes
 *    Fixed bug that caused document body to be dropped when pcrs joblist was empty.
 *
 *    Revision 1.22  2001/07/18 12:29:34  oes
 *    - Made gif_deanimate_response respect
 *      csp->action->string[ACTION_STRING_DEANIMATE]
 *    - Logging cosmetics
 *
 *    Revision 1.21  2001/07/13 13:59:53  oes
 *     - Introduced gif_deanimate_response which shares the
 *       generic content modification interface of pcrs_filter_response
 *       and acts as a wrapper to deanimate.c:gif_deanimate()
 *     - Renamed re_process_buffer to pcrs_filter_response
 *     - pcrs_filter_response now returns NULL on failiure
 *     - Removed all #ifdef PCRS
 *
 *    Revision 1.20  2001/07/01 17:01:04  oes
 *    Added comments and missing return statement in is_untrusted_url()
 *
 *    Revision 1.19  2001/06/29 21:45:41  oes
 *    Indentation, CRLF->LF, Tab-> Space
 *
 *    Revision 1.18  2001/06/29 13:27:38  oes
 *    - Cleaned up, renamed and reorderd functions
 *      and improved comments
 *
 *    - block_url:
 *      - Ported to CGI platform. Now delivers
 *        http_response or NULL
 *      - Unified HTML and GIF generation (moved image detection
 *        and GIF generation here from jcc.c:chat())
 *      - Fixed HTTP status to:
 *       -  403 (Forbidden) for the "blocked" HTML message
 *       -  200 (OK) for GIF answers
 *       -  302 (Redirect) for redirect to GIF
 *
 *    - trust_url:
 *      - Ported to CGI platform. Now delivers
 *        http_response or NULL
 *      - Separated detection of untrusted URL into
 *        (bool)is_untrusted_url
 *      - Added enforcement of untrusted requests
 *
 *    - Moved redirect_url() from cgi.c to here
 *      and ported it to the CGI platform
 *
 *    - Removed logentry from cancelled commit
 *
 *    Revision 1.17  2001/06/09 10:55:28  jongfoster
 *    Changing BUFSIZ ==> BUFFER_SIZE
 *
 *    Revision 1.16  2001/06/07 23:10:26  jongfoster
 *    Allowing unanchored domain patterns to back off and retry
 *    if they partially match.  Optimized right-anchored patterns.
 *    Moving ACL and forward files into config file.
 *    Replacing struct gateway with struct forward_spec
 *
 *    Revision 1.15  2001/06/03 19:12:00  oes
 *    extracted-CGI relevant stuff
 *
 *    Revision 1.14  2001/06/01 10:30:55  oes
 *    Added optional left-anchoring to domaincmp
 *
 *    Revision 1.13  2001/05/31 21:21:30  jongfoster
 *    Permissionsfile / actions file changes:
 *    - Changed "permission" to "action" throughout
 *    - changes to file format to allow string parameters
 *    - Moved helper functions to actions.c
 *
 *    Revision 1.12  2001/05/31 17:35:20  oes
 *
 *     - Enhanced domain part globbing with infix and prefix asterisk
 *       matching and optional unanchored operation
 *
 *    Revision 1.11  2001/05/29 11:53:23  oes
 *    "See why" link added to "blocked" page
 *
 *    Revision 1.10  2001/05/29 09:50:24  jongfoster
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
 *    Revision 1.9  2001/05/27 22:17:04  oes
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
 *    Revision 1.8  2001/05/26 17:13:28  jongfoster
 *    Filled in a function comment.
 *
 *    Revision 1.7  2001/05/26 15:26:15  jongfoster
 *    ACL feature now provides more security by immediately dropping
 *    connections from untrusted hosts.
 *
 *    Revision 1.6  2001/05/26 00:28:36  jongfoster
 *    Automatic reloading of config file.
 *    Removed obsolete SIGHUP support (Unix) and Reload menu option (Win32).
 *    Most of the global variables have been moved to a new
 *    struct configuration_spec, accessed through csp->config->globalname
 *    Most of the globals remaining are used by the Win32 GUI.
 *
 *    Revision 1.5  2001/05/25 22:34:30  jongfoster
 *    Hard tabs->Spaces
 *
 *    Revision 1.4  2001/05/22 18:46:04  oes
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
 *    Revision 1.3  2001/05/20 16:44:47  jongfoster
 *    Removing last hardcoded Junkbusters.com URLs.
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


#include "config.h"

#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <assert.h>

#ifndef _WIN32
#ifndef __OS2__
#include <unistd.h>
#endif /* ndef __OS2__ */
#include <netinet/in.h>
#else
#include <winsock2.h>
#endif /* ndef _WIN32 */

#ifdef __OS2__
#include <utils.h>
#endif /* def __OS2__ */

#include "project.h"
#include "filters.h"
#include "encode.h"
#include "parsers.h"
#include "ssplit.h"
#include "errlog.h"
#include "jbsockets.h"
#include "miscutil.h"
#include "actions.h"
#include "cgi.h"
#include "list.h"
#include "deanimate.h"
#include "urlmatch.h"
#include "loaders.h"

#ifdef _WIN32
#include "win32.h"
#endif

const char filters_h_rcs[] = FILTERS_H_VERSION;

/* Fix a problem with Solaris.  There should be no effect on other
 * platforms.
 * Solaris's isspace() is a macro which uses it's argument directly
 * as an array index.  Therefore we need to make sure that high-bit
 * characters generate +ve values, and ideally we also want to make
 * the argument match the declared parameter type of "int".
 */
#define ijb_isdigit(__X) isdigit((int)(unsigned char)(__X))

static jb_err remove_chunked_transfer_coding(char *buffer, size_t *size);
static jb_err prepare_for_filtering(struct client_state *csp);

#ifdef FEATURE_ACL
/*********************************************************************
 *
 * Function    :  block_acl
 *
 * Description :  Block this request?
 *                Decide yes or no based on ACL file.
 *
 * Parameters  :
 *          1  :  dst = The proxy or gateway address this is going to.
 *                      Or NULL to check all possible targets.
 *          2  :  csp = Current client state (buffers, headers, etc...)
 *                      Also includes the client IP address.
 *
 * Returns     : 0 = FALSE (don't block) and 1 = TRUE (do block)
 *
 *********************************************************************/
int block_acl(const struct access_control_addr *dst, const struct client_state *csp)
{
   struct access_control_list *acl = csp->config->acl;

   /* if not using an access control list, then permit the connection */
   if (acl == NULL)
   {
      return(0);
   }

   /* search the list */
   while (acl != NULL)
   {
      if ((csp->ip_addr_long & acl->src->mask) == acl->src->addr)
      {
         if (dst == NULL)
         {
            /* Just want to check if they have any access */
            if (acl->action == ACL_PERMIT)
            {
               return(0);
            }
         }
         else if ( ((dst->addr & acl->dst->mask) == acl->dst->addr)
           && ((dst->port == acl->dst->port) || (acl->dst->port == 0)))
         {
            if (acl->action == ACL_PERMIT)
            {
               return(0);
            }
            else
            {
               return(1);
            }
         }
      }
      acl = acl->next;
   }

   return(1);

}


/*********************************************************************
 *
 * Function    :  acl_addr
 *
 * Description :  Called from `load_config' to parse an ACL address.
 *
 * Parameters  :
 *          1  :  aspec = String specifying ACL address.
 *          2  :  aca = struct access_control_addr to fill in.
 *
 * Returns     :  0 => Ok, everything else is an error.
 *
 *********************************************************************/
int acl_addr(const char *aspec, struct access_control_addr *aca)
{
   int i, masklength;
   long port;
   char *p;
   char *acl_spec = NULL;

   masklength = 32;
   port       =  0;

   /*
    * Use a temporary acl spec copy so we can log
    * the unmodified original in case of parse errors.
    */
   acl_spec = strdup(aspec);
   if (acl_spec == NULL)
   {
      /* XXX: This will be logged as parse error. */
      return(-1);
   }

   if ((p = strchr(acl_spec, '/')) != NULL)
   {
      *p++ = '\0';
      if (ijb_isdigit(*p) == 0)
      {
         freez(acl_spec);
         return(-1);
      }
      masklength = atoi(p);
   }

   if ((masklength < 0) || (masklength > 32))
   {
      freez(acl_spec);
      return(-1);
   }

   if ((p = strchr(acl_spec, ':')) != NULL)
   {
      char *endptr;

      *p++ = '\0';
      port = strtol(p, &endptr, 10);

      if (port <= 0 || port > 65535 || *endptr != '\0')
      {
         freez(acl_spec);
         return(-1);
      }
   }

   aca->port = (unsigned long)port;

   aca->addr = ntohl(resolve_hostname_to_ip(acl_spec));
   freez(acl_spec);

   if (aca->addr == INADDR_NONE)
   {
      /* XXX: This will be logged as parse error. */
      return(-1);
   }

   /* build the netmask */
   aca->mask = 0;
   for (i=1; i <= masklength ; i++)
   {
      aca->mask |= (1U << (32 - i));
   }

   /* now mask off the host portion of the ip address
    * (i.e. save on the network portion of the address).
    */
   aca->addr = aca->addr & aca->mask;

   return(0);

}
#endif /* def FEATURE_ACL */


/*********************************************************************
 *
 * Function    :  connect_port_is_forbidden
 *
 * Description :  Check to see if CONNECT requests to the destination
 *                port of this request are forbidden. The check is
 *                independend of the actual request method.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  True if yes, false otherwise.
 *
 *********************************************************************/
int connect_port_is_forbidden(const struct client_state *csp)
{
   return ((csp->action->flags & ACTION_LIMIT_CONNECT) &&
     !match_portlist(csp->action->string[ACTION_STRING_LIMIT_CONNECT],
        csp->http->port));
}


/*********************************************************************
 *
 * Function    :  block_url
 *
 * Description :  Called from `chat'.  Check to see if we need to block this.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  NULL => unblocked, else HTTP block response
 *
 *********************************************************************/
struct http_response *block_url(struct client_state *csp)
{
   struct http_response *rsp;
   const char *new_content_type = NULL;

   /*
    * If it's not blocked, don't block it ;-)
    */
   if ((csp->action->flags & ACTION_BLOCK) == 0)
   {
      return NULL;
   }
   if (csp->action->flags & ACTION_REDIRECT)
   {
      log_error(LOG_LEVEL_ERROR, "redirect{} overruled by block.");     
   }
   /*
    * Else, prepare a response
    */
   if (NULL == (rsp = alloc_http_response()))
   {
      return cgi_error_memory();
   }

   /*
    * If it's an image-url, send back an image or redirect
    * as specified by the relevant +image action
    */
#ifdef FEATURE_IMAGE_BLOCKING
   if (((csp->action->flags & ACTION_IMAGE_BLOCKER) != 0)
        && is_imageurl(csp))
   {
      char *p;
      /* determine HOW images should be blocked */
      p = csp->action->string[ACTION_STRING_IMAGE_BLOCKER];

      if(csp->action->flags & ACTION_HANDLE_AS_EMPTY_DOCUMENT)
      {
         log_error(LOG_LEVEL_ERROR, "handle-as-empty-document overruled by handle-as-image.");
      }
#if 1 /* Two alternative strategies, use this one for now: */

      /* and handle accordingly: */
      if ((p == NULL) || (0 == strcmpic(p, "pattern")))
      {
         rsp->status = strdup("403 Request blocked by Privoxy");
         if (rsp->status == NULL)
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }
         rsp->body = bindup(image_pattern_data, image_pattern_length);
         if (rsp->body == NULL)
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }
         rsp->content_length = image_pattern_length;

         if (enlist_unique_header(rsp->headers, "Content-Type", BUILTIN_IMAGE_MIMETYPE))
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }
      }

      else if (0 == strcmpic(p, "blank"))
      {
         rsp->status = strdup("403 Request blocked by Privoxy");
         if (rsp->status == NULL)
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }
         rsp->body = bindup(image_blank_data, image_blank_length);
         if (rsp->body == NULL)
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }
         rsp->content_length = image_blank_length;

         if (enlist_unique_header(rsp->headers, "Content-Type", BUILTIN_IMAGE_MIMETYPE))
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }
      }

      else
      {
         rsp->status = strdup("302 Local Redirect from Privoxy");
         if (rsp->status == NULL)
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }

         if (enlist_unique_header(rsp->headers, "Location", p))
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }
      }

#else /* Following code is disabled for now */

      /* and handle accordingly: */
      if ((p == NULL) || (0 == strcmpic(p, "pattern")))
      {
         p = CGI_PREFIX "send-banner?type=pattern";
      }
      else if (0 == strcmpic(p, "blank"))
      {
         p = CGI_PREFIX "send-banner?type=blank";
      }
      rsp->status = strdup("302 Local Redirect from Privoxy");
      if (rsp->status == NULL)
      {
         free_http_response(rsp);
         return cgi_error_memory();
      }

      if (enlist_unique_header(rsp->headers, "Location", p))
      {
         free_http_response(rsp);
         return cgi_error_memory();
      }
#endif /* Preceeding code is disabled for now */
   }
   else if(csp->action->flags & ACTION_HANDLE_AS_EMPTY_DOCUMENT)
   {
     /*
      *  Send empty document.               
      */
      new_content_type = csp->action->string[ACTION_STRING_CONTENT_TYPE];

      freez(rsp->body);
      rsp->body = strdup(" ");
      rsp->content_length = 1;

      rsp->status = strdup("403 Request blocked by Privoxy");
      if (rsp->status == NULL)
      {
         free_http_response(rsp);
         return cgi_error_memory();
      }
      if (new_content_type != 0)
      {
         log_error(LOG_LEVEL_HEADER, "Overwriting Content-Type with %s", new_content_type);
         if (enlist_unique_header(rsp->headers, "Content-Type", new_content_type))
         {
            free_http_response(rsp);
            return cgi_error_memory();
         }
      }
   }
   else
#endif /* def FEATURE_IMAGE_BLOCKING */

   /*
    * Else, generate an HTML "blocked" message:
    */
   {
      jb_err err;
      struct map * exports;
      char *p;

      /*
       * Workaround for stupid Netscape bug which prevents
       * pages from being displayed if loading a referenced
       * JavaScript or style sheet fails. So make it appear
       * as if it succeeded.
       */
      if ( NULL != (p = get_header_value(csp->headers, "User-Agent:"))
           && !strncmpic(p, "mozilla", 7) /* Catch Netscape but */
           && !strstr(p, "Gecko")         /* save Mozilla, */
           && !strstr(p, "compatible")    /* MSIE */
           && !strstr(p, "Opera"))        /* and Opera. */
      {
         rsp->status = strdup("200 Request for blocked URL");
      }
      else
      {
         rsp->status = strdup("403 Request for blocked URL");
      }

      if (rsp->status == NULL)
      {
         free_http_response(rsp);
         return cgi_error_memory();
      }

      exports = default_exports(csp, NULL);
      if (exports == NULL)
      {
         free_http_response(rsp);
         return cgi_error_memory();
      }

#ifdef FEATURE_FORCE_LOAD
      err = map(exports, "force-prefix", 1, FORCE_PREFIX, 1);
      /*
       * Export the force conditional block killer if
       *
       * - Privoxy was compiled without FEATURE_FORCE_LOAD, or
       * - Privoxy is configured to enforce blocks, or
       * - it's a CONNECT request and enforcing wouldn't work anyway.
       */
      if ((csp->config->feature_flags & RUNTIME_FEATURE_ENFORCE_BLOCKS)
       || (0 == strcmpic(csp->http->gpc, "connect")))
#endif /* ndef FEATURE_FORCE_LOAD */
      {
         err = map_block_killer(exports, "force-support");
      }

      if (!err) err = map(exports, "protocol", 1, csp->http->ssl ? "https://" : "http://", 1);
      if (!err) err = map(exports, "hostport", 1, html_encode(csp->http->hostport), 0);
      if (!err) err = map(exports, "path", 1, html_encode(csp->http->path), 0);
      if (!err) err = map(exports, "path-ue", 1, url_encode(csp->http->path), 0);
      if (!err)
      {
         const char *block_reason;
         if (csp->action->string[ACTION_STRING_BLOCK] != NULL)
         {
            block_reason = csp->action->string[ACTION_STRING_BLOCK];
         }
         else
         {
            assert(connect_port_is_forbidden(csp));
            block_reason = "Forbidden CONNECT port.";
         }
         err = map(exports, "block-reason", 1, html_encode(block_reason), 0);
      }
      if (err)
      {
         free_map(exports);
         free_http_response(rsp);
         return cgi_error_memory();
      }

      err = template_fill_for_cgi(csp, "blocked", exports, rsp);
      if (err)
      {
         free_http_response(rsp);
         return cgi_error_memory();
      }
   }
   rsp->reason = RSP_REASON_BLOCKED;

   return finish_http_response(csp, rsp);

}


#ifdef FEATURE_TRUST
/*********************************************************************
 *
 * Function    :  trust_url FIXME: I should be called distrust_url
 *
 * Description :  Calls is_untrusted_url to determine if the URL is trusted
 *                and if not, returns a HTTP 403 response with a reject message.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  NULL => trusted, else http_response.
 *
 *********************************************************************/
struct http_response *trust_url(struct client_state *csp)
{
   struct http_response *rsp;
   struct map * exports;
   char buf[BUFFER_SIZE];
   char *p;
   struct url_spec **tl;
   struct url_spec *t;
   jb_err err;

   /*
    * Don't bother to work on trusted URLs
    */
   if (!is_untrusted_url(csp))
   {
      return NULL;
   }

   /*
    * Else, prepare a response:
    */
   if (NULL == (rsp = alloc_http_response()))
   {
      return cgi_error_memory();
   }

   rsp->status = strdup("403 Request blocked by Privoxy");
   exports = default_exports(csp, NULL);
   if (exports == NULL || rsp->status == NULL)
   {
      free_http_response(rsp);
      return cgi_error_memory();
   }

   /*
    * Export the protocol, host, port, and referrer information
    */
   err = map(exports, "hostport", 1, csp->http->hostport, 1);
   if (!err) err = map(exports, "protocol", 1, csp->http->ssl ? "https://" : "http://", 1); 
   if (!err) err = map(exports, "path", 1, csp->http->path, 1);

   if (NULL != (p = get_header_value(csp->headers, "Referer:")))
   {
      if (!err) err = map(exports, "referrer", 1, html_encode(p), 0);
   }
   else
   {
      if (!err) err = map(exports, "referrer", 1, "none set", 1);
   }

   if (err)
   {
      free_map(exports);
      free_http_response(rsp);
      return cgi_error_memory();
   }

   /*
    * Export the trust list
    */
   p = strdup("");
   for (tl = csp->config->trust_list; (t = *tl) != NULL ; tl++)
   {
      snprintf(buf, sizeof(buf), "<li>%s</li>\n", t->spec);
      string_append(&p, buf);
   }
   err = map(exports, "trusted-referrers", 1, p, 0);

   if (err)
   {
      free_map(exports);
      free_http_response(rsp);
      return cgi_error_memory();
   }

   /*
    * Export the trust info, if available
    */
   if (csp->config->trust_info->first)
   {
      struct list_entry *l;

      p = strdup("");
      for (l = csp->config->trust_info->first; l ; l = l->next)
      {
         snprintf(buf, sizeof(buf), "<li> <a href=\"%s\">%s</a><br>\n", l->str, l->str);
         string_append(&p, buf);
      }
      err = map(exports, "trust-info", 1, p, 0);
   }
   else
   {
      err = map_block_killer(exports, "have-trust-info");
   }

   if (err)
   {
      free_map(exports);
      free_http_response(rsp);
      return cgi_error_memory();
   }

   /*
    * Export the force conditional block killer if
    *
    * - Privoxy was compiled without FEATURE_FORCE_LOAD, or
    * - Privoxy is configured to enforce blocks, or
    * - it's a CONNECT request and enforcing wouldn't work anyway.
    */
#ifdef FEATURE_FORCE_LOAD
   if ((csp->config->feature_flags & RUNTIME_FEATURE_ENFORCE_BLOCKS)
    || (0 == strcmpic(csp->http->gpc, "connect")))
   {
      err = map_block_killer(exports, "force-support");
   }
   else
   {
      err = map(exports, "force-prefix", 1, FORCE_PREFIX, 1);
   }
#else /* ifndef FEATURE_FORCE_LOAD */
   err = map_block_killer(exports, "force-support");
#endif /* ndef FEATURE_FORCE_LOAD */

   if (err)
   {
      free_map(exports);
      free_http_response(rsp);
      return cgi_error_memory();
   }

   /*
    * Build the response
    */
   err = template_fill_for_cgi(csp, "untrusted", exports, rsp);
   if (err)
   {
      free_http_response(rsp);
      return cgi_error_memory();
   }
   rsp->reason = RSP_REASON_UNTRUSTED;

   return finish_http_response(csp, rsp);
}
#endif /* def FEATURE_TRUST */


/*********************************************************************
 *
 * Function    :  compile_dynamic_pcrs_job_list
 *
 * Description :  Compiles a dynamic pcrs job list (one with variables
 *                resolved at request time)
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  b = The filter list to compile
 *
 * Returns     :  NULL in case of errors, otherwise the
 *                pcrs job list.  
 *
 *********************************************************************/
pcrs_job *compile_dynamic_pcrs_job_list(const struct client_state *csp, const struct re_filterfile_spec *b)
{
   struct list_entry *pattern;
   pcrs_job *job_list = NULL;
   pcrs_job *dummy = NULL;
   pcrs_job *lastjob = NULL;
   int error = 0;

   const struct pcrs_variable variables[] =
   {
      {"url",    csp->http->url,   1},
      {"path",   csp->http->path,  1},
      {"host",   csp->http->host,  1},
      {"origin", csp->ip_addr_str, 1},
      {NULL,     NULL,             1}
   };

   for (pattern = b->patterns->first; pattern != NULL; pattern = pattern->next)
   {
      assert(pattern->str != NULL);

      dummy = pcrs_compile_dynamic_command(pattern->str, variables, &error);
      if (NULL == dummy)
      {
         assert(error < 0);
         log_error(LOG_LEVEL_ERROR,
            "Adding filter job \'%s\' to dynamic filter %s failed: %s",
            pattern->str, b->name, pcrs_strerror(error));
         continue;
      }
      else
      {
         if (error == PCRS_WARN_TRUNCATION)
         {
            log_error(LOG_LEVEL_ERROR,
               "At least one of the variables in \'%s\' had to "
               "be truncated before compilation", pattern->str);
         }
         if (job_list == NULL)
         {
            job_list = dummy;
         }
         else
         {
            lastjob->next = dummy;
         }
         lastjob = dummy;
      }
   }

   return job_list;
}


/*********************************************************************
 *
 * Function    :  rewrite_url
 *
 * Description :  Rewrites a URL with a single pcrs command
 *                and returns the result if it differs from the
 *                original and isn't obviously invalid.
 *
 * Parameters  :
 *          1  :  old_url = URL to rewrite.
 *          2  :  pcrs_command = pcrs command formatted as string (s@foo@bar@)
 *
 *
 * Returns     :  NULL if the pcrs_command didn't change the url, or 
 *                the result of the modification.
 *
 *********************************************************************/
char *rewrite_url(char *old_url, const char *pcrs_command)
{
   char *new_url = NULL;
   int hits;

   assert(old_url);
   assert(pcrs_command);

   new_url = pcrs_execute_single_command(old_url, pcrs_command, &hits);

   if (hits == 0)
   {
      log_error(LOG_LEVEL_REDIRECTS,
         "pcrs command \"%s\" didn't change \"%s\".",
         pcrs_command, old_url);
      freez(new_url);
   }
   else if (hits < 0)
   {
      log_error(LOG_LEVEL_REDIRECTS,
         "executing pcrs command \"%s\" to rewrite %s failed: %s",
         pcrs_command, old_url, pcrs_strerror(hits));
      freez(new_url);
   }
   else if (strncmpic(new_url, "http://", 7) && strncmpic(new_url, "https://", 8))
   {
      log_error(LOG_LEVEL_ERROR,
         "pcrs command \"%s\" changed \"%s\" to \"%s\" (%u hi%s), "
         "but the result doesn't look like a valid URL and will be ignored.",
         pcrs_command, old_url, new_url, hits, (hits == 1) ? "t" : "ts");
      freez(new_url);
   }
   else
   {
      log_error(LOG_LEVEL_REDIRECTS,
         "pcrs command \"%s\" changed \"%s\" to \"%s\" (%u hi%s).",
         pcrs_command, old_url, new_url, hits, (hits == 1) ? "t" : "ts");
   }

   return new_url;

}


#ifdef FEATURE_FAST_REDIRECTS
/*********************************************************************
 *
 * Function    :  get_last_url
 *
 * Description :  Search for the last URL inside a string.
 *                If the string already is a URL, it will
 *                be the first URL found.
 *
 * Parameters  :
 *          1  :  subject = the string to check
 *          2  :  redirect_mode = +fast-redirect{} mode 
 *
 * Returns     :  NULL if no URL was found, or
 *                the last URL found.
 *
 *********************************************************************/
char *get_last_url(char *subject, const char *redirect_mode)
{
   char *new_url = NULL;
   char *tmp;

   assert(subject);
   assert(redirect_mode);

   subject = strdup(subject);
   if (subject == NULL)
   {
      log_error(LOG_LEVEL_ERROR, "Out of memory while searching for redirects.");
      return NULL;
   }

   if (0 == strcmpic(redirect_mode, "check-decoded-url"))
   {  
      log_error(LOG_LEVEL_REDIRECTS, "Decoding \"%s\" if necessary.", subject);
      new_url = url_decode(subject);
      if (new_url != NULL)
      {
         freez(subject);
         subject = new_url;
      }
      else
      {
         log_error(LOG_LEVEL_ERROR, "Unable to decode \"%s\".", subject);
      }
   }

   log_error(LOG_LEVEL_REDIRECTS, "Checking \"%s\" for redirects.", subject);

   /*
    * Find the last URL encoded in the request
    */
   tmp = subject;
   while ((tmp = strstr(tmp, "http://")) != NULL)
   {
      new_url = tmp++;
   }
   tmp = (new_url != NULL) ? new_url : subject;
   while ((tmp = strstr(tmp, "https://")) != NULL)
   {
      new_url = tmp++;
   }

   if ((new_url != NULL)
      && (  (new_url != subject)
         || (0 == strncmpic(subject, "http://", 7))
         || (0 == strncmpic(subject, "https://", 8))
         ))
   {
      /*
       * Return new URL if we found a redirect 
       * or if the subject already was a URL.
       *
       * The second case makes sure that we can
       * chain get_last_url after another redirection check
       * (like rewrite_url) without losing earlier redirects.
       */
      new_url = strdup(new_url);
      freez(subject);
      return new_url;
   }

   freez(subject);
   return NULL;

}
#endif /* def FEATURE_FAST_REDIRECTS */


/*********************************************************************
 *
 * Function    :  redirect_url
 *
 * Description :  Checks if Privoxy should answer the request with
 *                a HTTP redirect and generates the redirect if
 *                necessary.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  NULL if the request can pass, HTTP redirect otherwise.
 *
 *********************************************************************/
struct http_response *redirect_url(struct client_state *csp)
{
   struct http_response *rsp;
#ifdef FEATURE_FAST_REDIRECTS
   /*
    * XXX: Do we still need FEATURE_FAST_REDIRECTS
    * as compile-time option? The user can easily disable
    * it in his action file.
    */
   char * redirect_mode;
#endif /* def FEATURE_FAST_REDIRECTS */
   char *old_url = NULL;
   char *new_url = NULL;
   char *redirection_string;

   if ((csp->action->flags & ACTION_REDIRECT))
   {
      redirection_string = csp->action->string[ACTION_STRING_REDIRECT];

      /*
       * If the redirection string begins with 's',
       * assume it's a pcrs command, otherwise treat it as
       * properly formatted URL and use it for the redirection
       * directly.
       *
       * According to RFC 2616 section 14.30 the URL
       * has to be absolute and if the user tries:
       * +redirect{shit/this/will/be/parsed/as/pcrs_command.html}
       * she would get undefined results anyway.
       *
       */

      if (*redirection_string == 's')
      {
         old_url = csp->http->url;
         new_url = rewrite_url(old_url, redirection_string);
      }
      else
      {
         log_error(LOG_LEVEL_REDIRECTS,
            "No pcrs command recognized, assuming that \"%s\" is already properly formatted.",
            redirection_string);
         new_url = strdup(redirection_string);
      }
   }

#ifdef FEATURE_FAST_REDIRECTS
   if ((csp->action->flags & ACTION_FAST_REDIRECTS))
   {
      redirect_mode = csp->action->string[ACTION_STRING_FAST_REDIRECTS];

      /*
       * If it exists, use the previously rewritten URL as input
       * otherwise just use the old path.
       */
      old_url = (new_url != NULL) ? new_url : strdup(csp->http->path);
      new_url = get_last_url(old_url, redirect_mode);
      freez(old_url);
   }

   /*
    * Disable redirect checkers, so that they
    * will be only run more than once if the user
    * also enables them through tags.
    *
    * From a performance point of view
    * it doesn't matter, but the duplicated
    * log messages are annoying.
    */
   csp->action->flags &= ~ACTION_FAST_REDIRECTS;
#endif /* def FEATURE_FAST_REDIRECTS */
   csp->action->flags &= ~ACTION_REDIRECT;

   /* Did any redirect action trigger? */   
   if (new_url)
   {
      if (0 == strcmpic(new_url, csp->http->url))
      {
         log_error(LOG_LEVEL_ERROR,
            "New URL \"%s\" and old URL \"%s\" are the same. Redirection loop prevented.",
            csp->http->url, new_url);
            freez(new_url);
      }
      else
      {
         log_error(LOG_LEVEL_REDIRECTS, "New URL is: %s", new_url);

         if (NULL == (rsp = alloc_http_response()))
         {
            freez(new_url);
            return cgi_error_memory();
         }

         if ( enlist_unique_header(rsp->headers, "Location", new_url)
           || (NULL == (rsp->status = strdup("302 Local Redirect from Privoxy"))) )
         {
            freez(new_url);
            free_http_response(rsp);
            return cgi_error_memory();
         }
         rsp->reason = RSP_REASON_REDIRECTED;
         freez(new_url);

         return finish_http_response(csp, rsp);
      }
   }

   /* Only reached if no redirect is required */
   return NULL;

}


#ifdef FEATURE_IMAGE_BLOCKING
/*********************************************************************
 *
 * Function    :  is_imageurl
 *
 * Description :  Given a URL, decide whether it is an image or not,
 *                using either the info from a previous +image action
 *                or, #ifdef FEATURE_IMAGE_DETECT_MSIE, and the browser
 *                is MSIE and not on a Mac, tell from the browser's accept
 *                header.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  True (nonzero) if URL is an image, false (0)
 *                otherwise
 *
 *********************************************************************/
int is_imageurl(const struct client_state *csp)
{
#ifdef FEATURE_IMAGE_DETECT_MSIE
   char *tmp;

   tmp = get_header_value(csp->headers, "User-Agent:");
   if (tmp && strstr(tmp, "MSIE") && !strstr(tmp, "Mac_"))
   {
      tmp = get_header_value(csp->headers, "Accept:");
      if (tmp && strstr(tmp, "image/gif"))
      {
         /* Client will accept HTML.  If this seems counterintuitive,
          * blame Microsoft.
          */
         return(0);
      }
      else
      {
         return(1);
      }
   }
#endif /* def FEATURE_IMAGE_DETECT_MSIE */

   return ((csp->action->flags & ACTION_IMAGE) != 0);

}
#endif /* def FEATURE_IMAGE_BLOCKING */


#ifdef FEATURE_TRUST
/*********************************************************************
 *
 * Function    :  is_untrusted_url
 *
 * Description :  Should we "distrust" this URL (and block it)?
 *
 *                Yes if it matches a line in the trustfile, or if the
 *                    referrer matches a line starting with "+" in the
 *                    trustfile.
 *                No  otherwise.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  0 => trusted, 1 => untrusted
 *
 *********************************************************************/
int is_untrusted_url(const struct client_state *csp)
{
   struct file_list *fl;
   struct block_spec *b;
   struct url_spec **trusted_url;
   struct http_request rhttp[1];
   const char * referer;
   jb_err err;

   /*
    * If we don't have a trustlist, we trust everybody
    */
   if (((fl = csp->tlist) == NULL) || ((b  = fl->f) == NULL))
   {
      return 0;
   }

   memset(rhttp, '\0', sizeof(*rhttp));

   /*
    * Do we trust the request URL itself?
    */
   for (b = b->next; b ; b = b->next)
   {
      if (url_match(b->url, csp->http))
      {
         return b->reject;
      }
   }

   if (NULL == (referer = get_header_value(csp->headers, "Referer:")))
   {
      /* no referrer was supplied */
      return 1;
   }


   /*
    * If not, do we maybe trust its referrer?
    */
   err = parse_http_url(referer, rhttp, REQUIRE_PROTOCOL);
   if (err)
   {
      return 1;
   }

   for (trusted_url = csp->config->trust_list; *trusted_url != NULL; trusted_url++)
   {
      if (url_match(*trusted_url, rhttp))
      {
         /* if the URL's referrer is from a trusted referrer, then
          * add the target spec to the trustfile as an unblocked
          * domain and return 0 (which means it's OK).
          */

         FILE *fp;

         if (NULL != (fp = fopen(csp->config->trustfile, "a")))
         {
            char * path;
            char * path_end;
            char * new_entry = strdup("~");

            string_append(&new_entry, csp->http->hostport);

            path = csp->http->path;
            if ( (path[0] == '/')
              && (path[1] == '~')
              && ((path_end = strchr(path + 2, '/')) != NULL))
            {
               /* since this path points into a user's home space
                * be sure to include this spec in the trustfile.
                */
               long path_len = path_end - path; /* save offset */
               path = strdup(path); /* Copy string */
               if (path != NULL)
               {
                  path_end = path + path_len; /* regenerate ptr to new buffer */
                  *(path_end + 1) = '\0'; /* Truncate path after '/' */
               }
               string_join(&new_entry, path);
            }

            /*
             * Give a reason for generating this entry.
             */
            string_append(&new_entry, " # Trusted referrer was: ");
            string_append(&new_entry, referer);

            if (new_entry != NULL)
            {
               if (-1 == fprintf(fp, "%s\n", new_entry))
               {
                  log_error(LOG_LEVEL_ERROR, "Failed to append \'%s\' to trustfile \'%s\': %E",
                     new_entry, csp->config->trustfile);
               }
               freez(new_entry);
            }
            else
            {
               /* FIXME: No way to handle out-of memory, so mostly ignoring it */
               log_error(LOG_LEVEL_ERROR, "Out of memory adding pattern to trust file");
            }

            fclose(fp);
         }
         else
         {
            log_error(LOG_LEVEL_ERROR, "Failed to append new entry for \'%s\' to trustfile \'%s\': %E",
               csp->http->hostport, csp->config->trustfile);
         }
         return 0;
      }
   }

   return 1;
}
#endif /* def FEATURE_TRUST */


/*********************************************************************
 *
 * Function    :  pcrs_filter_response
 *
 * Description :  Execute all text substitutions from all applying
 *                +filter actions on the text buffer that's been
 *                accumulated in csp->iob->buf.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  a pointer to the (newly allocated) modified buffer.
 *                or NULL if there were no hits or something went wrong
 *
 *********************************************************************/
static char *pcrs_filter_response(struct client_state *csp)
{
   int hits=0;
   size_t size, prev_size;

   char *old = NULL;
   char *new = NULL;
   pcrs_job *job;

   struct file_list *fl;
   struct re_filterfile_spec *b;
   struct list_entry *filtername;

   int i, found_filters = 0;

   /* 
    * Sanity first
    */
   if (csp->iob->cur >= csp->iob->eod)
   {
      return(NULL);
   }

   /*
    * Need to check the set of re_filterfiles...
    */
   for (i = 0; i < MAX_AF_FILES; i++)
   {
      fl = csp->rlist[i];
      if (NULL != fl)
      {
         if (NULL != fl->f)
         {
           found_filters = 1;
           break;
         }
      }
   }

   if (0 == found_filters)
   {
      log_error(LOG_LEVEL_ERROR, "Inconsistent configuration: "
         "content filtering enabled, but no content filters available.");
      return(NULL);
   }

   size = (size_t)(csp->iob->eod - csp->iob->cur);
   old = csp->iob->cur;

   for (i = 0; i < MAX_AF_FILES; i++)
   {
     fl = csp->rlist[i];
     if ((NULL == fl) || (NULL == fl->f))
     {
        /*
         * Either there are no filter files
         * left, or this filter file just
         * contains no valid filters.
         *
         * Continue to be sure we don't miss
         * valid filter files that are chained
         * after empty or invalid ones.
         */
        continue;
     }
   /*
    * For all applying +filter actions, look if a filter by that
    * name exists and if yes, execute it's pcrs_joblist on the
    * buffer.
    */
   for (b = fl->f; b; b = b->next)
   {
      if (b->type != FT_CONTENT_FILTER)
      {
         /* Skip header filters */
         continue;
      }

      for (filtername = csp->action->multi[ACTION_MULTI_FILTER]->first;
           filtername ; filtername = filtername->next)
      {
         if (strcmp(b->name, filtername->str) == 0)
         {
            int current_hits = 0; /* Number of hits caused by this filter */
            int job_number   = 0; /* Which job we're currently executing  */
            int job_hits     = 0; /* How many hits the current job caused */
            pcrs_job *joblist = b->joblist;

            if (b->dynamic) joblist = compile_dynamic_pcrs_job_list(csp, b);

            if (NULL == joblist)
            {
               log_error(LOG_LEVEL_RE_FILTER, "Filter %s has empty joblist. Nothing to do.", b->name);
               continue;
            }

            prev_size = size;
            /* Apply all jobs from the joblist */
            for (job = joblist; NULL != job; job = job->next)
            {
               job_number++;
               job_hits = pcrs_execute(job, old, size, &new, &size);

               if (job_hits >= 0)
               {
                  /*
                   * That went well. Continue filtering
                   * and use the result of this job as
                   * input for the next one.
                   */
                  current_hits += job_hits;
                  if (old != csp->iob->cur)
                  {
                     freez(old);
                  }
                  old = new;
               }
               else
               {
                  /*
                   * This job caused an unexpected error. Inform the user
                   * and skip the rest of the jobs in this filter. We could
                   * continue with the next job, but usually the jobs
                   * depend on each other or are similar enough to
                   * fail for the same reason.
                   *
                   * At the moment our pcrs expects the error codes of pcre 3.4,
                   * but newer pcre versions can return additional error codes.
                   * As a result pcrs_strerror()'s error message might be
                   * "Unknown error ...", therefore we print the numerical value
                   * as well.
                   *
                   * XXX: Is this important enough for LOG_LEVEL_ERROR or
                   * should we use LOG_LEVEL_RE_FILTER instead?
                   */
                  log_error(LOG_LEVEL_ERROR, "Skipped filter \'%s\' after job number %u: %s (%d)",
                     b->name, job_number, pcrs_strerror(job_hits), job_hits);
                  break;
               }
            }

            if (b->dynamic) pcrs_free_joblist(joblist);

            log_error(LOG_LEVEL_RE_FILTER,
               "filtering %s%s (size %d) with \'%s\' produced %d hits (new size %d).",
               csp->http->hostport, csp->http->path, prev_size, b->name, current_hits, size);

            hits += current_hits;
         }
      }
   }
   }

   /*
    * If there were no hits, destroy our copy and let
    * chat() use the original in csp->iob
    */
   if (!hits)
   {
      freez(new);
      return(NULL);
   }

   csp->flags |= CSP_FLAG_MODIFIED;
   csp->content_length = size;
   IOB_RESET(csp);

   return(new);

}


/*********************************************************************
 *
 * Function    :  gif_deanimate_response
 *
 * Description :  Deanimate the GIF image that has been accumulated in
 *                csp->iob->buf, set csp->content_length to the modified
 *                size and raise the CSP_FLAG_MODIFIED flag.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  a pointer to the (newly allocated) modified buffer.
 *                or NULL in case something went wrong.
 *
 *********************************************************************/
static char *gif_deanimate_response(struct client_state *csp)
{
   struct binbuffer *in, *out;
   char *p;
   size_t size;

   size = (size_t)(csp->iob->eod - csp->iob->cur);

   if (  (NULL == (in =  (struct binbuffer *)zalloc(sizeof *in )))
      || (NULL == (out = (struct binbuffer *)zalloc(sizeof *out))) )
   {
      log_error(LOG_LEVEL_DEANIMATE, "failed! (no mem)");
      return NULL;
   }

   in->buffer = csp->iob->cur;
   in->size = size;

   if (gif_deanimate(in, out, strncmp("last", csp->action->string[ACTION_STRING_DEANIMATE], 4)))
   {
      log_error(LOG_LEVEL_DEANIMATE, "failed! (gif parsing)");
      freez(in);
      buf_free(out);
      return(NULL);
   }
   else
   {
      if ((int)size == out->offset)
      {
         log_error(LOG_LEVEL_DEANIMATE, "GIF not changed.");
      }
      else
      {
         log_error(LOG_LEVEL_DEANIMATE, "Success! GIF shrunk from %d bytes to %d.", size, out->offset);
      }
      csp->content_length = out->offset;
      csp->flags |= CSP_FLAG_MODIFIED;
      p = out->buffer;
      freez(in);
      freez(out);
      return(p);
   }

}


/*********************************************************************
 *
 * Function    :  get_filter_function
 *
 * Description :  Decides which content filter function has
 *                to be applied (if any).
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  The content filter function to run, or
 *                NULL if no content filter is active
 *
 *********************************************************************/
filter_function_ptr get_filter_function(struct client_state *csp)
{
   filter_function_ptr filter_function = NULL;

   /*
    * Are we enabling text mode by force?
    */
   if (csp->action->flags & ACTION_FORCE_TEXT_MODE)
   {
      /*
       * Do we really have to?
       */
      if (csp->content_type & CT_TEXT)
      {
         log_error(LOG_LEVEL_HEADER, "Text mode is already enabled.");   
      }
      else
      {
         csp->content_type |= CT_TEXT;
         log_error(LOG_LEVEL_HEADER, "Text mode enabled by force. Take cover!");   
      }
   }

   if (!(csp->content_type & CT_DECLARED))
   {
      /*
       * The server didn't bother to declare a MIME-Type.
       * Assume it's text that can be filtered.
       *
       * This also regulary happens with 304 responses,
       * therefore logging anything here would cause
       * too much noise.
       */
      csp->content_type |= CT_TEXT;
   }

   /*
    * Choose the applying filter function based on
    * the content type and action settings.
    */
   if ((csp->content_type & CT_TEXT) &&
       (csp->rlist != NULL) &&
       (!list_is_empty(csp->action->multi[ACTION_MULTI_FILTER])))
   {
      filter_function = pcrs_filter_response;
   }
   else if ((csp->content_type & CT_GIF)  &&
            (csp->action->flags & ACTION_DEANIMATE))
   {
      filter_function = gif_deanimate_response;
   }

   return filter_function;
}


/*********************************************************************
 *
 * Function    :  remove_chunked_transfer_coding
 *
 * Description :  In-situ remove the "chunked" transfer coding as defined
 *                in rfc2616 from a buffer.
 *
 * Parameters  :
 *          1  :  buffer = Pointer to the text buffer
 *          2  :  size =  In: Number of bytes to be processed,
 *                       Out: Number of bytes after de-chunking.
 *                       (undefined in case of errors)
 *
 * Returns     :  JB_ERR_OK for success,
 *                JB_ERR_PARSE otherwise
 *
 *********************************************************************/
static jb_err remove_chunked_transfer_coding(char *buffer, size_t *size)
{
   size_t newsize = 0;
   unsigned int chunksize = 0;
   char *from_p, *to_p;

   assert(buffer);
   from_p = to_p = buffer;

   if (sscanf(buffer, "%x", &chunksize) != 1)
   {
      log_error(LOG_LEVEL_ERROR, "Invalid first chunksize while stripping \"chunked\" transfer coding");
      return JB_ERR_PARSE;
   }

   while (chunksize > 0U)
   {
      if (NULL == (from_p = strstr(from_p, "\r\n")))
      {
         log_error(LOG_LEVEL_ERROR, "Parse error while stripping \"chunked\" transfer coding");
         return JB_ERR_PARSE;
      }

      if ((newsize += chunksize) >= *size)
      {
         log_error(LOG_LEVEL_ERROR,
            "Chunk size %d exceeds buffer size %d in  \"chunked\" transfer coding",
            chunksize, *size);
         return JB_ERR_PARSE;
      }
      from_p += 2;

      memmove(to_p, from_p, (size_t) chunksize);
      to_p = buffer + newsize;
      from_p += chunksize + 2;

      if (sscanf(from_p, "%x", &chunksize) != 1)
      {
         log_error(LOG_LEVEL_INFO, "Invalid \"chunked\" transfer encoding detected and ignored.");
         break;
      }
   }
   
   /* XXX: Should get its own loglevel. */
   log_error(LOG_LEVEL_RE_FILTER, "De-chunking successful. Shrunk from %d to %d", *size, newsize);

   *size = newsize;

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  prepare_for_filtering
 *
 * Description :  If necessary, de-chunks and decompresses
 *                the content so it can get filterd.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK for success,
 *                JB_ERR_PARSE otherwise
 *
 *********************************************************************/
static jb_err prepare_for_filtering(struct client_state *csp)
{
   jb_err err = JB_ERR_OK;

   /*
    * If the body has a "chunked" transfer-encoding,
    * get rid of it, adjusting size and iob->eod
    */
   if (csp->flags & CSP_FLAG_CHUNKED)
   {
      size_t size = (size_t)(csp->iob->eod - csp->iob->cur);

      log_error(LOG_LEVEL_RE_FILTER, "Need to de-chunk first");
      err = remove_chunked_transfer_coding(csp->iob->cur, &size);
      if (JB_ERR_OK == err)
      {
         csp->iob->eod = csp->iob->cur + size;
         csp->flags |= CSP_FLAG_MODIFIED;
      }
      else
      {
         return JB_ERR_PARSE;
      }
   }

#ifdef FEATURE_ZLIB
   /*
    * If the body has a supported transfer-encoding,
    * decompress it, adjusting size and iob->eod.
    */
   if (csp->content_type & (CT_GZIP|CT_DEFLATE))
   {
      if (0 == csp->iob->eod - csp->iob->cur)
      {
         /* Nothing left after de-chunking. */
         return JB_ERR_OK;
      }

      err = decompress_iob(csp);

      if (JB_ERR_OK == err)
      {
         csp->flags |= CSP_FLAG_MODIFIED;
         csp->content_type &= ~CT_TABOO;
      }
      else
      {
         /*
          * Unset CT_GZIP and CT_DEFLATE to remember not
          * to modify the Content-Encoding header later.
          */
         csp->content_type &= ~CT_GZIP;
         csp->content_type &= ~CT_DEFLATE;
      }
   }
#endif

   return err;
}


/*********************************************************************
 *
 * Function    :  execute_content_filter
 *
 * Description :  Executes a given content filter.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  content_filter = The filter function to execute
 *
 * Returns     :  Pointer to the modified buffer, or
 *                NULL if filtering failed or wasn't necessary.
 *
 *********************************************************************/
char *execute_content_filter(struct client_state *csp, filter_function_ptr content_filter)
{
   if (0 == csp->iob->eod - csp->iob->cur)
   {
      /*
       * No content (probably status code 301, 302 ...),
       * no filtering necessary.
       */
      return NULL;
   }

   if (JB_ERR_OK != prepare_for_filtering(csp))
   {
      /*
       * failed to de-chunk or decompress.
       */
      return NULL;
   }

   if (0 == csp->iob->eod - csp->iob->cur)
   {
      /*
       * Clown alarm: chunked and/or compressed nothing delivered.
       */
      return NULL;
   }

   return ((*content_filter)(csp));
}


/*********************************************************************
 *
 * Function    :  get_url_actions
 *
 * Description :  Gets the actions for this URL.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  http = http_request request for blocked URLs
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void get_url_actions(struct client_state *csp, struct http_request *http)
{
   struct file_list *fl;
   struct url_actions *b;
   int i;

   init_current_action(csp->action);

   for (i = 0; i < MAX_AF_FILES; i++)
   {
      if (((fl = csp->actions_list[i]) == NULL) || ((b = fl->f) == NULL))
      {
         return;
      }

      apply_url_actions(csp->action, http, b);
   }

   return;
}


/*********************************************************************
 *
 * Function    :  apply_url_actions
 *
 * Description :  Applies a list of URL actions.
 *
 * Parameters  :
 *          1  :  action = Destination.
 *          2  :  http = Current URL
 *          3  :  b = list of URL actions to apply
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void apply_url_actions(struct current_action_spec *action,
                       struct http_request *http,
                       struct url_actions *b)
{
   if (b == NULL)
   {
      /* Should never happen */
      return;
   }

   for (b = b->next; NULL != b; b = b->next)
   {
      if (url_match(b->url, http))
      {
         merge_current_action(action, b->action);
      }
   }
}


/*********************************************************************
 *
 * Function    :  get_forward_override_settings
 *
 * Description :  Returns forward settings as specified with the
 *                forward-override{} action. forward-override accepts
 *                forward lines similar to the one used in the
 *                configuration file, but without the URL pattern.
 *
 *                For example:
 *
 *                   forward / .
 *
 *                in the configuration file can be replaced with
 *                the action section:
 *
 *                 {+forward-override{forward .}}
 *                 /
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  Pointer to forwarding structure in case of success.
 *                Invalid syntax is fatal.
 *
 *********************************************************************/
const static struct forward_spec *get_forward_override_settings(struct client_state *csp)
{
   const char *forward_override_line = csp->action->string[ACTION_STRING_FORWARD_OVERRIDE];
   char forward_settings[BUFFER_SIZE];
   char *http_parent = NULL;
   /* variable names were chosen for consistency reasons. */
   struct forward_spec *fwd = NULL;
   int vec_count;
   char *vec[3];

   assert(csp->action->flags & ACTION_FORWARD_OVERRIDE);
   /* Should be enforced by load_one_actions_file() */
   assert(strlen(forward_override_line) < sizeof(forward_settings) - 1);

   /* Create a copy ssplit can modify */
   strlcpy(forward_settings, forward_override_line, sizeof(forward_settings));

   if (NULL != csp->fwd)
   {
      /*
       * XXX: Currently necessary to prevent memory
       * leaks when the show-url-info cgi page is visited.
       */
      unload_forward_spec(csp->fwd);
   }

   /*
    * allocate a new forward node, valid only for
    * the lifetime of this request. Save its location
    * in csp as well, so sweep() can free it later on.
    */
   fwd = csp->fwd = zalloc(sizeof(*fwd));
   if (NULL == fwd)
   {
      log_error(LOG_LEVEL_FATAL,
         "can't allocate memory for forward-override{%s}", forward_override_line);
      /* Never get here - LOG_LEVEL_FATAL causes program exit */
      return NULL;
   }

   vec_count = ssplit(forward_settings, " \t", vec, SZ(vec), 1, 1);
   if ((vec_count == 2) && !strcasecmp(vec[0], "forward"))
   {
      fwd->type = SOCKS_NONE;

      /* Parse the parent HTTP proxy host:port */
      http_parent = vec[1];

   }
   else if (vec_count == 3)
   {
      char *socks_proxy = NULL;

      if  (!strcasecmp(vec[0], "forward-socks4"))
      {
         fwd->type = SOCKS_4;
         socks_proxy = vec[1];
      }
      else if (!strcasecmp(vec[0], "forward-socks4a"))
      {
         fwd->type = SOCKS_4A;
         socks_proxy = vec[1];
      }
      else if (!strcasecmp(vec[0], "forward-socks5"))
      {
         fwd->type = SOCKS_5;
         socks_proxy = vec[1];
      }

      if (NULL != socks_proxy)
      {
         /* Parse the SOCKS proxy host[:port] */
         fwd->gateway_host = strdup(socks_proxy);

         if (NULL != (socks_proxy = strchr(fwd->gateway_host, ':')))
         {
            *socks_proxy++ = '\0';
            fwd->gateway_port = (int)strtol(socks_proxy, NULL, 0);
         }

         if (fwd->gateway_port <= 0)
         {
            fwd->gateway_port = 1080;
         }

         http_parent = vec[2];
      }
   }

   if (NULL == http_parent)
   {
      log_error(LOG_LEVEL_FATAL,
         "Invalid forward-override syntax in: %s", forward_override_line);
      /* Never get here - LOG_LEVEL_FATAL causes program exit */
   }

   /* Parse http forwarding settings */
   if (strcmp(http_parent, ".") != 0)
   {
      fwd->forward_host = strdup(http_parent);

      if (NULL != (http_parent = strchr(fwd->forward_host, ':')))
      {
         *http_parent++ = '\0';
         fwd->forward_port = (int)strtol(http_parent, NULL, 0);
      }

      if (fwd->forward_port <= 0)
      {
         fwd->forward_port = 8000;
      }
   }

   assert (NULL != fwd);

   log_error(LOG_LEVEL_CONNECT,
      "Overriding forwarding settings based on \'%s\'", forward_override_line);

   return fwd;
}


/*********************************************************************
 *
 * Function    :  forward_url
 *
 * Description :  Should we forward this to another proxy?
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  http = http_request request for current URL
 *
 * Returns     :  Pointer to forwarding information.
 *
 *********************************************************************/
const struct forward_spec *forward_url(struct client_state *csp,
                                       const struct http_request *http)
{
   static const struct forward_spec fwd_default[1] = { FORWARD_SPEC_INITIALIZER };
   struct forward_spec *fwd = csp->config->forward;

   if (csp->action->flags & ACTION_FORWARD_OVERRIDE)
   {
      return get_forward_override_settings(csp);
   }

   if (fwd == NULL)
   {
      return fwd_default;
   }

   while (fwd != NULL)
   {
      if (url_match(fwd->url, http))
      {
         return fwd;
      }
      fwd = fwd->next;
   }

   return fwd_default;
}


/*********************************************************************
 *
 * Function    :  direct_response 
 *
 * Description :  Check if Max-Forwards == 0 for an OPTIONS or TRACE
 *                request and if so, return a HTTP 501 to the client.
 *
 *                FIXME: I have a stupid name and I should handle the
 *                requests properly. Still, what we do here is rfc-
 *                compliant, whereas ignoring or forwarding are not.
 *
 * Parameters  :  
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  http_response if , NULL if nonmatch or handler fail
 *
 *********************************************************************/
struct http_response *direct_response(struct client_state *csp)
{
   struct http_response *rsp;
   struct list_entry *p;

   if ((0 == strcmpic(csp->http->gpc, "trace"))
      || (0 == strcmpic(csp->http->gpc, "options")))
   {
      for (p = csp->headers->first; (p != NULL) ; p = p->next)
      {
         if (!strncmpic("Max-Forwards:", p->str, 13))
         {
            unsigned int max_forwards;

            /*
             * If it's a Max-Forwards value of zero,
             * we have to intercept the request.
             */
            if (1 == sscanf(p->str+12, ": %u", &max_forwards) && max_forwards == 0)
            {
               /*
                * FIXME: We could handle at least TRACE here,
                * but that would require a verbatim copy of
                * the request which we don't have anymore
                */
                log_error(LOG_LEVEL_HEADER,
                  "Detected header \'%s\' in OPTIONS or TRACE request. Returning 501.",
                  p->str);

               /* Get mem for response or fail*/
               if (NULL == (rsp = alloc_http_response()))
               {
                  return cgi_error_memory();
               }
            
               if (NULL == (rsp->status = strdup("501 Not Implemented")))
               {
                  free_http_response(rsp);
                  return cgi_error_memory();
               }

               rsp->is_static = 1;
               rsp->reason = RSP_REASON_UNSUPPORTED;

               return(finish_http_response(csp, rsp));
            }
         }
      }
   }
   return NULL;
}


/*********************************************************************
 *
 * Function    :  content_filters_enabled
 *
 * Description :  Checks whether there are any content filters
 *                enabled for the current request.
 *
 * Parameters  :  
 *          1  :  action = Action spec to check.
 *
 * Returns     :  TRUE for yes, FALSE otherwise
 *
 *********************************************************************/
int content_filters_enabled(const struct current_action_spec *action)
{
   return ((action->flags & ACTION_DEANIMATE) ||
      !list_is_empty(action->multi[ACTION_MULTI_FILTER]));
}

/*
  Local Variables:
  tab-width: 3
  end:
*/
