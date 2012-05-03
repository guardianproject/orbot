const char parsers_rcs[] = "$Id: parsers.c,v 1.154 2009/03/13 14:10:07 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/parsers.c,v $
 *
 * Purpose     :  Declares functions to parse/crunch headers and pages.
 *                Functions declared include:
 *                   `add_to_iob', `client_cookie_adder', `client_from',
 *                   `client_referrer', `client_send_cookie', `client_ua',
 *                   `client_uagent', `client_x_forwarded',
 *                   `client_x_forwarded_adder', `client_xtra_adder',
 *                   `content_type', `crumble', `destroy_list', `enlist',
 *                   `flush_socket', ``get_header', `sed', `filter_header'
 *                   `server_content_encoding', `server_content_disposition',
 *                   `server_last_modified', `client_accept_language',
 *                   `crunch_client_header', `client_if_modified_since',
 *                   `client_if_none_match', `get_destination_from_headers',
 *                   `parse_header_time', `decompress_iob' and `server_set_cookie'.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2009 the
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
 *    $Log: parsers.c,v $
 *    Revision 1.154  2009/03/13 14:10:07  fabiankeil
 *    Fix some more harmless warnings on amd64.
 *
 *    Revision 1.153  2009/03/07 13:09:17  fabiankeil
 *    Change csp->expected_content and_csp->expected_content_length from
 *    size_t to unsigned long long to reduce the likelihood of integer
 *    overflows that would let us close the connection prematurely.
 *    Bug found while investigating #2669131, reported by cyberpatrol.
 *
 *    Revision 1.152  2009/03/01 18:43:48  fabiankeil
 *    Help clang understand that we aren't dereferencing
 *    NULL pointers here.
 *
 *    Revision 1.151  2009/02/15 14:46:35  fabiankeil
 *    Don't let hide-referrer{conditional-*}} pass
 *    Referer headers without http URLs.
 *
 *    Revision 1.150  2008/12/04 18:12:19  fabiankeil
 *    Fix some cparser warnings.
 *
 *    Revision 1.149  2008/11/21 18:39:53  fabiankeil
 *    In case of CONNECT requests there's no point
 *    in trying to keep the connection alive.
 *
 *    Revision 1.148  2008/11/16 12:43:49  fabiankeil
 *    Turn keep-alive support into a runtime feature
 *    that is disabled by setting keep-alive-timeout
 *    to a negative value.
 *
 *    Revision 1.147  2008/11/04 17:20:31  fabiankeil
 *    HTTP/1.1 responses without Connection
 *    header imply keep-alive. Act accordingly.
 *
 *    Revision 1.146  2008/10/12 16:46:35  fabiankeil
 *    Remove obsolete warning about delayed delivery with chunked
 *    transfer encoding and FEATURE_CONNECTION_KEEP_ALIVE enabled.
 *
 *    Revision 1.145  2008/10/09 18:21:41  fabiankeil
 *    Flush work-in-progress changes to keep outgoing connections
 *    alive where possible. Incomplete and mostly #ifdef'd out.
 *
 *    Revision 1.144  2008/09/21 13:59:33  fabiankeil
 *    Treat unknown change-x-forwarded-for parameters as fatal errors.
 *
 *    Revision 1.143  2008/09/21 13:36:52  fabiankeil
 *    If change-x-forwarded-for{add} is used and the client
 *    sends multiple X-Forwarded-For headers, append the client's
 *    IP address to each one of them. "Traditionally" we would
 *    lose all but the last one.
 *
 *    Revision 1.142  2008/09/20 10:04:33  fabiankeil
 *    Remove hide-forwarded-for-headers action which has
 *    been obsoleted by change-x-forwarded-for{block}.
 *
 *    Revision 1.141  2008/09/19 15:26:28  fabiankeil
 *    Add change-x-forwarded-for{} action to block or add
 *    X-Forwarded-For headers. Mostly based on code removed
 *    before 3.0.7.
 *
 *    Revision 1.140  2008/09/12 17:51:43  fabiankeil
 *    - A few style fixes.
 *    - Remove a pointless cast.
 *
 *    Revision 1.139  2008/09/04 08:13:58  fabiankeil
 *    Prepare for critical sections on Windows by adding a
 *    layer of indirection before the pthread mutex functions.
 *
 *    Revision 1.138  2008/08/30 12:03:07  fabiankeil
 *    Remove FEATURE_COOKIE_JAR.
 *
 *    Revision 1.137  2008/05/30 15:50:08  fabiankeil
 *    Remove questionable micro-optimizations
 *    whose usefulness has never been measured.
 *
 *    Revision 1.136  2008/05/26 16:02:24  fabiankeil
 *    s@Insufficent@Insufficient@
 *
 *    Revision 1.135  2008/05/21 20:12:10  fabiankeil
 *    The whole point of strclean() is to modify the
 *    first parameter, so don't mark it immutable,
 *    even though the compiler lets us get away with it.
 *
 *    Revision 1.134  2008/05/21 19:27:25  fabiankeil
 *    As the wafer actions are gone, we can stop including encode.h.
 *
 *    Revision 1.133  2008/05/21 15:50:47  fabiankeil
 *    Ditch cast from (char **) to (char **).
 *
 *    Revision 1.132  2008/05/21 15:47:14  fabiankeil
 *    Streamline sed()'s prototype and declare
 *    the header parse and add structures static.
 *
 *    Revision 1.131  2008/05/20 20:13:30  fabiankeil
 *    Factor update_server_headers() out of sed(), ditch the
 *    first_run hack and make server_patterns_light static.
 *
 *    Revision 1.130  2008/05/19 17:18:04  fabiankeil
 *    Wrap memmove() calls in string_move()
 *    to document the purpose in one place.
 *
 *    Revision 1.129  2008/05/17 14:02:07  fabiankeil
 *    Normalize linear header white space.
 *
 *    Revision 1.128  2008/05/16 16:39:03  fabiankeil
 *    If a header is split across multiple lines,
 *    merge them to a single line before parsing them.
 *
 *    Revision 1.127  2008/05/10 13:23:38  fabiankeil
 *    Don't provide get_header() with the whole client state
 *    structure when it only needs access to csp->iob.
 *
 *    Revision 1.126  2008/05/03 16:40:45  fabiankeil
 *    Change content_filters_enabled()'s parameter from
 *    csp->action to action so it can be also used in the
 *    CGI code. Don't bother checking if there are filters
 *    loaded, as that's somewhat besides the point.
 *
 *    Revision 1.125  2008/04/17 14:40:49  fabiankeil
 *    Provide get_http_time() with the buffer size so it doesn't
 *    have to blindly assume that the buffer is big enough.
 *
 *    Revision 1.124  2008/04/16 16:38:21  fabiankeil
 *    Don't pass the whole csp structure to flush_socket()
 *    when it only needs a file descriptor and a buffer.
 *
 *    Revision 1.123  2008/03/29 12:13:46  fabiankeil
 *    Remove send-wafer and send-vanilla-wafer actions.
 *
 *    Revision 1.122  2008/03/28 15:13:39  fabiankeil
 *    Remove inspect-jpegs action.
 *
 *    Revision 1.121  2008/01/05 21:37:03  fabiankeil
 *    Let client_range() also handle Request-Range headers
 *    which apparently are still supported by many servers.
 *
 *    Revision 1.120  2008/01/04 17:43:45  fabiankeil
 *    Improve the warning messages that get logged if the action files
 *    "enable" filters but no filters of that type have been loaded.
 *
 *    Revision 1.119  2007/12/28 18:32:51  fabiankeil
 *    In server_content_type():
 *    - Don't require leading white space when detecting image content types.
 *    - Change '... not replaced ...' message to sound less crazy if the text
 *      type actually is 'text/plain'.
 *    - Mark the 'text/plain == binary data' assumption for removal.
 *    - Remove a bunch of trailing white space.
 *
 *    Revision 1.118  2007/12/28 16:56:35  fabiankeil
 *    Minor server_content_disposition() changes:
 *    - Don't regenerate the header name all lower-case.
 *    - Some white space fixes.
 *    - Remove useless log message in case of ENOMEM.
 *
 *    Revision 1.117  2007/12/06 18:11:50  fabiankeil
 *    Garbage-collect the code to add a X-Forwarded-For
 *    header as it seems to be mostly used by accident.
 *
 *    Revision 1.116  2007/12/01 13:04:22  fabiankeil
 *    Fix a crash on mingw32 with some Last Modified times in the future.
 *
 *    Revision 1.115  2007/11/02 16:52:50  fabiankeil
 *    Remove a "can't happen" error block which, over
 *    time, mutated into a "guaranteed to happen" block.
 *
 *    Revision 1.114  2007/10/19 16:56:26  fabiankeil
 *    - Downgrade "Buffer limit reached" message to LOG_LEVEL_INFO.
 *    - Use shiny new content_filters_enabled() in client_range().
 *
 *    Revision 1.113  2007/10/10 17:29:57  fabiankeil
 *    I forgot about Poland.
 *
 *    Revision 1.112  2007/10/09 16:38:40  fabiankeil
 *    Remove Range and If-Range headers if content filtering is enabled.
 *
 *    Revision 1.111  2007/10/04 18:07:00  fabiankeil
 *    Move ACTION_VANILLA_WAFER handling from jcc's chat() into
 *    client_cookie_adder() to make sure send-vanilla-wafer can be
 *    controlled through tags (and thus regression-tested).
 *
 *    Revision 1.110  2007/09/29 10:42:37  fabiankeil
 *    - Remove "scanning headers for" log message again.
 *    - Some more whitespace fixes.
 *
 *    Revision 1.109  2007/09/08 14:25:48  fabiankeil
 *    Refactor client_referrer() and add conditional-forge parameter.
 *
 *    Revision 1.108  2007/08/28 18:21:03  fabiankeil
 *    A bunch of whitespace fixes, pointy hat to me.
 *
 *    Revision 1.107  2007/08/28 18:16:32  fabiankeil
 *    Fix possible memory corruption in server_http, make sure it's not
 *    executed for ordinary server headers and mark some problems for later.
 *
 *    Revision 1.106  2007/08/18 14:30:32  fabiankeil
 *    Let content-type-overwrite{} honour force-text-mode again.
 *
 *    Revision 1.105  2007/08/11 14:49:49  fabiankeil
 *    - Add prototpyes for the header parsers and make them static.
 *    - Comment out client_accept_encoding_adder() which isn't used right now.
 *
 *    Revision 1.104  2007/07/14 07:38:19  fabiankeil
 *    Move the ACTION_FORCE_TEXT_MODE check out of
 *    server_content_type(). Signal other functions
 *    whether or not a content type has been declared.
 *    Part of the fix for BR#1750917.
 *
 *    Revision 1.103  2007/06/01 16:31:54  fabiankeil
 *    Change sed() to return a jb_err in preparation for forward-override{}.
 *
 *    Revision 1.102  2007/05/27 12:39:32  fabiankeil
 *    Adjust "X-Filter: No" to disable dedicated header filters.
 *
 *    Revision 1.101  2007/05/14 10:16:41  fabiankeil
 *    Streamline client_cookie_adder().
 *
 *    Revision 1.100  2007/04/30 15:53:11  fabiankeil
 *    Make sure filters with dynamic jobs actually use them.
 *
 *    Revision 1.99  2007/04/30 15:06:26  fabiankeil
 *    - Introduce dynamic pcrs jobs that can resolve variables.
 *    - Remove unnecessary update_action_bits_for_all_tags() call.
 *
 *    Revision 1.98  2007/04/17 18:32:10  fabiankeil
 *    - Make tagging based on tags set by earlier taggers
 *      of the same kind possible.
 *    - Log whether or not new tags cause action bits updates
 *      (in which case a matching tag-pattern section exists).
 *    - Log if the user tries to set a tag that is already set.
 *
 *    Revision 1.97  2007/04/15 16:39:21  fabiankeil
 *    Introduce tags as alternative way to specify which
 *    actions apply to a request. At the moment tags can be
 *    created based on client and server headers.
 *
 *    Revision 1.96  2007/04/12 12:53:58  fabiankeil
 *    Log a warning if the content is compressed, filtering is
 *    enabled and Privoxy was compiled without zlib support.
 *    Closes FR#1673938.
 *
 *    Revision 1.95  2007/03/25 14:26:40  fabiankeil
 *    - Fix warnings when compiled with glibc.
 *    - Don't use crumble() for cookie crunching.
 *    - Move cookie time parsing into parse_header_time().
 *    - Let parse_header_time() return a jb_err code
 *      instead of a pointer that can only be used to
 *      check for NULL anyway.
 *
 *    Revision 1.94  2007/03/21 12:23:53  fabiankeil
 *    - Add better protection against malicious gzip headers.
 *    - Stop logging the first hundred bytes of decompressed content.
 *      It looks like it's working and there is always debug 16.
 *    - Log the content size after decompression in decompress_iob()
 *      instead of pcrs_filter_response().
 *
 *    Revision 1.93  2007/03/20 15:21:44  fabiankeil
 *    - Use dedicated header filter actions instead of abusing "filter".
 *      Replace "filter-client-headers" and "filter-client-headers"
 *      with "server-header-filter" and "client-header-filter".
 *    - Remove filter_client_header() and filter_client_header(),
 *      filter_header() now checks the shiny new
 *      CSP_FLAG_CLIENT_HEADER_PARSING_DONE flag instead.
 *
 *    Revision 1.92  2007/03/05 13:25:32  fabiankeil
 *    - Cosmetical changes for LOG_LEVEL_RE_FILTER messages.
 *    - Handle "Cookie:" and "Connection:" headers a bit smarter
 *      (don't crunch them just to recreate them later on).
 *    - Add another non-standard time format for the cookie
 *      expiration date detection.
 *    - Fix a valgrind warning.
 *
 *    Revision 1.91  2007/02/24 12:27:32  fabiankeil
 *    Improve cookie expiration date detection.
 *
 *    Revision 1.90  2007/02/08 19:12:35  fabiankeil
 *    Don't run server_content_length() the first time
 *    sed() parses server headers; only adjust the
 *    Content-Length header if the page was modified.
 *
 *    Revision 1.89  2007/02/07 16:52:11  fabiankeil
 *    Fix log messages regarding the cookie time format
 *    (cookie and request URL were mixed up).
 *
 *    Revision 1.88  2007/02/07 11:27:12  fabiankeil
 *    - Let decompress_iob()
 *      - not corrupt the content if decompression fails
 *        early. (the first byte(s) were lost).
 *      - use pointer arithmetics with defined outcome for
 *        a change.
 *    - Use a different kludge to remember a failed decompression.
 *
 *    Revision 1.87  2007/01/31 16:21:38  fabiankeil
 *    Search for Max-Forwards headers case-insensitive,
 *    don't generate the "501 unsupported" message for invalid
 *    Max-Forwards values and don't increase negative ones.
 *
 *    Revision 1.86  2007/01/30 13:05:26  fabiankeil
 *    - Let server_set_cookie() check the expiration date
 *      of cookies and don't touch the ones that are already
 *      expired. Fixes problems with low quality web applications
 *      as described in BR 932612.
 *
 *    - Adjust comment in client_max_forwards to reality;
 *      remove invalid Max-Forwards headers.
 *
 *    Revision 1.85  2007/01/26 15:33:46  fabiankeil
 *    Stop filter_header() from unintentionally removing
 *    empty header lines that were enlisted by the continue
 *    hack.
 *
 *    Revision 1.84  2007/01/24 12:56:52  fabiankeil
 *    - Repeat the request URL before logging any headers.
 *      Makes reading the log easier in case of simultaneous requests.
 *    - If there are more than one Content-Type headers in one request,
 *      use the first one and remove the others.
 *    - Remove "newval" variable in server_content_type().
 *      It's only used once.
 *
 *    Revision 1.83  2007/01/12 15:03:02  fabiankeil
 *    Correct a cast, check inflateEnd() exit code
 *    to see if we have to, replace sprintf calls
 *    with snprintf.
 *
 *    Revision 1.82  2007/01/01 19:36:37  fabiankeil
 *    Integrate a modified version of Wil Mahan's
 *    zlib patch (PR #895531).
 *
 *    Revision 1.81  2006/12/31 22:21:33  fabiankeil
 *    Skip empty filter files in filter_header()
 *    but don't ignore the ones that come afterwards.
 *    Fixes BR 1619208, this time for real.
 *
 *    Revision 1.80  2006/12/29 19:08:22  fabiankeil
 *    Reverted parts of my last commit
 *    to keep error handling working.
 *
 *    Revision 1.79  2006/12/29 18:04:40  fabiankeil
 *    Fixed gcc43 conversion warnings.
 *
 *    Revision 1.78  2006/12/26 17:19:20  fabiankeil
 *    Bringing back the "useless" localtime() call
 *    I removed in revision 1.67. On some platforms
 *    it's necessary to prevent time zone offsets.
 *
 *    Revision 1.77  2006/12/07 18:44:26  fabiankeil
 *    Rebuild request URL in get_destination_from_headers()
 *    to make sure redirect{pcrs command} works as expected
 *    for intercepted requests.
 *
 *    Revision 1.76  2006/12/06 19:52:25  fabiankeil
 *    Added get_destination_from_headers().
 *
 *    Revision 1.75  2006/11/13 19:05:51  fabiankeil
 *    Make pthread mutex locking more generic. Instead of
 *    checking for OSX and OpenBSD, check for FEATURE_PTHREAD
 *    and use mutex locking unless there is an _r function
 *    available. Better safe than sorry.
 *
 *    Fixes "./configure --disable-pthread" and should result
 *    in less threading-related problems on pthread-using platforms,
 *    but it still doesn't fix BR#1122404.
 *
 *    Revision 1.74  2006/10/02 16:59:12  fabiankeil
 *    The special header "X-Filter: No" now disables
 *    header filtering as well.
 *
 *    Revision 1.73  2006/09/23 13:26:38  roro
 *    Replace TABs by spaces in source code.
 *
 *    Revision 1.72  2006/09/23 12:37:21  fabiankeil
 *    Don't print a log message every time filter_headers is
 *    entered or left. It only creates noise without any real
 *    information.
 *
 *    Revision 1.71  2006/09/21 19:55:17  fabiankeil
 *    Fix +hide-if-modified-since{-n}.
 *
 *    Revision 1.70  2006/09/08 12:06:34  fabiankeil
 *    Have hide-if-modified-since interpret the random
 *    range value as minutes instead of hours. Allows
 *    more fine-grained configuration.
 *
 *    Revision 1.69  2006/09/06 16:25:51  fabiankeil
 *    Always have parse_header_time return a pointer
 *    that actual makes sense, even though we currently
 *    only need it to detect problems.
 *
 *    Revision 1.68  2006/09/06 10:43:32  fabiankeil
 *    Added config option enable-remote-http-toggle
 *    to specify if Privoxy should recognize special
 *    headers (currently only X-Filter) to change its
 *    behaviour. Disabled by default.
 *
 *    Revision 1.67  2006/09/04 11:01:26  fabiankeil
 *    After filtering de-chunked instances, remove
 *    "Transfer-Encoding" header entirely instead of changing
 *    it to "Transfer-Encoding: identity", which is invalid.
 *    Thanks Michael Shields <shields@msrl.com>. Fixes PR 1318658.
 *
 *    Don't use localtime in parse_header_time. An empty time struct
 *    is good enough, it gets overwritten by strptime anyway.
 *
 *    Revision 1.66  2006/09/03 19:38:28  fabiankeil
 *    Use gmtime_r if available, fallback to gmtime with mutex
 *    protection for MacOSX and use vanilla gmtime for the rest.
 *
 *    Revision 1.65  2006/08/22 10:55:56  fabiankeil
 *    Changed client_referrer to use the right type (size_t) for
 *    hostlenght and to shorten the temporary referrer string with
 *    '\0' instead of adding a useless line break.
 *
 *    Revision 1.64  2006/08/17 17:15:10  fabiankeil
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
 *    Revision 1.63  2006/08/14 13:18:08  david__schmidt
 *    OS/2 compilation compatibility fixups
 *
 *    Revision 1.62  2006/08/14 08:58:42  fabiankeil
 *    Changed include from strptime.c to strptime.h
 *
 *    Revision 1.61  2006/08/14 08:25:19  fabiankeil
 *    Split filter-headers{} into filter-client-headers{}
 *    and filter-server-headers{}.
 *    Added parse_header_time() to share some code.
 *    Replaced timegm() with mktime().
 *
 *    Revision 1.60  2006/08/12 03:54:37  david__schmidt
 *    Windows service integration
 *
 *    Revision 1.59  2006/08/03 02:46:41  david__schmidt
 *    Incorporate Fabian Keil's patch work:http://www.fabiankeil.de/sourcecode/privoxy/
 *
 *    Revision 1.58  2006/07/18 14:48:47  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.56.2.10  2006/01/21 16:16:08  david__schmidt
 *    Thanks to  Edward Carrel for his patch to modernize OSX'spthreads support.  See bug #1409623.
 *
 *    Revision 1.56.2.9  2004/10/03 12:53:45  david__schmidt
 *    Add the ability to check jpeg images for invalid
 *    lengths of comment blocks.  Defensive strategy
 *    against the exploit:
 *       Microsoft Security Bulletin MS04-028
 *       Buffer Overrun in JPEG Processing (GDI+) Could
 *       Allow Code Execution (833987)
 *    Enabled with +inspect-jpegs in actions files.
 *
 *    Revision 1.56.2.8  2003/07/11 13:21:25  oes
 *    Excluded text/plain objects from filtering. This fixes a
 *    couple of client-crashing, download corruption and
 *    Privoxy performance issues, whose root cause lies in
 *    web servers labelling content of unknown type as text/plain.
 *
 *    Revision 1.56.2.7  2003/05/06 12:07:26  oes
 *    Fixed bug #729900: Suspicious HOST: headers are now killed and regenerated if necessary
 *
 *    Revision 1.56.2.6  2003/04/14 21:28:30  oes
 *    Completing the previous change
 *
 *    Revision 1.56.2.5  2003/04/14 12:08:16  oes
 *    Added temporary workaround for bug in PHP < 4.2.3
 *
 *    Revision 1.56.2.4  2003/03/07 03:41:05  david__schmidt
 *    Wrapping all *_r functions (the non-_r versions of them) with mutex semaphores for OSX.  Hopefully this will take care of all of those pesky crash reports.
 *
 *    Revision 1.56.2.3  2002/11/10 04:20:02  hal9
 *    Fix typo: supressed -> suppressed
 *
 *    Revision 1.56.2.2  2002/09/25 14:59:53  oes
 *    Improved cookie logging
 *
 *    Revision 1.56.2.1  2002/09/25 14:52:45  oes
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
 *    Revision 1.56  2002/05/12 15:34:22  jongfoster
 *    Fixing typo in a comment
 *
 *    Revision 1.55  2002/05/08 16:01:07  oes
 *    Optimized add_to_iob:
 *     - Use realloc instead of malloc(), memcpy(), free()
 *     - Expand to powers of two if possible, to get
 *       O(log n) reallocs instead of O(n).
 *     - Moved check for buffer limit here from chat
 *     - Report failure via returncode
 *
 *    Revision 1.54  2002/04/02 15:03:16  oes
 *    Tiny code cosmetics
 *
 *    Revision 1.53  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.52  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.51  2002/03/13 00:27:05  jongfoster
 *    Killing warnings
 *
 *    Revision 1.50  2002/03/12 01:45:35  oes
 *    More verbose logging
 *
 *    Revision 1.49  2002/03/09 20:03:52  jongfoster
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
 *    Revision 1.48  2002/03/07 03:46:53  oes
 *    Fixed compiler warnings etc
 *
 *    Revision 1.47  2002/02/20 23:15:13  jongfoster
 *    Parsing functions now handle out-of-memory gracefully by returning
 *    an error code.
 *
 *    Revision 1.46  2002/01/17 21:03:47  jongfoster
 *    Moving all our URL and URL pattern parsing code to urlmatch.c.
 *
 *    Revision 1.45  2002/01/09 14:33:03  oes
 *    Added support for localtime_r.
 *
 *    Revision 1.44  2001/12/14 01:22:54  steudten
 *    Remove 'user:pass@' from 'proto://user:pass@host' for the
 *    new added header 'Host: ..'. (See Req ID 491818)
 *
 *    Revision 1.43  2001/11/23 00:26:38  jongfoster
 *    Fixing two really stupid errors in my previous commit
 *
 *    Revision 1.42  2001/11/22 21:59:30  jongfoster
 *    Adding code to handle +no-cookies-keep
 *
 *    Revision 1.41  2001/11/05 23:43:05  steudten
 *    Add time+date to log files.
 *
 *    Revision 1.40  2001/10/26 20:13:09  jongfoster
 *    ctype.h is needed in Windows, too.
 *
 *    Revision 1.39  2001/10/26 17:40:04  oes
 *    Introduced get_header_value()
 *    Removed http->user_agent, csp->referrer and csp->accept_types
 *    Removed client_accept()
 *
 *    Revision 1.38  2001/10/25 03:40:48  david__schmidt
 *    Change in porting tactics: OS/2's EMX porting layer doesn't allow multiple
 *    threads to call select() simultaneously.  So, it's time to do a real, live,
 *    native OS/2 port.  See defines for __EMX__ (the porting layer) vs. __OS2__
 *    (native). Both versions will work, but using __OS2__ offers multi-threading.
 *
 *    Revision 1.37  2001/10/23 21:36:02  jongfoster
 *    Documenting sed()'s error behaviou (doc change only)
 *
 *    Revision 1.36  2001/10/13 12:51:51  joergs
 *    Removed client_host, (was only required for the old 2.0.2-11 http://noijb.
 *    force-load), instead crumble Host: and add it (again) in client_host_adder
 *    (in case we get a HTTP/1.0 request without Host: header and forward it to
 *    a HTTP/1.1 server/proxy).
 *
 *    Revision 1.35  2001/10/09 22:39:21  jongfoster
 *    assert.h is also required under Win32, so moving out of #ifndef _WIN32
 *    block.
 *
 *    Revision 1.34  2001/10/07 18:50:55  oes
 *    Added server_content_encoding, renamed server_transfer_encoding
 *
 *    Revision 1.33  2001/10/07 18:04:49  oes
 *    Changed server_http11 to server_http and its pattern to "HTTP".
 *      Additional functionality: it now saves the HTTP status into
 *      csp->http->status and sets CT_TABOO for Status 206 (partial range)
 *
 *    Revision 1.32  2001/10/07 15:43:28  oes
 *    Removed FEATURE_DENY_GZIP and replaced it with client_accept_encoding,
 *       client_te and client_accept_encoding_adder, triggered by the new
 *       +no-compression action. For HTTP/1.1 the Accept-Encoding header is
 *       changed to allow only identity and chunked, and the TE header is
 *       crunched. For HTTP/1.0, Accept-Encoding is crunched.
 *
 *    parse_http_request no longer does anything than parsing. The rewriting
 *      of http->cmd and version mangling are gone. It now also recognizes
 *      the put and delete methods and saves the url in http->url. Removed
 *      unused variable.
 *
 *    renamed content_type and content_length to have the server_ prefix
 *
 *    server_content_type now only works if csp->content_type != CT_TABOO
 *
 *    added server_transfer_encoding, which
 *      - Sets CT_TABOO to prohibit filtering if encoding compresses
 *      - Raises the CSP_FLAG_CHUNKED flag if Encoding is "chunked"
 *      - Change from "chunked" to "identity" if body was chunked
 *        but has been de-chunked for filtering.
 *
 *    added server_content_md5 which crunches any Content-MD5 headers
 *      if the body was modified.
 *
 *    made server_http11 conditional on +downgrade action
 *
 *    Replaced 6 boolean members of csp with one bitmap (csp->flags)
 *
 *    Revision 1.31  2001/10/05 14:25:02  oes
 *    Crumble Keep-Alive from Server
 *
 *    Revision 1.30  2001/09/29 12:56:03  joergs
 *    IJB now changes HTTP/1.1 to HTTP/1.0 in requests and answers.
 *
 *    Revision 1.29  2001/09/24 21:09:24  jongfoster
 *    Fixing 2 memory leaks that Guy spotted, where the paramater to
 *    enlist() was not being free()d.
 *
 *    Revision 1.28  2001/09/22 16:32:28  jongfoster
 *    Removing unused #includes.
 *
 *    Revision 1.27  2001/09/20 15:45:25  steudten
 *
 *    add casting from size_t to int for printf()
 *    remove local variable shadow s2
 *
 *    Revision 1.26  2001/09/16 17:05:14  jongfoster
 *    Removing unused #include showarg.h
 *
 *    Revision 1.25  2001/09/16 13:21:27  jongfoster
 *    Changes to use new list functions.
 *
 *    Revision 1.24  2001/09/13 23:05:50  jongfoster
 *    Changing the string paramater to the header parsers a "const".
 *
 *    Revision 1.23  2001/09/12 18:08:19  steudten
 *
 *    In parse_http_request() header rewriting miss the host value, so
 *    from http://www.mydomain.com the result was just " / " not
 *    http://www.mydomain.com/ in case we forward.
 *
 *    Revision 1.22  2001/09/10 10:58:53  oes
 *    Silenced compiler warnings
 *
 *    Revision 1.21  2001/07/31 14:46:00  oes
 *     - Persistant connections now suppressed
 *     - sed() no longer appends empty header to csp->headers
 *
 *    Revision 1.20  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.19  2001/07/25 17:21:54  oes
 *    client_uagent now saves copy of User-Agent: header value
 *
 *    Revision 1.18  2001/07/13 14:02:46  oes
 *     - Included fix to repair broken HTTP requests that
 *       don't contain a path, not even '/'.
 *     - Removed all #ifdef PCRS
 *     - content_type now always inspected and classified as
 *       text, gif or other.
 *     - formatting / comments
 *
 *    Revision 1.17  2001/06/29 21:45:41  oes
 *    Indentation, CRLF->LF, Tab-> Space
 *
 *    Revision 1.16  2001/06/29 13:32:42  oes
 *    - Fixed a comment
 *    - Adapted free_http_request
 *    - Removed logentry from cancelled commit
 *
 *    Revision 1.15  2001/06/03 19:12:38  oes
 *    deleted const struct interceptors
 *
 *    Revision 1.14  2001/06/01 18:49:17  jongfoster
 *    Replaced "list_share" with "list" - the tiny memory gain was not
 *    worth the extra complexity.
 *
 *    Revision 1.13  2001/05/31 21:30:33  jongfoster
 *    Removed list code - it's now in list.[ch]
 *    Renamed "permission" to "action", and changed many features
 *    to use the actions file rather than the global config.
 *
 *    Revision 1.12  2001/05/31 17:33:13  oes
 *
 *    CRLF -> LF
 *
 *    Revision 1.11  2001/05/29 20:11:19  joergs
 *    '/ * inside comment' warning removed.
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
 *    Revision 1.9  2001/05/28 17:26:33  jongfoster
 *    Fixing segfault if last header was crunched.
 *    Fixing Windows build (snprintf() is _snprintf() under Win32, but we
 *    can use the cross-platform sprintf() instead.)
 *
 *    Revision 1.8  2001/05/27 22:17:04  oes
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
 *    Revision 1.7  2001/05/27 13:19:06  oes
 *    Patched Joergs solution for the content-length in.
 *
 *    Revision 1.6  2001/05/26 13:39:32  jongfoster
 *    Only crunches Content-Length header if applying RE filtering.
 *    Without this fix, Microsoft Windows Update wouldn't work.
 *
 *    Revision 1.5  2001/05/26 00:28:36  jongfoster
 *    Automatic reloading of config file.
 *    Removed obsolete SIGHUP support (Unix) and Reload menu option (Win32).
 *    Most of the global variables have been moved to a new
 *    struct configuration_spec, accessed through csp->config->globalname
 *    Most of the globals remaining are used by the Win32 GUI.
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
 *    Revision 1.3  2001/05/20 01:21:20  jongfoster
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
 *    Revision 1.2  2001/05/17 23:02:36  oes
 *     - Made referrer option accept 'L' as a substitute for '§'
 *
 *    Revision 1.1.1.1  2001/05/15 13:59:01  oes
 *    Initial import of version 2.9.3 source tree
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

#ifdef __GLIBC__
/*
 * Convince GNU's libc to provide a strptime prototype.
 */
#define __USE_XOPEN
#endif /*__GLIBC__ */
#include <time.h>

#ifdef FEATURE_ZLIB
#include <zlib.h>
#endif

#if !defined(_WIN32) && !defined(__OS2__)
#include <unistd.h>
#endif

#include "project.h"

#ifdef FEATURE_PTHREAD
#include "jcc.h"
/* jcc.h is for mutex semapores only */
#endif /* def FEATURE_PTHREAD */
#include "list.h"
#include "parsers.h"
#include "ssplit.h"
#include "errlog.h"
#include "jbsockets.h"
#include "miscutil.h"
#include "list.h"
#include "actions.h"
#include "filters.h"

#ifndef HAVE_STRPTIME
#include "strptime.h"
#endif

const char parsers_h_rcs[] = PARSERS_H_VERSION;

/* Fix a problem with Solaris.  There should be no effect on other
 * platforms.
 * Solaris's isspace() is a macro which uses its argument directly
 * as an array index.  Therefore we need to make sure that high-bit
 * characters generate +ve values, and ideally we also want to make
 * the argument match the declared parameter type of "int".
 *
 * Why did they write a character function that can't take a simple
 * "char" argument?  Doh!
 */
#define ijb_isupper(__X) isupper((int)(unsigned char)(__X))
#define ijb_tolower(__X) tolower((int)(unsigned char)(__X))

static char *get_header_line(struct iob *iob);
static jb_err scan_headers(struct client_state *csp);
static jb_err header_tagger(struct client_state *csp, char *header);
static jb_err parse_header_time(const char *header_time, time_t *result);

static jb_err crumble                   (struct client_state *csp, char **header);
static jb_err filter_header             (struct client_state *csp, char **header);
static jb_err client_connection         (struct client_state *csp, char **header);
static jb_err client_referrer           (struct client_state *csp, char **header);
static jb_err client_uagent             (struct client_state *csp, char **header);
static jb_err client_ua                 (struct client_state *csp, char **header);
static jb_err client_from               (struct client_state *csp, char **header);
static jb_err client_send_cookie        (struct client_state *csp, char **header);
static jb_err client_x_forwarded        (struct client_state *csp, char **header);
static jb_err client_accept_encoding    (struct client_state *csp, char **header);
static jb_err client_te                 (struct client_state *csp, char **header);
static jb_err client_max_forwards       (struct client_state *csp, char **header);
static jb_err client_host               (struct client_state *csp, char **header);
static jb_err client_if_modified_since  (struct client_state *csp, char **header);
static jb_err client_accept_language    (struct client_state *csp, char **header);
static jb_err client_if_none_match      (struct client_state *csp, char **header);
static jb_err crunch_client_header      (struct client_state *csp, char **header);
static jb_err client_x_filter           (struct client_state *csp, char **header);
static jb_err client_range              (struct client_state *csp, char **header);
static jb_err server_set_cookie         (struct client_state *csp, char **header);
static jb_err server_connection         (struct client_state *csp, char **header);
static jb_err server_content_type       (struct client_state *csp, char **header);
static jb_err server_adjust_content_length(struct client_state *csp, char **header);
static jb_err server_content_md5        (struct client_state *csp, char **header);
static jb_err server_content_encoding   (struct client_state *csp, char **header);
static jb_err server_transfer_coding    (struct client_state *csp, char **header);
static jb_err server_http               (struct client_state *csp, char **header);
static jb_err crunch_server_header      (struct client_state *csp, char **header);
static jb_err server_last_modified      (struct client_state *csp, char **header);
static jb_err server_content_disposition(struct client_state *csp, char **header);

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
static jb_err server_save_content_length(struct client_state *csp, char **header);
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

static jb_err client_host_adder       (struct client_state *csp);
static jb_err client_xtra_adder       (struct client_state *csp);
static jb_err client_x_forwarded_for_adder(struct client_state *csp);
static jb_err client_connection_header_adder(struct client_state *csp);
static jb_err server_connection_close_adder(struct client_state *csp);

static jb_err create_forged_referrer(char **header, const char *hostport);
static jb_err create_fake_referrer(char **header, const char *fake_referrer);
static jb_err handle_conditional_hide_referrer_parameter(char **header,
   const char *host, const int parameter_conditional_block);
static const char *get_appropiate_connection_header(const struct client_state *csp);

/*
 * List of functions to run on a list of headers.
 */
struct parsers
{
   /** The header prefix to match */
   const char *str;
   
   /** The length of the prefix to match */
   const size_t len;
   
   /** The function to apply to this line */
   const parser_func_ptr parser;
};

static const struct parsers client_patterns[] = {
   { "referer:",                  8,   client_referrer },
   { "user-agent:",              11,   client_uagent },
   { "ua-",                       3,   client_ua },
   { "from:",                     5,   client_from },
   { "cookie:",                   7,   client_send_cookie },
   { "x-forwarded-for:",         16,   client_x_forwarded },
   { "Accept-Encoding:",         16,   client_accept_encoding },
   { "TE:",                       3,   client_te },
   { "Host:",                     5,   client_host },
   { "if-modified-since:",       18,   client_if_modified_since },
   { "Keep-Alive:",              11,   crumble },
   { "connection:",              11,   client_connection },
   { "proxy-connection:",        17,   crumble },
   { "max-forwards:",            13,   client_max_forwards },
   { "Accept-Language:",         16,   client_accept_language },
   { "if-none-match:",           14,   client_if_none_match },
   { "Range:",                    6,   client_range },
   { "Request-Range:",           14,   client_range },
   { "If-Range:",                 9,   client_range },
   { "X-Filter:",                 9,   client_x_filter },
   { "*",                         0,   crunch_client_header },
   { "*",                         0,   filter_header },
   { NULL,                        0,   NULL }
};

static const struct parsers server_patterns[] = {
   { "HTTP/",                     5, server_http },
   { "set-cookie:",              11, server_set_cookie },
   { "connection:",              11, server_connection },
   { "Content-Type:",            13, server_content_type },
   { "Content-MD5:",             12, server_content_md5 },
   { "Content-Encoding:",        17, server_content_encoding },
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   { "Content-Length:",          15, server_save_content_length },
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */
   { "Transfer-Encoding:",       18, server_transfer_coding },
   { "Keep-Alive:",              11, crumble },
   { "content-disposition:",     20, server_content_disposition },
   { "Last-Modified:",           14, server_last_modified },
   { "*",                         0, crunch_server_header },
   { "*",                         0, filter_header },
   { NULL,                        0, NULL }
};

static const add_header_func_ptr add_client_headers[] = {
   client_host_adder,
   client_x_forwarded_for_adder,
   client_xtra_adder,
   /* Temporarily disabled:    client_accept_encoding_adder, */
   client_connection_header_adder,
   NULL
};

static const add_header_func_ptr add_server_headers[] = {
   server_connection_close_adder,
   NULL
};

/*********************************************************************
 *
 * Function    :  flush_socket
 *
 * Description :  Write any pending "buffered" content.
 *
 * Parameters  :
 *          1  :  fd = file descriptor of the socket to read
 *          2  :  iob = The I/O buffer to flush, usually csp->iob.
 *
 * Returns     :  On success, the number of bytes written are returned (zero
 *                indicates nothing was written).  On error, -1 is returned,
 *                and errno is set appropriately.  If count is zero and the
 *                file descriptor refers to a regular file, 0 will be
 *                returned without causing any other effect.  For a special
 *                file, the results are not portable.
 *
 *********************************************************************/
long flush_socket(jb_socket fd, struct iob *iob)
{
   long len = iob->eod - iob->cur;

   if (len <= 0)
   {
      return(0);
   }

   if (write_socket(fd, iob->cur, (size_t)len))
   {
      return(-1);
   }
   iob->eod = iob->cur = iob->buf;
   return(len);

}


/*********************************************************************
 *
 * Function    :  add_to_iob
 *
 * Description :  Add content to the buffered page, expanding the
 *                buffer if necessary.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  buf = holds the content to be added to the page
 *          3  :  n = number of bytes to be added
 *
 * Returns     :  JB_ERR_OK on success, JB_ERR_MEMORY if out-of-memory
 *                or buffer limit reached.
 *
 *********************************************************************/
jb_err add_to_iob(struct client_state *csp, char *buf, long n)
{
   struct iob *iob = csp->iob;
   size_t used, offset, need, want;
   char *p;

   if (n <= 0) return JB_ERR_OK;

   used   = (size_t)(iob->eod - iob->buf);
   offset = (size_t)(iob->cur - iob->buf);
   need   = used + (size_t)n + 1;

   /*
    * If the buffer can't hold the new data, extend it first.
    * Use the next power of two if possible, else use the actual need.
    */
   if (need > csp->config->buffer_limit)
   {
      log_error(LOG_LEVEL_INFO,
         "Buffer limit reached while extending the buffer (iob). Needed: %d. Limit: %d",
         need, csp->config->buffer_limit);
      return JB_ERR_MEMORY;
   }

   if (need > iob->size)
   {
      for (want = csp->iob->size ? csp->iob->size : 512; want <= need;) want *= 2;
      
      if (want <= csp->config->buffer_limit && NULL != (p = (char *)realloc(iob->buf, want)))
      {
         iob->size = want;
      }
      else if (NULL != (p = (char *)realloc(iob->buf, need)))
      {
         iob->size = need;
      }
      else
      {
         log_error(LOG_LEVEL_ERROR, "Extending the buffer (iob) failed: %E");
         return JB_ERR_MEMORY;
      }

      /* Update the iob pointers */
      iob->cur = p + offset;
      iob->eod = p + used;
      iob->buf = p;
   }

   /* copy the new data into the iob buffer */
   memcpy(iob->eod, buf, (size_t)n);

   /* point to the end of the data */
   iob->eod += n;

   /* null terminate == cheap insurance */
   *iob->eod = '\0';

   return JB_ERR_OK;

}


#ifdef FEATURE_ZLIB
/*********************************************************************
 *
 * Function    :  decompress_iob
 *
 * Description :  Decompress buffered page, expanding the
 *                buffer as necessary.  csp->iob->cur
 *                should point to the the beginning of the
 *                compressed data block.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK on success,
 *                JB_ERR_MEMORY if out-of-memory limit reached, and
 *                JB_ERR_COMPRESS if error decompressing buffer.
 *
 *********************************************************************/
jb_err decompress_iob(struct client_state *csp)
{
   char  *buf;       /* new, uncompressed buffer */
   char  *cur;       /* Current iob position (to keep the original 
                      * iob->cur unmodified if we return early) */
   size_t bufsize;   /* allocated size of the new buffer */
   size_t old_size;  /* Content size before decompression */
   size_t skip_size; /* Number of bytes at the beginning of the iob
                        that we should NOT decompress. */
   int status;       /* return status of the inflate() call */
   z_stream zstr;    /* used by calls to zlib */

   assert(csp->iob->cur - csp->iob->buf > 0);
   assert(csp->iob->eod - csp->iob->cur > 0);

   bufsize = csp->iob->size;
   skip_size = (size_t)(csp->iob->cur - csp->iob->buf);
   old_size = (size_t)(csp->iob->eod - csp->iob->cur);

   cur = csp->iob->cur;

   if (bufsize < (size_t)10)
   {
      /*
       * This is to protect the parsing of gzipped data,
       * but it should(?) be valid for deflated data also.
       */
      log_error(LOG_LEVEL_ERROR, "Buffer too small decompressing iob");
      return JB_ERR_COMPRESS;
   }

   if (csp->content_type & CT_GZIP)
   {
      /*
       * Our task is slightly complicated by the facts that data
       * compressed by gzip does not include a zlib header, and
       * that there is no easily accessible interface in zlib to
       * handle a gzip header. We strip off the gzip header by
       * hand, and later inform zlib not to expect a header.
       */

      /*
       * Strip off the gzip header. Please see RFC 1952 for more
       * explanation of the appropriate fields.
       */
      if ((*cur++ != (char)0x1f)
       || (*cur++ != (char)0x8b)
       || (*cur++ != Z_DEFLATED))
      {
         log_error(LOG_LEVEL_ERROR, "Invalid gzip header when decompressing");
         return JB_ERR_COMPRESS;
      }
      else
      {
         int flags = *cur++;
         /*
          * XXX: These magic numbers should be replaced
          * with macros to give a better idea what they do.
          */
         if (flags & 0xe0)
         {
            /* The gzip header has reserved bits set; bail out. */
            log_error(LOG_LEVEL_ERROR, "Invalid gzip header flags when decompressing");
            return JB_ERR_COMPRESS;
         }
         cur += 6;

         /* Skip extra fields if necessary. */
         if (flags & 0x04)
         {
            /*
             * Skip a given number of bytes, specified
             * as a 16-bit little-endian value.
             */
            /*
             * XXX: This code used to be:
             * 
             * csp->iob->cur += *csp->iob->cur++ + (*csp->iob->cur++ << 8);
             *
             * which I had to change into:
             *
             * cur += *cur++ + (*cur++ << 8);
             *
             * at which point gcc43 finally noticed that the value
             * of cur is undefined (it depends on which of the
             * summands is evaluated first).
             *
             * I haven't come across a site where this
             * code is actually executed yet, but I hope
             * it works anyway.
             */
            int skip_bytes;
            skip_bytes = *cur++;
            skip_bytes = *cur++ << 8;

            assert(skip_bytes == *csp->iob->cur - 2 + ((*csp->iob->cur - 1) << 8));

            /*
             * The number of bytes to skip should be positive
             * and we'd like to stay in the buffer.
             */
            if ((skip_bytes < 0) || (skip_bytes >= (csp->iob->eod - cur)))
            {
               log_error(LOG_LEVEL_ERROR,
                  "Unreasonable amount of bytes to skip (%d). Stopping decompression",
                  skip_bytes);
               return JB_ERR_COMPRESS;
            }
            log_error(LOG_LEVEL_INFO,
               "Skipping %d bytes for gzip compression. Does this sound right?",
               skip_bytes);
            cur += skip_bytes;
         }

         /* Skip the filename if necessary. */
         if (flags & 0x08)
         {
            /* A null-terminated string is supposed to follow. */
            while (*cur++ && (cur < csp->iob->eod));

         }

         /* Skip the comment if necessary. */
         if (flags & 0x10)
         {
            /* A null-terminated string is supposed to follow. */
            while (*cur++ && (cur < csp->iob->eod));
         }

         /* Skip the CRC if necessary. */
         if (flags & 0x02)
         {
            cur += 2;
         }

         if (cur >= csp->iob->eod)
         {
            /*
             * If the current position pointer reached or passed
             * the buffer end, we were obviously tricked to skip
             * too much.
             */
            log_error(LOG_LEVEL_ERROR,
               "Malformed gzip header detected. Aborting decompression.");
            return JB_ERR_COMPRESS;
         }
      }
   }
   else if (csp->content_type & CT_DEFLATE)
   {
      /*
       * XXX: The debug level should be lowered
       * before the next stable release.
       */
      log_error(LOG_LEVEL_INFO, "Decompressing deflated iob: %d", *cur);
      /*
       * In theory (that is, according to RFC 1950), deflate-compressed
       * data should begin with a two-byte zlib header and have an
       * adler32 checksum at the end. It seems that in practice only
       * the raw compressed data is sent. Note that this means that
       * we are not RFC 1950-compliant here, but the advantage is that
       * this actually works. :)
       *
       * We add a dummy null byte to tell zlib where the data ends,
       * and later inform it not to expect a header.
       *
       * Fortunately, add_to_iob() has thoughtfully null-terminated
       * the buffer; we can just increment the end pointer to include
       * the dummy byte.  
       */
      csp->iob->eod++;
   }
   else
   {
      log_error(LOG_LEVEL_ERROR,
         "Unable to determine compression format for decompression");
      return JB_ERR_COMPRESS;
   }

   /* Set up the fields required by zlib. */
   zstr.next_in  = (Bytef *)cur;
   zstr.avail_in = (unsigned int)(csp->iob->eod - cur);
   zstr.zalloc   = Z_NULL;
   zstr.zfree    = Z_NULL;
   zstr.opaque   = Z_NULL;

   /*
    * Passing -MAX_WBITS to inflateInit2 tells the library
    * that there is no zlib header.
    */
   if (inflateInit2 (&zstr, -MAX_WBITS) != Z_OK)
   {
      log_error(LOG_LEVEL_ERROR, "Error initializing decompression");
      return JB_ERR_COMPRESS;
   }

   /*
    * Next, we allocate new storage for the inflated data.
    * We don't modify the existing iob yet, so in case there
    * is error in decompression we can recover gracefully.
    */
   buf = zalloc(bufsize);
   if (NULL == buf)
   {
      log_error(LOG_LEVEL_ERROR, "Out of memory decompressing iob");
      return JB_ERR_MEMORY;
   }

   assert(bufsize >= skip_size);
   memcpy(buf, csp->iob->buf, skip_size);
   zstr.avail_out = bufsize - skip_size;
   zstr.next_out  = (Bytef *)buf + skip_size;

   /* Try to decompress the whole stream in one shot. */
   while (Z_BUF_ERROR == (status = inflate(&zstr, Z_FINISH)))
   {
      /* We need to allocate more memory for the output buffer. */

      char *tmpbuf;                /* used for realloc'ing the buffer */
      size_t oldbufsize = bufsize; /* keep track of the old bufsize */

      /*
       * If zlib wants more data then there's a problem, because
       * the complete compressed file should have been buffered.
       */
      if (0 == zstr.avail_in)
      {
         log_error(LOG_LEVEL_ERROR, "Unexpected end of compressed iob");
         return JB_ERR_COMPRESS;
      }

      /*
       * If we tried the limit and still didn't have enough
       * memory, just give up.
       */
      if (bufsize == csp->config->buffer_limit)
      {
         log_error(LOG_LEVEL_ERROR, "Buffer limit reached while decompressing iob");
         return JB_ERR_MEMORY;
      }

      /* Try doubling the buffer size each time. */
      bufsize *= 2;

      /* Don't exceed the buffer limit. */
      if (bufsize > csp->config->buffer_limit)
      {
         bufsize = csp->config->buffer_limit;
      }
    
      /* Try to allocate the new buffer. */
      tmpbuf = realloc(buf, bufsize);
      if (NULL == tmpbuf)
      {
         log_error(LOG_LEVEL_ERROR, "Out of memory decompressing iob");
         freez(buf);
         return JB_ERR_MEMORY;
      }
      else
      {
         char *oldnext_out = (char *)zstr.next_out;

         /*
          * Update the fields for inflate() to use the new
          * buffer, which may be in a location different from
          * the old one.
          */
         zstr.avail_out += bufsize - oldbufsize;
         zstr.next_out   = (Bytef *)tmpbuf + bufsize - zstr.avail_out;

         /*
          * Compare with an uglier method of calculating these values
          * that doesn't require the extra oldbufsize variable.
          */
         assert(zstr.avail_out == tmpbuf + bufsize - (char *)zstr.next_out);
         assert((char *)zstr.next_out == tmpbuf + ((char *)oldnext_out - buf));
         assert(zstr.avail_out > 0U);

         buf = tmpbuf;
      }
   }

   if (Z_STREAM_ERROR == inflateEnd(&zstr))
   {
      log_error(LOG_LEVEL_ERROR,
         "Inconsistent stream state after decompression: %s", zstr.msg);
      /*
       * XXX: Intentionally no return.
       *
       * According to zlib.h, Z_STREAM_ERROR is returned
       * "if the stream state was inconsistent".
       *
       * I assume in this case inflate()'s status
       * would also be something different than Z_STREAM_END
       * so this check should be redundant, but lets see.
       */
   }

   if (status != Z_STREAM_END)
   {
      /* We failed to decompress the stream. */
      log_error(LOG_LEVEL_ERROR,
         "Error in decompressing to the buffer (iob): %s", zstr.msg);
      return JB_ERR_COMPRESS;
   }

   /*
    * Finally, we can actually update the iob, since the
    * decompression was successful. First, free the old
    * buffer.
    */
   freez(csp->iob->buf);

   /* Now, update the iob to use the new buffer. */
   csp->iob->buf  = buf;
   csp->iob->cur  = csp->iob->buf + skip_size;
   csp->iob->eod  = (char *)zstr.next_out;
   csp->iob->size = bufsize;
  
   /*
    * Make sure the new uncompressed iob obeys some minimal
    * consistency conditions.
    */
   if ((csp->iob->buf <  csp->iob->cur)
    && (csp->iob->cur <= csp->iob->eod)
    && (csp->iob->eod <= csp->iob->buf + csp->iob->size))
   {
      const size_t new_size = (size_t)(csp->iob->eod - csp->iob->cur);
      if (new_size > (size_t)0)
      {
         log_error(LOG_LEVEL_RE_FILTER,
            "Decompression successful. Old size: %d, new size: %d.",
            old_size, new_size);
      }
      else
      {
         /* zlib thinks this is OK, so lets do the same. */
         log_error(LOG_LEVEL_INFO, "Decompression didn't result in any content.");
      }
   }
   else
   {
      /* It seems that zlib did something weird. */
      log_error(LOG_LEVEL_ERROR,
         "Unexpected error decompressing the buffer (iob): %d==%d, %d>%d, %d<%d",
         csp->iob->cur, csp->iob->buf + skip_size, csp->iob->eod, csp->iob->buf,
         csp->iob->eod, csp->iob->buf + csp->iob->size);
      return JB_ERR_COMPRESS;
   }

   return JB_ERR_OK;

}
#endif /* defined(FEATURE_ZLIB) */


/*********************************************************************
 *
 * Function    :  string_move
 *
 * Description :  memmove wrapper to move the last part of a string
 *                towards the beginning, overwriting the part in
 *                the middle. strlcpy() can't be used here as the
 *                strings overlap.
 *
 * Parameters  :
 *          1  :  dst = Destination to overwrite
 *          2  :  src = Source to move.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void string_move(char *dst, char *src)
{
   assert(dst < src);

   /* +1 to copy the terminating nul as well. */
   memmove(dst, src, strlen(src)+1);
}


/*********************************************************************
 *
 * Function    :  normalize_lws
 *
 * Description :  Reduces unquoted linear white space in headers
 *                to a single space in accordance with RFC 2616 2.2.
 *                This simplifies parsing and filtering later on.
 *
 *                XXX: Remove log messages before
 *                     the next stable release?
 *
 * Parameters  :
 *          1  :  header = A header with linear white space to reduce.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void normalize_lws(char *header)
{
   char *p = header;

   while (*p != '\0')
   {
      if (ijb_isspace(*p) && ijb_isspace(*(p+1)))
      {
         char *q = p+1;

         while (ijb_isspace(*q))
         {
            q++;
         }
         log_error(LOG_LEVEL_HEADER, "Reducing white space in '%s'", header);
         string_move(p+1, q);
      }

      if (*p == '\t')
      {
         log_error(LOG_LEVEL_HEADER,
            "Converting tab to space in '%s'", header);
         *p = ' ';
      }
      else if (*p == '"')
      {
         char *end_of_token = strstr(p+1, "\"");

         if (NULL != end_of_token)
         {
            /* Don't mess with quoted text. */
            p = end_of_token;
         }
         else
         {
            log_error(LOG_LEVEL_HEADER,
               "Ignoring single quote in '%s'", header);
         }
      }
      p++;
   }

   p = strchr(header, ':');
   if ((p != NULL) && (p != header) && ijb_isspace(*(p-1)))
   {
      /*
       * There's still space before the colon.
       * We don't want it.
       */
      string_move(p-1, p);
   }
}


