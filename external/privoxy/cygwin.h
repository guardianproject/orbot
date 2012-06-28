#ifndef CYGWIN_H_INCLUDED
#define CYGWIN_H_INCLUDED
#define CYGWIN_H_VERSION "$Id: cygwin.h,v 1.6 2006/07/18 14:48:45 david__schmidt Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/cygwin.h,v $
 *
 * Purpose     :  The windows.h file seems to be a *tad* different, so I
 *                will bridge the gaps here.  Perhaps I should convert the
 *                latest SDK too?  Shudder, I think not.
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
 *    $Log: cygwin.h,v $
 *    Revision 1.6  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.4  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.3  2002/03/24 13:25:43  swa
 *    name change related issues
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

/* Conditionally include this whole file. */
#ifdef __MINGW32__

/* Hmmm, seems to be overlooked. */
#define _RICHEDIT_VER 0x0300

/*
 * Named slightly different ... but not in Cygwin v1.3.1 ...
 *
 * #define LVITEM   LV_ITEM
 * #define LVCOLUMN LV_COLUMN
 */

#endif /* def __MINGW32__ */
#endif /* ndef CYGWIN_H_INCLUDED */


/*
  Local Variables:
  tab-width: 3
  end:
*/
