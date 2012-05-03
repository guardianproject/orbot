#ifndef SSPLIT_H_INCLUDED
#define SSPLIT_H_INCLUDED
#define SSPLIT_H_VERSION "$Id: ssplit.h,v 1.7 2006/07/18 14:48:47 david__schmidt Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/ssplit.h,v $
 *
 * Purpose     :  A function to split a string at specified deliminters.
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
 *    $Log: ssplit.h,v $
 *    Revision 1.7  2006/07/18 14:48:47  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.5  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.4  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.3  2001/07/29 18:43:08  jongfoster
 *    Changing #ifdef _FILENAME_H to FILENAME_H_INCLUDED, to conform to
 *    ANSI C rules.
 *
 *    Revision 1.2  2001/05/29 08:54:25  jongfoster
 *    Rewrote the innards of ssplit() to be easier to understand,
 *    faster, and to use less memory.  Didn't change the interface
 *    except to give the parameters meaningful names.
 *
 *    Revision 1.1.1.1  2001/05/15 13:59:04  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

extern int ssplit(char *str, const char *delim, char *vec[], int vec_len, 
                  int dont_save_empty_fields, int ignore_leading);

/* Revision control strings from this header and associated .c file */
extern const char ssplit_rcs[];
extern const char ssplit_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef SSPLIT_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
