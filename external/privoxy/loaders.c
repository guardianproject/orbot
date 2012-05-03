const char loaders_rcs[] = "$Id: loaders.c,v 1.71 2009/03/04 18:24:47 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/loaders.c,v $
 *
 * Purpose     :  Functions to load and unload the various
 *                configuration files.  Also contains code to manage
 *                the list of active loaders, and to automatically
 *                unload files that are no longer in use.
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
 *    $Log: loaders.c,v $
 *    Revision 1.71  2009/03/04 18:24:47  fabiankeil
 *    No need to create empty strings manually, strdup("") FTW.
 *
 *    Revision 1.70  2009/03/01 18:34:24  fabiankeil
 *    Help clang understand that we aren't dereferencing
 *    NULL pointers here.
 *
 *    Revision 1.69  2008/09/21 13:36:52  fabiankeil
 *    If change-x-forwarded-for{add} is used and the client
 *    sends multiple X-Forwarded-For headers, append the client's
 *    IP address to each one of them. "Traditionally" we would
 *    lose all but the last one.
 *
 *    Revision 1.68  2008/09/19 15:26:28  fabiankeil
 *    Add change-x-forwarded-for{} action to block or add
 *    X-Forwarded-For headers. Mostly based on code removed
 *    before 3.0.7.
 *
 *    Revision 1.67  2008/03/30 14:52:08  fabiankeil
 *    Rename load_actions_file() and load_re_filterfile()
 *    as they load multiple files "now".
 *
 *    Revision 1.66  2008/03/21 11:16:30  fabiankeil
 *    Garbage-collect csp->my_ip_addr_str and csp->my_hostname.
 *
 *    Revision 1.65  2007/12/07 18:29:23  fabiankeil
 *    Remove now-obsolete csp member x_forwarded.
 *
 *    Revision 1.64  2007/06/01 14:12:38  fabiankeil
 *    Add unload_forward_spec() in preparation for forward-override{}.
 *
 *    Revision 1.63  2007/05/14 10:41:15  fabiankeil
 *    Ditch the csp member cookie_list[] which isn't used anymore.
 *
 *    Revision 1.62  2007/04/30 15:02:18  fabiankeil
 *    Introduce dynamic pcrs jobs that can resolve variables.
 *
 *    Revision 1.61  2007/04/15 16:39:21  fabiankeil
 *    Introduce tags as alternative way to specify which
 *    actions apply to a request. At the moment tags can be
 *    created based on client and server headers.
 *
 *    Revision 1.60  2007/03/20 15:16:34  fabiankeil
 *    Use dedicated header filter actions instead of abusing "filter".
 *    Replace "filter-client-headers" and "filter-client-headers"
 *    with "server-header-filter" and "client-header-filter".
 *
 *    Revision 1.59  2007/01/25 13:38:20  fabiankeil
 *    Freez csp->error_message in sweep().
 *
 *    Revision 1.58  2006/12/31 14:25:20  fabiankeil
 *    Fix gcc43 compiler warnings.
 *
 *    Revision 1.57  2006/12/21 12:22:22  fabiankeil
 *    html_encode filter descriptions.
 *
 *    Have "Ignoring job ..." error messages
 *    print the filter file name correctly.
 *
 *    Revision 1.56  2006/09/07 10:40:30  fabiankeil
 *    Turns out trusted referrers above our arbitrary
 *    limit are downgraded too ordinary trusted URLs.
 *    Adjusted error message.
 *
 *    Revision 1.55  2006/09/07 10:25:39  fabiankeil
 *    Fix typo.
 *
 *    Revision 1.54  2006/09/07 10:22:20  fabiankeil
 *    If too many trusted referrers are used,
 *    print only one error message instead of logging
 *    every single trusted referrer above the arbitrary
 *    limit.
 *
 *    Revision 1.53  2006/08/31 16:25:06  fabiankeil
 *    Work around a buffer overflow that caused Privoxy to
 *    segfault if too many trusted referrers were used. Good
 *    enough for now, but should be replaced with a real
 *    solution after the next release.
 *
 *    Revision 1.52  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.50.2.8  2006/01/30 15:16:25  david__schmidt
 *    Remove a little residual debugging info
 *
 *    Revision 1.50.2.7  2006/01/29 23:10:56  david__schmidt
 *    Multiple filter file support
 *
 *    Revision 1.50.2.6  2003/10/24 10:17:54  oes
 *    Nit: Allowed tabs as separators in filter headings
 *
 *    Revision 1.50.2.5  2003/05/08 15:19:15  oes
 *    sweep: Made loop structure of sweep step mirror that of mark step
 *
 *    Revision 1.50.2.4  2003/05/06 15:57:12  oes
 *    Bugfix: Update last_active pointer in sweep() before
 *    leaving an active client. Closes bugs #724395, #727882
 *
 *    Revision 1.50.2.3  2002/11/20 17:12:30  oes
 *    Ooops, forgot one change.
 *
 *    Revision 1.50.2.2  2002/11/20 14:38:15  oes
 *    Fixed delayed/incomplete freeing of client resources and
 *    simplified loop structure in sweep.
 *    Thanks to Oliver Stoeneberg for the hint.
 *
 *    Revision 1.50.2.1  2002/07/26 15:19:24  oes
 *    - PCRS jobs now chained in order of appearance. Previous
 *      reverse chaining was counter-intuitive.
 *    - Changed loglevel of PCRS job compile errors to
 *      LOG_LEVEL_ERROR
 *
 *    Revision 1.50  2002/04/24 02:12:16  oes
 *    Jon's multiple AF patch: Sweep now takes care of all AFs
 *
 *    Revision 1.49  2002/04/19 16:53:25  jongfoster
 *    Optimize away a function call by using an equivalent macro
 *
 *    Revision 1.48  2002/04/05 00:56:09  gliptak
 *    Correcting typo to clean up on realloc failure
 *
 *    Revision 1.47  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.46  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.45  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.44  2002/03/16 21:51:00  jongfoster
 *    Fixing free(NULL).
 *
 *    Revision 1.43  2002/03/16 20:28:34  oes
 *    Added descriptions to the filters so users will know what they select in the cgi editor
 *
 *    Revision 1.42  2002/03/13 00:27:05  jongfoster
 *    Killing warnings
 *
 *    Revision 1.41  2002/03/12 01:42:50  oes
 *    Introduced modular filters
 *
 *    Revision 1.40  2002/03/08 17:46:04  jongfoster
 *    Fixing int/size_t warnings
 *
 *    Revision 1.39  2002/03/07 03:46:17  oes
 *    Fixed compiler warnings
 *
 *    Revision 1.38  2002/03/06 22:54:35  jongfoster
 *    Automated function-comment nitpicking.
 *
 *    Revision 1.37  2002/03/03 15:07:49  oes
 *    Re-enabled automatic config reloading
 *
 *    Revision 1.36  2002/01/22 23:46:18  jongfoster
 *    Moving edit_read_line() and simple_read_line() to loaders.c, and
 *    extending them to support reading MS-DOS, Mac and UNIX style files
 *    on all platforms.
 *
 *    Modifying read_config_line() (without changing it's prototype) to
 *    be a trivial wrapper for edit_read_line().  This means that we have
 *    one function to read a line and handle comments, which is common
 *    between the initialization code and the edit interface.
 *
 *    Revision 1.35  2002/01/17 21:03:08  jongfoster
 *    Moving all our URL and URL pattern parsing code to urlmatch.c.
 *
 *    Renaming free_url to free_url_spec, since it frees a struct url_spec.
 *
 *    Revision 1.34  2001/12/30 14:07:32  steudten
 *    - Add signal handling (unix)
 *    - Add SIGHUP handler (unix)
 *    - Add creation of pidfile (unix)
 *    - Add action 'top' in rc file (RH)
 *    - Add entry 'SIGNALS' to manpage
 *    - Add exit message to logfile (unix)
 *
 *    Revision 1.33  2001/11/13 00:16:38  jongfoster
 *    Replacing references to malloc.h with the standard stdlib.h
 *    (See ANSI or K&R 2nd Ed)
 *
 *    Revision 1.32  2001/11/07 00:02:13  steudten
 *    Add line number in error output for lineparsing for
 *    actionsfile and configfile.
 *    Special handling for CLF added.
 *
 *    Revision 1.31  2001/10/26 17:39:01  oes
 *    Removed csp->referrer
 *    Moved ijb_isspace and ijb_tolower to project.h
 *
 *    Revision 1.30  2001/10/25 03:40:48  david__schmidt
 *    Change in porting tactics: OS/2's EMX porting layer doesn't allow multiple
 *    threads to call select() simultaneously.  So, it's time to do a real, live,
 *    native OS/2 port.  See defines for __EMX__ (the porting layer) vs. __OS2__
 *    (native). Both versions will work, but using __OS2__ offers multi-threading.
 *
 *    Revision 1.29  2001/10/23 21:38:53  jongfoster
 *    Adding error-checking to create_url_spec()
 *
 *    Revision 1.28  2001/10/07 15:40:39  oes
 *    Replaced 6 boolean members of csp with one bitmap (csp->flags)
 *
 *    Revision 1.27  2001/09/22 16:36:59  jongfoster
 *    Removing unused parameter fs from read_config_line()
 *
 *    Revision 1.26  2001/09/22 14:05:22  jongfoster
 *    Bugfix: Multiple escaped "#" characters in a configuration
 *    file are now permitted.
 *    Also removing 3 unused headers.
 *
 *    Revision 1.25  2001/09/13 22:44:03  jongfoster
 *    Adding {} to an if statement
 *
 *    Revision 1.24  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.23  2001/07/20 15:51:54  oes
 *    Fixed indentation of prepocessor commands
 *
 *    Revision 1.22  2001/07/20 15:16:17  haroon
 *    - per Guy's suggestion, added a while loop in sweep() to catch not just
 *      the last inactive CSP but all other consecutive inactive CSPs after that
 *      as well
 *
 *    Revision 1.21  2001/07/18 17:26:24  oes
 *    Changed to conform to new pcrs interface
 *
 *    Revision 1.20  2001/07/17 13:07:01  oes
 *    Fixed segv when last line in config files
 *     lacked a terminating (\r)\n
 *
 *    Revision 1.19  2001/07/13 14:01:54  oes
 *    Removed all #ifdef PCRS
 *
 *    Revision 1.18  2001/06/29 21:45:41  oes
 *    Indentation, CRLF->LF, Tab-> Space
 *
 *    Revision 1.17  2001/06/29 13:31:51  oes
 *    Various adaptions
 *
 *    Revision 1.16  2001/06/09 10:55:28  jongfoster
 *    Changing BUFSIZ ==> BUFFER_SIZE
 *
 *    Revision 1.15  2001/06/07 23:14:14  jongfoster
 *    Removing ACL and forward file loaders - these
 *    files have been merged into the config file.
 *    Cosmetic: Moving unloader funcs next to their
 *    respective loader funcs
 *
 *    Revision 1.14  2001/06/01 03:27:04  oes
 *    Fixed line continuation problem
 *
 *    Revision 1.13  2001/05/31 21:28:49  jongfoster
 *    Removed all permissionsfile code - it's now called the actions
 *    file, and (almost) all the code is in actions.c
 *
 *    Revision 1.12  2001/05/31 17:32:31  oes
 *
 *     - Enhanced domain part globbing with infix and prefix asterisk
 *       matching and optional unanchored operation
 *
 *    Revision 1.11  2001/05/29 23:25:24  oes
 *
 *     - load_config_line() and load_permissions_file() now use chomp()
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
 *    Revision 1.9  2001/05/26 17:12:07  jongfoster
 *    Fatal errors loading configuration files now give better error messages.
 *
 *    Revision 1.8  2001/05/26 00:55:20  jongfoster
 *    Removing duplicated code.  load_forwardfile() now uses create_url_spec()
 *
 *    Revision 1.7  2001/05/26 00:28:36  jongfoster
 *    Automatic reloading of config file.
 *    Removed obsolete SIGHUP support (Unix) and Reload menu option (Win32).
 *    Most of the global variables have been moved to a new
 *    struct configuration_spec, accessed through csp->config->globalname
 *    Most of the globals remaining are used by the Win32 GUI.
 *
 *    Revision 1.6  2001/05/23 12:27:33  oes
 *
 *    Fixed ugly indentation of my last changes
 *
 *    Revision 1.5  2001/05/23 10:39:05  oes
 *    - Added support for escaping the comment character
 *      in config files by a backslash
 *    - Added support for line continuation in config
 *      files
 *    - Fixed a buffer overflow bug with long config lines
 *
 *    Revision 1.4  2001/05/22 18:56:28  oes
 *    CRLF -> LF
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
 *    Revision 1.1.1.1  2001/05/15 13:58:59  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "config.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include <sys/stat.h>
#include <ctype.h>
#include <assert.h>

#if !defined(_WIN32) && !defined(__OS2__)
#include <unistd.h>
#endif

#include "project.h"
#include "list.h"
#include "loaders.h"
#include "filters.h"
#include "parsers.h"
#include "jcc.h"
#include "miscutil.h"
#include "errlog.h"
#include "actions.h"
#include "urlmatch.h"
#include "encode.h"

const char loaders_h_rcs[] = LOADERS_H_VERSION;

/*
 * Currently active files.
 * These are also entered in the main linked list of files.
 */

#ifdef FEATURE_TRUST
static struct file_list *current_trustfile      = NULL;
#endif /* def FEATURE_TRUST */

static int load_one_re_filterfile(struct client_state *csp, int fileid);

static struct file_list *current_re_filterfile[MAX_AF_FILES]  = {
   NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL
};

/*
 * Pseudo filter type for load_one_re_filterfile
 */
#define NO_NEW_FILTER -1


/*********************************************************************
 *
 * Function    :  sweep
 *
 * Description :  Basically a mark and sweep garbage collector, it is run
 *                (by the parent thread) every once in a while to reclaim memory.
 *
 * It uses a mark and sweep strategy:
 *   1) mark all files as inactive
 *
 *   2) check with each client:
 *       if it is active,   mark its files as active
 *       if it is inactive, free its resources
 *
 *   3) free the resources of all of the files that
 *      are still marked as inactive (and are obsolete).
 *
 *   N.B. files that are not obsolete don't have an unloader defined.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void sweep(void)
{
   struct file_list *fl, *nfl;
   struct client_state *csp, *last_active;
   int i;

   /* clear all of the file's active flags */
   for ( fl = files->next; NULL != fl; fl = fl->next )
   {
      fl->active = 0;
   }

   last_active = clients;
   csp = clients->next;

   while (NULL != csp)
   {
      if (csp->flags & CSP_FLAG_ACTIVE)
      {
         /* Mark this client's files as active */

         /*
          * Always have a configuration file.
          * (Also note the slightly non-standard extra
          * indirection here.)
          */
         csp->config->config_file_list->active = 1;

         /* 
          * Actions files
          */
         for (i = 0; i < MAX_AF_FILES; i++)
         {
            if (csp->actions_list[i])     
            {
               csp->actions_list[i]->active = 1;
            }
         }

         /*
          * Filter files
          */
         for (i = 0; i < MAX_AF_FILES; i++)
         {
            if (csp->rlist[i])     
            {
               csp->rlist[i]->active = 1;
            }
         }

         /*
          * Trust file
          */
#ifdef FEATURE_TRUST
         if (csp->tlist)
         {
            csp->tlist->active = 1;
         }
#endif /* def FEATURE_TRUST */
         
         last_active = csp;
         csp = csp->next;

      }
      else 
      /*
       * This client is not active. Free its resources.
       */
      {
         last_active->next = csp->next;

         freez(csp->ip_addr_str);
         freez(csp->iob->buf);
         freez(csp->error_message);

         if (csp->action->flags & ACTION_FORWARD_OVERRIDE &&
             NULL != csp->fwd)
         {
            unload_forward_spec(csp->fwd);
         }
         free_http_request(csp->http);

         destroy_list(csp->headers);
         destroy_list(csp->tags);

         free_current_action(csp->action);

#ifdef FEATURE_STATISTICS
         urls_read++;
         if (csp->flags & CSP_FLAG_REJECTED)
         {
            urls_rejected++;
         }
#endif /* def FEATURE_STATISTICS */

         freez(csp);
         
         csp = last_active->next;
      }
   }

   nfl = files;
   fl = files->next;

   while (fl != NULL)
   {
      if ( ( 0 == fl->active ) && ( NULL != fl->unloader ) )
      {
         nfl->next = fl->next;

         (fl->unloader)(fl->f);

         freez(fl->filename);
         freez(fl);

         fl = nfl->next;
      }
      else
      {
         nfl = fl;
         fl = fl->next;
      }
   }

}


