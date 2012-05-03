#ifndef W32LOG_H_INCLUDED
#define W32LOG_H_INCLUDED
#define W32LOG_H_VERSION "$Id: w32log.h,v 1.13 2009/03/07 17:58:02 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/w32log.h,v $
 *
 * Purpose     :  Functions for creating and destroying the log window,
 *                ouputting strings, processing messages and so on.
 *
 * Copyright   :  Written by and Copyright (C) 2001-2009 members of
 *                the Privoxy team.  http://www.privoxy.org/
 *
 *                Written by and Copyright (C) 1999 Adam Lock
 *                <locka@iol.ie>
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
 *    $Log: w32log.h,v $
 *    Revision 1.13  2009/03/07 17:58:02  fabiankeil
 *    Fix two mingw32-only buffer overflows. Note that triggering
 *    them requires control over the configuration file in which
 *    case all bets are off anyway.
 *
 *    Revision 1.12  2006/07/18 14:48:48  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.10.2.2  2002/11/20 14:39:05  oes
 *    Fixed compiler warning
 *
 *    Revision 1.10.2.1  2002/08/21 17:58:05  oes
 *    Temp kludge to let user and default action file be edited through win32 GUI (FR 592080)
 *
 *    Revision 1.10  2002/03/26 22:57:10  jongfoster
 *    Web server name should begin www.
 *
 *    Revision 1.9  2002/03/24 12:03:47  jongfoster
 *    Name change
 *
 *    Revision 1.8  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.7  2001/07/29 18:43:08  jongfoster
 *    Changing #ifdef _FILENAME_H to FILENAME_H_INCLUDED, to conform to
 *    ANSI C rules.
 *
 *    Revision 1.6  2001/07/13 14:04:59  oes
 *    Removed all #ifdef PCRS
 *
 *    Revision 1.5  2001/06/07 23:08:12  jongfoster
 *    Forward and ACL edit options removed.
 *
 *    Revision 1.4  2001/05/31 21:37:11  jongfoster
 *    GUI changes to rename "permissions file" to "actions file".
 *
 *    Revision 1.3  2001/05/29 09:50:24  jongfoster
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
 *    Revision 1.2  2001/05/26 00:28:36  jongfoster
 *    Automatic reloading of config file.
 *    Removed obsolete SIGHUP support (Unix) and Reload menu option (Win32).
 *    Most of the global variables have been moved to a new
 *    struct configuration_spec, accessed through csp->config->globalname
 *    Most of the globals remaining are used by the Win32 GUI.
 *
 *    Revision 1.1.1.1  2001/05/15 13:59:07  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

extern HWND g_hwndLogFrame;

/* Indicates whether task bar shows activity animation */
extern BOOL g_bShowActivityAnimation;

/* Indicates if the log window appears on the task bar */
extern BOOL g_bShowOnTaskBar;

/* Indicates whether closing the log window really just hides it */
extern BOOL g_bCloseHidesWindow;

/* Indicates if messages are logged at all */
extern BOOL g_bLogMessages;

/* Indicates whether log messages are highlighted */
extern BOOL g_bHighlightMessages;

/* Indicates if buffer is limited in size */
extern BOOL g_bLimitBufferSize;

/* Maximum number of lines allowed in buffer when limited */
extern int g_nMaxBufferLines;

/* Font to use */
extern char g_szFontFaceName[32];

/* Size of font to use */
extern int g_nFontSize;


/* FIXME: this is a kludge */

extern const char * g_default_actions_file;
extern const char * g_user_actions_file;
extern const char * g_re_filterfile;
#ifdef FEATURE_TRUST
extern const char * g_trustfile;
#endif /* def FEATURE_TRUST */

/* FIXME: end kludge */

extern HICON g_hiconApp;
extern int LogPutString(const char *pszText);
extern BOOL InitLogWindow(void);
extern void TermLogWindow(void);
extern void ShowLogWindow(BOOL bShow);
extern void LogShowActivity(void);

/* Revision control strings from this header and associated .c file */
extern const char w32log_rcs[];
extern const char w32log_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef W32LOG_H_INCLUDED */


/*
  Local Variables:
  tab-width: 3
  end:
*/
