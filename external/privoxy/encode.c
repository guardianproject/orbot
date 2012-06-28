const char encode_rcs[] = "$Id: encode.c,v 1.14 2008/05/21 15:38:13 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/encode.c,v $
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
 *    $Log: encode.c,v $
 *    Revision 1.14  2008/05/21 15:38:13  fabiankeil
 *    Garbage-collect cookie_encode().
 *
 *    Revision 1.13  2007/08/18 14:34:27  fabiankeil
 *    Make xtoi() extern so it can be used in pcrs.c.
 *
 *    Revision 1.12  2007/08/04 10:15:51  fabiankeil
 *    Use strlcpy() instead of strcpy().
 *
 *    Revision 1.11  2006/12/28 18:25:53  fabiankeil
 *    Fixed gcc43 compiler warning.
 *
 *    Revision 1.10  2006/07/18 14:48:45  david__schmidt
 *    Reorganizing the repository: swapping out what was HEAD (the old 3.1 branch)
 *    with what was really the latest development (the v_3_0_branch branch)
 *
 *    Revision 1.8  2002/03/26 22:29:54  swa
 *    we have a new homepage!
 *
 *    Revision 1.7  2002/03/24 13:25:43  swa
 *    name change related issues
 *
 *    Revision 1.6  2002/03/13 00:27:04  jongfoster
 *    Killing warnings
 *
 *    Revision 1.5  2002/03/07 03:46:53  oes
 *    Fixed compiler warnings etc
 *
 *    Revision 1.4  2002/01/22 23:28:07  jongfoster
 *    Adding convenience function html_encode_and_free_original()
 *    Making all functions accept NULL paramaters - in this case, they
 *    simply return NULL.  This allows error-checking to be deferred.
 *
 *    Revision 1.3  2001/11/13 00:16:40  jongfoster
 *    Replacing references to malloc.h with the standard stdlib.h
 *    (See ANSI or K&R 2nd Ed)
 *
 *    Revision 1.2  2001/05/17 22:52:35  oes
 *     - Cleaned CRLF's from the sources and related files
 *
 *    Revision 1.1.1.1  2001/05/15 13:58:51  oes
 *    Initial import of version 2.9.3 source tree
 *
 *
 *********************************************************************/


#include "config.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "miscutil.h"
#include "encode.h"

const char encode_h_rcs[] = ENCODE_H_VERSION;

/* Maps special characters in a URL to their equivalent % codes. */
static const char * const url_code_map[256] = {
   NULL, "%01", "%02", "%03", "%04", "%05", "%06", "%07", "%08", "%09",
   "%0A", "%0B", "%0C", "%0D", "%0E", "%0F", "%10", "%11", "%12", "%13",
   "%14", "%15", "%16", "%17", "%18", "%19", "%1A", "%1B", "%1C", "%1D",
   "%1E", "%1F", "+",   "%21", "%22", "%23", "%24", "%25", "%26", "%27",
   "%28", "%29", NULL,  "%2B", "%2C", NULL,  NULL,  "%2F", NULL,  NULL,
   NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  "%3A", "%3B",
   "%3C", "%3D", "%3E", "%3F", NULL,  NULL,  NULL,  NULL,  NULL,  NULL,
   NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,
   NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,
   NULL,  "%5B", "%5C", "%5D", "%5E", NULL,  "%60", NULL,  NULL,  NULL,
   NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,
   NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,  NULL,
   NULL,  NULL,  NULL,  "%7B", "%7C", "%7D", "%7E", "%7F", "%80", "%81",
   "%82", "%83", "%84", "%85", "%86", "%87", "%88", "%89", "%8A", "%8B",
   "%8C", "%8D", "%8E", "%8F", "%90", "%91", "%92", "%93", "%94", "%95",
   "%96", "%97", "%98", "%99", "%9A", "%9B", "%9C", "%9D", "%9E", "%9F",
   "%A0", "%A1", "%A2", "%A3", "%A4", "%A5", "%A6", "%A7", "%A8", "%A9",
   "%AA", "%AB", "%AC", "%AD", "%AE", "%AF", "%B0", "%B1", "%B2", "%B3",
   "%B4", "%B5", "%B6", "%B7", "%B8", "%B9", "%BA", "%BB", "%BC", "%BD",
   "%BE", "%BF", "%C0", "%C1", "%C2", "%C3", "%C4", "%C5", "%C6", "%C7",
   "%C8", "%C9", "%CA", "%CB", "%CC", "%CD", "%CE", "%CF", "%D0", "%D1",
   "%D2", "%D3", "%D4", "%D5", "%D6", "%D7", "%D8", "%D9", "%DA", "%DB",
   "%DC", "%DD", "%DE", "%DF", "%E0", "%E1", "%E2", "%E3", "%E4", "%E5",
   "%E6", "%E7", "%E8", "%E9", "%EA", "%EB", "%EC", "%ED", "%EE", "%EF",
   "%F0", "%F1", "%F2", "%F3", "%F4", "%F5", "%F6", "%F7", "%F8", "%F9",
   "%FA", "%FB", "%FC", "%FD", "%FE", "%FF"
};

/* Maps special characters in HTML to their equivalent entites. */
static const char * const html_code_map[256] = {
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL,"&quot;",NULL,NULL,NULL,"&amp;",NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   "&lt;",NULL,"&gt;",NULL,NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL
};


/*********************************************************************
 *
 * Function    :  html_encode
 *
 * Description :  Encodes a string so it's not interpreted as
 *                containing HTML tags or entities.
 *                Replaces <, >, &, and " with the appropriate HTML
 *                entities.
 *
 * Parameters  :
 *          1  :  s = String to encode.  Null-terminated.
 *
 * Returns     :  Encoded string, newly allocated on the heap. 
 *                Caller is responsible for freeing it with free().
 *                If s is NULL, or on out-of memory, returns NULL.
 *
 *********************************************************************/
