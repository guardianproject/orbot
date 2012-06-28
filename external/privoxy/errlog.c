const char errlog_rcs[] = "$Id: errlog.c,v 1.112 2011/09/04 11:10:56 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/errlog.c,v $
 *
 * Purpose     :  Log errors to a designated destination in an elegant,
 *                printf-like fashion.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2010 the
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


#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <ctype.h>

#include "config.h"
#include "miscutil.h"

/* For gettimeofday() */
#include <sys/time.h>

#if !defined(_WIN32) && !defined(__OS2__)
#include <unistd.h>
#endif /* !defined(_WIN32) && !defined(__OS2__) */

#include <errno.h>
#include <assert.h>

#ifdef _WIN32
#ifndef STRICT
#define STRICT
#endif
#include <windows.h>
#ifndef _WIN_CONSOLE
#include "w32log.h"
#endif /* ndef _WIN_CONSOLE */
#endif /* def _WIN32 */
#ifdef _MSC_VER
#define inline __inline
#endif /* def _MSC_VER */

#ifdef __OS2__
#include <sys/socket.h> /* For sock_errno */
#define INCL_DOS
#include <os2.h>
#endif

#include "errlog.h"
#include "project.h"
#include "jcc.h"

const char errlog_h_rcs[] = ERRLOG_H_VERSION;


/*
 * LOG_LEVEL_FATAL cannot be turned off.  (There are
 * some exceptional situations where we need to get a
 * message to the user).
 */
#define LOG_LEVEL_MINIMUM  LOG_LEVEL_FATAL

/* where to log (default: stderr) */
static FILE *logfp = NULL;

/* logging detail level. XXX: stupid name. */
static int debug = (LOG_LEVEL_FATAL | LOG_LEVEL_ERROR);

/* static functions */
static void fatal_error(const char * error_message);
#ifdef _WIN32
static char *w32_socket_strerr(int errcode, char *tmp_buf);
#endif
#ifdef __OS2__
static char *os2_socket_strerr(int errcode, char *tmp_buf);
#endif

#ifdef MUTEX_LOCKS_AVAILABLE
static inline void lock_logfile(void)
{
   privoxy_mutex_lock(&log_mutex);
}
static inline void unlock_logfile(void)
{
   privoxy_mutex_unlock(&log_mutex);
}
static inline void lock_loginit(void)
{
   privoxy_mutex_lock(&log_init_mutex);
}
static inline void unlock_loginit(void)
{
   privoxy_mutex_unlock(&log_init_mutex);
}
#else /* ! MUTEX_LOCKS_AVAILABLE */
/*
 * FIXME we need a cross-platform locking mechanism.
 * The locking/unlocking functions below should be
 * fleshed out for non-pthread implementations.
 */
static inline void lock_logfile() {}
static inline void unlock_logfile() {}
static inline void lock_loginit() {}
static inline void unlock_loginit() {}
#endif

/*********************************************************************
 *
 * Function    :  fatal_error
 *
 * Description :  Displays a fatal error to standard error (or, on
 *                a WIN32 GUI, to a dialog box), and exits Privoxy
 *                with status code 1.
 *
 * Parameters  :
 *          1  :  error_message = The error message to display.
 *
 * Returns     :  Does not return.
 *
 *********************************************************************/
static void fatal_error(const char *error_message)
{
#if defined(_WIN32) && !defined(_WIN_CONSOLE)
   /* Skip timestamp and thread id for the message box. */
   const char *box_message = strstr(error_message, "Fatal error");
   if (NULL == box_message)
   {
      /* Shouldn't happen but ... */
      box_message = error_message;
   }
   MessageBox(g_hwndLogFrame, box_message, "Privoxy Error",
      MB_OK | MB_ICONERROR | MB_TASKMODAL | MB_SETFOREGROUND | MB_TOPMOST);

   /* Cleanup - remove taskbar icon etc. */
   TermLogWindow();
#endif /* defined(_WIN32) && !defined(_WIN_CONSOLE) */

   if (logfp != NULL)
   {
      fputs(error_message, logfp);
   }

#if defined(unix)
   if (pidfile)
   {
      unlink(pidfile);
   }
#endif /* unix */

   exit(1);
}


/*********************************************************************
 *
 * Function    :  show_version
 *
 * Description :  Logs the Privoxy version and the program name.
 *
 * Parameters  :
 *          1  :  prog_name = The program name.
 *
 * Returns     :  Nothing.
 *
 *********************************************************************/
void show_version(const char *prog_name)
{
   log_error(LOG_LEVEL_INFO, "Privoxy version " VERSION);
   if (prog_name != NULL)
   {
      log_error(LOG_LEVEL_INFO, "Program name: %s", prog_name);
   }
}


