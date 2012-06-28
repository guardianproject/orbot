const char jcc_rcs[] = "$Id: jcc.c,v 1.235 2009/03/18 21:01:20 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/jcc.c,v $
 *
 * Purpose     :  Main file.  Contains main() method, main loop, and
 *                the main connection-handling function.
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
 *    $Log: jcc.c,v $
 *    Revision 1.235  2009/03/18 21:01:20  fabiankeil
 *    Comment fix. Spotted by Roland.
 *
 *    Revision 1.234  2009/03/18 20:48:42  fabiankeil
 *    If the --no-daemon option is used, enable LOG_LEVEL_INFO
 *    before the config file has been parsed (as we always did).
 *
 *    Revision 1.233  2009/03/13 14:10:07  fabiankeil
 *    Fix some more harmless warnings on amd64.
 *
 *    Revision 1.232  2009/03/08 19:29:16  fabiankeil
 *    Reinitialize the timeout structure every time before passing
 *    it to select(). Apparently some implementations mess with it.
 *    Probably fixes #2669131 reported by cyberpatrol.
 *
 *    Revision 1.231  2009/03/08 14:19:23  fabiankeil
 *    Fix justified (but harmless) compiler warnings
 *    on platforms where sizeof(int) < sizeof(long).
 *
 *    Revision 1.230  2009/03/07 13:09:17  fabiankeil
 *    Change csp->expected_content and_csp->expected_content_length from
 *    size_t to unsigned long long to reduce the likelihood of integer
 *    overflows that would let us close the connection prematurely.
 *    Bug found while investigating #2669131, reported by cyberpatrol.
 *
 *    Revision 1.229  2009/03/07 11:17:01  fabiankeil
 *    Fix compiler warning.
 *
 *    Revision 1.228  2009/03/06 20:30:13  fabiankeil
 *    Log unsigned values as such.
 *
 *    Revision 1.227  2009/03/02 19:18:11  fabiankeil
 *    Streamline parse_http_request()'s prototype. As
 *    cparser pointed out it doesn't actually use csp.
 *
 *    Revision 1.226  2009/03/01 18:28:24  fabiankeil
 *    Help clang understand that we aren't dereferencing
 *    NULL pointers here.
 *
 *    Revision 1.225  2009/02/19 18:09:32  fabiankeil
 *    Unbreak build without FEATURE_CONNECTION_KEEP_ALIVE.
 *    Noticed by David.
 *
 *    Revision 1.224  2009/02/14 15:32:04  fabiankeil
 *    Add the request URL to the timeout message in chat().
 *    Suggested by Lee.
 *
 *    Revision 1.223  2009/02/09 21:21:16  fabiankeil
 *    Now that init_log_module() is called earlier, call show_version()
 *    later on from main() directly so it doesn't get called for --help
 *    or --version.
 *
 *    Revision 1.222  2009/02/08 12:56:51  fabiankeil
 *    Call initialize_mutexes() before init_log_module() again.
 *    Broken since r220, might be the cause of Lee's #2579448.
 *
 *    Revision 1.221  2009/02/06 18:02:58  fabiankeil
 *    When dropping privileges, also give up membership in supplementary
 *    groups. Thanks to Matthias Drochner for reporting the problem,
 *    providing the initial patch and testing the final version.
 *
 *    Revision 1.220  2009/02/04 18:29:07  fabiankeil
 *    Initialize the log module before parsing arguments.
 *    Thanks to Matthias Drochner for the report.
 *
 *    Revision 1.219  2009/01/31 16:08:21  fabiankeil
 *    Remove redundant error check in receive_client_request().
 *
 *    Revision 1.218  2009/01/31 12:25:54  fabiankeil
 *    Flatten indentation in receive_client_request().
 *
 *    Revision 1.217  2009/01/07 19:50:09  fabiankeil
 *    - If the socket-timeout has been reached and the client
 *      hasn't received any data yet, send an explanation before
 *      closing the connection.
 *    - In get_request_line(), signal timeouts the right way.
 *
 *    Revision 1.216  2008/12/24 22:13:11  ler762
 *    fix GCC 3.4.4 warning
 *
 *    Revision 1.215  2008/12/24 17:06:19  fabiankeil
 *    Keep a thread around to timeout alive connections
 *    even if no new requests are coming in.
 *
 *    Revision 1.214  2008/12/20 14:53:55  fabiankeil
 *    Add config option socket-timeout to control the time
 *    Privoxy waits for data to arrive on a socket. Useful
 *    in case of stale ssh tunnels or when fuzz-testing.
 *
 *    Revision 1.213  2008/12/15 18:45:51  fabiankeil
 *    When logging crunches, log the whole URL, so one can easily
 *    differentiate between vanilla HTTP and CONNECT requests.
 *
 *    Revision 1.212  2008/12/14 15:46:22  fabiankeil
 *    Give crunched requests their own log level.
 *
 *    Revision 1.211  2008/12/06 10:05:03  fabiankeil
 *    Downgrade "Received x bytes while expecting y." message to
 *    LOG_LEVEL_CONNECT as it doesn't necessarily indicate an error.
 *
 *    Revision 1.210  2008/12/02 22:03:18  fabiankeil
 *    Don't miscalculate byte_count if we don't get all the
 *    server headers with one read_socket() call. With keep-alive
 *    support enabled, this caused delays until the server closed
 *    the connection.
 *
 *    Revision 1.209  2008/11/27 09:44:04  fabiankeil
 *    Cosmetics for the last commit: Don't watch out for
 *    the last chunk if the content isn't chunk-encoded or
 *    if we already determined the content length previously.
 *
 *    Revision 1.208  2008/11/26 18:24:17  fabiankeil
 *    Recognize that the server response is complete if the
 *    last chunk is read together with the server headers.
 *    Reported by Lee.
 *
 *    Revision 1.207  2008/11/25 17:25:16  fabiankeil
 *    Don't convert the client-header list to text until we need to.
 *
 *    Revision 1.206  2008/11/23 17:00:11  fabiankeil
 *    Some more chat() cosmetics.
 *
 *    Revision 1.205  2008/11/16 12:43:49  fabiankeil
 *    Turn keep-alive support into a runtime feature
 *    that is disabled by setting keep-alive-timeout
 *    to a negative value.
 *
 *    Revision 1.204  2008/11/06 19:42:17  fabiankeil
 *    Fix last-chunk detection hack to also apply
 *    if buf[] contains nothing but the last-chunk.
 *
 *    Revision 1.203  2008/11/06 18:34:35  fabiankeil
 *    Factor receive_client_request() and
 *    parse_client_request() out of chat().
 *
 *    Revision 1.202  2008/11/02 18:40:34  fabiankeil
 *    If we received a different amount of data than we expected,
 *    log a warning and make sure the server socket isn't reused.
 *
 *    Revision 1.201  2008/11/02 16:48:20  fabiankeil
 *    Revert revision 1.195 and try again.
 *
 *    Revision 1.200  2008/10/26 16:53:18  fabiankeil
 *    Fix gcc44 warning.
 *
 *    Revision 1.199  2008/10/26 15:36:10  fabiankeil
 *    Remove two debug messages with LOG_LEVEL_INFO.
 *
 *    Revision 1.198  2008/10/22 15:19:55  fabiankeil
 *    Once More, With Feeling: if there is no logfile
 *    because the user didn't specify one, we shouldn't
 *    call init_error_log() after receiving SIGHUP either.
 *
 *    Revision 1.197  2008/10/20 17:02:40  fabiankeil
 *    If SIGHUP is received while we aren't running in daemon
 *    mode, calling init_error_log() would be a mistake.
 *
 *    Revision 1.196  2008/10/16 09:16:41  fabiankeil
 *    - Fix two gcc44 conversion warnings.
 *    - Don't bother logging the last five bytes
 *      of the 0-chunk.
 *
 *    Revision 1.195  2008/10/13 16:04:37  fabiankeil
 *    Make sure we don't try to reuse tainted server sockets.
 *
 *    Revision 1.194  2008/10/12 18:35:18  fabiankeil
 *    The last commit was a bit too ambitious, apparently the content
 *    length adjustment is only necessary if we aren't buffering.
 *
 *    Revision 1.193  2008/10/12 15:57:35  fabiankeil
 *    Fix content length calculation if we read headers
 *    and the start of the body at once. Now that we have
 *    FEATURE_CONNECTION_KEEP_ALIVE, it actually matters.
 *
 *    Revision 1.192  2008/10/11 18:19:14  fabiankeil
 *    Even more chat() cosmetics.
 *
 *    Revision 1.191  2008/10/11 18:00:14  fabiankeil
 *    Reformat some comments in chat().
 *
 *    Revision 1.190  2008/10/11 14:58:00  fabiankeil
 *    In case of chunk-encoded content, stop reading if
 *    the buffer looks like it ends with the last chunk.
 *
 *    Revision 1.189  2008/10/11 09:53:00  fabiankeil
 *    Let server_response_is_complete() deal properly with
 *    content that is neither buffered nor read all at once.
 *
 *    Revision 1.188  2008/10/09 18:21:41  fabiankeil
 *    Flush work-in-progress changes to keep outgoing connections
 *    alive where possible. Incomplete and mostly #ifdef'd out.
 *
 *    Revision 1.187  2008/09/07 12:35:05  fabiankeil
 *    Add mutex lock support for _WIN32.
 *
 *    Revision 1.186  2008/09/04 08:13:58  fabiankeil
 *    Prepare for critical sections on Windows by adding a
 *    layer of indirection before the pthread mutex functions.
 *
 *    Revision 1.185  2008/08/30 12:03:07  fabiankeil
 *    Remove FEATURE_COOKIE_JAR.
 *
 *    Revision 1.184  2008/08/22 15:34:45  fabiankeil
 *    - Silence LLVM/Clang complaint.
 *    - Make received_hup_signal static.
 *    - Hide definitions for basedir, pidfile and received_hup_signal
 *      from __EMX__ as they only seem to be used in case of #ifdef unix.
 *
 *    Revision 1.183  2008/08/21 07:09:35  fabiankeil
 *    Accept Shoutcast responses again. Problem reported
 *    and fix suggested by Stefan in #2062860.
 *
 *    Revision 1.182  2008/06/27 11:13:56  fabiankeil
 *    Fix possible NULL-pointer dereference reported
 *    by din_a4 in #2003937. Pointy hat to me.
 *
 *    Revision 1.181  2008/05/21 15:47:15  fabiankeil
 *    Streamline sed()'s prototype and declare
 *    the header parse and add structures static.
 *
 *    Revision 1.180  2008/05/21 15:26:32  fabiankeil
 *    - Mark csp as immutable for send_crunch_response().
 *    - Fix comment spelling.
 *
 *    Revision 1.179  2008/05/20 20:13:32  fabiankeil
 *    Factor update_server_headers() out of sed(), ditch the
 *    first_run hack and make server_patterns_light static.
 *
 *    Revision 1.178  2008/05/10 13:23:38  fabiankeil
 *    Don't provide get_header() with the whole client state
 *    structure when it only needs access to csp->iob.
 *
 *    Revision 1.177  2008/05/10 11:51:12  fabiankeil
 *    Make the "read the rest of the headers" loop a bit more readable.
 *
 *    Revision 1.176  2008/05/10 11:37:57  fabiankeil
 *    - Instead of logging when the IIS5 hack is enabled, log when it fails.
 *    - Remove useless comment.
 *
 *    Revision 1.175  2008/05/09 18:53:59  fabiankeil
 *    Fix comment grammar.
 *
 *    Revision 1.174  2008/05/07 18:05:53  fabiankeil
 *    Remove the pointless buffer in client_protocol_is_unsupported().
 *
 *    Revision 1.173  2008/05/06 15:09:00  fabiankeil
 *    Least-effort fix for bug #1821930 (reported by Lee):
 *    If the response doesn't look like HTTP,
 *    tell the client and log the problem.
 *
 *    Revision 1.172  2008/04/16 16:38:21  fabiankeil
 *    Don't pass the whole csp structure to flush_socket()
 *    when it only needs a file descriptor and a buffer.
 *
 *    Revision 1.171  2008/03/27 18:27:25  fabiankeil
 *    Remove kill-popups action.
 *
 *    Revision 1.170  2008/03/06 16:33:46  fabiankeil
 *    If limit-connect isn't used, don't limit CONNECT requests to port 443.
 *
 *    Revision 1.169  2008/03/04 18:30:39  fabiankeil
 *    Remove the treat-forbidden-connects-like-blocks action. We now
 *    use the "blocked" page for forbidden CONNECT requests by default.
 *
 *    Revision 1.168  2008/03/02 12:25:25  fabiankeil
 *    Also use shiny new connect_port_is_forbidden() in jcc.c.
 *
 *    Revision 1.167  2008/02/23 16:57:12  fabiankeil
 *    Rename url_actions() to get_url_actions() and let it
 *    use the standard parameter ordering.
 *
 *    Revision 1.166  2008/02/23 16:33:43  fabiankeil
 *    Let forward_url() use the standard parameter ordering
 *    and mark its second parameter immutable.
 *
 *    Revision 1.165  2008/02/02 19:36:56  fabiankeil
 *    Remove the "Listening ... for local connections only" log message.
 *    Whether or not remote connections are able to reach Privoxy is up
 *    to the operating system.
 *
 *    Revision 1.164  2007/12/16 18:32:46  fabiankeil
 *    Prevent the log messages for CONNECT requests to unacceptable
 *    ports from printing the limit-connect argument as [null] if
 *    limit-connect hasn't been explicitly enabled.
 *
 *    Revision 1.163  2007/12/13 01:47:11  david__schmidt
 *    Make sure all console-mode apps get a usage() instance
 *
 *    Revision 1.162  2007/12/06 17:54:57  fabiankeil
 *    Reword NO_SERVER_DATA_RESPONSE to make it harder
 *    to misunderstand what the message is all about.
 *
 *    Revision 1.161  2007/12/04 19:44:22  fabiankeil
 *    Unbreak trustfile which previously didn't work without
 *    FEATURE_TOGGLE. Fixes BR#1843585, reported by Lee.
 *
 *    Revision 1.160  2007/11/29 18:00:29  fabiankeil
 *    Plug memory leak. Spotted by Valgrind, triggered by
 *    Privoxy-Regression-Test feeding proxyfuzz.py.
 *
 *    Revision 1.159  2007/11/24 14:34:09  fabiankeil
 *    In the HTTP snipplets, refer to the client as client.
 *
 *    Revision 1.158  2007/11/11 16:44:17  fabiankeil
 *    Emit a log message when activating the MS IIS5 hack.
 *
 *    Revision 1.157  2007/11/03 17:34:49  fabiankeil
 *    Log the "weak randomization factor" warning only
 *    once for mingw32 and provide some more details.
 *
 *    Revision 1.156  2007/11/01 18:20:58  fabiankeil
 *    Initialize log module after initializing mutexes, future
 *    deadlocks in that code should now work cross-platform.
 *
 *    Revision 1.155  2007/10/23 20:12:45  fabiankeil
 *    Fix first CSUCCEED line to end in \r\n as required by RFC1945.
 *    Reported by Bert van Leeuwen in BR#1818808.
 *
 *    Revision 1.154  2007/10/19 17:00:08  fabiankeil
 *    Downgrade "Flushing header and buffers" message to LOG_LEVEL_INFO.
 *
 *    Revision 1.153  2007/10/14 14:12:41  fabiankeil
 *    When in daemon mode, close stderr after the configuration file has been
 *    parsed the first time. If logfile isn't set, stop logging. Fixes BR#897436.
 *
 *    Revision 1.152  2007/10/04 18:03:34  fabiankeil
 *    - Fix a crash when parsing invalid requests whose first header
 *      is rejected by get_header(). Regression (re?)introduced
 *      in r1.143 by yours truly.
 *    - Move ACTION_VANILLA_WAFER handling into parsers.c's
 *      client_cookie_adder() to make sure send-vanilla-wafer can be
 *      controlled through tags (and thus regression-tested).
 *
 *    Revision 1.151  2007/09/29 10:21:16  fabiankeil
 *    - Move get_filter_function() from jcc.c to filters.c
 *      so the filter functions can be static.
 *    - Don't bother filtering body-less responses.
 *
 *    Revision 1.150  2007/09/28 16:39:29  fabiankeil
 *    Execute content filters through execute_content_filter().
 *
 *    Revision 1.149  2007/09/04 15:08:48  fabiankeil
 *    Initialize req to NULL to make sure it's defined if the
 *    first read_socket() call fails. Reported by icmp30.
 *
 *    Revision 1.148  2007/08/26 16:47:13  fabiankeil
 *    Add Stephen Gildea's --pre-chroot-nslookup patch [#1276666],
 *    extensive comments moved to user manual.
 *
 *    Revision 1.147  2007/08/25 14:42:40  fabiankeil
 *    Don't crash if a broken header filter wiped out the request line.
 *
 *    Revision 1.146  2007/08/20 17:09:32  fabiankeil
 *    Fix byte_count calculation in case of flushes
 *    and don't parse the server headers a second time.
 *
 *    Revision 1.145  2007/08/19 13:13:31  fabiankeil
 *    - If there's a connection problem after we already forwarded
 *      parts of the original content, just hang up. Fixes BR#1776724.
 *    - Fix warnings about unused code on mingw32.
 *    - In case of flushes, calculate the byte count
 *      less incorrectly (I think).
 *
 *    Revision 1.144  2007/08/11 14:43:22  fabiankeil
 *    Add some more prototypes for static functions.
 *
 *    Revision 1.143  2007/08/05 13:58:19  fabiankeil
 *    Comment out request_contains_null_bytes() until it's used again.
 *
 *    Revision 1.142  2007/08/05 13:50:26  fabiankeil
 *    #1763173 from Stefan Huehner: s@const static@static const@
 *    and declare some more functions static.
 *
 *    Revision 1.141  2007/08/04 09:56:23  fabiankeil
 *    - Log rejected CONNECT requests with LOG_LEVEL_INFO
 *      and explain why they were rejected in the first place.
 *    - Fix the LOG_LEVEL_CLF message for crunches of unallowed
 *      CONNECT requests. The request line was missing.
 *    - Add two more XXX reminders as we don't have enough already.
 *
 *    Revision 1.140  2007/07/21 11:51:36  fabiankeil
 *    As Hal noticed, checking dispatch_cgi() as the last cruncher
 *    looks like a bug if CGI requests are blocked unintentionally,
 *    so don't do it unless the user enabled the new config option
 *    "allow-cgi-request-crunching".
 *
 *    Revision 1.139  2007/07/14 07:46:41  fabiankeil
 *    - Allow to rewrite the request destination behind the client's back.
 *    - Turn the weird-looking unconditional for loop that
 *      reads the client request into a conditional while loop.
 *      Move the stuff that only runs once out of the loop.
 *    - Move parts of chat(), server_content_type() and the
 *      necessary stuff to fix BR#1750917 into get_filter_function().
 *
 *    Revision 1.138  2007/06/03 18:45:18  fabiankeil
 *    Temporary workaround for BR#1730105.
 *
 *    Revision 1.137  2007/06/01 18:16:36  fabiankeil
 *    Use the same mutex for gethostbyname() and gethostbyaddr() to prevent
 *    deadlocks and crashes on OpenBSD and possibly other OS with neither
 *    gethostbyname_r() nor gethostaddr_r(). Closes BR#1729174.
 *    Thanks to Ralf Horstmann for report and solution.
 *
 *    Revision 1.136  2007/06/01 16:41:11  fabiankeil
 *    Add forward-override{} to change the forwarding settings through
 *    action sections. This is mainly interesting to forward different
 *    clients differently (for example based on User-Agent or request
 *    origin).
 *
 *    Revision 1.135  2007/05/24 17:03:50  fabiankeil
 *    - Let usage() mention the --chroot parameter.
 *    - Use read_socket() consistently and always leave
 *      the last buffer byte alone, even in cases where
 *      null termination (currently) doesn't matter.
 *
 *    Revision 1.134  2007/05/16 14:59:46  fabiankeil
 *    - Fix config file loading on Unix if no config file is specified.
 *      Since r1.97 Privoxy would always interpret the last argument as
 *      config file, even if it's a valid command line option.
 *    - Abort in case of unrecognized command line options. Closes #1719696.
 *    - Remove a bunch of unnecessary strcpy() calls (yay for c&p without thinking).
 *    - Replace the remaining strcpy() and strcat() calls with strlcpy() and strcat().
 *
 *    Revision 1.133  2007/05/04 11:23:19  fabiankeil
 *    - Don't rerun crunchers that only depend on the request URL.
 *    - Don't count redirects and CGI requests as "blocked requests".
 *
 *    Revision 1.132  2007/04/25 15:15:17  fabiankeil
 *    Support crunching based on tags created by server-header taggers.
 *
 *    Revision 1.131  2007/04/22 13:24:50  fabiankeil
 *    Make HTTP snippets static (again). Add a Content-Type for those
 *    with content so the browser doesn't guess it based on the URL.
 *
 *    Revision 1.130  2007/04/19 13:47:34  fabiankeil
 *    Move crunching and request line rebuilding out of chat().
 *
 *    Revision 1.129  2007/04/15 16:39:20  fabiankeil
 *    Introduce tags as alternative way to specify which
 *    actions apply to a request. At the moment tags can be
 *    created based on client and server headers.
 *
 *    Revision 1.128  2007/03/25 16:55:54  fabiankeil
 *    Don't CLF-log CONNECT requests twice.
 *
 *    Revision 1.127  2007/03/20 13:53:17  fabiankeil
 *    Log the source address for ACL-related connection drops.
 *
 *    Revision 1.126  2007/03/17 15:20:05  fabiankeil
 *    New config option: enforce-blocks.
 *
 *    Revision 1.125  2007/03/09 14:12:00  fabiankeil
 *    - Move null byte check into separate function.
 *    - Don't confuse the client with error pages
 *      if a CONNECT request was already confirmed.
 *
 *    Revision 1.124  2007/02/23 14:59:54  fabiankeil
 *    Speed up NULL byte escaping and only log the complete
 *    NULL byte requests with header debugging enabled.
 *
 *    Revision 1.123  2007/02/21 18:42:10  fabiankeil
 *    Answer requests that contain NULL bytes with
 *    a custom response instead of waiting for more
 *    data until the client eventually hangs up.
 *
 *    Revision 1.122  2007/02/07 11:12:02  fabiankeil
 *    - Move delivery and logging of crunched responses
 *      from chat() into send_crunch_response().
 *    - Display the reason for generating http_responses.
 *    - Log the content length for LOG_LEVEL_CLF correctly
 *      (still incorrect for some fixed responses).
 *    - Reword an incorrect comment about
 *      treat-forbidden-connects-like-blocks violating
 *      the specs.
 *    - Add some log messages.
 *
 *    Revision 1.121  2007/01/27 10:52:56  fabiankeil
 *    Move mutex initialization into separate
 *    function and exit in case of errors.
 *
 *    Revision 1.120  2007/01/26 14:18:42  fabiankeil
 *    - Start to reduce chat()'s line count and move
 *      parts of it into separate functions.
 *    - Add "HTTP/1.1 100 Continue" hack for BR 756734.
 *
 *    Revision 1.119  2007/01/25 14:02:30  fabiankeil
 *    - Add Proxy-Agent header to HTTP snippets that are
 *      supposed to reach HTTP clients only.
 *    - Made a few CONNECT log messages more descriptive.
 *    - Catch completely empty server responses (as seen
 *      with Tor's fake ".noconnect" top level domain).
 *    - Use shiny new "forwarding-failed" template for socks errors.
 *
 *    Revision 1.118  2007/01/07 07:43:43  joergs
 *    AmigaOS4 support added.
 *
 *    Revision 1.117  2006/12/31 17:56:37  fabiankeil
 *    Added config option accept-intercepted-requests
 *    and disabled it by default.
 *
 *    Revision 1.116  2006/12/29 19:08:22  fabiankeil
 *    Reverted parts of my last commit
 *    to keep error handling working.
 *
 *    Revision 1.115  2006/12/29 17:38:57  fabiankeil
 *    Fixed gcc43 conversion warnings.
 *
 *    Revision 1.114  2006/12/27 18:52:02  fabiankeil
 *    Fix -pedantic ISO C warning about converting
 *    from function pointer to object pointer.
 *
 *    Revision 1.113  2006/12/26 17:38:50  fabiankeil
 *    Silence compiler warning I introduced with my last commit.
 *
 *    Revision 1.112  2006/12/26 17:31:41  fabiankeil
 *    Mutex protect rand() if POSIX threading
 *    is used, warn the user if that's not possible
 *    and stop using it on _WIN32 where it could
 *    cause crashes.
 *
 *    Revision 1.111  2006/12/23 16:15:06  fabiankeil
 *    Don't prevent core dumps by catching SIGABRT.
 *    It's rude and makes debugging unreasonable painful.
 *
 *    Revision 1.110  2006/12/13 14:52:53  etresoft
 *    Fix build failure on MacOS X. Global symbols can be either static or extern, but not both.
 *
 *    Revision 1.109  2006/12/06 19:41:40  fabiankeil
 *    Privoxy is now able to run as intercepting
 *    proxy in combination with any packet filter
 *    that does the port redirection. The destination
 *    is extracted from the "Host:" header which
 *    should be available for nearly all requests.
 *
 *    Moved HTTP snipplets into jcc.c.
 *    Added error message for gopher proxy requests.
 *
 *    Revision 1.108  2006/11/28 15:38:51  fabiankeil
 *    Only unlink the pidfile if it's actually used.
 *
 *    Change order of interception checks to make
 *    it possible to block or redirect requests for
 *    the cgi pages.
 *
 *    Revision 1.107  2006/11/13 19:05:51  fabiankeil
 *    Make pthread mutex locking more generic. Instead of
 *    checking for OSX and OpenBSD, check for FEATURE_PTHREAD
 *    and use mutex locking unless there is an _r function
 *    available. Better safe than sorry.
 *
 *    Fixes "./configure --disable-pthread" and should result
 *    in less threading-related problems on pthread-using platforms,
 *    but it still doesn't fix BR#1122404.
 *
 *    Revision 1.106  2006/11/06 19:58:23  fabiankeil
 *    Move pthread.h inclusion from jcc.c to jcc.h.
 *    Fixes build on x86-freebsd1 (FreeBSD 5.4-RELEASE).
 *
 *    Revision 1.105  2006/11/06 14:26:02  fabiankeil
 *    Don't exit after receiving the second SIGHUP on Solaris.
 *
 *    Fixes BR 1052235, but the same problem may exist on other
 *    systems. Once 3.0.6 is out we should use sigset()
 *    where available and see if it breaks anything.
 *
 *    Revision 1.104  2006/09/23 13:26:38  roro
 *    Replace TABs by spaces in source code.
 *
 *    Revision 1.103  2006/09/21 12:54:43  fabiankeil
 *    Fix +redirect{}. Didn't work with -fast-redirects.
 *
 *    Revision 1.102  2006/09/06 13:03:04  fabiankeil
 *    Respond with 400 and a short text message
 *    if the client tries to use Privoxy as FTP proxy.
 *
 *    Revision 1.101  2006/09/06 09:23:37  fabiankeil
 *    Make number of retries in case of forwarded-connect problems
 *    a config file option (forwarded-connect-retries) and use 0 as
 *    default.
 *
 *    Revision 1.100  2006/09/03 19:42:59  fabiankeil
 *    Set random(3) seed.
 *
 *    Revision 1.99  2006/09/02 15:36:42  fabiankeil
 *    Follow the OpenBSD port's lead and protect the resolve
 *    functions on OpenBSD as well.
 *
 *    Revision 1.98  2006/08/24 11:01:34  fabiankeil
 *    --user fix. Only use the user as group if no group is specified.
 *    Solves BR 1492612. Thanks to Spinor S. and David Laight.
 *
 *    Revision 1.97  2006/08/18 15:23:17  david__schmidt
 *    Windows service (re-)integration
 *
 *    The new args are:
 *
 *    --install[:service_name]
 *    --uninstall[:service_name]
 *    --service
 *
 *    They work as follows:
 *    --install will create a service for you and then terminate.
 *    By default the service name will be "privoxy" (without the quotes).
 *    However you can run multiple services if you wish, just by adding
 *    a colon and then a name (no spaces).
 *
 *    --uninstall follows the exact same rules a --install.
 *
 *    --service is used when the program is executed by the service
 *    control manager, and in normal circumstances would never be
 *    used as a command line argument.
 *
 *    Revision 1.96  2006/08/15 20:12:36  david__schmidt
 *    Windows service integration
 *
 *    Revision 1.95  2006/08/03 02:46:41  david__schmidt
 *    Incorporate Fabian Keil's patch work:
http://www.fabiankeil.de/sourcecode/privoxy/
 *
 *    Revision 1.94  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.92.2.16  2005/04/03 20:10:50  david__schmidt
 *    Thanks to Jindrich Makovicka for a race condition fix for the log
 *    file.  The race condition remains for non-pthread implementations.
 *    Reference patch #1175720.
 *
 *    Revision 1.92.2.15  2004/10/03 12:53:32  david__schmidt
 *    Add the ability to check jpeg images for invalid
 *    lengths of comment blocks.  Defensive strategy
 *    against the exploit:
 *       Microsoft Security Bulletin MS04-028
 *       Buffer Overrun in JPEG Processing (GDI+) Could
 *       Allow Code Execution (833987)
 *    Enabled with +inspect-jpegs in actions files.
 *
 *    Revision 1.92.2.14  2003/12/12 12:52:53  oes
 *    - Fixed usage info for non-unix platforms
 *    - Fixed small cmdline parsing bug
 *
 *    Revision 1.92.2.13  2003/11/27 19:20:27  oes
 *    Diagnostics: Now preserve the returncode of pthread_create
 *    in errno. Closes BR #775721. Thanks to Geoffrey Hausheer.
 *
 *    Revision 1.92.2.12  2003/07/11 11:34:19  oes
 *    No longer ignore SIGCHLD. Fixes bug #769381
 *
 *    Revision 1.92.2.11  2003/05/14 12:32:02  oes
 *    Close jarfile on graceful exit, remove stray line
 *
 *    Revision 1.92.2.10  2003/05/08 15:13:46  oes
 *    Cosmetics: Killed a warning, a typo and an allocation left at exit
 *
 *    Revision 1.92.2.9  2003/04/03 15:08:42  oes
 *    No longer rely on non-POSIX.1 extensions of getcwd().
 *    Fixes bug #711001
 *
 *    Revision 1.92.2.8  2003/03/31 13:12:32  oes
 *    Replaced setenv() by posix-compliant putenv()
 *    Thanks to Neil McCalden (nmcc AT users.sf.net).
 *
 *    Revision 1.92.2.7  2003/03/17 16:48:59  oes
 *    Added chroot ability, thanks to patch by Sviatoslav Sviridov
 *
 *    Revision 1.92.2.6  2003/03/11 11:55:00  oes
 *    Clean-up and extension of improvements for forked mode:
 *     - Child's return code now consists of flags RC_FLAG_*
 *     - Reporting toggle to parent now properly #ifdef'ed
 *     - Children now report blocking to parent. This enables
 *       statistics in forked mode
 *
 *    Revision 1.92.2.5  2003/03/10 23:45:32  oes
 *    Fixed bug #700381: Non-Threaded version now capable of being toggled.
 *    Children now report having been toggled through _exit(17), parents
 *    watch for that code and toggle themselves if found.
 *
 *    Revision 1.92.2.4  2003/03/07 03:41:04  david__schmidt
 *    Wrapping all *_r functions (the non-_r versions of them) with 
 *    mutex semaphores for OSX.  Hopefully this will take care of all 
 *    of those pesky crash reports.
 *
 *    Revision 1.92.2.3  2003/02/28 12:53:06  oes
 *    Fixed two mostly harmless mem leaks
 *
 *    Revision 1.92.2.2  2002/11/20 14:37:47  oes
 *    Fix: Head of global clients list now initialized to NULL
 *
 *    Revision 1.92.2.1  2002/09/25 14:52:24  oes
 *    Added basic support for OPTIONS and TRACE HTTP methods:
 *     - New interceptor direct_response() added in chat().
 *     - sed() moved to earlier in the process, so that the
 *       Host: header is evaluated before actions and forwarding
 *       are decided on.
 *
 *    Revision 1.92  2002/05/08 16:00:46  oes
 *    Chat's buffer handling:
 *     - Fixed bug with unchecked out-of-mem conditions
 *       while reading client request & server headers
 *     - No longer predict if the buffer limit will be exceeded
 *       in the next read -- check add_to_iob's new
 *       return code. If buffer couldn't be extended
 *       (policy or out-of-mem) while
 *       - reading from client: abort
 *       - reading server headers: send error page
 *       - buffering server body for filter: flush,
 *         and if that fails: send error page
 *
 *    Revision 1.91  2002/04/08 20:35:58  swa
 *    fixed JB spelling
 *
 *    Revision 1.90  2002/04/02 14:57:28  oes
 *    Made sending wafers independent of FEATURE_COOKIE_JAR
 *
 *    Revision 1.89  2002/03/31 17:18:59  jongfoster
 *    Win32 only: Enabling STRICT to fix a VC++ compile warning.
 *
 *    Revision 1.88  2002/03/27 14:32:43  david__schmidt
 *    More compiler warning message maintenance
 *
 *    Revision 1.87  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.86  2002/03/25 17:04:55  david__schmidt
 *    Workaround for closing the jarfile before load_config() comes around again
 *
 *    Revision 1.85  2002/03/24 15:23:33  jongfoster
 *    Name changes
 *
 *    Revision 1.84  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.83  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.82  2002/03/13 00:27:05  jongfoster
 *    Killing warnings
 *
 *    Revision 1.81  2002/03/12 01:42:50  oes
 *    Introduced modular filters
 *
 *    Revision 1.80  2002/03/11 22:07:05  david__schmidt
 *    OS/2 port maintenance:
 *    - Fixed EMX build - it had decayed a little
 *    - Fixed inexplicable crash during FD_ZERO - must be due to a bad macro.
 *      substituted a memset for now.
 *
 *    Revision 1.79  2002/03/09 20:03:52  jongfoster
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
 *    Revision 1.78  2002/03/08 21:35:04  oes
 *    Added optional group supplement to --user option. Will now use default group of user if no group given
 *
 *    Revision 1.77  2002/03/07 03:52:06  oes
 *     - Fixed compiler warnings etc
 *     - Improved handling of failed DNS lookups
 *
 *    Revision 1.76  2002/03/06 22:54:35  jongfoster
 *    Automated function-comment nitpicking.
 *
 *    Revision 1.75  2002/03/06 10:02:19  oes
 *    Fixed stupid bug when --user was not given
 *
 *    Revision 1.74  2002/03/06 00:49:31  jongfoster
 *    Fixing warning on Windows
 *    Making #ifdefs that refer to the same variable consistently
 *    use #ifdef unix rather than mixing #ifdef unix & #ifndef OS2
 *
 *    Revision 1.73  2002/03/05 23:57:30  hal9
 *    Stray character 's' on line 1618 was breaking build.
 *
 *    Revision 1.72  2002/03/05 21:33:45  david__schmidt
 *    - Re-enable OS/2 building after new parms were added
 *    - Fix false out of memory report when resolving CGI templates when no IP
 *      address is available of failed attempt (a la no such domain)
 *
 *    Revision 1.71  2002/03/05 18:13:56  oes
 *    Added --user option
 *
 *    Revision 1.70  2002/03/05 04:52:42  oes
 *    Deleted non-errlog debugging code
 *
 *    Revision 1.69  2002/03/04 23:50:00  jongfoster
 *    Splitting off bind_port() call into bind_port_helper(), with
 *    improved logging.
 *
 *    Revision 1.68  2002/03/04 20:17:32  oes
 *    Fixed usage info
 *
 *    Revision 1.67  2002/03/04 18:18:57  oes
 *    - Removed _DEBUG mode
 *    - Cleand up cmdline parsing
 *    - Introduced --no-daemon, --pidfile options
 *    - Cleaned up signal handling:
 *      - Terminate cleanly on INT, TERM and ABRT
 *      - Schedule logfile for re-opening on HUP
 *      - Ignore CHLD and PIPE
 *      - Leave the rest with their default handlers
 *      - Uniform handler registration
 *    - Added usage() function
 *    - Played styleguide police
 *
 *    Revision 1.66  2002/03/03 15:06:55  oes
 *    Re-enabled automatic config reloading
 *
 *    Revision 1.65  2002/03/03 14:49:11  oes
 *    Fixed CLF logging: Now uses client's original HTTP request
 *
 *    Revision 1.64  2002/03/03 09:18:03  joergs
 *    Made jumbjuster work on AmigaOS again.
 *
 *    Revision 1.63  2002/03/02 04:14:50  david__schmidt
 *    Clean up a little CRLF unpleasantness that suddenly appeared
 *
 *    Revision 1.62  2002/02/20 23:17:23  jongfoster
 *    Detecting some out-of memory conditions and exiting with a log message.
 *
 *    Revision 1.61  2002/01/17 21:01:52  jongfoster
 *    Moving all our URL and URL pattern parsing code to urlmatch.c.
 *
 *    Revision 1.60  2001/12/30 14:07:32  steudten
 *    - Add signal handling (unix)
 *    - Add SIGHUP handler (unix)
 *    - Add creation of pidfile (unix)
 *    - Add action 'top' in rc file (RH)
 *    - Add entry 'SIGNALS' to manpage
 *    - Add exit message to logfile (unix)
 *
 *    Revision 1.59  2001/12/13 14:07:18  oes
 *    Fixed Bug: 503 error page now sent OK
 *
 *    Revision 1.58  2001/11/30 23:37:24  jongfoster
 *    Renaming the Win32 config file to config.txt - this is almost the
 *    same as the corresponding UNIX name "config"
 *
 *    Revision 1.57  2001/11/16 00:47:43  jongfoster
 *    Changing the tty-disconnection code to use setsid().
 *
 *    Revision 1.56  2001/11/13 20:20:54  jongfoster
 *    Tabs->spaces, fixing a bug with missing {} around an if()
 *
 *    Revision 1.55  2001/11/13 20:14:53  jongfoster
 *    Patch for FreeBSD setpgrp() as suggested by Alexander Lazic
 *
 *    Revision 1.54  2001/11/07 00:03:14  steudten
 *    Give reliable return value if an error
 *    occurs not just 0 with new daemon mode.
 *
 *    Revision 1.53  2001/11/05 21:41:43  steudten
 *    Add changes to be a real daemon just for unix os.
 *    (change cwd to /, detach from controlling tty, set
 *    process group and session leader to the own process.
 *    Add DBG() Macro.
 *    Add some fatal-error log message for failed malloc().
 *    Add '-d' if compiled with 'configure --with-debug' to
 *    enable debug output.
 *
 *    Revision 1.52  2001/10/26 20:11:20  jongfoster
 *    Fixing type mismatch
 *
 *    Revision 1.51  2001/10/26 17:38:28  oes
 *    Cosmetics
 *
 *    Revision 1.50  2001/10/25 03:40:48  david__schmidt
 *    Change in porting tactics: OS/2's EMX porting layer doesn't allow multiple
 *    threads to call select() simultaneously.  So, it's time to do a real, live,
 *    native OS/2 port.  See defines for __EMX__ (the porting layer) vs. __OS2__
 *    (native). Both versions will work, but using __OS2__ offers multi-threading.
 *
 *    Revision 1.49  2001/10/23 21:41:35  jongfoster
 *    Added call to initialize the (statically-allocated of course)
 *    "out of memory" CGI response.
 *
 *    Revision 1.48  2001/10/10 19:56:46  jongfoster
 *    Moving some code that wasn't cookie-related out of an #ifdef
 *    FEATURE_COOKIE_JAR
 *
 *    Revision 1.47  2001/10/10 16:44:36  oes
 *    Added CONNECT destination port limitation check
 *
 *    Revision 1.46  2001/10/08 15:17:41  oes
 *    Re-enabled SSL forwarding
 *
 *    Revision 1.45  2001/10/07 15:42:11  oes
 *    Replaced 6 boolean members of csp with one bitmap (csp->flags)
 *
 *    Moved downgrading of the HTTP version from parse_http_request to
 *      chat(), since we can't decide if it is necessary before we have
 *      determined the actions for the URL. The HTTP command is now
 *      *always* re-built so the repairs need no longer be special-cased.
 *
 *    filter_popups now gets a csp pointer so it can raise the new
 *      CSP_FLAG_MODIFIED flag.
 *
 *    Bugfix
 *
 *    Added configurable size limit for the IOB. If the IOB grows so
 *      large that the next read would exceed the limit, the header
 *      is generated, and the header & unfiltered buffer are flushed
 *      to the client. Chat then continues in non-buffering,
 *      non-filtering body mode.
 *
 *    Revision 1.44  2001/10/02 18:13:57  oes
 *    Ooops
 *
 *    Revision 1.43  2001/10/02 15:32:13  oes
 *    Moved generation of hdr
 *
 *    Revision 1.42  2001/09/21 23:02:02  david__schmidt
 *    Cleaning up 2 compiler warnings on OS/2.
 *
 *    Revision 1.41  2001/09/16 17:05:14  jongfoster
 *    Removing unused #include showarg.h
 *
 *    Revision 1.40  2001/09/16 15:41:45  jongfoster
 *    Fixing signed/unsigned comparison warning.
 *
 *    Revision 1.39  2001/09/16 13:21:27  jongfoster
 *    Changes to use new list functions.
 *
 *    Revision 1.38  2001/09/16 13:01:46  jongfoster
 *    Removing redundant function call that zeroed zalloc()'d memory.
 *
 *    Revision 1.37  2001/09/10 11:12:24  oes
 *    Deleted unused variable
 *
 *    Revision 1.36  2001/09/10 10:56:15  oes
 *    Silenced compiler warnings
 *
 *    Revision 1.35  2001/07/31 14:44:22  oes
 *    Deleted unused size parameter from filter_popups()
 *
 *    Revision 1.34  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.33  2001/07/29 19:32:00  jongfoster
 *    Renaming _main() [mingw32 only] to real_main(), for ANSI compliance.
 *
 *    Revision 1.32  2001/07/29 18:47:05  jongfoster
 *    Adding missing #include "loadcfg.h"
 *
 *    Revision 1.31  2001/07/29 12:17:48  oes
 *    Applied pthread fix by Paul Lieverse
 *
 *    Revision 1.30  2001/07/25 22:57:13  jongfoster
 *    __BEOS__ no longer overrides FEATURE_PTHREAD.
 *    This is because FEATURE_PTHREAD will soon be widely used, so I
 *    want to keep it simple.
 *
 *    Revision 1.29  2001/07/24 12:47:06  oes
 *    Applied BeOS support update by Eugenia
 *
 *    Revision 1.28  2001/07/23 13:26:12  oes
 *    Fixed bug in popup-killing for the first read that caused binary garbage to be sent between headers and body
 *
 *    Revision 1.27  2001/07/19 19:09:47  haroon
 *    - Added code to take care of the situation where while processing the first
 *      server response (which includes the server header), after finding the end
 *      of the headers we were not looking past the end of the headers for
 *      content modification. I enabled it for filter_popups.
 *      Someone else should look to see if other similar operations should be
 *      done to the discarded portion of the buffer.
 *
 *      Note 2001/07/20: No, the other content modification mechanisms will process
 *                       the whole iob later anyway. --oes
 *
 *    Revision 1.26  2001/07/18 12:31:36  oes
 *    cosmetics
 *
 *    Revision 1.25  2001/07/15 19:43:49  jongfoster
 *    Supports POSIX threads.
 *    Also removed some unused #includes.
 *
 *    Revision 1.24  2001/07/13 14:00:40  oes
 *     - Generic content modification scheme:
 *       Each feature has its own applicability flag that is set
 *       from csp->action->flags.
 *       Replaced the "filtering" int flag , by a function pointer
 *       "content_filter" to the function that will do the content
 *       modification. If it is != NULL, the document will be buffered
 *       and processed through *content_filter, which must set
 *       csp->content_length and return a modified copy of the body
 *       or return NULL (on failiure).
 *     - Changed csp->is_text to the more generic bitmap csp->content_type
 *       which can currently take the valued CT_TEXT or CT_GIF
 *     - Reformatting etc
 *     - Removed all #ifdef PCRS
 *
 *    Revision 1.23  2001/07/02 02:28:25  iwanttokeepanon
 *    Added "#ifdef ACL_FILES" conditional compilation to line 1291 to exclude
 *    the `block_acl' call.  This prevents a compilation error when the user
 *    does not wish to use the "ACL" feature.
 *
 *    Revision 1.22  2001/06/29 21:45:41  oes
 *    Indentation, CRLF->LF, Tab-> Space
 *
 *    Revision 1.21  2001/06/29 13:29:36  oes
 *    - Cleaned up, improved comments
 *    - Unified all possible interceptors (CGI,
 *      block, trust, fast_redirect) in one
 *      place, with one (CGI) answer generation
 *      mechansim. Much clearer now.
 *    - Removed the GIF image generation, which
 *      is now done in filters.c:block_url()
 *    - Made error conditions like domain lookup
 *      failiure or (various) problems while talking
 *      to the server use cgi.c:error_response()
 *      instead of generating HTML/HTTP in chat() (yuck!)
 *    - Removed logentry from cancelled commit
 *
 *    Revision 1.20  2001/06/09 10:55:28  jongfoster
 *    Changing BUFSIZ ==> BUFFER_SIZE
 *
 *    Revision 1.19  2001/06/07 23:12:52  jongfoster
 *    Replacing function pointer in struct gateway with a directly
 *    called function forwarded_connect().
 *    Replacing struct gateway with struct forward_spec
 *
 *    Revision 1.18  2001/06/03 19:12:16  oes
 *    introduced new cgi handling
 *
 *    Revision 1.17  2001/06/01 20:07:23  jongfoster
 *    Now uses action +image-blocker{} rather than config->tinygif
 *
 *    Revision 1.16  2001/06/01 18:49:17  jongfoster
 *    Replaced "list_share" with "list" - the tiny memory gain was not
 *    worth the extra complexity.
 *
 *    Revision 1.15  2001/05/31 21:24:47  jongfoster
 *    Changed "permission" to "action" throughout.
 *    Removed DEFAULT_USER_AGENT - it must now be specified manually.
 *    Moved vanilla wafer check into chat(), since we must now
 *    decide whether or not to add it based on the URL.
 *
 *    Revision 1.14  2001/05/29 20:14:01  joergs
 *    AmigaOS bugfix: PCRS needs a lot of stack, stacksize for child threads
 *    increased.
 *
 *    Revision 1.13  2001/05/29 09:50:24  jongfoster
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
 *    Revision 1.12  2001/05/27 22:17:04  oes
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
 *    Revision 1.11  2001/05/26 17:27:53  jongfoster
 *    Added support for CLF and fixed LOG_LEVEL_LOG.
 *    Also did CRLF->LF fix of my previous patch.
 *
 *    Revision 1.10  2001/05/26 15:26:15  jongfoster
 *    ACL feature now provides more security by immediately dropping
 *    connections from untrusted hosts.
 *
 *    Revision 1.9  2001/05/26 00:28:36  jongfoster
 *    Automatic reloading of config file.
 *    Removed obsolete SIGHUP support (Unix) and Reload menu option (Win32).
 *    Most of the global variables have been moved to a new
 *    struct configuration_spec, accessed through csp->config->globalname
 *    Most of the globals remaining are used by the Win32 GUI.
 *
 *    Revision 1.8  2001/05/25 22:43:18  jongfoster
 *    Fixing minor memory leak and buffer overflow.
 *
 *    Revision 1.7  2001/05/25 22:34:30  jongfoster
 *    Hard tabs->Spaces
 *
 *    Revision 1.6  2001/05/23 00:13:58  joergs
 *    AmigaOS support fixed.
 *
 *    Revision 1.5  2001/05/22 18:46:04  oes
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
 *    Revision 1.4  2001/05/21 19:34:01  jongfoster
 *    Made failure to bind() a fatal error.
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
 *    Revision 1.2  2001/05/17 22:34:44  oes
 *     - Added hint on GIF char array generation to jcc.c
 *     - Cleaned CRLF's from the sources and related files
 *     - Repaired logging for REF and FRC
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:56  oes
 *    Initial import of version 2.9.3 source tree
 *
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

int no_daemon = 0;
struct client_state  clients[1];
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
static jb_socket bind_port_helper(struct configuration_spec *config);
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
   "HTTP/1.0 200 Connection established\r\n"
   "Proxy-Agent: Privoxy/" VERSION "\r\n\r\n";

static const char CHEADER[] =
   "HTTP/1.0 400 Invalid header received from client\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Invalid header received from client.\r\n";

static const char FTP_RESPONSE[] =
   "HTTP/1.0 400 Invalid request received from client\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Invalid request. Privoxy doesn't support FTP.\r\n";

static const char GOPHER_RESPONSE[] =
   "HTTP/1.0 400 Invalid request received from client\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Invalid request. Privoxy doesn't support gopher.\r\n";

/* XXX: should be a template */
static const char MISSING_DESTINATION_RESPONSE[] =
   "HTTP/1.0 400 Bad request received from client\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Bad request. Privoxy was unable to extract the destination.\r\n";

/* XXX: should be a template */
static const char NO_SERVER_DATA_RESPONSE[] =
   "HTTP/1.0 502 Server or forwarder response empty\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Empty server or forwarder response.\r\n"
   "The connection has been closed but Privoxy didn't receive any data.\r\n";

/* XXX: should be a template */
static const char INVALID_SERVER_HEADERS_RESPONSE[] =
   "HTTP/1.0 502 Server or forwarder response invalid\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Bad response. The server or forwarder response doesn't look like HTTP.\r\n";

#if 0
/* XXX: should be a template */
static const char NULL_BYTE_RESPONSE[] =
   "HTTP/1.0 400 Bad request received from client\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Bad request. Null byte(s) before end of request.\r\n";
#endif

/* XXX: should be a template */
static const char MESSED_UP_REQUEST_RESPONSE[] =
   "HTTP/1.0 400 Malformed request after rewriting\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "Bad request. Messed up with header filters.\r\n";

/* XXX: should be a template */
static const char CONNECTION_TIMEOUT_RESPONSE[] =
   "HTTP/1.0 502 Connection timeout\r\n"
   "Proxy-Agent: Privoxy " VERSION "\r\n"
   "Content-Type: text/plain\r\n"
   "Connection: close\r\n\r\n"
   "The connection timed out.\r\n";

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
      /* Split the domain we just got for pattern matching */
      init_domain_components(csp->http);

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

   switch (rsp->reason)
   {
      case RSP_REASON_UNSUPPORTED:
         reason = "Unsupported HTTP feature";
         break;
      case RSP_REASON_BLOCKED:
         reason = "Blocked";
         break;
      case RSP_REASON_UNTRUSTED:
         reason = "Untrusted";
         break;
      case RSP_REASON_REDIRECTED:
         reason = "Redirected";
         break;
      case RSP_REASON_CGI_CALL:
         reason = "CGI Call";
         break;
      case RSP_REASON_NO_SUCH_DOMAIN:
         reason = "DNS failure";
         break;
      case RSP_REASON_FORWARDING_FAILED:
         reason = "Forwarding failed";
         break;
      case RSP_REASON_CONNECT_FAILED:
         reason = "Connection failure";
         break;
      case RSP_REASON_OUT_OF_MEMORY:
         reason = "Out of memory (may mask other reasons)";
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
         /*
          * Not supposed to happen. If it does
          * anyway, treat it as an unknown error.
          */
         cgi_error_unknown(csp, rsp, RSP_REASON_INTERNAL_ERROR);
         /* return code doesn't matter */
      }

      if (rsp == NULL)
      {
         /* If rsp is still NULL, we have serious internal problems. */
         log_error(LOG_LEVEL_FATAL,
            "NULL response in send_crunch_response and cgi_error_unknown failed as well.");
      }

      /*
       * Extract the status code from the actual head
       * that was send to the client. It is the only
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

      /* Write the answer to the client */
      if (write_socket(csp->cfd, rsp->head, rsp->head_length)
       || write_socket(csp->cfd, rsp->body, rsp->content_length))
      {
         /* There is nothing we can do about it. */
         log_error(LOG_LEVEL_ERROR, "write to: %s failed: %E", csp->http->host);
      }

      /* Log that the request was crunched and why. */
      log_error(LOG_LEVEL_CRUNCH, "%s: %s", crunch_reason(rsp), http->url);
      log_error(LOG_LEVEL_CLF, "%s - - [%T] \"%s\" %s %u",
         csp->ip_addr_str, http->ocmd, status_code, rsp->content_length);

      /* Clean up and return */
      if (cgi_error_memory() != rsp)
      {
         free_http_response(rsp);
      } 
      return;
}


#if 0
/*********************************************************************
 *
 * Function    :  request_contains_null_bytes
 *
 * Description :  Checks for NULL bytes in the request and sends
 *                an error message to the client if any were found.
 *
 *                XXX: currently not used, see comment in chat().
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *          2  :  buf = Data from the client's request to check.
 *          3  :  len = The data length.
 *
 * Returns     :  TRUE if the request contained one or more NULL bytes, or
 *                FALSE otherwise.
 *
 *********************************************************************/
static int request_contains_null_bytes(const struct client_state *csp, char *buf, int len)
{
   size_t c_len; /* Request lenght when treated as C string */

   c_len = strlen(buf);

   if (c_len < len)
   {
      /*
       * Null byte(s) found. Log the request,
       * return an error response and hang up.
       */
      size_t tmp_len = c_len;

      do
      {
        /*
         * Replace NULL byte(s) with '°' characters
         * so the request can be logged as string.
         * XXX: Is there a better replacement character?
         */
         buf[tmp_len]='°';
         tmp_len += strlen(buf+tmp_len);
      } while (tmp_len < len);

      log_error(LOG_LEVEL_ERROR, "%s\'s request contains at least one NULL byte "
         "(length=%d, strlen=%u).", csp->ip_addr_str, len, c_len);
      log_error(LOG_LEVEL_HEADER, 
         "Offending request data with NULL bytes turned into \'°\' characters: %s", buf);

      write_socket(csp->cfd, NULL_BYTE_RESPONSE, strlen(NULL_BYTE_RESPONSE));

      /* XXX: Log correct size */
      log_error(LOG_LEVEL_CLF, "%s - - [%T] \"Invalid request\" 400 0", csp->ip_addr_str);

      return TRUE;
   }

   return FALSE;
}
#endif


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

   if (csp->http->status == 304)
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
static void wait_for_alive_connections()
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
   if ((csp->flags & CSP_FLAG_SERVER_CONNECTION_KEEP_ALIVE))
   {
      log_error(LOG_LEVEL_CONNECT, "Unsetting keep-alive flag.");
      csp->flags &= ~CSP_FLAG_SERVER_CONNECTION_KEEP_ALIVE;
   }
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
         log_error(LOG_LEVEL_ERROR,
            "Stopped waiting for the request line.");
         write_socket(csp->cfd, CONNECTION_TIMEOUT_RESPONSE,
            strlen(CONNECTION_TIMEOUT_RESPONSE));
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
 *               Note that since we're not using select() we could get
 *               blocked here if a client connected, then didn't say
 *               anything!
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
          * header and can finaly enlist it.
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

   return JB_ERR_OK;

}


/*********************************************************************
 *
 * Function    :  chat
 *
 * Description :  Once a connection to the client has been accepted,
 *                this function is called (via serve()) to handle the
 *                main business of the communication.  When this
 *                function returns, the caller must close the client
 *                socket handle.
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
   int forwarded_connect_retries = 0;
   int max_forwarded_connect_retries = csp->config->forwarded_connect_retries;
   const struct forward_spec *fwd;
   struct http_request *http;
   long len = 0; /* for buffer sizes (and negative error codes) */

   /* Function that does the content filtering for the current request */
   filter_function_ptr content_filter = NULL;

   /* Skeleton for HTTP response, if we should intercept the request */
   struct http_response *rsp;
   struct timeval timeout;

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
         "limit-connect{%s} doesn't allow CONNECT requests to port %d.",
         csp->ip_addr_str, acceptable_connect_ports, csp->http->port);
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
       * Yes. The client got the crunch response
       * and we are done here after cleaning up.
       */
      /* XXX: why list_remove_all()? */
      list_remove_all(csp->headers);

      return;
   }

   log_error(LOG_LEVEL_GPC, "%s%s", http->hostport, http->path);

   if (fwd->forward_host)
   {
      log_error(LOG_LEVEL_CONNECT, "via %s:%d to: %s",
         fwd->forward_host, fwd->forward_port, http->hostport);
   }
   else
   {
      log_error(LOG_LEVEL_CONNECT, "to %s", http->hostport);
   }

   /* here we connect to the server, gateway, or the forwarder */

   while ((csp->sfd = forwarded_connect(fwd, http, csp))
      && (errno == EINVAL)
      && (forwarded_connect_retries++ < max_forwarded_connect_retries))
   {
      log_error(LOG_LEVEL_ERROR,
         "failed request #%u to connect to %s. Trying again.",
         forwarded_connect_retries, http->hostport);
   }

   if (csp->sfd == JB_INVALID_SOCKET)
   {
      if (fwd->type != SOCKS_NONE)
      {
         /* Socks error. */
         rsp = error_response(csp, "forwarding-failed", errno);
      }
      else if (errno == EINVAL)
      {
         rsp = error_response(csp, "no-such-domain", errno);
      }
      else
      {
         rsp = error_response(csp, "connect-failed", errno);
         log_error(LOG_LEVEL_CONNECT, "connect to: %s failed: %E",
            http->hostport);
      }

      /* Write the answer to the client */
      if (rsp != NULL)
      {
         send_crunch_response(csp, rsp);
      }

      return;
   }

   hdr = list_to_text(csp->headers);
   if (hdr == NULL)
   {
      /* FIXME Should handle error properly */
      log_error(LOG_LEVEL_FATAL, "Out of memory parsing client header");
   }
   list_remove_all(csp->headers);

   if (fwd->forward_host || (http->ssl == 0))
   {
      /*
       * Write the client's (modified) header to the server
       * (along with anything else that may be in the buffer)
       */
      if (write_socket(csp->sfd, hdr, strlen(hdr))
       || (flush_socket(csp->sfd, csp->iob) <  0))
      {
         log_error(LOG_LEVEL_CONNECT,
            "write header to: %s failed: %E", http->hostport);

         rsp = error_response(csp, "connect-failed", errno);
         if (rsp)
         {
            send_crunch_response(csp, rsp);
         }

         freez(hdr);
         return;
      }
   }
   else
   {
      /*
       * We're running an SSL tunnel and we're not forwarding,
       * so just send the "connect succeeded" message to the
       * client, flush the rest, and get out of the way.
       */
      if (write_socket(csp->cfd, CSUCCEED, strlen(CSUCCEED)))
      {
         freez(hdr);
         return;
      }
      IOB_RESET(csp);
   }

   log_error(LOG_LEVEL_CONNECT, "to %s successful", http->hostport);

   /* we're finished with the client's header */
   freez(hdr);

   maxfd = (csp->cfd > csp->sfd) ? csp->cfd : csp->sfd;

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
      FD_SET(csp->cfd, &rfds);
      FD_SET(csp->sfd, &rfds);

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
      if ((csp->flags & CSP_FLAG_CHUNKED)
         && !(csp->flags & CSP_FLAG_CONTENT_LENGTH_SET)
         && ((csp->iob->eod - csp->iob->cur) >= 5)
         && !memcmp(csp->iob->eod-5, "0\r\n\r\n", 5))
      {
         log_error(LOG_LEVEL_CONNECT,
            "Looks like we read the last chunk together with "
            "the server headers. We better stop reading.");
         byte_count = (unsigned long long)(csp->iob->eod - csp->iob->cur);
         csp->expected_content_length = byte_count;
         csp->flags |= CSP_FLAG_CONTENT_LENGTH_SET;
      }
      if (server_body && server_response_is_complete(csp, byte_count))
      {
         log_error(LOG_LEVEL_CONNECT,
            "Done reading from server. Expected content length: %llu. "
            "Actual content length: %llu. Most recently received: %d.",
            csp->expected_content_length, byte_count, len);
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
            write_socket(csp->cfd, CONNECTION_TIMEOUT_RESPONSE,
               strlen(CONNECTION_TIMEOUT_RESPONSE));
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
       */
      if (FD_ISSET(csp->cfd, &rfds))
      {
         len = read_socket(csp->cfd, buf, sizeof(buf) - 1);

         if (len <= 0)
         {
            /* XXX: not sure if this is necessary. */
            mark_server_socket_tainted(csp);
            break; /* "game over, man" */
         }

         if (write_socket(csp->sfd, buf, (size_t)len))
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
      if (FD_ISSET(csp->sfd, &rfds))
      {
         fflush(0);
         len = read_socket(csp->sfd, buf, sizeof(buf) - 1);

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

            rsp = error_response(csp, "connect-failed", errno);
            if (rsp)
            {
               send_crunch_response(csp, rsp);
            }

            return;
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
               if (content_filter)
               {
                  p = execute_content_filter(csp, content_filter);
                  /*
                   * If the content filter fails, use the original
                   * buffer and length.
                   * (see p != NULL ? p : csp->iob->cur below)
                   */
                  if (NULL == p)
                  {
                     csp->content_length = (size_t)(csp->iob->eod - csp->iob->cur);
                  }

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
            if (content_filter)
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
                  content_filter = NULL;
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
            const char *header_start;
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

            header_start = csp->iob->cur;

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
                  log_error(LOG_LEVEL_INFO,
                     "MS IIS5 hack didn't produce valid headers.");
                  break;
               }
               else
               {
                  /*
                   * Since we have to wait for more from the server before
                   * we can parse the headers we just continue here.
                   */
                  long header_offset = csp->iob->cur - header_start;
                  assert(csp->iob->cur >= header_start);
                  byte_count += (unsigned long long)(len - header_offset);
                  log_error(LOG_LEVEL_CONNECT, "Continuing buffering headers. "
                     "byte_count: %llu. header_offset: %d. len: %d.",
                     byte_count, header_offset, len);
                  continue;
               }
            }

            /* Did we actually get anything? */
            if (NULL == csp->headers->first)
            {
               log_error(LOG_LEVEL_ERROR, "Empty server or forwarder response.");
               log_error(LOG_LEVEL_CLF, "%s - - [%T] \"%s\" 502 0", csp->ip_addr_str, http->cmd);
               write_socket(csp->cfd, NO_SERVER_DATA_RESPONSE, strlen(NO_SERVER_DATA_RESPONSE));
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
               content_filter = get_filter_function(csp);
            }
            /*
             * Only write if we're not buffering for content modification
             */
            if (!content_filter)
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

               byte_count += (unsigned long long)len;
            }
            else
            {
               /*
                * XXX: the header lenght should probably
                * be calculated by get_server_headers().
                */
               long header_length = csp->iob->cur - header_start;
               assert(csp->iob->cur > header_start);
               byte_count += (unsigned long long)(len - header_length);
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
               log_error(LOG_LEVEL_INFO,
                  "Closed server connection detected with MS IIS5 hack enabled.");
               break;
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
       * If Privoxy didn't recalculate the Content-Lenght,
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
}


/*********************************************************************
 *
 * Function    :  serve
 *
 * Description :  This is little more than chat.  We only "serve" to
 *                to close any socket that chat may have opened.
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
   chat(csp);
   close_socket(csp->cfd);

   if (csp->sfd != JB_INVALID_SOCKET)
   {
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
      static int monitor_thread_running = 0;

      if ((csp->config->feature_flags & RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE)
       && (csp->flags & CSP_FLAG_SERVER_CONNECTION_KEEP_ALIVE))
      {
         remember_connection(csp->sfd, csp->http, forward_url(csp, csp->http));
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
      else
      {
         forget_connection(csp->sfd);
         close_socket(csp->sfd);
      }
#else
      close_socket(csp->sfd);
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */
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
int real_main(int argc, const char *argv[])
#else
int main(int argc, const char *argv[])
#endif
{
   int argc_pos = 0;
   unsigned int random_seed;
#ifdef unix
   struct passwd *pw = NULL;
   struct group *grp = NULL;
   char *p;
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
      else if (strncmp(argv[argc_pos], "--uninstall", + 11) == 0)
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
         no_daemon = 1;
      }

      else if (strcmp(argv[argc_pos], "--pidfile" ) == 0)
      {
         if (++argc_pos == argc) usage(argv[0]);
         pidfile = strdup(argv[argc_pos]);
      }

      else if (strcmp(argv[argc_pos], "--user" ) == 0)
      {
         if (++argc_pos == argc) usage(argv[argc_pos]);

         if ((NULL != (p = strchr(argv[argc_pos], '.'))) && *(p + 1) != '0')
         {
            *p++ = '\0';
            if (NULL == (grp = getgrnam(p)))
            {
               log_error(LOG_LEVEL_FATAL, "Group %s not found.", p);
            }
         }

         if (NULL == (pw = getpwnam(argv[argc_pos])))
         {
            log_error(LOG_LEVEL_FATAL, "User %s not found.", argv[argc_pos]);
         }

         if (p != NULL) *--p = '\0';
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
   const int catched_signals[] = { SIGTERM, SIGINT, SIGHUP, 0 };
   const int ignored_signals[] = { SIGPIPE, 0 };

   for (idx = 0; catched_signals[idx] != 0; idx++)
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

   for (idx = 0; ignored_signals[idx] != 0; idx++)
   {
      if (signal(ignored_signals[idx], SIG_IGN) == SIG_ERR)
      {
         log_error(LOG_LEVEL_FATAL, "Can't set ignore-handler for signal %d: %E", ignored_signals[idx]);
      }
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
   pid_t pid = 0;
#if 0
   int   fd;
#endif

   if (!no_daemon)
   {
      pid  = fork();

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
#if 1
      /* Should be more portable, but not as well tested */
      setsid();
#else /* !1 */
#ifdef __FreeBSD__
      setpgrp(0,0);
#else /* ndef __FreeBSD__ */
      setpgrp();
#endif /* ndef __FreeBSD__ */
      fd = open("/dev/tty", O_RDONLY);
      if ( fd )
      {
         /* no error check here */
         ioctl( fd, TIOCNOTTY,0 );
         close ( fd );
      }
#endif /* 1 */
      /*
       * stderr (fd 2) will be closed later on,
       * when the config file has been parsed.
       */

      close( 0 );
      close( 1 );
      chdir("/");

   } /* -END- if (!no_daemon) */

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
 *          1  :  config = Privoxy configuration.  Specifies port
 *                         to bind to.
 *
 * Returns     :  Port that was opened.
 *
 *********************************************************************/
static jb_socket bind_port_helper(struct configuration_spec * config)
{
   int result;
   jb_socket bfd;

   if (config->haddr == NULL)
   {
      log_error(LOG_LEVEL_INFO, "Listening on port %d on all IP addresses",
                config->hport);
   }
   else
   {
      log_error(LOG_LEVEL_INFO, "Listening on port %d on IP address %s",
                config->hport, config->haddr);
   }

   result = bind_port(config->haddr, config->hport, &bfd);

   if (result < 0)
   {
      switch(result)
      {
         case -3 :
            log_error(LOG_LEVEL_FATAL, "can't bind to %s:%d: "
               "There may be another Privoxy or some other "
               "proxy running on port %d",
               (NULL != config->haddr) ? config->haddr : "INADDR_ANY",
                      config->hport, config->hport);

         case -2 :
            log_error(LOG_LEVEL_FATAL, "can't bind to %s:%d: " 
               "The hostname is not resolvable",
               (NULL != config->haddr) ? config->haddr : "INADDR_ANY", config->hport);

         default :
            log_error(LOG_LEVEL_FATAL, "can't bind to %s:%d: because %E",
               (NULL != config->haddr) ? config->haddr : "INADDR_ANY", config->hport);
      }

      /* shouldn't get here */
      return JB_INVALID_SOCKET;
   }

   config->need_bind = 0;

   return bfd;
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
   struct client_state *csp = NULL;
   jb_socket bfd;
   struct configuration_spec * config;

   config = load_config();

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   /*
    * XXX: Should be relocated once it no
    * longer needs to emit log messages.
    */
   initialize_reusable_connections();
#endif /* def FEATURE_CONNECTION_KEEP_ALIVE */

   bfd = bind_port_helper(config);

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
      sweep();

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

      if ( NULL == (csp = (struct client_state *) zalloc(sizeof(*csp))) )
      {
         log_error(LOG_LEVEL_FATAL, "malloc(%d) for csp failed: %E", sizeof(*csp));
         continue;
      }

      csp->flags |= CSP_FLAG_ACTIVE;
      csp->sfd    = JB_INVALID_SOCKET;

      csp->config = config = load_config();

      if ( config->need_bind )
      {
         /*
          * Since we were listening to the "old port", we will not see
          * a "listen" param change until the next IJB request.  So, at
          * least 1 more request must be made for us to find the new
          * setting.  I am simply closing the old socket and binding the
          * new one.
          *
          * Which-ever is correct, we will serve 1 more page via the
          * old settings.  This should probably be a "show-proxy-args"
          * request.  This should not be a so common of an operation
          * that this will hurt people's feelings.
          */

         close_socket(bfd);

         bfd = bind_port_helper(config);
      }

      log_error(LOG_LEVEL_CONNECT, "Listening for new connections ... ");

      if (!accept_connection(csp, bfd))
      {
         log_error(LOG_LEVEL_CONNECT, "accept failed: %E");

#ifdef AMIGA
         if(!childs)
         {
            exit(1);
         }
#endif
         freez(csp);
         continue;
      }
      else
      {
         log_error(LOG_LEVEL_CONNECT, "accepted connection from %s", csp->ip_addr_str);
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
         log_error(LOG_LEVEL_CONNECT, "Connection from %s dropped due to ACL", csp->ip_addr_str);
         close_socket(csp->cfd);
         freez(csp);
         continue;
      }
#endif /* def FEATURE_ACL */

      /* add it to the list of clients */
      csp->next = clients->next;
      clients->next = csp;

      if (config->multi_threaded)
      {
         int child_id;

/* this is a switch () statment in the C preprocessor - ugh */
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

         if (child_id < 0) /* failed */
         {
            char buf[BUFFER_SIZE];

            log_error(LOG_LEVEL_ERROR, "can't fork: %E");

            snprintf(buf , sizeof(buf), "Privoxy: can't fork: errno = %d", errno);

            write_socket(csp->cfd, buf, strlen(buf));
            close_socket(csp->cfd);
            csp->flags &= ~CSP_FLAG_ACTIVE;
            sleep(5);
            continue;
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
   freez(configfile);

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