/*********************************************************************
 *
 * Function    :  get_header
 *
 * Description :  This (odd) routine will parse the csp->iob
 *                to get the next complete header.
 *
 * Parameters  :
 *          1  :  iob = The I/O buffer to parse, usually csp->iob.
 *
 * Returns     :  Any one of the following:
 *
 * 1) a pointer to a dynamically allocated string that contains a header line
 * 2) NULL  indicating that the end of the header was reached
 * 3) ""    indicating that the end of the iob was reached before finding
 *          a complete header line.
 *
 *********************************************************************/
char *get_header(struct iob *iob)
{
   char *header;

   header = get_header_line(iob);

   if ((header == NULL) || (*header == '\0'))
   {
      /*
       * No complete header read yet, tell the client.
       */
      return header;
   }

   while ((iob->cur[0] == ' ') || (iob->cur[0] == '\t'))
   {
      /*
       * Header spans multiple lines, append the next one.
       */
      char *continued_header;
      
      continued_header = get_header_line(iob);
      if ((continued_header == NULL) || (*continued_header == '\0'))
      {
         /*
          * No complete header read yet, return what we got.
          * XXX: Should "unread" header instead.
          */
         log_error(LOG_LEVEL_INFO,
            "Failed to read a multi-line header properly: '%s'",
            header);
         break;
      }

      if (JB_ERR_OK != string_join(&header, continued_header))
      {
         log_error(LOG_LEVEL_FATAL,
            "Out of memory while appending multiple headers.");
      }
      else
      {
         /* XXX: remove before next stable release. */
         log_error(LOG_LEVEL_HEADER,
            "Merged multiple header lines to: '%s'",
            header);
      }
   }

   normalize_lws(header);

   return header;

}