/*********************************************************************
 *
 * Function    :  check_file_changed
 *
 * Description :  Helper function to check if a file needs reloading.
 *                If "current" is still current, return it.  Otherwise
 *                allocates a new (zeroed) "struct file_list", fills
 *                in the disk file name and timestamp, and returns it.
 *
 * Parameters  :
 *          1  :  current = The file_list currently being used - will
 *                          be checked to see if it is out of date.
 *                          May be NULL (which is treated as out of
 *                          date).
 *          2  :  filename = Name of file to check.
 *          3  :  newfl    = New file list. [Output only]
 *                           This will be set to NULL, OR a struct
 *                           file_list newly allocated on the
 *                           heap, with the filename and lastmodified
 *                           fields filled, and all others zeroed.
 *
 * Returns     :  If file unchanged: 0 (and sets newfl == NULL)
 *                If file changed: 1 and sets newfl != NULL
 *                On error: 1 and sets newfl == NULL
 *
 *********************************************************************/
int check_file_changed(const struct file_list * current,
                       const char * filename,
                       struct file_list ** newfl)
{
   struct file_list *fs;
   struct stat statbuf[1];

   *newfl = NULL;

   if (stat(filename, statbuf) < 0)
   {
      /* Error, probably file not found. */
      return 1;
   }

