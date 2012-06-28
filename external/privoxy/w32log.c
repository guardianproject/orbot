const char w32log_rcs[] = "$Id: w32log.c,v 1.40 2011/09/04 11:10:56 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/w32log.c,v $
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


#include "config.h"

#include <assert.h>
#include <stdio.h>

#ifndef STRICT
#define STRICT
#endif
#include <windows.h>
#include <richedit.h>

#include "project.h"
#include "w32log.h"
#include "w32taskbar.h"
#include "win32.h"
#include "w32res.h"
#include "jcc.h"
#include "miscutil.h"
#include "errlog.h"
#include "loadcfg.h"

const char w32res_h_rcs[] = W32RES_H_VERSION;

#ifdef __MINGW32__
#include "cygwin.h"
const char cygwin_h_rcs[] = CYGWIN_H_VERSION;
#endif

const char w32log_h_rcs[] = W32LOG_H_VERSION;

#ifndef _WIN_CONSOLE /* entire file */

/*
 * Timers and the various durations
 */
#define TIMER_ANIM_ID               1
#define TIMER_ANIM_TIME             100
#define TIMER_ANIMSTOP_ID           2
#define TIMER_ANIMSTOP_TIME         1000
#define TIMER_CLIPBUFFER_ID         3
#define TIMER_CLIPBUFFER_TIME       1000
#define TIMER_CLIPBUFFER_FORCE_ID   4
#define TIMER_CLIPBUFFER_FORCE_TIME 5000

/*
 * Styles of text that can be output
 */
#define STYLE_NONE      0
#define STYLE_HIGHLIGHT 1
#define STYLE_LINK      2
#define STYLE_HEADER    3

/*
 * Number of frames of animation in tray activity sequence
 */
#define ANIM_FRAMES 8

#define DEFAULT_MAX_BUFFER_LINES    200
#define DEFAULT_LOG_FONT_NAME       "MS Sans Serif"
#define DEFAULT_LOG_FONT_SIZE       8

/*
 * These values affect the way the log window behaves, they should be read
 * from a file but for the moment, they are hardcoded here. Some options are
 * configurable through the UI.
 */

/* Indicates whether task bar shows activity animation */
BOOL g_bShowActivityAnimation = 1;

/* Indicates whether the log window is shown */
BOOL g_bShowLogWindow = 1;

/* Indicates if the log window appears on the task bar */
BOOL g_bShowOnTaskBar = 0;

/* Indicates whether closing the log window really just hides it */
BOOL g_bCloseHidesWindow = 1;

/* Indicates if messages are logged at all */
BOOL g_bLogMessages = 1;

/* Indicates whether log messages are highlighted */
BOOL g_bHighlightMessages = 1;

/* Indicates if buffer is limited in size */
BOOL g_bLimitBufferSize = 1;

/* Maximum number of lines allowed in buffer when limited */
int g_nMaxBufferLines = DEFAULT_MAX_BUFFER_LINES;

/* Font to use */
char g_szFontFaceName[32] = DEFAULT_LOG_FONT_NAME;

/* Size of font to use */
int g_nFontSize = DEFAULT_LOG_FONT_SIZE;


/* FIXME: this is a kludge */

const char * g_default_actions_file = NULL;
const char * g_user_actions_file = NULL;
const char * g_default_filterfile = NULL;
const char * g_user_filterfile = NULL;
#ifdef FEATURE_TRUST
const char * g_trustfile = NULL;
#endif /* def FEATURE_TRUST */

/* FIXME: end kludge */

/* Regular expression for detected URLs */
#define RE_URL "http:[^ \n\r]*"

/*
 * Regular expressions that are used to perform highlight in the log window
 */
