const char w32_svrapi_rcs[] = "$Id: w32svrapi.c,v 1.5 2011/09/04 11:10:56 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/w32svrapi.c,v $
 *
 * Purpose     :  Win32 Services API for Privoxy.
 *                Provides the implementation of an Win32 service to
 *                allow the code to directly register and run as a
 *                native Windows service application.
 *
 *                Since Win9x/ME platforms don't provide or support
 *                running programs as services, this code uses runtime
 *                loading and calling of the Win32 Service API, to
 *                prevent the possibility of getting "entry point not
 *                found" type errors on unsupported platforms. This adds
 *                a little more complexity to the code, but it is worth
 *                doing to provide that isolation.
 *
 * Copyright   :  Written by and Copyright (C) 2003, 2006 members of
 *                the Privoxy team.  http://www.privoxy.org/
 *
 *                Written by and Copyright (C) 2003 Ian Cummings
 *                <ian_a_c@hotmail.com>
 *
 *                Special thanks to Mates Dolák <matesek@post.cz> for
 *                some very helpful feedback and suggestions during the
 *                development of this code.
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

#ifndef STRICT
#define STRICT
#endif
#include <windows.h>
#include <process.h>

#ifndef _WIN_CONSOLE
#  include "w32log.h"
#endif /* ndef _WIN_CONSOLE */

#include "w32svrapi.h"
const char w32_svrapi_h_rcs[] = W32_SVRAPI_H_VERSION;


/* Only the ANSI Win32 APIs are used at this time. If for some
 * reason, we're building under unicode then we must stop
 */
#ifdef UNICODE
#error "Privoxy interface to Win32 Services only runs under ANSI builds. Unicode is not supported at present, but you can volunteer for the job if you like! :)"
#endif


/* Default to not running as service, unless the command line says so */
BOOL bRunAsService = FALSE;

/* According to the Win32 docs for CreateService,
 * the max length for the service name is 256 chars
 */
char szThisServiceName[260];

static BOOL get_service_description(const char *pszServiceName, char *pszDisplayName, DWORD dwDispSize);
static void WINAPI privoxy_w32_service_start(DWORD dw, LPSTR* psz);
static void WINAPI privoxy_w32_service_handler(DWORD dwOpcode);
SERVICE_TABLE_ENTRY w32ServiceDispatchTable[] = {{"", privoxy_w32_service_start}, {NULL, NULL}};
static SERVICE_STATUS_HANDLE hSrv_status = 0;
static SERVICE_STATUS srv_status;



/*********************************************************************
 * This function returns TRUE if we are running on an OS that can
 * support services, like NT, etc. It returns FALSE for Win9x/ME.
 *********************************************************************/
static BOOL HasServiceControlManager()
{
   HMODULE     hDll;
   FARPROC     pFunc;
   SC_HANDLE   hScm;

   /* Load the DLL with the SCM functions or return a failure status */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      printf("Can't load Advapi32.dll -- LoadLibrary failed!\n");
      return FALSE;
   }

   /* Get the address of the ANSI OpenSCManager function, or return a failure status */
   pFunc = GetProcAddress(hDll, "OpenSCManagerA");
   if (pFunc == NULL)
   {
      printf("Can't find OpenSCManagerA -- GetProcAddress failed!\n");
      FreeLibrary(hDll);
      return FALSE;
   }

   /* Try and connect to the SCM. If it fails check and see if the error
    * code is ERROR_CALL_NOT_IMPLEMENTED, which means:
    *    "This function is not supported on this system."
    */
   hScm = (SC_HANDLE)(*pFunc)(NULL, NULL, SC_MANAGER_CONNECT);
   if (hScm == NULL)
   {
      DWORD dwErr = GetLastError();
      if (dwErr == ERROR_CALL_NOT_IMPLEMENTED)
      {
         /* Expected error under Win9x/Me, so don't print any debug info
          * here as we'll leave that up to the calling function to do
          */
         FreeLibrary(hDll);
         return FALSE;
      }

      printf("Call to OpenSCManager failed -- GetLastError() returned %lu!\n", dwErr);
      FreeLibrary(hDll);
      return FALSE;
   }

   w32_close_service_handle(hScm);

   /* OpenSCManager function exists and works, so we're on an NT type platform */
   FreeLibrary(hDll);

   return TRUE;

} /* -END- HasServiceControlManager */