/*********************************************************************
 *
 * Function    :  init_log_module
 *
 * Description :  Initializes the logging module to log to stderr.
 *                Can only be called while stderr hasn't been closed
 *                yet and is only supposed to be called once.
 *
 * Parameters  :
 *          1  :  prog_name = The program name.
 *
 * Returns     :  Nothing.
 *
 *********************************************************************/
void init_log_module(void)
{
   lock_logfile();
   logfp = stderr;
   unlock_logfile();
   set_debug_level(debug);
}


/*********************************************************************
 *
 * Function    :  set_debug_level
 *
 * Description :  Sets the debug level to the provided value
 *                plus LOG_LEVEL_MINIMUM.
 *
 *                XXX: we should only use the LOG_LEVEL_MINIMUM
 *                until the first time the configuration file has
 *                been parsed.
 *
 * Parameters  :  1: debug_level = The debug level to set.
 *
 * Returns     :  Nothing.
 *
 *********************************************************************/
void set_debug_level(int debug_level)
{
   debug = debug_level | LOG_LEVEL_MINIMUM;
}


/*********************************************************************
 *
 * Function    :  disable_logging
 *
 * Description :  Disables logging.
 *
 * Parameters  :  None.
 *
 * Returns     :  Nothing.
 *
 *********************************************************************/
void disable_logging(void)
{
   if (logfp != NULL)
   {
      log_error(LOG_LEVEL_INFO,
         "No logfile configured. Please enable it before reporting any problems.");
      lock_logfile();
      fclose(logfp);
      logfp = NULL;
      unlock_logfile();
   }
}


/*********************************************************************
 *
 * Function    :  init_error_log
 *
 * Description :  Initializes the logging module to log to a file.
 *
 *                XXX: should be renamed.
 *
 * Parameters  :
 *          1  :  prog_name  = The program name.
 *          2  :  logfname   = The logfile to (re)open.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void init_error_log(const char *prog_name, const char *logfname)
{
   FILE *fp;

   assert(NULL != logfname);

   lock_loginit();

   if ((logfp != NULL) && (logfp != stderr))
   {
      log_error(LOG_LEVEL_INFO, "(Re-)Opening logfile \'%s\'", logfname);
   }

   /* set the designated log file */
   fp = fopen(logfname, "a");
   if ((NULL == fp) && (logfp != NULL))
   {
      /*
       * Some platforms (like OS/2) don't allow us to open
       * the same file twice, therefore we give it another
       * shot after closing the old file descriptor first.
       *
       * We don't do it right away because it prevents us
       * from logging the "can't open logfile" message to
       * the old logfile.
       *
       * XXX: this is a lame workaround and once the next
       * release is out we should stop bothering reopening
       * the logfile unless we have to.
       *
       * Currently we reopen it every time the config file
       * has been reloaded, but actually we only have to
       * reopen it if the file name changed or if the
       * configuration reload was caused by a SIGHUP.
       */
      log_error(LOG_LEVEL_INFO, "Failed to reopen logfile: \'%s\'. "
         "Retrying after closing the old file descriptor first. If that "
         "doesn't work, Privoxy will exit without being able to log a message.",
         logfname);
      lock_logfile();
      fclose(logfp);
      logfp = NULL;
      unlock_logfile();
      fp = fopen(logfname, "a");
   }

   if (NULL == fp)
   {
      log_error(LOG_LEVEL_FATAL, "init_error_log(): can't open logfile: \'%s\'", logfname);
   }

   /* set logging to be completely unbuffered */
   setbuf(fp, NULL);

   lock_logfile();
   if (logfp != NULL)
   {
      fclose(logfp);
   }
#ifdef unix
   if (daemon_mode && (logfp == stderr))
   {
      if (dup2(1, 2) == -1)
      {
         /*
          * We only use fatal_error() to clear the pid
          * file and to exit. Given that stderr has just
          * been closed, the user will not see the error
          * message.
          */
         fatal_error("Failed to reserve fd 2.");
      }
   }
#endif
   logfp = fp;
   unlock_logfile();

   show_version(prog_name);

   unlock_loginit();

} /* init_error_log */


/*********************************************************************
 *
 * Function    :  get_thread_id
 *
 * Description :  Returns a number that is different for each thread.
 *
 *                XXX: Should be moved elsewhere (miscutil.c?)
 *
 * Parameters  :  None
 *
 * Returns     :  thread_id
 *
 *********************************************************************/
