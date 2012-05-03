#ifndef LOADERS_H_INCLUDED
#define LOADERS_H_INCLUDED
#define LOADERS_H_VERSION "$Id: loaders.h,v 1.23 2008/03/30 14:52:10 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/loaders.h,v $
 *
 * Purpose     :  Functions to load and unload the various
 *                configuration files.  Also contains code to manage
 *                the list of active loaders, and to automatically 
 *                unload files that are no longer in use.
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
 *    $Log: loaders.h,v $
 *    Revision 1.23  2008/03/30 14:52:10  fabiankeil
 *    Rename load_actions_file() and load_re_filterfile()
 *    as they load multiple files "now".
 *
 *    Revision 1.22  2007/06/01 14:12:38  fabiankeil
 *    Add unload_forward_spec() in preparation for forward-override{}.
 *
 *    Revision 1.21  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.19  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.18  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.17  2002/03/16 23:54:06  jongfoster
 *    Adding graceful termination feature, to help look for memory leaks.
 *    If you enable this (which, by design, has to be done by hand
 *    editing config.h) and then go to http://i.j.b/die, then the program
 *    will exit cleanly after the *next* request.  It should free all the
 *    memory that was used.
 *
 *    Revision 1.16  2002/03/07 03:46:17  oes
 *    Fixed compiler warnings
 *
 *    Revision 1.15  2002/01/22 23:46:18  jongfoster
 *    Moving edit_read_line() and simple_read_line() to loaders.c, and
 *    extending them to support reading MS-DOS, Mac and UNIX style files
 *    on all platforms.
 *
 *    Modifying read_config_line() (without changing it's prototype) to
 *    be a trivial wrapper for edit_read_line().  This means that we have
 *    one function to read a line and handle comments, which is common
 *    between the initialization code and the edit interface.
 *
 *    Revision 1.14  2002/01/17 21:03:08  jongfoster
 *    Moving all our URL and URL pattern parsing code to urlmatch.c.
 *
 *    Renaming free_url to free_url_spec, since it frees a struct url_spec.
 *
 *    Revision 1.13  2001/12/30 14:07:32  steudten
 *    - Add signal handling (unix)
 *    - Add SIGHUP handler (unix)
 *    - Add creation of pidfile (unix)
 *    - Add action 'top' in rc file (RH)
 *    - Add entry 'SIGNALS' to manpage
 *    - Add exit message to logfile (unix)
 *
 *    Revision 1.12  2001/11/07 00:02:13  steudten
 *    Add line number in error output for lineparsing for
 *    actionsfile and configfile.
 *    Special handling for CLF added.
 *
 *    Revision 1.11  2001/10/23 21:38:53  jongfoster
 *    Adding error-checking to create_url_spec()
 *
 *    Revision 1.10  2001/09/22 16:36:59  jongfoster
 *    Removing unused parameter fs from read_config_line()
 *
 *    Revision 1.9  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.8  2001/07/29 18:58:15  jongfoster
 *    Removing nested #includes, adding forward declarations for needed
 *    structures, and changing the #define _FILENAME_H to FILENAME_H_INCLUDED.
 *
 *    Revision 1.7  2001/07/13 14:01:54  oes
 *    Removed all #ifdef PCRS
 *
 *    Revision 1.6  2001/06/07 23:14:38  jongfoster
 *    Removing ACL and forward file loaders - these files have
 *    been merged into the config file.
 *
 *    Revision 1.5  2001/05/31 21:28:49  jongfoster
 *    Removed all permissionsfile code - it's now called the actions
 *    file, and (almost) all the code is in actions.c
 *
 *    Revision 1.4  2001/05/29 09:50:24  jongfoster
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
 *    Revision 1.3  2001/05/26 00:28:36  jongfoster
 *    Automatic reloading of config file.
 *    Removed obsolete SIGHUP support (Unix) and Reload menu option (Win32).
 *    Most of the global variables have been moved to a new
 *    struct configuration_spec, accessed through csp->config->globalname
 *    Most of the globals remaining are used by the Win32 GUI.
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
 *    Revision 1.1.1.1  2001/05/15 13:59:00  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

/* Structures taken from project.h */
struct client_state;
struct file_list;
struct configuration_spec;
struct url_spec;

extern void sweep(void);
extern char *read_config_line(char *buf, size_t buflen, FILE *fp, unsigned long *linenum);
extern int check_file_changed(const struct file_list * current,
                              const char * filename,
                              struct file_list ** newfl);

extern jb_err edit_read_line(FILE *fp,
                             char **raw_out,
                             char **prefix_out,
                             char **data_out,
                             int *newline,
                             unsigned long *line_number);

extern jb_err simple_read_line(FILE *fp, char **dest, int *newline);

/*
 * Various types of newlines that a file may contain.
 */
#define NEWLINE_UNKNOWN 0  /* Newline convention in file is unknown */
#define NEWLINE_UNIX    1  /* Newline convention in file is '\n'   (ASCII 10) */
#define NEWLINE_DOS     2  /* Newline convention in file is '\r\n' (ASCII 13,10) */
#define NEWLINE_MAC     3  /* Newline convention in file is '\r'   (ASCII 13) */

/*
 * Types of newlines that a file may contain, as strings.  If you have an
 * extremely wierd compiler that does not have '\r' == CR == ASCII 13 and
 * '\n' == LF == ASCII 10), then fix CHAR_CR and CHAR_LF in loaders.c as
 * well as these definitions.
 */
#define NEWLINE(style) ((style)==NEWLINE_DOS ? "\r\n" : \
                        ((style)==NEWLINE_MAC ? "\r" : "\n"))


extern short int MustReload;
extern int load_action_files(struct client_state *csp);
extern int load_re_filterfiles(struct client_state *csp);

#ifdef FEATURE_TRUST
extern int load_trustfile(struct client_state *csp);
#endif /* def FEATURE_TRUST */

#ifdef FEATURE_GRACEFUL_TERMINATION
#ifdef FEATURE_TRUST
void unload_current_trust_file(void);
#endif
void unload_current_re_filterfile(void);
#endif /* FEATURE_GRACEFUL_TERMINATION */

void unload_forward_spec(struct forward_spec *fwd);

extern void add_loader(int (*loader)(struct client_state *), 
                       struct configuration_spec * config);
extern int run_loader(struct client_state *csp);

/* Revision control strings from this header and associated .c file */
extern const char loaders_rcs[];
extern const char loaders_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef LOADERS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
