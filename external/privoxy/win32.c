const char win32_rcs[] = "$Id: win32.c,v 1.18 2011/09/04 11:10:56 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/win32.c,v $
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
 *********************************************************************/


#include "config.h"

#ifdef _WIN32

#include <stdio.h>

#include "project.h"
#include "jcc.h"
#include "miscutil.h"

/* Uncomment this if you want to build Win32 as a console app */
/* #define _WIN_CONSOLE */

#ifndef STRICT
#define STRICT
#endif
#include <windows.h>

#include <stdarg.h>
#include <process.h>

#if defined(_WIN32) && defined(_MSC_VER) && defined(_DEBUG)
/* Visual C++ Heap debugging */
#include <crtdbg.h>
#endif /* defined(_WIN32) && defined(_MSC_VER) && defined(_DEBUG) */

#include "win32.h"

const char win32_h_rcs[] = WIN32_H_VERSION;

/**
 * A short introductory text about Privoxy.  Used for the "About" box
 * or the console startup message.
 */
const char win32_blurb[] =
"Privoxy version " VERSION " for Windows\n"
"Copyright (C) 2000-2010 the Privoxy Team (" HOME_PAGE_URL ")\n"
"Based on the Internet Junkbuster by Junkbusters Corp.\n"
"This is free software; it may be used and copied under the\n"
"GNU General Public License, version 2: http://www.gnu.org/licenses/old-licenses/gpl-2.0.html\n"
"This program comes with ABSOLUTELY NO WARRANTY OF ANY KIND.\n";

#ifdef _WIN_CONSOLE

/**
 * Hide the console.  If set, the program will disconnect from the
 * console and run in the background.  This allows the command-prompt
 * window to close.
 */
int hideConsole     = 0;


#else /* ndef _WIN_CONSOLE */


/**
 * The application instance handle.
 */
HINSTANCE g_hInstance;


/**
 * The command to show the window that was specified at startup.
 */
int g_nCmdShow;

static void  __cdecl UserInterfaceThread(void *);


#endif /* ndef _WIN_CONSOLE */