static long get_thread_id(void)
{
   long this_thread = 1;  /* was: pthread_t this_thread;*/

#ifdef __OS2__
   PTIB     ptib;
   APIRET   ulrc; /* XXX: I have no clue what this does */
#endif /* __OS2__ */

   /* FIXME get current thread id */
#ifdef FEATURE_PTHREAD
   this_thread = (long)pthread_self();
#ifdef __MACH__
   /*
    * Mac OSX (and perhaps other Mach instances) doesn't have a debuggable
    * value at the first 4 bytes of pthread_self()'s return value, a pthread_t.
    * pthread_t is supposed to be opaque... but it's fairly random, though, so
    * we make it mostly presentable.
    */
   this_thread = abs(this_thread % 1000);
#endif /* def __MACH__ */
#elif defined(_WIN32)
   this_thread = GetCurrentThreadId();
#elif defined(__OS2__)
   ulrc = DosGetInfoBlocks(&ptib, NULL);
   if (ulrc == 0)
     this_thread = ptib -> tib_ptib2 -> tib2_ultid;
#endif /* def FEATURE_PTHREAD */

   return this_thread;
}


/*********************************************************************
 *
 * Function    :  get_log_timestamp
 *
 * Description :  Generates the time stamp for the log message prefix.
 *
 * Parameters  :
 *          1  :  buffer = Storage buffer
 *          2  :  buffer_size = Size of storage buffer
 *
 * Returns     :  Number of written characters or 0 for error.
 *
 *********************************************************************/
static inline size_t get_log_timestamp(char *buffer, size_t buffer_size)
{
   size_t length;
   time_t now;
   struct tm tm_now;
   struct timeval tv_now; /* XXX: stupid name */
   long msecs;
   int msecs_length = 0;

   gettimeofday(&tv_now, NULL);
   msecs = tv_now.tv_usec / 1000;
   now = tv_now.tv_sec;

#ifdef HAVE_LOCALTIME_R
   tm_now = *localtime_r(&now, &tm_now);
#elif defined(MUTEX_LOCKS_AVAILABLE)
   privoxy_mutex_lock(&localtime_mutex);
   tm_now = *localtime(&now);
   privoxy_mutex_unlock(&localtime_mutex);
#else
   tm_now = *localtime(&now);
#endif

   length = strftime(buffer, buffer_size, "%Y-%m-%d %H:%M:%S", &tm_now);
   if (length > (size_t)0)
   {
      msecs_length = snprintf(buffer+length, buffer_size - length, ".%.3ld", msecs);
   }
   if (msecs_length > 0)
   {
      length += (size_t)msecs_length;
   }
   else
   {
      length = 0;
   }

   return length;
}


/*********************************************************************
 *
 * Function    :  get_clf_timestamp
 *
 * Description :  Generates a Common Log Format time string.
 *
 * Parameters  :
 *          1  :  buffer = Storage buffer
 *          2  :  buffer_size = Size of storage buffer
 *
 * Returns     :  Number of written characters or 0 for error.
 *
 *********************************************************************/
static inline size_t get_clf_timestamp(char *buffer, size_t buffer_size)
{
   /*
    * Complex because not all OSs have tm_gmtoff or
    * the %z field in strftime()
    */
   time_t now;
   struct tm *tm_now;
   struct tm gmt;
#ifdef HAVE_LOCALTIME_R
   struct tm dummy;
#endif
   int days, hrs, mins;
   size_t length;
   int tz_length = 0;

   time (&now);
#ifdef HAVE_GMTIME_R
   gmt = *gmtime_r(&now, &gmt);
#elif defined(MUTEX_LOCKS_AVAILABLE)
   privoxy_mutex_lock(&gmtime_mutex);
   gmt = *gmtime(&now);
   privoxy_mutex_unlock(&gmtime_mutex);
#else
   gmt = *gmtime(&now);
#endif
#ifdef HAVE_LOCALTIME_R
   tm_now = localtime_r(&now, &dummy);
#elif defined(MUTEX_LOCKS_AVAILABLE)
   privoxy_mutex_lock(&localtime_mutex);
   tm_now = localtime(&now);
   privoxy_mutex_unlock(&localtime_mutex);
#else
   tm_now = localtime(&now);
#endif
   days = tm_now->tm_yday - gmt.tm_yday;
   hrs = ((days < -1 ? 24 : 1 < days ? -24 : days * 24) + tm_now->tm_hour - gmt.tm_hour);
   mins = hrs * 60 + tm_now->tm_min - gmt.tm_min;

   length = strftime(buffer, buffer_size, "%d/%b/%Y:%H:%M:%S ", tm_now);

   if (length > (size_t)0)
   {
      tz_length = snprintf(buffer+length, buffer_size-length,
                     "%+03d%02d", mins / 60, abs(mins) % 60);
   }
   if (tz_length > 0)
   {
      length += (size_t)tz_length;
   }
   else
   {
      length = 0;
   }

   return length;
}


/*********************************************************************
 *
 * Function    :  get_log_level_string
 *
 * Description :  Translates a numerical loglevel into a string.
 *
 * Parameters  :
 *          1  :  loglevel = LOG_LEVEL_FOO
 *
 * Returns     :  Log level string.
 *
 *********************************************************************/
