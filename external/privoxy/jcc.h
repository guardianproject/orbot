#ifndef JCC_H_INCLUDED
#define JCC_H_INCLUDED
#define JCC_H_VERSION "$Id: jcc.h,v 1.25 2008/10/09 18:21:41 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/jcc.h,v $
 *
 * Purpose     :  Main file.  Contains main() method, main loop, and 
 *                the main connection-handling function.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2006 the SourceForge
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
 *    $Log: jcc.h,v $
 *    Revision 1.25  2008/10/09 18:21:41  fabiankeil
 *    Flush work-in-progress changes to keep outgoing connections
 *    alive where possible. Incomplete and mostly #ifdef'd out.
 *
 *    Revision 1.24  2008/09/07 12:35:05  fabiankeil
 *    Add mutex lock support for _WIN32.
 *
 *    Revision 1.23  2008/09/04 08:13:58  fabiankeil
 *    Prepare for critical sections on Windows by adding a
 *    layer of indirection before the pthread mutex functions.
 *
 *    Revision 1.22  2007/06/01 18:16:36  fabiankeil
 *    Use the same mutex for gethostbyname() and gethostbyaddr() to prevent
 *    deadlocks and crashes on OpenBSD and possibly other OS with neither
 *    gethostbyname_r() nor gethostaddr_r(). Closes BR#1729174.
 *    Thanks to Ralf Horstmann for report and solution.
 *
 *    Revision 1.21  2007/04/22 13:18:06  fabiankeil
 *    Keep the HTTP snippets local.
 *
 *    Revision 1.20  2006/12/26 17:31:41  fabiankeil
 *    Mutex protect rand() if POSIX threading
 *    is used, warn the user if that's not possible
 *    and stop using it on _WIN32 where it could
 *    cause crashes.
 *
 *    Revision 1.19  2006/12/06 19:41:39  fabiankeil
 *    Privoxy is now able to run as intercepting
 *    proxy in combination with any packet filter
 *    that does the port redirection. The destination
 *    is extracted from the "Host:" header which
 *    should be available for nearly all requests.
 *
 *    Moved HTTP snipplets into jcc.c.
 *    Added error message for gopher proxy requests.
 *
 *    Revision 1.18  2006/11/13 19:05:51  fabiankeil
 *    Make pthread mutex locking more generic. Instead of
 *    checking for OSX and OpenBSD, check for FEATURE_PTHREAD
 *    and use mutex locking unless there is an _r function
 *    available. Better safe than sorry.
 *
 *    Fixes "./configure --disable-pthread" and should result
 *    in less threading-related problems on pthread-using platforms,
 *    but it still doesn't fix BR#1122404.
 *
 *    Revision 1.17  2006/11/06 19:58:23  fabiankeil
 *    Move pthread.h inclusion from jcc.c to jcc.h.
 *    Fixes build on x86-freebsd1 (FreeBSD 5.4-RELEASE).
 *
 *    Revision 1.16  2006/09/02 15:36:42  fabiankeil
 *    Follow the OpenBSD port's lead and protect the resolve
 *    functions on OpenBSD as well.
 *
 *    Revision 1.15  2006/09/02 10:24:30  fabiankeil
 *    Include pthread.h for OpenBSD to make Privoxy build again.
 *
 *    Tested shortly on OpenBSD 3.9 without problems, but the OpenBSD
 *    port has additional patches to use the mutexes OSX_DARWIN needs,
 *    and it should be investigated if they are still required for
 *    reliable operation.
 *
 *    Revision 1.14  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.12.2.3  2006/01/21 16:16:08  david__schmidt
 *    Thanks to  Edward Carrel for his patch to modernize OSX'spthreads support.  See bug #1409623.
 *
 *    Revision 1.12.2.2  2005/04/03 20:10:50  david__schmidt
 *    Thanks to Jindrich Makovicka for a race condition fix for the log
 *    file.  The race condition remains for non-pthread implementations.
 *    Reference patch #1175720.
 *
 *    Revision 1.12.2.1  2003/03/07 03:41:05  david__schmidt
 *    Wrapping all *_r functions (the non-_r versions of them) with mutex 
 *    semaphores for OSX.  Hopefully this will take care of all of those pesky
 *    crash reports.
 *
 *    Revision 1.12  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.11  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.10  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.9  2002/03/07 03:52:44  oes
 *    Set logging to tty for --no-daemon mode
 *
 *    Revision 1.8  2002/03/04 18:19:49  oes
 *    Added extern const char *pidfile
 *
 *    Revision 1.7  2001/11/05 21:41:43  steudten
 *    Add changes to be a real daemon just for unix os.
 *    (change cwd to /, detach from controlling tty, set
 *    process group and session leader to the own process.
 *    Add DBG() Macro.
 *    Add some fatal-error log message for failed malloc().
 *    Add '-d' if compiled with 'configure --with-debug' to
 *    enable debug output.
 *
 *    Revision 1.6  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.5  2001/07/29 19:32:00  jongfoster
 *    Renaming _main() [mingw32 only] to real_main(), for ANSI compliance.
 *
 *    Revision 1.4  2001/07/29 18:58:15  jongfoster
 *    Removing nested #includes, adding forward declarations for needed
 *    structures, and changing the #define _FILENAME_H to FILENAME_H_INCLUDED.
 *
 *    Revision 1.3  2001/07/18 12:31:58  oes
 *    moved #define freez from jcc.h to project.h
 *
 *    Revision 1.2  2001/05/31 21:24:47  jongfoster
 *    Changed "permission" to "action" throughout.
 *    Removed DEFAULT_USER_AGENT - it must now be specified manually.
 *    Moved vanilla wafer check into chat(), since we must now
 *    decide whether or not to add it based on the URL.
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:56  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

struct client_state;
struct file_list;

/* Global variables */

#ifdef FEATURE_STATISTICS
extern int urls_read;
extern int urls_rejected;
#endif /*def FEATURE_STATISTICS*/

extern struct client_state clients[1];
extern struct file_list    files[1];

#ifdef unix
extern const char *pidfile;
#endif
extern int no_daemon;

#ifdef FEATURE_GRACEFUL_TERMINATION
extern int g_terminate;
#endif

#if defined(FEATURE_PTHREAD) || defined(_WIN32)
#define MUTEX_LOCKS_AVAILABLE

#ifdef FEATURE_PTHREAD
#include <pthread.h>

typedef pthread_mutex_t privoxy_mutex_t;

#else

typedef CRITICAL_SECTION privoxy_mutex_t;

#endif

extern void privoxy_mutex_lock(privoxy_mutex_t *mutex);
extern void privoxy_mutex_unlock(privoxy_mutex_t *mutex);

extern privoxy_mutex_t log_mutex;
extern privoxy_mutex_t log_init_mutex;
extern privoxy_mutex_t connection_reuse_mutex;

#ifndef HAVE_GMTIME_R
extern privoxy_mutex_t gmtime_mutex;
#endif /* ndef HAVE_GMTIME_R */

#ifndef HAVE_LOCALTIME_R
extern privoxy_mutex_t localtime_mutex;
#endif /* ndef HAVE_GMTIME_R */

#if !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_GETHOSTBYNAME_R)
extern privoxy_mutex_t resolver_mutex;
#endif /* !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_GETHOSTBYNAME_R) */

#ifndef HAVE_RANDOM
extern privoxy_mutex_t rand_mutex;
#endif /* ndef HAVE_RANDOM */

#endif /* FEATURE_PTHREAD */

/* Functions */

#ifdef __MINGW32__
int real_main(int argc, const char *argv[]);
#else
int main(int argc, const char *argv[]);
#endif

/* Revision control strings from this header and associated .c file */
extern const char jcc_rcs[];
extern const char jcc_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef JCC_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
