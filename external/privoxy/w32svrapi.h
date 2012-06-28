#ifndef W32_SVRAPI_H_INCLUDED
#define W32_SVRAPI_H_INCLUDED
#define W32_SVRAPI_H_VERSION "$Id: w32svrapi.h,v 1.3 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/w32svrapi.h,v $
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
 * Copyright   :  Written by and Copyright (C) 2003 members of
 *                the Privoxy team.  http://www.privoxy.org/
 *
 *                Written by and Copyright (C) 2003 Ian Cummings
 *                <ian_a_c@hotmail.com>
 *
 *                Special thanks to Mates Dolák <matesek@post.cz> for
 *                some very helpful feedback and suggestions during the
 *                development of this code.
 *
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

#ifdef _WIN32


extern char szThisServiceName[];
extern BOOL bRunAsService;
extern SERVICE_TABLE_ENTRY w32ServiceDispatchTable[];

extern BOOL install_service(const char *service_name);
extern BOOL uninstall_service(const char *service_name);
extern void w32_service_exit_notify(void);
extern void w32_set_service_cwd(void);
extern void w32_service_listen_loop(void *p);


extern BOOL CanSystemSupportServices();


extern SC_HANDLE w32_open_sc_manager(
  LPCTSTR lpMachineName,   /* computer name */
  LPCTSTR lpDatabaseName,  /* SCM database name */
  DWORD dwDesiredAccess);  /* access type */


extern BOOL w32_close_service_handle(
  SC_HANDLE hSCObject);    /* handle to service or SCM object */


extern SC_HANDLE w32_open_service(
  SC_HANDLE hSCManager,    /* handle to SCM database */
  LPCTSTR lpServiceName,   /* service name */
  DWORD dwDesiredAccess);  /* access */


extern SC_HANDLE w32_create_service(
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
  LPCTSTR lpPassword);        /* account password */


extern BOOL w32_delete_service(
  SC_HANDLE hService);   /* handle to service */


extern BOOL w32_query_service_config(
  SC_HANDLE hService,                     /* handle to service */
  LPQUERY_SERVICE_CONFIG lpServiceConfig, /* buffer */
  DWORD cbBufSize,                        /* size of buffer */
  LPDWORD pcbBytesNeeded);                /* bytes needed */


extern BOOL w32_start_service_ctrl_dispatcher(
  CONST LPSERVICE_TABLE_ENTRY lpServiceTable);   /* service table */


extern SERVICE_STATUS_HANDLE w32_register_service_ctrl_handler(
  LPCTSTR lpServiceName,               /* service name */
  LPHANDLER_FUNCTION lpHandlerProc);   /* handler function */


extern BOOL w32_set_service_status(
  SERVICE_STATUS_HANDLE hServiceStatus,   /* service status handle */
  LPSERVICE_STATUS lpServiceStatus);      /* status buffer */


#endif  /* def _WIN32 */


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef W32_SVRAPI_H_INCLUDED */