static struct _Pattern
{
   const char *str;
   int style;
   regex_t buffer;
} patterns_to_highlight[] =
{
   /* url headers */
   { RE_URL,                STYLE_LINK },
/* { "[a-zA-Z0-9]+\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+\\.[^ \n\r]*", STYLE_LINK }, */
   /* interesting text to highlight */
   /*   see jcc.c crunch_reason for the full list */
   { "Crunch: Blocked:",            STYLE_HIGHLIGHT },
   { "Crunch: Untrusted",           STYLE_HIGHLIGHT },
   { "Crunch: Redirected:",         STYLE_HIGHLIGHT },
   { "Crunch: DNS failure",         STYLE_HIGHLIGHT },
   { "Crunch: Forwarding failed",   STYLE_HIGHLIGHT },
   { "Crunch: Connection failure",  STYLE_HIGHLIGHT },
   { "Crunch: Out of memory",       STYLE_HIGHLIGHT },
   { "Connect: Found reusable socket",     STYLE_HIGHLIGHT },
   { "Connect: Reusing server socket",     STYLE_HIGHLIGHT },
   { "Connect: Created new connection to", STYLE_HIGHLIGHT },
   { "hung up on us",               STYLE_HIGHLIGHT },
   { "Crunching Referer:",          STYLE_HIGHLIGHT },
   /* what are all the possible error strings?? */
   { "Error:",                      STYLE_HIGHLIGHT },
   /* http headers */
   { "referer:",            STYLE_HEADER },
   { "proxy-connection:",   STYLE_HEADER },
   { "proxy-agent:",        STYLE_HEADER },
   { "user-agent:",         STYLE_HEADER },
   { "host:",               STYLE_HEADER },
   { "accept:",             STYLE_HEADER },
   { "accept-encoding:",    STYLE_HEADER },
   { "accept-language:",    STYLE_HEADER },
   { "accept-charset:",     STYLE_HEADER },
   { "accept-ranges:",      STYLE_HEADER },
   { "date:",               STYLE_HEADER },
   { "cache-control:",      STYLE_HEADER },
   { "cache-last-checked:", STYLE_HEADER },
   { "connection:",         STYLE_HEADER },
   { "content-type",        STYLE_HEADER },
   { "content-length",      STYLE_HEADER },
   { "cookie",              STYLE_HEADER },
   { "last-modified:",      STYLE_HEADER },
   { "pragma:",             STYLE_HEADER },
   { "server:",             STYLE_HEADER },
   { "etag:",               STYLE_HEADER },
   { "expires:",            STYLE_HEADER },
   { "warning:",            STYLE_HEADER },
   /* this is the terminator statement - do not delete! */
   { NULL,                  STYLE_NONE }
};

/*
 * Public variables
 */
HWND g_hwndLogFrame;
HICON g_hiconApp;

/*
 * Private variables
 */
static CRITICAL_SECTION g_criticalsection;
static HWND g_hwndTray;
static HWND g_hwndLogBox;
static WNDPROC g_fnLogBox;
static HICON g_hiconAnim[ANIM_FRAMES];
static HICON g_hiconIdle;
static HICON g_hiconOff;
static int g_nAnimFrame;
static BOOL g_bClipPending = FALSE;
static int g_nRichEditVersion = 0;

/*
 * Private functions
 */
static HWND CreateLogWindow(HINSTANCE hInstance, int nCmdShow);
static HWND CreateHiddenLogOwnerWindow(HINSTANCE hInstance);
static LRESULT CALLBACK LogWindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
static LRESULT CALLBACK LogOwnerWindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
static LRESULT CALLBACK LogRichEditProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
static BOOL InitRichEdit(void);
static void LogClipBuffer(void);
static void LogCreatePatternMatchingBuffers(void);
static void LogDestroyPatternMatchingBuffers(void);
static int LogPutStringNoMatch(const char *pszText, int style);
static void SetIdleIcon(void);


/*********************************************************************
 *
 * Function    :  InitLogWindow
 *
 * Description :  Initialise the log window.
 *
 * Parameters  :  None
 *
 * Returns     :  Always TRUE (there should be error checking on the resources).
 *
 *********************************************************************/
BOOL InitLogWindow(void)
{
   int i;

   /* Load the icons */
   g_hiconIdle = LoadIcon(g_hInstance, MAKEINTRESOURCE(IDI_IDLE));
   g_hiconOff  = LoadIcon(g_hInstance, MAKEINTRESOURCE(IDI_OFF));
   for (i = 0; i < ANIM_FRAMES; i++)
   {
      g_hiconAnim[i] = LoadIcon(g_hInstance, MAKEINTRESOURCE(IDI_ANIMATED1 + i));
   }
   g_hiconApp = LoadIcon(g_hInstance, MAKEINTRESOURCE(IDI_MAINICON));

   /* Create the user interface */
   g_hwndLogFrame = CreateLogWindow(g_hInstance, g_nCmdShow);
   g_hwndTray = CreateTrayWindow(g_hInstance);
   TrayAddIcon(g_hwndTray, 1, g_hiconApp, "Privoxy");

   /* Create pattern matching buffers (for highlighting */
   LogCreatePatternMatchingBuffers();

   /* Create a critical section to protect multi-threaded access to certain things */
   InitializeCriticalSection(&g_criticalsection);

   return TRUE;

}


/*********************************************************************
 *
 * Function    :  TermLogWindow
 *
 * Description :  Cleanup the logwindow.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void TermLogWindow(void)
{
   int i;

   LogDestroyPatternMatchingBuffers();

   TrayDeleteIcon(g_hwndTray, 1);
   DeleteObject(g_hiconApp);
   DeleteObject(g_hiconIdle);
   DeleteObject(g_hiconOff);
   for (i = 0; i < ANIM_FRAMES; i++)
   {
      DeleteObject(g_hiconAnim[i]);
   }

}


/*********************************************************************
 *
 * Function    :  LogCreatePatternMatchingBuffers
 *
 * Description :  Compile the pattern matching buffers.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void LogCreatePatternMatchingBuffers(void)
{
   int i;
   for (i = 0; patterns_to_highlight[i].str != NULL; i++)
   {
      regcomp(&patterns_to_highlight[i].buffer, patterns_to_highlight[i].str, REG_ICASE);
   }
}


/*********************************************************************
 *
 * Function    :  LogDestroyPatternMatchingBuffers
 *
 * Description :  Free up the pattern matching buffers.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void LogDestroyPatternMatchingBuffers(void)
{
   int i;
   for (i = 0; patterns_to_highlight[i].str != NULL; i++)
   {
      regfree(&patterns_to_highlight[i].buffer);
   }
}


/*********************************************************************
 *
 * Function    :  LogPutString
 *
 * Description :  Inserts text into the logging window.  This is really
 *                a regexp aware wrapper function to `LogPutStringNoMatch'.
 *
 * Parameters  :
 *          1  :  pszText = pointer to string going to the log window
 *
 * Returns     :  1 => success, else the return code from `LogPutStringNoMatch'.
 *                FIXME: this is backwards to the rest of IJB and to common
 *                programming practice.  Please use 0 => success instead.
 *
 *********************************************************************/
