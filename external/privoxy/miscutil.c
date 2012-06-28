const char miscutil_rcs[] = "$Id: miscutil.c,v 1.68 2011/09/04 11:10:56 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/miscutil.c,v $
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
 *                The timegm replacement function was taken from GnuPG,
 *                Copyright (C) 2004 Free Software Foundation, Inc.
 *
 *                The snprintf replacement function is written by
 *                Mark Martinec who also holds the copyright. It can be
 *                used under the terms of the GPL or the terms of the
 *                "Frontier Artistic License".
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


#include "config.h"

#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#if !defined(_WIN32) && !defined(__OS2__)
#include <unistd.h>
#endif /* #if !defined(_WIN32) && !defined(__OS2__) */
#include <string.h>
#include <ctype.h>
#include <assert.h>

#if !defined(HAVE_TIMEGM) && defined(HAVE_TZSET) && defined(HAVE_PUTENV)
#include <time.h>
#endif /* !defined(HAVE_TIMEGM) && defined(HAVE_TZSET) && defined(HAVE_PUTENV) */

#include "project.h"
#include "miscutil.h"
#include "errlog.h"
#include "jcc.h"

const char miscutil_h_rcs[] = MISCUTIL_H_VERSION;

/*********************************************************************
 *
 * Function    :  zalloc
 *
 * Description :  Malloc some memory and set it to '\0'.
 *                The way calloc() ought to be -acjc
 *
 * Parameters  :
 *          1  :  size = Size of memory chunk to return.
 *
 * Returns     :  Pointer to newly malloc'd memory chunk.
 *
 *********************************************************************/
void *zalloc(size_t size)
{
   void * ret;

   if ((ret = (void *)malloc(size)) != NULL)
   {
      memset(ret, 0, size);
   }

   return(ret);

}


#if defined(unix)
/*********************************************************************
 *
 * Function    :  write_pid_file
 *
 * Description :  Writes a pid file with the pid of the main process
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void write_pid_file(void)
{
   FILE   *fp;

   /*
    * If no --pidfile option was given,
    * we can live without one.
    */
   if (pidfile == NULL) return;

   if ((fp = fopen(pidfile, "w")) == NULL)
   {
      log_error(LOG_LEVEL_INFO, "can't open pidfile '%s': %E", pidfile);
   }
   else
   {
      fprintf(fp, "%u\n", (unsigned int) getpid());
      fclose (fp);
   }
   return;

}
#endif /* def unix */


/*********************************************************************
 *
 * Function    :  hash_string
 *
 * Description :  Take a string and compute a (hopefuly) unique numeric
 *                integer value.  This has several uses, but being able
 *                to "switch" a string the one of my favorites.
 *
 * Parameters  :
 *          1  :  s : string to be hashed.
 *
 * Returns     :  an unsigned long variable with the hashed value.
 *
 *********************************************************************/
unsigned int hash_string( const char* s )
{
   unsigned int h = 0;

   for ( ; *s; ++s )
   {
      h = 5 * h + (unsigned int)*s;
   }

   return (h);

}


/*********************************************************************
 *
 * Function    :  strcmpic
 *
 * Description :  Case insensitive string comparison
 *
 * Parameters  :
 *          1  :  s1 = string 1 to compare
 *          2  :  s2 = string 2 to compare
 *
 * Returns     :  0 if s1==s2, Negative if s1<s2, Positive if s1>s2
 *
 *********************************************************************/
int strcmpic(const char *s1, const char *s2)
{
   if (!s1) s1 = "";
   if (!s2) s2 = "";

   while (*s1 && *s2)
   {
      if ( ( *s1 != *s2 ) && ( ijb_tolower(*s1) != ijb_tolower(*s2) ) )
      {
         break;
      }
      s1++, s2++;
   }
   return(ijb_tolower(*s1) - ijb_tolower(*s2));

}


/*********************************************************************
 *
 * Function    :  strncmpic
 *
 * Description :  Case insensitive string comparison (up to n characters)
 *
 * Parameters  :
 *          1  :  s1 = string 1 to compare
 *          2  :  s2 = string 2 to compare
 *          3  :  n = maximum characters to compare
 *
 * Returns     :  0 if s1==s2, Negative if s1<s2, Positive if s1>s2
 *
 *********************************************************************/
int strncmpic(const char *s1, const char *s2, size_t n)
{
   if (n <= (size_t)0) return(0);
   if (!s1) s1 = "";
   if (!s2) s2 = "";

   while (*s1 && *s2)
   {
      if ( ( *s1 != *s2 ) && ( ijb_tolower(*s1) != ijb_tolower(*s2) ) )
      {
         break;
      }

      if (--n <= (size_t)0) break;

      s1++, s2++;
   }
   return(ijb_tolower(*s1) - ijb_tolower(*s2));

}


/*********************************************************************
 *
 * Function    :  chomp
 *
 * Description :  In-situ-eliminate all leading and trailing whitespace
 *                from a string.
 *
 * Parameters  :
 *          1  :  s : string to be chomped.
 *
 * Returns     :  chomped string
 *
 *********************************************************************/
char *chomp(char *string)
{
   char *p, *q, *r;

   /*
    * strip trailing whitespace
    */
   p = string + strlen(string);
   while (p > string && ijb_isspace(*(p-1)))
   {
      p--;
   }
   *p = '\0';

   /*
    * find end of leading whitespace
    */
   q = r = string;
   while (*q && ijb_isspace(*q))
   {
      q++;
   }

   /*
    * if there was any, move the rest forwards
    */
   if (q != string)
   {
      while (q <= p)
      {
         *r++ = *q++;
      }
   }

   return(string);

}


