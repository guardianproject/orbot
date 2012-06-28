#ifndef JCC_H_INCLUDED
#define JCC_H_INCLUDED
#define JCC_H_VERSION "$Id: jcc.h,v 1.32 2011/11/06 11:48:23 fabiankeil Exp $"
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

extern struct client_states clients[1];
extern struct file_list    files[1];

#ifdef unix
extern const char *pidfile;
#endif
extern int daemon_mode;

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
int real_main(int argc, char **argv);
#else
int main(int argc, char **argv);
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
