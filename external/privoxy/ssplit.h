#ifndef SSPLIT_H_INCLUDED
#define SSPLIT_H_INCLUDED
#define SSPLIT_H_VERSION "$Id: ssplit.h,v 1.10 2011/09/04 11:10:56 fabiankeil Exp $"
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
 *********************************************************************/


#ifdef __cplusplus
extern "C" {
#endif

extern int ssplit(char *str, const char *delim, char *vec[], size_t vec_len,
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
