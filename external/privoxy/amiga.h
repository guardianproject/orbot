#ifdef AMIGA
#ifndef AMIGA_H_INCLUDED
#define AMIGA_H_INCLUDED
#define AMIGA_H_VERSION "$Id: amiga.h,v 1.14 2011/09/04 11:10:56 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/amiga.h,v $
 *
 * Purpose     :  Amiga-specific declarations.
 *
 * Copyright   :  Written by and Copyright (C) 2001 the SourceForge
 *                Privoxy team. http://www.privoxy.org/
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


#define _KERNEL
#include <sys/socket.h>
#undef _KERNEL

#define __NOLIBBASE__
#define __NOGLOBALIFACE__
#include <proto/socket.h>
#undef __NOLIBBASE__
#undef __NOGLOBALIFACE__

#define __CONSTLIBBASEDECL__ const
#include <proto/exec.h>
#include <exec/tasks.h>
#include <proto/dos.h>
#include <dos/dostags.h>

struct UserData
{
#ifdef __amigaos4__
   struct SocketIFace *si;
#else
   struct Library *sb;
#endif
   int eno;
};

#ifdef __amigaos4__
#define ISocket (((struct UserData *)(FindTask(NULL)->tc_UserData))->si)
#undef errno
#else
#define SocketBase ((struct Library *)(((struct UserData *)(FindTask(NULL)->tc_UserData))->sb))
#endif
#define errno (((struct UserData *)(FindTask(NULL)->tc_UserData))->eno)
#define select(a,b,c,d,e) WaitSelect(a,b,c,d,e,NULL)
#define inet_ntoa(x) Inet_NtoA(x.s_addr)

extern int childs;
extern struct Task *main_task;

void InitAmiga(void);
void amiga_exit(void);
void __memCleanUp(void);
SAVEDS ULONG server_thread(void);

#ifdef __amigaos4__
#define exit(x)                                             \
{                                                           \
   if(main_task)                                            \
   {                                                        \
      if(main_task == FindTask(NULL))                       \
      {                                                     \
         while(childs) Delay(10*TICKS_PER_SECOND); exit(x); \
      }                                                     \
      else                                                  \
      {                                                     \
         if (ISocket)                                       \
         {                                                  \
             struct Library *sb = ISocket->Data.LibBase;    \
             DropInterface((struct Interface *)ISocket);    \
             CloseLibrary(sb);                              \
         }                                                  \
         childs--;                                          \
         RemTask(NULL);                                     \
      }                                                     \
   }                                                        \
   else                                                     \
   {                                                        \
      exit(x);                                              \
   }                                                        \
}
#else
#define exit(x)                                             \
{                                                           \
   if(main_task)                                            \
   {                                                        \
      if(main_task == FindTask(NULL))                       \
      {                                                     \
         while(childs) Delay(10*TICKS_PER_SECOND); exit(x); \
      }                                                     \
      else                                                  \
      {                                                     \
         CloseLibrary(SocketBase);                          \
         childs--;                                          \
         RemTask(NULL);                                     \
      }                                                     \
   }                                                        \
   else                                                     \
   {                                                        \
      exit(x);                                              \
   }                                                        \
}

#undef HAVE_RANDOM
#define h_errno 0
#define HAVE_TIMEGM
#define timegm(tm) mktime(tm)
#endif /* __amigaos4__ */

#undef EINTR
#define EINTR 0

#endif /* ndef AMIGA_H_INCLUDED */
#endif /* def AMIGA */