/*********************************************************************
 *
 * Function    :  get_header_line
 *
 * Description :  This (odd) routine will parse the csp->iob
 *                to get the next header line.
 *
 * Parameters  :
 *          1  :  iob = The I/O buffer to parse, usually csp->iob.
 *
 * Returns     :  Any one of the following:
 *
 * 1) a pointer to a dynamically allocated string that contains a header line
 * 2) NULL  indicating that the end of the header was reached
 * 3) ""    indicating that the end of the iob was reached before finding
 *          a complete header line.
 *
 *********************************************************************/
static char *get_header_line(struct iob *iob)
{
   char *p, *q, *ret;

   if ((iob->cur == NULL)
      || ((p = strchr(iob->cur, '\n')) == NULL))
   {
      return(""); /* couldn't find a complete header */
   }

   *p = '\0';

   ret = strdup(iob->cur);
   if (ret == NULL)
   {
      /* FIXME No way to handle error properly */
      log_error(LOG_LEVEL_FATAL, "Out of memory in get_header_line()");
   }
   assert(ret != NULL);

   iob->cur = p+1;

   if ((q = strchr(ret, '\r')) != NULL) *q = '\0';

   /* is this a blank line (i.e. the end of the header) ? */
   if (*ret == '\0')
   {
      freez(ret);
      return NULL;
   }

   return ret;

}