/*********************************************************************
 *
 * Function    :  string_append
 *
 * Description :  Reallocate target_string and append text to it.
 *                This makes it easier to append to malloc'd strings.
 *                This is similar to the (removed) strsav(), but
 *                running out of memory isn't catastrophic.
 *
 *                Programming style:
 *
 *                The following style provides sufficient error
 *                checking for this routine, with minimal clutter
 *                in the source code.  It is recommended if you
 *                have many calls to this function:
 *
 *                char * s = strdup(...); // don't check for error
 *                string_append(&s, ...);  // don't check for error
 *                string_append(&s, ...);  // don't check for error
 *                string_append(&s, ...);  // don't check for error
 *                if (NULL == s) { ... handle error ... }
 *
 *                OR, equivalently:
 *
 *                char * s = strdup(...); // don't check for error
 *                string_append(&s, ...);  // don't check for error
 *                string_append(&s, ...);  // don't check for error
 *                if (string_append(&s, ...)) {... handle error ...}
 *
 * Parameters  :
 *          1  :  target_string = Pointer to old text that is to be
 *                extended.  *target_string will be free()d by this
 *                routine.  target_string must be non-NULL.
 *                If *target_string is NULL, this routine will
 *                do nothing and return with an error - this allows
 *                you to make many calls to this routine and only
 *                check for errors after the last one.
 *          2  :  text_to_append = Text to be appended to old.
 *                Must not be NULL.
 *
 * Returns     :  JB_ERR_OK on success, and sets *target_string
 *                   to newly malloc'ed appended string.  Caller
 *                   must free(*target_string).
 *                JB_ERR_MEMORY on out-of-memory.  (And free()s
 *                   *target_string and sets it to NULL).
 *                JB_ERR_MEMORY if *target_string is NULL.
 *
 *********************************************************************/
jb_err string_append(char **target_string, const char *text_to_append)
{
   size_t old_len;
   char *new_string;
   size_t new_size;

   assert(target_string);
   assert(text_to_append);

   if (*target_string == NULL)
   {
      return JB_ERR_MEMORY;
   }

   if (*text_to_append == '\0')
   {
      return JB_ERR_OK;
   }

   old_len = strlen(*target_string);

   new_size = strlen(text_to_append) + old_len + 1;

   if (NULL == (new_string = realloc(*target_string, new_size)))
   {
      free(*target_string);

      *target_string = NULL;
      return JB_ERR_MEMORY;
   }

   strlcpy(new_string + old_len, text_to_append, new_size - old_len);

   *target_string = new_string;
   return JB_ERR_OK;
}


/*********************************************************************
 *
 * Function    :  string_join
 *
 * Description :  Join two strings together.  Frees BOTH the original
 *                strings.  If either or both input strings are NULL,
 *                fails as if it had run out of memory.
 *
 *                For comparison, string_append requires that the
 *                second string is non-NULL, and doesn't free it.
 *
 *                Rationale: Too often, we want to do
 *                string_append(s, html_encode(s2)).  That assert()s
 *                if s2 is NULL or if html_encode() runs out of memory.
 *                It also leaks memory.  Proper checking is cumbersome.
 *                The solution: string_join(s, html_encode(s2)) is safe,
 *                and will free the memory allocated by html_encode().
 *
 * Parameters  :
 *          1  :  target_string = Pointer to old text that is to be
 *                extended.  *target_string will be free()d by this
 *                routine.  target_string must be non-NULL.
 *          2  :  text_to_append = Text to be appended to old.
 *
 * Returns     :  JB_ERR_OK on success, and sets *target_string
 *                   to newly malloc'ed appended string.  Caller
 *                   must free(*target_string).
 *                JB_ERR_MEMORY on out-of-memory, or if
 *                   *target_string or text_to_append is NULL.  (In
 *                   this case, frees *target_string and text_to_append,
 *                   sets *target_string to NULL).
 *
 *********************************************************************/
jb_err string_join(char **target_string, char *text_to_append)
{
   jb_err err;

   assert(target_string);

   if (text_to_append == NULL)
   {
      freez(*target_string);
      return JB_ERR_MEMORY;
   }

   err = string_append(target_string, text_to_append);

   freez(text_to_append);

   return err;
}


/*********************************************************************
 *
 * Function    :  string_toupper
 *
 * Description :  Produce a copy of string with all convertible
 *                characters converted to uppercase.
 *
 * Parameters  :
 *          1  :  string = string to convert
 *
 * Returns     :  Uppercase copy of string if possible,
 *                NULL on out-of-memory or if string was NULL.
 *
 *********************************************************************/
char *string_toupper(const char *string)
{
   char *result, *p;
   const char *q;

   if (!string || ((result = (char *) zalloc(strlen(string) + 1)) == NULL))
   {
      return NULL;
   }

   q = string;
   p = result;

   while (*q != '\0')
   {
      *p++ = (char)toupper((int) *q++);
   }

   return result;

}


/*********************************************************************
 *
 * Function    :  bindup
 *
 * Description :  Duplicate the first n characters of a string that may
 *                contain '\0' characters.
 *
 * Parameters  :
 *          1  :  string = string to be duplicated
 *          2  :  len = number of bytes to duplicate
 *
 * Returns     :  pointer to copy, or NULL if failiure
 *
 *********************************************************************/
char *bindup(const char *string, size_t len)
{
   char *duplicate;

   if (NULL == (duplicate = (char *)malloc(len)))
   {
      return NULL;
   }
   else
   {
     memcpy(duplicate, string, len);
   }

   return duplicate;

}


/*********************************************************************
 *
 * Function    :  make_path
 *
 * Description :  Takes a directory name and a file name, returns
 *                the complete path.  Handles windows/unix differences.
 *                If the file name is already an absolute path, or if
 *                the directory name is NULL or empty, it returns
 *                the filename.
 *
 * Parameters  :
 *          1  :  dir: Name of directory or NULL for none.
 *          2  :  file: Name of file.  Should not be NULL or empty.
 *
 * Returns     :  "dir/file" (Or on windows, "dir\file").
 *                It allocates the string on the heap.  Caller frees.
 *                Returns NULL in error (i.e. NULL file or out of
 *                memory)
 *
 *********************************************************************/