static inline const char *get_log_level_string(int loglevel)
{
   char *log_level_string = NULL;

   assert(0 < loglevel);

   switch (loglevel)
   {
      case LOG_LEVEL_ERROR:
         log_level_string = "Error";
         break;
      case LOG_LEVEL_FATAL:
         log_level_string = "Fatal error";
         break;
      case LOG_LEVEL_GPC:
         log_level_string = "Request";
         break;
      case LOG_LEVEL_CONNECT:
         log_level_string = "Connect";
         break;
      case LOG_LEVEL_WRITING:
         log_level_string = "Writing";
         break;
      case LOG_LEVEL_RECEIVED:
         log_level_string = "Received";
         break;
      case LOG_LEVEL_HEADER:
         log_level_string = "Header";
         break;
      case LOG_LEVEL_INFO:
         log_level_string = "Info";
         break;
      case LOG_LEVEL_RE_FILTER:
         log_level_string = "Re-Filter";
         break;
#ifdef FEATURE_FORCE_LOAD
      case LOG_LEVEL_FORCE:
         log_level_string = "Force";
         break;
#endif /* def FEATURE_FORCE_LOAD */
      case LOG_LEVEL_REDIRECTS:
         log_level_string = "Redirect";
         break;
      case LOG_LEVEL_DEANIMATE:
         log_level_string = "Gif-Deanimate";
         break;
      case LOG_LEVEL_CRUNCH:
         log_level_string = "Crunch";
         break;
      case LOG_LEVEL_CGI:
         log_level_string = "CGI";
         break;
      default:
         log_level_string = "Unknown log level";
         break;
   }
   assert(NULL != log_level_string);

   return log_level_string;
}


/*********************************************************************
 *
 * Function    :  log_error
 *
 * Description :  This is the error-reporting and logging function.
 *
 * Parameters  :
 *          1  :  loglevel  = the type of message to be logged
 *          2  :  fmt       = the main string we want logged, printf-like
 *          3  :  ...       = arguments to be inserted in fmt (printf-like).
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void log_error(int loglevel, const char *fmt, ...)
{
   va_list ap;
   char *outbuf = NULL;
   static char *outbuf_save = NULL;
   char tempbuf[BUFFER_SIZE];
   size_t length = 0;
   const char * src = fmt;
   long thread_id;
   char timestamp[30];
   /*
    * XXX: Make this a config option,
    * why else do we allocate instead of using
    * an array?
    */
   size_t log_buffer_size = BUFFER_SIZE;

#if defined(_WIN32) && !defined(_WIN_CONSOLE)
   /*
    * Irrespective of debug setting, a GET/POST/CONNECT makes
    * the taskbar icon animate.  (There is an option to disable
    * this but checking that is handled inside LogShowActivity()).
    */
   if ((loglevel == LOG_LEVEL_GPC) || (loglevel == LOG_LEVEL_CRUNCH))
   {
      LogShowActivity();
   }