/*********************************************************************
 *
 * Function    :  get_header_value
 *
 * Description :  Get the value of a given header from a chained list
 *                of header lines or return NULL if no such header is
 *                present in the list.
 *
 * Parameters  :
 *          1  :  header_list = pointer to list
 *          2  :  header_name = string with name of header to look for.
 *                              Trailing colon required, capitalization
 *                              doesn't matter.
 *
 * Returns     :  NULL if not found, else value of header
 *
 *********************************************************************/
char *get_header_value(const struct list *header_list, const char *header_name)
{
   struct list_entry *cur_entry;
   char *ret = NULL;
   size_t length = 0;

   assert(header_list);
   assert(header_name);
   length = strlen(header_name);

   for (cur_entry = header_list->first; cur_entry ; cur_entry = cur_entry->next)
   {
      if (cur_entry->str)
      {
         if (!strncmpic(cur_entry->str, header_name, length))
         {
            /*
             * Found: return pointer to start of value
             */
            ret = cur_entry->str + length;
            while (*ret && ijb_isspace(*ret)) ret++;
            return ret;
         }
      }
   }

   /* 
    * Not found
    */
   return NULL;

}


/*********************************************************************
 *
 * Function    :  scan_headers
 *
 * Description :  Scans headers, applies tags and updates action bits. 
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK
 *
 *********************************************************************/
static jb_err scan_headers(struct client_state *csp)
{
   struct list_entry *h; /* Header */
   jb_err err = JB_ERR_OK;

   for (h = csp->headers->first; (err == JB_ERR_OK) && (h != NULL) ; h = h->next)
   {
      /* Header crunch()ed in previous run? -> ignore */
      if (h->str == NULL) continue;
      log_error(LOG_LEVEL_HEADER, "scan: %s", h->str);
      err = header_tagger(csp, h->str);
   }

   return err;
}


/*********************************************************************
 *
 * Function    :  sed
 *
 * Description :  add, delete or modify lines in the HTTP header streams.
 *                On entry, it receives a linked list of headers space
 *                that was allocated dynamically (both the list nodes
 *                and the header contents).
 *
 *                As a side effect it frees the space used by the original
 *                header lines.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  filter_server_headers = Boolean to switch between
 *                                        server and header filtering.
 *
 * Returns     :  JB_ERR_OK in case off success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err sed(struct client_state *csp, int filter_server_headers)
{
   /* XXX: use more descriptive names. */
   struct list_entry *p;
   const struct parsers *v;
   const add_header_func_ptr *f;
   jb_err err = JB_ERR_OK;

   if (filter_server_headers)
   {
      v = server_patterns;
      f = add_server_headers;
   }
   else
   {
      v = client_patterns;
      f = add_client_headers;
   }

   scan_headers(csp);

   while ((err == JB_ERR_OK) && (v->str != NULL))
   {
      for (p = csp->headers->first; (err == JB_ERR_OK) && (p != NULL); p = p->next)
      {
         /* Header crunch()ed in previous run? -> ignore */
         if (p->str == NULL) continue;

         /* Does the current parser handle this header? */
         if ((strncmpic(p->str, v->str, v->len) == 0) ||
             (v->len == CHECK_EVERY_HEADER_REMAINING))
         {
            err = v->parser(csp, &(p->str));
         }
      }
      v++;
   }

   /* place additional headers on the csp->headers list */
   while ((err == JB_ERR_OK) && (*f))
   {
      err = (*f)(csp);
      f++;
   }

   return err;
}