char * make_path(const char * dir, const char * file)
{
#ifdef AMIGA
   char path[512];

   if(dir)
   {
      if(dir[0] == '.')
      {
         if(dir[1] == '/')
         {
            strncpy(path,dir+2,512);
         }
         else
         {
            strncpy(path,dir+1,512);
         }
      }
      else
      {
         strncpy(path,dir,512);
      }
      path[511]=0;
   }
   else
   {
      path[0]=0;
   }
   if(AddPart(path,file,512))
   {
      return strdup(path);
   }
   else
   {
      return NULL;
   }
#else /* ndef AMIGA */

   if ((file == NULL) || (*file == '\0'))
   {
      return NULL; /* Error */
   }

   if ((dir == NULL) || (*dir == '\0') /* No directory specified */
#if defined(_WIN32) || defined(__OS2__)
      || (*file == '\\') || (file[1] == ':') /* Absolute path (DOS) */
#else /* ifndef _WIN32 || __OS2__ */
      || (*file == '/') /* Absolute path (U*ix) */
#endif /* ifndef _WIN32 || __OS2__  */
      )
   {
      return strdup(file);
   }
   else
   {
      char * path;
      size_t path_size = strlen(dir) + strlen(file) + 2; /* +2 for trailing (back)slash and \0 */

#if defined(unix)
      if ( *dir != '/' && basedir && *basedir )
      {
         /*
          * Relative path, so start with the base directory.
          */
         path_size += strlen(basedir) + 1; /* +1 for the slash */
         path = malloc(path_size);
         if (!path ) log_error(LOG_LEVEL_FATAL, "malloc failed!");
         strlcpy(path, basedir, path_size);
         strlcat(path, "/", path_size);
         strlcat(path, dir, path_size);
      }
      else
#endif /* defined unix */
      {
         path = malloc(path_size);
         if (!path ) log_error(LOG_LEVEL_FATAL, "malloc failed!");
         strlcpy(path, dir, path_size);
      }

      assert(NULL != path);
#if defined(_WIN32) || defined(__OS2__)
      if(path[strlen(path)-1] != '\\')
      {
         strlcat(path, "\\", path_size);
      }
#else /* ifndef _WIN32 || __OS2__ */
      if(path[strlen(path)-1] != '/')
      {
         strlcat(path, "/", path_size);
      }
#endif /* ifndef _WIN32 || __OS2__ */
      strlcat(path, file, path_size);

      return path;
   }
#endif /* ndef AMIGA */
}


/*********************************************************************
 *
 * Function    :  pick_from_range
 *
 * Description :  Pick a positive number out of a given range.
 *                Should only be used if randomness would be nice,
 *                but isn't really necessary.
 *
 * Parameters  :
 *          1  :  range: Highest possible number to pick.
 *
 * Returns     :  Picked number.
 *
 *********************************************************************/
long int pick_from_range(long int range)
{
   long int number;
#ifdef _WIN32
   static unsigned long seed = 0;
#endif /* def _WIN32 */

   assert(range != 0);
   assert(range > 0);

   if (range <= 0) return 0;

#ifdef HAVE_RANDOM
   number = random() % range + 1;
#elif defined(MUTEX_LOCKS_AVAILABLE)
   privoxy_mutex_lock(&rand_mutex);
#ifdef _WIN32
   if (!seed)
   {
      seed = (unsigned long)(GetCurrentThreadId()+GetTickCount());
   }
   srand(seed);
   seed = (unsigned long)((rand() << 16) + rand());
#endif /* def _WIN32 */
   number = (unsigned long)((rand() << 16) + (rand())) % (unsigned long)(range + 1);
   privoxy_mutex_unlock(&rand_mutex);
#else
   /*
    * XXX: Which platforms reach this and are there
    * better options than just using rand() and hoping
    * that it's safe?
    */
   log_error(LOG_LEVEL_INFO, "No thread-safe PRNG available? Header time randomization "
      "might cause crashes, predictable results or even combine these fine options.");
   number = rand() % (long int)(range + 1);

#endif /* (def HAVE_RANDOM) */

   return number;
}


#ifdef USE_PRIVOXY_STRLCPY
/*********************************************************************
 *
 * Function    :  privoxy_strlcpy
 *
 * Description :  strlcpy(3) look-alike for those without decent libc.
 *
 * Parameters  :
 *          1  :  destination: buffer to copy into.
 *          2  :  source: String to copy.
 *          3  :  size: Size of destination buffer.
 *
 * Returns     :  The length of the string that privoxy_strlcpy() tried to create.
 *
 *********************************************************************/
size_t privoxy_strlcpy(char *destination, const char *source, const size_t size)
{
   if (0 < size)
   {
      snprintf(destination, size, "%s", source);
      /*
       * Platforms that lack strlcpy() also tend to have
       * a broken snprintf implementation that doesn't
       * guarantee nul termination.
       *
       * XXX: the configure script should detect and reject those.
       */
      destination[size-1] = '\0';
   }
   return strlen(source);
}
#endif /* def USE_PRIVOXY_STRLCPY */


#ifndef HAVE_STRLCAT
/*********************************************************************
 *
 * Function    :  privoxy_strlcat
 *
 * Description :  strlcat(3) look-alike for those without decent libc.
 *
 * Parameters  :
 *          1  :  destination: C string.
 *          2  :  source: String to copy.
 *          3  :  size: Size of destination buffer.
 *
 * Returns     :  The length of the string that privoxy_strlcat() tried to create.
 *
 *********************************************************************/
size_t privoxy_strlcat(char *destination, const char *source, const size_t size)
{
   const size_t old_length = strlen(destination);
   return old_length + strlcpy(destination + old_length, source, size - old_length);
}
#endif /* ndef HAVE_STRLCAT */


#if !defined(HAVE_TIMEGM) && defined(HAVE_TZSET) && defined(HAVE_PUTENV)
/*********************************************************************
 *
 * Function    :  timegm
 *
 * Description :  libc replacement function for the inverse of gmtime().
 *                Copyright (C) 2004 Free Software Foundation, Inc.
 *
 *                Code originally copied from GnuPG, modifications done
 *                for Privoxy: style changed, #ifdefs for _WIN32 added
 *                to have it work on mingw32.
 *
 *                XXX: It's very unlikely to happen, but if the malloc()
 *                call fails the time zone will be permanently set to UTC.
 *
 * Parameters  :
 *          1  :  tm: Broken-down time struct.
 *
 * Returns     :  tm converted into time_t seconds.
 *
 *********************************************************************/
time_t timegm(struct tm *tm)
{
   time_t answer;
   char *zone;

   zone = getenv("TZ");
   putenv("TZ=UTC");
   tzset();
   answer = mktime(tm);
   if (zone)
   {
      char *old_zone;

      old_zone = malloc(3 + strlen(zone) + 1);
      if (old_zone)
      {
         strcpy(old_zone, "TZ=");
         strcat(old_zone, zone);
         putenv(old_zone);
#ifdef _WIN32
         free(old_zone);
#endif /* def _WIN32 */
      }
   }
   else
   {
#ifdef HAVE_UNSETENV
      unsetenv("TZ");
#elif defined(_WIN32)
      putenv("TZ=");
#else
      putenv("TZ");
#endif
   }
   tzset();

   return answer;
}
#endif /* !defined(HAVE_TIMEGM) && defined(HAVE_TZSET) && defined(HAVE_PUTENV) */


