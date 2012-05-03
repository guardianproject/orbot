#ifndef W32TASKBAR_H_INCLUDED
#define W32TASKBAR_H_INCLUDED
#define W32TASKBAR_H_VERSION "$Id: w32taskbar.h,v 1.6 2006/07/18 14:48:48 david__schmidt Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/w32taskbar.h,v $
 *
 * Purpose     :  Functions for creating, setting and destroying the
 *                workspace tray icon
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
 *    $Log: w32taskbar.h,v $
 *    Revision 1.6  2006/07/18 14:48:48  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.4  2002/03/26 22:57:10  jongfoster
 *    Web server name should begin www.
 *
 *    Revision 1.3  2002/03/24 12:03:47  jongfoster
 *    Name change
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

extern HWND CreateTrayWindow(HINSTANCE hInstance);
extern BOOL TrayAddIcon(HWND hwnd, UINT uID, HICON hicon, const char *pszToolTip);
extern BOOL TraySetIcon(HWND hwnd, UINT uID, HICON hicon);
extern BOOL TrayDeleteIcon(HWND hwnd, UINT uID);

/* Revision control strings from this header and associated .c file */
extern const char w32taskbar_rcs[];
extern const char w32taskbar_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef W32TASKBAR_H_INCLUDED */


/*
  Local Variables:
  tab-width: 3
  end:
*/
