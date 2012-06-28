#ifndef PARSERS_H_INCLUDED
#define PARSERS_H_INCLUDED
#define PARSERS_H_VERSION "$Id: parsers.h,v 1.49 2009/03/13 14:10:07 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/parsers.h,v $
 *
 * Purpose     :  Declares functions to parse/crunch headers and pages.
 *                Functions declared include:
 *                   `add_to_iob', `client_cookie_adder', `client_from',
 *                   `client_referrer', `client_send_cookie', `client_ua',
 *                   `client_uagent', `client_x_forwarded',
 *                   `client_x_forwarded_adder', `client_xtra_adder',
 *                   `content_type', `crumble', `destroy_list', `enlist',
 *                   `flush_socket', `free_http_request', `get_header',
 *                   `list_to_text', `parse_http_request', `sed',
 *                   and `server_set_cookie'.
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
 *    $Log: parsers.h,v $
 *    Revision 1.49  2009/03/13 14:10:07  fabiankeil
 *    Fix some more harmless warnings on amd64.
 *
 *    Revision 1.48  2008/05/30 15:57:23  fabiankeil
 *    Remove now-useless reference to debug.
 *
 *    Revision 1.47  2008/05/21 20:12:11  fabiankeil
 *    The whole point of strclean() is to modify the
 *    first parameter, so don't mark it immutable,
 *    even though the compiler lets us get away with it.
 *
 *    Revision 1.46  2008/05/21 15:47:14  fabiankeil
 *    Streamline sed()'s prototype and declare
 *    the header parse and add structures static.
 *
 *    Revision 1.45  2008/05/20 20:13:30  fabiankeil
 *    Factor update_server_headers() out of sed(), ditch the
 *    first_run hack and make server_patterns_light static.
 *
 *    Revision 1.44  2008/05/20 16:05:09  fabiankeil
 *    Move parsers structure definition from project.h to parsers.h.
 *
 *    Revision 1.43  2008/05/10 13:23:38  fabiankeil
 *    Don't provide get_header() with the whole client state
 *    structure when it only needs access to csp->iob.
 *
 *    Revision 1.42  2008/04/17 14:40:49  fabiankeil
 *    Provide get_http_time() with the buffer size so it doesn't
 *    have to blindly assume that the buffer is big enough.
 *
 *    Revision 1.41  2008/04/16 16:38:21  fabiankeil
 *    Don't pass the whole csp structure to flush_socket()
 *    when it only needs a file descriptor and a buffer.
 *
 *    Revision 1.40  2007/08/11 14:47:26  fabiankeil
 *    Remove the prototypes for functions that are only
 *    used in parsers.c and thus should be static.
 *
 *    Revision 1.39  2007/06/01 16:31:55  fabiankeil
 *    Change sed() to return a jb_err in preparation for forward-override{}.
 *
 *    Revision 1.38  2007/03/25 14:27:11  fabiankeil
 *    Let parse_header_time() return a jb_err code
 *    instead of a pointer that can only be used to
 *    check for NULL anyway.
 *
 *    Revision 1.37  2007/03/20 15:22:17  fabiankeil
 *    - Remove filter_client_header() and filter_client_header(),
 *      filter_header() now checks the shiny new
 *      CSP_FLAG_CLIENT_HEADER_PARSING_DONE flag instead.
 *
 *    Revision 1.36  2007/03/05 13:25:32  fabiankeil
 *    - Cosmetical changes for LOG_LEVEL_RE_FILTER messages.
 *    - Handle "Cookie:" and "Connection:" headers a bit smarter
 *      (don't crunch them just to recreate them later on).
 *    - Add another non-standard time format for the cookie
 *      expiration date detection.
 *    - Fix a valgrind warning.
 *
 *    Revision 1.35  2007/01/01 19:36:37  fabiankeil
 *    Integrate a modified version of Wil Mahan's
 *    zlib patch (PR #895531).
 *
 *    Revision 1.34  2006/12/29 19:08:22  fabiankeil
 *    Reverted parts of my last commit
 *    to keep error handling working.
 *
 *    Revision 1.33  2006/12/29 18:04:40  fabiankeil
 *    Fixed gcc43 conversion warnings.
 *
 *    Revision 1.32  2006/12/06 19:14:23  fabiankeil
 *    Added prototype for get_destination_from_headers().
 *
 *    Revision 1.31  2006/08/17 17:15:10  fabiankeil
 *    - Back to timegm() using GnuPG's replacement if necessary.
 *      Using mktime() and localtime() could add a on hour offset if
 *      the randomize factor was big enough to lead to a summer/wintertime
 *      switch.
 *
 *    - Removed now-useless Privoxy 3.0.3 compatibility glue.
 *
 *    - Moved randomization code into pick_from_range().
 *
 *    - Changed parse_header_time definition.
 *      time_t isn't guaranteed to be signed and
 *      if it isn't, -1 isn't available as error code.
 *      Changed some variable types in client_if_modified_since()
 *      because of the same reason.
 *
 *    Revision 1.30  2006/08/14 08:25:19  fabiankeil
 *    Split filter-headers{} into filter-client-headers{}
 *    and filter-server-headers{}.
 *    Added parse_header_time() to share some code.
 *    Replaced timegm() with mktime().
 *
 *    Revision 1.29  2006/08/03 02:46:41  david__schmidt
 *    Incorporate Fabian Keil's patch work:http://www.fabiankeil.de/sourcecode/privoxy/
 *
 *    Revision 1.28  2006/07/18 14:48:47  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.26.2.1  2002/09/25 14:52:46  oes
 *    Added basic support for OPTIONS and TRACE HTTP methods:
 *     - New parser function client_max_forwards which decrements
 *       the Max-Forwards HTTP header field of OPTIONS and TRACE
 *       requests by one before forwarding
 *     - New parser function client_host which extracts the host
 *       and port information from the HTTP header field if the
 *       request URI was not absolute
 *     - Don't crumble and re-add the Host: header, but only generate
 *       and append if missing
 *
 *    Revision 1.26  2002/05/08 15:59:53  oes
 *    Changed add_to_iob signature (now returns jb_err)
 *
 *    Revision 1.25  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.24  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.23  2002/03/13 00:27:05  jongfoster
 *    Killing warnings
 *
 *    Revision 1.22  2002/03/09 20:03:52  jongfoster
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
 *    Revision 1.21  2002/03/07 03:46:17  oes
 *    Fixed compiler warnings
 *
 *    Revision 1.20  2002/02/20 23:15:13  jongfoster
 *    Parsing functions now handle out-of-memory gracefully by returning
 *    an error code.
 *
 *    Revision 1.19  2002/01/17 21:03:47  jongfoster
 *    Moving all our URL and URL pattern parsing code to urlmatch.c.
 *
 *    Revision 1.18  2001/10/26 17:40:23  oes
 *    Introduced get_header_value()
 *    Removed client_accept()
 *
 *    Revision 1.17  2001/10/13 12:47:32  joergs
 *    Removed client_host, added client_host_adder
 *
 *    Revision 1.16  2001/10/07 18:50:16  oes
 *    Added server_content_encoding, renamed server_transfer_encoding
 *
 *    Revision 1.15  2001/10/07 18:01:55  oes
 *    Changed server_http11 to server_http
 *
 *    Revision 1.14  2001/10/07 15:45:48  oes
 *    added client_accept_encoding, client_te, client_accept_encoding_adder
 *
 *    renamed content_type and content_length
 *
 *    fixed client_host and strclean prototypes
 *
 *    Revision 1.13  2001/09/29 12:56:03  joergs
 *    IJB now changes HTTP/1.1 to HTTP/1.0 in requests and answers.
 *
 *    Revision 1.12  2001/09/13 23:05:50  jongfoster
 *    Changing the string paramater to the header parsers a "const".
 *
 *    Revision 1.11  2001/07/31 14:46:53  oes
 *    Added prototype for connection_close_adder
 *
 *    Revision 1.10  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.9  2001/07/29 18:43:08  jongfoster
 *    Changing #ifdef _FILENAME_H to FILENAME_H_INCLUDED, to conform to
 *    ANSI C rules.
 *
 *    Revision 1.8  2001/07/13 14:01:54  oes
 *    Removed all #ifdef PCRS
 *
 *    Revision 1.7  2001/06/29 13:32:14  oes
 *    Removed logentry from cancelled commit
 *
 *    Revision 1.6  2001/06/03 19:12:38  oes
 *    deleted const struct interceptors
 *
 *    Revision 1.5  2001/05/31 21:30:33  jongfoster
 *    Removed list code - it's now in list.[ch]
 *    Renamed "permission" to "action", and changed many features
 *    to use the actions file rather than the global config.
 *
 *    Revision 1.4  2001/05/27 13:19:06  oes
 *    Patched Joergs solution for the content-length in.
 *
 *    Revision 1.3  2001/05/26 13:39:32  jongfoster
 *    Only crunches Content-Length header if applying RE filtering.
 *    Without this fix, Microsoft Windows Update wouldn't work.
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
 *    Revision 1.1.1.1  2001/05/15 13:59:01  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "project.h"

#ifdef __cplusplus
extern "C" {
#endif

/* Used for sed()'s second argument. */
#define FILTER_CLIENT_HEADERS 0
#define FILTER_SERVER_HEADERS 1

extern long flush_socket(jb_socket fd, struct iob *iob);
extern jb_err add_to_iob(struct client_state *csp, char *buf, long n);
extern jb_err decompress_iob(struct client_state *csp);
extern char *get_header(struct iob *iob);
extern char *get_header_value(const struct list *header_list, const char *header_name);
extern jb_err sed(struct client_state *csp, int filter_server_headers);
extern jb_err update_server_headers(struct client_state *csp);
extern void get_http_time(int time_offset, char *buf, size_t buffer_size);
extern jb_err get_destination_from_headers(const struct list *headers, struct http_request *http);

#ifdef FEATURE_FORCE_LOAD
extern int strclean(char *string, const char *substring);
#endif /* def FEATURE_FORCE_LOAD */

/* Revision control strings from this header and associated .c file */
extern const char parsers_rcs[];
extern const char parsers_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef PARSERS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