#ifndef HAVE_SNPRINTF
/*
 * What follows is a portable snprintf routine, written by Mark Martinec.
 * See: http://www.ijs.si/software/snprintf/

                                  snprintf.c
                   - a portable implementation of snprintf,
       including vsnprintf.c, asnprintf, vasnprintf, asprintf, vasprintf

   snprintf is a routine to convert numeric and string arguments to
   formatted strings. It is similar to sprintf(3) provided in a system's
   C library, yet it requires an additional argument - the buffer size -
   and it guarantees never to store anything beyond the given buffer,
   regardless of the format or arguments to be formatted. Some newer
   operating systems do provide snprintf in their C library, but many do
   not or do provide an inadequate (slow or idiosyncratic) version, which
   calls for a portable implementation of this routine.

Author

   Mark Martinec <mark.martinec@ijs.si>, April 1999, June 2000
   Copyright © 1999, Mark Martinec

 */

#define PORTABLE_SNPRINTF_VERSION_MAJOR 2
#define PORTABLE_SNPRINTF_VERSION_MINOR 2

#if defined(NEED_ASPRINTF) || defined(NEED_ASNPRINTF) || defined(NEED_VASPRINTF) || defined(NEED_VASNPRINTF)
# if defined(NEED_SNPRINTF_ONLY)
# undef NEED_SNPRINTF_ONLY
# endif
# if !defined(PREFER_PORTABLE_SNPRINTF)
# define PREFER_PORTABLE_SNPRINTF
# endif
#endif

#if defined(SOLARIS_BUG_COMPATIBLE) && !defined(SOLARIS_COMPATIBLE)
#define SOLARIS_COMPATIBLE
#endif

#if defined(HPUX_BUG_COMPATIBLE) && !defined(HPUX_COMPATIBLE)
#define HPUX_COMPATIBLE
#endif

#if defined(DIGITAL_UNIX_BUG_COMPATIBLE) && !defined(DIGITAL_UNIX_COMPATIBLE)
#define DIGITAL_UNIX_COMPATIBLE
#endif

#if defined(PERL_BUG_COMPATIBLE) && !defined(PERL_COMPATIBLE)
#define PERL_COMPATIBLE
#endif

#if defined(LINUX_BUG_COMPATIBLE) && !defined(LINUX_COMPATIBLE)
#define LINUX_COMPATIBLE
#endif

#include <sys/types.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <assert.h>
#include <errno.h>

#ifdef isdigit
#undef isdigit
#endif
#define isdigit(c) ((c) >= '0' && (c) <= '9')

/* For copying strings longer or equal to 'breakeven_point'
 * it is more efficient to call memcpy() than to do it inline.
 * The value depends mostly on the processor architecture,
 * but also on the compiler and its optimization capabilities.
 * The value is not critical, some small value greater than zero
 * will be just fine if you don't care to squeeze every drop
 * of performance out of the code.
 *
 * Small values favor memcpy, large values favor inline code.
 */
#if defined(__alpha__) || defined(__alpha)
#  define breakeven_point   2    /* AXP (DEC Alpha)     - gcc or cc or egcs */
#endif
#if defined(__i386__)  || defined(__i386)
#  define breakeven_point  12    /* Intel Pentium/Linux - gcc 2.96 */
#endif
#if defined(__hppa)
#  define breakeven_point  10    /* HP-PA               - gcc */
#endif
#if defined(__sparc__) || defined(__sparc)
#  define breakeven_point  33    /* Sun Sparc 5         - gcc 2.8.1 */
#endif

/* some other values of possible interest: */
/* #define breakeven_point  8 */ /* VAX 4000          - vaxc */
/* #define breakeven_point 19 */ /* VAX 4000          - gcc 2.7.0 */

#ifndef breakeven_point
#  define breakeven_point   6    /* some reasonable one-size-fits-all value */
#endif

#define fast_memcpy(d,s,n) \
  { register size_t nn = (size_t)(n); \
    if (nn >= breakeven_point) memcpy((d), (s), nn); \
    else if (nn > 0) { /* proc call overhead is worth only for large strings*/\
      register char *dd; register const char *ss; \
      for (ss=(s), dd=(d); nn>0; nn--) *dd++ = *ss++; } }

#define fast_memset(d,c,n) \
  { register size_t nn = (size_t)(n); \
    if (nn >= breakeven_point) memset((d), (int)(c), nn); \
    else if (nn > 0) { /* proc call overhead is worth only for large strings*/\
      register char *dd; register const int cc=(int)(c); \
      for (dd=(d); nn>0; nn--) *dd++ = cc; } }

/* prototypes */

#if defined(NEED_ASPRINTF)
int asprintf   (char **ptr, const char *fmt, /*args*/ ...);
#endif
#if defined(NEED_VASPRINTF)
int vasprintf  (char **ptr, const char *fmt, va_list ap);
#endif
#if defined(NEED_ASNPRINTF)
int asnprintf  (char **ptr, size_t str_m, const char *fmt, /*args*/ ...);
#endif
#if defined(NEED_VASNPRINTF)
int vasnprintf (char **ptr, size_t str_m, const char *fmt, va_list ap);
#endif

#if defined(HAVE_SNPRINTF)
/* declare our portable snprintf  routine under name portable_snprintf  */
/* declare our portable vsnprintf routine under name portable_vsnprintf */
#else
/* declare our portable routines under names snprintf and vsnprintf */
#define portable_snprintf snprintf
#if !defined(NEED_SNPRINTF_ONLY)
#define portable_vsnprintf vsnprintf
#endif
#endif

#if !defined(HAVE_SNPRINTF) || defined(PREFER_PORTABLE_SNPRINTF)
int portable_snprintf(char *str, size_t str_m, const char *fmt, /*args*/ ...);
#if !defined(NEED_SNPRINTF_ONLY)
int portable_vsnprintf(char *str, size_t str_m, const char *fmt, va_list ap);
#endif
#endif

/* declarations */

