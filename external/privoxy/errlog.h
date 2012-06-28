#ifndef ERRLOG_H_INCLUDED
#define ERRLOG_H_INCLUDED
#define ERRLOG_H_VERSION "$Id: errlog.h,v 1.22 2009/02/09 21:21:15 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/errlog.h,v $
 *
 * Purpose     :  Log errors to a designated destination in an elegant,
 *                printf-like fashion.
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
 *    $Log: errlog.h,v $
 *    Revision 1.22  2009/02/09 21:21:15  fabiankeil
 *    Now that init_log_module() is called earlier, call show_version()
 *    later on from main() directly so it doesn't get called for --help
 *    or --version.
 *
 *    Revision 1.21  2008/12/14 15:46:22  fabiankeil
 *    Give crunched requests their own log level.
 *
 *    Revision 1.20  2008/03/27 18:27:23  fabiankeil
 *    Remove kill-popups action.
 *
 *    Revision 1.19  2007/10/14 14:12:41  fabiankeil
 *    When in daemon mode, close stderr after the configuration file has been
 *    parsed the first time. If logfile isn't set, stop logging. Fixes BR#897436.
 *
 *    Revision 1.18  2007/07/14 07:28:47  fabiankeil
 *    Add translation function for JB_ERR_FOO codes.
 *
 *    Revision 1.17  2007/03/31 13:33:28  fabiankeil
 *    Add alternative log_error() with timestamps
 *    that contain milliseconds and without using
 *    strcpy(), strcat() or sprintf().
 *
 *    Revision 1.16  2006/11/28 15:29:50  fabiankeil
 *    Define LOG_LEVEL_REDIRECTS independently of
 *    FEATURE_FAST_REDIRECTS. It is used by redirect{}
 *    as well.
 *
 *    Revision 1.15  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.13.2.1  2002/08/05 17:57:06  oes
 *    Cosmetic change
 *
 *    Revision 1.13  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.12  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.11  2002/03/06 23:02:57  jongfoster
 *    Removing tabs
 *
 *    Revision 1.10  2001/09/13 20:08:06  jongfoster
 *    Adding support for LOG_LEVEL_CGI
 *
 *    Revision 1.9  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.8  2001/07/29 18:43:08  jongfoster
 *    Changing #ifdef _FILENAME_H to FILENAME_H_INCLUDED, to conform to
 *    ANSI C rules.
 *
 *    Revision 1.7  2001/07/19 19:02:53  haroon
 *    Added define for LOG_LEVEL_POPUPS
 *
 *    Revision 1.6  2001/07/13 13:59:22  oes
 *     - Added LOG_LEVEL_DEANIMATE
 *     - Changed LOG_LEVEL_CLF
 *     - Removed all #ifdef PCRS
 *
 *    Revision 1.5  2001/05/26 17:25:14  jongfoster
 *    Added support for CLF (Common Log Format) and fixed LOG_LEVEL_LOG
 *
 *    Revision 1.4  2001/05/25 21:56:06  jongfoster
 *    Added FIXME comment to (broken) LOG_LEVEL_LOG
 *
 *    Revision 1.3  2001/05/22 18:46:04  oes
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
 *    Revision 1.2  2001/05/20 01:11:40  jongfoster
 *    Added support for LOG_LEVEL_FATAL
 *    Renamed LOG_LEVEL_FRC to LOG_LEVEL_FORCE,
 *    and LOG_LEVEL_REF to LOG_LEVEL_RE_FILTER
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:51  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

/* Debug level for errors */

/* XXX: Should be renamed. */
#define LOG_LEVEL_GPC        0x0001
#define LOG_LEVEL_CONNECT    0x0002
#define LOG_LEVEL_IO         0x0004
#define LOG_LEVEL_HEADER     0x0008
#define LOG_LEVEL_LOG        0x0010
#ifdef FEATURE_FORCE_LOAD
#define LOG_LEVEL_FORCE      0x0020
#endif /* def FEATURE_FORCE_LOAD */
#define LOG_LEVEL_RE_FILTER  0x0040
#define LOG_LEVEL_REDIRECTS  0x0080
#define LOG_LEVEL_DEANIMATE  0x0100
#define LOG_LEVEL_CLF        0x0200 /* Common Log File format */
#define LOG_LEVEL_CRUNCH     0x0400
#define LOG_LEVEL_CGI        0x0800 /* CGI / templates */

/* Following are always on: */
#define LOG_LEVEL_INFO    0x1000
#define LOG_LEVEL_ERROR   0x2000
#define LOG_LEVEL_FATAL   0x4000 /* Exits after writing log */

extern void init_error_log(const char *prog_name, const char *logfname);
extern void set_debug_level(int debuglevel);
extern void disable_logging(void);
extern void init_log_module(void);
extern void show_version(const char *prog_name);
extern void log_error(int loglevel, const char *fmt, ...);
extern const char *jb_err_to_string(int error);

/* Revision control strings from this header and associated .c file */
extern const char errlog_rcs[];
extern const char errlog_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef ERRLOG_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/