int LogPutString(const char *pszText)
{
   int i;
   int result = 0;

   if (!g_bLogMessages)
   {
      return 1;
   }

   if (pszText == NULL || strlen(pszText) == 0)
   {
      return 1;
   }

   /* Critical section stops multiple threads doing nasty interactions that
    * foul up the highlighting and output.
    */
   EnterCriticalSection(&g_criticalsection);

   if (g_bHighlightMessages)
   {
      regmatch_t match;

      /* First things first, regexp scan for various things that we would like highlighted */
      for (i = 0; patterns_to_highlight[i].str != NULL; i++)
      {
         if (regexec(&patterns_to_highlight[i].buffer, pszText, 1, &match, 0) == 0)
         {
            char *pszBefore = NULL;
            char *pszMatch = NULL;
            char *pszAfter = NULL;
            int nMatchSize;

            /* Split the string up into pieces representing the strings, before
               at and after the matching pattern
             */
            if (match.rm_so > 0)
            {
               pszBefore = (char *)malloc((match.rm_so + 1) * sizeof(char));
               memset(pszBefore, 0, (match.rm_so + 1) * sizeof(char));
               strncpy(pszBefore, pszText, match.rm_so);
            }
            if (match.rm_eo < (regoff_t)strlen(pszText))
            {
               pszAfter = strdup(&pszText[match.rm_eo]);
            }
            nMatchSize = match.rm_eo - match.rm_so;
            pszMatch = (char *)malloc(nMatchSize + 1);
            strncpy(pszMatch, &pszText[match.rm_so], nMatchSize);
            pszMatch[nMatchSize] = '\0';

            /* Recursively call LogPutString */
            if (pszBefore)
            {
               LogPutString(pszBefore);
               free(pszBefore);
            }
            if (pszMatch)
            {
               LogPutStringNoMatch(pszMatch, patterns_to_highlight[i].style);
               free(pszMatch);
            }
            if (pszAfter)
            {
               LogPutString(pszAfter);
               free(pszAfter);
            }

            result = 1;
            goto end;
         }
      }
   }

   result = LogPutStringNoMatch(pszText, STYLE_NONE);

end:
   LeaveCriticalSection(&g_criticalsection);

   return result;

}


/*********************************************************************
 *
 * Function    :  LogPutStringNoMatch
 *
 * Description :  Puts a string into the logging window.
 *
 * Parameters  :
 *          1  :  pszText = pointer to string going to the log window
 *          2  :  style = STYLE_NONE, STYLE_HEADER, STYLE_HIGHLIGHT, or STYLE_LINK
 *
 * Returns     :  Always 1 => success.
 *                FIXME: this is backwards to the rest of IJB and to common
 *                programming practice.  Please use 0 => success instead.
 *
 *********************************************************************/
int LogPutStringNoMatch(const char *pszText, int style)
{
   CHARRANGE range;
   CHARFORMAT format;
   int nTextLength;

   assert(g_hwndLogBox);
   if (g_hwndLogBox == NULL)
   {
      return 1;
   }

   /* TODO preserve existing selection */

   /* Go to the end of the text */
   nTextLength = GetWindowTextLength(g_hwndLogBox);
   range.cpMin = nTextLength;
   range.cpMax = nTextLength;
   SendMessage(g_hwndLogBox, EM_EXSETSEL, 0, (LPARAM) &range);

   /* Apply a formatting style */
   memset(&format, 0, sizeof(format));
   format.cbSize = sizeof(format);
   format.dwMask = CFM_BOLD | CFM_UNDERLINE | CFM_STRIKEOUT |
      CFM_ITALIC | CFM_COLOR | CFM_FACE | CFM_SIZE | CFM_CHARSET;
   format.bCharSet = DEFAULT_CHARSET;
   format.yHeight = (g_nFontSize * 1440) / 72;
   strlcpy(format.szFaceName, g_szFontFaceName, sizeof(format.szFaceName));
   if (style == STYLE_NONE)
   {
      /* DO NOTHING */
      format.dwEffects |= CFE_AUTOCOLOR;
   }
   else if (style == STYLE_HEADER)
   {
      format.dwEffects |= CFE_AUTOCOLOR | CFE_ITALIC;
   }
   else if (style == STYLE_HIGHLIGHT)
   {
      format.dwEffects |= CFE_AUTOCOLOR | CFE_BOLD;
   }
   else if (style == STYLE_LINK)
   {
      format.dwEffects |= CFE_UNDERLINE;
      format.crTextColor = RGB(0, 0, 255);
   }
   SendMessage(g_hwndLogBox, EM_SETCHARFORMAT, SCF_SELECTION, (LPARAM) &format);

   /* Append text to the end */
   SendMessage(g_hwndLogBox, EM_REPLACESEL, FALSE, (LPARAM) pszText);

   /* TODO Restore the old selection */

   /* Purge buffer */
   if (strchr(pszText, '\n') != NULL)
   {
      SetTimer(g_hwndLogFrame, TIMER_CLIPBUFFER_ID, TIMER_CLIPBUFFER_TIME, NULL);
      if (!g_bClipPending)
      {
         /* Set the force clip timer going. This timer ensures clipping is done
            intermittently even when there is a sustained burst of logging
         */
         SetTimer(g_hwndLogFrame, TIMER_CLIPBUFFER_FORCE_ID, TIMER_CLIPBUFFER_FORCE_TIME, NULL);
      }
      g_bClipPending = TRUE;
   }

   return 1;

}