static char credits[] = "\n\
@(#)snprintf.c, v2.2: Mark Martinec, <mark.martinec@ijs.si>\n\
@(#)snprintf.c, v2.2: Copyright 1999, Mark Martinec. Frontier Artistic License applies.\n\
@(#)snprintf.c, v2.2: http://www.ijs.si/software/snprintf/\n";

#if defined(NEED_ASPRINTF)
int asprintf(char **ptr, const char *fmt, /*args*/ ...) {
  va_list ap;
  size_t str_m;
  int str_l;

  *ptr = NULL;
  va_start(ap, fmt);                            /* measure the required size */
  str_l = portable_vsnprintf(NULL, (size_t)0, fmt, ap);
  va_end(ap);
  assert(str_l >= 0);        /* possible integer overflow if str_m > INT_MAX */
  *ptr = (char *) malloc(str_m = (size_t)str_l + 1);
  if (*ptr == NULL) { errno = ENOMEM; str_l = -1; }
  else {
    int str_l2;
    va_start(ap, fmt);
    str_l2 = portable_vsnprintf(*ptr, str_m, fmt, ap);
    va_end(ap);
    assert(str_l2 == str_l);
  }
  return str_l;
}
#endif

#if defined(NEED_VASPRINTF)
int vasprintf(char **ptr, const char *fmt, va_list ap) {
  size_t str_m;
  int str_l;

  *ptr = NULL;
  { va_list ap2;
    va_copy(ap2, ap);  /* don't consume the original ap, we'll need it again */
    str_l = portable_vsnprintf(NULL, (size_t)0, fmt, ap2);/*get required size*/
    va_end(ap2);
  }
  assert(str_l >= 0);        /* possible integer overflow if str_m > INT_MAX */
  *ptr = (char *) malloc(str_m = (size_t)str_l + 1);
  if (*ptr == NULL) { errno = ENOMEM; str_l = -1; }
  else {
    int str_l2 = portable_vsnprintf(*ptr, str_m, fmt, ap);
    assert(str_l2 == str_l);
  }
  return str_l;
}
#endif

#if defined(NEED_ASNPRINTF)
int asnprintf (char **ptr, size_t str_m, const char *fmt, /*args*/ ...) {
  va_list ap;
  int str_l;

  *ptr = NULL;
  va_start(ap, fmt);                            /* measure the required size */
  str_l = portable_vsnprintf(NULL, (size_t)0, fmt, ap);
  va_end(ap);
  assert(str_l >= 0);        /* possible integer overflow if str_m > INT_MAX */
  if ((size_t)str_l + 1 < str_m) str_m = (size_t)str_l + 1;      /* truncate */
  /* if str_m is 0, no buffer is allocated, just set *ptr to NULL */
  if (str_m == 0) {  /* not interested in resulting string, just return size */
  } else {
    *ptr = (char *) malloc(str_m);
    if (*ptr == NULL) { errno = ENOMEM; str_l = -1; }
    else {
      int str_l2;
      va_start(ap, fmt);
      str_l2 = portable_vsnprintf(*ptr, str_m, fmt, ap);
      va_end(ap);
      assert(str_l2 == str_l);
    }
  }
  return str_l;
}
#endif

#if defined(NEED_VASNPRINTF)
int vasnprintf (char **ptr, size_t str_m, const char *fmt, va_list ap) {
  int str_l;

  *ptr = NULL;
  { va_list ap2;
    va_copy(ap2, ap);  /* don't consume the original ap, we'll need it again */
    str_l = portable_vsnprintf(NULL, (size_t)0, fmt, ap2);/*get required size*/
    va_end(ap2);
  }
  assert(str_l >= 0);        /* possible integer overflow if str_m > INT_MAX */
  if ((size_t)str_l + 1 < str_m) str_m = (size_t)str_l + 1;      /* truncate */
  /* if str_m is 0, no buffer is allocated, just set *ptr to NULL */
  if (str_m == 0) {  /* not interested in resulting string, just return size */
  } else {
    *ptr = (char *) malloc(str_m);
    if (*ptr == NULL) { errno = ENOMEM; str_l = -1; }
    else {
      int str_l2 = portable_vsnprintf(*ptr, str_m, fmt, ap);
      assert(str_l2 == str_l);
    }
  }
  return str_l;
}
#endif

/*
 * If the system does have snprintf and the portable routine is not
 * specifically required, this module produces no code for snprintf/vsnprintf.
 */
#if !defined(HAVE_SNPRINTF) || defined(PREFER_PORTABLE_SNPRINTF)

#if !defined(NEED_SNPRINTF_ONLY)
int portable_snprintf(char *str, size_t str_m, const char *fmt, /*args*/ ...) {
  va_list ap;
  int str_l;

  va_start(ap, fmt);
  str_l = portable_vsnprintf(str, str_m, fmt, ap);
  va_end(ap);
  return str_l;
}
#endif

