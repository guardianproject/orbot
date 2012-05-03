#ifndef JBSOCKETS_H_INCLUDED
#define JBSOCKETS_H_INCLUDED
#define JBSOCKETS_H_VERSION "$Id: jbsockets.h,v 1.14 2008/12/20 14:53:55 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/jbsockets.h,v $
 *
 * Purpose     :  Contains wrappers for system-specific sockets code,
 *                so that the rest of Junkbuster can be more
 *                OS-independent.  Contains #ifdefs to make this work
 *                on many platforms.
 *
 * Copyright   :  Written by and Copyright (C) 2001 the SourceForge
 *                Privoxy team. http://www.privoxy.org/
 *
 *                Based on the Internet Junkbuster originally written
 *                by and Copyright (C) 1997 Anonymous Coders and 
 *                Junkbusters Corporation.  http://www.junkbusters.com
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
 *    $Log: jbsockets.h,v $
 *    Revision 1.14  2008/12/20 14:53:55  fabiankeil
 *    Add config option socket-timeout to control the time
 *    Privoxy waits for data to arrive on a socket. Useful
 *    in case of stale ssh tunnels or when fuzz-testing.
 *
 *    Revision 1.13  2008/03/21 11:13:59  fabiankeil
 *    Only gather host information if it's actually needed.
 *    Also move the code out of accept_connection() so it's less likely
 *    to delay other incoming connections if the host is misconfigured.
 *
 *    Revision 1.12  2006/07/18 14:48:46  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.9.2.1  2002/05/26 23:41:27  joergs
 *    AmigaOS: Fixed wrong type of len in write_socket()
 *
 *    Revision 1.9  2002/04/08 20:31:41  swa
 *    fixed JB spelling
 *
 *    Revision 1.8  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.7  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.6  2002/03/13 00:27:05  jongfoster
 *    Killing warnings
 *
 *    Revision 1.5  2002/03/09 20:03:52  jongfoster
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
 *    Revision 1.4  2002/03/07 03:51:36  oes
 *     - Improved handling of failed DNS lookups
 *     - Fixed compiler warnings etc
 *
 *    Revision 1.3  2001/07/29 19:01:11  jongfoster
 *    Changed _FILENAME_H to FILENAME_H_INCLUDED.
 *    Added forward declarations for needed structures.
 *
 *    Revision 1.2  2001/06/07 23:06:09  jongfoster
 *    The host parameter to connect_to() is now const.
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:54  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "project.h"

#ifdef __cplusplus
extern "C" {
#endif

struct client_state;

extern jb_socket connect_to(const char *host, int portnum, struct client_state *csp);
#ifdef AMIGA
extern int write_socket(jb_socket fd, const char *buf, ssize_t n);
#else
extern int write_socket(jb_socket fd, const char *buf, size_t n);
#endif
extern int read_socket(jb_socket fd, char *buf, int n);
extern int data_is_available(jb_socket fd, int seconds_to_wait);
extern void close_socket(jb_socket fd);

extern int bind_port(const char *hostnam, int portnum, jb_socket *pfd);
extern int accept_connection(struct client_state * csp, jb_socket fd);
extern void get_host_information(jb_socket afd, char **ip_address, char **hostname);

extern unsigned long resolve_hostname_to_ip(const char *host);

/* Revision control strings from this header and associated .c file */
extern const char jbsockets_rcs[];
extern const char jbsockets_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef JBSOCKETS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
