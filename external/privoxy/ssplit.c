const char ssplit_rcs[] = "$Id: ssplit.c,v 1.9 2007/11/03 14:35:45 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/ssplit.c,v $
 *
 * Purpose     :  A function to split a string at specified delimiters.
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
 *    $Log: ssplit.c,v $
 *    Revision 1.9  2007/11/03 14:35:45  fabiankeil
 *    Fix spelling in Purpose line. Patch submitted by Simon Ruderich.
 *
 *    Revision 1.8  2006/07/18 14:48:47  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.6  2002/03/26 22:29:55  swa
 *    we have a new homepage!
 *
 *    Revision 1.5  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.4  2001/11/13 00:16:38  jongfoster
 *    Replacing references to malloc.h with the standard stdlib.h
 *    (See ANSI or K&R 2nd Ed)
 *
 *    Revision 1.3  2001/05/29 08:54:25  jongfoster
 *    Rewrote the innards of ssplit() to be easier to understand,
 *    faster, and to use less memory.  Didn't change the interface
 *    except to give the parameters meaningful names.
 *
 *    Revision 1.2  2001/05/17 23:01:01  oes
 *     - Cleaned CRLF's from the sources and related files
 *
 *    Revision 1.1.1.1  2001/05/15 13:59:04  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "config.h"

#include <string.h>
#include <stdlib.h>

#include "ssplit.h"
#include "miscutil.h"

const char ssplit_h_rcs[] = SSPLIT_H_VERSION;

/* Define this for lots of debugging information to stdout */
#undef SSPLIT_VERBOSE
/* #define SSPLIT_VERBOSE 1 */


/*********************************************************************
 *
 * Function    :  ssplit
 *
 * Description :  Split a string using delimiters in `delim'.  Results
 *                go into `vec'.
 *
 * Parameters  :
 *          1  :  str = string to split.  Will be split in place
 *                (i.e. do not free until you've finished with vec,
 *                previous contents will be trashed by the call).
 *          2  :  delim = array of delimiters (if NULL, uses " \t").
 *          3  :  vec[] = results vector (aka. array) [out]
 *          4  :  vec_len = number of usable slots in the vector (aka. array size)
 *          5  :  dont_save_empty_fields = zero if consecutive delimiters
 *                give a null output field(s), nonzero if they are just 
 *                to be considered as single delimeter
 *          6  :  ignore_leading = nonzero to ignore leading field
 *                separators.
 *
 * Returns     :  -1 => Error: vec_len is too small to hold all the 
 *                      data, or str == NULL.
 *                >=0 => the number of fields put in `vec'.
 *                On error, vec and str may still have been overwritten.
 *
 *********************************************************************/
int ssplit(char *str, const char *delim, char *vec[], int vec_len, 
           int dont_save_empty_fields, int ignore_leading)
{
   unsigned char is_delim[256];
   unsigned char char_type;
   int vec_count = 0;

   if (!str)
   {
      return(-1);
   }


   /* Build is_delim array */

   memset(is_delim, '\0', sizeof(is_delim));

   if (!delim)
   {
      delim = " \t";  /* default field separators */
   }

   while (*delim)
   {
      is_delim[(unsigned)(unsigned char)*delim++] = 1;   /* separator  */
   }

   is_delim[(unsigned)(unsigned char)'\0'] = 2;   /* terminator */
   is_delim[(unsigned)(unsigned char)'\n'] = 2;   /* terminator */


   /* Parse string */

   if (ignore_leading)
   {
      /* skip leading separators */
      while (is_delim[(unsigned)(unsigned char)*str] == 1)
      {
         str++;
      }
   }

   /* first pointer is the beginning of string */
   /* Check if we want to save this field */
   if ( (!dont_save_empty_fields)
     || (is_delim[(unsigned)(unsigned char)*str] == 0) )
      {
      /*
       * We want empty fields, or the first character in this 
       * field is not a delimiter or the end of string.
       * So save it.
       */
      if (vec_count >= vec_len)
      {
         return(-1); /* overflow */
      }
      vec[vec_count++] = (char *) str;   
   }

   while ((char_type = is_delim[(unsigned)(unsigned char)*str]) != 2)
   {
      if (char_type == 1)    
      {
         /* the char is a separator */

         /* null terminate the substring */
         *str++ = '\0';      

         /* Check if we want to save this field */
         if ( (!dont_save_empty_fields)
           || (is_delim[(unsigned)(unsigned char)*str] == 0) )
            {
            /*
             * We want empty fields, or the first character in this 
             * field is not a delimiter or the end of string.
             * So save it.
             */
            if (vec_count >= vec_len)
            {
               return(-1); /* overflow */
            }
            vec[vec_count++] = (char *) str;   
         }
      }
      else
      {
         str++;
      }
   }
   *str = '\0';     /* null terminate the substring */

#ifdef SSPLIT_VERBOSE
   {
      int i;
      printf("dump %d strings\n", vec_count);
      for (i = 0; i < vec_count; i++)
      {
         printf("%d '%s'\n", i, vec[i]);
      }
   }
#endif /* def SSPLIT_VERBOSE */

   return(vec_count);
}


/*
  Local Variables:
  tab-width: 3
  end:
*/