#if defined(NEED_SNPRINTF_ONLY)
int portable_snprintf(char *str, size_t str_m, const char *fmt, /*args*/ ...) {
#else
int portable_vsnprintf(char *str, size_t str_m, const char *fmt, va_list ap) {
#endif

#if defined(NEED_SNPRINTF_ONLY)
  va_list ap;
#endif
  size_t str_l = 0;
  const char *p = fmt;

/* In contrast with POSIX, the ISO C99 now says
 * that str can be NULL and str_m can be 0.
 * This is more useful than the old:  if (str_m < 1) return -1; */

#if defined(NEED_SNPRINTF_ONLY)
  va_start(ap, fmt);
#endif
  if (!p) p = "";
  while (*p) {
    if (*p != '%') {
   /* if (str_l < str_m) str[str_l++] = *p++;    -- this would be sufficient */
   /* but the following code achieves better performance for cases
    * where format string is long and contains few conversions */
      const char *q = strchr(p+1,'%');
      size_t n = !q ? strlen(p) : (q-p);
      if (str_l < str_m) {
        size_t avail = str_m-str_l;
        fast_memcpy(str+str_l, p, (n>avail?avail:n));
      }
      p += n; str_l += n;
    } else {
      const char *starting_p;
      size_t min_field_width = 0, precision = 0;
      int zero_padding = 0, precision_specified = 0, justify_left = 0;
      int alternate_form = 0, force_sign = 0;
      int space_for_positive = 1; /* If both the ' ' and '+' flags appear,
                                     the ' ' flag should be ignored. */
      char length_modifier = '\0';            /* allowed values: \0, h, l, L */
      char tmp[32];/* temporary buffer for simple numeric->string conversion */

      const char *str_arg;      /* string address in case of string argument */
      size_t str_arg_l;         /* natural field width of arg without padding
                                   and sign */
      unsigned char uchar_arg;
        /* unsigned char argument value - only defined for c conversion.
           N.B. standard explicitly states the char argument for
           the c conversion is unsigned */

      size_t number_of_zeros_to_pad = 0;
        /* number of zeros to be inserted for numeric conversions
           as required by the precision or minimal field width */

      size_t zero_padding_insertion_ind = 0;
        /* index into tmp where zero padding is to be inserted */

      char fmt_spec = '\0';
        /* current conversion specifier character */

      str_arg = credits;/* just to make compiler happy (defined but not used)*/
      str_arg = NULL;
      starting_p = p; p++;  /* skip '%' */
   /* parse flags */
      while (*p == '0' || *p == '-' || *p == '+' ||
             *p == ' ' || *p == '#' || *p == '\'') {
        switch (*p) {
        case '0': zero_padding = 1; break;
        case '-': justify_left = 1; break;
        case '+': force_sign = 1; space_for_positive = 0; break;
        case ' ': force_sign = 1;
     /* If both the ' ' and '+' flags appear, the ' ' flag should be ignored */
#ifdef PERL_COMPATIBLE
     /* ... but in Perl the last of ' ' and '+' applies */
                  space_for_positive = 1;
#endif
                  break;
        case '#': alternate_form = 1; break;
        case '\'': break;
        }
        p++;
      }
   /* If the '0' and '-' flags both appear, the '0' flag should be ignored. */

   /* parse field width */
      if (*p == '*') {
        int j;
        p++; j = va_arg(ap, int);
        if (j >= 0) min_field_width = j;
        else { min_field_width = -j; justify_left = 1; }
      } else if (isdigit((int)(*p))) {
        /* size_t could be wider than unsigned int;
           make sure we treat argument like common implementations do */
        unsigned int uj = *p++ - '0';
        while (isdigit((int)(*p))) uj = 10*uj + (unsigned int)(*p++ - '0');
        min_field_width = uj;
      }
   /* parse precision */
      if (*p == '.') {
        p++; precision_specified = 1;
        if (*p == '*') {
          int j = va_arg(ap, int);
          p++;
          if (j >= 0) precision = j;
          else {
            precision_specified = 0; precision = 0;
         /* NOTE:
          *   Solaris 2.6 man page claims that in this case the precision
          *   should be set to 0.  Digital Unix 4.0, HPUX 10 and BSD man page
          *   claim that this case should be treated as unspecified precision,
          *   which is what we do here.
          */
          }
        } else if (isdigit((int)(*p))) {
          /* size_t could be wider than unsigned int;
             make sure we treat argument like common implementations do */
          unsigned int uj = *p++ - '0';
          while (isdigit((int)(*p))) uj = 10*uj + (unsigned int)(*p++ - '0');
          precision = uj;
        }
      }
   /* parse 'h', 'l' and 'll' length modifiers */
      if (*p == 'h' || *p == 'l') {
        length_modifier = *p; p++;
        if (length_modifier == 'l' && *p == 'l') {   /* double l = long long */
#ifdef SNPRINTF_LONGLONG_SUPPORT
          length_modifier = '2';                  /* double l encoded as '2' */
#else
          length_modifier = 'l';                 /* treat it as a single 'l' */
#endif
          p++;
        }
      }
      fmt_spec = *p;
   /* common synonyms: */
      switch (fmt_spec) {
      case 'i': fmt_spec = 'd'; break;
      case 'D': fmt_spec = 'd'; length_modifier = 'l'; break;
      case 'U': fmt_spec = 'u'; length_modifier = 'l'; break;
      case 'O': fmt_spec = 'o'; length_modifier = 'l'; break;
      default: break;
      }
   /* get parameter value, do initial processing */
      switch (fmt_spec) {
      case '%': /* % behaves similar to 's' regarding flags and field widths */
      case 'c': /* c behaves similar to 's' regarding flags and field widths */
      case 's':
        length_modifier = '\0';          /* wint_t and wchar_t not supported */
     /* the result of zero padding flag with non-numeric conversion specifier*/
     /* is undefined. Solaris and HPUX 10 does zero padding in this case,    */
     /* Digital Unix and Linux does not. */
#if !defined(SOLARIS_COMPATIBLE) && !defined(HPUX_COMPATIBLE)
        zero_padding = 0;    /* turn zero padding off for string conversions */
#endif
        str_arg_l = 1;
        switch (fmt_spec) {
        case '%':
          str_arg = p; break;
        case 'c': {
          int j = va_arg(ap, int);
          uchar_arg = (unsigned char) j;   /* standard demands unsigned char */
          str_arg = (const char *) &uchar_arg;
          break;
        }
        case 's':
          str_arg = va_arg(ap, const char *);
          if (!str_arg) str_arg_l = 0;
       /* make sure not to address string beyond the specified precision !!! */
          else if (!precision_specified) str_arg_l = strlen(str_arg);
       /* truncate string if necessary as requested by precision */
          else if (precision == 0) str_arg_l = 0;
          else {
       /* memchr on HP does not like n > 2^31  !!! */
            const char *q = memchr(str_arg, '\0',
                             precision <= 0x7fffffff ? precision : 0x7fffffff);
            str_arg_l = !q ? precision : (q-str_arg);
          }
          break;
        default: break;
        }
        break;
      case 'd': case 'u': case 'o': case 'x': case 'X': case 'p': {
        /* NOTE: the u, o, x, X and p conversion specifiers imply
                 the value is unsigned;  d implies a signed value */

        int arg_sign = 0;
          /* 0 if numeric argument is zero (or if pointer is NULL for 'p'),
            +1 if greater than zero (or nonzero for unsigned arguments),
            -1 if negative (unsigned argument is never negative) */

        int int_arg = 0;  unsigned int uint_arg = 0;
          /* only defined for length modifier h, or for no length modifiers */

        long int long_arg = 0;  unsigned long int ulong_arg = 0;
          /* only defined for length modifier l */

        void *ptr_arg = NULL;
          /* pointer argument value -only defined for p conversion */

#ifdef SNPRINTF_LONGLONG_SUPPORT
        long long int long_long_arg = 0;
        unsigned long long int ulong_long_arg = 0;
          /* only defined for length modifier ll */
#endif
        if (fmt_spec == 'p') {
        /* HPUX 10: An l, h, ll or L before any other conversion character
         *   (other than d, i, u, o, x, or X) is ignored.
         * Digital Unix:
         *   not specified, but seems to behave as HPUX does.
         * Solaris: If an h, l, or L appears before any other conversion
         *   specifier (other than d, i, u, o, x, or X), the behavior
         *   is undefined. (Actually %hp converts only 16-bits of address
         *   and %llp treats address as 64-bit data which is incompatible
         *   with (void *) argument on a 32-bit system).
         */
#ifdef SOLARIS_COMPATIBLE
#  ifdef SOLARIS_BUG_COMPATIBLE
          /* keep length modifiers even if it represents 'll' */
#  else
          if (length_modifier == '2') length_modifier = '\0';
#  endif
#else
          length_modifier = '\0';
#endif
          ptr_arg = va_arg(ap, void *);
          if (ptr_arg != NULL) arg_sign = 1;
        } else if (fmt_spec == 'd') {  /* signed */
          switch (length_modifier) {
          case '\0':
          case 'h':
         /* It is non-portable to specify a second argument of char or short
          * to va_arg, because arguments seen by the called function
          * are not char or short.  C converts char and short arguments
          * to int before passing them to a function.
          */
            int_arg = va_arg(ap, int);
            if      (int_arg > 0) arg_sign =  1;
            else if (int_arg < 0) arg_sign = -1;
            break;
          case 'l':
            long_arg = va_arg(ap, long int);
            if      (long_arg > 0) arg_sign =  1;
            else if (long_arg < 0) arg_sign = -1;
            break;
#ifdef SNPRINTF_LONGLONG_SUPPORT
          case '2':
            long_long_arg = va_arg(ap, long long int);
            if      (long_long_arg > 0) arg_sign =  1;
            else if (long_long_arg < 0) arg_sign = -1;
            break;
#endif
          }
        } else {  /* unsigned */
          switch (length_modifier) {
          case '\0':
          case 'h':
            uint_arg = va_arg(ap, unsigned int);
            if (uint_arg) arg_sign = 1;
            break;
          case 'l':
            ulong_arg = va_arg(ap, unsigned long int);
            if (ulong_arg) arg_sign = 1;
            break;
#ifdef SNPRINTF_LONGLONG_SUPPORT
          case '2':
            ulong_long_arg = va_arg(ap, unsigned long long int);
            if (ulong_long_arg) arg_sign = 1;
            break;
#endif
          }
        }
        str_arg = tmp; str_arg_l = 0;
     /* NOTE:
      *   For d, i, u, o, x, and X conversions, if precision is specified,
      *   the '0' flag should be ignored. This is so with Solaris 2.6,
      *   Digital UNIX 4.0, HPUX 10, Linux, FreeBSD, NetBSD; but not with Perl.
      */
#ifndef PERL_COMPATIBLE
        if (precision_specified) zero_padding = 0;
#endif
        if (fmt_spec == 'd') {
          if (force_sign && arg_sign >= 0)
            tmp[str_arg_l++] = space_for_positive ? ' ' : '+';
         /* leave negative numbers for sprintf to handle,
            to avoid handling tricky cases like (short int)(-32768) */
#ifdef LINUX_COMPATIBLE
        } else if (fmt_spec == 'p' && force_sign && arg_sign > 0) {
          tmp[str_arg_l++] = space_for_positive ? ' ' : '+';
#endif
        } else if (alternate_form) {
          if (arg_sign != 0 && (fmt_spec == 'x' || fmt_spec == 'X') )
            { tmp[str_arg_l++] = '0'; tmp[str_arg_l++] = fmt_spec; }
         /* alternate form should have no effect for p conversion, but ... */
#ifdef HPUX_COMPATIBLE
          else if (fmt_spec == 'p'
         /* HPUX 10: for an alternate form of p conversion,
          *          a nonzero result is prefixed by 0x. */
#ifndef HPUX_BUG_COMPATIBLE
         /* Actually it uses 0x prefix even for a zero value. */
                   && arg_sign != 0
#endif
                  ) { tmp[str_arg_l++] = '0'; tmp[str_arg_l++] = 'x'; }
#endif
        }
        zero_padding_insertion_ind = str_arg_l;
        if (!precision_specified) precision = 1;   /* default precision is 1 */
        if (precision == 0 && arg_sign == 0
#if defined(HPUX_BUG_COMPATIBLE) || defined(LINUX_COMPATIBLE)
            && fmt_spec != 'p'
         /* HPUX 10 man page claims: With conversion character p the result of
          * converting a zero value with a precision of zero is a null string.
          * Actually HP returns all zeroes, and Linux returns "(nil)". */
#endif
        ) {
         /* converted to null string */
         /* When zero value is formatted with an explicit precision 0,
            the resulting formatted string is empty (d, i, u, o, x, X, p).   */
        } else {
          char f[5]; int f_l = 0;
          f[f_l++] = '%';    /* construct a simple format string for sprintf */
          if (!length_modifier) { }
          else if (length_modifier=='2') { f[f_l++] = 'l'; f[f_l++] = 'l'; }
          else f[f_l++] = length_modifier;
          f[f_l++] = fmt_spec; f[f_l++] = '\0';
          if (fmt_spec == 'p') str_arg_l += sprintf(tmp+str_arg_l, f, ptr_arg);
          else if (fmt_spec == 'd') {  /* signed */
            switch (length_modifier) {
            case '\0':
            case 'h': str_arg_l+=sprintf(tmp+str_arg_l, f, int_arg);  break;
            case 'l': str_arg_l+=sprintf(tmp+str_arg_l, f, long_arg); break;
#ifdef SNPRINTF_LONGLONG_SUPPORT
            case '2': str_arg_l+=sprintf(tmp+str_arg_l,f,long_long_arg); break;
#endif
            }
          } else {  /* unsigned */
            switch (length_modifier) {
            case '\0':
            case 'h': str_arg_l+=sprintf(tmp+str_arg_l, f, uint_arg);  break;
            case 'l': str_arg_l+=sprintf(tmp+str_arg_l, f, ulong_arg); break;
#ifdef SNPRINTF_LONGLONG_SUPPORT
            case '2': str_arg_l+=sprintf(tmp+str_arg_l,f,ulong_long_arg);break;
#endif
            }
          }
         /* include the optional minus sign and possible "0x"
            in the region before the zero padding insertion point */
          if (zero_padding_insertion_ind < str_arg_l &&
              tmp[zero_padding_insertion_ind] == '-') {
            zero_padding_insertion_ind++;
          }
          if (zero_padding_insertion_ind+1 < str_arg_l &&
              tmp[zero_padding_insertion_ind]   == '0' &&
             (tmp[zero_padding_insertion_ind+1] == 'x' ||
              tmp[zero_padding_insertion_ind+1] == 'X') ) {
            zero_padding_insertion_ind += 2;
          }
        }
        { size_t num_of_digits = str_arg_l - zero_padding_insertion_ind;
          if (alternate_form && fmt_spec == 'o'
#ifdef HPUX_COMPATIBLE                                  /* ("%#.o",0) -> ""  */
              && (str_arg_l > 0)
#endif
#ifdef DIGITAL_UNIX_BUG_COMPATIBLE                      /* ("%#o",0) -> "00" */
#else
              /* unless zero is already the first character */
              && !(zero_padding_insertion_ind < str_arg_l
                   && tmp[zero_padding_insertion_ind] == '0')
#endif
          ) {        /* assure leading zero for alternate-form octal numbers */
            if (!precision_specified || precision < num_of_digits+1) {
             /* precision is increased to force the first character to be zero,
                except if a zero value is formatted with an explicit precision
                of zero */
              precision = num_of_digits+1; precision_specified = 1;
            }
          }
       /* zero padding to specified precision? */
          if (num_of_digits < precision)
            number_of_zeros_to_pad = precision - num_of_digits;
        }
     /* zero padding to specified minimal field width? */
        if (!justify_left && zero_padding) {
          int n = min_field_width - (str_arg_l+number_of_zeros_to_pad);
          if (n > 0) number_of_zeros_to_pad += n;
        }
        break;
      }
      default: /* unrecognized conversion specifier, keep format string as-is*/
        zero_padding = 0;  /* turn zero padding off for non-numeric convers. */
#ifndef DIGITAL_UNIX_COMPATIBLE
        justify_left = 1; min_field_width = 0;                /* reset flags */
#endif
#if defined(PERL_COMPATIBLE) || defined(LINUX_COMPATIBLE)
     /* keep the entire format string unchanged */
        str_arg = starting_p; str_arg_l = p - starting_p;
     /* well, not exactly so for Linux, which does something between,
      * and I don't feel an urge to imitate it: "%+++++hy" -> "%+y"  */
#else
     /* discard the unrecognized conversion, just keep *
      * the unrecognized conversion character          */
        str_arg = p; str_arg_l = 0;
#endif
        if (*p) str_arg_l++;  /* include invalid conversion specifier unchanged
                                 if not at end-of-string */
        break;
      }
      if (*p) p++;      /* step over the just processed conversion specifier */
   /* insert padding to the left as requested by min_field_width;
      this does not include the zero padding in case of numerical conversions*/
      if (!justify_left) {                /* left padding with blank or zero */
        int n = min_field_width - (str_arg_l+number_of_zeros_to_pad);
        if (n > 0) {
          if (str_l < str_m) {
            size_t avail = str_m-str_l;
            fast_memset(str+str_l, (zero_padding?'0':' '), (n>avail?avail:n));
          }
          str_l += n;
        }
      }
   /* zero padding as requested by the precision or by the minimal field width
    * for numeric conversions required? */
      if (number_of_zeros_to_pad <= 0) {
     /* will not copy first part of numeric right now, *
      * force it to be copied later in its entirety    */
        zero_padding_insertion_ind = 0;
      } else {
     /* insert first part of numerics (sign or '0x') before zero padding */
        int n = zero_padding_insertion_ind;
        if (n > 0) {
          if (str_l < str_m) {
            size_t avail = str_m-str_l;
            fast_memcpy(str+str_l, str_arg, (n>avail?avail:n));
          }
          str_l += n;
        }
     /* insert zero padding as requested by the precision or min field width */
        n = number_of_zeros_to_pad;
        if (n > 0) {
          if (str_l < str_m) {
            size_t avail = str_m-str_l;
            fast_memset(str+str_l, '0', (n>avail?avail:n));
          }
          str_l += n;
        }
      }
   /* insert formatted string
    * (or as-is conversion specifier for unknown conversions) */
      { int n = str_arg_l - zero_padding_insertion_ind;
        if (n > 0) {
          if (str_l < str_m) {
            size_t avail = str_m-str_l;
            fast_memcpy(str+str_l, str_arg+zero_padding_insertion_ind,
                        (n>avail?avail:n));
          }
          str_l += n;
        }
      }
   /* insert right padding */
      if (justify_left) {          /* right blank padding to the field width */
        int n = min_field_width - (str_arg_l+number_of_zeros_to_pad);
        if (n > 0) {
          if (str_l < str_m) {
            size_t avail = str_m-str_l;
            fast_memset(str+str_l, ' ', (n>avail?avail:n));
          }
          str_l += n;
        }
      }
    }
  }
#if defined(NEED_SNPRINTF_ONLY)
  va_end(ap);
#endif
  if (str_m > 0) { /* make sure the string is null-terminated
                      even at the expense of overwriting the last character
                      (shouldn't happen, but just in case) */
    str[str_l <= str_m-1 ? str_l : str_m-1] = '\0';
  }
  /* Return the number of characters formatted (excluding trailing null
   * character), that is, the number of characters that would have been
   * written to the buffer if it were large enough.
   *
   * The value of str_l should be returned, but str_l is of unsigned type
   * size_t, and snprintf is int, possibly leading to an undetected
   * integer overflow, resulting in a negative return value, which is illegal.
   * Both XSH5 and ISO C99 (at least the draft) are silent on this issue.
   * Should errno be set to EOVERFLOW and EOF returned in this case???
   */
  return (int) str_l;
}
#endif
#endif /* ndef HAVE_SNPRINTF */
/*
  Local Variables:
  tab-width: 3
  end:
*/
