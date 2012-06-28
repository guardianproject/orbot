#ifndef W32TASKBAR_H_INCLUDED
#define W32TASKBAR_H_INCLUDED
#define W32TASKBAR_H_VERSION "$Id: w32taskbar.h,v 1.8 2011/09/04 11:10:56 fabiankeil Exp $"
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
