const char amiga_rcs[] = "$Id: amiga.c,v 1.12 2007/01/07 07:40:52 joergs Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/amiga.c,v $
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
 * Revisions   :
 *    $Log: amiga.c,v $
 *    Revision 1.12  2007/01/07 07:40:52  joergs
 *    Added AmigaOS4 support and made it work on AmigaOS 3.x with current sources.
 *
 *    Revision 1.11  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.9  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.8  2002/03/25 19:32:15  joergs
 *    Name in version string changed from junkbuster to Privoxy.
 *
 *    Revision 1.7  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.6  2002/03/09 20:03:52  jongfoster
 *    - Making various functions return int rather than size_t.
 *      (Undoing a recent change).  Since size_t is unsigned on
 *      Windows, functions like read_socket that return -1 on
 *      error cannot return a size_t.
 *
 *      THIS WAS A MAJOR BUG - it caused frequent, unpredictable
 *      crashes, and also frequently caused JB to jump to 100%
 *      CPU and stay there.  (Because it thought it had just
 *      read ((unsigned)-1) == 4Gb of data...)
 *
 *    - The signature of write_socket has changed, it now simply
 *      returns success=0/failure=nonzero.
 *
 *    - Trying to get rid of a few warnings --with-debug on
 *      Windows, I've introduced a new type "jb_socket".  This is
 *      used for the socket file descriptors.  On Windows, this
 *      is SOCKET (a typedef for unsigned).  Everywhere else, it's
 *      an int.  The error value can't be -1 any more, so it's
 *      now JB_INVALID_SOCKET (which is -1 on UNIX, and in
 *      Windows it maps to the #define INVALID_SOCKET.)
 *
 *    - The signature of bind_port has changed.
 *
 *    Revision 1.5  2002/03/03 09:18:03  joergs
 *    Made jumbjuster work on AmigaOS again.
 *
 *    Revision 1.4  2001/10/07 15:35:13  oes
 *    Replaced 6 boolean members of csp with one bitmap (csp->flags)
 *
 *    Revision 1.3  2001/09/12 22:54:51  joergs
 *    Stacksize of main thread increased.
 *
 *    Revision 1.2  2001/05/23 00:13:58  joergs
 *    AmigaOS support fixed.
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:46  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "config.h"

#ifdef AMIGA

#include <stdio.h>
#include <signal.h>

#include "project.h"

const char amiga_h_rcs[] = AMIGA_H_VERSION;

static char *ver USED = "$VER: Privoxy " __AMIGAVERSION__ " (" __AMIGADATE__ ")";
#ifdef __amigaos4__
static char *stack USED = "$STACK: 524288";
#else
unsigned long __stack = 100*1024;
#endif
struct Task *main_task = NULL;
int childs = 0;

void serve(struct client_state *csp);

SAVEDS ULONG server_thread(void)
{
   struct client_state *local_csp;
   struct UserData UserData;
   struct Task *me=FindTask(NULL);
#ifdef __amigaos4__
   struct Library *SocketBase;
#endif

   Wait(SIGF_SINGLE);
   local_csp=(struct client_state *)(me->tc_UserData);
   me->tc_UserData=&UserData;
   SocketBase=(APTR)OpenLibrary("bsdsocket.library",3);
   if (SocketBase)
#ifdef __amigaos4__
   {
      ISocket = (struct SocketIFace *)GetInterface(SocketBase, "main", 1, NULL);
   }
   if (ISocket)
#endif
   {
      SetErrnoPtr(&(UserData.eno),sizeof(int));
      local_csp->cfd=ObtainSocket(local_csp->cfd, AF_INET, SOCK_STREAM, 0);
      if(JB_INVALID_SOCKET!=local_csp->cfd)
      {
         Signal(main_task,SIGF_SINGLE);
         serve((struct client_state *) local_csp);
      } else {
         local_csp->flags &= ~CSP_FLAG_ACTIVE;
         Signal(main_task,SIGF_SINGLE);
      }
#ifdef __amigaos4__
      DropInterface((struct Interface *)ISocket);
#endif
      CloseLibrary(SocketBase);
   } else {
#ifdef __amigaos4__
      CloseLibrary(SocketBase);
#endif
      local_csp->flags &= ~CSP_FLAG_ACTIVE;
      Signal(main_task,SIGF_SINGLE);
   }
   childs--;
   return 0;
}

static BPTR olddir;

void amiga_exit(void)
{
#ifdef __amigaos4__
   if (ISocket)
#else
   if (SocketBase)
#endif
   {
#ifdef __amigaos4__
      struct Library *SocketBase = ISocket->Data.LibBase;
      DropInterface((struct Interface *)ISocket);
#endif
      CloseLibrary(SocketBase);
   }
   CurrentDir(olddir);
}

#ifndef __amigaos4__
static struct SignalSemaphore memsem;
static struct SignalSemaphore *memsemptr = NULL;
#endif
static struct UserData GlobalUserData;

void InitAmiga(void)
{
#ifdef __amigaos4__
   struct Library *SocketBase;
#endif

   main_task = FindTask(NULL);
   main_task->tc_UserData = &GlobalUserData;

   if (((struct Library *)SysBase)->lib_Version < 39)
   {
      exit(RETURN_FAIL);
   }

   signal(SIGINT,SIG_IGN);
   SocketBase = (APTR)OpenLibrary("bsdsocket.library",3);
#ifdef __amigaos4__
   if (SocketBase)
   {
      ISocket = (struct SocketIFace *)GetInterface(SocketBase, "main", 1, NULL);
   }
   if (!ISocket)
#else
   if (!SocketBase)
#endif
   {
#ifdef __amigaos4__
      CloseLibrary(SocketBase);
#endif
      fprintf(stderr, "Can't open bsdsocket.library V3+\n");
      exit(RETURN_ERROR);
   }
   SetErrnoPtr(&(GlobalUserData.eno),sizeof(int));
#ifndef __amigaos4__
   InitSemaphore(&memsem);
   memsemptr = &memsem;
#endif

   olddir=CurrentDir(GetProgramDir());
   atexit(amiga_exit);
}

#ifndef __amigaos4__
#ifdef __GNUC__
#ifdef libnix
/* multithreadingsafe libnix replacements */
static void *memPool=NULL;

void *malloc (size_t s)
{
   ULONG *mem;
   LONG size = s;

   if (size<=0)
   {
      return NULL;
   }
   if (!memPool)
   {
      if (!(memPool=CreatePool(MEMF_ANY,32*1024,8*1024)))
      {
         return NULL;
      }
   }
   size += sizeof(ULONG) + MEM_BLOCKMASK;
   size &= ~MEM_BLOCKMASK;
   if (memsemptr)
   {
      ObtainSemaphore(memsemptr);
   }
   if ((mem=AllocPooled(memPool,size)))
   {
      *mem++=size;
   }
   if (memsemptr)
   {
      ReleaseSemaphore(memsemptr);
   }
   return mem;
}

void free (void *m)
{
   ULONG *mem = m;

   if(mem && memPool)
   {
      ULONG size=*--mem;

      if (memsemptr)
      {
         ObtainSemaphore(memsemptr);
      }
      FreePooled(memPool,mem,size);
      if (memsemptr)
      {
         ReleaseSemaphore(memsemptr);
      }
   }
}

void *realloc (void *old, size_t ns)
{
   void *new;
   LONG osize, *o = old;
   LONG nsize = ns;

   if (!old)
   {
      return malloc(nsize);
   }
   osize = (*(o-1)) - sizeof(ULONG);
   if (nsize <= osize)
   {
      return old;
   }
   if ((new = malloc(nsize)))
   {
      ULONG *n = new;

      osize >>= 2;
      while(osize--)
      {
         *n++ = *o++;
      }
      free(old);
   }
   return new;
}

void __memCleanUp (void)
{
   if (memsemptr)
   {
      ObtainSemaphore(memsemptr);
   }
   if (memPool)
   {
      DeletePool(memPool);
   }
   if (memsemptr)
   {
      ReleaseSemaphore(memsemptr);
   }
}

#define ADD2LIST(a,b,c) asm(".stabs \"_" #b "\"," #c ",0,0,_" #a )
#define ADD2EXIT(a,pri) ADD2LIST(a,__EXIT_LIST__,22); \
                        asm(".stabs \"___EXIT_LIST__\",20,0,0," #pri "+128")
ADD2EXIT(__memCleanUp,-50);
#elif !defined(ixemul)
#error No libnix and no ixemul!?
#endif /* libnix */
#else
#error Only GCC is supported, multithreading safe malloc/free required.
#endif /* __GNUC__ */
#endif /* !__amigaos4__ */

#endif /* def AMIGA */