char * html_encode(const char *s)
{
   char * buf;
   size_t buf_size;
   
   if (s == NULL)
   {
      return NULL;
   }

   /* each input char can expand to at most 6 chars */
   buf_size = (strlen(s) * 6) + 1;
   buf = (char *) malloc(buf_size);

   if (buf)
   {
      char c;
      char * p = buf;
      while ( (c = *s++) != '\0')
      {
         const char * replace_with = html_code_map[(unsigned char) c];
         if(replace_with != NULL)
         {
            const size_t bytes_written = (size_t)(p - buf);
            assert(bytes_written < buf_size);
            p += strlcpy(p, replace_with, buf_size - bytes_written);
         }
         else
         {
            *p++ = c;
         }
      }

      *p = '\0';
   }

   assert(strlen(buf) < buf_size);
   return(buf);
}


/*********************************************************************
 *
 * Function    :  html_encode_and_free_original
 *
 * Description :  Encodes a string so it's not interpreted as
 *                containing HTML tags or entities.
 *                Replaces <, >, &, and " with the appropriate HTML
 *                entities.  Free()s original string.
 *                If original string is NULL, simply returns NULL.
 *
 * Parameters  :
 *          1  :  s = String to encode.  Null-terminated.
 *
 * Returns     :  Encoded string, newly allocated on the heap. 
 *                Caller is responsible for freeing it with free().
 *                If s is NULL, or on out-of memory, returns NULL.
 *
 *********************************************************************/
char * html_encode_and_free_original(char *s)
{
   char * result;
   
   if (s == NULL)
   {
      return NULL;
   }

   result = html_encode(s);
   free(s);

   return result;
}


/*********************************************************************
 *
 * Function    :  url_encode
 *
 * Description :  Encodes a string so it can be used in a URL
 *                query string.  Replaces special characters with
 *                the appropriate %xx codes.
 *
 * Parameters  :
 *          1  :  s = String to encode.  Null-terminated.
 *
 * Returns     :  Encoded string, newly allocated on the heap. 
 *                Caller is responsible for freeing it with free().
 *                If s is NULL, or on out-of memory, returns NULL.
 *
 *********************************************************************/
char * url_encode(const char *s)
{
   char * buf;
   size_t buf_size;

   if (s == NULL)
   {
      return NULL;
   }

   /* each input char can expand to at most 3 chars */
   buf_size = (strlen(s) * 3) + 1;
   buf = (char *) malloc(buf_size);

   if (buf)
   {
      char c;
      char * p = buf;
      while( (c = *s++) != '\0')
      {
         const char * replace_with = url_code_map[(unsigned char) c];
         if (replace_with != NULL)
         {
            const size_t bytes_written = (size_t)(p - buf);
            assert(bytes_written < buf_size);
            p += strlcpy(p, replace_with, buf_size - bytes_written);
         }
         else
         {
            *p++ = c;
         }
      }

      *p = '\0';

   }

   assert(strlen(buf) < buf_size);
   return(buf);
}


/*********************************************************************
 *
 * Function    :  xdtoi
 *
 * Description :  Converts a single hex digit to an integer.
 *
 * Parameters  :
 *          1  :  d = in the range of ['0'..'9', 'A'..'F', 'a'..'f']
 *
 * Returns     :  The integer value, or -1 for non-hex characters.
 *
 *********************************************************************/
static int xdtoi(const int d)
{
   if ((d >= '0') && (d <= '9'))
   {
      return(d - '0');
   }
   else if ((d >= 'a') && (d <= 'f')) 
   {
      return(d - 'a' + 10);
   }
   else if ((d >= 'A') && (d <= 'F'))
   {
      return(d - 'A' + 10);
   }
   else
   {
      return(-1);
   }
}


/*********************************************************************
 *
 * Function    :  xtoi
 *
 * Description :  Hex string to integer conversion.
 *
 * Parameters  :
 *          1  :  s = a 2 digit hex string (e.g. "1f").  Only the
 *                    first two characters will be looked at.
 *
 * Returns     :  The integer value, or 0 for non-hex strings.
 *
 *********************************************************************/
int xtoi(const char *s)
{
   int d1, d2;

   d1 = xdtoi(*s);
   if(d1 >= 0)
   {
      d2 = xdtoi(*(s+1));
      if(d2 >= 0)
      {
         return (d1 << 4) + d2;
      }
   }

   return 0;
}


/*********************************************************************
 *
 * Function    :  url_decode
 *
 * Description :  Decodes a URL query string, replacing %xx codes
 *                with their decoded form.
 *
 * Parameters  :
 *          1  :  s = String to decode.  Null-terminated.
 *
 * Returns     :  Decoded string, newly allocated on the heap. 
 *                Caller is responsible for freeing it with free().
 *
 *********************************************************************/
char *url_decode(const char * s)
{
   char *buf = malloc(strlen(s) + 1);
   char *q = buf;

   if (buf)
   {
      while (*s)
      {
         switch (*s)
         {
            case '+':
               s++;
               *q++ = ' ';
               break;

            case '%':
               if ((*q = (char)xtoi(s + 1)) != '\0')
               {
                  s += 3;
                  q++;
               }
               else
               {
                  /* malformed, just use it */
                  *q++ = *s++;
               }
               break;

            default:
               *q++ = *s++;
               break;
         }
      }
      *q = '\0';
   }

   return(buf);

}


/*
  Local Variables:
  tab-width: 3
  end:
*/