BOOL CanSystemSupportServices()
{
   BOOL bHasScm = HasServiceControlManager();
   return bHasScm;

} /* -END- CanSystemSupportServices */



/*********************************************************************
 *
 * The Service functions are defined in <winsvc.h> which is where
 * the declarations used in this file are taken from
 *
 *********************************************************************/


/*********************************************************************
 * Open a connection to the service control manager
 *********************************************************************/
SC_HANDLE w32_open_sc_manager(
  LPCTSTR lpMachineName,   /* computer name */
  LPCTSTR lpDatabaseName,  /* SCM database name */
  DWORD dwDesiredAccess)   /* access type */
{
   HMODULE     hDll = NULL;
   SC_HANDLE   hScm = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;

   /* Load the DLL with the SCM functions or return failure */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return NULL;
   }

   /* Get the address of the ANSI OpenSCManager function, or return failure */
   pFunc = GetProcAddress(hDll, "OpenSCManagerA");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return NULL;
   }

   /* Call the SCM function, and save the error code */
   hScm = (SC_HANDLE)(*pFunc)(lpMachineName, lpDatabaseName, dwDesiredAccess);
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return hScm;

} /* -END- w32_open_sc_manager */



BOOL w32_close_service_handle(
  SC_HANDLE hSCObject)   /* handle to service or SCM object */
{
   HMODULE     hDll = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;
   BOOL        bRet;

   /* Load the DLL with the SCM functions or return a failure status */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return FALSE;
   }

   /* Get the address of the CloseServiceHandle function, or return a failure status */
   pFunc = GetProcAddress(hDll, "CloseServiceHandle");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return FALSE;
   }

   /* Close the handle, and save the error code */
   bRet = (BOOL)(*pFunc)(hSCObject);
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return bRet;

} /* -END- w32_close_service_handle */



/*********************************************************************
 * Open a service
 *********************************************************************/
SC_HANDLE w32_open_service(
  SC_HANDLE hSCManager,   /* handle to SCM database */
  LPCTSTR lpServiceName,  /* service name */
  DWORD dwDesiredAccess)  /* access */
{
   HMODULE     hDll = NULL;
   SC_HANDLE   hSrv = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;

   /* Load the DLL with the SCM functions or return failure */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return NULL;
   }

   /* Get the address of the ANSI OpenService function, or return failure */
   pFunc = GetProcAddress(hDll, "OpenServiceA");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return NULL;
   }

   /* Call the SCM function, and save the error code */
   hSrv = (SC_HANDLE)(*pFunc)(hSCManager, lpServiceName, dwDesiredAccess);
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return hSrv;

} /* -END- w32_open_service */



SC_HANDLE w32_create_service(
  SC_HANDLE hSCManager,       /* handle to SCM database */
  LPCTSTR lpServiceName,      /* name of service to start */
  LPCTSTR lpDisplayName,      /* display name */
  DWORD dwDesiredAccess,      /* type of access to service */
  DWORD dwServiceType,        /* type of service */
  DWORD dwStartType,          /* when to start service */
  DWORD dwErrorControl,       /* severity of service failure */
  LPCTSTR lpBinaryPathName,   /* name of binary file */
  LPCTSTR lpLoadOrderGroup,   /* name of load ordering group */
  LPDWORD lpdwTagId,          /* tag identifier */
  LPCTSTR lpDependencies,     /* array of dependency names */
  LPCTSTR lpServiceStartName, /* account name */
  LPCTSTR lpPassword)         /* account password */
{
   HMODULE     hDll = NULL;
   SC_HANDLE   hSrv = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;

   /* Load the DLL with the SCM functions or return failure */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return NULL;
   }

   /* Get the address of the ANSI CreateService function, or return failure */
   pFunc = GetProcAddress(hDll, "CreateServiceA");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return NULL;
   }

   /* Call the SCM function, and save the error code */
   hSrv = (SC_HANDLE)(*pFunc)(hSCManager,          /* handle to SCM database */
                              lpServiceName,       /* name of service to start */
                              lpDisplayName,       /* display name */
                              dwDesiredAccess,     /* type of access to service */
                              dwServiceType,       /* type of service */
                              dwStartType,         /* when to start service */
                              dwErrorControl,      /* severity of service failure */
                              lpBinaryPathName,    /* name of binary file */
                              lpLoadOrderGroup,    /* name of load ordering group */
                              lpdwTagId,           /* tag identifier */
                              lpDependencies,      /* array of dependency names */
                              lpServiceStartName,  /* account name */
                              lpPassword);         /* account password */
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return hSrv;

} /* -END- w32_create_service */