   if (current
       && (current->lastmodified == statbuf->st_mtime)
       && (0 == strcmp(current->filename, filename)))
   {
      return 0;
   }

   fs = (struct file_list *)zalloc(sizeof(struct file_list));
   if (fs == NULL)
   {
      /* Out of memory error */
      return 1;
   }


   fs->filename = strdup(filename);
   fs->lastmodified = statbuf->st_mtime;

   if (fs->filename == NULL)
   {
      /* Out of memory error */
      freez (fs);
      return 1;
   }
   *newfl = fs;
   return 1;
}


/*********************************************************************
 *
 * Function    :  simple_read_line
 *
 * Description :  Read a single line from a file and return it.
 *                This is basically a version of fgets() that malloc()s
 *                it's own line buffer.  Note that the buffer will
 *                always be a multiple of BUFFER_SIZE bytes long.
 *                Therefore if you are going to keep the string for
 *                an extended period of time, you should probably
 *                strdup() it and free() the original, to save memory.
 *
 *
 * Parameters  :
 *          1  :  dest = destination for newly malloc'd pointer to
 *                line data.  Will be set to NULL on error.
 *          2  :  fp = File to read from
 *          3  :  newline = Standard for newlines in the file.
 *                Will be unchanged if it's value on input is not
 *                NEWLINE_UNKNOWN.
 *                On output, may be changed from NEWLINE_UNKNOWN to
 *                actual convention in file.
 *
 * Returns     :  JB_ERR_OK     on success
 *                JB_ERR_MEMORY on out-of-memory
 *                JB_ERR_FILE   on EOF.
 *
 *********************************************************************/