/*********************************************************************
 *
 * Function    :  update_server_headers
 *
 * Description :  Updates server headers after the body has been modified.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK in case off success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err update_server_headers(struct client_state *csp)
{
   jb_err err = JB_ERR_OK;

   static const struct parsers server_patterns_light[] = {
      { "Content-Length:",    15, server_adjust_content_length },
      { "Transfer-Encoding:", 18, server_transfer_coding },
#ifdef FEATURE_ZLIB
      { "Content-Encoding:",  17, server_content_encoding },
#endif /* def FEATURE_ZLIB */
      { NULL,                  0, NULL }
   };

   if (strncmpic(csp->http->cmd, "HEAD", 4))
   {
      const struct parsers *v;
      struct list_entry *p;

      for (v = server_patterns_light; (err == JB_ERR_OK) && (v->str != NULL); v++)
      {
         for (p = csp->headers->first; (err == JB_ERR_OK) && (p != NULL); p = p->next)
         {
            /* Header crunch()ed in previous run? -> ignore */
            if (p->str == NULL) continue;

            /* Does the current parser handle this header? */
            if (strncmpic(p->str, v->str, v->len) == 0)
            {
               err = v->parser(csp, (char **)&(p->str));
            }
         }
      }
   }

   return err;
}


/*********************************************************************
 *
 * Function    :  header_tagger
 *
 * Description :  Executes all text substitutions from applying
 *                tag actions and saves the result as tag.
 *
 *                XXX: Shares enough code with filter_header() and
 *                pcrs_filter_response() to warrant some helper functions.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = Header that is used as tagger input
 *
 * Returns     :  JB_ERR_OK on success and always succeeds
 *
 *********************************************************************/
static jb_err header_tagger(struct client_state *csp, char *header)
{
   int wanted_filter_type;
   int multi_action_index;
   int i;
   pcrs_job *job;

   struct file_list *fl;
   struct re_filterfile_spec *b;
   struct list_entry *tag_name;

   int found_filters = 0;
   const size_t header_length = strlen(header);

   if (csp->flags & CSP_FLAG_CLIENT_HEADER_PARSING_DONE)
   {
      wanted_filter_type = FT_SERVER_HEADER_TAGGER;
      multi_action_index = ACTION_MULTI_SERVER_HEADER_TAGGER;
   }
   else
   {
      wanted_filter_type = FT_CLIENT_HEADER_TAGGER;
      multi_action_index = ACTION_MULTI_CLIENT_HEADER_TAGGER;
   }

   /* Check if there are any filters */
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
         "tagging enabled, but no taggers available.");
      return JB_ERR_OK;
   }

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

      /* For all filters, */
      for (b = fl->f; b; b = b->next)
      {
         if (b->type != wanted_filter_type)
         {
            /* skip the ones we don't care about, */
            continue;
         }
         /* leaving only taggers that could apply, of which we use the ones, */
         for (tag_name = csp->action->multi[multi_action_index]->first;
              NULL != tag_name; tag_name = tag_name->next)
         {
            /* that do apply, and */
            if (strcmp(b->name, tag_name->str) == 0)
            {
               char *modified_tag = NULL;
               char *tag = header;
               size_t size = header_length;
               pcrs_job *joblist = b->joblist;

               if (b->dynamic) joblist = compile_dynamic_pcrs_job_list(csp, b);

               if (NULL == joblist)
               {
                  log_error(LOG_LEVEL_RE_FILTER,
                     "Tagger %s has empty joblist. Nothing to do.", b->name);
                  continue;
               }

               /* execute their pcrs_joblist on the header. */
               for (job = joblist; NULL != job; job = job->next)
               {
                  const int hits = pcrs_execute(job, tag, size, &modified_tag, &size);

                  if (0 < hits)
                  {
                     /* Success, continue with the modified version. */
                     if (tag != header)
                     {
                        freez(tag);
                     }
                     tag = modified_tag;
                  }
                  else
                  {
                     /* Tagger doesn't match */
                     if (0 > hits)
                     {
                        /* Regex failure, log it but continue anyway. */
                        assert(NULL != header);
                        log_error(LOG_LEVEL_ERROR,
                           "Problems with tagger \'%s\' and header \'%s\': %s",
                           b->name, *header, pcrs_strerror(hits));
                     }
                     freez(modified_tag);
                  }
               }

               if (b->dynamic) pcrs_free_joblist(joblist);

               /* If this tagger matched */
               if (tag != header)
               {
                  if (0 == size)
                  {
                     /*
                      * There is to technical limitation which makes
                      * it impossible to use empty tags, but I assume
                      * no one would do it intentionally.
                      */
                     freez(tag);
                     log_error(LOG_LEVEL_INFO,
                        "Tagger \'%s\' created an empty tag. Ignored.",
                        b->name);
                     continue;
                  }
 
                  if (!list_contains_item(csp->tags, tag))
                  {
                     if (JB_ERR_OK != enlist(csp->tags, tag))
                     {
                        log_error(LOG_LEVEL_ERROR,
                           "Insufficient memory to add tag \'%s\', "
                           "based on tagger \'%s\' and header \'%s\'",
                           tag, b->name, *header);
                     }
                     else
                     {
                        char *action_message;
                        /*
                         * update the action bits right away, to make
                         * tagging based on tags set by earlier taggers
                         * of the same kind possible.
                         */
                        if (update_action_bits_for_tag(csp, tag))
                        {
                           action_message = "Action bits updated accordingly.";
                        }
                        else
                        {
                           action_message = "No action bits update necessary.";
                        }

                        log_error(LOG_LEVEL_HEADER,
                           "Tagger \'%s\' added tag \'%s\'. %s",
                           b->name, tag, action_message);
                     }
                  }
                  else
                  {
                     /* XXX: Is this log-worthy? */
                     log_error(LOG_LEVEL_HEADER,
                        "Tagger \'%s\' didn't add tag \'%s\'. "
                        "Tag already present", b->name, tag);
                  }
                  freez(tag);
               } /* if the tagger matched */
            } /* if the tagger applies */
         } /* for every tagger that could apply */
      } /* for all filters */
   } /* for all filter files */

   return JB_ERR_OK;
}

/* here begins the family of parser functions that reformat header lines */

/*********************************************************************
 *
 * Function    :  filter_header
 *
 * Description :  Executes all text substitutions from all applying
 *                +(server|client)-header-filter actions on the header.
 *                Most of the code was copied from pcrs_filter_response,
 *                including the rather short variable names
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success and always succeeds
 *
 *********************************************************************/