#endif /* defined(_WIN32) && !defined(_WIN_CONSOLE) */

   /*
    * verify that the loglevel applies to current
    * settings and that logging is enabled.
    * Bail out otherwise.
    */
   if ((0 == (loglevel & debug))
#ifndef _WIN32
      || (logfp == NULL)
#endif
      )
   {
      if (loglevel == LOG_LEVEL_FATAL)
      {
         fatal_error("Fatal error. You're not supposed to"
            "see this message. Please file a bug report.");
      }
      return;
   }

   thread_id = get_thread_id();
   get_log_timestamp(timestamp, sizeof(timestamp));

   /* protect the whole function because of the static buffer (outbuf) */
   lock_logfile();

   if (NULL == outbuf_save)
   {
      outbuf_save = (char*)zalloc(log_buffer_size + 1); /* +1 for paranoia */
      if (NULL == outbuf_save)
      {
         snprintf(tempbuf, sizeof(tempbuf),
            "%s %08lx Fatal error: Out of memory in log_error().",
            timestamp, thread_id);
         fatal_error(tempbuf); /* Exit */
         return;
      }
   }
   outbuf = outbuf_save;

   /*
    * Memsetting the whole buffer to zero (in theory)
    * makes things easier later on.
    */
   memset(outbuf, 0, log_buffer_size);

   /* Add prefix for everything but Common Log Format messages */
   if (loglevel != LOG_LEVEL_CLF)
   {
      length = (size_t)snprintf(outbuf, log_buffer_size, "%s %08lx %s: ",
         timestamp, thread_id, get_log_level_string(loglevel));
   }

   /* get ready to scan var. args. */
   va_start(ap, fmt);

   /* build formatted message from fmt and var-args */
   while ((*src) && (length < log_buffer_size-2))
   {
      const char *sval = NULL; /* %N string  */
      int ival;                /* %N string length or an error code */
      unsigned uval;           /* %u value */
      long lval;               /* %l value */
      unsigned long ulval;     /* %ul value */
      char ch;
      const char *format_string = tempbuf;

      ch = *src++;
      if (ch != '%')
      {
         outbuf[length++] = ch;
         /*
          * XXX: Only necessary on platforms where multiple threads
          * can write to the buffer at the same time because we
          * don't support mutexes (OS/2 for example).
          */
         outbuf[length] = '\0';
         continue;
      }
      outbuf[length] = '\0';
      ch = *src++;
      switch (ch) {
         case '%':
            tempbuf[0] = '%';
            tempbuf[1] = '\0';
            break;
         case 'd':
            ival = va_arg( ap, int );
            snprintf(tempbuf, sizeof(tempbuf), "%d", ival);
            break;
         case 'u':
            uval = va_arg( ap, unsigned );
            snprintf(tempbuf, sizeof(tempbuf), "%u", uval);
            break;
         case 'l':
            /* this is a modifier that must be followed by u, lu, or d */
            ch = *src++;
            if (ch == 'd')
            {
               lval = va_arg( ap, long );
               snprintf(tempbuf, sizeof(tempbuf), "%ld", lval);
            }
            else if (ch == 'u')
            {
               ulval = va_arg( ap, unsigned long );
               snprintf(tempbuf, sizeof(tempbuf), "%lu", ulval);
            }
            else if ((ch == 'l') && (*src == 'u'))
            {
               unsigned long long lluval = va_arg(ap, unsigned long long);
               snprintf(tempbuf, sizeof(tempbuf), "%llu", lluval);
               src++;
            }
            else
            {
               snprintf(tempbuf, sizeof(tempbuf), "Bad format string: \"%s\"", fmt);
               loglevel = LOG_LEVEL_FATAL;
            }
            break;
         case 'c':
            /*
             * Note that char paramaters are converted to int, so we need to
             * pass "int" to va_arg.  (See K&R, 2nd ed, section A7.3.2, page 202)
             */
            tempbuf[0] = (char) va_arg(ap, int);
            tempbuf[1] = '\0';
            break;
         case 's':
            format_string = va_arg(ap, char *);
            if (format_string == NULL)
            {
               format_string = "[null]";
            }
            break;
         case 'N':
            /*
             * Non-standard: Print a counted unterminated string,
             * replacing unprintable bytes with their hex value.
             * Takes 2 parameters: int length, const char * string.
             */
            ival = va_arg(ap, int);
            assert(ival >= 0);
            sval = va_arg(ap, char *);
            assert(sval != NULL);

            while ((ival-- > 0) && (length < log_buffer_size - 6))
            {
               if (isprint((int)*sval) && (*sval != '\\'))
               {
                  outbuf[length++] = *sval;
                  outbuf[length] = '\0';
               }
               else
               {
                  int ret = snprintf(outbuf + length,
                     log_buffer_size - length - 2, "\\x%.2x", (unsigned char)*sval);
                  assert(ret == 4);
                  length += 4;
               }
               sval++;
            }
            /*
             * XXX: In case of printable characters at the end of
             *      the %N string, we're not using the whole buffer.
             */
            format_string = (length < log_buffer_size - 6) ? "" : "[too long]";
            break;
         case 'E':
            /* Non-standard: Print error code from errno */
#ifdef _WIN32
            ival = WSAGetLastError();
            format_string = w32_socket_strerr(ival, tempbuf);
#elif __OS2__
            ival = sock_errno();
            if (ival != 0)
            {
               format_string = os2_socket_strerr(ival, tempbuf);
            }
            else
            {
               ival = errno;
               format_string = strerror(ival);
            }
#else /* ifndef _WIN32 */
            ival = errno;
#ifdef HAVE_STRERROR
            format_string = strerror(ival);
#else /* ifndef HAVE_STRERROR */
            format_string = NULL;
#endif /* ndef HAVE_STRERROR */
            if (sval == NULL)
            {
               snprintf(tempbuf, sizeof(tempbuf), "(errno = %d)", ival);
            }
#endif /* ndef _WIN32 */
            break;
         case 'T':
            /* Non-standard: Print a Common Log File timestamp */
            get_clf_timestamp(tempbuf, sizeof(tempbuf));
            break;
         default:
            snprintf(tempbuf, sizeof(tempbuf), "Bad format string: \"%s\"", fmt);
            loglevel = LOG_LEVEL_FATAL;
            break;
      }

      assert(length < log_buffer_size);
      length += strlcpy(outbuf + length, format_string, log_buffer_size - length);

      if (length >= log_buffer_size-2)
      {
         static const char warning[] = "... [too long, truncated]";

         length = log_buffer_size - sizeof(warning) - 1;
         length += strlcpy(outbuf + length, warning, log_buffer_size - length);
         assert(length < log_buffer_size);

         break;
      }
   }

   /* done with var. args */
   va_end(ap);

   assert(length < log_buffer_size);
   length += strlcpy(outbuf + length, "\n", log_buffer_size - length);

   /* Some sanity checks */
   if ((length >= log_buffer_size)
    || (outbuf[log_buffer_size-1] != '\0')
    || (outbuf[log_buffer_size] != '\0')
      )
   {
      /* Repeat as assertions */
      assert(length < log_buffer_size);
      assert(outbuf[log_buffer_size-1] == '\0');
      /*
       * outbuf's real size is log_buffer_size+1,
       * so while this looks like an off-by-one,
       * we're only checking our paranoia byte.
       */
      assert(outbuf[log_buffer_size] == '\0');

      snprintf(outbuf, log_buffer_size,
         "%s %08lx Fatal error: log_error()'s sanity checks failed."
         "length: %d. Exiting.",
         timestamp, thread_id, (int)length);
      loglevel = LOG_LEVEL_FATAL;
   }