BOOL w32_delete_service(
  SC_HANDLE hService)   /* handle to service */
{
   HMODULE     hDll = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;
   BOOL        bRet;

   /* Load the DLL with the SCM functions or return a failure status */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return FALSE;
   }

   /* Get the address of the DeleteService function, or return a failure status */
   pFunc = GetProcAddress(hDll, "DeleteService");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return FALSE;
   }

   /* Close the handle, and save the error code */
   bRet = (BOOL)(*pFunc)(hService);
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return bRet;

} /* -END- w32_delete_service */



BOOL w32_query_service_config(
  SC_HANDLE hService,                     /* handle to service */
  LPQUERY_SERVICE_CONFIG lpServiceConfig, /* buffer */
  DWORD cbBufSize,                        /* size of buffer */
  LPDWORD pcbBytesNeeded)                 /* bytes needed */
{
   HMODULE     hDll = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;
   BOOL        bRet;

   /* Load the DLL with the SCM functions or return a failure status */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return FALSE;
   }

   /* Get the address of the QueryServiceConfig function, or return a failure status */
   pFunc = GetProcAddress(hDll, "QueryServiceConfigA");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return FALSE;
   }

   /* Close the handle, and save the error code */
   bRet = (BOOL)(*pFunc)(hService, lpServiceConfig, cbBufSize, pcbBytesNeeded);
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return bRet;

} /* -END- w32_query_service_config */


BOOL w32_start_service_ctrl_dispatcher(
  CONST LPSERVICE_TABLE_ENTRY lpServiceTable)   /* service table */
{
   HMODULE     hDll = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;
   BOOL        bRet;

   /* Load the DLL with the SCM functions or return a failure status */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return FALSE;
   }

   /* Get the address of the StartServiceCtrlDispatcher function, or return a failure status */
   pFunc = GetProcAddress(hDll, "StartServiceCtrlDispatcherA");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return FALSE;
   }

   /* Close the handle, and save the error code */
   bRet = (BOOL)(*pFunc)(lpServiceTable);
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return bRet;

} /* -END- w32_start_service_ctrl_dispatcher */



SERVICE_STATUS_HANDLE w32_register_service_ctrl_handler(
  LPCTSTR lpServiceName,               /* service name */
  LPHANDLER_FUNCTION lpHandlerProc)    /* handler function */
{
   HMODULE     hDll = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;
   SERVICE_STATUS_HANDLE hServStat = (SERVICE_STATUS_HANDLE)0;

   /* Load the DLL with the SCM functions or return a failure status */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return hServStat;
   }

   /* Get the address of the RegisterServiceCtrlHandler function, or return a failure status */
   pFunc = GetProcAddress(hDll, "RegisterServiceCtrlHandlerA");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return hServStat;
   }

   /* Close the handle, and save the error code */
   hServStat = (SERVICE_STATUS_HANDLE)(*pFunc)(lpServiceName, lpHandlerProc);
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return hServStat;

} /* -END- w32_register_service_ctrl_handler */



