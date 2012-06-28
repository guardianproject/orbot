const char deanimate_rcs[] = "$Id: deanimate.c,v 1.21 2011/09/04 11:10:56 fabiankeil Exp $";
/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/deanimate.c,v $
 *
 * Purpose     :  Declares functions to manipulate binary images on the
 *                fly.  High-level functions include:
 *                  - Deanimation of GIF images
 *
 *                Functions declared include: gif_deanimate, buf_free,
 *                buf_copy,  buf_getbyte, gif_skip_data_block
 *                and gif_extract_image
 *
 * Copyright   :  Written by and Copyright (C) 2001 - 2004, 2006 by the
 *                SourceForge Privoxy team. http://www.privoxy.org/
 *
 *                Based on the GIF file format specification (see
 *                http://tronche.com/computer-graphics/gif/gif89a.html)
 *                and ideas from the Image::DeAnim Perl module by
 *                Ken MacFarlane, <ksm+cpan@universal.dca.net>
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
 **********************************************************************/


#include "config.h"

#include <string.h>
#include <fcntl.h>

#include "errlog.h"
#include "project.h"
#include "deanimate.h"
#include "miscutil.h"

const char deanimate_h_rcs[] = DEANIMATE_H_VERSION;

/*********************************************************************
 *
 * Function    :  buf_free
 *
 * Description :  Safely frees a struct binbuffer
 *
 * Parameters  :
 *          1  :  buf = Pointer to the binbuffer to be freed
 *
 * Returns     :  N/A
 *
 *********************************************************************/
void buf_free(struct binbuffer *buf)
{
   if (buf == NULL) return;

   if (buf->buffer != NULL)
   {
      free(buf->buffer);
   }

   free(buf);

}


/*********************************************************************
 *
 * Function    :  buf_extend
 *
 * Description :  Ensure that a given binbuffer can hold a given amount
 *                of bytes, by reallocating its buffer if necessary.
 *                Allocate new mem in chunks of 1024 bytes, so we don't
 *                have to realloc() too often.
 *
 * Parameters  :
 *          1  :  buf = Pointer to the binbuffer
 *          2  :  length = Desired minimum size
 *
 *
 * Returns     :  0 on success, 1 on failure.
 *
 *********************************************************************/
static int buf_extend(struct binbuffer *buf, size_t length)
{
   char *newbuf;

   if (buf->offset + length > buf->size)
   {
      buf->size = ((buf->size + length + (size_t)1023) & ~(size_t)1023);
      newbuf = (char *)realloc(buf->buffer, buf->size);

      if (newbuf == NULL)
      {
         freez(buf->buffer);
         return 1;
      }
      else
      {
         buf->buffer = newbuf;
         return 0;
      }
   }
   return 0;

}


/*********************************************************************
 *
 * Function    :  buf_copy
 *
 * Description :  Safely copies a given amount of bytes from one
 *                struct binbuffer to another, advancing the
 *                offsets appropriately.
 *
 * Parameters  :
 *          1  :  src = Pointer to the source binbuffer
 *          2  :  dst = Pointer to the destination binbuffer
 *          3  :  length = Number of bytes to be copied
 *
 * Returns     :  0 on success, 1 on failure.
 *
 *********************************************************************/
static int buf_copy(struct binbuffer *src, struct binbuffer *dst, size_t length)
{

   /*
    * Sanity check: Can't copy more data than we have
    */
   if (src->offset + length > src->size)
   {
      return 1;
   }

   /*
    * Ensure that dst can hold the new data
    */
   if (buf_extend(dst, length))
   {
      return 1;
   }

   /*
    * Now that it's safe, memcpy() the desired amount of
    * data from src to dst and adjust the offsets
    */
   memcpy(dst->buffer + dst->offset, src->buffer + src->offset, length);
   src->offset += length;
   dst->offset += length;

   return 0;

}


/*********************************************************************
 *
 * Function    :  buf_getbyte
 *
 * Description :  Safely gets a byte from a given binbuffer at a
 *                given offset
 *
 * Parameters  :
 *          1  :  src = Pointer to the source binbuffer
 *          2  :  offset = Offset to the desired byte
 *
 * Returns     :  The byte on success, or 0 on failure
 *
 *********************************************************************/
static unsigned char buf_getbyte(const struct binbuffer *src, size_t offset)
{
   if (src->offset + offset < src->size)
   {
      return (unsigned char)*(src->buffer + src->offset + offset);
   }
   else
   {
      return '\0';
   }

}


/*********************************************************************
 *
 * Function    :  gif_skip_data_block
 *
 * Description :  Safely advances the offset of a given struct binbuffer
 *                that contains a GIF image and whose offset is
 *                positioned at the start of a data block, behind
 *                that block.
 *
 * Parameters  :
 *          1  :  buf = Pointer to the binbuffer
 *
 * Returns     :  0 on success, or 1 on failure
 *
 *********************************************************************/
static int gif_skip_data_block(struct binbuffer *buf)
{
   unsigned char c;

   /*
    * Data blocks are sequences of chunks, which are headed
    * by a one-byte length field, with the last chunk having
    * zero length.
    */
   while((c = buf_getbyte(buf, 0)) != '\0')
   {
      buf->offset += (size_t)c + 1;
      if (buf->offset >= buf->size - 1)
      {
         return 1;
      }
   }
   buf->offset++;

   return 0;

}


/*********************************************************************
 *
 * Function    :  gif_extract_image
 *
 * Description :  Safely extracts an image data block from a given
 *                struct binbuffer that contains a GIF image and whose
 *                offset is positioned at the start of a data block
 *                into a given destination binbuffer.
 *
 * Parameters  :
 *          1  :  src = Pointer to the source binbuffer
 *          2  :  dst = Pointer to the destination binbuffer
 *
 * Returns     :  0 on success, or 1 on failure
 *
 *********************************************************************/
static int gif_extract_image(struct binbuffer *src, struct binbuffer *dst)
{
   unsigned char c;

   /*
    * Remember the colormap flag and copy the image head
    */
   c = buf_getbyte(src, 9);
   if (buf_copy(src, dst, 10))
   {
      return 1;
   }

   /*
    * If the image has a local colormap, copy it.
    */
   if (c & 0x80)
   {
      int map_length = 3 * (1 << ((c & 0x07) + 1));
      if (map_length <= 0)
      {
         log_error(LOG_LEVEL_DEANIMATE,
            "colormap length = %d (%c)?", map_length, c);
         return 1;
      }
      if (buf_copy(src, dst, (size_t)map_length))
      {
         return 1;
      }
   }
   if (buf_copy(src, dst, 1)) return 1;

   /*
    * Copy the image chunk by chunk.
    */
   while((c = buf_getbyte(src, 0)) != '\0')
   {
      if (buf_copy(src, dst, 1 + (size_t) c)) return 1;
   }
   if (buf_copy(src, dst, 1)) return 1;

   /*
    * Trim and rewind the dst buffer
    */
   if (NULL == (dst->buffer = (char *)realloc(dst->buffer, dst->offset))) return 1;
   dst->size = dst->offset;
   dst->offset = 0;

   return(0);

}

/*********************************************************************
 *
 * Function    :  gif_deanimate
 *
 * Description :  Deanimate a given GIF image, i.e. given a GIF with
 *                an (optional) image block and an arbitrary number
 *                of image extension blocks, produce an output GIF with
 *                only one image block that contains the last image
 *                (extenstion) block of the original.
 *                Also strip Comments, Application extenstions, etc.
 *
 * Parameters  :
 *          1  :  src = Pointer to the source binbuffer
 *          2  :  dst = Pointer to the destination binbuffer
 *          3  :  get_first_image = Flag: If set, get the first image
 *                                        If unset (default), get the last
 *
 * Returns     :  0 on success, or 1 on failure
 *
 *********************************************************************/
int gif_deanimate(struct binbuffer *src, struct binbuffer *dst, int get_first_image)
{
   unsigned char c;
   struct binbuffer *image;

   if (NULL == src || NULL == dst)
   {
      return 1;
   }

   c = buf_getbyte(src, 10);

   /*
    * Check & copy GIF header
    */
   if (strncmp(src->buffer, "GIF89a", 6) && strncmp(src->buffer, "GIF87a", 6))
   {
      return 1;
   }
   else
   {
      if (buf_copy(src, dst, 13))
      {
         return 1;
      }
   }

   /*
    * Look for global colormap and  copy if found.
    */
   if(c & 0x80)
   {
      int map_length = 3 * (1 << ((c & 0x07) + 1));
      if (map_length <= 0)
      {
         log_error(LOG_LEVEL_DEANIMATE,
            "colormap length = %d (%c)?", map_length, c);
         return 1;
      }
      if (buf_copy(src, dst, (size_t)map_length))
      {
         return 1;
      }
   }

   /*
    * Reserve a buffer for the current image block
    */
   if (NULL == (image = (struct binbuffer *)zalloc(sizeof(*image))))
   {
      return 1;
   }

   /*
    * Parse the GIF block by block and copy the relevant
    * parts to dst
    */
   while(src->offset < src->size)
   {
      switch(buf_getbyte(src, 0))
      {
         /*
          *  End-of-GIF Marker: Append current image and return
          */
      case 0x3b:
         goto write;

         /*
          * Image block: Extract to current image buffer.
          */
      case 0x2c:
         image->offset = 0;
         if (gif_extract_image(src, image)) goto failed;
         if (get_first_image) goto write;
         continue;

         /*
          * Extension block: Look at next byte and decide
          */
      case 0x21:
         switch (buf_getbyte(src, 1))
         {
            /*
             * Image extension: Copy extension  header and image
             *                  to the current image buffer
             */
         case 0xf9:
            image->offset = 0;
            if (buf_copy(src, image, 8) || buf_getbyte(src, 0) != 0x2c) goto failed;
            if (gif_extract_image(src, image)) goto failed;
            if (get_first_image) goto write;
            continue;

            /*
             * Application extension: Skip
             */
         case 0xff:
            if ((src->offset += 14) >= src->size || gif_skip_data_block(src)) goto failed;
            continue;

            /*
             * Comment extension: Skip
             */
         case 0xfe:
            if ((src->offset += 2) >= src->size || gif_skip_data_block(src)) goto failed;
            continue;

            /*
             * Plain text extension: Skip
             */
         case 0x01:
            if ((src->offset += 15) >= src->size || gif_skip_data_block(src)) goto failed;
            continue;

            /*
             * Ooops, what type of extension is that?
             */
         default:
            goto failed;

         }

         /*
          * Ooops, what type of block is that?
          */
      default:
         goto failed;

      }
   } /* -END- while src */

   /*
    * Either we got here by goto, or because the GIF is
    * bogus and EOF was reached before an end-of-gif marker
    * was found.
    */

failed:
   buf_free(image);
   return 1;

   /*
    * Append the current image to dst and return
    */

write:
   if (buf_copy(image, dst, image->size)) goto failed;
   if (buf_extend(dst, 1)) goto failed;
   *(dst->buffer + dst->offset++) = 0x3b;
   buf_free(image);
   return 0;

}


/*
  Local Variables:
  tab-width: 3
  end:
*/