/*********************************************************************
 *
 * Function    :  LogShowActivity
 *
 * Description :  Start the spinner.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void LogShowActivity(void)
{
   /* Start some activity timers */
   if (g_bShowActivityAnimation)
   {
      SetTimer(g_hwndLogFrame, TIMER_ANIM_ID, TIMER_ANIM_TIME, NULL);
      SetTimer(g_hwndLogFrame, TIMER_ANIMSTOP_ID, TIMER_ANIMSTOP_TIME, NULL);
   }

}


/*********************************************************************
 *
 * Function    :  LogClipBuffer
 *
 * Description :  Prunes old lines from the log.
 *
 * Parameters  :  None
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void LogClipBuffer(void)
{
   int nLines = SendMessage(g_hwndLogBox, EM_GETLINECOUNT, 0, 0);
   if (g_bLimitBufferSize && nLines > g_nMaxBufferLines)
   {
      /* Compute the range representing the lines to be deleted */
      LONG nLastLineToDelete = nLines - g_nMaxBufferLines;
      LONG nLastChar = SendMessage(g_hwndLogBox, EM_LINEINDEX, nLastLineToDelete, 0);
      CHARRANGE range;
      range.cpMin = 0;
      range.cpMax = nLastChar;

      /* TODO get current selection */

      /* TODO adjust and clip old selection against range to be deleted */

      /* Select range and erase it (turning off autoscroll to prevent
         nasty scrolling) */
      SendMessage(g_hwndLogBox, EM_SETOPTIONS, ECOOP_XOR, ECO_AUTOVSCROLL);
      SendMessage(g_hwndLogBox, EM_EXSETSEL, 0, (LPARAM) &range);
      SendMessage(g_hwndLogBox, EM_REPLACESEL, FALSE, (LPARAM) "");
      SendMessage(g_hwndLogBox, EM_SETOPTIONS, ECOOP_XOR, ECO_AUTOVSCROLL);

      /* reposition (back to) the end of the log content */
      range.cpMin = SendMessage (g_hwndLogBox, WM_GETTEXTLENGTH, 0, 0);
      range.cpMax = -1;
      SendMessage(g_hwndLogBox, EM_EXSETSEL, 0, (LPARAM) &range);

      /* restore vertical ScrollBar stuff (messed up by AUTOVSCROLL) */
      SendMessage (g_hwndLogBox, EM_SCROLL, SB_LINEDOWN, 0);

   }

}


/*********************************************************************
 *
 * Function    :  CreateHiddenLogOwnerWindow
 *
 * Description :  Creates a hidden owner window that stops the log
 *                window appearing in the task bar.
 *
 * Parameters  :
 *          1  :  hInstance = application's instance handle
 *
 * Returns     :  Handle to newly created window.
 *
 *********************************************************************/
HWND CreateHiddenLogOwnerWindow(HINSTANCE hInstance)
{
   static const char *szWndName = "PrivoxyLogOwner";
   WNDCLASS wc;
   HWND hwnd;

   wc.style          = 0;
   wc.lpfnWndProc    = LogOwnerWindowProc;
   wc.cbClsExtra     = 0;
   wc.cbWndExtra     = 0;
   wc.hInstance      = hInstance;
   wc.hIcon          = g_hiconApp;
   wc.hCursor        = 0;
   wc.hbrBackground  = 0;
   wc.lpszMenuName   = 0;
   wc.lpszClassName  = szWndName;

   RegisterClass(&wc);

   hwnd = CreateWindow(szWndName, szWndName,
      WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
      CW_USEDEFAULT, NULL, NULL, hInstance, NULL );

   return hwnd;

}