#ifndef _WIN32
   /*
    * On Windows this is acceptable in case
    * we are logging to the GUI window only.
    */
   assert(NULL != logfp);
#endif

   if (loglevel == LOG_LEVEL_FATAL)
   {
      fatal_error(outbuf_save);
      /* Never get here */
   }
   if (logfp != NULL)
   {
      fputs(outbuf_save, logfp);
   }

#if defined(_WIN32) && !defined(_WIN_CONSOLE)
   /* Write to display */
   LogPutString(outbuf_save);
#endif /* defined(_WIN32) && !defined(_WIN_CONSOLE) */

   unlock_logfile();

}


/*********************************************************************
 *
 * Function    :  jb_err_to_string
 *
 * Description :  Translates JB_ERR_FOO codes into strings.
 *
 *                XXX: the type of error codes is jb_err
 *                but the typedef'inition is currently not
 *                visible to all files that include errlog.h.
 *
 * Parameters  :
 *          1  :  jb_error = a valid jb_err code
 *
 * Returns     :  A string with the jb_err translation
 *
 *********************************************************************/
const char *jb_err_to_string(int jb_error)
{
   switch (jb_error)
   {
      case JB_ERR_OK:
         return "Success, no error";
      case JB_ERR_MEMORY:
         return "Out of memory";
      case JB_ERR_CGI_PARAMS:
         return "Missing or corrupt CGI parameters";
      case JB_ERR_FILE:
         return "Error opening, reading or writing a file";
      case JB_ERR_PARSE:
         return "Parse error";
      case JB_ERR_MODIFIED:
         return "File has been modified outside of the CGI actions editor.";
      case JB_ERR_COMPRESS:
         return "(De)compression failure";
      default:
         assert(0);
         return "Unknown error";
   }
   assert(0);
   return "Internal error";
}

#ifdef _WIN32
/*********************************************************************
 *
 * Function    :  w32_socket_strerr
 *
 * Description :  Translate the return value from WSAGetLastError()
 *                into a string.
 *
 * Parameters  :
 *          1  :  errcode = The return value from WSAGetLastError().
 *          2  :  tmp_buf = A temporary buffer that might be used to
 *                          store the string.
 *
 * Returns     :  String representing the error code.  This may be
 *                a global string constant or a string stored in
 *                tmp_buf.
 *
 *********************************************************************/