/*********************************************************************
 *
 * Function    :  WinMain
 *
 * Description :  M$ Windows "main" routine:
 *                parse the `lpCmdLine' param into main's argc and argv variables,
 *                start the user interface thread (for the systray window), and
 *                call main (i.e. patch execution into normal startup).
 *
 * Parameters  :
 *          1  :  hInstance = instance handle of this execution
 *          2  :  hPrevInstance = instance handle of previous execution
 *          3  :  lpCmdLine = command line string which started us
 *          4  :  nCmdShow = window show value (MIN, MAX, NORMAL, etc...)
 *
 * Returns     :  `main' never returns, so WinMain will also never return.
 *
 *********************************************************************/
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow)
{
#if 0   /* See comment about __argc & __argv below */
   int i;
   int argc = 1;
   const char *argv[3];
   char szModule[MAX_PATH+1];
#endif

   int res;
#ifndef _WIN_CONSOLE
   HANDLE hInitCompleteEvent = NULL;
#endif


#if defined(_WIN32) && defined(_MSC_VER) && defined(_DEBUG)
#if 0
   /* Visual C++ Heap debugging */

   /* Get current flag*/
   int tmpFlag = _CrtSetDbgFlag( _CRTDBG_REPORT_FLAG );

   /* Turn on leak-checking bit */
   tmpFlag |= _CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF | _CRTDBG_CHECK_ALWAYS_DF;

   /* Turn off CRT block checking bit */
   tmpFlag &= ~(_CRTDBG_CHECK_CRT_DF | _CRTDBG_DELAY_FREE_MEM_DF);

   /* Set flag to the new value */
   _CrtSetDbgFlag( tmpFlag );
#endif
#endif /* defined(_WIN32) && defined(_MSC_VER) && defined(_DEBUG) */


/************
 * I couldn't figure out why the command line was being sorta parsed here
 * instead of using the __argc & __argv globals usually defined in stdlib.h
 *
 * From what I can tell by looking at the MinWG source, it supports these
 * globals, so i'd hope that the other compilers do so as well.
 * Obviously, if i'm wrong i'll find out soon enough!  :)
 ************/
#if 0
   /*
    * Cheat in parsing the command line.  We only ever have at most one
    * paramater, which may optionally be specified inside double quotes.
    */

   if (lpCmdLine != NULL)
   {
      /* Make writable copy */
      lpCmdLine = strdup(lpCmdLine);
   }
   if (lpCmdLine != NULL)
   {
      chomp(lpCmdLine);
      i = strlen(lpCmdLine);
      if ((i >= 2) && (lpCmdLine[0] == '\"') && (lpCmdLine[i - 1] == '\"'))
      {
         lpCmdLine[i - 1] = '\0';
         lpCmdLine++;
      }
      if (lpCmdLine[0] == '\0')
      {
         lpCmdLine = NULL;
      }
   }

   GetModuleFileName(hInstance, szModule, MAX_PATH);
   argv[0] = szModule;
   argv[1] = lpCmdLine;
   argv[2] = NULL;
   argc = ((lpCmdLine != NULL) ? 2 : 1);
#endif /* -END- 0 */


#ifndef _WIN_CONSOLE
   /* Create a user-interface thread and wait for it to initialise */
   hInitCompleteEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
   g_hInstance = hInstance;
   g_nCmdShow = nCmdShow;
   _beginthread(UserInterfaceThread, 0, &hInitCompleteEvent);
   WaitForSingleObject(hInitCompleteEvent, INFINITE);
   DeleteObject(hInitCompleteEvent);
#endif

#ifdef __MINGW32__
   res = real_main( __argc, __argv );
#else
   res = main( __argc, __argv );
#endif

   return res;

}

#endif

/*********************************************************************
 *
 * Function    :  InitWin32
 *
 * Description :  Initialise windows, setting up the console or windows as appropriate.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void InitWin32(void)
{
   WORD wVersionRequested;
   WSADATA wsaData;

#ifdef _WIN_CONSOLE
   SetProcessShutdownParameters(0x100, SHUTDOWN_NORETRY);
   if (hideConsole)
   {
      FreeConsole();
   }
#endif
   wVersionRequested = MAKEWORD(2, 0);
   if (WSAStartup(wVersionRequested, &wsaData) != 0)
   {
#ifndef _WIN_CONSOLE
      MessageBox(NULL, "Cannot initialize WinSock library", "Privoxy Error",
         MB_OK | MB_ICONERROR | MB_TASKMODAL | MB_SETFOREGROUND | MB_TOPMOST);
#endif
      exit(1);
   }

}


#ifndef _WIN_CONSOLE
#include <signal.h>
#include <assert.h>

#include "win32.h"
#include "w32log.h"


/*********************************************************************
 *
 * Function    :  UserInterfaceThread
 *
 * Description :  User interface thread.  WinMain will wait for us to set
 *                the hInitCompleteEvent before patching over to `main'.
 *                This ensures the systray window is active before beginning
 *                operations.
 *
 * Parameters  :
 *          1  :  pData = pointer to `hInitCompleteEvent'.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
static void __cdecl UserInterfaceThread(void *pData)
{
   MSG msg;
   HANDLE hInitCompleteEvent = *((HANDLE *) pData);

   /* Initialise */
   InitLogWindow();
   SetEvent(hInitCompleteEvent);

   /* Enter a message processing loop */
   while (GetMessage(&msg, (HWND) NULL, 0, 0))
   {
      TranslateMessage(&msg);
      DispatchMessage(&msg);
   }

   /* Cleanup */
   TermLogWindow();

   /* Time to die... */
   exit(0);

}


#endif /* ndef _WIN_CONSOLE */


/*
  Local Variables:
  tab-width: 3
  end:
*/