/*********************************************************************
 *
 * Function    :  LogOwnerWindowProc
 *
 * Description :  Dummy procedure that does nothing special.
 *
 * Parameters  :
 *          1  :  hwnd = window handle
 *          2  :  uMsg = message number
 *          3  :  wParam = first param for this message
 *          4  :  lParam = next param for this message
 *
 * Returns     :  Same as `DefWindowProc'.
 *
 *********************************************************************/
LRESULT CALLBACK LogOwnerWindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
   return DefWindowProc(hwnd, uMsg, wParam, lParam);

}


/*********************************************************************
 *
 * Function    :  CreateLogWindow
 *
 * Description :  Create the logging window.
 *
 * Parameters  :
 *          1  :  hInstance = application's instance handle
 *          2  :  nCmdShow = window show value (MIN, MAX, NORMAL, etc...)
 *
 * Returns     :  Handle to newly created window.
 *
 *********************************************************************/
HWND CreateLogWindow(HINSTANCE hInstance, int nCmdShow)
{
   static const char *szWndName = "PrivoxyLogWindow";
   static const char *szWndTitle = "Privoxy";

   HWND hwnd = NULL;
   HWND hwndOwner = (g_bShowOnTaskBar) ? NULL : CreateHiddenLogOwnerWindow(hInstance);
   RECT rcClient;
   WNDCLASSEX wc;

   memset(&wc, 0, sizeof(wc));
   wc.cbSize         = sizeof(wc);
   wc.style          = CS_DBLCLKS;
   wc.lpfnWndProc    = LogWindowProc;
   wc.cbClsExtra     = 0;
   wc.cbWndExtra     = 0;
   wc.hInstance      = hInstance;
   wc.hIcon          = g_hiconApp;
   wc.hCursor        = 0;
   wc.hbrBackground  = 0;
   wc.lpszMenuName   = MAKEINTRESOURCE(IDR_LOGVIEW);
   wc.lpszClassName  = szWndName;
   wc.hbrBackground  = GetStockObject(WHITE_BRUSH);
   RegisterClassEx(&wc);

   hwnd = CreateWindowEx(WS_EX_APPWINDOW, szWndName, szWndTitle,
      WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
      CW_USEDEFAULT, hwndOwner, NULL, hInstance, NULL);

   /* Now create a child list box */
   GetClientRect(hwnd, &rcClient);

   /* Create a rich edit control */
   InitRichEdit();
   g_hwndLogBox = CreateWindowEx(0, (g_nRichEditVersion == 0x0100) ? "RichEdit" : RICHEDIT_CLASS, "",
      ES_AUTOVSCROLL | ES_MULTILINE | ES_READONLY | ES_NOHIDESEL | WS_CHILD | WS_VSCROLL | WS_HSCROLL | WS_VISIBLE,
      rcClient.left, rcClient.top, rcClient.right, rcClient.bottom,
      hwnd, NULL, hInstance, NULL);
/* SendMessage(g_hwndLogBox, EM_SETWORDWRAPMODE, 0, 0); */

   /* Subclass the control to catch certain messages */
   g_fnLogBox = (WNDPROC) GetWindowLong(g_hwndLogBox, GWL_WNDPROC);
   SetWindowLong(g_hwndLogBox, GWL_WNDPROC, (LONG) LogRichEditProc);

   /* Minimizing looks stupid when the log window is not on the task bar, so hide instead */
   if (!g_bShowOnTaskBar &&
         (nCmdShow == SW_SHOWMINIMIZED ||
          nCmdShow == SW_MINIMIZE ||
          nCmdShow == SW_SHOWMINNOACTIVE))
   {
      g_bShowLogWindow = FALSE;
      nCmdShow = SW_HIDE;
   }

   ShowWindow(hwnd, nCmdShow);
   UpdateWindow(hwnd);


   GetClientRect(g_hwndLogFrame, &rcClient);
   SetWindowPos(g_hwndLogBox, NULL, rcClient.left, rcClient.top, rcClient.right - rcClient.left, rcClient.bottom - rcClient.top, SWP_NOZORDER);

   return hwnd;

}


/*********************************************************************
 *
 * Function    :  InitRichEdit
 *
 * Description :  Initialise the rich edit control library.
 *
 * Parameters  :  None
 *
 * Returns     :  TRUE => success, FALSE => failure.
 *                FIXME: this is backwards to the rest of IJB and to common
 *                programming practice.  Please use 0 => success instead.
 *
 *********************************************************************/
BOOL InitRichEdit(void)
{
   static HINSTANCE hInstRichEdit;
   if (hInstRichEdit == NULL)
   {
      g_nRichEditVersion = 0;
      hInstRichEdit = LoadLibraryA("RICHED20.DLL");
      if (hInstRichEdit)
      {
         g_nRichEditVersion = _RICHEDIT_VER;
      }
      else
      {
         hInstRichEdit = LoadLibraryA("RICHED32.DLL");
         if (hInstRichEdit)
         {
            g_nRichEditVersion = 0x0100;
         }
      }
   }
   return (hInstRichEdit != NULL) ? TRUE : FALSE;

}


