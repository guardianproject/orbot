#ifndef MISCUTIL_H_INCLUDED
#define MISCUTIL_H_INCLUDED
#define MISCUTIL_H_VERSION "$Id: miscutil.h,v 1.34 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/miscutil.h,v $
 *
 * Purpose     :  zalloc, hash_string, strcmpic, strncmpic, and
 *                MinGW32 strdup functions.  These are each too small
 *                to deserve their own file but don't really fit in
 *                any other file.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2011 the
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


#include "project.h"

#if defined(__cplusplus)
extern "C" {
#endif

extern const char *basedir;
extern void *zalloc(size_t size);

#if defined(unix)
extern void write_pid_file(void);
#endif /* unix */

extern unsigned int hash_string(const char* s);

extern int strcmpic(const char *s1, const char *s2);
extern int strncmpic(const char *s1, const char *s2, size_t n);

extern jb_err string_append(char **target_string, const char *text_to_append);
extern jb_err string_join  (char **target_string,       char *text_to_append);

extern char *string_toupper(const char *string);
extern char *chomp(char *string);
extern char *bindup(const char *string, size_t len);

extern char *make_path(const char * dir, const char * file);

long int pick_from_range(long int range);

#ifndef HAVE_SNPRINTF
extern int snprintf(char *, size_t, const char *, /*args*/ ...);
#endif /* ndef HAVE_SNPRINTF */

#if !defined(HAVE_TIMEGM) && defined(HAVE_TZSET) && defined(HAVE_PUTENV)
time_t timegm(struct tm *tm);
#endif /* !defined(HAVE_TIMEGM) && defined(HAVE_TZSET) && defined(HAVE_PUTENV) */

/* Here's looking at you, Ulrich. */
#if !defined(HAVE_STRLCPY)
size_t privoxy_strlcpy(char *destination, const char *source, size_t size);
#define strlcpy privoxy_strlcpy
#define USE_PRIVOXY_STRLCPY 1
#define HAVE_STRLCPY 1
#endif /* ndef HAVE_STRLCPY*/

#ifndef HAVE_STRLCAT
size_t privoxy_strlcat(char *destination, const char *source, size_t size);
#define strlcat privoxy_strlcat
#endif /* ndef HAVE_STRLCAT */

/* Revision control strings from this header and associated .c file */
extern const char miscutil_rcs[];
extern const char miscutil_h_rcs[];

#if defined(__cplusplus)
}
#endif

#endif /* ndef MISCUTIL_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