BOOL w32_set_service_status(
  SERVICE_STATUS_HANDLE hServiceStatus,   /* service status handle */
  LPSERVICE_STATUS lpServiceStatus)       /* status buffer */
{
   HMODULE     hDll = NULL;
   FARPROC     pFunc = NULL;
   DWORD       dwLastErr = 0;
   BOOL        bRet;

   /* Load the DLL with the SCM functions or return a failure status */
   hDll = LoadLibrary("Advapi32.dll");
   if (hDll == NULL)
   {
      return FALSE;
   }

   /* Get the address of the SetServiceStatus function, or return a failure status */
   pFunc = GetProcAddress(hDll, "SetServiceStatus");
   if (pFunc == NULL)
   {
      FreeLibrary(hDll);
      return FALSE;
   }

   /* Close the handle, and save the error code */
   bRet = (BOOL)(*pFunc)(hServiceStatus, lpServiceStatus);
   dwLastErr = GetLastError();

   /* Release the library and then restore the last error
    * code, in case FreeLibrary altered it.
    */
   FreeLibrary(hDll);
   SetLastError(dwLastErr);

   return bRet;

} /* -END- w32_set_service_status */


static void display_win32_msg(BOOL bIsError, char *msg)
{
#ifdef _WIN_CONSOLE
   printf("%s", msg);
#else
   if (bIsError)
   {
      MessageBox(NULL, msg, "Privoxy Error",
         MB_OK | MB_ICONERROR | MB_TASKMODAL | MB_SETFOREGROUND | MB_TOPMOST);
   }
   else
   {
      MessageBox(NULL, msg, "Privoxy Information",
         MB_OK | MB_ICONINFORMATION | MB_TASKMODAL | MB_SETFOREGROUND | MB_TOPMOST);
   }
#endif

} /* -END- display_win32_msg */


static BOOL get_service_description(const char *pszServiceName, char *pszDisplayName, DWORD dwDispSize)
{
   /*********************************************************************
    * Create a simple display name
    *********************************************************************/
   strcpy(pszDisplayName, "Privoxy (");
   strncat(pszDisplayName, pszServiceName, dwDispSize - strlen(pszDisplayName) - 2);
   strcat(pszDisplayName, ")");

   return TRUE;
}



BOOL install_service(const char *service_name)
{
   char szModule[(MAX_PATH*2)+1];
   char szDisplayName[MAX_PATH+2];
   SC_HANDLE hSCM;
   SC_HANDLE hService;

   /*********************************************************************
    * First check if this system can support a service architecture
    *********************************************************************/
   if (!CanSystemSupportServices())
   {
      display_win32_msg(TRUE, "This system doesn't support installing Privoxy as a service.\nWinNT/2000/XP are required for this feature.\n");
      return FALSE;
   }

   /* Use a default service name if none was supplied */
   if ((service_name == NULL) || (strlen(service_name) == 0))
   {
      service_name = "privoxy";
   }

   /*********************************************************************
    * Open a handle to the Service Control Manager with full access rights
    *********************************************************************/
   hSCM = w32_open_sc_manager(NULL, NULL, SC_MANAGER_ALL_ACCESS);
   if (hSCM == NULL)
   {
      display_win32_msg(TRUE, "Can't open Service Control Manager - Service install failed!\n  Administrator rights are required to create a service.\n");
      return FALSE;
   }


   /*********************************************************************
    * Work out the full image path plus command line for the service
    * We'll temporarily use szDisplayName as a second buffer.
    *********************************************************************/
   GetModuleFileName(NULL, szDisplayName, MAX_PATH);
   sprintf(szModule, "\"%s\" --service", szDisplayName);


   /*********************************************************************
    * Get the display name for the service
    *********************************************************************/
   get_service_description(service_name, szDisplayName, sizeof(szDisplayName)/sizeof(char));


   /*********************************************************************
    * Create the service
    *********************************************************************/
   hService = w32_create_service(hSCM,
                           service_name,              /* the internal service name */
                           szDisplayName,             /* the display name */
                           SERVICE_ALL_ACCESS,        /* get full access during creation */
                           SERVICE_WIN32_OWN_PROCESS  /* run in our own process */
#ifndef _WIN_CONSOLE
                         + SERVICE_INTERACTIVE_PROCESS /* GUI also wants interactive rights */
#endif
                           ,
                           SERVICE_DEMAND_START,      /* For now, only start when asked to */
                           SERVICE_ERROR_NORMAL,      /* Normal error handling by the SCM */
                           szModule,                  /* The executable service file */
                           NULL,                      /* No load order info needed */
                           NULL,                      /* No load order info needed */
                           NULL,                      /* No dependencies */
                           NULL,                      /* Default to LocalSystem... */
                           NULL);                     /* ...which doesn't require a password */
   if (hService == NULL)
   {
      display_win32_msg(TRUE, "Can't install service!\n");
      w32_close_service_handle(hSCM);
      return FALSE;
   }

   display_win32_msg(FALSE, "Service was successfully created.\n*** IMPORTANT NOTE: You should now use the Services control panel to\n*** configure the startup type and user account details for the service.\n\n");

   /* tidy up */
   w32_close_service_handle(hService);
   w32_close_service_handle(hSCM);
   return TRUE;

} /* -END- install_service */