/*********************************************************************
 *
 * Function    :  ShowLogWindow
 *
 * Description :  Shows or hides the log window.  We will also raise the
 *                window on a show command in case it is buried.
 *
 * Parameters  :
 *          1  :  bShow = TRUE to show, FALSE to mimize/hide
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void ShowLogWindow(BOOL bShow)
{
   if (bShow)
   {
      SetForegroundWindow(g_hwndLogFrame);
      SetWindowPos(g_hwndLogFrame, HWND_TOP, 0, 0, 0, 0, SWP_SHOWWINDOW | SWP_NOSIZE | SWP_NOMOVE);

   }
   else if (g_bShowOnTaskBar)
   {
      ShowWindow(g_hwndLogFrame, SW_MINIMIZE);
   }
   else
   {
      ShowWindow(g_hwndLogFrame, SW_HIDE);
   }
}


/*********************************************************************
 *
 * Function    :  EditFile
 *
 * Description :  Opens the specified setting file for editing.
 * FIXME: What if the file has no associated application. Check for return values
*        from ShellExecute??
 *
 * Parameters  :
 *          1  :  filename = filename from the config (aka config.txt) file.
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void EditFile(const char *filename)
{
   if (filename)
   {
      ShellExecute(g_hwndLogFrame, "open", filename, NULL, NULL, SW_SHOWNORMAL);
   }

}


/*--------------------------------------------------------------------------*/
/* Windows message handlers                                                 */
/*--------------------------------------------------------------------------*/


/*********************************************************************
 *
 * Function    :  OnLogRButtonUp
 *
 * Description :  Handler for WM_RBUTTONUP messages.
 *
 * Parameters  :
 *          1  :  nModifier = wParam from mouse message (unused)
 *          2  :  x = x coordinate of the mouse event
 *          3  :  y = y coordinate of the mouse event
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void OnLogRButtonUp(int nModifier, int x, int y)
{
   HMENU hMenu = LoadMenu(g_hInstance, MAKEINTRESOURCE(IDR_POPUP_SELECTION));
   if (hMenu != NULL)
   {
      HMENU hMenuPopup = GetSubMenu(hMenu, 0);

      /* Check if there is a selection */
      CHARRANGE range;
      SendMessage(g_hwndLogBox, EM_EXGETSEL, 0, (LPARAM) &range);
      if (range.cpMin == range.cpMax)
      {
         EnableMenuItem(hMenuPopup, ID_EDIT_COPY, MF_BYCOMMAND | MF_GRAYED);
      }
      else
      {
         EnableMenuItem(hMenuPopup, ID_EDIT_COPY, MF_BYCOMMAND | MF_ENABLED);
      }

      /* Display the popup */
      TrackPopupMenu(hMenuPopup, TPM_LEFTALIGN | TPM_TOPALIGN | TPM_RIGHTBUTTON, x, y, 0, g_hwndLogFrame, NULL);
      DestroyMenu(hMenu);
   }

}


/*********************************************************************
 *
 * Function    :  OnLogCommand
 *
 * Description :  Handler for WM_COMMAND messages.
 *
 * Parameters  :
 *          1  :  nCommand = the command portion of the menu selection event
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void OnLogCommand(int nCommand)
{
   switch (nCommand)
   {
      case ID_TOGGLE_SHOWWINDOW:
         g_bShowLogWindow = !g_bShowLogWindow;

         ShowLogWindow(g_bShowLogWindow);
         break;

      case ID_FILE_EXIT:
         PostMessage(g_hwndLogFrame, WM_CLOSE, 0, 0);
         break;

      case ID_EDIT_COPY:
         SendMessage(g_hwndLogBox, WM_COPY, 0, 0);
         break;

      case ID_VIEW_CLEARLOG:
         SendMessage(g_hwndLogBox, WM_SETTEXT, 0, (LPARAM) "");
         break;

      case ID_VIEW_LOGMESSAGES:
         g_bLogMessages = !g_bLogMessages;
         /* SaveLogSettings(); */
         break;

      case ID_VIEW_MESSAGEHIGHLIGHTING:
         g_bHighlightMessages = !g_bHighlightMessages;
         /* SaveLogSettings(); */
         break;

      case ID_VIEW_LIMITBUFFERSIZE:
         g_bLimitBufferSize = !g_bLimitBufferSize;
         /* SaveLogSettings(); */
         break;

      case ID_VIEW_ACTIVITYANIMATION:
         g_bShowActivityAnimation = !g_bShowActivityAnimation;
         /* SaveLogSettings(); */
         break;

#ifdef FEATURE_TOGGLE
      /* by haroon - change toggle to its opposite value */
      case ID_TOGGLE_ENABLED:
         global_toggle_state = !global_toggle_state;
         if (global_toggle_state)
         {
            log_error(LOG_LEVEL_INFO, "Now toggled ON.");
         }
         else
         {
            log_error(LOG_LEVEL_INFO, "Now toggled OFF.");
         }
         SetIdleIcon();
         break;
