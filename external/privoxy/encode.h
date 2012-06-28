#ifndef ENCODE_H_INCLUDED
#define ENCODE_H_INCLUDED
#define ENCODE_H_VERSION "$Id: encode.h,v 1.9 2008/05/21 15:38:13 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/encode.h,v $
 *
 * Purpose     :  Functions to encode and decode URLs, and also to
 *                encode cookies and HTML text.
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
 *    $Log: encode.h,v $
 *    Revision 1.9  2008/05/21 15:38:13  fabiankeil
 *    Garbage-collect cookie_encode().
 *
 *    Revision 1.8  2007/08/18 14:34:27  fabiankeil
 *    Make xtoi() extern so it can be used in pcrs.c.
 *
 *    Revision 1.7  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.5  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.4  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.3  2002/01/22 23:28:07  jongfoster
 *    Adding convenience function html_encode_and_free_original()
 *    Making all functions accept NULL paramaters - in this case, they
 *    simply return NULL.  This allows error-checking to be deferred.
 *
 *    Revision 1.2  2001/07/29 18:43:08  jongfoster
 *    Changing #ifdef _FILENAME_H to FILENAME_H_INCLUDED, to conform to
 *    ANSI C rules.
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:51  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

extern char * html_encode(const char *s);
extern char * url_encode(const char *s);
extern char * url_decode(const char *str);
extern int    xtoi(const char *s);
extern char * html_encode_and_free_original(char *s);

/* Revision control strings from this header and associated .c file */
extern const char encode_rcs[];
extern const char encode_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef ENCODE_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