static jb_err filter_header(struct client_state *csp, char **header)
{
   int hits=0;
   int matches;
   size_t size = strlen(*header);

   char *newheader = NULL;
   pcrs_job *job;

   struct file_list *fl;
   struct re_filterfile_spec *b;
   struct list_entry *filtername;

   int i, found_filters = 0;
   int wanted_filter_type;
   int multi_action_index;

   if (csp->flags & CSP_FLAG_NO_FILTERING)
   {
      return JB_ERR_OK;
   }

   if (csp->flags & CSP_FLAG_CLIENT_HEADER_PARSING_DONE)
   {
      wanted_filter_type = FT_SERVER_HEADER_FILTER;
      multi_action_index = ACTION_MULTI_SERVER_HEADER_FILTER;
   }
   else
   {
      wanted_filter_type = FT_CLIENT_HEADER_FILTER;
      multi_action_index = ACTION_MULTI_CLIENT_HEADER_FILTER;
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
         "header filtering enabled, but no matching filters available.");
      return JB_ERR_OK;
   }

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
       * name exists and if yes, execute its pcrs_joblist on the
       * buffer.
       */
      for (b = fl->f; b; b = b->next)
      {
         if (b->type != wanted_filter_type)
         {
            /* Skip other filter types */
            continue;
         }

         for (filtername = csp->action->multi[multi_action_index]->first;
              filtername ; filtername = filtername->next)
         {
            if (strcmp(b->name, filtername->str) == 0)
            {
               int current_hits = 0;
               pcrs_job *joblist = b->joblist;

               if (b->dynamic) joblist = compile_dynamic_pcrs_job_list(csp, b);

               if (NULL == joblist)
               {
                  log_error(LOG_LEVEL_RE_FILTER, "Filter %s has empty joblist. Nothing to do.", b->name);
                  continue;
               }

               log_error(LOG_LEVEL_RE_FILTER, "filtering \'%s\' (size %d) with \'%s\' ...",
                         *header, size, b->name);

               /* Apply all jobs from the joblist */
               for (job = joblist; NULL != job; job = job->next)
               {
                  matches = pcrs_execute(job, *header, size, &newheader, &size);
                  if ( 0 < matches )
                  {
                     current_hits += matches; 
                     log_error(LOG_LEVEL_HEADER, "Transforming \"%s\" to \"%s\"", *header, newheader);
                     freez(*header);
                     *header = newheader;
                  }
                  else if ( 0 == matches )
                  {
                     /* Filter doesn't change header */
                     freez(newheader);
                  }
                  else
                  {
                     /* RegEx failure */
                     log_error(LOG_LEVEL_ERROR, "Filtering \'%s\' with \'%s\' didn't work out: %s",
                        *header, b->name, pcrs_strerror(matches));
                     if (newheader != NULL)
                     {
                        log_error(LOG_LEVEL_ERROR, "Freeing what's left: %s", newheader);
                        freez(newheader);
                     }
                  }
               }

               if (b->dynamic) pcrs_free_joblist(joblist);

               log_error(LOG_LEVEL_RE_FILTER, "... produced %d hits (new size %d).", current_hits, size);
               hits += current_hits;
            }
         }
      }
   }

   /*
    * Additionally checking for hits is important because if
    * the continue hack is triggered, server headers can
    * arrive empty to separate multiple heads from each other.
    */
   if ((0 == size) && hits)
   {
      log_error(LOG_LEVEL_HEADER, "Removing empty header %s", *header);
      freez(*header);
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  server_connection
 *
 * Description :  Makes sure that the value of the Connection: header
 *                is "close" and signals server_connection_close_adder 
 *                to do nothing.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_connection(struct client_state *csp, char **header)
{
   char *old_header = *header;

   /* Do we have a 'Connection: close' header? */
   if (strcmpic(*header, "Connection: close"))
   {
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
      if ((csp->config->feature_flags &
           RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE)
         && !strcmpic(*header, "Connection: keep-alive"))
      {
         /* Remember to keep the connection alive. */
         csp->flags |= CSP_FLAG_SERVER_CONNECTION_KEEP_ALIVE;
      }
#endif /* FEATURE_CONNECTION_KEEP_ALIVE */

      *header = strdup("Connection: close");
      if (header == NULL)
      { 
         return JB_ERR_MEMORY;
      }
      log_error(LOG_LEVEL_HEADER, "Replaced: \'%s\' with \'%s\'", old_header, *header);
      freez(old_header);
   }

   /* Signal server_connection_close_adder() to return early. */
   csp->flags |= CSP_FLAG_SERVER_CONNECTION_CLOSE_SET;

   return JB_ERR_OK;
}

/*********************************************************************
 *
 * Function    :  client_connection
 *
 * Description :  Makes sure a proper "Connection:" header is
 *                set and signals connection_header_adder 
 *                to do nothing.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_connection(struct client_state *csp, char **header)
{
   char *old_header = *header;
   const char *wanted_header = get_appropiate_connection_header(csp);

   if (strcmpic(*header, wanted_header))
   {
      *header = strdup(wanted_header);
      if (header == NULL)
      { 
         return JB_ERR_MEMORY;
      }
      log_error(LOG_LEVEL_HEADER,
         "Replaced: \'%s\' with \'%s\'", old_header, *header);
      freez(old_header);
   }

   /* Signal client_connection_close_adder() to return early. */
   csp->flags |= CSP_FLAG_CLIENT_CONNECTION_HEADER_SET;

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  crumble
 *
 * Description :  This is called if a header matches a pattern to "crunch"
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err crumble(struct client_state *csp, char **header)
{
   (void)csp;
   log_error(LOG_LEVEL_HEADER, "crumble crunched: %s!", *header);
   freez(*header);
   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  crunch_server_header
 *
 * Description :  Crunch server header if it matches a string supplied by the
 *                user. Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success and always succeeds
 *
 *********************************************************************/
static jb_err crunch_server_header(struct client_state *csp, char **header)
{
   const char *crunch_pattern;

   /* Do we feel like crunching? */
   if ((csp->action->flags & ACTION_CRUNCH_SERVER_HEADER))
   {
      crunch_pattern = csp->action->string[ACTION_STRING_SERVER_HEADER];

      /* Is the current header the lucky one? */
      if (strstr(*header, crunch_pattern))
      {
         log_error(LOG_LEVEL_HEADER, "Crunching server header: %s (contains: %s)", *header, crunch_pattern);  
         freez(*header);
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  server_content_type
 *
 * Description :  Set the content-type for filterable types (text/.*,
 *                .*xml.*, javascript and image/gif) unless filtering has been
 *                forbidden (CT_TABOO) while parsing earlier headers.
 *                NOTE: Since text/plain is commonly used by web servers
 *                      for files whose correct type is unknown, we don't
 *                      set CT_TEXT for it.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_content_type(struct client_state *csp, char **header)
{
   /* Remove header if it isn't the first Content-Type header */
   if ((csp->content_type & CT_DECLARED))
   {
     /*
      * Another, slightly slower, way to see if
      * we already parsed another Content-Type header.
      */
      assert(NULL != get_header_value(csp->headers, "Content-Type:"));

      log_error(LOG_LEVEL_ERROR,
         "Multiple Content-Type headers. Removing and ignoring: \'%s\'",
         *header);
      freez(*header);

      return JB_ERR_OK;
   }

   /*
    * Signal that the Content-Type has been set.
    */
   csp->content_type |= CT_DECLARED;

   if (!(csp->content_type & CT_TABOO))
   {
      /*
       * XXX: The assumption that text/plain is a sign of
       * binary data seems to be somewhat unreasonable nowadays
       * and should be dropped after 3.0.8 is out.
       */
      if ((strstr(*header, "text/") && !strstr(*header, "plain"))
        || strstr(*header, "xml")
        || strstr(*header, "application/x-javascript"))
      {
         csp->content_type |= CT_TEXT;
      }
      else if (strstr(*header, "image/gif"))
      {
         csp->content_type |= CT_GIF;
      }
   }

   /*
    * Are we messing with the content type?
    */
   if (csp->action->flags & ACTION_CONTENT_TYPE_OVERWRITE)
   {
      /*
       * Make sure the user doesn't accidently
       * change the content type of binary documents. 
       */
      if ((csp->content_type & CT_TEXT) || (csp->action->flags & ACTION_FORCE_TEXT_MODE))
      {
         freez(*header);
         *header = strdup("Content-Type: ");
         string_append(header, csp->action->string[ACTION_STRING_CONTENT_TYPE]);

         if (header == NULL)
         {
            log_error(LOG_LEVEL_HEADER, "Insufficient memory to replace Content-Type!");
            return JB_ERR_MEMORY;
         }
         log_error(LOG_LEVEL_HEADER, "Modified: %s!", *header);
      }
      else
      {
         log_error(LOG_LEVEL_HEADER, "%s not replaced. "
            "It doesn't look like a content type that should be filtered. "
            "Enable force-text-mode if you know what you're doing.", *header);
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  server_transfer_coding
 *
 * Description :  - Prohibit filtering (CT_TABOO) if transfer coding compresses
 *                - Raise the CSP_FLAG_CHUNKED flag if coding is "chunked"
 *                - Remove header if body was chunked but has been
 *                  de-chunked for filtering.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_transfer_coding(struct client_state *csp, char **header)
{
   /*
    * Turn off pcrs and gif filtering if body compressed
    */
   if (strstr(*header, "gzip") || strstr(*header, "compress") || strstr(*header, "deflate"))
   {
#ifdef FEATURE_ZLIB
      /*
       * XXX: Added to test if we could use CT_GZIP and CT_DEFLATE here.
       */
      log_error(LOG_LEVEL_INFO, "Marking content type for %s as CT_TABOO because of %s.",
         csp->http->cmd, *header);
#endif /* def FEATURE_ZLIB */
      csp->content_type = CT_TABOO;
   }

   /*
    * Raise flag if body chunked
    */
   if (strstr(*header, "chunked"))
   {
      csp->flags |= CSP_FLAG_CHUNKED;

      /*
       * If the body was modified, it has been de-chunked first
       * and the header must be removed.
       *
       * FIXME: If there is more than one transfer encoding,
       * only the "chunked" part should be removed here.
       */
      if (csp->flags & CSP_FLAG_MODIFIED)
      {
         log_error(LOG_LEVEL_HEADER, "Removing: %s", *header);
         freez(*header);
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  server_content_encoding
 *
 * Description :  This function is run twice for each request,
 *                unless FEATURE_ZLIB and filtering are disabled.
 *
 *                The first run is used to check if the content
 *                is compressed, if FEATURE_ZLIB is disabled
 *                filtering is then disabled as well, if FEATURE_ZLIB
 *                is enabled the content is marked for decompression.
 *                
 *                The second run is used to remove the Content-Encoding
 *                header if the decompression was successful.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_content_encoding(struct client_state *csp, char **header)
{
#ifdef FEATURE_ZLIB
   if ((csp->flags & CSP_FLAG_MODIFIED)
    && (csp->content_type & (CT_GZIP | CT_DEFLATE)))
   {
      /*
       * We successfully decompressed the content,
       * and have to clean the header now, so the
       * client no longer expects compressed data..
       *
       * XXX: There is a difference between cleaning
       * and removing it completely.
       */
      log_error(LOG_LEVEL_HEADER, "Crunching: %s", *header);
      freez(*header);
   }
   else if (strstr(*header, "gzip"))
   {
      /* Mark for gzip decompression */
      csp->content_type |= CT_GZIP;
   }
   else if (strstr(*header, "deflate"))
   {
      /* Mark for zlib decompression */
      csp->content_type |= CT_DEFLATE;
   }
   else if (strstr(*header, "compress"))
   {
      /*
       * We can't decompress this; therefore we can't filter
       * it either.
       */
      csp->content_type |= CT_TABOO;
   }
#else /* !defined(FEATURE_ZLIB) */
   if (strstr(*header, "gzip") || strstr(*header, "compress") || strstr(*header, "deflate"))
   {
      /*
       * Body is compressed, turn off pcrs and gif filtering.
       */
      csp->content_type |= CT_TABOO;

      /*
       * Log a warning if the user expects the content to be filtered.
       */
      if ((csp->rlist != NULL) &&
         (!list_is_empty(csp->action->multi[ACTION_MULTI_FILTER])))
      {
         log_error(LOG_LEVEL_INFO,
            "Compressed content detected, content filtering disabled. "
            "Consider recompiling Privoxy with zlib support or "
            "enable the prevent-compression action.");
      }
   }
#endif /* defined(FEATURE_ZLIB) */

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  server_adjust_content_length
 *
 * Description :  Adjust Content-Length header if we modified
 *                the body.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_adjust_content_length(struct client_state *csp, char **header)
{
   const size_t max_header_length = 80;

   /* Regenerate header if the content was modified. */
   if (csp->flags & CSP_FLAG_MODIFIED)
   {
      freez(*header);
      *header = (char *) zalloc(max_header_length);
      if (*header == NULL)
      {
         return JB_ERR_MEMORY;
      }

      snprintf(*header, max_header_length, "Content-Length: %d",
         (int)csp->content_length);
      log_error(LOG_LEVEL_HEADER, "Adjusted Content-Length to %d",
         (int)csp->content_length);
   }

   return JB_ERR_OK;
}


#ifdef FEATURE_CONNECTION_KEEP_ALIVE
/*********************************************************************
 *
 * Function    :  server_save_content_length
 *
 * Description :  Save the Content-Length sent by the server.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_save_content_length(struct client_state *csp, char **header)
{
   unsigned long long content_length = 0;

   assert(*(*header+14) == ':');

   if (1 != sscanf(*header+14, ": %llu", &content_length))
   {
      log_error(LOG_LEVEL_ERROR, "Crunching invalid header: %s", *header);
      freez(*header);
   }
   else
   {
      csp->expected_content_length = content_length;
      csp->flags |= CSP_FLAG_CONTENT_LENGTH_SET;
   }

   return JB_ERR_OK;
}
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */


/*********************************************************************
 *
 * Function    :  server_content_md5
 *
 * Description :  Crumble any Content-MD5 headers if the document was
 *                modified. FIXME: Should we re-compute instead?
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_content_md5(struct client_state *csp, char **header)
{
   if (csp->flags & CSP_FLAG_MODIFIED)
   {
      log_error(LOG_LEVEL_HEADER, "Crunching Content-MD5");
      freez(*header);
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  server_content_disposition
 *
 * Description :  If enabled, blocks or modifies the "Content-Disposition" header.
 *                Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_content_disposition(struct client_state *csp, char **header)
{
   const char *newval;

   /*
    * Are we messing with the Content-Disposition header?
    */
   if ((csp->action->flags & ACTION_HIDE_CONTENT_DISPOSITION) == 0)
   {
      /* Me tinks not */
      return JB_ERR_OK;
   }

   newval = csp->action->string[ACTION_STRING_CONTENT_DISPOSITION];

   if ((newval == NULL) || (0 == strcmpic(newval, "block")))
   {
      /*
       * Blocking content-disposition header
       */
      log_error(LOG_LEVEL_HEADER, "Crunching %s!", *header);
      freez(*header);
      return JB_ERR_OK;
   }
   else
   {  
      /*
       * Replacing Content-Disposition header
       */
      freez(*header);
      *header = strdup("Content-Disposition: ");
      string_append(header, newval);

      if (*header != NULL)
      {
         log_error(LOG_LEVEL_HEADER,
            "Content-Disposition header crunched and replaced with: %s", *header);
      }
   }
   return (*header == NULL) ? JB_ERR_MEMORY : JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  server_last_modified
 *
 * Description :  Changes Last-Modified header to the actual date
 *                to help hide-if-modified-since.
 *                Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_last_modified(struct client_state *csp, char **header)
{
   const char *newval;
   char buf[BUFFER_SIZE];

   char newheader[50];
#ifdef HAVE_GMTIME_R
   struct tm gmt;
#endif
   struct tm *timeptr = NULL;
   time_t now, last_modified;                  
   long int rtime;
   long int days, hours, minutes, seconds;
   
   /*
    * Are we messing with the Last-Modified header?
    */
   if ((csp->action->flags & ACTION_OVERWRITE_LAST_MODIFIED) == 0)
   {
      /*Nope*/
      return JB_ERR_OK;
   }

   newval = csp->action->string[ACTION_STRING_LAST_MODIFIED];

   if (0 == strcmpic(newval, "block") )
   {
      /*
       * Blocking Last-Modified header. Useless but why not.
       */
      log_error(LOG_LEVEL_HEADER, "Crunching %s!", *header);
      freez(*header);
      return JB_ERR_OK;
   }
   else if (0 == strcmpic(newval, "reset-to-request-time"))
   {  
      /*
       * Setting Last-Modified Header to now.
       */
      get_http_time(0, buf, sizeof(buf));
      freez(*header);
      *header = strdup("Last-Modified: ");
      string_append(header, buf);   

      if (*header == NULL)
      {
         log_error(LOG_LEVEL_HEADER, "Insufficient memory. Last-Modified header got lost, boohoo.");  
      }
      else
      {
         log_error(LOG_LEVEL_HEADER, "Reset to present time: %s", *header);
      }
   }
   else if (0 == strcmpic(newval, "randomize"))
   {
      const char *header_time = *header + sizeof("Last-Modified:");

      log_error(LOG_LEVEL_HEADER, "Randomizing: %s", *header);
      now = time(NULL);
#ifdef HAVE_GMTIME_R
      timeptr = gmtime_r(&now, &gmt);
#elif FEATURE_PTHREAD
      privoxy_mutex_lock(&gmtime_mutex);
      timeptr = gmtime(&now);
      privoxy_mutex_unlock(&gmtime_mutex);
#else
      timeptr = gmtime(&now);
#endif
      if (JB_ERR_OK != parse_header_time(header_time, &last_modified))
      {
         log_error(LOG_LEVEL_HEADER, "Couldn't parse: %s in %s (crunching!)", header_time, *header);
         freez(*header);
      }
      else
      {
         rtime = (long int)difftime(now, last_modified);
         if (rtime)
         {
            int negative = 0;

            if (rtime < 0)
            {
               rtime *= -1; 
               negative = 1;
               log_error(LOG_LEVEL_HEADER, "Server time in the future.");
            }
            rtime = pick_from_range(rtime);
            if (negative) rtime *= -1;
            last_modified += rtime;
#ifdef HAVE_GMTIME_R
            timeptr = gmtime_r(&last_modified, &gmt);
#elif FEATURE_PTHREAD
            privoxy_mutex_lock(&gmtime_mutex);
            timeptr = gmtime(&last_modified);
            privoxy_mutex_unlock(&gmtime_mutex);
#else
            timeptr = gmtime(&last_modified);
#endif
            strftime(newheader, sizeof(newheader), "%a, %d %b %Y %H:%M:%S GMT", timeptr);
            freez(*header);
            *header = strdup("Last-Modified: ");
            string_append(header, newheader);

            if (*header == NULL)
            {
               log_error(LOG_LEVEL_ERROR, "Insufficient memory, header crunched without replacement.");
               return JB_ERR_MEMORY;  
            }

            days    = rtime / (3600 * 24);
            hours   = rtime / 3600 % 24;
            minutes = rtime / 60 % 60;
            seconds = rtime % 60;

            log_error(LOG_LEVEL_HEADER,
               "Randomized:  %s (added %d da%s %d hou%s %d minut%s %d second%s",
               *header, days, (days == 1) ? "y" : "ys", hours, (hours == 1) ? "r" : "rs",
               minutes, (minutes == 1) ? "e" : "es", seconds, (seconds == 1) ? ")" : "s)");
         }
         else
         {
            log_error(LOG_LEVEL_HEADER, "Randomized ... or not. No time difference to work with.");
         }
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_accept_encoding
 *
 * Description :  Rewrite the client's Accept-Encoding header so that
 *                if doesn't allow compression, if the action applies.
 *                Note: For HTTP/1.0 the absence of the header is enough.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_accept_encoding(struct client_state *csp, char **header)
{
   if ((csp->action->flags & ACTION_NO_COMPRESSION) != 0)
   {
      log_error(LOG_LEVEL_HEADER, "Suppressed offer to compress content");

      freez(*header);

      /* Temporarily disable the correct behaviour to
       * work around a PHP bug. 
       *
       * if (!strcmpic(csp->http->ver, "HTTP/1.1"))
       * {
       *    *header = strdup("Accept-Encoding: identity;q=1.0, *;q=0");
       *    if (*header == NULL)
       *    {
       *       return JB_ERR_MEMORY;
       *    }
       * }
       * 
       */
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_te
 *
 * Description :  Rewrite the client's TE header so that
 *                if doesn't allow compression, if the action applies.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_te(struct client_state *csp, char **header)
{
   if ((csp->action->flags & ACTION_NO_COMPRESSION) != 0)
   {
      freez(*header);
      log_error(LOG_LEVEL_HEADER, "Suppressed offer to compress transfer");
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_referrer
 *
 * Description :  Handle the "referer" config setting properly.
 *                Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_referrer(struct client_state *csp, char **header)
{
   const char *parameter;
   /* booleans for parameters we have to check multiple times */
   int parameter_conditional_block;
   int parameter_conditional_forge;
 
#ifdef FEATURE_FORCE_LOAD
   /*
    * Since the referrer can include the prefix even
    * if the request itself is non-forced, we must
    * clean it unconditionally.
    *
    * XXX: strclean is too broad
    */
   strclean(*header, FORCE_PREFIX);
#endif /* def FEATURE_FORCE_LOAD */

   if ((csp->action->flags & ACTION_HIDE_REFERER) == 0)
   {
      /* Nothing left to do */
      return JB_ERR_OK;
   }

   parameter = csp->action->string[ACTION_STRING_REFERER];
   assert(parameter != NULL);
   parameter_conditional_block = (0 == strcmpic(parameter, "conditional-block"));
   parameter_conditional_forge = (0 == strcmpic(parameter, "conditional-forge"));

   if (!parameter_conditional_block && !parameter_conditional_forge)
   {
      /*
       * As conditional-block and conditional-forge are the only
       * parameters that rely on the original referrer, we can
       * remove it now for all the others.
       */
      freez(*header);
   }

   if (0 == strcmpic(parameter, "block"))
   {
      log_error(LOG_LEVEL_HEADER, "Referer crunched!");
      return JB_ERR_OK;
   }
   else if (parameter_conditional_block || parameter_conditional_forge)
   {
      return handle_conditional_hide_referrer_parameter(header,
         csp->http->hostport, parameter_conditional_block);
   }
   else if (0 == strcmpic(parameter, "forge"))
   {
      return create_forged_referrer(header, csp->http->hostport);
   }
   else
   {
      /* interpret parameter as user-supplied referer to fake */
      return create_fake_referrer(header, parameter);
   }
}


/*********************************************************************
 *
 * Function    :  client_accept_language
 *
 * Description :  Handle the "Accept-Language" config setting properly.
 *                Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_accept_language(struct client_state *csp, char **header)
{
   const char *newval;

   /*
    * Are we messing with the Accept-Language?
    */
   if ((csp->action->flags & ACTION_HIDE_ACCEPT_LANGUAGE) == 0)
   {
      /*I don't think so*/
      return JB_ERR_OK;
   }

   newval = csp->action->string[ACTION_STRING_LANGUAGE];

   if ((newval == NULL) || (0 == strcmpic(newval, "block")) )
   {
      /*
       * Blocking Accept-Language header
       */
      log_error(LOG_LEVEL_HEADER, "Crunching Accept-Language!");
      freez(*header);
      return JB_ERR_OK;
   }
   else
   {  
      /*
       * Replacing Accept-Language header
       */
      freez(*header);
      *header = strdup("Accept-Language: ");
      string_append(header, newval);   

      if (*header == NULL)
      {
         log_error(LOG_LEVEL_ERROR,
            "Insufficient memory. Accept-Language header crunched without replacement.");  
      }
      else
      {
         log_error(LOG_LEVEL_HEADER,
            "Accept-Language header crunched and replaced with: %s", *header);
      }
   }
   return (*header == NULL) ? JB_ERR_MEMORY : JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  crunch_client_header
 *
 * Description :  Crunch client header if it matches a string supplied by the
 *                user. Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success and always succeeds
 *
 *********************************************************************/
static jb_err crunch_client_header(struct client_state *csp, char **header)
{
   const char *crunch_pattern;

   /* Do we feel like crunching? */
   if ((csp->action->flags & ACTION_CRUNCH_CLIENT_HEADER))
   {
      crunch_pattern = csp->action->string[ACTION_STRING_CLIENT_HEADER];

      /* Is the current header the lucky one? */
      if (strstr(*header, crunch_pattern))
      {
         log_error(LOG_LEVEL_HEADER, "Crunching client header: %s (contains: %s)", *header, crunch_pattern);  
         freez(*header);
      }
   }
   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_uagent
 *
 * Description :  Handle the "user-agent" config setting properly
 *                and remember its original value to enable browser
 *                bug workarounds. Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_uagent(struct client_state *csp, char **header)
{
   const char *newval;

   if ((csp->action->flags & ACTION_HIDE_USER_AGENT) == 0)
   {
      return JB_ERR_OK;
   }

   newval = csp->action->string[ACTION_STRING_USER_AGENT];
   if (newval == NULL)
   {
      return JB_ERR_OK;
   }

   freez(*header);
   *header = strdup("User-Agent: ");
   string_append(header, newval);

   log_error(LOG_LEVEL_HEADER, "Modified: %s", *header);

   return (*header == NULL) ? JB_ERR_MEMORY : JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_ua
 *
 * Description :  Handle "ua-" headers properly.  Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_ua(struct client_state *csp, char **header)
{
   if ((csp->action->flags & ACTION_HIDE_USER_AGENT) != 0)
   {
      log_error(LOG_LEVEL_HEADER, "crunched User-Agent!");
      freez(*header);
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_from
 *
 * Description :  Handle the "from" config setting properly.
 *                Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_from(struct client_state *csp, char **header)
{
   const char *newval;

   if ((csp->action->flags & ACTION_HIDE_FROM) == 0)
   {
      return JB_ERR_OK;
   }

   freez(*header);

   newval = csp->action->string[ACTION_STRING_FROM];

   /*
    * Are we blocking the e-mail address?
    */
   if ((newval == NULL) || (0 == strcmpic(newval, "block")) )
   {
      log_error(LOG_LEVEL_HEADER, "crunched From!");
      return JB_ERR_OK;
   }

   log_error(LOG_LEVEL_HEADER, " modified");

   *header = strdup("From: ");
   string_append(header, newval);

   return (*header == NULL) ? JB_ERR_MEMORY : JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_send_cookie
 *
 * Description :  Crunches the "cookie" header if necessary.
 *                Called from `sed'.
 *
 *                XXX: Stupid name, doesn't send squat.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_send_cookie(struct client_state *csp, char **header)
{
   if (csp->action->flags & ACTION_NO_COOKIE_READ)
   {
      log_error(LOG_LEVEL_HEADER, "Crunched outgoing cookie: %s", *header);
      freez(*header);
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_x_forwarded
 *
 * Description :  Handle the "x-forwarded-for" config setting properly,
 *                also used in the add_client_headers list.  Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
jb_err client_x_forwarded(struct client_state *csp, char **header)
{
   if (0 != (csp->action->flags & ACTION_CHANGE_X_FORWARDED_FOR))
   {
      const char *parameter = csp->action->string[ACTION_STRING_CHANGE_X_FORWARDED_FOR];

      if (0 == strcmpic(parameter, "block"))
      {
         freez(*header);
         log_error(LOG_LEVEL_HEADER, "crunched x-forwarded-for!");
      }
      else if (0 == strcmpic(parameter, "add"))
      {
         string_append(header, ", ");
         string_append(header, csp->ip_addr_str);

         if (*header == NULL)
         {
            return JB_ERR_MEMORY;
         }
         log_error(LOG_LEVEL_HEADER,
            "Appended client IP address to %s", *header);
         csp->flags |= CSP_FLAG_X_FORWARDED_FOR_APPENDED;
      }
      else
      {
         log_error(LOG_LEVEL_FATAL,
            "Invalid change-x-forwarded-for parameter: '%s'", parameter);
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_max_forwards
 *
 * Description :  If the HTTP method is OPTIONS or TRACE, subtract one
 *                from the value of the Max-Forwards header field.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_max_forwards(struct client_state *csp, char **header)
{
   int max_forwards;

   if ((0 == strcmpic(csp->http->gpc, "trace")) ||
       (0 == strcmpic(csp->http->gpc, "options")))
   {
      assert(*(*header+12) == ':');
      if (1 == sscanf(*header+12, ": %d", &max_forwards))
      {
         if (max_forwards > 0)
         {
            snprintf(*header, strlen(*header)+1, "Max-Forwards: %d", --max_forwards);
            log_error(LOG_LEVEL_HEADER,
               "Max-Forwards value for %s request reduced to %d.",
               csp->http->gpc, max_forwards);
         }
         else if (max_forwards < 0)
         {
            log_error(LOG_LEVEL_ERROR, "Crunching invalid header: %s", *header);
            freez(*header);
         }
      }
      else
      {
         log_error(LOG_LEVEL_ERROR, "Crunching invalid header: %s", *header);
         freez(*header);
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_host
 *
 * Description :  If the request URI did not contain host and
 *                port information, parse and evaluate the Host
 *                header field.
 *
 *                Also, kill ill-formed HOST: headers as sent by
 *                Apple's iTunes software when used with a proxy.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_host(struct client_state *csp, char **header)
{
   char *p, *q;

   /*
    * If the header field name is all upper-case, chances are that it's
    * an ill-formed one from iTunes. BTW, killing innocent headers here is
    * not a problem -- they are regenerated later.
    */
   if ((*header)[1] == 'O')
   {
      log_error(LOG_LEVEL_HEADER, "Killed all-caps Host header line: %s", *header);
      freez(*header);
      return JB_ERR_OK;
   }

   if (!csp->http->hostport || (*csp->http->hostport == '*') ||  
       *csp->http->hostport == ' ' || *csp->http->hostport == '\0')
   {
      
      if (NULL == (p = strdup((*header)+6)))
      {
         return JB_ERR_MEMORY;
      }
      chomp(p);
      if (NULL == (q = strdup(p)))
      {
         freez(p);
         return JB_ERR_MEMORY;
      }

      freez(csp->http->hostport);
      csp->http->hostport = p;
      freez(csp->http->host);
      csp->http->host = q;
      q = strchr(csp->http->host, ':');
      if (q != NULL)
      {
         /* Terminate hostname and evaluate port string */
         *q++ = '\0';
         csp->http->port = atoi(q);
      }
      else
      {
         csp->http->port = csp->http->ssl ? 443 : 80;
      }

      log_error(LOG_LEVEL_HEADER, "New host and port from Host field: %s = %s:%d",
                csp->http->hostport, csp->http->host, csp->http->port);
   }

   /* Signal client_host_adder() to return right away */
   csp->flags |= CSP_FLAG_HOST_HEADER_IS_SET;

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_if_modified_since
 *
 * Description :  Remove or modify the If-Modified-Since header.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_if_modified_since(struct client_state *csp, char **header)
{
   char newheader[50];
#ifdef HAVE_GMTIME_R
   struct tm gmt;
#endif
   struct tm *timeptr = NULL;
   time_t tm = 0;                  
   const char *newval;
   long int rtime;
   long int hours, minutes, seconds;
   int negative = 0;
   char * endptr;
   
   if ( 0 == strcmpic(*header, "If-Modified-Since: Wed, 08 Jun 1955 12:00:00 GMT"))
   {
      /* 
       * The client got an error message because of a temporary problem,
       * the problem is gone and the client now tries to revalidate our
       * error message on the real server. The revalidation would always
       * end with the transmission of the whole document and there is
       * no need to expose the bogus If-Modified-Since header.
       */
      log_error(LOG_LEVEL_HEADER, "Crunching useless If-Modified-Since header.");
      freez(*header);
   }
   else if (csp->action->flags & ACTION_HIDE_IF_MODIFIED_SINCE)
   {
      newval = csp->action->string[ACTION_STRING_IF_MODIFIED_SINCE];

      if ((0 == strcmpic(newval, "block")))
      {
         log_error(LOG_LEVEL_HEADER, "Crunching %s", *header);
         freez(*header);
      }
      else /* add random value */
      {
         const char *header_time = *header + sizeof("If-Modified-Since:");

         if (JB_ERR_OK != parse_header_time(header_time, &tm))
         {
            log_error(LOG_LEVEL_HEADER, "Couldn't parse: %s in %s (crunching!)", header_time, *header);
            freez(*header);
         }
         else
         {
            rtime = strtol(newval, &endptr, 0);
            if (rtime)
            {
               log_error(LOG_LEVEL_HEADER, "Randomizing: %s (random range: %d minut%s)",
                  *header, rtime, (rtime == 1 || rtime == -1) ? "e": "es");
               if (rtime < 0)
               {
                  rtime *= -1; 
                  negative = 1;
               }
               rtime *= 60;
               rtime = pick_from_range(rtime);
            }
            else
            {
               log_error(LOG_LEVEL_ERROR, "Random range is 0. Assuming time transformation test.",
                  *header);
            }
            tm += rtime * (negative ? -1 : 1);
#ifdef HAVE_GMTIME_R
            timeptr = gmtime_r(&tm, &gmt);
#elif FEATURE_PTHREAD
            privoxy_mutex_lock(&gmtime_mutex);
            timeptr = gmtime(&tm);
            privoxy_mutex_unlock(&gmtime_mutex);
#else
            timeptr = gmtime(&tm);
#endif
            strftime(newheader, sizeof(newheader), "%a, %d %b %Y %H:%M:%S GMT", timeptr);

            freez(*header);
            *header = strdup("If-Modified-Since: ");
            string_append(header, newheader);

            if (*header == NULL)
            {
               log_error(LOG_LEVEL_HEADER, "Insufficient memory, header crunched without replacement.");
               return JB_ERR_MEMORY;  
            }

            hours   = rtime / 3600;
            minutes = rtime / 60 % 60;
            seconds = rtime % 60;

            log_error(LOG_LEVEL_HEADER,
               "Randomized:  %s (%s %d hou%s %d minut%s %d second%s",
               *header, (negative) ? "subtracted" : "added", hours,
               (hours == 1) ? "r" : "rs", minutes, (minutes == 1) ? "e" : "es",
               seconds, (seconds == 1) ? ")" : "s)");
         }
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_if_none_match
 *
 * Description :  Remove the If-None-Match header.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_if_none_match(struct client_state *csp, char **header)
{
   if (csp->action->flags & ACTION_CRUNCH_IF_NONE_MATCH)
   {  
      log_error(LOG_LEVEL_HEADER, "Crunching %s", *header);
      freez(*header);
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_x_filter
 *
 * Description :  Disables filtering if the client set "X-Filter: No".
 *                Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success
 *
 *********************************************************************/
jb_err client_x_filter(struct client_state *csp, char **header)
{
   if ( 0 == strcmpic(*header, "X-Filter: No"))
   {
      if (!(csp->config->feature_flags & RUNTIME_FEATURE_HTTP_TOGGLE))
      {
         log_error(LOG_LEVEL_INFO, "Ignored the client's request to fetch without filtering.");
      }
      else
      {
         if (csp->action->flags & ACTION_FORCE_TEXT_MODE)
         {
            log_error(LOG_LEVEL_HEADER,
               "force-text-mode overruled the client's request to fetch without filtering!");
         }
         else
         {  
            csp->content_type = CT_TABOO; /* XXX: This hack shouldn't be necessary */
            csp->flags |= CSP_FLAG_NO_FILTERING;
            log_error(LOG_LEVEL_HEADER, "Accepted the client's request to fetch without filtering.");
         }
         log_error(LOG_LEVEL_HEADER, "Crunching %s", *header);
         freez(*header);
      }
   }
   return JB_ERR_OK; 
}


/*********************************************************************
 *
 * Function    :  client_range
 *
 * Description :  Removes Range, Request-Range and If-Range headers if
 *                content filtering is enabled. If the client's version
 *                of the document has been altered by Privoxy, the server
 *                could interpret the range differently than the client
 *                intended in which case the user could end up with
 *                corrupted content.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK
 *
 *********************************************************************/
static jb_err client_range(struct client_state *csp, char **header)
{
   if (content_filters_enabled(csp->action))
   {
      log_error(LOG_LEVEL_HEADER, "Content filtering is enabled."
         " Crunching: \'%s\' to prevent range-mismatch problems.", *header);
      freez(*header);
   }

   return JB_ERR_OK; 
}

/* the following functions add headers directly to the header list */

/*********************************************************************
 *
 * Function    :  client_host_adder
 *
 * Description :  Adds the Host: header field if it is missing.
 *                Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_host_adder(struct client_state *csp)
{
   char *p;
   jb_err err;

   if (csp->flags & CSP_FLAG_HOST_HEADER_IS_SET)
   {
      /* Header already set by the client, nothing to do. */
      return JB_ERR_OK;
   }

   if ( !csp->http->hostport || !*(csp->http->hostport))
   {
      /* XXX: When does this happen and why is it OK? */
      log_error(LOG_LEVEL_INFO, "Weirdness in client_host_adder detected and ignored.");
      return JB_ERR_OK;
   }

   /*
    * remove 'user:pass@' from 'proto://user:pass@host'
    */
   if ( (p = strchr( csp->http->hostport, '@')) != NULL )
   {
      p++;
   }
   else
   {
      p = csp->http->hostport;
   }

   /* XXX: Just add it, we already made sure that it will be unique */
   log_error(LOG_LEVEL_HEADER, "addh-unique: Host: %s", p);
   err = enlist_unique_header(csp->headers, "Host", p);
   return err;

}


#if 0
/*********************************************************************
 *
 * Function    :  client_accept_encoding_adder
 *
 * Description :  Add an Accept-Encoding header to the client's request
 *                that disables compression if the action applies, and
 *                the header is not already there. Called from `sed'.
 *                Note: For HTTP/1.0, the absence of the header is enough.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_accept_encoding_adder(struct client_state *csp)
{
   if (   ((csp->action->flags & ACTION_NO_COMPRESSION) != 0)
       && (!strcmpic(csp->http->ver, "HTTP/1.1")) )
   {
      return enlist_unique(csp->headers, "Accept-Encoding: identity;q=1.0, *;q=0", 16);
   }

   return JB_ERR_OK;
}
#endif


/*********************************************************************
 *
 * Function    :  client_xtra_adder
 *
 * Description :  Used in the add_client_headers list.  Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_xtra_adder(struct client_state *csp)
{
   struct list_entry *lst;
   jb_err err;

   for (lst = csp->action->multi[ACTION_MULTI_ADD_HEADER]->first;
        lst ; lst = lst->next)
   {
      log_error(LOG_LEVEL_HEADER, "addh: %s", lst->str);
      err = enlist(csp->headers, lst->str);
      if (err)
      {
         return err;
      }

   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  client_x_forwarded_for_adder
 *
 * Description :  Used in the add_client_headers list.  Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_x_forwarded_for_adder(struct client_state *csp)
{
   char *header = NULL;
   jb_err err;

   if (!((csp->action->flags & ACTION_CHANGE_X_FORWARDED_FOR)
         && (0 == strcmpic(csp->action->string[ACTION_STRING_CHANGE_X_FORWARDED_FOR], "add")))
      || (csp->flags & CSP_FLAG_X_FORWARDED_FOR_APPENDED))
   {
      /*
       * If we aren't adding X-Forwarded-For headers,
       * or we already appended an existing X-Forwarded-For
       * header, there's nothing left to do here.
       */
      return JB_ERR_OK;
   }

   header = strdup("X-Forwarded-For: ");
   string_append(&header, csp->ip_addr_str);

   if (header == NULL)
   {
      return JB_ERR_MEMORY;
   }

   log_error(LOG_LEVEL_HEADER, "addh: %s", header);
   err = enlist(csp->headers, header);
   freez(header);

   return err;
}


/*********************************************************************
 *
 * Function    :  server_connection_close_adder
 *
 * Description :  "Temporary" fix for the needed but missing HTTP/1.1
 *                support. Adds a "Connection: close" header to csp->headers
 *                unless the header was already present. Called from `sed'.
 *
 *                FIXME: This whole function shouldn't be neccessary!
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_connection_close_adder(struct client_state *csp)
{
   const unsigned int flags = csp->flags;
   const char *response_status_line = csp->headers->first->str;

   if ((flags & CSP_FLAG_CLIENT_HEADER_PARSING_DONE)
    && (flags & CSP_FLAG_SERVER_CONNECTION_CLOSE_SET))
   {
      return JB_ERR_OK;
   }

   /*
    * XXX: if we downgraded the response, this check will fail.
    */
   if ((csp->config->feature_flags &
        RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE)
    && (NULL != response_status_line)
    && !strncmpic(response_status_line, "HTTP/1.1", 8))
   {
      log_error(LOG_LEVEL_HEADER, "A HTTP/1.1 response "
         "without Connection header implies keep-alive.");
      csp->flags |= CSP_FLAG_SERVER_CONNECTION_KEEP_ALIVE;
   }

   log_error(LOG_LEVEL_HEADER, "Adding: Connection: close");

   return enlist(csp->headers, "Connection: close");
}


/*********************************************************************
 *
 * Function    :  client_connection_header_adder
 *
 * Description :  Adds a proper "Connection:" header to csp->headers
 *                unless the header was already present. Called from `sed'.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err client_connection_header_adder(struct client_state *csp)
{
   const unsigned int flags = csp->flags;
   const char *wanted_header = get_appropiate_connection_header(csp);

   if (!(flags & CSP_FLAG_CLIENT_HEADER_PARSING_DONE)
     && (flags & CSP_FLAG_CLIENT_CONNECTION_HEADER_SET))
   {
      return JB_ERR_OK;
   }

   log_error(LOG_LEVEL_HEADER, "Adding: %s", wanted_header);

   return enlist(csp->headers, wanted_header);
}


/*********************************************************************
 *
 * Function    :  server_http
 *
 * Description :  - Save the HTTP Status into csp->http->status
 *                - Set CT_TABOO to prevent filtering if the answer
 *                  is a partial range (HTTP status 206)
 *                - Rewrite HTTP/1.1 answers to HTTP/1.0 if +downgrade
 *                  action applies.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_http(struct client_state *csp, char **header)
{
   sscanf(*header, "HTTP/%*d.%*d %d", &(csp->http->status));
   if (csp->http->status == 206)
   {
      csp->content_type = CT_TABOO;
   }

   if ((csp->action->flags & ACTION_DOWNGRADE) != 0)
   {
      /* XXX: Should we do a real validity check here? */
      if (strlen(*header) > 8)
      {
         (*header)[7] = '0';
         log_error(LOG_LEVEL_HEADER, "Downgraded answer to HTTP/1.0");
      }
      else
      {
         /*
          * XXX: Should we block the request or
          * enlist a valid status code line here?
          */
         log_error(LOG_LEVEL_INFO, "Malformed server response detected. "
            "Downgrading to HTTP/1.0 impossible.");
      }
   }

   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  server_set_cookie
 *
 * Description :  Handle the server "cookie" header properly.
 *                Log cookie to the jar file.  Then "crunch",
 *                accept or rewrite it to a session cookie.
 *                Called from `sed'.
 *
 *                TODO: Allow the user to specify a new expiration
 *                time to cause the cookie to expire even before the
 *                browser is closed.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  header = On input, pointer to header to modify.
 *                On output, pointer to the modified header, or NULL
 *                to remove the header.  This function frees the
 *                original string if necessary.
 *
 * Returns     :  JB_ERR_OK on success, or
 *                JB_ERR_MEMORY on out-of-memory error.
 *
 *********************************************************************/
static jb_err server_set_cookie(struct client_state *csp, char **header)
{
   time_t now;
   time_t cookie_time; 

   time(&now);

   if ((csp->action->flags & ACTION_NO_COOKIE_SET) != 0)
   {
      log_error(LOG_LEVEL_HEADER, "Crunching incoming cookie: %s", *header);
      freez(*header);
   }
   else if ((csp->action->flags & ACTION_NO_COOKIE_KEEP) != 0)
   {
      /* Flag whether or not to log a message */
      int changed = 0;

      /* A variable to store the tag we're working on */
      char *cur_tag;

      /* Skip "Set-Cookie:" (11 characters) in header */
      cur_tag = *header + 11;

      /* skip whitespace between "Set-Cookie:" and value */
      while (*cur_tag && ijb_isspace(*cur_tag))
      {
         cur_tag++;
      }

      /* Loop through each tag in the cookie */
      while (*cur_tag)
      {
         /* Find next tag */
         char *next_tag = strchr(cur_tag, ';');
         if (next_tag != NULL)
         {
            /* Skip the ';' character itself */
            next_tag++;

            /* skip whitespace ";" and start of tag */
            while (*next_tag && ijb_isspace(*next_tag))
            {
               next_tag++;
            }
         }
         else
         {
            /* "Next tag" is the end of the string */
            next_tag = cur_tag + strlen(cur_tag);
         }

         /*
          * Check the expiration date to see
          * if the cookie is still valid, if yes,
          * rewrite it to a session cookie.
          */
         if ((strncmpic(cur_tag, "expires=", 8) == 0) && *(cur_tag + 8))
         {
            char *expiration_date = cur_tag + 8; /* Skip "[Ee]xpires=" */

            /* Did we detect the date properly? */
            if (JB_ERR_OK != parse_header_time(expiration_date, &cookie_time))
            {
               /*
                * Nope, treat it as if it was still valid.
                *
                * XXX: Should we remove the whole cookie instead?
                */
               log_error(LOG_LEVEL_ERROR,
                  "Can't parse \'%s\', send by %s. Unsupported time format?", cur_tag, csp->http->url);
               string_move(cur_tag, next_tag);
               changed = 1;
            }
            else
            {
               /*
                * Yes. Check if the cookie is still valid.
                *
                * If the cookie is already expired it's probably
                * a delete cookie and even if it isn't, the browser
                * will discard it anyway.
                */

               /*
                * XXX: timegm() isn't available on some AmigaOS
                * versions and our replacement doesn't work.
                *
                * Our options are to either:
                *
                * - disable session-cookies-only completely if timegm
                *   is missing,
                *
                * - to simply remove all expired tags, like it has
                *   been done until Privoxy 3.0.6 and to live with
                *    the consequence that it can cause login/logout
                *   problems on servers that don't validate their
                *   input properly, or
                *
                * - to replace it with mktime in which
                *   case there is a slight chance of valid cookies
                *   passing as already expired.
                *
                *   This is the way it's currently done and it's not
                *   as bad as it sounds. If the missing GMT offset is
                *   enough to change the result of the expiration check
                *   the cookie will be only valid for a few hours
                *   anyway, which in many cases will be shorter
                *   than a browser session.
                */
               if (cookie_time - now < 0)
               {
                  log_error(LOG_LEVEL_HEADER,
                     "Cookie \'%s\' is already expired and can pass unmodified.", *header);
                  /* Just in case some clown sets more then one expiration date */
                  cur_tag = next_tag;
               }
               else
               {
                  /*
                   * Still valid, delete expiration date by copying
                   * the rest of the string over it.
                   */
                  string_move(cur_tag, next_tag);

                  /* That changed the header, need to issue a log message */
                  changed = 1;

                  /*
                   * Note that the next tag has now been moved to *cur_tag,
                   * so we do not need to update the cur_tag pointer.
                   */
               }
            }

         }
         else
         {
            /* Move on to next cookie tag */
            cur_tag = next_tag;
         }
      }

      if (changed)
      {
         assert(NULL != *header);
         log_error(LOG_LEVEL_HEADER, "Cookie rewritten to a temporary one: %s",
            *header);
      }
   }

   return JB_ERR_OK;
}


#ifdef FEATURE_FORCE_LOAD
/*********************************************************************
 *
 * Function    :  strclean
 *
 * Description :  In-Situ-Eliminate all occurances of substring in
 *                string
 *
 * Parameters  :
 *          1  :  string = string to clean
 *          2  :  substring = substring to eliminate
 *
 * Returns     :  Number of eliminations
 *
 *********************************************************************/
int strclean(char *string, const char *substring)
{
   int hits = 0;
   size_t len;
   char *pos, *p;

   len = strlen(substring);

   while((pos = strstr(string, substring)) != NULL)
   {
      p = pos + len;
      do
      {
         *(p - len) = *p;
      }
      while (*p++ != '\0');

      hits++;
   }

   return(hits);
}
#endif /* def FEATURE_FORCE_LOAD */


/*********************************************************************
 *
 * Function    :  parse_header_time
 *
 * Description :  Parses time formats used in HTTP header strings
 *                to get the numerical respresentation.
 *
 * Parameters  :
 *          1  :  header_time = HTTP header time as string. 
 *          2  :  result = storage for header_time in seconds
 *
 * Returns     :  JB_ERR_OK if the time format was recognized, or
 *                JB_ERR_PARSE otherwise.
 *
 *********************************************************************/
static jb_err parse_header_time(const char *header_time, time_t *result)
{
   struct tm gmt;

   /*
    * Zero out gmt to prevent time zone offsets.
    *
    * While this is only necessary on some platforms
    * (mingw32 for example), I don't know how to
    * detect these automatically and doing it everywhere
    * shouldn't hurt.
    */
   memset(&gmt, 0, sizeof(gmt));

                            /* Tue, 02 Jun 2037 20:00:00 */
   if ((NULL == strptime(header_time, "%a, %d %b %Y %H:%M:%S", &gmt))
                            /* Tue, 02-Jun-2037 20:00:00 */
    && (NULL == strptime(header_time, "%a, %d-%b-%Y %H:%M:%S", &gmt))
                            /* Tue, 02-Jun-37 20:00:00 */
    && (NULL == strptime(header_time, "%a, %d-%b-%y %H:%M:%S", &gmt))
                        /* Tuesday, 02-Jun-2037 20:00:00 */
    && (NULL == strptime(header_time, "%A, %d-%b-%Y %H:%M:%S", &gmt))
                        /* Tuesday Jun 02 20:00:00 2037 */
    && (NULL == strptime(header_time, "%A %b %d %H:%M:%S %Y", &gmt)))
   {
      return JB_ERR_PARSE;
   }

   *result = timegm(&gmt);

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  get_destination_from_headers
 *
 * Description :  Parse the "Host:" header to get the request's destination.
 *                Only needed if the client's request was forcefully
 *                redirected into Privoxy.
 *
 *                Code mainly copied from client_host() which is currently
 *                run too late for this purpose.
 *
 * Parameters  :
 *          1  :  headers = List of headers (one of them hopefully being
 *                the "Host:" header)
 *          2  :  http = storage for the result (host, port and hostport). 
 *
 * Returns     :  JB_ERR_MEMORY in case of memory problems,
 *                JB_ERR_PARSE if the host header couldn't be found,
 *                JB_ERR_OK otherwise.
 *
 *********************************************************************/
jb_err get_destination_from_headers(const struct list *headers, struct http_request *http)
{
   char *q;
   char *p;
   char *host;

   host = get_header_value(headers, "Host:");

   if (NULL == host)
   {
      log_error(LOG_LEVEL_ERROR, "No \"Host:\" header found.");
      return JB_ERR_PARSE;
   }

   if (NULL == (p = strdup((host))))
   {
      log_error(LOG_LEVEL_ERROR, "Out of memory while parsing \"Host:\" header");
      return JB_ERR_MEMORY;
   }
   chomp(p);
   if (NULL == (q = strdup(p)))
   {
      freez(p);
      log_error(LOG_LEVEL_ERROR, "Out of memory while parsing \"Host:\" header");
      return JB_ERR_MEMORY;
   }

   freez(http->hostport);
   http->hostport = p;
   freez(http->host);
   http->host = q;
   q = strchr(http->host, ':');
   if (q != NULL)
   {
      /* Terminate hostname and evaluate port string */
      *q++ = '\0';
      http->port = atoi(q);
   }
   else
   {
      http->port = http->ssl ? 443 : 80;
   }

   /* Rebuild request URL */
   freez(http->url);
   http->url = strdup(http->ssl ? "https://" : "http://");
   string_append(&http->url, http->hostport);
   string_append(&http->url, http->path);
   if (http->url == NULL)
   {
      return JB_ERR_MEMORY;
   }

   log_error(LOG_LEVEL_HEADER, "Destination extracted from \"Host:\" header. New request URL: %s",
      http->url);

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  create_forged_referrer
 *
 * Description :  Helper for client_referrer to forge a referer as
 *                'http://[hostname:port/' to fool stupid
 *                checks for in-site links 
 *
 * Parameters  :
 *          1  :  header   = Pointer to header pointer
 *          2  :  hostport = Host and optionally port as string
 *
 * Returns     :  JB_ERR_OK in case of success, or
 *                JB_ERR_MEMORY in case of memory problems.
 *
 *********************************************************************/
static jb_err create_forged_referrer(char **header, const char *hostport)
{
    assert(NULL == *header);

    *header = strdup("Referer: http://");
    string_append(header, hostport);
    string_append(header, "/");

    if (NULL == *header)
    {
       return JB_ERR_MEMORY;
    }

    log_error(LOG_LEVEL_HEADER, "Referer forged to: %s", *header);

    return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  create_fake_referrer
 *
 * Description :  Helper for client_referrer to create a fake referrer
 *                based on a string supplied by the user.
 *
 * Parameters  :
 *          1  :  header   = Pointer to header pointer
 *          2  :  hosthost = Referrer to fake
 *
 * Returns     :  JB_ERR_OK in case of success, or
 *                JB_ERR_MEMORY in case of memory problems.
 *
 *********************************************************************/
static jb_err create_fake_referrer(char **header, const char *fake_referrer)
{
   assert(NULL == *header);

   if ((0 != strncmpic(fake_referrer, "http://", 7)) && (0 != strncmpic(fake_referrer, "https://", 8)))
   {
      log_error(LOG_LEVEL_HEADER,
         "Parameter: +hide-referrer{%s} is a bad idea, but I don't care.", fake_referrer);
   }
   *header = strdup("Referer: ");
   string_append(header, fake_referrer);

   if (NULL == *header)
   {
      return JB_ERR_MEMORY;
   }

   log_error(LOG_LEVEL_HEADER, "Referer replaced with: %s", *header);

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  handle_conditional_hide_referrer_parameter
 *
 * Description :  Helper for client_referrer to crunch or forge
 *                the referrer header if the host has changed.
 *
 * Parameters  :
 *          1  :  header = Pointer to header pointer
 *          2  :  host   = The target host (may include the port)
 *          3  :  parameter_conditional_block = Boolean to signal
 *                if we're in conditional-block mode. If not set,
 *                we're in conditional-forge mode.
 *
 * Returns     :  JB_ERR_OK in case of success, or
 *                JB_ERR_MEMORY in case of memory problems.
 *
 *********************************************************************/
static jb_err handle_conditional_hide_referrer_parameter(char **header,
   const char *host, const int parameter_conditional_block)
{
   char *referer = strdup(*header);
   const size_t hostlenght = strlen(host);
   const char *referer_url = NULL;

   if (NULL == referer)
   {
      freez(*header);
      return JB_ERR_MEMORY;
   }

   /* referer begins with 'Referer: http[s]://' */
   if ((hostlenght+17) < strlen(referer))
   {
      /*
       * Shorten referer to make sure the referer is blocked
       * if www.example.org/www.example.com-shall-see-the-referer/
       * links to www.example.com/
       */
      referer[hostlenght+17] = '\0';
   }
   referer_url = strstr(referer, "http://");
   if ((NULL == referer_url) || (NULL == strstr(referer_url, host)))
   {
      /* Host has changed, Referer is invalid or a https URL. */
      if (parameter_conditional_block)
      {
         log_error(LOG_LEVEL_HEADER, "New host is: %s. Crunching %s!", host, *header);
         freez(*header);
      }
      else
      {
         freez(*header);
         freez(referer);
         return create_forged_referrer(header, host);
      }
   }
   freez(referer);

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  get_appropiate_connection_header
 *
 * Description :  Returns an appropiate Connection header
 *                depending on whether or not we try to keep
 *                the connection to the server alive.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  Pointer to statically allocated header buffer.
 *
 *********************************************************************/
static const char *get_appropiate_connection_header(const struct client_state *csp)
{
   static const char connection_keep_alive[] = "Connection: keep-alive";
   static const char connection_close[] = "Connection: close";

   if ((csp->config->feature_flags & RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE)
    && (csp->http->ssl == 0))
   {
      return connection_keep_alive;
   }
   return connection_close;
}
/*
  Local Variables:
  tab-width: 3
  end:
*/