#endif /* def FEATURE_TOGGLE */

      case ID_TOOLS_EDITCONFIG:
         EditFile(configfile);
         break;

      case ID_TOOLS_EDITDEFAULTACTIONS:
         EditFile(g_default_actions_file);
         break;

      case ID_TOOLS_EDITUSERACTIONS:
         EditFile(g_user_actions_file);
         break;

      case ID_TOOLS_EDITDEFAULTFILTERS:
         EditFile(g_default_filterfile);
         break;

      case ID_TOOLS_EDITUSERFILTERS:
         EditFile(g_user_filterfile);
         break;

#ifdef FEATURE_TRUST
      case ID_TOOLS_EDITTRUST:
         EditFile(g_trustfile);
         break;
#endif /* def FEATURE_TRUST */

      case ID_HELP_GPL:
         ShellExecute(g_hwndLogFrame, "open", "LICENSE.txt", NULL, NULL, SW_SHOWNORMAL);
         break;

      case ID_HELP_FAQ:
         ShellExecute(g_hwndLogFrame, "open", "doc\\faq\\index.html", NULL, NULL, SW_SHOWNORMAL);
         break;

      case ID_HELP_MANUAL:
         ShellExecute(g_hwndLogFrame, "open", "doc\\user-manual\\index.html", NULL, NULL, SW_SHOWNORMAL);
         break;

      case ID_HELP_STATUS:
         ShellExecute(g_hwndLogFrame, "open", CGI_PREFIX "show-status", NULL, NULL, SW_SHOWNORMAL);
         break;

      case ID_HELP_ABOUT:
         MessageBox(g_hwndLogFrame, win32_blurb, "About Privoxy", MB_OK);
         break;

      default:
         /* DO NOTHING */
         break;
   }

}


/*********************************************************************
 *
 * Function    :  OnLogInitMenu
 *
 * Description :  Handler for WM_INITMENU messages.  Enable, disable,
 *                check, and/or uncheck menu options as apropos.
 *
 * Parameters  :
 *          1  :  hmenu = handle to menu to "make current"
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void OnLogInitMenu(HMENU hmenu)
{
   /* Only enable editors if there is a file to edit */
   EnableMenuItem(hmenu, ID_TOOLS_EDITDEFAULTACTIONS, MF_BYCOMMAND | (g_default_actions_file ? MF_ENABLED : MF_GRAYED));
   EnableMenuItem(hmenu, ID_TOOLS_EDITUSERACTIONS, MF_BYCOMMAND | (g_user_actions_file ? MF_ENABLED : MF_GRAYED));
   EnableMenuItem(hmenu, ID_TOOLS_EDITDEFAULTFILTERS, MF_BYCOMMAND | (g_default_filterfile ? MF_ENABLED : MF_GRAYED));
   EnableMenuItem(hmenu, ID_TOOLS_EDITUSERFILTERS, MF_BYCOMMAND | (g_user_filterfile ? MF_ENABLED : MF_GRAYED));
#ifdef FEATURE_TRUST
   EnableMenuItem(hmenu, ID_TOOLS_EDITTRUST, MF_BYCOMMAND | (g_trustfile ? MF_ENABLED : MF_GRAYED));
#endif /* def FEATURE_TRUST */

   /* Check/uncheck options */
   CheckMenuItem(hmenu, ID_VIEW_LOGMESSAGES, MF_BYCOMMAND | (g_bLogMessages ? MF_CHECKED : MF_UNCHECKED));
   CheckMenuItem(hmenu, ID_VIEW_MESSAGEHIGHLIGHTING, MF_BYCOMMAND | (g_bHighlightMessages ? MF_CHECKED : MF_UNCHECKED));
   CheckMenuItem(hmenu, ID_VIEW_LIMITBUFFERSIZE, MF_BYCOMMAND | (g_bLimitBufferSize ? MF_CHECKED : MF_UNCHECKED));
   CheckMenuItem(hmenu, ID_VIEW_ACTIVITYANIMATION, MF_BYCOMMAND | (g_bShowActivityAnimation ? MF_CHECKED : MF_UNCHECKED));
#ifdef FEATURE_TOGGLE
   /* by haroon - menu item for Enable toggle on/off */
   CheckMenuItem(hmenu, ID_TOGGLE_ENABLED, MF_BYCOMMAND | (global_toggle_state ? MF_CHECKED : MF_UNCHECKED));
#endif /* def FEATURE_TOGGLE */
   CheckMenuItem(hmenu, ID_TOGGLE_SHOWWINDOW, MF_BYCOMMAND | (g_bShowLogWindow ? MF_CHECKED : MF_UNCHECKED));

}