static char *w32_socket_strerr(int errcode, char *tmp_buf)
{
#define TEXT_FOR_ERROR(code,text) \
   if (errcode == code)           \
   {                              \
      return #code " - " text;    \
   }

   TEXT_FOR_ERROR(WSAEACCES, "Permission denied")
   TEXT_FOR_ERROR(WSAEADDRINUSE, "Address already in use.")
   TEXT_FOR_ERROR(WSAEADDRNOTAVAIL, "Cannot assign requested address.");
   TEXT_FOR_ERROR(WSAEAFNOSUPPORT, "Address family not supported by protocol family.");
   TEXT_FOR_ERROR(WSAEALREADY, "Operation already in progress.");
   TEXT_FOR_ERROR(WSAECONNABORTED, "Software caused connection abort.");
   TEXT_FOR_ERROR(WSAECONNREFUSED, "Connection refused.");
   TEXT_FOR_ERROR(WSAECONNRESET, "Connection reset by peer.");
   TEXT_FOR_ERROR(WSAEDESTADDRREQ, "Destination address required.");
   TEXT_FOR_ERROR(WSAEFAULT, "Bad address.");
   TEXT_FOR_ERROR(WSAEHOSTDOWN, "Host is down.");
   TEXT_FOR_ERROR(WSAEHOSTUNREACH, "No route to host.");
   TEXT_FOR_ERROR(WSAEINPROGRESS, "Operation now in progress.");
   TEXT_FOR_ERROR(WSAEINTR, "Interrupted function call.");
   TEXT_FOR_ERROR(WSAEINVAL, "Invalid argument.");
   TEXT_FOR_ERROR(WSAEISCONN, "Socket is already connected.");
   TEXT_FOR_ERROR(WSAEMFILE, "Too many open sockets.");
   TEXT_FOR_ERROR(WSAEMSGSIZE, "Message too long.");
   TEXT_FOR_ERROR(WSAENETDOWN, "Network is down.");
   TEXT_FOR_ERROR(WSAENETRESET, "Network dropped connection on reset.");
   TEXT_FOR_ERROR(WSAENETUNREACH, "Network is unreachable.");
   TEXT_FOR_ERROR(WSAENOBUFS, "No buffer space available.");
   TEXT_FOR_ERROR(WSAENOPROTOOPT, "Bad protocol option.");
   TEXT_FOR_ERROR(WSAENOTCONN, "Socket is not connected.");
   TEXT_FOR_ERROR(WSAENOTSOCK, "Socket operation on non-socket.");
   TEXT_FOR_ERROR(WSAEOPNOTSUPP, "Operation not supported.");
   TEXT_FOR_ERROR(WSAEPFNOSUPPORT, "Protocol family not supported.");
   TEXT_FOR_ERROR(WSAEPROCLIM, "Too many processes.");
   TEXT_FOR_ERROR(WSAEPROTONOSUPPORT, "Protocol not supported.");
   TEXT_FOR_ERROR(WSAEPROTOTYPE, "Protocol wrong type for socket.");
   TEXT_FOR_ERROR(WSAESHUTDOWN, "Cannot send after socket shutdown.");
   TEXT_FOR_ERROR(WSAESOCKTNOSUPPORT, "Socket type not supported.");
   TEXT_FOR_ERROR(WSAETIMEDOUT, "Connection timed out.");
   TEXT_FOR_ERROR(WSAEWOULDBLOCK, "Resource temporarily unavailable.");
   TEXT_FOR_ERROR(WSAHOST_NOT_FOUND, "Host not found.");
   TEXT_FOR_ERROR(WSANOTINITIALISED, "Successful WSAStartup not yet performed.");
   TEXT_FOR_ERROR(WSANO_DATA, "Valid name, no data record of requested type.");
   TEXT_FOR_ERROR(WSANO_RECOVERY, "This is a non-recoverable error.");
   TEXT_FOR_ERROR(WSASYSNOTREADY, "Network subsystem is unavailable.");
   TEXT_FOR_ERROR(WSATRY_AGAIN, "Non-authoritative host not found.");
   TEXT_FOR_ERROR(WSAVERNOTSUPPORTED, "WINSOCK.DLL version out of range.");
   TEXT_FOR_ERROR(WSAEDISCON, "Graceful shutdown in progress.");
   /*
    * The following error codes are documented in the Microsoft WinSock
    * reference guide, but don't actually exist.
    *
    * TEXT_FOR_ERROR(WSA_INVALID_HANDLE, "Specified event object handle is invalid.");
    * TEXT_FOR_ERROR(WSA_INVALID_PARAMETER, "One or more parameters are invalid.");
    * TEXT_FOR_ERROR(WSAINVALIDPROCTABLE, "Invalid procedure table from service provider.");
    * TEXT_FOR_ERROR(WSAINVALIDPROVIDER, "Invalid service provider version number.");
    * TEXT_FOR_ERROR(WSA_IO_PENDING, "Overlapped operations will complete later.");
    * TEXT_FOR_ERROR(WSA_IO_INCOMPLETE, "Overlapped I/O event object not in signaled state.");
    * TEXT_FOR_ERROR(WSA_NOT_ENOUGH_MEMORY, "Insufficient memory available.");
    * TEXT_FOR_ERROR(WSAPROVIDERFAILEDINIT, "Unable to initialize a service provider.");
    * TEXT_FOR_ERROR(WSASYSCALLFAILURE, "System call failure.");
    * TEXT_FOR_ERROR(WSA_OPERATION_ABORTED, "Overlapped operation aborted.");
    */

   sprintf(tmp_buf, "(error number %d)", errcode);
   return tmp_buf;
}
#endif /* def _WIN32 */


