const char w32taskbar_rcs[] = "$Id: w32taskbar.c,v 1.10 2006/09/23 13:26:38 roro Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/w32taskbar.c,v $
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
 *    $Log: w32taskbar.c,v $
 *    Revision 1.10  2006/09/23 13:26:38  roro
 *    Replace TABs by spaces in source code.
 *
 *    Revision 1.9  2006/07/18 14:48:48  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.7.2.2  2003/03/19 21:27:42  gliptak
 *    Corrected compilation error/typo
 *
 *    Revision 1.7.2.1  2002/11/20 14:39:32  oes
 *    Applied patch by Mattes Dolak which adds re-creation of the win32 taskbar
 *    icon on reception of the "TaskbarCreated" window message.
 *
 *    Revision 1.7  2002/03/31 17:19:00  jongfoster
 *    Win32 only: Enabling STRICT to fix a VC++ compile warning.
 *
 *    Revision 1.6  2002/03/26 22:57:10  jongfoster
 *    Web server name should begin www.
 *
 *    Revision 1.5  2002/03/24 12:03:47  jongfoster
 *    Name change
 *
 *    Revision 1.4  2001/11/16 00:46:31  jongfoster
 *    Fixing compiler warnings
 *
 *    Revision 1.3  2001/05/22 18:56:28  oes
 *    CRLF -> LF
 *
 *    Revision 1.2  2001/05/20 15:07:54  jongfoster
 *    File is now ignored if _WIN_CONSOLE is defined.
 *
 *    Revision 1.1.1.1  2001/05/15 13:59:08  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "config.h"

#include <stdio.h>

#ifndef STRICT
#define STRICT
#endif
#include <windows.h>

#include "w32taskbar.h"
#include "w32res.h"
#include "w32log.h"

const char w32taskbar_h_rcs[] = W32TASKBAR_H_VERSION;

#ifndef _WIN_CONSOLE /* entire file */

#define WM_TRAYMSG WM_USER+1

static HMENU g_hmenuTray;
static HWND g_hwndTrayX;
static UINT g_traycreatedmsg;

static LRESULT CALLBACK TrayProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);


/*********************************************************************
 *
 * Function    :  CreateTrayWindow
 *
 * Description :  Creates and returns the invisible window responsible
 *                for processing tray messages.
 *
 * Parameters  :
 *          1  :  hInstance = instance handle of this application
 *
 * Returns     :  Handle of the systray window.
 *
 *********************************************************************/
HWND CreateTrayWindow(HINSTANCE hInstance)
{
   WNDCLASS wc;
   static const char *szWndName = "PrivoxyTrayWindow";

   wc.style          = 0;
   wc.lpfnWndProc    = TrayProc;
   wc.cbClsExtra     = 0;
   wc.cbWndExtra     = 0;
   wc.hInstance      = hInstance;
   wc.hIcon          = 0;
   wc.hCursor        = 0;
   wc.hbrBackground  = 0;
   wc.lpszMenuName   = 0;
   wc.lpszClassName  = szWndName;

   RegisterClass(&wc);

   /* TaskbarCreated is sent to a window when it should re-add its tray icons */
   g_traycreatedmsg = RegisterWindowMessage("TaskbarCreated");

   g_hwndTrayX = CreateWindow(szWndName, szWndName,
      WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
      CW_USEDEFAULT, NULL, NULL, hInstance, NULL );

   ShowWindow(g_hwndTrayX, SW_HIDE);
   UpdateWindow(g_hwndTrayX);

   g_hmenuTray = LoadMenu(hInstance, MAKEINTRESOURCE(IDR_TRAYMENU));

   return g_hwndTrayX;

}


/*********************************************************************
 *
 * Function    :  TraySetIcon
 *
 * Description :  Sets the tray icon to the specified shape.
 *
 * Parameters  :
 *          1  :  hwnd = handle of the systray window
 *          2  :  uID = user message number to notify systray window
 *          3  :  hicon = set the current icon to this handle
 *
 * Returns     :  Same value as `Shell_NotifyIcon'.
 *
 *********************************************************************/