jb_err simple_read_line(FILE *fp, char **dest, int *newline)
{
   size_t len = 0;
   size_t buflen = BUFFER_SIZE;
   char * buf;
   char * p;
   int ch;
   int realnewline = NEWLINE_UNKNOWN;

   if (NULL == (buf = malloc(buflen)))
   {
      return JB_ERR_MEMORY;
   }

   p = buf;

/*
 * Character codes.  If you have a wierd compiler and the following are
 * incorrect, you also need to fix NEWLINE() in loaders.h
 */
#define CHAR_CR '\r' /* ASCII 13 */
#define CHAR_LF '\n' /* ASCII 10 */

   for (;;)
   {
      ch = getc(fp);
      if (ch == EOF)
      {
         if (len > 0)
         {
            *p = '\0';
            *dest = buf;
            return JB_ERR_OK;
         }
         else
         {
            free(buf);
            *dest = NULL;
            return JB_ERR_FILE;
         }
      }
      else if (ch == CHAR_CR)
      {
         ch = getc(fp);
         if (ch == CHAR_LF)
         {
            if (*newline == NEWLINE_UNKNOWN)
            {
               *newline = NEWLINE_DOS;
            }
         }
         else
         {
            if (ch != EOF)
            {
               ungetc(ch, fp);
            }
            if (*newline == NEWLINE_UNKNOWN)
            {
               *newline = NEWLINE_MAC;
            }
         }
         *p = '\0';
         *dest = buf;
         if (*newline == NEWLINE_UNKNOWN)
         {
            *newline = realnewline;
         }
         return JB_ERR_OK;
      }
      else if (ch == CHAR_LF)
      {
         *p = '\0';
         *dest = buf;
         if (*newline == NEWLINE_UNKNOWN)
         {
            *newline = NEWLINE_UNIX;
         }
         return JB_ERR_OK;
      }
      else if (ch == 0)
      {
         *p = '\0';
         *dest = buf;
         return JB_ERR_OK;
      }

      *p++ = (char)ch;

      if (++len >= buflen)
      {
         buflen += BUFFER_SIZE;
         if (NULL == (p = realloc(buf, buflen)))
         {
            free(buf);
            return JB_ERR_MEMORY;
         }
         buf = p;
         p = buf + len;
      }
   }
}


