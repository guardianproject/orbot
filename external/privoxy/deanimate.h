#ifndef DEANIMATE_H_INCLUDED
#define DEANIMATE_H_INCLUDED
#define DEANIMATE_H_VERSION "$Id: deanimate.h,v 1.12 2008/03/28 15:13:39 fabiankeil Exp $"
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/deanimate.h,v $
 *
 * Purpose     :  Declares functions to manipulate binary images on the
 *                fly.  High-level functions include:
 *                  - Deanimation of GIF images
 *                
 *                Functions declared include: gif_deanimate and buf_free.
 *                
 *
 * Copyright   :  Written by and Copyright (C) 2001 - 2004 by the the
 *                SourceForge Privoxy team. http://www.privoxy.org/
 *
 *                Based on ideas from the Image::DeAnim Perl module by
 *                Ken MacFarlane, <ksm+cpan@universal.dca.net>
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
 *    $Log: deanimate.h,v $
 *    Revision 1.12  2008/03/28 15:13:39  fabiankeil
 *    Remove inspect-jpegs action.
 *
 *    Revision 1.11  2007/01/12 15:41:00  fabiankeil
 *    Remove some white space at EOL.
 *
 *    Revision 1.10  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.8.2.1  2004/10/03 12:53:32  david__schmidt
 *    Add the ability to check jpeg images for invalid
 *    lengths of comment blocks.  Defensive strategy
 *    against the exploit:
 *       Microsoft Security Bulletin MS04-028
 *       Buffer Overrun in JPEG Processing (GDI+) Could
 *       Allow Code Execution (833987)
 *    Enabled with +inspect-jpegs in actions files.
 *
 *    Revision 1.8  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.7  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.6  2002/03/08 17:46:04  jongfoster
 *    Fixing int/size_t warnings
 *
 *    Revision 1.5  2002/03/07 03:46:17  oes
 *    Fixed compiler warnings
 *
 *    Revision 1.4  2001/07/29 18:50:04  jongfoster
 *    Fixing "extern C" block, and renaming #define _DEANIMATE_H
 *
 *    Revision 1.3  2001/07/18 12:29:05  oes
 *    Updated prototype for gif_deanimate
 *
 *    Revision 1.2  2001/07/13 13:46:20  oes
 *    Introduced GIF deanimation feature
 *
 *
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

/*
 * A struct that holds a buffer, a read/write offset,
 * and the buffer's capacity.
 */
struct binbuffer
{
   char *buffer;
   size_t offset;
   size_t size;
};

/*
 * Function prototypes
 */
extern int gif_deanimate(struct binbuffer *src, struct binbuffer *dst, int get_first_image);
extern void buf_free(struct binbuffer *buf);

/* 
 * Revision control strings from this header and associated .c file
 */
extern const char deanimate_rcs[];
extern const char deanimate_h_rcs[];

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef DEANIMATE_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