BOOL TraySetIcon(HWND hwnd, UINT uID, HICON hicon)
{
   NOTIFYICONDATA nid;

   memset(&nid, 0, sizeof(nid));

   nid.cbSize = sizeof(nid);
   nid.hWnd = hwnd;
   nid.uID = uID;
   nid.uFlags = NIF_ICON;
   nid.uCallbackMessage = 0;
   nid.hIcon = hicon;

   return( Shell_NotifyIcon(NIM_MODIFY, &nid) );

}


/*********************************************************************
 *
 * Function    :  TrayAddIcon
 *
 * Description :  Adds a tray icon.
 *
 * Parameters  :
 *          1  :  hwnd = handle of the systray window
 *          2  :  uID = user message number to notify systray window
 *          3  :  hicon = handle of icon to add to systray window
 *          4  :  pszToolTip = tool tip when mouse hovers over systray window
 *
 * Returns     :  Same as `Shell_NotifyIcon'.
 *
 *********************************************************************/
BOOL TrayAddIcon(HWND hwnd, UINT uID, HICON hicon, const char *pszToolTip)
{
   NOTIFYICONDATA nid;

   memset(&nid, 0, sizeof(nid));

   nid.cbSize = sizeof(nid);
   nid.hWnd = hwnd;
   nid.uID = uID;
   nid.uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP;
   nid.uCallbackMessage = WM_TRAYMSG;
   nid.hIcon = hicon;

   if (pszToolTip)
   {
      strcpy(nid.szTip, pszToolTip);
   }

   return( Shell_NotifyIcon(NIM_ADD, &nid) );

}


/*********************************************************************
 *
 * Function    :  TrayDeleteIcon
 *
 * Description :  Deletes a tray icon.
 *
 * Parameters  :
 *          1  :  hwnd = handle of the systray window
 *          2  :  uID = user message number to notify systray window
 *
 * Returns     :  Same as `Shell_NotifyIcon'.
 *
 *********************************************************************/
BOOL TrayDeleteIcon(HWND hwnd, UINT uID)
{
   NOTIFYICONDATA nid;

   memset(&nid, 0, sizeof(nid));

   nid.cbSize = sizeof(nid);
   nid.hWnd = hwnd;
   nid.uID = uID;

   return( Shell_NotifyIcon(NIM_DELETE, &nid) );

}


/*********************************************************************
 *
 * Function    :  TrayProc
 *
 * Description :  Call back procedure processes tray messages.
 *
 * Parameters  :
 *          1  :  hwnd = handle of the systray window
 *          2  :  msg = message number
 *          3  :  wParam = first param for this message
 *          4  :  lParam = next param for this message
 *
 * Returns     :  Appropriate M$ window message handler codes.
 *
 *********************************************************************/
LRESULT CALLBACK TrayProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
   switch (msg)
   {
      case WM_CREATE:
         return 0;

      case WM_CLOSE:
         PostQuitMessage(0);
         return 0;

      case WM_TRAYMSG:
      {
         /* UINT uID = (UINT) wParam; */
         UINT uMouseMsg = (UINT) lParam;

         if (uMouseMsg == WM_RBUTTONDOWN)
         {
            POINT pt;
            HMENU hmenu = GetSubMenu(g_hmenuTray,0);
            GetCursorPos(&pt);
            SetForegroundWindow(g_hwndLogFrame);
            TrackPopupMenu(hmenu, TPM_LEFTALIGN | TPM_TOPALIGN, pt.x, pt.y, 0, g_hwndLogFrame, NULL);
            PostMessage(g_hwndLogFrame, WM_NULL, 0, 0 ) ;
         }
         else if (uMouseMsg == WM_LBUTTONDBLCLK)
         {
            ShowLogWindow(TRUE);
         }
      }
      return 0;

      default:

         if (msg == g_traycreatedmsg)
         {
            TrayAddIcon(g_hwndTrayX, 1, g_hiconApp, "Privoxy");
         }
         break;
   }

   return DefWindowProc(hwnd, msg, wParam, lParam);

}


#endif /* ndef _WIN_CONSOLE - entire file */

/*
  Local Variables:
  tab-width: 3
  end:
*/