/*********************************************************************
 *
 * Function    :  edit_read_line
 *
 * Description :  Read a single non-empty line from a file and return
 *                it.  Trims comments, leading and trailing whitespace
 *                and respects escaping of newline and comment char.
 *                Provides the line in 2 alternative forms: raw and
 *                preprocessed.
 *                - raw is the raw data read from the file.  If the
 *                  line is not modified, then this should be written
 *                  to the new file.
 *                - prefix is any comments and blank lines that were
 *                  read from the file.  If the line is modified, then
 *                  this should be written out to the file followed
 *                  by the modified data.  (If this string is non-empty
 *                  then it will have a newline at the end).
 *                - data is the actual data that will be parsed
 *                  further by appropriate routines.
 *                On EOF, the 3 strings will all be set to NULL and
 *                0 will be returned.
 *
 * Parameters  :
 *          1  :  fp = File to read from
 *          2  :  raw_out = destination for newly malloc'd pointer to
 *                raw line data.  May be NULL if you don't want it.
 *          3  :  prefix_out = destination for newly malloc'd pointer to
 *                comments.  May be NULL if you don't want it.
 *          4  :  data_out = destination for newly malloc'd pointer to
 *                line data with comments and leading/trailing spaces
 *                removed, and line continuation performed.  May be
 *                NULL if you don't want it.
 *          5  :  newline = Standard for newlines in the file.
 *                On input, set to value to use or NEWLINE_UNKNOWN.
 *                On output, may be changed from NEWLINE_UNKNOWN to
 *                actual convention in file.  May be NULL if you
 *                don't want it.
 *          6  :  line_number = Line number in file.  In "lines" as
 *                reported by a text editor, not lines containing data.
 *
 * Returns     :  JB_ERR_OK     on success
 *                JB_ERR_MEMORY on out-of-memory
 *                JB_ERR_FILE   on EOF.
 *
 *********************************************************************/
jb_err edit_read_line(FILE *fp,
                      char **raw_out,
                      char **prefix_out,
                      char **data_out,
                      int *newline,
                      unsigned long *line_number)
{
   char *p;          /* Temporary pointer   */
   char *linebuf;    /* Line read from file */
   char *linestart;  /* Start of linebuf, usually first non-whitespace char */
   int contflag = 0; /* Nonzero for line continuation - i.e. line ends '\' */
   int is_empty = 1; /* Flag if not got any data yet */
   char *raw    = NULL; /* String to be stored in raw_out    */
   char *prefix = NULL; /* String to be stored in prefix_out */
   char *data   = NULL; /* String to be stored in data_out   */
   int scrapnewline;    /* Used for (*newline) if newline==NULL */
   jb_err rval = JB_ERR_OK;

   assert(fp);
   assert(raw_out || data_out);
   assert(newline == NULL
       || *newline == NEWLINE_UNKNOWN
       || *newline == NEWLINE_UNIX
       || *newline == NEWLINE_DOS
       || *newline == NEWLINE_MAC);

   if (newline == NULL)
   {
      scrapnewline = NEWLINE_UNKNOWN;
      newline = &scrapnewline;
   }

   /* Set output parameters to NULL */
   if (raw_out)
   {
      *raw_out    = NULL;
   }
   if (prefix_out)
   {
      *prefix_out = NULL;
   }
   if (data_out)
   {
      *data_out   = NULL;
   }

   /* Set string variables to new, empty strings. */

   if (raw_out)
   {
      raw = strdup("");
      if (NULL == raw)
      {
         return JB_ERR_MEMORY;
      }
   }
   if (prefix_out)
   {
      prefix = strdup("");
      if (NULL == prefix)
      {
         freez(raw);
         return JB_ERR_MEMORY;
      }
   }
   if (data_out)
   {
      data = strdup("");
      if (NULL == data)
      {
         freez(raw);
         freez(prefix);
         return JB_ERR_MEMORY;
      }
   }

   /* Main loop.  Loop while we need more data & it's not EOF. */

   while ( (contflag || is_empty)
        && (JB_ERR_OK == (rval = simple_read_line(fp, &linebuf, newline))))
   {
      if (line_number)
      {
         (*line_number)++;
      }
      if (raw)
      {
         string_append(&raw,linebuf);
         if (string_append(&raw,NEWLINE(*newline)))
         {
            freez(prefix);
            freez(data);
            free(linebuf);
            return JB_ERR_MEMORY;
         }
      }

      /* Line continuation? Trim escape and set flag. */
      p = linebuf + strlen(linebuf) - 1;
      contflag = ((*linebuf != '\0') && (*p == '\\'));
      if (contflag)
      {
         *p = '\0';
      }

      /* Trim leading spaces if we're at the start of the line */
      linestart = linebuf;
      assert(NULL != data);
      if (*data == '\0')
      {
         /* Trim leading spaces */
         while (*linestart && isspace((int)(unsigned char)*linestart))
         {
            linestart++;
         }
      }

      /* Handle comment characters. */
      p = linestart;
      while ((p = strchr(p, '#')) != NULL)
      {
         /* Found a comment char.. */
         if ((p != linebuf) && (*(p-1) == '\\'))
         {
            /* ..and it's escaped, left-shift the line over the escape. */
            char *q = p - 1;
            while ((*q = *(q + 1)) != '\0')
            {
               q++;
            }
            /* Now scan from just after the "#". */
         }
         else
         {
            /* Real comment.  Save it... */
            if (p == linestart)
            {
               /* Special case:  Line only contains a comment, so all the
                * previous whitespace is considered part of the comment.
                * Undo the whitespace skipping, if any.
                */
               linestart = linebuf;
               p = linestart;
            }
            if (prefix)
            {
               string_append(&prefix,p);
               if (string_append(&prefix, NEWLINE(*newline)))
               {
                  freez(raw);
                  freez(data);
                  free(linebuf);
                  return JB_ERR_MEMORY;
               }
            }

            /* ... and chop off the rest of the line */
            *p = '\0';
         }
      } /* END while (there's a # character) */

      /* Write to the buffer */
      if (*linestart)
      {
         is_empty = 0;
         if (data)
         {
            if (string_append(&data, linestart))
            {
               freez(raw);
               freez(prefix);
               free(linebuf);
               return JB_ERR_MEMORY;
            }
         }
      }

      free(linebuf);
   } /* END while(we need more data) */

   /* Handle simple_read_line() errors - ignore EOF */
   if ((rval != JB_ERR_OK) && (rval != JB_ERR_FILE))
   {
      freez(raw);
      freez(prefix);
      freez(data);
      return rval;
   }

   if (raw ? (*raw == '\0') : is_empty)
   {
      /* EOF and no data there.  (Definition of "data" depends on whether
       * the caller cares about "raw" or just "data").
       */

      freez(raw);
      freez(prefix);
      freez(data);

      return JB_ERR_FILE;
   }
   else
   {
      /* Got at least some data */

      /* Remove trailing whitespace */
      chomp(data);

      if (raw_out)
      {
         *raw_out    = raw;
      }
      else
      {
         freez(raw);
      }
      if (prefix_out)
      {
         *prefix_out = prefix;
      }
      else
      {
         freez(prefix);
      }
      if (data_out)
      {
         *data_out   = data;
      }
      else
      {
         freez(data);
      }
      return JB_ERR_OK;
   }
}