/*********************************************************************
 *
 * Function    :  OnLogTimer
 *
 * Description :  Handler for WM_TIMER messages.
 *
 * Parameters  :
 *          1  :  nTimer = timer id (animation start/stop or clip buffer)
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void OnLogTimer(int nTimer)
{
   switch (nTimer)
   {
      case TIMER_ANIM_ID:
         TraySetIcon(g_hwndTray, 1, g_hiconAnim[g_nAnimFrame++ % ANIM_FRAMES]);
         break;

      case TIMER_ANIMSTOP_ID:
         g_nAnimFrame = 0;
         SetIdleIcon();
         KillTimer(g_hwndLogFrame, TIMER_ANIM_ID);
         KillTimer(g_hwndLogFrame, TIMER_ANIMSTOP_ID);
         break;

      case TIMER_CLIPBUFFER_ID:
      case TIMER_CLIPBUFFER_FORCE_ID:
         LogClipBuffer();
         g_bClipPending = FALSE;
         KillTimer(g_hwndLogFrame, TIMER_CLIPBUFFER_ID);
         KillTimer(g_hwndLogFrame, TIMER_CLIPBUFFER_FORCE_ID);
         break;

      default:
         /* DO NOTHING */
         break;
   }

}


/*********************************************************************
 *
 * Function    :  SetIdleIcon
 *
 * Description :  Sets the tray icon to either idle or off
 *
 * Parameters  :  none
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void SetIdleIcon()
{
#ifdef FEATURE_TOGGLE
         if (!global_toggle_state)
         {
            TraySetIcon(g_hwndTray, 1, g_hiconOff);
            /* log_error(LOG_LEVEL_INFO, "Privoxy OFF icon selected."); */
         }
         else
#endif /* def FEATURE_TOGGLE */
         TraySetIcon(g_hwndTray, 1, g_hiconIdle);
}


/*********************************************************************
 *
 * Function    :  LogRichEditProc
 *
 * Description :  Window subclass routine handles some events for the rich edit control.
 *
 * Parameters  :
 *          1  :  hwnd = window handle of the rich edit control
 *          2  :  uMsg = message number
 *          3  :  wParam = first param for this message
 *          4  :  lParam = next param for this message
 *
 * Returns     :  Appropriate M$ window message handler codes.
 *
 *********************************************************************/
LRESULT CALLBACK LogRichEditProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
   switch (uMsg)
   {
      case WM_RBUTTONUP:
      {
         POINT pt;
         pt.x = LOWORD(lParam);
         pt.y = HIWORD(lParam);
         ClientToScreen(hwnd, &pt);
         OnLogRButtonUp(wParam, pt.x, pt.y);
         return 0;
      }
      case WM_CHAR:
      {
         if ((GetKeyState(VK_CONTROL) != 0) && (wParam == 4)) /* ctrl+d */
         {
             OnLogCommand(ID_VIEW_CLEARLOG);
             return 0;
         }
      }
   }
   return CallWindowProc(g_fnLogBox, hwnd, uMsg, wParam, lParam);

}


/*********************************************************************
 *
 * Function    :  LogWindowProc
 *
 * Description :  Windows call back routine handles events on the log window.
 *
 * Parameters  :
 *          1  :  hwnd = handle of the logging window
 *          2  :  uMsg = message number
 *          3  :  wParam = first param for this message
 *          4  :  lParam = next param for this message
 *
 * Returns     :  Appropriate M$ window message handler codes.
 *
 *********************************************************************/
LRESULT CALLBACK LogWindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
   switch (uMsg)
   {
      case WM_CREATE:
         return 0;

      case WM_CLOSE:
         /* This is the end - beautiful friend - the end */
         DestroyWindow(g_hwndLogBox);
         DestroyWindow(g_hwndLogFrame);
         return 0;

      case WM_DESTROY:
         PostQuitMessage(0);
         return 0;

      case WM_SHOWWINDOW:
         g_bShowLogWindow = wParam;
      case WM_SIZE:
         /* Resize the logging window to fit the new frame */
         if (g_hwndLogBox)
         {
            RECT rc;
            GetClientRect(g_hwndLogFrame, &rc);
            SetWindowPos(g_hwndLogBox, NULL, rc.left, rc.top, rc.right - rc.left, rc.bottom - rc.top, SWP_NOZORDER);
         }
         return 0;

      case WM_INITMENU:
         OnLogInitMenu((HMENU) wParam);
         return 0;

      case WM_TIMER:
         OnLogTimer(wParam);
         return 0;

      case WM_COMMAND:
         OnLogCommand(LOWORD(wParam));
         return 0;

      case WM_SYSCOMMAND:
         switch (wParam)
         {
            case SC_CLOSE:
               if (g_bCloseHidesWindow)
               {
                  ShowLogWindow(FALSE);
                  return 0;
               }
               break;
            case SC_MINIMIZE:
               ShowLogWindow(FALSE);
               return 0;
         }
         break;

      case WM_CHAR:
         if ((GetKeyState(VK_CONTROL) != 0) && (wParam == 4)) /* ctrl+d */
         {
             OnLogCommand(ID_VIEW_CLEARLOG);
             return 0;
         }
         break;
   }

   return DefWindowProc(hwnd, uMsg, wParam, lParam);

}

#endif /* ndef _WIN_CONSOLE - entire file */

/*
  Local Variables:
  tab-width: 3
  end:
*/