BOOL uninstall_service(const char *service_name)
{
   char szDisplayName[MAX_PATH+2];
   SC_HANDLE hSCM;
   SC_HANDLE hService;
   BOOL bResult = FALSE;


   /*********************************************************************
    * First check if this system can support a service architecture
    *********************************************************************/
   if (!CanSystemSupportServices())
   {
      display_win32_msg(TRUE, "This system doesn't support installing Privoxy as a service.\nWinNT/2000/XP are required for this feature.\n");
      return FALSE;
   }


   /* Use a default service name if none was supplied */
   if ((service_name == NULL) || (strlen(service_name) == 0))
   {
      service_name = "privoxy";
   }


   /*********************************************************************
    * Open a handle to the Service Control Manager with full access rights
    *********************************************************************/
   hSCM = w32_open_sc_manager(NULL, NULL, SC_MANAGER_ALL_ACCESS);
   if (hSCM == NULL)
   {
      display_win32_msg(TRUE, "Can't open Service Control Manager - Service uninstall failed!\n  Administrator rights are required to delete a service.\n");
      return FALSE;
   }


   /*********************************************************************
    * Get the display name for the service
    *********************************************************************/
   get_service_description(service_name, szDisplayName, sizeof(szDisplayName)/sizeof(char));


   /*********************************************************************
    * Open and then delete the service
    *********************************************************************/
   hService = w32_open_service(hSCM, service_name, DELETE);
   if (hService == NULL)
   {
      display_win32_msg(TRUE, "Can't open service for delete access rights!\n");
      w32_close_service_handle(hSCM);
      return FALSE;
   }

   if (w32_delete_service(hService))
   {
      display_win32_msg(FALSE, "Service was deleted successfully.\n");
      bResult = TRUE;
   }
   else
   {
      display_win32_msg(TRUE, "Service could not be deleted!\n");
      bResult = FALSE;
   }

   w32_close_service_handle(hService);
   w32_close_service_handle(hSCM);
   return bResult;

} /* -END- uninstall_service */



/*********************************************************************
 *
 * Function    :  privoxy_w32_service_start
 *
 * Description :  This is the entry point function for the service.
 *                In other words, it's the ServiceMain function.
 *
 * Parameters  :  Defined by the Win32 API, but not used here
 *
 * Returns     :  void
 *
 *********************************************************************/
static void WINAPI privoxy_w32_service_start(DWORD dw, LPSTR* pszArgs)
{
   int child_id;

   /* Arg zero is always the service name, and we need to
    * know it when we call RegisterServiceCtrlHandler.
    */
   strcpy(szThisServiceName, pszArgs[0]);

   /* Tell the SCM we are running */
   srv_status.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
   srv_status.dwCurrentState = SERVICE_RUNNING;
   srv_status.dwCheckPoint = 0;
   srv_status.dwControlsAccepted = SERVICE_ACCEPT_STOP;
   srv_status.dwWin32ExitCode = NO_ERROR;
   srv_status.dwServiceSpecificExitCode = 0;
   srv_status.dwWaitHint = 0;

   hSrv_status = w32_register_service_ctrl_handler(szThisServiceName, privoxy_w32_service_handler);
   if (!hSrv_status)
   {
      return;
   }
   w32_set_service_status(hSrv_status, &srv_status);

#ifndef FEATURE_PTHREAD
   child_id = _beginthread(w32_service_listen_loop, 0, NULL);
   if (child_id > 0)
#else
#error "FIXME: Do pthread stuff here!"
#endif
   {
      w32_set_service_status(hSrv_status, &srv_status);
   }
   else
   {
      srv_status.dwCurrentState = SERVICE_STOPPED;
      srv_status.dwCheckPoint = 0;
      srv_status.dwWin32ExitCode = ERROR_SERVICE_SPECIFIC_ERROR;
      srv_status.dwServiceSpecificExitCode = ERROR_SERVICE_NO_THREAD;
      w32_set_service_status(hSrv_status, &srv_status);
   }
}