/*********************************************************************
 *
 * Function    :  read_config_line
 *
 * Description :  Read a single non-empty line from a file and return
 *                it.  Trims comments, leading and trailing whitespace
 *                and respects escaping of newline and comment char.
 *
 * Parameters  :
 *          1  :  buf = Buffer to use.
 *          2  :  buflen = Size of buffer in bytes.
 *          3  :  fp = File to read from
 *          4  :  linenum = linenumber in file
 *
 * Returns     :  NULL on EOF or error
 *                Otherwise, returns buf.
 *
 *********************************************************************/
char *read_config_line(char *buf, size_t buflen, FILE *fp, unsigned long *linenum)
{
   jb_err err;
   char *buf2 = NULL;
   err = edit_read_line(fp, NULL, NULL, &buf2, NULL, linenum);
   if (err)
   {
      if (err == JB_ERR_MEMORY)
      {
         log_error(LOG_LEVEL_FATAL, "Out of memory loading a config file");
      }
      return NULL;
   }
   else
   {
      assert(buf2);
      assert(strlen(buf2) + 1U < buflen);
      strncpy(buf, buf2, buflen - 1);
      free(buf2);
      buf[buflen - 1] = '\0';
      return buf;
   }
}


#ifdef FEATURE_TRUST
/*********************************************************************
 *
 * Function    :  unload_trustfile
 *
 * Description :  Unloads a trustfile.
 *
 * Parameters  :
 *          1  :  f = the data structure associated with the trustfile.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void unload_trustfile(void *f)
{
   struct block_spec *cur = (struct block_spec *)f;
   struct block_spec *next;

   while (cur != NULL)
   {
      next = cur->next;

      free_url_spec(cur->url);
      free(cur);

      cur = next;
   }

}


#ifdef FEATURE_GRACEFUL_TERMINATION
/*********************************************************************
 *
 * Function    :  unload_current_trust_file
 *
 * Description :  Unloads current trust file - reset to state at
 *                beginning of program.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void unload_current_trust_file(void)
{
   if (current_trustfile)
   {
      current_trustfile->unloader = unload_trustfile;
      current_trustfile = NULL;
   }
}
#endif /* FEATURE_GRACEFUL_TERMINATION */


/*********************************************************************
 *
 * Function    :  load_trustfile
 *
 * Description :  Read and parse a trustfile and add to files list.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  0 => Ok, everything else is an error.
 *
 *********************************************************************/
int load_trustfile(struct client_state *csp)
{
   FILE *fp;

   struct block_spec *b, *bl;
   struct url_spec **tl;

   char  buf[BUFFER_SIZE], *p, *q;
   int reject, trusted;
   struct file_list *fs;
   unsigned long linenum = 0;
   int trusted_referrers = 0;

   if (!check_file_changed(current_trustfile, csp->config->trustfile, &fs))
   {
      /* No need to load */
      if (csp)
      {
         csp->tlist = current_trustfile;
      }
      return(0);
   }
   if (!fs)
   {
      goto load_trustfile_error;
   }

   fs->f = bl = (struct block_spec *)zalloc(sizeof(*bl));
   if (bl == NULL)
   {
      goto load_trustfile_error;
   }

   if ((fp = fopen(csp->config->trustfile, "r")) == NULL)
   {
      goto load_trustfile_error;
   }

   tl = csp->config->trust_list;

   while (read_config_line(buf, sizeof(buf), fp, &linenum) != NULL)
   {
      trusted = 0;
      reject  = 1;

      if (*buf == '+')
      {
         trusted = 1;
         *buf = '~';
      }

      if (*buf == '~')
      {
         reject = 0;
         p = buf;
         q = p+1;
         while ((*p++ = *q++) != '\0')
         {
            /* nop */
         }
      }

      /* skip blank lines */
      if (*buf == '\0')
      {
         continue;
      }

      /* allocate a new node */
      if ((b = zalloc(sizeof(*b))) == NULL)
      {
         fclose(fp);
         goto load_trustfile_error;
      }

      /* add it to the list */
      b->next  = bl->next;
      bl->next = b;

      b->reject = reject;

      /* Save the URL pattern */
      if (create_url_spec(b->url, buf))
      {
         fclose(fp);
         goto load_trustfile_error;
      }

      /*
       * save a pointer to URL's spec in the list of trusted URL's, too
       */
      if (trusted)
      {
         if(++trusted_referrers < MAX_TRUSTED_REFERRERS)
         {
            *tl++ = b->url;
         }
      }
   }

   if(trusted_referrers >= MAX_TRUSTED_REFERRERS) 
   {
      /*
       * FIXME: ... after Privoxy 3.0.4 is out.
       */
       log_error(LOG_LEVEL_ERROR, "Too many trusted referrers. Current limit is %d, you are using %d.\n"
          "  Additional trusted referrers are treated like ordinary trusted URLs.\n"
          "  (You can increase this limit by changing MAX_TRUSTED_REFERRERS in project.h and recompiling).",
          MAX_TRUSTED_REFERRERS, trusted_referrers);
   }

   *tl = NULL;

   fclose(fp);

   /* the old one is now obsolete */
   if (current_trustfile)
   {
      current_trustfile->unloader = unload_trustfile;
   }

   fs->next    = files->next;
   files->next = fs;
   current_trustfile = fs;

   if (csp)
   {
      csp->tlist = fs;
   }

   return(0);

load_trustfile_error:
   log_error(LOG_LEVEL_FATAL, "can't load trustfile '%s': %E",
             csp->config->trustfile);
   return(-1);

}
#endif /* def FEATURE_TRUST */


