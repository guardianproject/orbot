#ifndef DEANIMATE_H_INCLUDED
#define DEANIMATE_H_INCLUDED
#define DEANIMATE_H_VERSION "$Id: deanimate.h,v 1.14 2011/09/04 11:10:56 fabiankeil Exp $"
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