#ifdef __OS2__
/*********************************************************************
 *
 * Function    :  os2_socket_strerr
 *
 * Description :  Translate the return value from sock_errno()
 *                into a string.
 *
 * Parameters  :
 *          1  :  errcode = The return value from sock_errno().
 *          2  :  tmp_buf = A temporary buffer that might be used to
 *                          store the string.
 *
 * Returns     :  String representing the error code.  This may be
 *                a global string constant or a string stored in
 *                tmp_buf.
 *
 *********************************************************************/
static char *os2_socket_strerr(int errcode, char *tmp_buf)
{
#define TEXT_FOR_ERROR(code,text) \
   if (errcode == code)           \
   {                              \
      return #code " - " text;    \
   }

   TEXT_FOR_ERROR(SOCEPERM          , "Not owner.")
   TEXT_FOR_ERROR(SOCESRCH          , "No such process.")
   TEXT_FOR_ERROR(SOCEINTR          , "Interrupted system call.")
   TEXT_FOR_ERROR(SOCENXIO          , "No such device or address.")
   TEXT_FOR_ERROR(SOCEBADF          , "Bad file number.")
   TEXT_FOR_ERROR(SOCEACCES         , "Permission denied.")
   TEXT_FOR_ERROR(SOCEFAULT         , "Bad address.")
   TEXT_FOR_ERROR(SOCEINVAL         , "Invalid argument.")
   TEXT_FOR_ERROR(SOCEMFILE         , "Too many open files.")
   TEXT_FOR_ERROR(SOCEPIPE          , "Broken pipe.")
   TEXT_FOR_ERROR(SOCEWOULDBLOCK    , "Operation would block.")
   TEXT_FOR_ERROR(SOCEINPROGRESS    , "Operation now in progress.")
   TEXT_FOR_ERROR(SOCEALREADY       , "Operation already in progress.")
   TEXT_FOR_ERROR(SOCENOTSOCK       , "Socket operation on non-socket.")
   TEXT_FOR_ERROR(SOCEDESTADDRREQ   , "Destination address required.")
   TEXT_FOR_ERROR(SOCEMSGSIZE       , "Message too long.")
   TEXT_FOR_ERROR(SOCEPROTOTYPE     , "Protocol wrong type for socket.")
   TEXT_FOR_ERROR(SOCENOPROTOOPT    , "Protocol not available.")
   TEXT_FOR_ERROR(SOCEPROTONOSUPPORT, "Protocol not supported.")
   TEXT_FOR_ERROR(SOCESOCKTNOSUPPORT, "Socket type not supported.")
   TEXT_FOR_ERROR(SOCEOPNOTSUPP     , "Operation not supported.")
   TEXT_FOR_ERROR(SOCEPFNOSUPPORT   , "Protocol family not supported.")
   TEXT_FOR_ERROR(SOCEAFNOSUPPORT   , "Address family not supported by protocol family.")
   TEXT_FOR_ERROR(SOCEADDRINUSE     , "Address already in use.")
   TEXT_FOR_ERROR(SOCEADDRNOTAVAIL  , "Can't assign requested address.")
   TEXT_FOR_ERROR(SOCENETDOWN       , "Network is down.")
   TEXT_FOR_ERROR(SOCENETUNREACH    , "Network is unreachable.")
   TEXT_FOR_ERROR(SOCENETRESET      , "Network dropped connection on reset.")
   TEXT_FOR_ERROR(SOCECONNABORTED   , "Software caused connection abort.")
   TEXT_FOR_ERROR(SOCECONNRESET     , "Connection reset by peer.")
   TEXT_FOR_ERROR(SOCENOBUFS        , "No buffer space available.")
   TEXT_FOR_ERROR(SOCEISCONN        , "Socket is already connected.")
   TEXT_FOR_ERROR(SOCENOTCONN       , "Socket is not connected.")
   TEXT_FOR_ERROR(SOCESHUTDOWN      , "Can't send after socket shutdown.")
   TEXT_FOR_ERROR(SOCETOOMANYREFS   , "Too many references: can't splice.")
   TEXT_FOR_ERROR(SOCETIMEDOUT      , "Operation timed out.")
   TEXT_FOR_ERROR(SOCECONNREFUSED   , "Connection refused.")
   TEXT_FOR_ERROR(SOCELOOP          , "Too many levels of symbolic links.")
   TEXT_FOR_ERROR(SOCENAMETOOLONG   , "File name too long.")
   TEXT_FOR_ERROR(SOCEHOSTDOWN      , "Host is down.")
   TEXT_FOR_ERROR(SOCEHOSTUNREACH   , "No route to host.")
   TEXT_FOR_ERROR(SOCENOTEMPTY      , "Directory not empty.")
   TEXT_FOR_ERROR(SOCEOS2ERR        , "OS/2 Error.")

   sprintf(tmp_buf, "(error number %d)", errcode);
   return tmp_buf;
}
#endif /* def __OS2__ */


/*
  Local Variables:
  tab-width: 3
  end:
*/
