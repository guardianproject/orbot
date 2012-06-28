const char encode_rcs[] = "$Id: encode.c,v 1.24 2011/11/06 11:51:57 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/encode.c,v $
 *
 * Purpose     :  Functions to encode and decode URLs, and also to
 *                encode cookies and HTML text.
 *
 * Copyright   :  Written by and Copyright (C) 2001 the
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


#include "config.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "miscutil.h"
#include "encode.h"

const char encode_h_rcs[] = ENCODE_H_VERSION;

/* Maps special characters in a URL to their equivalent % codes. */
static const char const url_code_map[256][4] = {
   "",    "%01", "%02", "%03", "%04", "%05", "%06", "%07", "%08", "%09",
   "%0A", "%0B", "%0C", "%0D", "%0E", "%0F", "%10", "%11", "%12", "%13",
   "%14", "%15", "%16", "%17", "%18", "%19", "%1A", "%1B", "%1C", "%1D",
   "%1E", "%1F", "%20", "%21", "%22", "%23", "%24", "%25", "%26", "%27",
   "%28", "%29", "",    "%2B", "%2C", "",    "",    "%2F", "",    "",
   "",    "",    "",    "",    "",    "",    "",    "",    "%3A", "%3B",
   "%3C", "%3D", "%3E", "%3F", "",    "",    "",    "",    "",    "",
   "",    "",    "",    "",    "",    "",    "",    "",    "",    "",
   "",    "",    "",    "",    "",    "",    "",    "",    "",    "",
   "",    "%5B", "%5C", "%5D", "%5E", "",    "%60", "",    "",    "",
   "",    "",    "",    "",    "",    "",    "",    "",    "",    "",
   "",    "",    "",    "",    "",    "",    "",    "",    "",    "",
   "",    "",    "",    "%7B", "%7C", "%7D", "%7E", "%7F", "%80", "%81",
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

/* Maps special characters in HTML to their equivalent entities. */
static const char * const html_code_map[256] = {
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL,"&quot;",NULL,NULL,NULL,"&amp;","&#39;",
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
 *                XXX: url_query_encode() would be a more fitting
 *                     name.
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
         const char *replace_with = url_code_map[(unsigned char) c];
         if (*replace_with != '\0')
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
   int d1;

   d1 = xdtoi(*s);
   if(d1 >= 0)
   {
      int d2 = xdtoi(*(s+1));
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


/*********************************************************************
 *
 * Function    :  percent_encode_url
 *
 * Description :  Percent-encodes a string so it no longer contains
 *                any characters that aren't valid in an URL according
 *                to RFC 3986.
 *
 *                XXX: Do not confuse with encode_url()
 *
 * Parameters  :
 *          1  :  s = String to encode.  Null-terminated.
 *
 * Returns     :  Encoded string, newly allocated on the heap.
 *                Caller is responsible for freeing it with free().
 *                If s is NULL, or on out-of memory, returns NULL.
 *
 *********************************************************************/
char *percent_encode_url(const char *s)
{
   static const char allowed_characters[128] = {
      '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
      '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
      '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
      '\0', '\0', '\0', '!',  '\0', '#',  '$',  '%',  '&',  '\'',
      '(',  ')',  '*',  '+',  ',',  '-',  '.',  '/',  '0',  '1',
      '2',  '3',  '4',  '5',  '6',  '7',  '8',  '9',  ':',  ';',
      '\0', '=',  '\0', '?',  '@',  'A',  'B',  'C',  'D',  'E',
      'F',  'G',  'H',  'I',  'J',  'K',  'L',  'M',  'N',  'O',
      'P',  'Q',  'R',  'S',  'T',  'U',  'V',  'W',  'X',  'Y',
      'Z',  '[',  '\0', ']',  '\0', '_',  '\0', 'a',  'b',  'c',
      'd',  'e',  'f',  'g',  'h',  'i',  'j',  'k',  'l',  'm',
      'n',  'o',  'p',  'q',  'r',  's',  't',  'u',  'v',  'w',
      'x',  'y',  'z',  '\0', '\0', '\0', '~',  '\0'
   };
   char *buf;
   size_t buf_size;

   assert(s != NULL);

   /* Each input char can expand to at most 3 chars. */
   buf_size = (strlen(s) * 3) + 1;
   buf = (char *)malloc(buf_size);

   if (buf != NULL)
   {
      char c;
      char *p = buf;
      while((c = *s++) != '\0')
      {
         const unsigned int i = (unsigned char)c;
         if (i >= sizeof(allowed_characters) || '\0' == allowed_characters[i])
         {
            const char *replace_with = url_code_map[i];
            assert(*replace_with != '\0');
            if (*replace_with != '\0')
            {
               const size_t bytes_written = (size_t)(p - buf);
               assert(bytes_written < buf_size);
               p += strlcpy(p, replace_with, buf_size - bytes_written);
            }
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


/*
  Local Variables:
  tab-width: 3
  end:
*/
