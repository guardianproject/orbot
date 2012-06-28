#ifndef W32LOG_H_INCLUDED
#define W32LOG_H_INCLUDED
#define W32LOG_H_VERSION "$Id: w32log.h,v 1.16 2011/09/04 11:10:56 fabiankeil Exp $"
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
extern const char * g_default_filterfile;
extern const char * g_user_filterfile;
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