/*********************************************************************
 *
 * Function    :  unload_re_filterfile
 *
 * Description :  Unload the re_filter list by freeing all chained
 *                re_filterfile specs and their data.
 *
 * Parameters  :
 *          1  :  f = the data structure associated with the filterfile.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void unload_re_filterfile(void *f)
{
   struct re_filterfile_spec *a, *b = (struct re_filterfile_spec *)f;

   while (b != NULL)
   {
      a = b->next;

      destroy_list(b->patterns);
      pcrs_free_joblist(b->joblist);
      freez(b->name);
      freez(b->description);
      freez(b);

      b = a;
   }

   return;
}

/*********************************************************************
 *
 * Function    :  unload_forward_spec
 *
 * Description :  Unload the forward spec settings by freeing all 
 *                memory referenced by members and the memory for
 *                the spec itself.
 *
 * Parameters  :
 *          1  :  fwd = the forward spec.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void unload_forward_spec(struct forward_spec *fwd)
{
   free_url_spec(fwd->url);
   freez(fwd->gateway_host);
   freez(fwd->forward_host);
   free(fwd);

   return;
}


#ifdef FEATURE_GRACEFUL_TERMINATION
/*********************************************************************
 *
 * Function    :  unload_current_re_filterfile
 *
 * Description :  Unloads current re_filter file - reset to state at
 *                beginning of program.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void unload_current_re_filterfile(void)
{
   int i;

   for (i = 0; i < MAX_AF_FILES; i++)
   {
      if (current_re_filterfile[i])
      {
         current_re_filterfile[i]->unloader = unload_re_filterfile;
         current_re_filterfile[i] = NULL;
      }
   }
}
#endif


/*********************************************************************
 *
 * Function    :  load_re_filterfiles
 *
 * Description :  Loads all the filterfiles. 
 *                Generate a chained list of re_filterfile_spec's from
 *                the "FILTER: " blocks, compiling all their substitutions
 *                into chained lists of pcrs_job structs.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  0 => Ok, everything else is an error.
 *
 *********************************************************************/
int load_re_filterfiles(struct client_state *csp)
{
   int i;
   int result;

   for (i = 0; i < MAX_AF_FILES; i++)
   {
      if (csp->config->re_filterfile[i])
      {
         result = load_one_re_filterfile(csp, i);
         if (result)
         {
            return result;
         }
      }
      else if (current_re_filterfile[i])
      {
         current_re_filterfile[i]->unloader = unload_re_filterfile;
         current_re_filterfile[i] = NULL;
      }
   }

   return 0;
}


/*********************************************************************
 *
 * Function    :  load_one_re_filterfile
 *
 * Description :  Load a re_filterfile. 
 *                Generate a chained list of re_filterfile_spec's from
 *                the "FILTER: " blocks, compiling all their substitutions
 *                into chained lists of pcrs_job structs.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *
 * Returns     :  0 => Ok, everything else is an error.
 *
 *********************************************************************/
