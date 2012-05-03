const char loadcfg_rcs[] = "$Id: loadcfg.c,v 1.93 2009/03/18 21:46:26 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/loadcfg.c,v $
 *
 * Purpose     :  Loads settings from the configuration file into
 *                global variables.  This file contains both the
 *                routine to load the configuration and the global
 *                variables it writes to.
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
 *    $Log: loadcfg.c,v $
 *    Revision 1.93  2009/03/18 21:46:26  fabiankeil
 *    Revert the last commit as there's a better way.
 *
 *    Revision 1.92  2009/03/18 20:43:19  fabiankeil
 *    Don't enable LOG_LEVEL_INFO by default and don't apply the user's
 *    debug settings until the logfile has been opened (if there is one).
 *    Patch submitted by Roland in #2624120.
 *
 *    Revision 1.91  2009/03/09 17:29:08  fabiankeil
 *    As of r1.88, the show-status page can use a single line for
 *    warnings about ignored directives and the names of the ignored
 *    directives themselves. Reminded by Lee, finally closes #1856559.
 *
 *    Revision 1.90  2009/03/07 17:58:02  fabiankeil
 *    Fix two mingw32-only buffer overflows. Note that triggering
 *    them requires control over the configuration file in which
 *    case all bets are off anyway.
 *
 *    Revision 1.89  2009/03/01 18:46:33  fabiankeil
 *    - Help clang understand that we aren't
 *      dereferencing NULL pointers here.
 *    - Some style fixes in the modified region.
 *
 *    Revision 1.88  2009/02/28 10:57:10  fabiankeil
 *    Gimme a break or two. Don't let the show-status page
 *    link to the website documentation for the user-manual
 *    directive itself.
 *
 *    Revision 1.87  2009/02/15 07:56:13  fabiankeil
 *    Increase default socket timeout to 300 seconds.
 *
 *    Revision 1.86  2009/02/08 19:18:57  fabiankeil
 *    Now that we have the match-all.action file, the other action
 *    files changed their position in config->actions_file[] back
 *    to the way it was before standard.action got removed and the
 *    changes from revision 1.84 have to be reverted.
 *
 *    Revision 1.85  2009/01/22 12:06:26  fabiankeil
 *    Don't keep connections alive when running single-threaded.
 *
 *    Revision 1.84  2009/01/14 16:14:36  fabiankeil
 *    Due to the standard.action file removal, the other action
 *    files changed their position in config->actions_file[].
 *    Update mingw32 kludge accordingly.
 *
 *    Revision 1.83  2008/12/20 14:53:55  fabiankeil
 *    Add config option socket-timeout to control the time
 *    Privoxy waits for data to arrive on a socket. Useful
 *    in case of stale ssh tunnels or when fuzz-testing.
 *
 *    Revision 1.82  2008/11/16 12:43:49  fabiankeil
 *    Turn keep-alive support into a runtime feature
 *    that is disabled by setting keep-alive-timeout
 *    to a negative value.
 *
 *    Revision 1.81  2008/11/13 09:08:42  fabiankeil
 *    Add new config option: keep-alive-timeout.
 *
 *    Revision 1.80  2008/08/31 15:59:03  fabiankeil
 *    There's no reason to let remote toggling support depend
 *    on FEATURE_CGI_EDIT_ACTIONS, so make sure it doesn't.
 *
 *    Revision 1.79  2008/08/30 12:03:07  fabiankeil
 *    Remove FEATURE_COOKIE_JAR.
 *
 *    Revision 1.78  2008/08/02 08:23:22  fabiankeil
 *    If the enforce-blocks directive is used with FEATURE_FORCE_LOAD
 *    disabled, log a message that blocks will always be enforced
 *    instead of complaining about an unrecognized directive.
 *    Reported by Pietro Leone.
 *
 *    Revision 1.77  2008/05/26 16:13:22  fabiankeil
 *    Reuse directive_hash and don't hash the same directive twice.
 *
 *    Revision 1.76  2008/05/10 09:03:16  fabiankeil
 *    - Merge three string_append() calls.
 *    - Remove useless assertion.
 *
 *    Revision 1.75  2008/03/30 14:52:05  fabiankeil
 *    Rename load_actions_file() and load_re_filterfile()
 *    as they load multiple files "now".
 *
 *    Revision 1.74  2008/03/26 18:07:07  fabiankeil
 *    Add hostname directive. Closes PR#1918189.
 *
 *    Revision 1.73  2008/02/16 16:54:51  fabiankeil
 *    Fix typo.
 *
 *    Revision 1.72  2008/02/03 13:46:15  fabiankeil
 *    Add SOCKS5 support. Patch #1862863 by Eric M. Hopper with minor changes.
 *
 *    Revision 1.71  2007/12/23 15:24:56  fabiankeil
 *    Reword "unrecognized directive" warning, use better
 *    mark up and add a <br>. Fixes parts of #1856559.
 *
 *    Revision 1.70  2007/12/15 14:24:05  fabiankeil
 *    Plug memory leak if listen-address only specifies the port.
 *
 *    Revision 1.69  2007/10/27 13:02:27  fabiankeil
 *    Relocate daemon-mode-related log messages to make sure
 *    they aren't shown again in case of configuration reloads.
 *
 *    Revision 1.68  2007/10/19 16:32:34  fabiankeil
 *    Plug memory leak introduced with my last commit.
 *
 *    Revision 1.67  2007/10/14 14:12:41  fabiankeil
 *    When in daemon mode, close stderr after the configuration file has been
 *    parsed the first time. If logfile isn't set, stop logging. Fixes BR#897436.
 *
 *    Revision 1.66  2007/08/05 14:02:09  fabiankeil
 *    #1763173 from Stefan Huehner: declare unload_configfile() static.
 *
 *    Revision 1.65  2007/07/21 11:51:36  fabiankeil
 *    As Hal noticed, checking dispatch_cgi() as the last cruncher
 *    looks like a bug if CGI requests are blocked unintentionally,
 *    so don't do it unless the user enabled the new config option
 *    "allow-cgi-request-crunching".
 *
 *    Revision 1.64  2007/05/21 10:44:08  fabiankeil
 *    - Use strlcpy() instead of strcpy().
 *    - Stop treating actions files special. Expect a complete file name
 *      (with or without path) like it's done for the rest of the files.
 *      Closes FR#588084.
 *    - Remove an unnecessary temporary memory allocation.
 *    - Don't log anything to the console when running as
 *      daemon and no errors occurred.
 *
 *    Revision 1.63  2007/04/09 18:11:36  fabiankeil
 *    Don't mistake VC++'s _snprintf() for a snprintf() replacement.
 *
 *    Revision 1.62  2007/03/17 15:20:05  fabiankeil
 *    New config option: enforce-blocks.
 *
 *    Revision 1.61  2007/03/16 16:47:35  fabiankeil
 *    - Mention other reasons why acl directive loading might have failed.
 *    - Don't log the acl source if the acl destination is to blame.
 *
 *    Revision 1.60  2007/01/27 13:09:16  fabiankeil
 *    Add new config option "templdir" to
 *    change the templates directory.
 *
 *    Revision 1.59  2006/12/31 17:56:38  fabiankeil
 *    Added config option accept-intercepted-requests
 *    and disabled it by default.
 *
 *    Revision 1.58  2006/12/31 14:24:29  fabiankeil
 *    Fix gcc43 compiler warnings.
 *
 *    Revision 1.57  2006/12/21 12:57:48  fabiankeil
 *    Add config option "split-large-forms"
 *    to work around the browser bug reported
 *    in BR #1570678.
 *
 *    Revision 1.56  2006/12/17 17:04:51  fabiankeil
 *    Move the <br> in the generated HTML for the config
 *    options from the beginning of the string to its end.
 *    Keeps the white space in balance.
 *
 *    Revision 1.55  2006/11/28 15:31:52  fabiankeil
 *    Fix memory leak in case of config file reloads.
 *
 *    Revision 1.54  2006/10/21 16:04:22  fabiankeil
 *    Modified kludge for win32 to make ming32 menu
 *    "Options/Edit Filters" (sort of) work again.
 *    Same limitations as for the action files apply.
 *    Fixes BR 1567373.
 *
 *    Revision 1.53  2006/09/06 18:45:03  fabiankeil
 *    Incorporate modified version of Roland Rosenfeld's patch to
 *    optionally access the user-manual via Privoxy. Closes patch 679075.
 *
 *    Formatting changed to Privoxy style, added call to
 *    cgi_error_no_template if the requested file doesn't
 *    exist and modified check whether or not Privoxy itself
 *    should serve the manual. Should work cross-platform now.
 *
 *    Revision 1.52  2006/09/06 10:43:32  fabiankeil
 *    Added config option enable-remote-http-toggle
 *    to specify if Privoxy should recognize special
 *    headers (currently only X-Filter) to change its
 *    behaviour. Disabled by default.
 *
 *    Revision 1.51  2006/09/06 09:23:37  fabiankeil
 *    Make number of retries in case of forwarded-connect problems
 *    a config file option (forwarded-connect-retries) and use 0 as
 *    default.
 *
 *    Revision 1.50  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.48.2.7  2006/02/02 17:29:16  david__schmidt
 *    Don't forget to malloc space for the null terminator...
 *
 *    Revision 1.48.2.6  2006/01/29 23:10:56  david__schmidt
 *    Multiple filter file support
 *
 *    Revision 1.48.2.5  2003/05/08 15:17:25  oes
 *    Closed two memory leaks; hopefully the last remaining ones
 *    (in the main execution paths, anyway).
 *
 *    Revision 1.48.2.4  2003/04/11 12:06:14  oes
 *    Addressed bug #719435
 *     - Extraneous filterfile directives now logged as errors
 *     - This and unrecnonised directives now really obvious on status page
 *
 *    Revision 1.48.2.3  2003/03/11 11:53:59  oes
 *    Cosmetic: Renamed cryptic variable
 *
 *    Revision 1.48.2.2  2002/11/12 16:28:20  oes
 *    Move unrelated variable declaration out of #ifdef FEATURE_ACL; fixes bug #636655
 *
 *    Revision 1.48.2.1  2002/08/21 17:58:05  oes
 *    Temp kludge to let user and default action file be edited through win32 GUI (FR 592080)
 *
 *    Revision 1.48  2002/05/14 21:30:38  oes
 *    savearg now uses own linking code instead of (now special-cased) add_help_link
 *
 *    Revision 1.47  2002/05/12 21:36:29  jongfoster
 *    Correcting function comments
 *
 *    Revision 1.46  2002/04/26 12:55:14  oes
 *     - New option "user-manual", defaults to our site
 *       via project.h #define
 *     - savearg now embeds option names in help links
 *
 *    Revision 1.45  2002/04/24 02:11:54  oes
 *    Jon's multiple AF patch: Allow up to MAX_AF_FILES actionsfile options
 *
 *    Revision 1.44  2002/04/08 20:37:13  swa
 *    fixed JB spelling
 *
 *    Revision 1.43  2002/04/08 20:36:50  swa
 *    fixed JB spelling
 *
 *    Revision 1.42  2002/04/05 15:50:15  oes
 *    fix for invalid HTML proxy_args
 *
 *    Revision 1.41  2002/03/31 17:19:00  jongfoster
 *    Win32 only: Enabling STRICT to fix a VC++ compile warning.
 *
 *    Revision 1.40  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.39  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.38  2002/03/24 13:05:48  jongfoster
 *    Renaming re_filterfile to filterfile
 *
 *    Revision 1.37  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.36  2002/03/13 00:27:05  jongfoster
 *    Killing warnings
 *
 *    Revision 1.35  2002/03/07 03:52:44  oes
 *    Set logging to tty for --no-daemon mode
 *
 *    Revision 1.34  2002/03/06 23:14:35  jongfoster
 *    Trivial cosmetic changes to make function comments easier to find.
 *
 *    Revision 1.33  2002/03/05 04:52:42  oes
 *    Deleted non-errlog debugging code
 *
 *    Revision 1.32  2002/03/04 18:24:53  oes
 *    Re-enabled output of unknown config directive hash
 *
 *    Revision 1.31  2002/03/03 15:07:20  oes
 *    Re-enabled automatic config reloading
 *
 *    Revision 1.30  2002/01/22 23:31:43  jongfoster
 *    Replacing strsav() with string_append()
 *
 *    Revision 1.29  2002/01/17 21:02:30  jongfoster
 *    Moving all our URL and URL pattern parsing code to urlmatch.c.
 *
 *    Renaming free_url to free_url_spec, since it frees a struct url_spec.
 *
 *    Revision 1.28  2001/12/30 14:07:32  steudten
 *    - Add signal handling (unix)
 *    - Add SIGHUP handler (unix)
 *    - Add creation of pidfile (unix)
 *    - Add action 'top' in rc file (RH)
 *    - Add entry 'SIGNALS' to manpage
 *    - Add exit message to logfile (unix)
 *
 *    Revision 1.27  2001/11/07 00:02:13  steudten
 *    Add line number in error output for lineparsing for
 *    actionsfile and configfile.
 *    Special handling for CLF added.
 *
 *    Revision 1.26  2001/11/05 21:41:43  steudten
 *    Add changes to be a real daemon just for unix os.
 *    (change cwd to /, detach from controlling tty, set
 *    process group and session leader to the own process.
 *    Add DBG() Macro.
 *    Add some fatal-error log message for failed malloc().
 *    Add '-d' if compiled with 'configure --with-debug' to
 *    enable debug output.
 *
 *    Revision 1.25  2001/10/25 03:40:48  david__schmidt
 *    Change in porting tactics: OS/2's EMX porting layer doesn't allow multiple
 *    threads to call select() simultaneously.  So, it's time to do a real, live,
 *    native OS/2 port.  See defines for __EMX__ (the porting layer) vs. __OS2__
 *    (native). Both versions will work, but using __OS2__ offers multi-threading.
 *
 *    Revision 1.24  2001/10/23 21:40:30  jongfoster
 *    Added support for enable-edit-actions and enable-remote-toggle config
 *    file options.
 *
 *    Revision 1.23  2001/10/07 15:36:00  oes
 *    Introduced new config option "buffer-limit"
 *
 *    Revision 1.22  2001/09/22 16:36:59  jongfoster
 *    Removing unused parameter fs from read_config_line()
 *
 *    Revision 1.21  2001/09/16 17:10:43  jongfoster
 *    Moving function savearg() here, since it was the only thing left in
 *    showargs.c.
 *
 *    Revision 1.20  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.19  2001/07/15 17:45:16  jongfoster
 *    Removing some unused #includes
 *
 *    Revision 1.18  2001/07/13 14:01:14  oes
 *     - Removed all #ifdef PCRS
 *     - Removed vim-settings
 *
 *    Revision 1.17  2001/06/29 13:31:03  oes
 *    - Improved comments
 *    - Fixed (actionsfile) and sorted hashes
 *    - Introduced admin_address and proxy-info-url
 *      as config parameters
 *    - Renamed config->proxy_args_invocation (which didn't have
 *      the invocation but the options!) to config->proxy_args
 *    - Various adaptions
 *    - Removed logentry from cancelled commit
 *
 *    Revision 1.16  2001/06/09 10:55:28  jongfoster
 *    Changing BUFSIZ ==> BUFFER_SIZE
 *
 *    Revision 1.15  2001/06/07 23:13:40  jongfoster
 *    Merging ACL and forward files into config file.
 *    Cosmetic: Sorting config file options alphabetically.
 *    Cosmetic: Adding brief syntax comments to config file options.
 *
 *    Revision 1.14  2001/06/07 14:46:25  joergs
 *    Missing make_path() added for re_filterfile.
 *
 *    Revision 1.13  2001/06/05 22:33:54  jongfoster
 *
 *    Fixed minor memory leak.
 *    Also now uses make_path to prepend the pathnames.
 *
 *    Revision 1.12  2001/06/05 20:04:09  jongfoster
 *    Now uses _snprintf() in place of snprintf() under Win32.
 *
 *    Revision 1.11  2001/06/04 18:31:58  swa
 *    files are now prefixed with either `confdir' or `logdir'.
 *    `make redhat-dist' replaces both entries confdir and logdir
 *    with redhat values
 *
 *    Revision 1.10  2001/06/03 19:11:54  oes
 *    introduced confdir option
 *
 *    Revision 1.9  2001/06/01 20:06:24  jongfoster
 *    Removed support for "tinygif" option - moved to actions file.
 *
 *    Revision 1.8  2001/05/31 21:27:13  jongfoster
 *    Removed many options from the config file and into the
 *    "actions" file: add_forwarded, suppress_vanilla_wafer,
 *    wafer, add_header, user_agent, referer, from
 *    Also globally replaced "permission" with "action".
 *
 *    Revision 1.7  2001/05/29 09:50:24  jongfoster
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
 *    Revision 1.2  2001/05/17 23:01:01  oes
 *     - Cleaned CRLF's from the sources and related files
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:58  oes
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
#include <ctype.h>
#include <assert.h>

#ifdef _WIN32

# ifndef STRICT
#  define STRICT
# endif
# include <windows.h>

# include "win32.h"
# ifndef _WIN_CONSOLE
#  include "w32log.h"
# endif /* ndef _WIN_CONSOLE */

#else /* ifndef _WIN32 */

#ifndef __OS2__
# include <unistd.h>
# include <sys/wait.h>
#endif
# include <sys/time.h>
# include <sys/stat.h>
# include <signal.h>

#endif

#include "loadcfg.h"
#include "list.h"
#include "jcc.h"
#include "filters.h"
#include "loaders.h"
#include "miscutil.h"
#include "errlog.h"
#include "ssplit.h"
#include "encode.h"
#include "urlmatch.h"
#include "cgi.h"
#include "gateway.h"

const char loadcfg_h_rcs[] = LOADCFG_H_VERSION;

/*
 * Fix a problem with Solaris.  There should be no effect on other
 * platforms.
 * Solaris's isspace() is a macro which uses it's argument directly
 * as an array index.  Therefore we need to make sure that high-bit
 * characters generate +ve values, and ideally we also want to make
 * the argument match the declared parameter type of "int".
 */
#define ijb_isupper(__X) isupper((int)(unsigned char)(__X))
#define ijb_tolower(__X) tolower((int)(unsigned char)(__X))

#ifdef FEATURE_TOGGLE
/* Privoxy is enabled by default. */
int global_toggle_state = 1;
#endif /* def FEATURE_TOGGLE */

/* The filename of the configfile */
const char *configfile  = NULL;

/*
 * CGI functions will later need access to the invocation args,
 * so we will make argc and argv global.
 */
int Argc = 0;
const char **Argv = NULL;

static struct file_list *current_configfile = NULL;


/*
 * This takes the "cryptic" hash of each keyword and aliases them to
 * something a little more readable.  This also makes changing the
 * hash values easier if they should change or the hash algorthm changes.
 * Use the included "hash" program to find out what the hash will be
 * for any string supplied on the command line.  (Or just put it in the
 * config file and read the number from the error message in the log).
 *
 * Please keep this list sorted alphabetically (but with the Windows
 * console and GUI specific options last).
 */

#define hash_actions_file                1196306641ul /* "actionsfile" */
#define hash_accept_intercepted_requests 1513024973ul /* "accept-intercepted-requests" */
#define hash_admin_address               4112573064ul /* "admin-address" */
#define hash_allow_cgi_request_crunching  258915987ul /* "allow-cgi-request-crunching" */
#define hash_buffer_limit                1881726070ul /* "buffer-limit */
#define hash_confdir                        1978389ul /* "confdir" */
#define hash_debug                            78263ul /* "debug" */
#define hash_deny_access                 1227333715ul /* "deny-access" */
#define hash_enable_edit_actions         2517097536ul /* "enable-edit-actions" */
#define hash_enable_remote_toggle        2979744683ul /* "enable-remote-toggle" */
#define hash_enable_remote_http_toggle    110543988ul /* "enable-remote-http-toggle" */
#define hash_enforce_blocks              1862427469ul /* "enforce-blocks" */
#define hash_filterfile                   250887266ul /* "filterfile" */
#define hash_forward                        2029845ul /* "forward" */
#define hash_forward_socks4              3963965521ul /* "forward-socks4" */
#define hash_forward_socks4a             2639958518ul /* "forward-socks4a" */
#define hash_forward_socks5              3963965522ul /* "forward-socks5" */
#define hash_forwarded_connect_retries    101465292ul /* "forwarded-connect-retries" */
#define hash_hostname                      10308071ul /* "hostname" */
#define hash_keep_alive_timeout          3878599515ul /* "keep-alive-timeout" */
#define hash_listen_address              1255650842ul /* "listen-address" */
#define hash_logdir                          422889ul /* "logdir" */
#define hash_logfile                        2114766ul /* "logfile" */
#define hash_permit_access               3587953268ul /* "permit-access" */
#define hash_proxy_info_url              3903079059ul /* "proxy-info-url" */
#define hash_single_threaded             4250084780ul /* "single-threaded" */
#define hash_socket_timeout              1809001761ul /* "socket-timeout" */
#define hash_split_large_cgi_forms        671658948ul /* "split-large-cgi-forms" */
#define hash_suppress_blocklists         1948693308ul /* "suppress-blocklists" */
#define hash_templdir                      11067889ul /* "templdir" */
#define hash_toggle                          447966ul /* "toggle" */
#define hash_trust_info_url               430331967ul /* "trust-info-url" */
#define hash_trustfile                     56494766ul /* "trustfile" */
#define hash_usermanual                  1416668518ul /* "user-manual" */
#define hash_activity_animation          1817904738ul /* "activity-animation" */
#define hash_close_button_minimizes      3651284693ul /* "close-button-minimizes" */
#define hash_hide_console                2048809870ul /* "hide-console" */
#define hash_log_buffer_size             2918070425ul /* "log-buffer-size" */
#define hash_log_font_name               2866730124ul /* "log-font-name" */
#define hash_log_font_size               2866731014ul /* "log-font-size" */
#define hash_log_highlight_messages      4032101240ul /* "log-highlight-messages" */
#define hash_log_max_lines               2868344173ul /* "log-max-lines" */
#define hash_log_messages                2291744899ul /* "log-messages" */
#define hash_show_on_task_bar             215410365ul /* "show-on-task-bar" */


static void savearg(char *command, char *argument, struct configuration_spec * config);

/*********************************************************************
 *
 * Function    :  unload_configfile
 *
 * Description :  Free the config structure and all components.
 *
 * Parameters  :
 *          1  :  data: struct configuration_spec to unload
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void unload_configfile (void * data)
{
   struct configuration_spec * config = (struct configuration_spec *)data;
   struct forward_spec *cur_fwd = config->forward;
   int i;

#ifdef FEATURE_ACL
   struct access_control_list *cur_acl = config->acl;

   while (cur_acl != NULL)
   {
      struct access_control_list * next_acl = cur_acl->next;
      free(cur_acl);
      cur_acl = next_acl;
   }
   config->acl = NULL;
#endif /* def FEATURE_ACL */

   while (cur_fwd != NULL)
   {
      struct forward_spec * next_fwd = cur_fwd->next;
      free_url_spec(cur_fwd->url);

      freez(cur_fwd->gateway_host);
      freez(cur_fwd->forward_host);
      free(cur_fwd);
      cur_fwd = next_fwd;
   }
   config->forward = NULL;

   freez(config->confdir);
   freez(config->logdir);
   freez(config->templdir);
   freez(config->hostname);

   freez(config->haddr);
   freez(config->logfile);

   for (i = 0; i < MAX_AF_FILES; i++)
   {
      freez(config->actions_file_short[i]);
      freez(config->actions_file[i]);
      freez(config->re_filterfile_short[i]);
      freez(config->re_filterfile[i]);
   }

   freez(config->admin_address);
   freez(config->proxy_info_url);
   freez(config->proxy_args);
   freez(config->usermanual);

#ifdef FEATURE_TRUST
   freez(config->trustfile);
   list_remove_all(config->trust_info);
#endif /* def FEATURE_TRUST */

   for (i = 0; i < MAX_AF_FILES; i++)
   {
      freez(config->re_filterfile[i]);
   }

   freez(config);
}


#ifdef FEATURE_GRACEFUL_TERMINATION
/*********************************************************************
 *
 * Function    :  unload_current_config_file
 *
 * Description :  Unloads current config file - reset to state at
 *                beginning of program.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void unload_current_config_file(void)
{
   if (current_configfile)
   {
      current_configfile->unloader = unload_configfile;
      current_configfile = NULL;
   }
}
#endif


/*********************************************************************
 *
 * Function    :  load_config
 *
 * Description :  Load the config file and all parameters.
 *
 *                XXX: more than thousand lines long
 *                and thus in serious need of refactoring.
 *
 * Parameters  :  None
 *
 * Returns     :  The configuration_spec, or NULL on error.
 *
 *********************************************************************/
struct configuration_spec * load_config(void)
{
   char buf[BUFFER_SIZE];
   char *p, *q;
   FILE *configfp = NULL;
   struct configuration_spec * config = NULL;
   struct client_state * fake_csp;
   struct file_list *fs;
   unsigned long linenum = 0;
   int i;
   char *logfile = NULL;
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   int keep_alive_timeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
#endif

   if (!check_file_changed(current_configfile, configfile, &fs))
   {
      /* No need to load */
      return ((struct configuration_spec *)current_configfile->f);
   }
   if (NULL == fs)
   {
      log_error(LOG_LEVEL_FATAL,
         "can't check configuration file '%s':  %E", configfile);
      return NULL;
   }

   if (NULL != current_configfile)
   {
      log_error(LOG_LEVEL_INFO, "Reloading configuration file '%s'", configfile);
   }

#ifdef FEATURE_TOGGLE
   global_toggle_state = 1;
#endif /* def FEATURE_TOGGLE */

   fs->f = config = (struct configuration_spec *)zalloc(sizeof(*config));

   if (NULL == config)
   {
      freez(fs->filename);
      freez(fs);
      log_error(LOG_LEVEL_FATAL, "can't allocate memory for configuration");
      return NULL;
   }

   /*
    * This is backwards from how it's usually done.
    * Following the usual pattern, "fs" would be stored in a member
    * variable in "csp", and then we'd access "config" from "fs->f",
    * using a cast.  However, "config" is used so often that a
    * cast each time would be very ugly, and the extra indirection
    * would waste CPU cycles.  Therefore we store "config" in
    * "csp->config", and "fs" in "csp->config->config_file_list".
    */
   config->config_file_list = fs;

   /*
    * Set to defaults
    */
   config->multi_threaded            = 1;
   config->hport                     = HADDR_PORT;
   config->buffer_limit              = 4096 * 1024;
   config->usermanual                = strdup(USER_MANUAL_URL);
   config->proxy_args                = strdup("");
   config->forwarded_connect_retries = 0;
   config->socket_timeout            = 300; /* XXX: Should be a macro. */
   config->feature_flags            &= ~RUNTIME_FEATURE_CGI_TOGGLE;
   config->feature_flags            &= ~RUNTIME_FEATURE_SPLIT_LARGE_FORMS;
   config->feature_flags            &= ~RUNTIME_FEATURE_ACCEPT_INTERCEPTED_REQUESTS;

   configfp = fopen(configfile, "r");
   if (NULL == configfp)
   {
      log_error(LOG_LEVEL_FATAL,
         "can't open configuration file '%s':  %E", configfile);
      /* Never get here - LOG_LEVEL_FATAL causes program exit */
   }

   while (read_config_line(buf, sizeof(buf), configfp, &linenum) != NULL)
   {
      char cmd[BUFFER_SIZE];
      char arg[BUFFER_SIZE];
      char tmp[BUFFER_SIZE];
#ifdef FEATURE_ACL
      struct access_control_list *cur_acl;
#endif /* def FEATURE_ACL */
      struct forward_spec *cur_fwd;
      int vec_count;
      char *vec[3];
      unsigned long directive_hash;

      strlcpy(tmp, buf, sizeof(tmp));

      /* Copy command (i.e. up to space or tab) into cmd */
      p = buf;
      q = cmd;
      while (*p && (*p != ' ') && (*p != '\t'))
      {
         *q++ = *p++;
      }
      *q = '\0';

      /* Skip over the whitespace in buf */
      while (*p && ((*p == ' ') || (*p == '\t')))
      {
         p++;
      }

      /* Copy the argument into arg */
      strlcpy(arg, p, sizeof(arg));

      /* Should never happen, but check this anyway */
      if (*cmd == '\0')
      {
         continue;
      }

      /* Make sure the command field is lower case */
      for (p = cmd; *p; p++)
      {
         if (ijb_isupper(*p))
         {
            *p = (char)ijb_tolower(*p);
         }
      }

      directive_hash = hash_string(cmd);
      switch (directive_hash)
      {
/* *************************************************************************
 * actionsfile actions-file-name
 * In confdir by default
 * *************************************************************************/
         case hash_actions_file :
            i = 0;
            while ((i < MAX_AF_FILES) && (NULL != config->actions_file[i]))
            {
               i++;
            }

            if (i >= MAX_AF_FILES)
            {
               log_error(LOG_LEVEL_FATAL, "Too many 'actionsfile' directives in config file - limit is %d.\n"
                  "(You can increase this limit by changing MAX_AF_FILES in project.h and recompiling).",
                  MAX_AF_FILES);
            }
            config->actions_file_short[i] = strdup(arg);
            config->actions_file[i] = make_path(config->confdir, arg);

            break;
/* *************************************************************************
 * accept-intercepted-requests
 * *************************************************************************/
         case hash_accept_intercepted_requests:
            if ((*arg != '\0') && (0 != atoi(arg)))
            {
               config->feature_flags |= RUNTIME_FEATURE_ACCEPT_INTERCEPTED_REQUESTS;
            }
            else
            {
               config->feature_flags &= ~RUNTIME_FEATURE_ACCEPT_INTERCEPTED_REQUESTS;
            }
            break;

/* *************************************************************************
 * admin-address email-address
 * *************************************************************************/
         case hash_admin_address :
            freez(config->admin_address);
            config->admin_address = strdup(arg);
            break;

/* *************************************************************************
 * allow-cgi-request-crunching
 * *************************************************************************/
         case hash_allow_cgi_request_crunching:
            if ((*arg != '\0') && (0 != atoi(arg)))
            {
               config->feature_flags |= RUNTIME_FEATURE_CGI_CRUNCHING;
            }
            else
            {
               config->feature_flags &= ~RUNTIME_FEATURE_CGI_CRUNCHING;
            }
            break;

/* *************************************************************************
 * buffer-limit n
 * *************************************************************************/
         case hash_buffer_limit :
            config->buffer_limit = (size_t)(1024 * atoi(arg));
            break;

/* *************************************************************************
 * confdir directory-name
 * *************************************************************************/
         case hash_confdir :
            freez(config->confdir);
            config->confdir = make_path( NULL, arg);
            break;

/* *************************************************************************
 * debug n
 * Specifies debug level, multiple values are ORed together.
 * *************************************************************************/
         case hash_debug :
            config->debug |= atoi(arg);
            break;

/* *************************************************************************
 * deny-access source-ip[/significant-bits] [dest-ip[/significant-bits]]
 * *************************************************************************/
#ifdef FEATURE_ACL
         case hash_deny_access:
            strlcpy(tmp, arg, sizeof(tmp));
            vec_count = ssplit(tmp, " \t", vec, SZ(vec), 1, 1);

            if ((vec_count != 1) && (vec_count != 2))
            {
               log_error(LOG_LEVEL_ERROR, "Wrong number of parameters for "
                     "deny-access directive in configuration file.");
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Wrong number of parameters for "
                  "deny-access directive in configuration file.<br><br>\n");
               break;
            }

            /* allocate a new node */
            cur_acl = (struct access_control_list *) zalloc(sizeof(*cur_acl));

            if (cur_acl == NULL)
            {
               log_error(LOG_LEVEL_FATAL, "can't allocate memory for configuration");
               /* Never get here - LOG_LEVEL_FATAL causes program exit */
               break;
            }
            cur_acl->action = ACL_DENY;

            if (acl_addr(vec[0], cur_acl->src) < 0)
            {
               log_error(LOG_LEVEL_ERROR, "Invalid source address, port or netmask "
                  "for deny-access directive in configuration file: \"%s\"", vec[0]);
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Invalid source address, port or netmask "
                  "for deny-access directive in configuration file: \"");
               string_append(&config->proxy_args,
                  vec[0]);
               string_append(&config->proxy_args,
                  "\"<br><br>\n");
               freez(cur_acl);
               break;
            }
            if (vec_count == 2)
            {
               if (acl_addr(vec[1], cur_acl->dst) < 0)
               {
                  log_error(LOG_LEVEL_ERROR, "Invalid destination address, port or netmask "
                     "for deny-access directive in configuration file: \"%s\"", vec[1]);
                  string_append(&config->proxy_args,
                     "<br>\nWARNING: Invalid destination address, port or netmask "
                     "for deny-access directive in configuration file: \"");
                  string_append(&config->proxy_args,
                     vec[1]);
                  string_append(&config->proxy_args,
                     "\"<br><br>\n");
                  freez(cur_acl);
                  break;
               }
            }

            /*
             * Add it to the list.  Note we reverse the list to get the
             * behaviour the user expects.  With both the ACL and
             * actions file, the last match wins.  However, the internal
             * implementations are different:  The actions file is stored
             * in the same order as the file, and scanned completely.
             * With the ACL, we reverse the order as we load it, then
             * when we scan it we stop as soon as we get a match.
             */
            cur_acl->next  = config->acl;
            config->acl = cur_acl;

            break;
#endif /* def FEATURE_ACL */

/* *************************************************************************
 * enable-edit-actions 0|1
 * *************************************************************************/
#ifdef FEATURE_CGI_EDIT_ACTIONS
         case hash_enable_edit_actions:
            if ((*arg != '\0') && (0 != atoi(arg)))
            {
               config->feature_flags |= RUNTIME_FEATURE_CGI_EDIT_ACTIONS;
            }
            else
            {
               config->feature_flags &= ~RUNTIME_FEATURE_CGI_EDIT_ACTIONS;
            }
            break;
#endif /* def FEATURE_CGI_EDIT_ACTIONS */

/* *************************************************************************
 * enable-remote-toggle 0|1
 * *************************************************************************/
#ifdef FEATURE_TOGGLE
         case hash_enable_remote_toggle:
            if ((*arg != '\0') && (0 != atoi(arg)))
            {
               config->feature_flags |= RUNTIME_FEATURE_CGI_TOGGLE;
            }
            else
            {
               config->feature_flags &= ~RUNTIME_FEATURE_CGI_TOGGLE;
            }
            break;
#endif /* def FEATURE_TOGGLE */

/* *************************************************************************
 * enable-remote-http-toggle 0|1
 * *************************************************************************/
         case hash_enable_remote_http_toggle:
            if ((*arg != '\0') && (0 != atoi(arg)))
            {
               config->feature_flags |= RUNTIME_FEATURE_HTTP_TOGGLE;
            }
            else
            {
               config->feature_flags &= ~RUNTIME_FEATURE_HTTP_TOGGLE;
            }
            break;

/* *************************************************************************
 * enforce-blocks 0|1
 * *************************************************************************/
         case hash_enforce_blocks:
#ifdef FEATURE_FORCE_LOAD
            if ((*arg != '\0') && (0 != atoi(arg)))
            {
               config->feature_flags |= RUNTIME_FEATURE_ENFORCE_BLOCKS;
            }
            else
            {
               config->feature_flags &= ~RUNTIME_FEATURE_ENFORCE_BLOCKS;
            }
#else
            log_error(LOG_LEVEL_ERROR, "Ignoring directive 'enforce-blocks'. "
               "FEATURE_FORCE_LOAD is disabled, blocks will always be enforced.");
#endif /* def FEATURE_FORCE_LOAD */
            break;

/* *************************************************************************
 * filterfile file-name
 * In confdir by default.
 * *************************************************************************/
         case hash_filterfile :
            i = 0;
            while ((i < MAX_AF_FILES) && (NULL != config->re_filterfile[i]))
            {
               i++;
            }

            if (i >= MAX_AF_FILES)
            {
               log_error(LOG_LEVEL_FATAL, "Too many 'filterfile' directives in config file - limit is %d.\n"
                  "(You can increase this limit by changing MAX_AF_FILES in project.h and recompiling).",
                  MAX_AF_FILES);
            }
            config->re_filterfile_short[i] = strdup(arg);
            config->re_filterfile[i] = make_path(config->confdir, arg);

            break;

/* *************************************************************************
 * forward url-pattern (.|http-proxy-host[:port])
 * *************************************************************************/
         case hash_forward:
            strlcpy(tmp, arg, sizeof(tmp));
            vec_count = ssplit(tmp, " \t", vec, SZ(vec), 1, 1);

            if (vec_count != 2)
            {
               log_error(LOG_LEVEL_ERROR, "Wrong number of parameters for forward "
                     "directive in configuration file.");
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Wrong number of parameters for "
                  "forward directive in configuration file.");
               break;
            }

            /* allocate a new node */
            cur_fwd = zalloc(sizeof(*cur_fwd));
            if (cur_fwd == NULL)
            {
               log_error(LOG_LEVEL_FATAL, "can't allocate memory for configuration");
               /* Never get here - LOG_LEVEL_FATAL causes program exit */
               break;
            }

            cur_fwd->type = SOCKS_NONE;

            /* Save the URL pattern */
            if (create_url_spec(cur_fwd->url, vec[0]))
            {
               log_error(LOG_LEVEL_ERROR, "Bad URL specifier for forward "
                     "directive in configuration file.");
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Bad URL specifier for "
                  "forward directive in configuration file.");
               break;
            }

            /* Parse the parent HTTP proxy host:port */
            p = vec[1];

            if (strcmp(p, ".") != 0)
            {
               cur_fwd->forward_host = strdup(p);

               if (NULL != (p = strchr(cur_fwd->forward_host, ':')))
               {
                  *p++ = '\0';
                  cur_fwd->forward_port = atoi(p);
               }

               if (cur_fwd->forward_port <= 0)
               {
                  cur_fwd->forward_port = 8000;
               }
            }

            /* Add to list. */
            cur_fwd->next = config->forward;
            config->forward = cur_fwd;

            break;

/* *************************************************************************
 * forward-socks4 url-pattern socks-proxy[:port] (.|http-proxy[:port])
 * *************************************************************************/
         case hash_forward_socks4:
            strlcpy(tmp, arg, sizeof(tmp));
            vec_count = ssplit(tmp, " \t", vec, SZ(vec), 1, 1);

            if (vec_count != 3)
            {
               log_error(LOG_LEVEL_ERROR, "Wrong number of parameters for "
                     "forward-socks4 directive in configuration file.");
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Wrong number of parameters for "
                  "forward-socks4 directive in configuration file.");
               break;
            }

            /* allocate a new node */
            cur_fwd = zalloc(sizeof(*cur_fwd));
            if (cur_fwd == NULL)
            {
               log_error(LOG_LEVEL_FATAL, "can't allocate memory for configuration");
               /* Never get here - LOG_LEVEL_FATAL causes program exit */
               break;
            }

            cur_fwd->type = SOCKS_4;

            /* Save the URL pattern */
            if (create_url_spec(cur_fwd->url, vec[0]))
            {
               log_error(LOG_LEVEL_ERROR, "Bad URL specifier for forward-socks4 "
                     "directive in configuration file.");
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Bad URL specifier for "
                  "forward-socks4 directive in configuration file.");
               break;
            }

            /* Parse the SOCKS proxy host[:port] */
            p = vec[1];

            if (strcmp(p, ".") != 0)
            {
               cur_fwd->gateway_host = strdup(p);

               if (NULL != (p = strchr(cur_fwd->gateway_host, ':')))
               {
                  *p++ = '\0';
                  cur_fwd->gateway_port = atoi(p);
               }
               if (cur_fwd->gateway_port <= 0)
               {
                  cur_fwd->gateway_port = 1080;
               }
            }

            /* Parse the parent HTTP proxy host[:port] */
            p = vec[2];

            if (strcmp(p, ".") != 0)
            {
               cur_fwd->forward_host = strdup(p);

               if (NULL != (p = strchr(cur_fwd->forward_host, ':')))
               {
                  *p++ = '\0';
                  cur_fwd->forward_port = atoi(p);
               }

               if (cur_fwd->forward_port <= 0)
               {
                  cur_fwd->forward_port = 8000;
               }
            }

            /* Add to list. */
            cur_fwd->next = config->forward;
            config->forward = cur_fwd;

            break;

/* *************************************************************************
 * forward-socks4a url-pattern socks-proxy[:port] (.|http-proxy[:port])
 * *************************************************************************/
         case hash_forward_socks4a:
         case hash_forward_socks5:
            strlcpy(tmp, arg, sizeof(tmp));
            vec_count = ssplit(tmp, " \t", vec, SZ(vec), 1, 1);

            if (vec_count != 3)
            {
               log_error(LOG_LEVEL_ERROR, "Wrong number of parameters for "
                     "forward-socks4a directive in configuration file.");
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Wrong number of parameters for "
                  "forward-socks4a directive in configuration file.");
               break;
            }

            /* allocate a new node */
            cur_fwd = zalloc(sizeof(*cur_fwd));
            if (cur_fwd == NULL)
            {
               log_error(LOG_LEVEL_FATAL, "can't allocate memory for configuration");
               /* Never get here - LOG_LEVEL_FATAL causes program exit */
               break;
            }

            if (directive_hash == hash_forward_socks4a)
            {
               cur_fwd->type = SOCKS_4A;
            }
            else
            {
               cur_fwd->type = SOCKS_5;
            }

            /* Save the URL pattern */
            if (create_url_spec(cur_fwd->url, vec[0]))
            {
               log_error(LOG_LEVEL_ERROR, "Bad URL specifier for forward-socks4a "
                     "directive in configuration file.");
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Bad URL specifier for "
                  "forward-socks4a directive in configuration file.");
               break;
            }

            /* Parse the SOCKS proxy host[:port] */
            p = vec[1];

            cur_fwd->gateway_host = strdup(p);

            if (NULL != (p = strchr(cur_fwd->gateway_host, ':')))
            {
               *p++ = '\0';
               cur_fwd->gateway_port = atoi(p);
            }
            if (cur_fwd->gateway_port <= 0)
            {
               cur_fwd->gateway_port = 1080;
            }

            /* Parse the parent HTTP proxy host[:port] */
            p = vec[2];

            if (strcmp(p, ".") != 0)
            {
               cur_fwd->forward_host = strdup(p);

               if (NULL != (p = strchr(cur_fwd->forward_host, ':')))
               {
                  *p++ = '\0';
                  cur_fwd->forward_port = atoi(p);
               }

               if (cur_fwd->forward_port <= 0)
               {
                  cur_fwd->forward_port = 8000;
               }
            }

            /* Add to list. */
            cur_fwd->next = config->forward;
            config->forward = cur_fwd;

            break;

/* *************************************************************************
 * forwarded-connect-retries n
 * *************************************************************************/
         case hash_forwarded_connect_retries :
            config->forwarded_connect_retries = atoi(arg);
            break;

/* *************************************************************************
 * hostname hostname-to-show-on-cgi-pages
 * *************************************************************************/
         case hash_hostname :
            freez(config->hostname);
            config->hostname = strdup(arg);
            if (NULL == config->hostname)
            {
               log_error(LOG_LEVEL_FATAL, "Out of memory saving hostname.");
            }
            break;

/* *************************************************************************
 * keep-alive-timeout timeout
 * *************************************************************************/
#ifdef FEATURE_CONNECTION_KEEP_ALIVE
         case hash_keep_alive_timeout :
            if (*arg != '\0')
            {
               int timeout = atoi(arg);
               if (0 <= timeout)
               {
                  config->feature_flags |= RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE;
                  keep_alive_timeout = timeout;
               }
               else
               {
                  config->feature_flags &= ~RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE;
               }
            }
            break;
#endif

/* *************************************************************************
 * listen-address [ip][:port]
 * *************************************************************************/
         case hash_listen_address :
            freez(config->haddr);
            config->haddr = strdup(arg);
            break;

/* *************************************************************************
 * logdir directory-name
 * *************************************************************************/
         case hash_logdir :
            freez(config->logdir);
            config->logdir = make_path(NULL, arg);
            break;

/* *************************************************************************
 * logfile log-file-name
 * In logdir by default
 * *************************************************************************/
         case hash_logfile :
            if (!no_daemon)
            {
               logfile = make_path(config->logdir, arg);
               if (NULL == logfile)
               {
                  log_error(LOG_LEVEL_FATAL, "Out of memory while creating logfile path");
               }
            }
            break;

/* *************************************************************************
 * permit-access source-ip[/significant-bits] [dest-ip[/significant-bits]]
 * *************************************************************************/
#ifdef FEATURE_ACL
         case hash_permit_access:
            strlcpy(tmp, arg, sizeof(tmp));
            vec_count = ssplit(tmp, " \t", vec, SZ(vec), 1, 1);

            if ((vec_count != 1) && (vec_count != 2))
            {
               log_error(LOG_LEVEL_ERROR, "Wrong number of parameters for "
                     "permit-access directive in configuration file.");
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Wrong number of parameters for "
                  "permit-access directive in configuration file.<br><br>\n");

               break;
            }

            /* allocate a new node */
            cur_acl = (struct access_control_list *) zalloc(sizeof(*cur_acl));

            if (cur_acl == NULL)
            {
               log_error(LOG_LEVEL_FATAL, "can't allocate memory for configuration");
               /* Never get here - LOG_LEVEL_FATAL causes program exit */
               break;
            }
            cur_acl->action = ACL_PERMIT;

            if (acl_addr(vec[0], cur_acl->src) < 0)
            {
               log_error(LOG_LEVEL_ERROR, "Invalid source address, port or netmask "
                  "for permit-access directive in configuration file: \"%s\"", vec[0]);
               string_append(&config->proxy_args,
                  "<br>\nWARNING: Invalid source address, port or netmask for "
                  "permit-access directive in configuration file: \"");
               string_append(&config->proxy_args,
                  vec[0]);
               string_append(&config->proxy_args,
                  "\"<br><br>\n");
               freez(cur_acl);
               break;
            }
            if (vec_count == 2)
            {
               if (acl_addr(vec[1], cur_acl->dst) < 0)
               {
                  log_error(LOG_LEVEL_ERROR, "Invalid destination address, port or netmask "
                     "for permit-access directive in configuration file: \"%s\"", vec[1]);
                  string_append(&config->proxy_args,
                     "<br>\nWARNING: Invalid destination address, port or netmask for "
                     "permit-access directive in configuration file: \"");
                  string_append(&config->proxy_args,
                     vec[1]);
                  string_append(&config->proxy_args,
                     "\"<br><br>\n");
                  freez(cur_acl);
                  break;
               }
            }

            /*
             * Add it to the list.  Note we reverse the list to get the
             * behaviour the user expects.  With both the ACL and
             * actions file, the last match wins.  However, the internal
             * implementations are different:  The actions file is stored
             * in the same order as the file, and scanned completely.
             * With the ACL, we reverse the order as we load it, then
             * when we scan it we stop as soon as we get a match.
             */
            cur_acl->next  = config->acl;
            config->acl = cur_acl;

            break;
#endif /* def FEATURE_ACL */

/* *************************************************************************
 * proxy-info-url url
 * *************************************************************************/
         case hash_proxy_info_url :
            freez(config->proxy_info_url);
            config->proxy_info_url = strdup(arg);
            break;

/* *************************************************************************
 * single-threaded
 * *************************************************************************/
         case hash_single_threaded :
            config->multi_threaded = 0;
            break;

/* *************************************************************************
 * socket-timeout numer_of_seconds
 * *************************************************************************/
         case hash_socket_timeout :
            if (*arg != '\0')
            {
               int socket_timeout = atoi(arg);
               if (0 < socket_timeout)
               {
                  config->socket_timeout = socket_timeout;
               }
               else
               {
                  log_error(LOG_LEVEL_FATAL,
                     "Invalid socket-timeout: '%s'", arg);
               }
            }
            break;

/* *************************************************************************
 * split-large-cgi-forms
 * *************************************************************************/
         case hash_split_large_cgi_forms :
            if ((*arg != '\0') && (0 != atoi(arg)))
            {
               config->feature_flags |= RUNTIME_FEATURE_SPLIT_LARGE_FORMS;
            }
            else
            {
               config->feature_flags &= ~RUNTIME_FEATURE_SPLIT_LARGE_FORMS;
            }
            break;

/* *************************************************************************
 * templdir directory-name
 * *************************************************************************/
         case hash_templdir :
            freez(config->templdir);
            config->templdir = make_path(NULL, arg);
            break;

/* *************************************************************************
 * toggle (0|1)
 * *************************************************************************/
#ifdef FEATURE_TOGGLE
         case hash_toggle :
            global_toggle_state = atoi(arg);
            break;
#endif /* def FEATURE_TOGGLE */

/* *************************************************************************
 * trust-info-url url
 * *************************************************************************/
#ifdef FEATURE_TRUST
         case hash_trust_info_url :
            enlist(config->trust_info, arg);
            break;
#endif /* def FEATURE_TRUST */

/* *************************************************************************
 * trustfile filename
 * (In confdir by default.)
 * *************************************************************************/
#ifdef FEATURE_TRUST
         case hash_trustfile :
            freez(config->trustfile);
            config->trustfile = make_path(config->confdir, arg);
            break;
#endif /* def FEATURE_TRUST */

/* *************************************************************************
 * usermanual url
 * *************************************************************************/
         case hash_usermanual :
            /*
             * XXX: If this isn't the first config directive, the
             * show-status page links to the website documentation
             * for the directives that were already parsed. Lame.
             */
            freez(config->usermanual);
            config->usermanual = strdup(arg);
            break;

/* *************************************************************************
 * Win32 Console options:
 * *************************************************************************/

/* *************************************************************************
 * hide-console
 * *************************************************************************/
#ifdef _WIN_CONSOLE
         case hash_hide_console :
            hideConsole = 1;
            break;
#endif /*def _WIN_CONSOLE*/


/* *************************************************************************
 * Win32 GUI options:
 * *************************************************************************/

#if defined(_WIN32) && ! defined(_WIN_CONSOLE)
/* *************************************************************************
 * activity-animation (0|1)
 * *************************************************************************/
         case hash_activity_animation :
            g_bShowActivityAnimation = atoi(arg);
            break;

/* *************************************************************************
 *  close-button-minimizes (0|1)
 * *************************************************************************/
         case hash_close_button_minimizes :
            g_bCloseHidesWindow = atoi(arg);
            break;

/* *************************************************************************
 * log-buffer-size (0|1)
 * *************************************************************************/
         case hash_log_buffer_size :
            g_bLimitBufferSize = atoi(arg);
            break;

/* *************************************************************************
 * log-font-name fontname
 * *************************************************************************/
         case hash_log_font_name :
            if (strlcpy(g_szFontFaceName, arg,
                   sizeof(g_szFontFaceName)) >= sizeof(g_szFontFaceName))
            {
               log_error(LOG_LEVEL_FATAL,
                  "log-font-name argument '%s' is longer than %u characters.",
                  arg, sizeof(g_szFontFaceName)-1);
            }
            break;

/* *************************************************************************
 * log-font-size n
 * *************************************************************************/
         case hash_log_font_size :
            g_nFontSize = atoi(arg);
            break;

/* *************************************************************************
 * log-highlight-messages (0|1)
 * *************************************************************************/
         case hash_log_highlight_messages :
            g_bHighlightMessages = atoi(arg);
            break;

/* *************************************************************************
 * log-max-lines n
 * *************************************************************************/
         case hash_log_max_lines :
            g_nMaxBufferLines = atoi(arg);
            break;

/* *************************************************************************
 * log-messages (0|1)
 * *************************************************************************/
         case hash_log_messages :
            g_bLogMessages = atoi(arg);
            break;

/* *************************************************************************
 * show-on-task-bar (0|1)
 * *************************************************************************/
         case hash_show_on_task_bar :
            g_bShowOnTaskBar = atoi(arg);
            break;

#endif /* defined(_WIN32) && ! defined(_WIN_CONSOLE) */


/* *************************************************************************
 * Warnings about unsupported features
 * *************************************************************************/
#ifndef FEATURE_ACL
         case hash_deny_access:
#endif /* ndef FEATURE_ACL */
#ifndef FEATURE_CGI_EDIT_ACTIONS
         case hash_enable_edit_actions:
#endif /* ndef FEATURE_CGI_EDIT_ACTIONS */
#ifndef FEATURE_TOGGLE
         case hash_enable_remote_toggle:
#endif /* ndef FEATURE_TOGGLE */
#ifndef FEATURE_ACL
         case hash_permit_access:
#endif /* ndef FEATURE_ACL */
#ifndef FEATURE_TOGGLE
         case hash_toggle :
#endif /* ndef FEATURE_TOGGLE */
#ifndef FEATURE_TRUST
         case hash_trustfile :
         case hash_trust_info_url :
#endif /* ndef FEATURE_TRUST */

#ifndef _WIN_CONSOLE
         case hash_hide_console :
#endif /* ndef _WIN_CONSOLE */

#if defined(_WIN_CONSOLE) || ! defined(_WIN32)
         case hash_activity_animation :
         case hash_close_button_minimizes :
         case hash_log_buffer_size :
         case hash_log_font_name :
         case hash_log_font_size :
         case hash_log_highlight_messages :
         case hash_log_max_lines :
         case hash_log_messages :
         case hash_show_on_task_bar :
#endif /* defined(_WIN_CONSOLE) || ! defined(_WIN32) */
            /* These warnings are annoying - so hide them. -- Jon */
            /* log_error(LOG_LEVEL_INFO, "Unsupported directive \"%s\" ignored.", cmd); */
            break;

/* *************************************************************************/
         default :
/* *************************************************************************/
            /*
             * I decided that I liked this better as a warning than an
             * error.  To change back to an error, just change log level
             * to LOG_LEVEL_FATAL.
             */
            log_error(LOG_LEVEL_ERROR, "Ignoring unrecognized directive '%s' (%luul) in line %lu "
                  "in configuration file (%s).",  buf, directive_hash, linenum, configfile);
            string_append(&config->proxy_args,
               " <strong class='warning'>Warning: Ignoring unrecognized directive:</strong>");
            break;

/* *************************************************************************/
      } /* end switch( hash_string(cmd) ) */

      /* Save the argument for the show-status page. */
      savearg(cmd, arg, config);

   } /* end while ( read_config_line(...) ) */

   fclose(configfp);

   set_debug_level(config->debug);

   freez(config->logfile);

   if (!no_daemon)
   {
      if (NULL != logfile)
      {
         config->logfile = logfile;
         init_error_log(Argv[0], config->logfile);
      }
      else
      {
         disable_logging();
      }
   }

#ifdef FEATURE_CONNECTION_KEEP_ALIVE
   if (config->feature_flags & RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE)
   {
      if (config->multi_threaded)
      {
         set_keep_alive_timeout(keep_alive_timeout);
      }
      else
      {
         /*
          * While we could use keep-alive without multiple threads
          * if we didn't bother with enforcing the connection timeout,
          * that might make Tor users sad, even though they shouldn't
          * enable the single-threaded option anyway.
          */
         config->feature_flags &= ~RUNTIME_FEATURE_CONNECTION_KEEP_ALIVE;
         log_error(LOG_LEVEL_ERROR,
            "Config option single-threaded disables connection keep-alive.");
      }
   }
#endif

   if (NULL == config->proxy_args)
   {
      log_error(LOG_LEVEL_FATAL, "Out of memory loading config - insufficient memory for config->proxy_args");
   }

   if (config->actions_file[0])
   {
      add_loader(load_action_files, config);
   }

   if (config->re_filterfile[0])
   {
      add_loader(load_re_filterfiles, config);
   }

#ifdef FEATURE_TRUST
   if (config->trustfile)
   {
      add_loader(load_trustfile, config);
   }
#endif /* def FEATURE_TRUST */

   if ( NULL == config->haddr )
   {
      config->haddr = strdup( HADDR_DEFAULT );
   }

   if ( NULL != config->haddr )
   {
      if (NULL != (p = strchr(config->haddr, ':')))
      {
         *p++ = '\0';
         if (*p)
         {
            config->hport = atoi(p);
         }
      }

      if (config->hport <= 0)
      {
         *--p = ':';
         log_error(LOG_LEVEL_FATAL, "invalid bind port spec %s", config->haddr);
         /* Never get here - LOG_LEVEL_FATAL causes program exit */
      }
      if (*config->haddr == '\0')
      {
         /*
          * Only the port specified. We stored it in config->hport
          * and don't need its text representation anymore.
          */
         freez(config->haddr);
      }
   }

   /*
    * Want to run all the loaders once now.
    *
    * Need to set up a fake csp, so they can get to the config.
    */
   fake_csp = (struct client_state *) zalloc (sizeof(*fake_csp));
   fake_csp->config = config;

   if (run_loader(fake_csp))
   {
      freez(fake_csp);
      log_error(LOG_LEVEL_FATAL, "A loader failed while loading config file. Exiting.");
      /* Never get here - LOG_LEVEL_FATAL causes program exit */
   }
   freez(fake_csp);

/* FIXME: this is a kludge for win32 */
#if defined(_WIN32) && !defined (_WIN_CONSOLE)

   g_default_actions_file = config->actions_file[1]; /* FIXME Hope this is default.action */
   g_user_actions_file = config->actions_file[2]; /* FIXME Hope this is user.action */
   g_re_filterfile    = config->re_filterfile[0]; /* FIXME Hope this is default.filter */

#ifdef FEATURE_TRUST
   g_trustfile        = config->trustfile;
#endif /* def FEATURE_TRUST */


#endif /* defined(_WIN32) && !defined (_WIN_CONSOLE) */
/* FIXME: end kludge */


   config->need_bind = 1;

   if (current_configfile)
   {
      struct configuration_spec * oldcfg = (struct configuration_spec *)
                                           current_configfile->f;
      /*
       * Check if config->haddr,hport == oldcfg->haddr,hport
       *
       * The following could be written more compactly as a single,
       * (unreadably long) if statement.
       */
      config->need_bind = 0;
      if (config->hport != oldcfg->hport)
      {
         config->need_bind = 1;
      }
      else if (config->haddr == NULL)
      {
         if (oldcfg->haddr != NULL)
         {
            config->need_bind = 1;
         }
      }
      else if (oldcfg->haddr == NULL)
      {
         config->need_bind = 1;
      }
      else if (0 != strcmp(config->haddr, oldcfg->haddr))
      {
         config->need_bind = 1;
      }

      current_configfile->unloader = unload_configfile;
   }

   fs->next = files->next;
   files->next = fs;

   current_configfile = fs;

   return (config);
}


/*********************************************************************
 *
 * Function    :  savearg
 *
 * Description :  Called from `load_config'.  It saves each non-empty
 *                and non-comment line from config into
 *                config->proxy_args.  This is used to create the
 *                show-proxy-args page.  On error, frees
 *                config->proxy_args and sets it to NULL
 *
 * Parameters  :
 *          1  :  command = config setting that was found
 *          2  :  argument = the setting's argument (if any)
 *          3  :  config = Configuration to save into.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void savearg(char *command, char *argument, struct configuration_spec * config)
{
   char * buf;
   char * s;

   assert(command);
   assert(argument);

   /*
    * Add config option name embedded in
    * link to its section in the user-manual
    */
   buf = strdup("\n<a href=\"");
   if (!strncmpic(config->usermanual, "file://", 7) ||
       !strncmpic(config->usermanual, "http", 4))
   {
      string_append(&buf, config->usermanual);
   }
   else
   {
      string_append(&buf, "http://" CGI_SITE_2_HOST "/user-manual/");
   }
   string_append(&buf, CONFIG_HELP_PREFIX);
   string_join  (&buf, string_toupper(command));
   string_append(&buf, "\">");
   string_append(&buf, command);
   string_append(&buf, "</a> ");

   if (NULL == buf)
   {
      freez(config->proxy_args);
      return;
   }

   if ( (NULL != argument) && ('\0' != *argument) )
   {
      s = html_encode(argument);
      if (NULL == s)
      {
         freez(buf);
         freez(config->proxy_args);
         return;
      }

      if (strncmpic(argument, "http://", 7) == 0)
      {
         string_append(&buf, "<a href=\"");
         string_append(&buf, s);
         string_append(&buf, "\">");
         string_join  (&buf, s);
         string_append(&buf, "</a>");
      }
      else
      {
         string_join  (&buf, s);
      }
   }

   string_append(&buf, "<br>");
   string_join(&config->proxy_args, buf);
}


/*
  Local Variables:
  tab-width: 3
  end:
*/
