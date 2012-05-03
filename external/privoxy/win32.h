#ifndef WIN32_H_INCLUDED
#define WIN32_H_INCLUDED
#define WIN32_H_VERSION "$Id: win32.h,v 1.7 2006/07/18 14:48:48 david__schmidt Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/win32.h,v $
 *
 * Purpose     :  Win32 User Interface initialization and message loop
 *
 * Copyright   :  Written by and Copyright (C) 2001-2002 members of
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
 *    $Log: win32.h,v $
 *    Revision 1.7  2006/07/18 14:48:48  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.5  2002/03/26 22:57:10  jongfoster
 *    Web server name should begin www.
 *
 *    Revision 1.4  2002/03/24 12:03:47  jongfoster
 *    Name change
 *
 *    Revision 1.3  2001/07/30 22:08:36  jongfoster
 *    Tidying up #defines:
 *    - All feature #defines are now of the form FEATURE_xxx
 *    - Permanently turned off WIN_GUI_EDIT
 *    - Permanently turned on WEBDAV and SPLIT_PROXY_ARGS
 *
 *    Revision 1.2  2001/07/29 18:43:08  jongfoster
 *    Changing #ifdef _FILENAME_H to FILENAME_H_INCLUDED, to conform to
 *    ANSI C rules.
 *
 *    Revision 1.1.1.1  2001/05/15 13:59:08  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

extern const char win32_blurb[];

extern void InitWin32(void);

#ifdef _WIN_CONSOLE
extern int hideConsole;
#endif /*def _WIN_CONSOLE */

extern HINSTANCE g_hInstance;
extern int g_nCmdShow;

extern int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow);

/* Revision control strings from this header and associated .c file */
extern const char win32_rcs[];
extern const char win32_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef WIN32_H_INCLUDED */
  
/*
  Local Variables:
  tab-width: 3
  end:
*/