int load_one_re_filterfile(struct client_state *csp, int fileid)
{
   FILE *fp;

   struct re_filterfile_spec *new_bl, *bl = NULL;
   struct file_list *fs;

   char  buf[BUFFER_SIZE];
   int error;
   unsigned long linenum = 0;
   pcrs_job *dummy, *lastjob = NULL;

   /*
    * No need to reload if unchanged
    */
   if (!check_file_changed(current_re_filterfile[fileid], csp->config->re_filterfile[fileid], &fs))
   {
      if (csp)
      {
         csp->rlist[fileid] = current_re_filterfile[fileid];
      }
      return(0);
   }
   if (!fs)
   {
      goto load_re_filterfile_error;
   }

   /* 
    * Open the file or fail
    */
   if ((fp = fopen(csp->config->re_filterfile[fileid], "r")) == NULL)
   {
      goto load_re_filterfile_error;
   }

   /* 
    * Read line by line
    */
   while (read_config_line(buf, sizeof(buf), fp, &linenum) != NULL)
   {
      int new_filter = NO_NEW_FILTER;

      if (strncmp(buf, "FILTER:", 7) == 0)
      {
         new_filter = FT_CONTENT_FILTER;
      }
      else if (strncmp(buf, "SERVER-HEADER-FILTER:", 21) == 0)
      {
         new_filter = FT_SERVER_HEADER_FILTER;
      }
      else if (strncmp(buf, "CLIENT-HEADER-FILTER:", 21) == 0)
      {
         new_filter = FT_CLIENT_HEADER_FILTER;
      }
      else if (strncmp(buf, "CLIENT-HEADER-TAGGER:", 21) == 0)
      {
         new_filter = FT_CLIENT_HEADER_TAGGER;
      }
      else if (strncmp(buf, "SERVER-HEADER-TAGGER:", 21) == 0)
      {
         new_filter = FT_SERVER_HEADER_TAGGER;
      }

      /*
       * If this is the head of a new filter block, make it a
       * re_filterfile spec of its own and chain it to the list:
       */
      if (new_filter != NO_NEW_FILTER)
      {
         new_bl = (struct re_filterfile_spec  *)zalloc(sizeof(*bl));
         if (new_bl == NULL)
         {
            goto load_re_filterfile_error;
         }
         if (new_filter == FT_CONTENT_FILTER)
         {
            new_bl->name = chomp(buf + 7);
         }
         else
         {
            new_bl->name = chomp(buf + 21);
         }
         new_bl->type = new_filter;

         /*
          * If a filter description is available,
          * encode it to HTML and save it.
          */
         if (NULL != (new_bl->description = strpbrk(new_bl->name, " \t")))
         {
            *new_bl->description++ = '\0';
            new_bl->description = html_encode(chomp(new_bl->description));
            if (NULL == new_bl->description)
            {
               new_bl->description = strdup("Out of memory while encoding this filter's description to HTML");
            }
         }
         else
         {
            new_bl->description = strdup("No description available for this filter");
         }

         new_bl->name = strdup(chomp(new_bl->name));
         
         /*
          * If this is the first filter block, chain it
          * to the file_list rather than its (nonexistant)
          * predecessor
          */
         if (fs->f == NULL)
         {
            fs->f = new_bl;
         }
         else
         {
            assert(NULL != bl);
            bl->next = new_bl;
         }
         bl = new_bl;

         log_error(LOG_LEVEL_RE_FILTER, "Reading in filter \"%s\" (\"%s\")", bl->name, bl->description);

         continue;
      }

      /* 
       * Else, save the expression, make it a pcrs_job
       * and chain it into the current filter's joblist 
       */
      if (bl != NULL)
      {
         error = enlist(bl->patterns, buf);
         if (JB_ERR_MEMORY == error)
         {
            log_error(LOG_LEVEL_FATAL,
               "Out of memory while enlisting re_filter job \'%s\' for filter %s.", buf, bl->name);
         }
         assert(JB_ERR_OK == error);

         if (pcrs_job_is_dynamic(buf))
         {
            /*
             * Dynamic pattern that might contain variables
             * and has to be recompiled for every request
             */
            if (bl->joblist != NULL)
            {
                pcrs_free_joblist(bl->joblist);
                bl->joblist = NULL;
            }
            bl->dynamic = 1;
            log_error(LOG_LEVEL_RE_FILTER,
               "Adding dynamic re_filter job \'%s\' to filter %s succeeded.", buf, bl->name);
            continue;             
         }
         else if (bl->dynamic)
         {
            /*
             * A previous job was dynamic and as we
             * recompile the whole filter anyway, it
             * makes no sense to compile this job now.
             */
            log_error(LOG_LEVEL_RE_FILTER,
               "Adding static re_filter job \'%s\' to dynamic filter %s succeeded.", buf, bl->name);
            continue;
         }

         if ((dummy = pcrs_compile_command(buf, &error)) == NULL)
         {
            log_error(LOG_LEVEL_ERROR,
               "Adding re_filter job \'%s\' to filter %s failed with error %d.", buf, bl->name, error);
            continue;
         }
         else
         {
            if (bl->joblist == NULL)
            {
               bl->joblist = dummy;
            }
            else if (NULL != lastjob)
            {
               lastjob->next = dummy;
            }
            lastjob = dummy;
            log_error(LOG_LEVEL_RE_FILTER, "Adding re_filter job \'%s\' to filter %s succeeded.", buf, bl->name);
         }
      }
      else
      {
         log_error(LOG_LEVEL_ERROR, "Ignoring job %s outside filter block in %s, line %d",
            buf, csp->config->re_filterfile[fileid], linenum);
      }
   }

   fclose(fp);

   /* 
    * Schedule the now-obsolete old data for unloading
    */
   if ( NULL != current_re_filterfile[fileid] )
   {
      current_re_filterfile[fileid]->unloader = unload_re_filterfile;
   }

   /*
    * Chain this file into the global list of loaded files
    */
   fs->next    = files->next;
   files->next = fs;
   current_re_filterfile[fileid] = fs;

   if (csp)
   {
      csp->rlist[fileid] = fs;
   }

   return( 0 );

load_re_filterfile_error:
   log_error(LOG_LEVEL_FATAL, "can't load re_filterfile '%s': %E",
             csp->config->re_filterfile[fileid]);
   return(-1);

}


/*********************************************************************
 *
 * Function    :  add_loader
 *
 * Description :  Called from `load_config'.  Called once for each input
 *                file found in config.
 *
 * Parameters  :
 *          1  :  loader = pointer to a function that can parse and load
 *                the appropriate config file.
 *          2  :  config = The configuration_spec to add the loader to.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void add_loader(int (*loader)(struct client_state *),
                struct configuration_spec * config)
{
   int i;

   for (i=0; i < NLOADERS; i++)
   {
      if (config->loaders[i] == NULL)
      {
         config->loaders[i] = loader;
         break;
      }
   }

}


/*********************************************************************
 *
 * Function    :  run_loader
 *
 * Description :  Called from `load_config' and `listen_loop'.  This
 *                function keeps the "csp" current with any file mods
 *                since the last loop.  If a file is unchanged, the
 *                loader functions do NOT reload the file.
 *
 * Parameters  :
 *          1  :  csp = Current client state (buffers, headers, etc...)
 *                      Must be non-null.  Reads: "csp->config"
 *                      Writes: various data members.
 *
 * Returns     :  0 => Ok, everything else is an error.
 *
 *********************************************************************/
int run_loader(struct client_state *csp)
{
   int ret = 0;
   int i;

   for (i=0; i < NLOADERS; i++)
   {
      if (csp->config->loaders[i] == NULL)
      {
         break;
      }
      ret |= (csp->config->loaders[i])(csp);
   }
   return(ret);

}


/*
  Local Variables:
  tab-width: 3
  end:
*/