/*********************************************************************
 *
 * Function    :  w32_set_service_cwd
 *
 * Description :  Simple function to change the current directory to
 *                the same location as the service executable.
 *
 * Parameters  :  void
 *
 * Returns     :  void
 *
 *********************************************************************/
void w32_set_service_cwd(void)
{
   char exe_name[MAX_PATH+1];
   char dir_name[MAX_PATH+1];
   char *pszFile = NULL;

   /* Get the exe name and path of the service */
   if (GetModuleFileName(NULL, exe_name, MAX_PATH))
   {
      /* Ask the API to tell us where the filename portion starts */
      if (GetFullPathName(exe_name, MAX_PATH, dir_name, &pszFile))
      {
         /* remove the filename from the string */
         if (pszFile != NULL)
         {
            *pszFile = '\0';
            /* We have just a directory path now, so make it current */
            SetCurrentDirectory(dir_name);
         }
      }
   }
}


/*********************************************************************
 *
 * Function    :  w32_service_exit_notify
 *
 * Description :  This is a simple atexit function that is called by the
 *                C runtime after exit has been called. It allows the
 *                service code to detect when the app is about to die and
 *                send an quick notification to the SCM that the service
 *                now stopped.
 *
 * Parameters  :  void
 *
 * Returns     :  void
 *
 *********************************************************************/
void w32_service_exit_notify(void)
{
   if (hSrv_status != 0)
   {
      if (srv_status.dwCurrentState != SERVICE_STOPPED)
      {
         srv_status.dwCurrentState = SERVICE_STOPPED;
         srv_status.dwCheckPoint = 0;
         srv_status.dwWin32ExitCode = ERROR_SERVICE_SPECIFIC_ERROR;
         srv_status.dwServiceSpecificExitCode = ERROR_PROCESS_ABORTED;
         w32_set_service_status(hSrv_status, &srv_status);
      }
   }
}


static void w32_mini_exit(void *p)
{
   Sleep(100);
#ifdef _WIN_CONSOLE
   exit(0);
#else
   PostMessage(g_hwndLogFrame, WM_CLOSE, 0, 0);
#endif /* def _WIN_CONSOLE */
}


/*********************************************************************
 *
 * Function    :  privoxy_w32_service_handler
 *
 * Description :  This is the control message handler function for
 *                the service.
 *
 * Parameters  :  dwOpcode
 *                   requested control code sent by SCM
 *
 * Returns     :  void
 *
 *********************************************************************/
static void WINAPI privoxy_w32_service_handler(DWORD dwOpcode)
{
   switch(dwOpcode)
   {
      case SERVICE_CONTROL_STOP:
         /* We've stopped
          */
         srv_status.dwCurrentState = SERVICE_STOPPED;
         srv_status.dwCheckPoint = 0;
         srv_status.dwWin32ExitCode = NO_ERROR;
         srv_status.dwServiceSpecificExitCode = 0;

         /* Maybe there is a more friendly way to stop, but this will do for now! */
         w32_set_service_status(hSrv_status, &srv_status);

         /* During testing, I kept getting error 109 (ERROR_BROKEN_PIPE) and
          * as far as the SCM was concerned the service was still stopping,
          * even after the process had disappeared.
          *
          * It seems that if we call exit in the ServiceMain thread, it causes
          * the SCM to not receive the status we sent in the line above. The
          * simple fix was to create a new thread to actually call exit for us
          * whilst this thread continues and returns to its caller.
          */

         if (_beginthread(w32_mini_exit, 0, NULL) < 0)
         {
            /* we failed to create the exit thread, so just force an exit here
             * and the SCM will just have to go and whistle!
             */
            exit(0);
         }
         break;

      default:
         break;
   }

   w32_set_service_status(hSrv_status, &srv_status);
}


#endif /* ifdef _WIN32 */

